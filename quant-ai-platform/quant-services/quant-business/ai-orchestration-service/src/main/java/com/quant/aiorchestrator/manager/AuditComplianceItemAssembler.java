package com.quant.aiorchestrator.manager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.dto.AuditCompliancePageQueryDTO;
import com.quant.aiorchestrator.domain.entity.AiAgentExecutionDO;
import com.quant.aiorchestrator.domain.entity.AiWorkflowInstanceDO;
import com.quant.aiorchestrator.domain.entity.AuditRecordDO;
import com.quant.aiorchestrator.domain.entity.ResearchReportDO;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.aiorchestrator.domain.entity.RiskWarningDO;
import com.quant.aiorchestrator.domain.entity.RiskWarningDetailDO;
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

    public AuditComplianceListItemVO toAuditComplianceItem(ResearchReportDO report,
                                                           ResearchTaskDO task,
                                                           List<AuditRecordDO> audits,
                                                           AiWorkflowInstanceDO workflow,
                                                           List<AiAgentExecutionDO> agentExecutions,
                                                           RiskWarningDO warning) {
        if (report == null || task == null) {
            return null;
        }

        List<AuditRecordDO> safeAudits = audits == null ? List.of() : audits;
        List<AiAgentExecutionDO> safeAgentExecutions = agentExecutions == null ? List.of() : agentExecutions;
        ReportReviewStatusEnum reviewStatus = resolveReviewStatus(report.getReviewStatus());
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
        vo.setTaskId(task.getTaskId());
        vo.setTaskTitle(task.getTaskTitle());
        vo.setTaskType(task.getTaskType());
        vo.setTargetCode(task.getTargetCode());
        vo.setTargetName(task.getTargetName());
        vo.setPriority(task.getPriority());
        vo.setTraceId(task.getTraceId());
        vo.setReportId(report.getReportId());
        vo.setReportType(resolveReportType(report, task));
        vo.setFinalStatus(report.getFinalStatus());
        vo.setReviewStatus(reviewStatus.name());
        vo.setReviewedBy(report.getReviewedBy());
        vo.setReviewedAt(report.getReviewedAt());
        vo.setReviewComment(report.getReviewComment());
        vo.setNeedHumanReview(needHumanReview);
        vo.setRevised(revised);
        vo.setIntercepted(intercepted);

        vo.setAuditCount((long) safeAudits.size());
        vo.setFailedAuditCount(failedAuditCount);
        vo.setAgentAuditCount(safeAudits.stream().filter(item -> "AGENT".equalsIgnoreCase(item.getOperatorType())).count());
        vo.setHumanAuditCount(safeAudits.stream().filter(item -> "HUMAN".equalsIgnoreCase(item.getOperatorType())).count());

        vo.setAgentExecutionCount((long) safeAgentExecutions.size());
        vo.setHumanReviewAgentCount(safeAgentExecutions.stream()
                .filter(item -> item.getNeedHumanReview() != null && item.getNeedHumanReview() == 1)
                .count());

        vo.setWorkflowInstanceId(workflow == null ? null : workflow.getWorkflowInstanceId());
        vo.setWorkflowCode(workflow == null ? null : workflow.getWorkflowCode());
        vo.setWorkflowVersion(workflow == null ? null : workflow.getWorkflowVersion());
        vo.setWorkflowStatus(workflow == null ? null : workflow.getStatus());
        vo.setCurrentNode(workflow == null ? null : workflow.getCurrentNode());

        vo.setHasInputLog(safeAgentExecutions.stream().anyMatch(item -> hasText(item.getInputRef())));
        vo.setHasOutputLog(safeAgentExecutions.stream().anyMatch(item -> hasText(item.getOutputRef())));
        vo.setHasDecisionLog(safeAgentExecutions.stream().anyMatch(item -> hasText(item.getDecisionRef())));

        vo.setLatestAuditType(latestAudit == null ? null : latestAudit.getAuditType());
        vo.setLatestAuditStage(latestAudit == null ? null : latestAudit.getAuditStage());
        vo.setLatestAuditActionCode(latestAudit == null ? null : latestAudit.getActionCode());
        vo.setLatestAuditResultStatus(latestAudit == null ? null : normalizeAuditResultStatus(latestAudit.getResultStatus()));
        vo.setLatestAuditRemark(latestAudit == null ? null : latestAudit.getRemark());
        vo.setLatestAuditAt(latestAudit == null ? null : latestAudit.getCreatedAt());

        vo.setOriginalSummary(resolveOriginalSummary(report));
        vo.setRevisedSummary(hasText(report.getRevisedSummary()) ? report.getRevisedSummary().trim() : null);
        vo.setOriginalHighlights(readTextList(report.getHighlights()));
        vo.setRevisedHighlights(readTextList(report.getRevisedHighlights()));
        vo.setOriginalRiskPoints(readTextList(report.getRiskPoints()));
        vo.setRevisedRiskPoints(readTextList(report.getRevisedRiskPoints()));
        vo.setCreatedAt(firstNonNullOf(latestAudit == null ? null : latestAudit.getCreatedAt(), report.getCreatedAt(), task.getCreatedAt()));
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

    private boolean isRevisedReport(ResearchReportDO report) {
        return hasText(report.getRevisedSummary())
                || !readTextList(report.getRevisedHighlights()).isEmpty()
                || !readTextList(report.getRevisedRiskPoints()).isEmpty();
    }

    private String resolveOriginalSummary(ResearchReportDO report) {
        return hasText(report.getSummary()) ? report.getSummary().trim() : null;
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

    private boolean isDomainRiskHumanReview(RiskWarningDO warning) {
        if (warning == null) {
            return false;
        }
        if ("NEED_HUMAN_REVIEW".equalsIgnoreCase(warning.getSuggestAction())) {
            return true;
        }
        RiskLevelEnum riskLevel = RiskLevelEnum.from(warning.getWarningLevel());
        return riskLevel == RiskLevelEnum.HIGH
                && ReportReviewStatusEnum.PENDING.name().equalsIgnoreCase(warning.getReviewStatus());
    }

    private RiskLevelEnum resolveDomainRiskLevel(RiskWarningDO warning) {
        RiskLevelEnum resolved = warning == null ? null : RiskLevelEnum.from(warning.getWarningLevel());
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

    private RiskProjection resolveRiskProjection(ResearchReportDO report,
                                                 RiskWarningDO warning,
                                                 List<RiskWarningDetailDO> details) {
        if (warning != null) {
            int warningCount = 1;
            int riskPointCount = details == null ? 0 : details.size();
            return new RiskProjection(
                    isDomainRiskHumanReview(warning),
                    warningCount,
                    riskPointCount,
                    warningCount + riskPointCount,
                    resolveDomainRiskLevel(warning)
            );
        }
        int warningCount = report == null ? 0 : readTextList(report.getRiskWarnings()).size();
        int riskPointCount = report == null ? 0 : readPreferredTextList(report.getRevisedRiskPoints(), report.getRiskPoints()).size();
        boolean needHumanReview = report != null && report.getNeedHumanReview() != null && report.getNeedHumanReview() == 1;
        int totalRiskCount = warningCount + riskPointCount;
        return new RiskProjection(
                needHumanReview,
                warningCount,
                riskPointCount,
                totalRiskCount,
                totalRiskCount > 0 || needHumanReview ? resolveRiskLevel(totalRiskCount, needHumanReview) : null
        );
    }

    private RiskProjection resolveRiskProjection(ResearchReportDO report, RiskWarningDO warning) {
        return resolveRiskProjection(report, warning, List.of());
    }

    private boolean containsIgnoreCase(String source, String target) {
        return source != null && target != null && source.toLowerCase().contains(target.toLowerCase());
    }

    private List<String> readPreferredTextList(String preferredRawJson, String fallbackRawJson) {
        List<String> preferred = readTextList(preferredRawJson);
        return preferred.isEmpty() ? readTextList(fallbackRawJson) : preferred;
    }

    private String resolveReportType(ResearchReportDO report, ResearchTaskDO task) {
        if (report.getReportType() != null && !report.getReportType().isBlank()) {
            return report.getReportType().trim();
        }
        return task == null ? null : task.getTaskType();
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
