package com.quant.aiorchestrator.audit;

import java.time.LocalDateTime;

public record AuditComplianceReportProjection(
        String reportId,
        String taskId,
        String taskType,
        String reportType,
        String finalStatus,
        String reviewStatus,
        String reviewedBy,
        LocalDateTime reviewedAt,
        String reviewComment,
        Integer needHumanReview,
        String summary,
        String revisedSummary,
        String highlights,
        String revisedHighlights,
        String riskPoints,
        String revisedRiskPoints,
        String riskWarnings,
        LocalDateTime createdAt
) {
}
