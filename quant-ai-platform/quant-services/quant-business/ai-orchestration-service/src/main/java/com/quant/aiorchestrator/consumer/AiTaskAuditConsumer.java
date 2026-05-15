package com.quant.aiorchestrator.consumer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.entity.AuditRecordDO;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.aiorchestrator.mapper.AuditRecordMapper;
import com.quant.aiorchestrator.mapper.ResearchTaskMapper;
import com.quant.aiorchestrator.service.AiTaskInboundMessageSupportService;
import com.quant.aiorchestrator.service.TaskMessageLogService;
import com.quant.common.messaging.KafkaTopicConstants;
import com.quant.common.model.TaskDomainConstants;
import com.quant.common.model.message.AiTaskAuditMessage;
import com.quant.common.redis.RedisKeyConstants;
import com.quant.common.redis.RedisKeyBuilder;
import com.quant.common.web.TraceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiTaskAuditConsumer {

    private static final int MAX_ACTION_DESC_LENGTH = 500;
    private static final int MAX_REMARK_LENGTH = 20000;
    private static final String SERVICE_NAME = "ai-orchestration-service";
    private static final String CONSUMER_GROUP = "ai-orchestration-audit-group";

    private final ObjectMapper objectMapper;
    private final AuditRecordMapper auditRecordMapper;
    private final ResearchTaskMapper researchTaskMapper;
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
                log.warn("ignore ai task audit because retry count mismatched, taskId={}, currentRetryCount={}, messageRetryCount={}",
                        message.getTaskId(), currentRetryCount, messageRetryCount);
                skipReason = "RETRY_COUNT_MISMATCH";
                return;
            }

            AuditRecordDO audit = new AuditRecordDO();
            audit.setAuditId(UUID.randomUUID().toString());
            audit.setTaskId(message.getTaskId());
            audit.setAuditType(TaskDomainConstants.AuditType.AI_TASK_AUDIT.name());
            audit.setAuditStage(TaskDomainConstants.AuditStage.WORKFLOW_FINISHED.name());
            audit.setOperatorType(TaskDomainConstants.AuditOperatorType.AGENT.name());
            audit.setOperatorId("python-ai-engine");
            audit.setActionCode(TaskDomainConstants.AuditActionCode.AUDIT_SUMMARY.name());
            audit.setActionDesc(safeTruncate(message.getPayload().getReviewSuggestion(), MAX_ACTION_DESC_LENGTH));
            audit.setResultStatus(TaskDomainConstants.AuditResultStatus.SUCCESS.name());
            audit.setRemark(buildAuditRemark(message));
            audit.setDeleted(0);

            auditRecordMapper.insert(audit);
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

    private String buildAuditRemark(AiTaskAuditMessage message) throws Exception {
        String json = objectMapper.writeValueAsString(message.getPayload());
        return safeTruncate(json, MAX_REMARK_LENGTH);
    }

    private String safeTruncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
