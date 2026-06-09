package com.quant.aiorchestrator.manager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.audit.AuditComplianceAgentExecutionProjection;
import com.quant.aiorchestrator.audit.AuditComplianceReportProjection;
import com.quant.aiorchestrator.audit.AuditComplianceRiskProjection;
import com.quant.aiorchestrator.audit.AuditComplianceTaskProjection;
import com.quant.aiorchestrator.audit.AuditComplianceWorkflowProjection;
import com.quant.aiorchestrator.domain.dto.AuditCompliancePageQueryDTO;
import com.quant.aiorchestrator.domain.entity.AuditRecordDO;
import com.quant.aiorchestrator.domain.vo.AuditComplianceListItemVO;
import com.quant.common.model.enums.ReportReviewStatusEnum;
import com.quant.common.model.enums.RiskLevelEnum;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class AuditComplianceItemAssembler {

    private final ObjectMapper objectMapper;

    public AuditComplianceItemAssembler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AuditComplianceListItemVO toAuditComplianceItem(AuditComplianceReportProjection report,
                                                           AuditComplianceTaskProjection task,
                                                           List<AuditRecordDO> audits,
                                                           AuditComplianceWorkflowProjection workflow,
                                                           List<AuditComplianceAgentExecutionProjection> agentExecutions,
                                                           AuditComplianceRiskProjection warning) {
        if (report == null || task == null) {
            return null;
        }

        List<AuditRecordDO> safeAudits = audits == null ? List.of() : audits;
        List<AuditComplianceAgentExecutionProjection> safeAgentExecutions = agentExecutions == null ? List.of() : agentExecutions;
        ReportReviewStatusEnum reviewStatus = resolveReviewStatus(report.reviewStatus());
        AuditRecordDO latestAudit = safeAudits.isEmpty() ? null : safeAudits.get(0);
        boolean needHumanReview = resolveRiskProjection(report, warning).needHumanReview();
        boolean revised = isRevisedReport(report);
        long failedAuditCount = safeAudits.stream()
                .filter(item -> "FAILED".equalsIgnoreCase(item.getResultStatus()))
                .count();
        boolean intercepted = reviewStatus == ReportReviewStatusEnum.REJECTED
                || failedAuditCount > 0
                || (needHumanReview && reviewStatus != ReportReviewStatusEnum.APPROVED);

        AuditComplianceListItemVO vo = new AuditComplianceListItemVO();
        vo.setTaskId(task.taskId());
        vo.setTaskTitle(task.taskTitle());
        vo.setTaskType(task.taskType());
        vo.setTargetCode(task.targetCode());
        vo.setTargetName(task.targetName());
        vo.setPriority(task.priority());
        vo.setTraceId(task.traceId());
        vo.setReportId(report.reportId());
        vo.setReportType(resolveReportType(report, task));
        vo.setFinalStatus(report.finalStatus());
        vo.setReviewStatus(reviewStatus.name());
        vo.setReviewedBy(report.reviewedBy());
        vo.setReviewedAt(report.reviewedAt());
        vo.setReviewComment(report.reviewComment());
        vo.setNeedHumanReview(needHumanReview);
        vo.setRevised(revised);
        vo.setIntercepted(intercepted);

        vo.setAuditCount((long) safeAudits.size());
        vo.setFailedAuditCount(failedAuditCount);
        vo.setAgentAuditCount(safeAudits.stream().filter(item -> "AGENT".equalsIgnoreCase(item.getOperatorType())).count());
        vo.setHumanAuditCount(safeAudits.stream().filter(item -> "HUMAN".equalsIgnoreCase(item.getOperatorType())).count());

        vo.setAgentExecutionCount((long) safeAgentExecutions.size());
        vo.setHumanReviewAgentCount(safeAgentExecutions.stream()
                .filter(item -> item.needHumanReview() != null && item.needHumanReview() == 1)
                .count());

        vo.setWorkflowInstanceId(workflow == null ? null : workflow.workflowInstanceId());
        vo.setWorkflowCode(workflow == null ? null : workflow.workflowCode());
        vo.setWorkflowVersion(workflow == null ? null : workflow.workflowVersion());
        vo.setWorkflowStatus(workflow == null ? null : workflow.status());
        vo.setCurrentNode(workflow == null ? null : workflow.currentNode());

        vo.setHasInputLog(safeAgentExecutions.stream().anyMatch(item -> hasText(item.inputRef())));
        vo.setHasOutputLog(safeAgentExecutions.stream().anyMatch(item -> hasText(item.outputRef())));
        vo.setHasDecisionLog(safeAgentExecutions.stream().anyMatch(item -> hasText(item.decisionRef())));

        vo.setLatestAuditType(latestAudit == null ? null : latestAudit.getAuditType());
        vo.setLatestAuditStage(latestAudit == null ? null : latestAudit.getAuditStage());
        vo.setLatestAuditActionCode(latestAudit == null ? null : latestAudit.getActionCode());
        vo.setLatestAuditResultStatus(latestAudit == null ? null : normalizeAuditResultStatus(latestAudit.getResultStatus()));
        vo.setLatestAuditRemark(latestAudit == null ? null : latestAudit.getRemark());
        vo.setLatestAuditAt(latestAudit == null ? null : latestAudit.getCreatedAt());

        vo.setOriginalSummary(resolveOriginalSummary(report));
        vo.setRevisedSummary(hasText(report.revisedSummary()) ? report.revisedSummary().trim() : null);
        vo.setOriginalHighlights(readTextList(report.highlights()));
        vo.setRevisedHighlights(readTextList(report.revisedHighlights()));
        vo.setOriginalRiskPoints(readTextList(report.riskPoints()));
        vo.setRevisedRiskPoints(readTextList(report.revisedRiskPoints()));
        vo.setCreatedAt(firstNonNullOf(latestAudit == null ? null : latestAudit.getCreatedAt(), report.createdAt(), task.createdAt()));
        return vo;
    }

    public boolean matchesAuditComplianceQuery(AuditComplianceListItemVO item, AuditCompliancePageQueryDTO queryDTO) {
        if (item == null) {
            return false;
        }
        if (queryDTO == null) {
            return true;
        }
        if (queryDTO.getTaskId() != null && !queryDTO.getTaskId().isBlank()
                && !containsIgnoreCase(item.getTaskId(), queryDTO.getTaskId())) {
            return false;
        }
        if (queryDTO.getTargetCode() != null && !queryDTO.getTargetCode().isBlank()
                && !containsIgnoreCase(item.getTargetCode(), queryDTO.getTargetCode())) {
            return false;
        }
        if (queryDTO.getTargetName() != null && !queryDTO.getTargetName().isBlank()
                && !containsIgnoreCase(item.getTargetName(), queryDTO.getTargetName())) {
            return false;
        }
        ReportReviewStatusEnum reviewStatus = ReportReviewStatusEnum.from(queryDTO.getReviewStatus());
        if (reviewStatus != null && !reviewStatus.name().equals(item.getReviewStatus())) {
            return false;
        }
        String auditResultStatus = normalizeAuditResultStatus(queryDTO.getAuditResultStatus());
        if (auditResultStatus != null && !auditResultStatus.equalsIgnoreCase(item.getLatestAuditResultStatus())) {
            return false;
        }
        if (queryDTO.getNeedHumanReview() != null
                && !queryDTO.getNeedHumanReview().equals(item.getNeedHumanReview())) {
            return false;
        }
        return !Boolean.TRUE.equals(queryDTO.getOnlyIntercepted()) || Boolean.TRUE.equals(item.getIntercepted());
    }

    public boolean hasDecisionTrace(AuditComplianceListItemVO item) {
        return item != null
                && (hasText(item.getWorkflowInstanceId())
                || (item.getAgentExecutionCount() != null && item.getAgentExecutionCount() > 0));
    }

    public boolean hasPromptAuditTrail(AuditComplianceListItemVO item) {
        return item != null
                && (Boolean.TRUE.equals(item.getHasInputLog())
                || Boolean.TRUE.equals(item.getHasOutputLog())
                || Boolean.TRUE.equals(item.getHasDecisionLog()));
    }

    private boolean isRevisedReport(AuditComplianceReportProjection report) {
        return hasText(report.revisedSummary())
                || !readTextList(report.revisedHighlights()).isEmpty()
                || !readTextList(report.revisedRiskPoints()).isEmpty();
    }

    private String resolveOriginalSummary(AuditComplianceReportProjection report) {
        return hasText(report.summary()) ? report.summary().trim() : null;
    }

    private String normalizeAuditResultStatus(String resultStatus) {
        if (!hasText(resultStatus)) {
            return null;
        }
        if ("SUCCESS".equalsIgnoreCase(resultStatus)) {
            return "SUCCESS";
        }
        if ("FAILED".equalsIgnoreCase(resultStatus)) {
            return "FAILED";
        }
        return resultStatus.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private LocalDateTime firstNonNullOf(LocalDateTime... candidates) {
        if (candidates == null || candidates.length == 0) {
            return null;
        }
        for (LocalDateTime candidate : candidates) {
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    private List<String> readTextList(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(rawJson, new TypeReference<List<String>>() {})
                    .stream()
                    .filter(item -> item != null && !item.isBlank())
                    .map(String::trim)
                    .toList();
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private boolean isDomainRiskHumanReview(AuditComplianceRiskProjection warning) {
        if (warning == null) {
            return false;
        }
        if ("NEED_HUMAN_REVIEW".equalsIgnoreCase(warning.suggestAction())) {
            return true;
        }
        RiskLevelEnum riskLevel = RiskLevelEnum.from(warning.warningLevel());
        return riskLevel == RiskLevelEnum.HIGH
                && ReportReviewStatusEnum.PENDING.name().equalsIgnoreCase(warning.reviewStatus());
    }

    private RiskLevelEnum resolveDomainRiskLevel(AuditComplianceRiskProjection warning) {
        RiskLevelEnum resolved = warning == null ? null : RiskLevelEnum.from(warning.warningLevel());
        return resolved == null ? RiskLevelEnum.LOW : resolved;
    }

    private ReportReviewStatusEnum resolveReviewStatus(String reviewStatus) {
        ReportReviewStatusEnum resolved = ReportReviewStatusEnum.from(reviewStatus);
        return resolved == null ? ReportReviewStatusEnum.PENDING : resolved;
    }

    private RiskLevelEnum resolveRiskLevel(int totalRiskCount, boolean needHumanReview) {
        if (needHumanReview || totalRiskCount >= 4) {
            return RiskLevelEnum.HIGH;
        }
        if (totalRiskCount >= 2) {
            return RiskLevelEnum.MEDIUM;
        }
        return RiskLevelEnum.LOW;
    }

    private RiskProjection resolveRiskProjection(AuditComplianceReportProjection report,
                                                 AuditComplianceRiskProjection warning) {
        if (warning != null) {
            int warningCount = 1;
            return new RiskProjection(
                    isDomainRiskHumanReview(warning),
                    warningCount,
                    0,
                    warningCount,
                    resolveDomainRiskLevel(warning)
            );
        }
        int warningCount = report == null ? 0 : readTextList(report.riskWarnings()).size();
        int riskPointCount = report == null ? 0 : readPreferredTextList(report.revisedRiskPoints(), report.riskPoints()).size();
        boolean needHumanReview = report != null && report.needHumanReview() != null && report.needHumanReview() == 1;
        int totalRiskCount = warningCount + riskPointCount;
        return new RiskProjection(
                needHumanReview,
                warningCount,
                riskPointCount,
                totalRiskCount,
                totalRiskCount > 0 || needHumanReview ? resolveRiskLevel(totalRiskCount, needHumanReview) : null
        );
    }

    private boolean containsIgnoreCase(String source, String target) {
        return source != null && target != null && source.toLowerCase().contains(target.toLowerCase());
    }

    private List<String> readPreferredTextList(String preferredRawJson, String fallbackRawJson) {
        List<String> preferred = readTextList(preferredRawJson);
        return preferred.isEmpty() ? readTextList(fallbackRawJson) : preferred;
    }

    private String resolveReportType(AuditComplianceReportProjection report, AuditComplianceTaskProjection task) {
        if (report.reportType() != null && !report.reportType().isBlank()) {
            return report.reportType().trim();
        }
        if (report.taskType() != null && !report.taskType().isBlank()) {
            return report.taskType().trim();
        }
        return task == null ? null : task.taskType();
    }

    private record RiskProjection(
            boolean needHumanReview,
            int warningCount,
            int riskPointCount,
            int totalRiskCount,
            RiskLevelEnum riskLevel
    ) {
    }
}
