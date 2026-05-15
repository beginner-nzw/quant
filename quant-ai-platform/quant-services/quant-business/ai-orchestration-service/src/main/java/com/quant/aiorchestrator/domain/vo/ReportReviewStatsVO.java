package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

@Data
public class ReportReviewStatsVO {
    private Long pendingCount;
    private Long approvedCount;
    private Long rejectedCount;
    private Long totalReportCount;
}
