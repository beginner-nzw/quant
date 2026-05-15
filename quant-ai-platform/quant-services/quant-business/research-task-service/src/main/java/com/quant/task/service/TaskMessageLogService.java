package com.quant.task.service;

import com.quant.common.messaging.MessageConsumeStatusConstants;
import com.quant.common.model.message.MessageEnvelope;
import com.quant.task.domain.entity.TaskMessageLogDO;
import com.quant.task.domain.entity.TaskOutboxMessageDO;
import com.quant.task.mapper.TaskMessageLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskMessageLogService {

    private static final int MAX_ERROR_MESSAGE_LENGTH = 1000;

    private final TaskMessageLogMapper taskMessageLogMapper;

    public void recordProduced(String topicName, MessageEnvelope message) {
        insertLog(topicName, message, MessageConsumeStatusConstants.PRODUCED, null);
    }

    public void recordFailed(String topicName, MessageEnvelope message, String errorMessage) {
        insertLog(topicName, message, MessageConsumeStatusConstants.FAILED, errorMessage);
    }

    public void recordProduced(TaskOutboxMessageDO outbox) {
        insertLog(outbox, MessageConsumeStatusConstants.PRODUCED, null);
    }

    public void recordFailed(TaskOutboxMessageDO outbox, String errorMessage) {
        insertLog(outbox, MessageConsumeStatusConstants.FAILED, errorMessage);
    }

    private void insertLog(String topicName,
                           MessageEnvelope message,
                           String consumeStatus,
                           String errorMessage) {
        if (message == null) {
            return;
        }
        try {
            TaskMessageLogDO entity = new TaskMessageLogDO();
            entity.setMessageLogId(UUID.randomUUID().toString());
            entity.setMessageId(defaultValue(message.getMessageId(), "missing-" + UUID.randomUUID()));
            entity.setTaskId(message.getTaskId());
            entity.setEventId(message.getEventId());
            entity.setTopicName(topicName);
            entity.setMessageType(defaultValue(message.getMessageType(), "UNKNOWN"));
            entity.setProducerService(defaultValue(message.getSourceService(), "research-task-service"));
            entity.setConsumerService(message.getTargetService());
            entity.setConsumeStatus(consumeStatus);
            entity.setRetryCount(message.getRetryCount() == null ? 0 : message.getRetryCount());
            entity.setErrorMessage(safeTruncate(errorMessage));
            entity.setRawMessageRef(buildRawMessageRef(topicName, entity.getMessageId()));
            entity.setMessageTimestamp(message.getTimestamp());
            entity.setTraceId(message.getTraceId());
            entity.setTenantId(defaultValue(message.getTenantId(), "default"));
            entity.setDeleted(0);
            taskMessageLogMapper.insert(entity);
        } catch (Exception e) {
            log.warn("record task message log failed, topic={}, taskId={}, messageId={}",
                    topicName, message.getTaskId(), message.getMessageId(), e);
        }
    }

    private void insertLog(TaskOutboxMessageDO outbox,
                           String consumeStatus,
                           String errorMessage) {
        if (outbox == null) {
            return;
        }
        try {
            TaskMessageLogDO entity = new TaskMessageLogDO();
            entity.setMessageLogId(UUID.randomUUID().toString());
            entity.setMessageId(defaultValue(outbox.getMessageId(), "missing-" + UUID.randomUUID()));
            entity.setTaskId(outbox.getTaskId());
            entity.setEventId(outbox.getEventId());
            entity.setTopicName(outbox.getTopicName());
            entity.setMessageType(defaultValue(outbox.getMessageType(), "UNKNOWN"));
            entity.setProducerService(defaultValue(outbox.getProducerService(), "research-task-service"));
            entity.setConsumerService(outbox.getTargetService());
            entity.setConsumeStatus(consumeStatus);
            entity.setRetryCount(outbox.getRetryCount() == null ? 0 : outbox.getRetryCount());
            entity.setErrorMessage(safeTruncate(errorMessage));
            entity.setRawMessageRef(buildRawMessageRef(outbox.getTopicName(), entity.getMessageId()));
            entity.setMessageTimestamp(outbox.getMessageTimestamp());
            entity.setTraceId(outbox.getTraceId());
            entity.setTenantId(defaultValue(outbox.getTenantId(), "default"));
            entity.setDeleted(0);
            taskMessageLogMapper.upsertOutboxLog(entity);
        } catch (Exception e) {
            log.warn("record task outbox message log failed, topic={}, taskId={}, messageId={}",
                    outbox.getTopicName(), outbox.getTaskId(), outbox.getMessageId(), e);
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
