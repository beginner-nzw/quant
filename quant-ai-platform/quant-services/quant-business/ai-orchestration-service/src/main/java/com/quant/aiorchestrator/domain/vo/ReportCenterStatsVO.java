package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

@Data
public class ReportCenterStatsVO {
    private Long totalCount;
    private Long highConfidenceCount;
    private Long pendingReviewCount;
    private Long approvedCount;
    private Long humanReviewCount;
}
