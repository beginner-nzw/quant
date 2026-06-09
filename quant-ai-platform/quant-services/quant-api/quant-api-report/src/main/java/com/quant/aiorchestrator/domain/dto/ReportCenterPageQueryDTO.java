package com.quant.aiorchestrator.domain.dto;

import lombok.Data;

@Data
public class ReportCenterPageQueryDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String targetCode;
    private String targetName;
    private String reportType;
    private String reviewStatus;
    private Boolean onlyHighConfidence;
    private Boolean needHumanReview;
}
