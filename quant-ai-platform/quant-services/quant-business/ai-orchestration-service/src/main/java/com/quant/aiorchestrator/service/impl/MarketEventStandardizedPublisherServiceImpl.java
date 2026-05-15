package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.service.MarketEventStandardizedPublisherService;
import com.quant.aiorchestrator.service.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.entity.MarketEventDO;
import com.quant.common.messaging.KafkaTopicConstants;
import com.quant.common.messaging.MessageTypeConstants;
import com.quant.common.model.message.MarketEventStandardizedMessage;
import com.quant.common.web.TraceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketEventStandardizedPublisherServiceImpl implements MarketEventStandardizedPublisherService {

    private static final String SERVICE_NAME = "ai-orchestration-service";
    private static final String TARGET_SERVICE = "market-event-subscribers";

    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TaskMessageLogService taskMessageLogService;

    public void publish(MarketEventDO event) {
        if (event == null || !StringUtils.hasText(event.getEventId())) {
            return;
        }

        MarketEventStandardizedMessage message = buildMessage(event);
        try {
            String payload = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(KafkaTopicConstants.MARKET_EVENT_STANDARDIZED, event.getEventId(), payload);
            taskMessageLogService.recordProduced(KafkaTopicConstants.MARKET_EVENT_STANDARDIZED, message);
        } catch (Exception e) {
            taskMessageLogService.recordFailed(KafkaTopicConstants.MARKET_EVENT_STANDARDIZED, message, e.getMessage());
            log.warn("publish market event standardized message failed, eventId={}, messageId={}",
                    event.getEventId(), message.getMessageId(), e);
        }
    }

    private MarketEventStandardizedMessage buildMessage(MarketEventDO event) {
        MarketEventStandardizedMessage message = new MarketEventStandardizedMessage();
        message.setMessageId(UUID.randomUUID().toString());
        message.setTraceId(TraceContext.resolveTraceId(TraceContext.currentTraceId()));
        message.setTaskId(null);
        message.setEventId(event.getEventId());
        message.setMessageType(MessageTypeConstants.MARKET_EVENT_STANDARDIZED);
        message.setSourceService(SERVICE_NAME);
        message.setTargetService(TARGET_SERVICE);
        message.setTenantId("default");
        message.setBizKey("MARKET_EVENT:" + event.getEventId());
        message.setTimestamp(System.currentTimeMillis());
        message.setVersion("1.0");
        message.setRetryCount(0);

        MarketEventStandardizedMessage.Payload payload = new MarketEventStandardizedMessage.Payload();
        payload.setEventId(event.getEventId());
        payload.setTargetType(event.getTargetType());
        payload.setTargetCode(event.getTargetCode());
        payload.setTargetName(event.getTargetName());
        payload.setEventType(event.getEventType());
        payload.setEventTitle(event.getEventTitle());
        payload.setEventSummary(event.getEventSummary());
        payload.setSourceChannel(event.getSourceChannel());
        payload.setSourceUrl(event.getSourceUrl());
        payload.setImpactLevel(event.getImpactLevel());
        payload.setEventStatus(event.getEventStatus());
        payload.setOccurredAt(formatDateTime(event.getOccurredAt()));
        payload.setAutoTriggerRuleCode(event.getAutoTriggerRuleCode());
        payload.setAutoTriggerStatus(event.getAutoTriggerStatus());
        payload.setAutoTriggerTaskId(event.getAutoTriggerTaskId());
        payload.setAutoTriggerMessage(event.getAutoTriggerMessage());
        payload.setAutoTriggerAttemptedAt(formatDateTime(event.getAutoTriggerAttemptedAt()));
        payload.setCreatedBy(event.getCreatedBy());
        payload.setCreatedAt(formatDateTime(event.getCreatedAt()));
        message.setPayload(payload);
        return message;
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.toString();
    }
}
