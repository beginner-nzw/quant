package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

@Data
public class HumanReviewQueueStatsVO {
    private Long totalCount;
    private Long pendingCount;
    private Long approvedCount;
    private Long rejectedCount;
    private Long reportCount;
    private Long riskCount;
    private Long complianceCount;
}
