package com.quant.aiorchestrator.domain.dto;

import lombok.Data;

@Data
public class AuditCompliancePageQueryDTO {

    private String taskId;
    private String targetCode;
    private String targetName;
    private String reviewStatus;
    private String auditResultStatus;
    private Boolean needHumanReview;
    private Boolean onlyIntercepted;
    private Integer pageNum;
    private Integer pageSize;
}
