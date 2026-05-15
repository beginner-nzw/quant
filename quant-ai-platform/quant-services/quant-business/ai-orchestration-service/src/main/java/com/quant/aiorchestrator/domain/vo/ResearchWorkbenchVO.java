package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class ResearchWorkbenchVO {
    private String targetCode;
    private String targetName;
    private String targetType;
    private Long taskCount;
    private Long reportCount;
    private Long activeTaskCount;
    private Long successTaskCount;
    private Long failedTaskCount;
    private Long highConfidenceReportCount;
    private Long pendingReviewCount;
    private ResearchWorkbenchDispositionSummaryVO riskDispositionSummary;
    private ResearchWorkbenchDispositionSummaryVO strategySignalDispositionSummary;
    private ResearchWorkbenchDispositionSummaryVO marketIntelligenceDispositionSummary;
    private ResearchWorkbenchInsightVO latestInsight;
    private List<ResearchWorkbenchRecentTaskVO> recentTasks;
}
