package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

@Data
public class EventSourcePreviewItemVO {

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
}
