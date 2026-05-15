package com.quant.aiorchestrator.consumer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.aiorchestrator.manager.TaskCacheVersionManager;
import com.quant.aiorchestrator.manager.TaskStateManager;
import com.quant.aiorchestrator.manager.TaskTraceManager;
import com.quant.aiorchestrator.mapper.ResearchTaskMapper;
import com.quant.aiorchestrator.service.AiTaskInboundMessageSupportService;
import com.quant.aiorchestrator.service.TaskMessageLogService;
import com.quant.common.messaging.KafkaTopicConstants;
import com.quant.common.model.enums.TaskStageEnum;
import com.quant.common.model.enums.TaskStatusEnum;
import com.quant.common.model.message.AiTaskStatusMessage;
import com.quant.common.redis.RedisKeyConstants;
import com.quant.common.redis.RedisKeyBuilder;
import com.quant.common.web.TraceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiTaskStatusConsumer {

    private static final String SERVICE_NAME = "ai-orchestration-service";
    private static final String CONSUMER_GROUP = "ai-orchestration-status-group";

    private final ResearchTaskMapper researchTaskMapper;
    private final TaskStateManager taskStateManager;
    private final TaskTraceManager taskTraceManager;
    private final StringRedisTemplate stringRedisTemplate;
    private final TaskCacheVersionManager taskCacheVersionManager;
    private final TaskMessageLogService taskMessageLogService;
    private final AiTaskInboundMessageSupportService inboundMessageSupportService;

    @KafkaListener(topics = KafkaTopicConstants.AI_TASK_STATUS, groupId = CONSUMER_GROUP)
    public void onMessage(String rawMessage) throws Exception {
        AiTaskStatusMessage message = inboundMessageSupportService.parseOrNull(
                rawMessage,
                AiTaskStatusMessage.class,
                KafkaTopicConstants.AI_TASK_STATUS,
                CONSUMER_GROUP,
                SERVICE_NAME
        );
        if (message == null) {
            return;
        }
        if (inboundMessageSupportService.rejectIfInvalidEnvelope(
                message,
                message.getPayload() != null,
                rawMessage,
                KafkaTopicConstants.AI_TASK_STATUS,
                CONSUMER_GROUP,
                SERVICE_NAME
        )) {
            return;
        }
        if (!StringUtils.hasText(message.getPayload().getStatus())) {
            inboundMessageSupportService.rejectInvalidMessage(
                    message,
                    rawMessage,
                    KafkaTopicConstants.AI_TASK_STATUS,
                    CONSUMER_GROUP,
                    SERVICE_NAME,
                    "STATUS_MISSING",
                    "payload.status is blank"
            );
            return;
        }
        log.info("consume ai task status, taskId={}, stage={}, node={}",
                message.getTaskId(),
                message.getPayload().getCurrentStage(),
                message.getPayload().getCurrentNode());
        TraceContext.bind(message.getTraceId());
        String skipReason = null;
        boolean failed = false;
        try {
            if (!taskMessageLogService.beginConsume(KafkaTopicConstants.AI_TASK_STATUS, message, SERVICE_NAME)) {
                skipReason = "DUPLICATE_MESSAGE";
                return;
            }
            ResearchTaskDO task = researchTaskMapper.selectOne(
                    new LambdaQueryWrapper<ResearchTaskDO>()
                            .eq(ResearchTaskDO::getTaskId, message.getTaskId())
                            .last("limit 1")
            );
            if (task == null) {
                skipReason = "TASK_NOT_FOUND";
                return;
            }

            int currentRetryCount = task.getRetryCount() == null ? 0 : task.getRetryCount();
            int messageRetryCount = message.getRetryCount() == null ? 0 : message.getRetryCount();
            if (messageRetryCount != currentRetryCount) {
                log.warn("ignore ai task status because retry count mismatched, taskId={}, currentRetryCount={}, messageRetryCount={}",
                        message.getTaskId(), currentRetryCount, messageRetryCount);
                skipReason = "RETRY_COUNT_MISMATCH";
                return;
            }

            if (taskStateManager.isFinalState(task.getStatus())) {
                log.warn("ignore ai task status because task already finalized, taskId={}, currentStatus={}, incomingStage={}",
                        message.getTaskId(), task.getStatus(), message.getPayload().getCurrentStage());
                skipReason = "TASK_ALREADY_FINALIZED";
                return;
            }

            TaskStatusEnum incomingStatus = TaskStatusEnum.from(message.getPayload().getStatus());
            if (incomingStatus == null) {
                log.warn("ignore ai task status because incoming status is invalid, taskId={}, incomingStatus={}",
                        message.getTaskId(), message.getPayload().getStatus());
                skipReason = "INVALID_STATUS";
                return;
            }
            if (incomingStatus != TaskStatusEnum.RUNNING) {
                log.info("ignore terminal ai task status update, taskId={}, incomingStatus={}, stage={}",
                        message.getTaskId(), incomingStatus.name(), message.getPayload().getCurrentStage());
                skipReason = "TERMINAL_STATUS_IGNORED";
                return;
            }
            if (!taskStateManager.canAcceptProgressUpdate(task.getStatus())) {
                skipReason = "STATUS_TRANSFER_NOT_ALLOWED";
                return;
            }

            String currentStage = TaskStageEnum.normalize(message.getPayload().getCurrentStage());

            int updated = researchTaskMapper.updateTaskStage(
                    message.getTaskId(),
                    TaskStatusEnum.RUNNING.name(),
                    currentStage
            );
            if (updated <= 0) {
                log.warn("ignore ai task status because task stage update was skipped, taskId={}, stage={}, node={}",
                        message.getTaskId(), currentStage, message.getPayload().getCurrentNode());
                skipReason = "TASK_STAGE_UPDATE_SKIPPED";
                return;
            }

            stringRedisTemplate.delete(RedisKeyBuilder.taskFull(message.getTaskId()));
            stringRedisTemplate.delete(RedisKeyConstants.TASK_STATS_GLOBAL);
            taskCacheVersionManager.bumpVersion();

            String workflowInstanceId = message.getPayload().getWorkflowInstanceId();
            if (workflowInstanceId == null || workflowInstanceId.isBlank()) {
                workflowInstanceId = "wf-" + message.getTaskId();
            }
            taskTraceManager.createWorkflowIfAbsent(workflowInstanceId, message.getTaskId(), task.getTaskType(), message.getPayload().getCurrentNode());
            taskTraceManager.appendStep(
                    message.getTaskId(),
                    currentStage,
                    message.getPayload().getCurrentNode(),
                    message.getPayload().getProgress()
            );
            taskTraceManager.appendAgentExecution(
                    workflowInstanceId,
                    message.getTaskId(),
                    message.getPayload().getCurrentNode(),
                    message.getPayload().getCurrentNode(),
                    message.getPayload().getCurrentNode(),
                    null,
                    false,
                    0L
            );
            taskTraceManager.updateWorkflowProgress(
                    workflowInstanceId,
                    message.getPayload().getCurrentNode()
            );

            ResearchTaskDO latestTask = researchTaskMapper.selectOne(
                    new LambdaQueryWrapper<ResearchTaskDO>()
                            .eq(ResearchTaskDO::getTaskId, message.getTaskId())
                            .last("limit 1")
            );
            if (latestTask != null && taskStateManager.isFinalState(latestTask.getStatus())) {
                log.info("skip redis running state refresh because task already finalized, taskId={}, finalStatus={}",
                        message.getTaskId(), latestTask.getStatus());
                skipReason = "TASK_FINALIZED_AFTER_PROGRESS_UPDATE";
                return;
            }

            String stateJson = """
                    {"status":"%s","currentStage":"%s","currentNode":"%s","progress":%d}
                    """.formatted(
                    TaskStatusEnum.RUNNING.name(),
                    currentStage,
                    message.getPayload().getCurrentNode(),
                    message.getPayload().getProgress() == null ? 0 : message.getPayload().getProgress()
            );
            stringRedisTemplate.opsForValue().set(
                    RedisKeyBuilder.taskState(message.getTaskId()),
                    stateJson,
                    Duration.ofHours(24)
            );
        } catch (Exception e) {
            failed = true;
            taskMessageLogService.recordFailed(KafkaTopicConstants.AI_TASK_STATUS, message, SERVICE_NAME, e.getMessage());
            throw e;
        } finally {
            if (!failed) {
                if (skipReason == null) {
                    taskMessageLogService.recordConsumed(KafkaTopicConstants.AI_TASK_STATUS, message, SERVICE_NAME);
                } else {
                    taskMessageLogService.recordSkipped(KafkaTopicConstants.AI_TASK_STATUS, message, SERVICE_NAME, skipReason);
                }
            }
            TraceContext.clear();
        }

    }
}
