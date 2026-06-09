package com.quant.task.market;

import java.time.LocalDateTime;

public record MarketEventTaskProjection(
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
        String sourceEventId,
        LocalDateTime createdAt
) {
}
