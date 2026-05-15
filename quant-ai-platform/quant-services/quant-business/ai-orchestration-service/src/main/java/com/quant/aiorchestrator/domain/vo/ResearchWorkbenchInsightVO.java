package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ResearchWorkbenchInsightVO {
    private String taskId;
    private String taskTitle;
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
    private String signalDirection;
    private String signalStrength;
    private String riskLevel;
    private String summary;
    private List<String> highlights;
    private List<String> riskPoints;
    private LocalDateTime createdAt;
}
