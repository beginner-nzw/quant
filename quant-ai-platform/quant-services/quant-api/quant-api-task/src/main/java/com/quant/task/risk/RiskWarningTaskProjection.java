package com.quant.task.risk;

import java.time.LocalDateTime;

public record RiskWarningTaskProjection(
        Long id,
        String taskId,
        String taskType,
        String taskTitle,
        String targetCode,
        String targetName,
        String priority,
        String status,
        String currentStage,
        String sourceTaskId,
        String sourceReportId,
        String sourceDomain,
        LocalDateTime createdAt
) {
}
