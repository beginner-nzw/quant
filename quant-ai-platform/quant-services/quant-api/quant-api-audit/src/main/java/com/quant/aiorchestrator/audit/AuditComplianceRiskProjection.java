package com.quant.aiorchestrator.audit;

public record AuditComplianceRiskProjection(
        String taskId,
        String warningLevel,
        String suggestAction,
        String reviewStatus
) {
}
