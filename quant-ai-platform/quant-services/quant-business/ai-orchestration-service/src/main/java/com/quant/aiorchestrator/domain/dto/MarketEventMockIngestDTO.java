package com.quant.aiorchestrator.domain.dto;

import lombok.Data;

@Data
public class MarketEventMockIngestDTO {

    private String targetType;
    private String targetCode;
    private String targetName;
    private String sourcePreset;
    private Integer itemCount;
}
