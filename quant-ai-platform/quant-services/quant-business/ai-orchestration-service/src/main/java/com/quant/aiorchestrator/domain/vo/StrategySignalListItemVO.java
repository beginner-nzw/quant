package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class StrategySignalListItemVO {
    private String signalId;
    private String taskId;
    private String taskTitle;
    private String taskType;
    private String targetCode;
    private String targetName;
    private String priority;
    private String reportId;
    private String reportType;
    private String finalStatus;
    private String signalDirection;
    private String signalStrength;
    private Integer signalScore;
    private Double confidenceScore;
    private String reportReviewStatus;
    private String reportReviewedBy;
    private LocalDateTime reportReviewedAt;
    private Boolean needHumanReview;
    private String reviewComment;
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
    private String strategySummary;
    private List<String> signalSources;
    private List<String> signalSourceTags;
    private String backtestStatus;
    private String backtestSummary;
    private LocalDateTime createdAt;
}
