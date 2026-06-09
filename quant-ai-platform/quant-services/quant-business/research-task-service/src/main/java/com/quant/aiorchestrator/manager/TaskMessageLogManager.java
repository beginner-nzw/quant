package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.entity.TaskMessageLogDO;
import com.quant.aiorchestrator.mapper.TaskMessageLogMapper;
import com.quant.common.model.message.AiTaskActorProvenance;
import com.quant.common.model.message.AiTaskActorProvenanceSupport;
import com.quant.common.model.message.MessageEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskMessageLogManager {

    private static final int MAX_ERROR_MESSAGE_LENGTH = 1000;

    private final TaskMessageLogMapper taskMessageLogMapper;

    public TaskMessageLogDO selectConsumerLog(String topicName, String messageId, String consumerService) {
        return taskMessageLogMapper.selectConsumerLog(topicName, messageId, consumerService);
    }

    public boolean resetFailedToProcessing(TaskMessageLogDO existing, MessageEnvelope message) {
        Integer retryCount = message.getRetryCount() == null ? existing.getRetryCount() : message.getRetryCount();
        Long messageTimestamp = message.getTimestamp() == null ? existing.getMessageTimestamp() : message.getTimestamp();
        String traceId = defaultValue(message.getTraceId(), existing.getTraceId());
        String tenantId = defaultValue(message.getTenantId(), existing.getTenantId());
        return taskMessageLogMapper.resetFailedToProcessing(
                existing.getId(),
                retryCount == null ? 0 : retryCount,
                messageTimestamp,
                traceId,
                tenantId
        ) > 0;
    }

    public int markConsumerLogCompleted(String topicName,
                                        String messageId,
                                        String consumerService,
                                        String consumeStatus,
                                        String errorMessage) {
        return taskMessageLogMapper.completeConsumerLog(
                topicName,
                messageId,
                consumerService,
                consumeStatus,
                safeTruncate(errorMessage)
        );
    }

    public void insertLog(String topicName,
                          String messageId,
                          String taskId,
                          String eventId,
                          String messageType,
                          String producerService,
                          String consumerService,
                          String consumeStatus,
                          Integer retryCount,
                          Long messageTimestamp,
                          String traceId,
                          String tenantId,
                          AiTaskActorProvenance provenance,
                          String errorMessage) {
        try {
            String safeMessageId = defaultValue(messageId, "missing-" + UUID.randomUUID());
            TaskMessageLogDO entity = new TaskMessageLogDO();
            entity.setMessageLogId(UUID.randomUUID().toString());
            entity.setMessageId(safeMessageId);
            entity.setTaskId(taskId);
            entity.setEventId(eventId);
            entity.setTopicName(topicName);
            entity.setMessageType(defaultValue(messageType, "UNKNOWN"));
            entity.setProducerService(producerService);
            entity.setConsumerService(consumerService);
            entity.setIdentitySource(AiTaskActorProvenanceSupport.identitySource(provenance));
            entity.setRoleSource(AiTaskActorProvenanceSupport.roleSource(provenance));
            entity.setServicePrincipal(AiTaskActorProvenanceSupport.servicePrincipal(provenance));
            entity.setConsumeStatus(consumeStatus);
            entity.setRetryCount(retryCount == null ? 0 : retryCount);
            entity.setErrorMessage(safeTruncate(errorMessage));
            entity.setRawMessageRef(buildRawMessageRef(topicName, safeMessageId));
            entity.setMessageTimestamp(messageTimestamp);
            entity.setTraceId(traceId);
            entity.setTenantId(defaultValue(tenantId, "default"));
            entity.setDeleted(0);
            taskMessageLogMapper.insert(entity);
        } catch (Exception e) {
            log.warn("record task message log failed, topic={}, taskId={}, messageId={}",
                    topicName, taskId, messageId, e);
        }
    }

    public void insertEnvelopeState(String topicName,
                                    MessageEnvelope message,
                                    String consumerService,
                                    String consumeStatus,
                                    String errorMessage) {
        insertLog(
                topicName,
                message == null ? null : message.getMessageId(),
                message == null ? null : message.getTaskId(),
                message == null ? null : message.getEventId(),
                message == null ? null : message.getMessageType(),
                message == null ? null : message.getSourceService(),
                consumerService,
                consumeStatus,
                message == null ? null : message.getRetryCount(),
                message == null ? null : message.getTimestamp(),
                message == null ? null : message.getTraceId(),
                message == null ? null : message.getTenantId(),
                extractActorProvenance(message),
                errorMessage
        );
    }

    public AiTaskActorProvenance extractActorProvenance(MessageEnvelope message) {
        if (message instanceof com.quant.common.model.message.AiTaskDispatchMessage dispatchMessage
                && dispatchMessage.getPayload() != null) {
            return dispatchMessage.getPayload().getActorProvenance();
        }
        if (message instanceof com.quant.common.model.message.AiTaskStatusMessage statusMessage
                && statusMessage.getPayload() != null) {
            return statusMessage.getPayload().getActorProvenance();
        }
        if (message instanceof com.quant.common.model.message.AiTaskResultMessage resultMessage
                && resultMessage.getPayload() != null) {
            return resultMessage.getPayload().getActorProvenance();
        }
        if (message instanceof com.quant.common.model.message.AiTaskAuditMessage auditMessage
                && auditMessage.getPayload() != null) {
            return auditMessage.getPayload().getActorProvenance();
        }
        return null;
    }

    private String buildRawMessageRef(String topicName, String messageId) {
        return "kafka:" + topicName + ":" + messageId;
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String safeTruncate(String value) {
        if (value == null || value.length() <= MAX_ERROR_MESSAGE_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }
}
