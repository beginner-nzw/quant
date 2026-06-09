package com.quant.aiorchestrator.service;

import com.quant.common.model.message.MessageEnvelope;

public interface AiTaskInboundMessageSupportService {

    <T extends MessageEnvelope> T parseOrNull(String rawMessage,
                                              Class<T> messageClass,
                                              String sourceTopic,
                                              String consumerGroup,
                                              String consumerService);

    boolean rejectIfInvalidEnvelope(MessageEnvelope message,
                                    boolean payloadPresent,
                                    String rawMessage,
                                    String sourceTopic,
                                    String consumerGroup,
                                    String consumerService);

    void rejectInvalidMessage(MessageEnvelope message,
                              String rawMessage,
                              String sourceTopic,
                              String consumerGroup,
                              String consumerService,
                              String errorCode,
                              String errorMessage);
}
