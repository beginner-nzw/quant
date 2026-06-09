package com.quant.aiorchestrator.audit;

public record HumanReviewQueueTaskProjection(
        String taskId,
        String taskTitle,
        String taskType,
        String targetCode,
        String targetName,
        String priority
) {
}
