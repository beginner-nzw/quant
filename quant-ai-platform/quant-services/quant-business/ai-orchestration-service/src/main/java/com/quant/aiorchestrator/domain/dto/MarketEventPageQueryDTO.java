package com.quant.aiorchestrator.domain.dto;

import lombok.Data;

@Data
public class MarketEventPageQueryDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String targetCode;
    private String targetName;
    private String eventType;
    private String impactLevel;
    private String eventStatus;
}
