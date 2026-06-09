package com.quant.aiorchestrator.report;

import java.time.LocalDateTime;

public record ReportCenterRiskProjection(
        String taskId,
        String warningLevel,
        String suggestAction,
        String reviewStatus,
        LocalDateTime createdAt
) {
}
