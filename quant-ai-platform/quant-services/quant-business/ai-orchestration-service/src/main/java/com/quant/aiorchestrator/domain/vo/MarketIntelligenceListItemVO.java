package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MarketIntelligenceListItemVO {
    private String taskId;
    private String taskTitle;
    private String taskType;
    private String targetCode;
    private String targetName;
    private String priority;
    private String sourceChannel;
    private String intelligenceType;
    private String reportId;
    private String reportType;
    private String finalStatus;
    private Double confidenceScore;
    private Boolean needHumanReview;
    private String reviewStatus;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
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
    private String signalDirection;
    private String riskLevel;
    private List<String> intelligenceSourceTags;
    private String summary;
    private LocalDateTime createdAt;
}
