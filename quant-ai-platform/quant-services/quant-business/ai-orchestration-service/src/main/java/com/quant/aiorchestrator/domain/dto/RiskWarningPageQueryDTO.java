package com.quant.aiorchestrator.domain.dto;

import lombok.Data;

@Data
public class RiskWarningPageQueryDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String targetCode;
    private String targetName;
    private String riskLevel;
    private String reportReviewStatus;
    private Boolean needHumanReview;
}
