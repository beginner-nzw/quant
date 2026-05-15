package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class TaskReportVO {
    private String taskType;
    private String finalStatus;
    private String reportId;
    private Integer versionNo;
    private String reportType;
    private String summary;
    private String originalSummary;
    private String displaySummary;
    private Double confidenceScore;
    private Boolean needHumanReview;
    private List<String> riskWarnings;
    private List<String> originalHighlights;
    private List<String> displayHighlights;
    private List<String> originalRiskPoints;
    private List<String> displayRiskPoints;
    private ReportMetaVO reportMeta;
    private String resultRef;
    private String rawPayload;
    private Map<String, Object> contextSnapshot;
    private List<ReportSectionVO> sections;
    private List<ReportEvidenceItemVO> evidenceItems;
    private List<String> evidenceRefs;
    private List<HumanReviewRecordVO> humanReviewRecords;
    private String reviewSuggestion;
    private String reviewStatus;
    private String reviewedBy;
    private String reviewedAt;
    private String revisedSummary;
    private List<String> revisedHighlights;
    private List<String> revisedRiskPoints;
    private String reviewComment;

    @Data
    public static class ReportMetaVO {
        private String reportId;
        private String reportType;
        private List<String> highlights;
        private List<String> riskPoints;
        private String summary;
    }

    @Data
    public static class ReportSectionVO {
        private String sectionId;
        private Integer versionNo;
        private String sectionCode;
        private String sectionTitle;
        private Integer sectionOrder;
        private String sectionContent;
        private List<String> sectionItems;
        private String revisedContent;
        private List<String> revisedItems;
        private String displayContent;
        private List<String> displayItems;
        private String reviewStatus;
        private String reviewedBy;
        private String reviewedAt;
        private String reviewComment;
        private Double confidenceScore;
    }

    @Data
    public static class ReportEvidenceItemVO {
        private String evidenceId;
        private String evidenceType;
        private String source;
        private String title;
        private String summary;
        private String url;
        private String occurredAt;
        private String referenceId;
        private String relevance;
    }

    @Data
    public static class HumanReviewRecordVO {
        private String reviewId;
        private String reviewerId;
        private String reviewerRole;
        private String reviewResult;
        private String reviewComment;
        private String beforeSnapshotRef;
        private String afterSnapshotRef;
        private String beforeSnapshot;
        private String afterSnapshot;
        private String traceId;
        private String createdAt;
    }
}
