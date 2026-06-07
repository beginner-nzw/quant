package com.quant.aiorchestrator.service;

import com.quant.common.model.message.MessageEnvelope;

public interface TaskMessageLogService {
    boolean beginConsume(String topicName, MessageEnvelope message, String consumerService);

    void recordProduced(String topicName, MessageEnvelope message);

    void recordFailed(String topicName, MessageEnvelope message, String errorMessage);

    void recordConsumed(String topicName, MessageEnvelope message, String consumerService);

    void recordSkipped(String topicName, MessageEnvelope message, String consumerService, String reason);

    void recordFailed(String topicName, MessageEnvelope message, String consumerService, String errorMessage);
}
