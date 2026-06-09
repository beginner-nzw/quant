package com.quant.aiorchestrator.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.dto.ResearchReportSnapshot;
import com.quant.aiorchestrator.manager.TaskTraceManager;
import com.quant.aiorchestrator.service.AiResultDomainProjectionService;
import com.quant.aiorchestrator.service.AiResultReportService;
import com.quant.aiorchestrator.service.AiTaskInboundMessageSupportService;
import com.quant.aiorchestrator.service.TaskDomainEventPublisherService;
import com.quant.aiorchestrator.service.TaskMessageLogService;
import com.quant.common.messaging.KafkaTopicConstants;
import com.quant.common.model.enums.TaskStageEnum;
import com.quant.common.model.enums.TaskStatusEnum;
import com.quant.common.model.message.AiTaskResultMessage;
import com.quant.common.redis.RedisKeyConstants;
import com.quant.common.redis.RedisKeyBuilder;
import com.quant.common.web.TraceContext;
import com.quant.task.api.AiTaskStateSnapshot;
import com.quant.task.port.AiTaskResultStatePort;
import com.quant.task.port.TaskCacheVersionPort;
import com.quant.task.port.TaskStatePolicy;
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
public class AiTaskResultConsumer {

    private static final String SERVICE_NAME = "ai-orchestration-service";
    private static final String CONSUMER_GROUP = "ai-orchestration-result-group";

    private final ObjectMapper objectMapper;
    private final AiTaskResultStatePort aiTaskResultStateManager;
    private final TaskStatePolicy taskStatePolicy;
    private final TaskTraceManager taskTraceManager;
    private final StringRedisTemplate stringRedisTemplate;
    private final TaskCacheVersionPort taskCacheVersionManager;
    private final AiResultReportService aiResultReportService;
    private final AiResultDomainProjectionService aiResultDomainProjectionService;
    private final TaskDomainEventPublisherService taskDomainEventPublisherService;
    private final TaskMessageLogService taskMessageLogService;
    private final AiTaskInboundMessageSupportService inboundMessageSupportService;

