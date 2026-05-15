package com.quant.aiorchestrator.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class TaskReportReviewDTO {
    private String reviewStatus;
    private String reviewedBy;
    private String revisedSummary;
    private List<String> revisedHighlights;
    private List<String> revisedRiskPoints;
    private String reviewComment;
}