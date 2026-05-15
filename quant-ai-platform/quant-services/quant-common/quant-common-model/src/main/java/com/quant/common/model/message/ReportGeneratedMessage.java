package com.quant.common.model.message;

import lombok.Data;

import java.math.BigDecimal;

public class ReportGeneratedMessage extends MessageEnvelope {

    private Payload payload;

    public Payload getPayload() {
        return payload;
    }

    public void setPayload(Payload payload) {
        this.payload = payload;
    }

    @Data
    public static class Payload {
        private String reportId;
        private String reportType;
        private String finalStatus;
        private String reviewStatus;
        private Boolean needHumanReview;
        private Integer evidenceCount;
        private BigDecimal confidenceScore;
        private String resultRef;
        private String sourceTaskId;
        private String sourceReportId;
        private String sourceEventId;
        private String sourceDomain;
    }
}