    @KafkaListener(topics = KafkaTopicConstants.AI_TASK_RESULT, groupId = CONSUMER_GROUP)
    public void onMessage(String rawMessage) throws Exception {
        AiTaskResultMessage message = inboundMessageSupportService.parseOrNull(
                rawMessage,
                AiTaskResultMessage.class,
                KafkaTopicConstants.AI_TASK_RESULT,
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
                KafkaTopicConstants.AI_TASK_RESULT,
                CONSUMER_GROUP,
                SERVICE_NAME
        )) {
            return;
        }
        if (!StringUtils.hasText(message.getPayload().getFinalStatus())) {
            inboundMessageSupportService.rejectInvalidMessage(
                    message,
                    rawMessage,
                    KafkaTopicConstants.AI_TASK_RESULT,
                    CONSUMER_GROUP,
                    SERVICE_NAME,
                    "FINAL_STATUS_MISSING",
                    "payload.finalStatus is blank"
            );
            return;
        }
        log.info("consume ai task result, taskId={}, finalStatus={}",
                message.getTaskId(),
                message.getPayload().getFinalStatus());
        TraceContext.bind(message.getTraceId());
        String skipReason = null;
        boolean failed = false;
        try {
            if (!taskMessageLogService.beginConsume(KafkaTopicConstants.AI_TASK_RESULT, message, SERVICE_NAME)) {
                skipReason = "DUPLICATE_MESSAGE";
                return;
            }

            AiTaskStateSnapshot task = aiTaskResultStateManager.selectTask(message.getTaskId());
            if (task == null) {
                skipReason = "TASK_NOT_FOUND";
                return;
            }

            int currentRetryCount = task.getRetryCount() == null ? 0 : task.getRetryCount();
            int messageRetryCount = message.getRetryCount() == null ? 0 : message.getRetryCount();
            if (messageRetryCount != currentRetryCount) {
                log.warn("ignore ai task result because retry count mismatched, taskId={}, currentRetryCount={}, messageRetryCount={}",
                        message.getTaskId(), currentRetryCount, messageRetryCount);
                skipReason = "RETRY_COUNT_MISMATCH";
                return;
            }

            String finalStatus = message.getPayload().getFinalStatus();
            String finalStage = resolveFinalStage(message);
            if (!taskStatePolicy.canTransfer(task.getStatus(), finalStatus)) {
                skipReason = "STATUS_TRANSFER_NOT_ALLOWED";
                return;
            }

            String workflowInstanceId = message.getPayload().getWorkflowInstanceId();
            if (workflowInstanceId == null || workflowInstanceId.isBlank()) {
                workflowInstanceId = "wf-" + message.getTaskId();
            }

            if (TaskStatusEnum.FAILED.name().equals(finalStatus)) {
                int updated = aiTaskResultStateManager.updateFinalState(message, task, finalStage);
                if (updated <= 0) {
                    skipReason = "TASK_FINAL_STATE_UPDATE_SKIPPED";
                    return;
                }
                taskTraceManager.finishWorkflow(workflowInstanceId, finalStage, finalStatus);

                stringRedisTemplate.opsForValue().set(
                        RedisKeyBuilder.taskState(message.getTaskId()),
                        """
                        {"status":"%s","currentStage":"%s","progress":100}
                        """.formatted(TaskStatusEnum.FAILED.name(), finalStage),
                        Duration.ofHours(24)
                );
                stringRedisTemplate.delete(RedisKeyBuilder.taskFull(message.getTaskId()));
                stringRedisTemplate.delete(RedisKeyConstants.TASK_STATS_GLOBAL);
                taskCacheVersionManager.bumpVersion();
                aiTaskResultStateManager.updateRetryLogStatus(message,
                        aiTaskResultStateManager.retryStatusForFinalStatus(finalStatus));
                return;
            }

            if (TaskStatusEnum.CANCELLED.name().equals(finalStatus)) {
                int updated = aiTaskResultStateManager.updateFinalState(message, task, finalStage);
                if (updated <= 0) {
                    skipReason = "TASK_FINAL_STATE_UPDATE_SKIPPED";
                    return;
                }
                taskTraceManager.finishWorkflow(workflowInstanceId, finalStage, finalStatus);

                stringRedisTemplate.opsForValue().set(
                        RedisKeyBuilder.taskState(message.getTaskId()),
                        """
                        {"status":"%s","currentStage":"%s","progress":100}
                        """.formatted(TaskStatusEnum.CANCELLED.name(), finalStage),
                        Duration.ofHours(24)
                );
                stringRedisTemplate.delete(RedisKeyBuilder.taskFull(message.getTaskId()));
                stringRedisTemplate.delete(RedisKeyConstants.TASK_STATS_GLOBAL);
                taskCacheVersionManager.bumpVersion();
                aiTaskResultStateManager.updateRetryLogStatus(message,
                        aiTaskResultStateManager.retryStatusForFinalStatus(finalStatus));
                return;
            }

            int updated = aiTaskResultStateManager.updateFinalState(message, task, finalStage);
            if (updated <= 0) {
                skipReason = "TASK_FINAL_STATE_UPDATE_SKIPPED";
                return;
            }

            ResearchReportSnapshot report = aiResultReportService.saveReport(message);
            aiResultDomainProjectionService.project(message, report);
            taskDomainEventPublisherService.publishGeneratedEvents(message, report);

            taskTraceManager.finishWorkflow(workflowInstanceId, finalStage, finalStatus);

            stringRedisTemplate.opsForValue().set(
                    RedisKeyBuilder.taskResult(message.getTaskId()),
                    objectMapper.writeValueAsString(message.getPayload()),
                    Duration.ofHours(12)
            );

            stringRedisTemplate.opsForValue().set(
                    RedisKeyBuilder.taskState(message.getTaskId()),
                    """
                    {"status":"%s","currentStage":"%s","progress":100}
                    """.formatted(finalStatus, finalStage),
                    Duration.ofHours(24)
            );
            stringRedisTemplate.delete(RedisKeyBuilder.taskFull(message.getTaskId()));
            stringRedisTemplate.delete(RedisKeyConstants.TASK_STATS_GLOBAL);
            taskCacheVersionManager.bumpVersion();
            aiTaskResultStateManager.updateRetryLogStatus(message,
                    aiTaskResultStateManager.retryStatusForFinalStatus(finalStatus));
        } catch (Exception e) {
            failed = true;
            taskMessageLogService.recordFailed(KafkaTopicConstants.AI_TASK_RESULT, message, SERVICE_NAME, e.getMessage());
            throw e;
        } finally {
            if (!failed) {
                if (skipReason == null) {
                    taskMessageLogService.recordConsumed(KafkaTopicConstants.AI_TASK_RESULT, message, SERVICE_NAME);
                } else {
                    taskMessageLogService.recordSkipped(KafkaTopicConstants.AI_TASK_RESULT, message, SERVICE_NAME, skipReason);
                }
            }
            TraceContext.clear();
        }
    }

    private String resolveFinalStage(AiTaskResultMessage message) {
        String finalStage = TaskStageEnum.normalize(message.getPayload().getFinalStage());
        if (finalStage != null && !finalStage.isBlank()) {
            return finalStage;
        }
        if (TaskStatusEnum.CANCELLED.name().equals(message.getPayload().getFinalStatus())) {
            return TaskStageEnum.CANCELLED.name();
        }
        if (TaskStatusEnum.FAILED.name().equals(message.getPayload().getFinalStatus())) {
            return TaskStageEnum.FAILED.name();
        }
        return TaskStageEnum.FINISHED.name();
    }

}
