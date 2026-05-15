package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.entity.TaskMessageLogDO;
import com.quant.aiorchestrator.mapper.TaskMessageLogMapper;
import com.quant.common.messaging.MessageConsumeStatusConstants;
import com.quant.common.model.message.MessageEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskMessageLogService {

    private static final int MAX_ERROR_MESSAGE_LENGTH = 1000;

    private final TaskMessageLogMapper taskMessageLogMapper;

    public boolean beginConsume(String topicName, MessageEnvelope message, String consumerService) {
        if (message == null || !StringUtils.hasText(message.getMessageId())) {
            return true;
        }
        TaskMessageLogDO existing = selectConsumerLog(topicName, message.getMessageId(), consumerService);
        if (existing != null) {
            if (MessageConsumeStatusConstants.FAILED.equals(existing.getConsumeStatus())) {
                return resetFailedToProcessing(existing, message);
            }
            return false;
        }
        try {
            insertInboundState(topicName, message, consumerService, MessageConsumeStatusConstants.PROCESSING, null);
            return true;
        } catch (DuplicateKeyException e) {
            TaskMessageLogDO duplicated = selectConsumerLog(topicName, message.getMessageId(), consumerService);
            if (duplicated != null && MessageConsumeStatusConstants.FAILED.equals(duplicated.getConsumeStatus())) {
                return resetFailedToProcessing(duplicated, message);
            }
            return false;
        }
    }

    public void recordProduced(String topicName, MessageEnvelope message) {
        insertLog(
                topicName,
                message == null ? null : message.getMessageId(),
                message == null ? null : message.getTaskId(),
                message == null ? null : message.getEventId(),
                message == null ? null : message.getMessageType(),
                message == null ? null : message.getSourceService(),
                message == null ? null : message.getTargetService(),
                MessageConsumeStatusConstants.PRODUCED,
                message == null ? null : message.getRetryCount(),
                message == null ? null : message.getTimestamp(),
                message == null ? null : message.getTraceId(),
                message == null ? null : message.getTenantId(),
                null
        );
    }

    public void recordFailed(String topicName, MessageEnvelope message, String errorMessage) {
        insertLog(
                topicName,
                message == null ? null : message.getMessageId(),
                message == null ? null : message.getTaskId(),
                message == null ? null : message.getEventId(),
                message == null ? null : message.getMessageType(),
                message == null ? null : message.getSourceService(),
                message == null ? null : message.getTargetService(),
                MessageConsumeStatusConstants.FAILED,
                message == null ? null : message.getRetryCount(),
                message == null ? null : message.getTimestamp(),
                message == null ? null : message.getTraceId(),
                message == null ? null : message.getTenantId(),
                errorMessage
        );
    }

    public void recordConsumed(String topicName, MessageEnvelope message, String consumerService) {
        completeInbound(topicName, message, consumerService, MessageConsumeStatusConstants.SUCCESS, null);
    }

    public void recordSkipped(String topicName, MessageEnvelope message, String consumerService, String reason) {
        completeInbound(topicName, message, consumerService, MessageConsumeStatusConstants.SUCCESS, reason);
    }

    public void recordFailed(String topicName, MessageEnvelope message, String consumerService, String errorMessage) {
        completeInbound(topicName, message, consumerService, MessageConsumeStatusConstants.FAILED, errorMessage);
    }

    private void completeInbound(String topicName,
                                 MessageEnvelope message,
                                 String consumerService,
                                 String consumeStatus,
                                 String errorMessage) {
        if (message != null
                && StringUtils.hasText(message.getMessageId())
                && StringUtils.hasText(consumerService)
                && markConsumerLogCompleted(topicName, message.getMessageId(), consumerService, consumeStatus, errorMessage) > 0) {
            return;
        }
        insertInboundState(topicName, message, consumerService, consumeStatus, errorMessage);
    }

    private void insertInboundState(String topicName,
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
                errorMessage
        );
    }

    private TaskMessageLogDO selectConsumerLog(String topicName, String messageId, String consumerService) {
        return taskMessageLogMapper.selectConsumerLog(topicName, messageId, consumerService);
    }

    private boolean resetFailedToProcessing(TaskMessageLogDO existing, MessageEnvelope message) {
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

    private int markConsumerLogCompleted(String topicName,
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

    private void insertLog(String topicName,
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
