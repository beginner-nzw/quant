package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.manager.TaskMessageConsumeLogManager;
import com.quant.aiorchestrator.manager.TaskMessageLogManager;
import com.quant.aiorchestrator.service.TaskMessageLogService;
import com.quant.common.messaging.MessageConsumeStatusConstants;
import com.quant.common.model.message.MessageEnvelope;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TaskMessageLogServiceImpl implements TaskMessageLogService {

    private final TaskMessageLogManager taskMessageLogManager;
    private final TaskMessageConsumeLogManager taskMessageConsumeLogManager;

    public boolean beginConsume(String topicName, MessageEnvelope message, String consumerService) {
        return taskMessageConsumeLogManager.beginConsume(topicName, message, consumerService);
    }

    public void recordProduced(String topicName, MessageEnvelope message) {
        taskMessageLogManager.insertEnvelopeState(topicName, message, null, MessageConsumeStatusConstants.PRODUCED, null);
    }

    public void recordFailed(String topicName, MessageEnvelope message, String errorMessage) {
        taskMessageLogManager.insertEnvelopeState(topicName, message, null, MessageConsumeStatusConstants.FAILED, errorMessage);
    }

    public void recordConsumed(String topicName, MessageEnvelope message, String consumerService) {
        taskMessageConsumeLogManager.completeInbound(topicName, message, consumerService, MessageConsumeStatusConstants.SUCCESS, null);
    }

    public void recordSkipped(String topicName, MessageEnvelope message, String consumerService, String reason) {
        taskMessageConsumeLogManager.completeInbound(topicName, message, consumerService, MessageConsumeStatusConstants.SUCCESS, reason);
    }

    public void recordFailed(String topicName, MessageEnvelope message, String consumerService, String errorMessage) {
        taskMessageConsumeLogManager.completeInbound(topicName, message, consumerService, MessageConsumeStatusConstants.FAILED, errorMessage);
    }
}
