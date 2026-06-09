package com.quant.aiorchestrator.domain.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ResearchReportSnapshot {

    private String reportId;
    private String taskId;
    private Integer versionNo;
    private String taskType;
    private String finalStatus;
    private String summary;
    private BigDecimal confidenceScore;
    private Integer needHumanReview;
    private String reportType;
    private String highlights;
    private String riskPoints;
    private String riskWarnings;
    private String resultRef;
    private String rawPayload;
    private String reviewStatus;
    private String reviewedBy;
    private String reviewedAt;
    private String revisedSummary;
    private String revisedHighlights;
    private String revisedRiskPoints;
    private String reviewComment;
}
