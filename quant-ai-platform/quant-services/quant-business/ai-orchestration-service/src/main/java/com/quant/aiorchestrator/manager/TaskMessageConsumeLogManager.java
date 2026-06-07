package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.entity.TaskMessageLogDO;
import com.quant.common.messaging.MessageConsumeStatusConstants;
import com.quant.common.model.message.MessageEnvelope;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class TaskMessageConsumeLogManager {

    private final TaskMessageLogManager taskMessageLogManager;

    public boolean beginConsume(String topicName, MessageEnvelope message, String consumerService) {
        if (message == null || !StringUtils.hasText(message.getMessageId())) {
            return true;
        }
        TaskMessageLogDO existing = taskMessageLogManager.selectConsumerLog(
                topicName,
                message.getMessageId(),
                consumerService
        );
        if (existing != null) {
            return beginExistingConsumerLog(existing, message);
        }
        try {
            insertInboundState(topicName, message, consumerService, MessageConsumeStatusConstants.PROCESSING, null);
            return true;
        } catch (DuplicateKeyException e) {
            return beginDuplicatedConsumerLog(topicName, message, consumerService);
        }
    }

    public void completeInbound(String topicName,
                                MessageEnvelope message,
                                String consumerService,
                                String consumeStatus,
                                String errorMessage) {
        if (message != null
                && StringUtils.hasText(message.getMessageId())
                && StringUtils.hasText(consumerService)
                && taskMessageLogManager.markConsumerLogCompleted(
                topicName,
                message.getMessageId(),
                consumerService,
                consumeStatus,
                errorMessage
        ) > 0) {
            return;
        }
        insertInboundState(topicName, message, consumerService, consumeStatus, errorMessage);
    }

    private boolean beginExistingConsumerLog(TaskMessageLogDO existing, MessageEnvelope message) {
        if (MessageConsumeStatusConstants.FAILED.equals(existing.getConsumeStatus())) {
            return taskMessageLogManager.resetFailedToProcessing(existing, message);
        }
        return false;
    }

    private boolean beginDuplicatedConsumerLog(String topicName, MessageEnvelope message, String consumerService) {
        TaskMessageLogDO duplicated = taskMessageLogManager.selectConsumerLog(
                topicName,
                message.getMessageId(),
                consumerService
        );
        if (duplicated != null && MessageConsumeStatusConstants.FAILED.equals(duplicated.getConsumeStatus())) {
            return taskMessageLogManager.resetFailedToProcessing(duplicated, message);
        }
        return false;
    }

    private void insertInboundState(String topicName,
                                    MessageEnvelope message,
                                    String consumerService,
                                    String consumeStatus,
                                    String errorMessage) {
        taskMessageLogManager.insertEnvelopeState(topicName, message, consumerService, consumeStatus, errorMessage);
    }
}
