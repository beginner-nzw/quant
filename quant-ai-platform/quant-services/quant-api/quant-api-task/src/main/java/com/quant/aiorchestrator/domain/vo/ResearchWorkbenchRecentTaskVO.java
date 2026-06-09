package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ResearchWorkbenchRecentTaskVO {
    private String taskId;
    private String taskTitle;
    private String priority;
    private String status;
    private String currentStage;
    private Integer retryCount;
    private String reportId;
    private String reportReviewStatus;
    private Boolean revised;
    private Boolean summaryRevised;
    private Boolean highlightsRevised;
    private Boolean riskPointsRevised;
    private Double confidenceScore;
    private LocalDateTime finishTime;
    private LocalDateTime createdAt;
}
