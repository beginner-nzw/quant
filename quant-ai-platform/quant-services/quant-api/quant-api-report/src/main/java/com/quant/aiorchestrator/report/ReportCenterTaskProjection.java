package com.quant.aiorchestrator.report;

import java.time.LocalDateTime;

public record ReportCenterTaskProjection(
        String taskId,
        String taskTitle,
        String taskType,
        String targetCode,
        String targetName,
        String priority,
        LocalDateTime createdAt
) {
}
