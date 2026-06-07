package com.quant.aiorchestrator.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.service.AiTaskDeadLetterPublisherService;
import com.quant.aiorchestrator.service.TaskMessageLogService;
import com.quant.common.model.message.MessageEnvelope;
import com.quant.common.model.message.SimpleMessageEnvelope;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class AiTaskInboundMessageManager {

    private final ObjectMapper objectMapper;
    private final TaskMessageLogService taskMessageLogService;
    private final AiTaskDeadLetterPublisherService deadLetterPublisherService;
    private final InboundMessageEnvelopeManager envelopeManager;

    public <T extends MessageEnvelope> T parseOrNull(String rawMessage,
                                                     Class<T> messageClass,
                                                     String sourceTopic,
                                                     String consumerGroup,
                                                     String consumerService) {
        try {
            return objectMapper.readValue(rawMessage, messageClass);
        } catch (Exception e) {
            rejectInvalidMessage(
                    null,
                    rawMessage,
                    sourceTopic,
                    consumerGroup,
                    consumerService,
                    "INVALID_JSON",
                    e.getMessage()
            );
            return null;
        }
    }

    public boolean rejectIfInvalidEnvelope(MessageEnvelope message,
                                           boolean payloadPresent,
                                           String rawMessage,
                                           String sourceTopic,
                                           String consumerGroup,
                                           String consumerService) {
        if (message == null) {
            rejectInvalidMessage(
                    null,
                    rawMessage,
                    sourceTopic,
                    consumerGroup,
                    consumerService,
                    "MESSAGE_EMPTY",
                    "message is null after deserialization"
            );
            return true;
        }
        if (!StringUtils.hasText(message.getMessageId())) {
            rejectInvalidMessage(
                    message,
                    rawMessage,
                    sourceTopic,
                    consumerGroup,
                    consumerService,
                    "MESSAGE_ID_MISSING",
                    "messageId is blank"
            );
            return true;
        }
        if (!StringUtils.hasText(message.getTaskId())) {
            rejectInvalidMessage(
                    message,
                    rawMessage,
                    sourceTopic,
                    consumerGroup,
                    consumerService,
                    "TASK_ID_MISSING",
                    "taskId is blank"
            );
            return true;
        }
        if (!payloadPresent) {
            rejectInvalidMessage(
                    message,
                    rawMessage,
                    sourceTopic,
                    consumerGroup,
                    consumerService,
                    "PAYLOAD_MISSING",
                    "payload is missing"
            );
            return true;
        }
        return false;
    }

    public void rejectInvalidMessage(MessageEnvelope message,
                                     String rawMessage,
                                     String sourceTopic,
                                     String consumerGroup,
                                     String consumerService,
                                     String errorCode,
                                     String errorMessage) {
        SimpleMessageEnvelope metadata = envelopeManager.buildLogEnvelope(message, rawMessage, consumerService);
        taskMessageLogService.recordFailed(
                sourceTopic,
                metadata,
                consumerService,
                envelopeManager.formatError(errorCode, errorMessage)
        );
        deadLetterPublisherService.publishInvalidMessage(
                sourceTopic,
                consumerGroup,
                consumerService,
                metadata,
                rawMessage,
                errorCode,
                errorMessage
        );
    }
}
