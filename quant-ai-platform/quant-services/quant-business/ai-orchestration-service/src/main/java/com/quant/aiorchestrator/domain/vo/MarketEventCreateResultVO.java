package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

@Data
public class MarketEventCreateResultVO {

    private String eventId;
    private Boolean duplicate;
    private String autoTriggerStatus;
    private String autoTriggerTaskId;
    private String autoTriggerMessage;
    private String message;
}
