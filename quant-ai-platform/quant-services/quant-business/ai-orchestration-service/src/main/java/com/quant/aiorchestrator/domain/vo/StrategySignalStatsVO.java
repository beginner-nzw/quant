package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

@Data
public class StrategySignalStatsVO {
    private Long totalCount;
    private Long positiveCount;
    private Long neutralCount;
    private Long negativeCount;
    private Long highConfidenceCount;
    private Long pendingReviewCount;
}
