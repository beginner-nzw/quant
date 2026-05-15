package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.service.AiTaskDeadLetterPublisherService;
import com.quant.aiorchestrator.service.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.common.messaging.KafkaTopicConstants;
import com.quant.common.messaging.MessageTypeConstants;
import com.quant.common.model.message.AiTaskDeadLetterMessage;
import com.quant.common.model.message.MessageEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiTaskDeadLetterPublisherServiceImpl implements AiTaskDeadLetterPublisherService {

    private static final int MAX_ERROR_MESSAGE_LENGTH = 1000;
    private static final int MAX_RAW_MESSAGE_LENGTH = 20000;
    private static final String TARGET_SERVICE = "deadletter-queue";

    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TaskMessageLogService taskMessageLogService;

    public void publishInvalidMessage(String sourceTopic,
                                      String consumerGroup,
                                      String consumerService,
                                      MessageEnvelope originalMessage,
                                      String rawMessage,
                                      String errorCode,
                                      String errorMessage) {
        AiTaskDeadLetterMessage deadLetterMessage = buildDeadLetterMessage(
                sourceTopic,
                consumerGroup,
                consumerService,
                originalMessage,
                rawMessage,
                errorCode,
                errorMessage
        );
        try {
            String messageJson = objectMapper.writeValueAsString(deadLetterMessage);
            kafkaTemplate.send(
                    KafkaTopicConstants.AI_TASK_DEADLETTER,
                    resolveKey(deadLetterMessage),
                    messageJson
            );
            taskMessageLogService.recordProduced(KafkaTopicConstants.AI_TASK_DEADLETTER, deadLetterMessage);
        } catch (Exception e) {
            taskMessageLogService.recordFailed(KafkaTopicConstants.AI_TASK_DEADLETTER, deadLetterMessage, e.getMessage());
            log.warn("publish ai task deadletter failed, sourceTopic={}, originalMessageId={}, taskId={}",
                    sourceTopic,
                    originalMessage == null ? null : originalMessage.getMessageId(),
                    originalMessage == null ? null : originalMessage.getTaskId(),
                    e);
        }
    }

    private AiTaskDeadLetterMessage buildDeadLetterMessage(String sourceTopic,
                                                           String consumerGroup,
                                                           String consumerService,
                                                           MessageEnvelope originalMessage,
                                                           String rawMessage,
                                                           String errorCode,
                                                           String errorMessage) {
        AiTaskDeadLetterMessage message = new AiTaskDeadLetterMessage();
        message.setMessageId(UUID.randomUUID().toString());
        message.setTraceId(originalMessage == null ? null : originalMessage.getTraceId());
        message.setTaskId(originalMessage == null ? null : originalMessage.getTaskId());
        message.setEventId(originalMessage == null ? null : originalMessage.getEventId());
        message.setMessageType(MessageTypeConstants.AI_TASK_DEADLETTER);
        message.setSourceService(defaultValue(consumerService, "ai-orchestration-service"));
        message.setTargetService(TARGET_SERVICE);
        message.setTenantId(defaultValue(originalMessage == null ? null : originalMessage.getTenantId(), "default"));
        message.setBizKey(defaultValue(originalMessage == null ? null : originalMessage.getBizKey(), sourceTopic));
        message.setTimestamp(System.currentTimeMillis());
        message.setVersion(defaultValue(originalMessage == null ? null : originalMessage.getVersion(), "1.0"));
        message.setRetryCount(originalMessage == null || originalMessage.getRetryCount() == null ? 0 : originalMessage.getRetryCount());

        AiTaskDeadLetterMessage.Payload payload = new AiTaskDeadLetterMessage.Payload();
        payload.setOriginalTopic(sourceTopic);
        payload.setOriginalMessageType(defaultValue(originalMessage == null ? null : originalMessage.getMessageType(), "UNKNOWN"));
        payload.setOriginalProducerService(originalMessage == null ? null : originalMessage.getSourceService());
        payload.setConsumerService(consumerService);
        payload.setConsumerGroup(consumerGroup);
        payload.setFailureStage("CONSUME");
        payload.setErrorCode(defaultValue(errorCode, "INVALID_MESSAGE"));
        payload.setErrorMessage(safeTruncate(errorMessage, MAX_ERROR_MESSAGE_LENGTH));
        payload.setRawMessage(safeTruncate(rawMessage, MAX_RAW_MESSAGE_LENGTH));
        message.setPayload(payload);
        return message;
    }

    private String resolveKey(AiTaskDeadLetterMessage message) {
        if (message == null) {
            return UUID.randomUUID().toString();
        }
        if (StringUtils.hasText(message.getTaskId())) {
            return message.getTaskId();
        }
        if (StringUtils.hasText(message.getMessageId())) {
            return message.getMessageId();
        }
        return UUID.randomUUID().toString();
    }

    private String defaultValue(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String safeTruncate(String value, int maxLength) {
        if (!StringUtils.hasText(value) || maxLength <= 0 || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
