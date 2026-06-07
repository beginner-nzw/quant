package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

@Data
public class MarketEventCreateResultVO {

    private String eventId;
    private Boolean duplicate;
    private String normalizedFingerprint;
    private String autoTriggerStatus;
    private String autoTriggerTaskId;
    private String autoTriggerMessage;
    private String autoTriggerReason;
    private String autoTriggerFailureCode;
    private String message;
}
