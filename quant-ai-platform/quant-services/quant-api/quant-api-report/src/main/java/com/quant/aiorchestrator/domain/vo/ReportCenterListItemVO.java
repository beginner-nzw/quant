package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReportCenterListItemVO {
    private String taskId;
    private String taskTitle;
    private String taskType;
    private String targetCode;
    private String targetName;
    private String priority;
    private String reportId;
    private String reportType;
    private String finalStatus;
    private Double confidenceScore;
    private Boolean needHumanReview;
    private String reviewStatus;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private Boolean revised;
    private Boolean summaryRevised;
    private Boolean highlightsRevised;
    private Boolean riskPointsRevised;
    private String summary;
    private LocalDateTime createdAt;
}
