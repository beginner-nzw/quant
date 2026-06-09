package com.quant.aiorchestrator.domain.dto;

import lombok.Data;

@Data
public class MarketEventSourceSyncDTO {

    private String targetType;
    private String targetCode;
    private String targetName;
    private Integer itemCount;
}
