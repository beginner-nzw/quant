package com.quant.task.workbench;

import java.time.LocalDateTime;

public record ResearchWorkbenchRiskProjection(
        String taskId,
        String warningId,
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
