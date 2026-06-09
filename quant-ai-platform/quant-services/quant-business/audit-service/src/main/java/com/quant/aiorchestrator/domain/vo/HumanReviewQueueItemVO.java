package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class HumanReviewQueueItemVO {
    private String queueId;
    private String domain;
    private String taskId;
    private String taskTitle;
    private String taskType;
    private String targetCode;
    private String targetName;
    private String priority;
    private String reportId;
    private String reportType;
    private String relatedObjectType;
    private String relatedObjectId;
    private String reviewStatus;
    private String riskLevel;
    private Boolean needHumanReview;
    private Boolean revised;
    private Boolean rerunnable;
    private String currentNode;
    private String summary;
    private List<String> riskPoints;
    private String reviewComment;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
}
