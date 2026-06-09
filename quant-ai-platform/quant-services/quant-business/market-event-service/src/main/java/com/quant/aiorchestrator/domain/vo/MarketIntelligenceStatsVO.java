package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

@Data
public class MarketIntelligenceStatsVO {
    private Long totalCount;
    private Long riskAlertCount;
    private Long strategySignalCount;
    private Long reportInsightCount;
    private Long highPriorityCount;
    private Long pendingReviewCount;
}