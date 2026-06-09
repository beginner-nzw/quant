package com.quant.aiorchestrator.domain.projection;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class MarketEventFollowUpProjection {
    private String eventId;
    private Integer followUpTaskCount;
    private String followUpStatus;
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
}
