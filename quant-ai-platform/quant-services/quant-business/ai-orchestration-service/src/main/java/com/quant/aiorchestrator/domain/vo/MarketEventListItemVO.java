package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class MarketEventListItemVO {
    private String eventId;
    private String targetType;
    private String targetCode;
    private String targetName;
    private String eventType;
    private String eventTitle;
    private String eventSummary;
    private String sourceChannel;
    private String sourceUrl;
    private String impactLevel;
    private String eventStatus;
    private String autoTriggerRuleCode;
    private String autoTriggerStatus;
    private String autoTriggerTaskId;
    private String autoTriggerMessage;
    private LocalDateTime autoTriggerAttemptedAt;
    private LocalDateTime occurredAt;
    private String createdBy;
    private LocalDateTime createdAt;
    private String followUpStatus;
    private Integer followUpTaskCount;
    private String latestFollowUpTaskId;
    private String latestFollowUpTaskTitle;
    private String latestFollowUpTaskStatus;
    private LocalDateTime latestFollowUpCreatedAt;
    private Integer relatedReportCount;
    private String latestReportTaskId;
    private String latestReportId;
    private String latestReportType;
    private String latestReportReviewStatus;
    private String latestReportSummary;
    private BigDecimal latestReportConfidenceScore;
    private Boolean latestNeedHumanReview;
    private LocalDateTime latestReportCreatedAt;
    private String derivedRiskLevel;
    private Integer derivedWarningCount;
    private Integer derivedRiskPointCount;
    private Integer derivedRiskCount;
    private String derivedSignalDirection;
    private String derivedSignalStrength;
    private Integer derivedSignalScore;
    private String derivedIntelligenceType;
    private Integer relationCount;
    private List<MarketEventRelationVO> relations;
}
