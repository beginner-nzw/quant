package com.quant.common.model.message;

import lombok.Data;

import java.math.BigDecimal;

public class StrategySignalGeneratedMessage extends MessageEnvelope {

    private Payload payload;

    public Payload getPayload() {
        return payload;
    }

    public void setPayload(Payload payload) {
        this.payload = payload;
    }

    @Data
    public static class Payload {
        private String signalId;
        private String signalType;
        private String entityCode;
        private String entityName;
        private String signalDate;
        private Integer signalScore;
        private String signalLevel;
        private String signalDirection;
        private String signalStatus;
        private String sourceEventId;
        private Integer factorCount;
        private BigDecimal confidenceScore;
    }
}
