package com.quant.aiorchestrator.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class HumanReviewDecisionDTO {
    private String decision;
    private String reviewedBy;
    private String reviewComment;
    private String revisedSummary;
    private List<String> revisedHighlights;
    private List<String> revisedRiskPoints;
    private Boolean rerunWorkflow;
    private String rerunNodeName;
}
