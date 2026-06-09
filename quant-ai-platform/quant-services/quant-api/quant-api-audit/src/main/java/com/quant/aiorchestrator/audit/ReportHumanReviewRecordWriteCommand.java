package com.quant.aiorchestrator.audit;

public record ReportHumanReviewRecordWriteCommand(
        String taskId,
        String reportId,
        String reviewerId,
        String reviewerRole,
        String reviewResult,
        String reviewComment,
        String beforeSnapshot,
        String afterSnapshot,
        String traceId,
        String tenantId
) {
}
