package com.quant.aiorchestrator.audit;

import java.time.LocalDateTime;

public record HumanReviewQueueReportProjection(
        String reportId,
        String taskId,
        String taskType,
        String reportType,
        String reviewStatus,
        String reviewedBy,
        LocalDateTime reviewedAt,
        String reviewComment,
        Integer needHumanReview,
        String summary,
        String revisedSummary,
        String riskPoints,
        String revisedRiskPoints,
        String riskWarnings,
        LocalDateTime createdAt
) {
}
