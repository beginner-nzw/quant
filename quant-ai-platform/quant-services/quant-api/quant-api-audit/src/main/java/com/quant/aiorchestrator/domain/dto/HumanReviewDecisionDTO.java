package com.quant.aiorchestrator.domain.dto;

import java.util.List;

public class HumanReviewDecisionDTO {
    private String decision;
    private String reviewedBy;
    private String reviewComment;
    private String revisedSummary;
    private List<String> revisedHighlights;
    private List<String> revisedRiskPoints;
    private Boolean rerunWorkflow;
    private String rerunNodeName;

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }

    public String getRevisedSummary() {
        return revisedSummary;
    }

    public void setRevisedSummary(String revisedSummary) {
        this.revisedSummary = revisedSummary;
    }

    public List<String> getRevisedHighlights() {
        return revisedHighlights;
    }

    public void setRevisedHighlights(List<String> revisedHighlights) {
        this.revisedHighlights = revisedHighlights;
    }

    public List<String> getRevisedRiskPoints() {
        return revisedRiskPoints;
    }

    public void setRevisedRiskPoints(List<String> revisedRiskPoints) {
        this.revisedRiskPoints = revisedRiskPoints;
    }

    public Boolean getRerunWorkflow() {
        return rerunWorkflow;
    }

    public void setRerunWorkflow(Boolean rerunWorkflow) {
        this.rerunWorkflow = rerunWorkflow;
    }

    public String getRerunNodeName() {
        return rerunNodeName;
    }

    public void setRerunNodeName(String rerunNodeName) {
        this.rerunNodeName = rerunNodeName;
    }
}
