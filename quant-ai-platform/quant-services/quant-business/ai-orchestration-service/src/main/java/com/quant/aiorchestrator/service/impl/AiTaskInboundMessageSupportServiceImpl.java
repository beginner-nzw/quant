package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.manager.AiTaskInboundMessageManager;
import com.quant.aiorchestrator.service.AiTaskInboundMessageSupportService;
import com.quant.common.model.message.MessageEnvelope;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiTaskInboundMessageSupportServiceImpl implements AiTaskInboundMessageSupportService {

    private final AiTaskInboundMessageManager inboundMessageManager;

    public <T extends MessageEnvelope> T parseOrNull(String rawMessage,
                                                     Class<T> messageClass,
                                                     String sourceTopic,
                                                     String consumerGroup,
                                                     String consumerService) {
        return inboundMessageManager.parseOrNull(rawMessage, messageClass, sourceTopic, consumerGroup, consumerService);
    }

    public boolean rejectIfInvalidEnvelope(MessageEnvelope message,
                                           boolean payloadPresent,
                                           String rawMessage,
                                           String sourceTopic,
                                           String consumerGroup,
                                           String consumerService) {
        return inboundMessageManager.rejectIfInvalidEnvelope(
                message,
                payloadPresent,
                rawMessage,
                sourceTopic,
                consumerGroup,
                consumerService
        );
    }

    public void rejectInvalidMessage(MessageEnvelope message,
                                     String rawMessage,
                                     String sourceTopic,
                                     String consumerGroup,
                                     String consumerService,
                                     String errorCode,
                                     String errorMessage) {
        inboundMessageManager.rejectInvalidMessage(
                message,
                rawMessage,
                sourceTopic,
                consumerGroup,
                consumerService,
                errorCode,
                errorMessage
        );
    }
}
