package com.quant.aiorchestrator.domain.dto;

import lombok.Data;

@Data
public class TaskPageQueryDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String taskType;
    private String status;
    private String targetCode;
    private String targetName;
    private Boolean onlyFailed;
    private Boolean hasRetry;
    private Boolean onlyPendingReview;
    private String reportReviewStatus;
    private String reportReviewedBy;
}