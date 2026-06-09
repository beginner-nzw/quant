package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class TaskReportReviewLogVO {
    private String reviewLogId;
    private String reportId;
    private String taskId;
    private Integer versionNo;
    private String reviewStatus;
    private String reviewedBy;
    private String reviewComment;
    private String revisedSummary;
    private List<String> revisedHighlights;
    private List<String> revisedRiskPoints;
    private String createdAt;
}
