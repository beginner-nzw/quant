package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

@Data
public class MarketEventStatsVO {
    private Long totalCount;
    private Long activeCount;
    private Long highImpactCount;
    private Long trackedCount;
    private Long todayCount;
}
