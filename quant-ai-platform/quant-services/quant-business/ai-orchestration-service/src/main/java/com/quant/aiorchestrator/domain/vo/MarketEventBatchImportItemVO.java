package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

@Data
public class MarketEventBatchImportItemVO {

    private Integer itemNo;
    private Boolean success;
    private Boolean duplicate;
    private String eventId;
    private String targetCode;
    private String targetName;
    private String eventTitle;
    private String autoTriggerStatus;
    private String autoTriggerTaskId;
    private String message;
}
