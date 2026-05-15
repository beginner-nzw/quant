package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

@Data
public class MarketEventIngestHistoryItemVO {

    private String historyId;
    private String sourceType;
    private String sourceLabel;
    private String sourceCode;
    private String sourceName;
    private String sourceCategory;
    private String sourceChannel;
    private String sourceDetail;
    private Integer totalCount;
    private Integer successCount;
    private Integer failedCount;
    private Integer duplicateCount;
    private Integer autoTriggeredCount;
    private String resultStatus;
    private String errorMessage;
    private String operatorId;
    private String operatorRole;
    private String summary;
    private String createdAt;
}
