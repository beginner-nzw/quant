package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

@Data
public class TaskSummaryVO {
    private Integer stepCount;
    private Integer successStepCount;
    private Integer failedStepCount;
    private Integer agentCount;
    private Integer retryCount;
    private Boolean hasAudit;
    private Boolean hasFailure;
}