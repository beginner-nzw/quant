package com.quant.aiorchestrator.service;

import com.quant.common.model.message.MessageEnvelope;

public interface AiTaskDeadLetterPublisherService {
    void publishInvalidMessage(String sourceTopic,
                               String consumerGroup,
                               String consumerService,
                               MessageEnvelope originalMessage,
                               String rawMessage,
                               String errorCode,
                               String errorMessage);
}
