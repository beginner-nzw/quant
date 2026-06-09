package com.quant.aiorchestrator.report;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TaskReportProjection(
        String reportId,
        String taskId,
        String taskType,
        String finalStatus,
        String summary,
        BigDecimal confidenceScore,
        Integer needHumanReview,
        String reportType,
        String highlights,
        String riskPoints,
        String riskWarnings,
        String resultRef,
        LocalDateTime createdAt,
        String reviewStatus,
        String reviewedBy,
        LocalDateTime reviewedAt,
        String revisedSummary,
        String revisedHighlights,
        String revisedRiskPoints,
        String reviewComment
) {
    public String getReportId() {
        return reportId;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getTaskType() {
        return taskType;
    }

    public String getFinalStatus() {
        return finalStatus;
    }

    public String getSummary() {
        return summary;
    }

    public BigDecimal getConfidenceScore() {
        return confidenceScore;
    }

    public Integer getNeedHumanReview() {
        return needHumanReview;
    }

    public String getReportType() {
        return reportType;
    }

    public String getHighlights() {
        return highlights;
    }

    public String getRiskPoints() {
        return riskPoints;
    }

    public String getRiskWarnings() {
        return riskWarnings;
    }

    public String getResultRef() {
        return resultRef;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public String getRevisedSummary() {
        return revisedSummary;
    }

    public String getRevisedHighlights() {
        return revisedHighlights;
    }

    public String getRevisedRiskPoints() {
        return revisedRiskPoints;
    }

    public String getReviewComment() {
        return reviewComment;
    }
}
