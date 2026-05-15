package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RiskWarningListItemVO {
    private String taskId;
    private String taskTitle;
    private String taskType;
    private String targetCode;
    private String targetName;
    private String priority;
    private String taskStatus;
    private String currentStage;
    private String reportId;
    private String reportType;
    private String finalStatus;
    private String riskLevel;
    private Integer warningCount;
    private Integer riskPointCount;
    private Integer totalRiskCount;
    private Boolean needHumanReview;
    private String reportReviewStatus;
    private String reportReviewedBy;
    private LocalDateTime reportReviewedAt;
    private Boolean revised;
    private Boolean summaryRevised;
    private Boolean highlightsRevised;
    private Boolean riskPointsRevised;
    private String followUpStatus;
    private Integer followUpTaskCount;
    private String latestFollowUpTaskId;
    private String latestFollowUpTaskTitle;
    private String latestFollowUpTaskStatus;
    private LocalDateTime latestFollowUpCreatedAt;
    private String reviewComment;
    private String summary;
    private List<String> riskReasons;
    private List<String> riskSourceTags;
    private LocalDateTime createdAt;
}
