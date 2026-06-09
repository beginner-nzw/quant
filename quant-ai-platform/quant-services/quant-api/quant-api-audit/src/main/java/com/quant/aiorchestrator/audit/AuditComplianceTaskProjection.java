package com.quant.aiorchestrator.audit;

import java.time.LocalDateTime;

public record AuditComplianceTaskProjection(
        String taskId,
        String taskTitle,
        String taskType,
        String targetCode,
        String targetName,
        String priority,
        String traceId,
        LocalDateTime createdAt
) {
}
