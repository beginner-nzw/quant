package com.quant.aiorchestrator.domain.dto;

import lombok.Data;

@Data
public class MarketIntelligencePageQueryDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String targetCode;
    private String targetName;
    private String intelligenceType;
    private String reviewStatus;
    private Boolean onlyHighPriority;
    private Boolean needHumanReview;
}