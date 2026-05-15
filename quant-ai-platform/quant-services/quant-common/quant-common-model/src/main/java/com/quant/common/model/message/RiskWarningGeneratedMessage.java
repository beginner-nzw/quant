package com.quant.common.model.message;

import lombok.Data;

import java.math.BigDecimal;

public class RiskWarningGeneratedMessage extends MessageEnvelope {

    private Payload payload;

    public Payload getPayload() {
        return payload;
    }

    public void setPayload(Payload payload) {
        this.payload = payload;
    }

    @Data
    public static class Payload {
        private String warningId;
        private String warningType;
        private String warningLevel;
        private String entityType;
        private String entityCode;
        private String entityName;
        private String triggerSource;
        private String triggerEventId;
        private String warningSummary;
        private String warningStatus;
        private String reviewStatus;
        private Integer detailCount;
        private BigDecimal confidenceScore;
    }
}
