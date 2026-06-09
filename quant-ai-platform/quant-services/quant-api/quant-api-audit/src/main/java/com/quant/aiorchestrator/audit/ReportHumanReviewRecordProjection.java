package com.quant.aiorchestrator.audit;

import java.time.LocalDateTime;

public record ReportHumanReviewRecordProjection(
        String reviewId,
        String taskId,
        String relatedObjectType,
        String relatedObjectId,
        String reviewerId,
        String reviewerRole,
        String reviewResult,
        String reviewComment,
        String beforeSnapshotRef,
        String afterSnapshotRef,
        String beforeSnapshot,
        String afterSnapshot,
        String traceId,
        String tenantId,
        LocalDateTime createdAt
) {
}
