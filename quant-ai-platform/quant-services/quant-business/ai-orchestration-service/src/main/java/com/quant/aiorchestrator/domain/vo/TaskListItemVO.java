package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskListItemVO {
    private String taskId;
    private String taskType;
    private String taskTitle;
    private String targetType;
    private String targetCode;
    private String targetName;
    private String priority;
    private String status;
    private String currentStage;
    private Integer retryCount;
    private String sourceTaskId;
    private String sourceReportId;
    private String sourceEventId;
    private String sourceDomain;
    private String sourceReviewStatus;
    private String analysisScope;
    private String reportId;
    private String reportType;
    private String errorMessage;
    private String reportReviewStatus;
    private Boolean revised;
    private Boolean summaryRevised;
    private Boolean highlightsRevised;
    private Boolean riskPointsRevised;
    private LocalDateTime startTime;
    private LocalDateTime finishTime;
    private LocalDateTime createdAt;
    private String reportReviewedBy;
    private LocalDateTime reportReviewedAt;
    private String reportReviewComment;
}
