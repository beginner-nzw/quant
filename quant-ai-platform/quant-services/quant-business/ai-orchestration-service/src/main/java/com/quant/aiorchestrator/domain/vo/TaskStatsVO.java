package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

@Data
public class TaskStatsVO {
    private Long totalCount;
    private Long runningCount;
    private Long successCount;
    private Long failedCount;
    private Long retriedCount;
}