package com.quant.aiorchestrator.consumer;

import com.quant.aiorchestrator.domain.vo.TaskDetailVO;
import com.quant.aiorchestrator.manager.AiTaskAuditRecordWriteManager;
import com.quant.aiorchestrator.service.AiTaskInboundMessageSupportService;
import com.quant.aiorchestrator.service.TaskMessageLogService;
import com.quant.aiorchestrator.service.TaskQueryService;
import com.quant.common.messaging.KafkaTopicConstants;
import com.quant.common.model.message.AiTaskAuditMessage;
import com.quant.common.redis.RedisKeyConstants;
import com.quant.common.redis.RedisKeyBuilder;
import com.quant.common.web.TraceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiTaskAuditConsumer {

    private static final String SERVICE_NAME = "audit-service";
    private static final String CONSUMER_GROUP = "audit-service-ai-task-audit-group";

    private final TaskQueryService taskQueryService;
    private final AiTaskAuditRecordWriteManager aiTaskAuditRecordWriteManager;
    private final StringRedisTemplate stringRedisTemplate;
    private final TaskMessageLogService taskMessageLogService;
    private final AiTaskInboundMessageSupportService inboundMessageSupportService;

    @KafkaListener(topics = KafkaTopicConstants.AI_TASK_AUDIT, groupId = CONSUMER_GROUP)
    public void onMessage(String rawMessage) throws Exception {
        AiTaskAuditMessage message = inboundMessageSupportService.parseOrNull(
                rawMessage,
                AiTaskAuditMessage.class,
                KafkaTopicConstants.AI_TASK_AUDIT,
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
                KafkaTopicConstants.AI_TASK_AUDIT,
                CONSUMER_GROUP,
                SERVICE_NAME
        )) {
            return;
        }
        log.info("consume ai task audit, taskId={}, workflowInstanceId={}",
                message.getTaskId(),
                message.getPayload().getWorkflowInstanceId());
        TraceContext.bind(message.getTraceId());
        String skipReason = null;
        boolean failed = false;
        try {
            if (!taskMessageLogService.beginConsume(KafkaTopicConstants.AI_TASK_AUDIT, message, SERVICE_NAME)) {
                skipReason = "DUPLICATE_MESSAGE";
                return;
            }

            TaskDetailVO task = taskQueryService.getTaskDetail(message.getTaskId());
            if (task == null) {
                skipReason = "TASK_NOT_FOUND";
                return;
            }

            int currentRetryCount = task.getRetryCount() == null ? 0 : task.getRetryCount();
            int messageRetryCount = message.getRetryCount() == null ? 0 : message.getRetryCount();
            if (messageRetryCount != currentRetryCount) {
                log.warn("ignore ai task audit because retry count mismatched, taskId={}, currentRetryCount={}, messageRetryCount={}",
                        message.getTaskId(), currentRetryCount, messageRetryCount);
                skipReason = "RETRY_COUNT_MISMATCH";
                return;
            }

            aiTaskAuditRecordWriteManager.recordAiTaskAudit(message);
            stringRedisTemplate.delete(RedisKeyBuilder.taskFull(message.getTaskId()));
            stringRedisTemplate.delete(RedisKeyConstants.TASK_STATS_GLOBAL);
        } catch (Exception e) {
            failed = true;
            taskMessageLogService.recordFailed(KafkaTopicConstants.AI_TASK_AUDIT, message, SERVICE_NAME, e.getMessage());
            throw e;
        } finally {
            if (!failed) {
                if (skipReason == null) {
                    taskMessageLogService.recordConsumed(KafkaTopicConstants.AI_TASK_AUDIT, message, SERVICE_NAME);
                } else {
                    taskMessageLogService.recordSkipped(KafkaTopicConstants.AI_TASK_AUDIT, message, SERVICE_NAME, skipReason);
                }
            }
            TraceContext.clear();
        }
    }

}
