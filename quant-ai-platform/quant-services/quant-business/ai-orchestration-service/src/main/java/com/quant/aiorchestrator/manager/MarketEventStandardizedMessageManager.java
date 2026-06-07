package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.entity.MarketEventDO;
import com.quant.common.messaging.MessageTypeConstants;
import com.quant.common.model.message.MarketEventStandardizedMessage;
import com.quant.common.web.TraceContext;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class MarketEventStandardizedMessageManager {

    private static final String SERVICE_NAME = "ai-orchestration-service";
    private static final String TARGET_SERVICE = "market-event-subscribers";

    public MarketEventStandardizedMessage buildMessage(MarketEventDO event) {
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
        message.setPayload(buildPayload(event));
        return message;
    }

    private MarketEventStandardizedMessage.Payload buildPayload(MarketEventDO event) {
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
        payload.setNormalizedFingerprint(event.getNormalizedFingerprint());
        payload.setProvenanceType(event.getProvenanceType());
        payload.setProvenanceRef(event.getProvenanceRef());
        payload.setProvenanceDetail(event.getProvenanceDetail());
        payload.setConfidenceScore(event.getConfidenceScore() == null ? null : event.getConfidenceScore().toPlainString());
        payload.setImpactLevel(event.getImpactLevel());
        payload.setEventStatus(event.getEventStatus());
        payload.setOccurredAt(formatDateTime(event.getOccurredAt()));
        payload.setAutoTriggerRuleCode(event.getAutoTriggerRuleCode());
        payload.setAutoTriggerStatus(event.getAutoTriggerStatus());
        payload.setAutoTriggerTaskId(event.getAutoTriggerTaskId());
        payload.setAutoTriggerMessage(event.getAutoTriggerMessage());
        payload.setAutoTriggerReason(event.getAutoTriggerReason());
        payload.setAutoTriggerSource(event.getAutoTriggerSource());
        payload.setAutoTriggerFailureCode(event.getAutoTriggerFailureCode());
        payload.setAutoTriggerRetryCount(event.getAutoTriggerRetryCount());
        payload.setAutoTriggerAttemptedAt(formatDateTime(event.getAutoTriggerAttemptedAt()));
        payload.setCreatedBy(event.getCreatedBy());
        payload.setCreatedAt(formatDateTime(event.getCreatedAt()));
        return payload;
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.toString();
    }
}
