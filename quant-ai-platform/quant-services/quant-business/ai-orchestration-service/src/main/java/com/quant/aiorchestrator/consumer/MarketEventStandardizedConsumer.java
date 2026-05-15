package com.quant.aiorchestrator.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.entity.MarketEventDO;
import com.quant.aiorchestrator.service.MarketEventAutoTriggerService;
import com.quant.aiorchestrator.service.TaskMessageLogService;
import com.quant.common.messaging.KafkaTopicConstants;
import com.quant.common.model.message.MarketEventStandardizedMessage;
import com.quant.common.model.message.SimpleMessageEnvelope;
import com.quant.common.web.TraceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketEventStandardizedConsumer {

    private static final String SERVICE_NAME = "ai-orchestration-service";
    private static final String CONSUMER_GROUP = "market-event-auto-trigger-group";

    private final ObjectMapper objectMapper;
    private final MarketEventAutoTriggerService marketEventAutoTriggerService;
    private final TaskMessageLogService taskMessageLogService;

    @KafkaListener(topics = KafkaTopicConstants.MARKET_EVENT_STANDARDIZED, groupId = CONSUMER_GROUP)
    public void onMessage(String rawMessage) throws Exception {
        MarketEventStandardizedMessage message = parse(rawMessage);
        if (message == null) {
            recordInvalid(rawMessage, "INVALID_JSON", "message parse failed");
            return;
        }
        if (!isValid(message)) {
            taskMessageLogService.recordFailed(
                    KafkaTopicConstants.MARKET_EVENT_STANDARDIZED,
                    message,
                    SERVICE_NAME,
                    "INVALID_MESSAGE: messageId/eventId/payload is missing"
            );
            return;
        }

        TraceContext.bind(message.getTraceId());
        String skipReason = null;
        boolean failed = false;
        try {
            if (!taskMessageLogService.beginConsume(KafkaTopicConstants.MARKET_EVENT_STANDARDIZED, message, SERVICE_NAME)) {
                skipReason = "DUPLICATE_MESSAGE";
                return;
            }

            MarketEventDO event = marketEventAutoTriggerService.loadEvent(message.getEventId());
            if (event == null) {
                skipReason = "EVENT_NOT_FOUND";
                return;
            }
            if (!marketEventAutoTriggerService.isPendingAutoTrigger(event)) {
                skipReason = "AUTO_TRIGGER_NOT_PENDING";
                return;
            }

            marketEventAutoTriggerService.executePendingAutoTrigger(event);
        } catch (Exception e) {
            failed = true;
            taskMessageLogService.recordFailed(KafkaTopicConstants.MARKET_EVENT_STANDARDIZED, message, SERVICE_NAME, e.getMessage());
            throw e;
        } finally {
            if (!failed) {
                if (skipReason == null) {
                    taskMessageLogService.recordConsumed(KafkaTopicConstants.MARKET_EVENT_STANDARDIZED, message, SERVICE_NAME);
                } else {
                    taskMessageLogService.recordSkipped(KafkaTopicConstants.MARKET_EVENT_STANDARDIZED, message, SERVICE_NAME, skipReason);
                }
            }
            TraceContext.clear();
        }
    }

    private MarketEventStandardizedMessage parse(String rawMessage) {
        try {
            return objectMapper.readValue(rawMessage, MarketEventStandardizedMessage.class);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isValid(MarketEventStandardizedMessage message) {
        return message != null
                && StringUtils.hasText(message.getMessageId())
                && StringUtils.hasText(message.getEventId())
                && message.getPayload() != null;
    }

    private void recordInvalid(String rawMessage, String errorCode, String errorMessage) {
        SimpleMessageEnvelope envelope = new SimpleMessageEnvelope();
        envelope.setMessageId("invalid-" + UUID.randomUUID());
        envelope.setEventId(extractEventId(rawMessage));
        envelope.setMessageType("UNKNOWN");
        envelope.setSourceService("unknown-producer");
        envelope.setTargetService(SERVICE_NAME);
        envelope.setTenantId("default");
        envelope.setTraceId(null);
        envelope.setVersion("1.0");
        envelope.setRetryCount(0);
        envelope.setTimestamp(System.currentTimeMillis());
        taskMessageLogService.recordFailed(
                KafkaTopicConstants.MARKET_EVENT_STANDARDIZED,
                envelope,
                SERVICE_NAME,
                errorCode + ": " + errorMessage
        );
    }

    private String extractEventId(String rawMessage) {
        if (!StringUtils.hasText(rawMessage)) {
            return null;
        }
        int fieldIndex = rawMessage.indexOf("\"eventId\"");
        if (fieldIndex < 0) {
            return null;
        }
        int colonIndex = rawMessage.indexOf(':', fieldIndex);
        int quoteStart = rawMessage.indexOf('"', colonIndex + 1);
        int quoteEnd = quoteStart < 0 ? -1 : rawMessage.indexOf('"', quoteStart + 1);
        if (quoteStart < 0 || quoteEnd < 0 || quoteEnd <= quoteStart) {
            return null;
        }
        String value = rawMessage.substring(quoteStart + 1, quoteEnd).trim();
        return value.isEmpty() ? null : value;
    }
}
