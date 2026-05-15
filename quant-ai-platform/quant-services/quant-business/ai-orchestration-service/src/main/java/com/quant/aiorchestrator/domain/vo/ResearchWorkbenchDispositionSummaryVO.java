package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

@Data
public class ResearchWorkbenchDispositionSummaryVO {
    private String domainCode;
    private Long totalCount;
    private Long notTrackedCount;
    private Long trackingCount;
    private Long completedCount;
    private Long failedCount;
}
