package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MarketEventBatchPreviewItemVO {

    private Integer itemNo;
    private Boolean valid;
    private Boolean importable;
    private Boolean duplicate;
    private String invalidField;
    private String duplicateSource;
    private String existingEventId;
    private String targetCode;
    private String targetName;
    private String eventTitle;
    private String normalizedTargetCode;
    private String normalizedEventType;
    private String normalizedImpactLevel;
    private String normalizedEventStatus;
    private String normalizedSourceChannel;
    private String normalizedFingerprint;
    private String provenanceType;
    private BigDecimal confidenceScore;
    private String autoTriggerStatus;
    private String autoTriggerRuleCode;
    private String estimatedTaskType;
    private String message;
}
