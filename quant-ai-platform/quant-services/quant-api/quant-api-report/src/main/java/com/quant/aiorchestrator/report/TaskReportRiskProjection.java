package com.quant.aiorchestrator.report;

public record TaskReportRiskProjection(
        String warningId,
        String taskId,
        String warningLevel,
        String warningSummary,
        String warningReason,
        String suggestAction,
        String reviewStatus
) {
}
