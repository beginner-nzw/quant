package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AuditComplianceListItemVO {

    private String taskId;
    private String taskTitle;
    private String taskType;
    private String targetCode;
    private String targetName;
    private String priority;
    private String traceId;

    private String reportId;
    private String reportType;
    private String finalStatus;
    private String reviewStatus;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private String reviewComment;
    private Boolean needHumanReview;

    private Boolean revised;
    private Boolean intercepted;

    private Long auditCount;
    private Long failedAuditCount;
    private Long agentAuditCount;
    private Long humanAuditCount;

    private Long agentExecutionCount;
    private Long humanReviewAgentCount;

    private String workflowInstanceId;
    private String workflowCode;
    private String workflowVersion;
    private String workflowStatus;
    private String currentNode;

    private Boolean hasInputLog;
    private Boolean hasOutputLog;
    private Boolean hasDecisionLog;

    private String latestAuditType;
    private String latestAuditStage;
    private String latestAuditActionCode;
    private String latestAuditResultStatus;
    private String latestAuditRemark;
    private LocalDateTime latestAuditAt;

    private String originalSummary;
    private String revisedSummary;
    private List<String> originalHighlights;
    private List<String> revisedHighlights;
    private List<String> originalRiskPoints;
    private List<String> revisedRiskPoints;

    private LocalDateTime createdAt;
}
