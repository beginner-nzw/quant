package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

@Data
public class RiskWarningStatsVO {
    private Long totalCount;
    private Long highCount;
    private Long mediumCount;
    private Long lowCount;
    private Long pendingReviewCount;
    private Long humanReviewCount;
}
