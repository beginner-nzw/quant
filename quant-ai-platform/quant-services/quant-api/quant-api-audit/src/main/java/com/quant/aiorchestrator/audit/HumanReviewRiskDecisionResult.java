package com.quant.aiorchestrator.audit;

public record HumanReviewRiskDecisionResult(
        String warningId,
        String reviewerId,
        Object beforeSnapshot,
        Object afterSnapshot,
        String traceId,
        String tenantId
) {
}
