package com.quant.common.model.message;

import lombok.Data;

public class MarketEventStandardizedMessage extends MessageEnvelope {

    private Payload payload;

    public Payload getPayload() {
        return payload;
    }

    public void setPayload(Payload payload) {
        this.payload = payload;
    }

    @Data
    public static class Payload {
        private String eventId;
        private String targetType;
        private String targetCode;
        private String targetName;
        private String eventType;
        private String eventTitle;
        private String eventSummary;
        private String sourceChannel;
        private String sourceUrl;
        private String impactLevel;
        private String eventStatus;
        private String occurredAt;
        private String autoTriggerRuleCode;
        private String autoTriggerStatus;
        private String autoTriggerTaskId;
        private String autoTriggerMessage;
        private String autoTriggerAttemptedAt;
        private String createdBy;
        private String createdAt;
    }
}
