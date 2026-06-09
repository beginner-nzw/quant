package com.quant.aiorchestrator.audit;

import java.time.LocalDateTime;

public record HumanReviewQueueRiskProjection(
        String warningId,
        String taskId,
        String warningLevel,
        String warningSummary,
        String warningReason,
        String suggestAction,
        String reviewStatus,
        String reviewerId,
        LocalDateTime reviewTime,
        LocalDateTime createdAt
) {
}
