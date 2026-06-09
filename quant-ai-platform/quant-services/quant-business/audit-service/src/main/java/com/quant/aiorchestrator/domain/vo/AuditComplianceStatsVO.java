package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

@Data
public class AuditComplianceStatsVO {

    private Long totalCount;
    private Long pendingReviewCount;
    private Long interceptedCount;
    private Long revisedReportCount;
    private Long humanReviewCount;
    private Long decisionTraceCount;
    private Long promptAuditCount;
}
