package com.quant.aiorchestrator.domain.dto;

import lombok.Data;

@Data
public class StrategySignalPageQueryDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String targetCode;
    private String targetName;
    private String signalDirection;
    private String signalStrength;
    private String reportReviewStatus;
    private Boolean onlyHighConfidence;
}
