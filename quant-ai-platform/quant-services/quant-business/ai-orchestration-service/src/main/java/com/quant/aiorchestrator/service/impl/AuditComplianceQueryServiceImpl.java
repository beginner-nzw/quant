package com.quant.aiorchestrator.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.dto.*;
import com.quant.aiorchestrator.domain.entity.*;
import com.quant.aiorchestrator.domain.vo.*;
import com.quant.aiorchestrator.mapper.*;
import com.quant.aiorchestrator.service.AuditComplianceQueryService;
import com.quant.aiorchestrator.util.CacheKeyUtil;
import com.quant.common.core.exception.BizException;
import com.quant.common.model.enums.MarketIntelligenceTypeEnum;
import com.quant.common.model.enums.ReportReviewStatusEnum;
import com.quant.common.model.enums.RiskLevelEnum;
import com.quant.common.model.enums.SignalDirectionEnum;
import com.quant.common.model.enums.SignalStrengthEnum;
import com.quant.common.model.enums.TaskStatusEnum;
import com.quant.common.redis.RedisKeyConstants;
import com.quant.common.redis.RedisKeyBuilder;
import com.quant.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditComplianceQueryServiceImpl implements AuditComplianceQueryService {

    private final ResearchTaskMapper researchTaskMapper;
    private final ResearchReportMapper researchReportMapper;
    private final AiWorkflowInstanceMapper aiWorkflowInstanceMapper;
    private final AiAgentExecutionMapper aiAgentExecutionMapper;
    private final AuditRecordMapper auditRecordMapper;
    private final RiskWarningMapper riskWarningMapper;
    private final ObjectMapper objectMapper;

private record RiskProjection(
            boolean needHumanReview,
            int warningCount,
            int riskPointCount,
            int totalRiskCount,
            RiskLevelEnum riskLevel
    ) {}

@Override
    public AuditCompliancePageVO pageAuditCompliance(AuditCompliancePageQueryDTO queryDTO) {
        AuditCompliancePageQueryDTO safeQuery = queryDTO == null ? new AuditCompliancePageQueryDTO() : queryDTO;
        int pageNum = safeQuery.getPageNum() == null || safeQuery.getPageNum() < 1 ? 1 : safeQuery.getPageNum();
        int pageSize = safeQuery.getPageSize() == null || safeQuery.getPageSize() < 1 ? 10 : safeQuery.getPageSize();

        List<AuditComplianceListItemVO> matchedRecords = listAuditComplianceRecords(safeQuery);
        int fromIndex = Math.min((pageNum - 1) * pageSize, matchedRecords.size());
        int toIndex = Math.min(fromIndex + pageSize, matchedRecords.size());

        AuditCompliancePageVO vo = new AuditCompliancePageVO();
        vo.setTotal((long) matchedRecords.size());
        vo.setPageNum((long) pageNum);
        vo.setPageSize((long) pageSize);
        vo.setRecords(fromIndex >= toIndex ? List.of() : matchedRecords.subList(fromIndex, toIndex));
        return vo;
    }

    @Override
    public AuditComplianceStatsVO getAuditComplianceStats() {
        List<AuditComplianceListItemVO> records = listAuditComplianceRecords(new AuditCompliancePageQueryDTO());
        AuditComplianceStatsVO vo = new AuditComplianceStatsVO();
        vo.setTotalCount((long) records.size());
        vo.setPendingReviewCount(records.stream().filter(item -> ReportReviewStatusEnum.PENDING.name().equals(item.getReviewStatus())).count());
        vo.setInterceptedCount(records.stream().filter(item -> Boolean.TRUE.equals(item.getIntercepted())).count());
        vo.setRevisedReportCount(records.stream().filter(item -> Boolean.TRUE.equals(item.getRevised())).count());
        vo.setHumanReviewCount(records.stream().filter(item -> Boolean.TRUE.equals(item.getNeedHumanReview())).count());
        vo.setDecisionTraceCount(records.stream().filter(this::hasDecisionTrace).count());
        vo.setPromptAuditCount(records.stream().filter(this::hasPromptAuditTrail).count());
        return vo;
    }

    private List<AuditComplianceListItemVO> listAuditComplianceRecords(AuditCompliancePageQueryDTO queryDTO) {
        List<ResearchReportDO> reports = researchReportMapper.selectList(
                new LambdaQueryWrapper<ResearchReportDO>()
                        .eq(ResearchReportDO::getDeleted, 0)
                        .orderByDesc(ResearchReportDO::getCreatedAt, ResearchReportDO::getId)
        );

        if (reports.isEmpty()) {
            return List.of();
        }

        Set<String> taskIds = reports.stream()
                .map(ResearchReportDO::getTaskId)
                .filter(taskId -> taskId != null && !taskId.isBlank())
                .collect(Collectors.toSet());

        if (taskIds.isEmpty()) {
            return List.of();
        }

        Map<String, ResearchTaskDO> taskMap = researchTaskMapper.selectList(
                new LambdaQueryWrapper<ResearchTaskDO>()
                        .eq(ResearchTaskDO::getDeleted, 0)
                        .in(ResearchTaskDO::getTaskId, taskIds)
        ).stream().collect(Collectors.toMap(
                ResearchTaskDO::getTaskId,
                item -> item,
                (left, right) -> left
        ));

        Map<String, List<AuditRecordDO>> auditMap = auditRecordMapper.selectList(
                new LambdaQueryWrapper<AuditRecordDO>()
                        .eq(AuditRecordDO::getDeleted, 0)
                        .in(AuditRecordDO::getTaskId, taskIds)
                        .orderByDesc(AuditRecordDO::getCreatedAt, AuditRecordDO::getId)
        ).stream().collect(Collectors.groupingBy(AuditRecordDO::getTaskId));

        Map<String, AiWorkflowInstanceDO> workflowMap = aiWorkflowInstanceMapper.selectList(
                new LambdaQueryWrapper<AiWorkflowInstanceDO>()
                        .eq(AiWorkflowInstanceDO::getDeleted, 0)
                        .in(AiWorkflowInstanceDO::getTaskId, taskIds)
                        .orderByDesc(AiWorkflowInstanceDO::getCreatedAt, AiWorkflowInstanceDO::getId)
        ).stream().collect(Collectors.toMap(
                AiWorkflowInstanceDO::getTaskId,
                item -> item,
                (left, right) -> left
        ));

        Map<String, List<AiAgentExecutionDO>> agentExecutionMap = aiAgentExecutionMapper.selectList(
                new LambdaQueryWrapper<AiAgentExecutionDO>()
                        .eq(AiAgentExecutionDO::getDeleted, 0)
                        .in(AiAgentExecutionDO::getTaskId, taskIds)
                        .orderByDesc(AiAgentExecutionDO::getCreatedAt, AiAgentExecutionDO::getId)
        ).stream().collect(Collectors.groupingBy(AiAgentExecutionDO::getTaskId));
        Map<String, RiskWarningDO> riskWarningMap = loadLatestRiskWarningMapByTaskIds(taskIds);

        return reports.stream()
                .map(report -> toAuditComplianceItem(
                        report,
                        taskMap.get(report.getTaskId()),
                        auditMap.getOrDefault(report.getTaskId(), List.of()),
                        workflowMap.get(report.getTaskId()),
                        agentExecutionMap.getOrDefault(report.getTaskId(), List.of()),
                        riskWarningMap.get(report.getTaskId())
                ))
                .filter(Objects::nonNull)
                .filter(item -> matchesAuditComplianceQuery(item, queryDTO))
                .sorted(Comparator
                        .comparing((AuditComplianceListItemVO item) -> Boolean.TRUE.equals(item.getIntercepted()))
                        .reversed()
                        .thenComparing(
                                AuditComplianceListItemVO::getLatestAuditAt,
                                Comparator.nullsLast(Comparator.reverseOrder())
                        )
                        .thenComparing(
                                AuditComplianceListItemVO::getReviewedAt,
                                Comparator.nullsLast(Comparator.reverseOrder())
                        )
                        .thenComparing(
                                AuditComplianceListItemVO::getCreatedAt,
                                Comparator.nullsLast(Comparator.reverseOrder())
                        )
                )
                .toList();
    }

    private AuditComplianceListItemVO toAuditComplianceItem(ResearchReportDO report,
                                                            ResearchTaskDO task,
                                                            List<AuditRecordDO> audits,
                                                            AiWorkflowInstanceDO workflow,
                                                            List<AiAgentExecutionDO> agentExecutions,
                                                            RiskWarningDO warning) {
        if (report == null || task == null) {
            return null;
        }

        ReportReviewStatusEnum reviewStatus = resolveReviewStatus(report.getReviewStatus());
        AuditRecordDO latestAudit = audits.isEmpty() ? null : audits.get(0);
        boolean needHumanReview = resolveRiskProjection(report, warning).needHumanReview();
        boolean revised = isRevisedReport(report);
        long failedAuditCount = audits.stream()
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

        vo.setAuditCount((long) audits.size());
        vo.setFailedAuditCount(failedAuditCount);
        vo.setAgentAuditCount(audits.stream().filter(item -> "AGENT".equalsIgnoreCase(item.getOperatorType())).count());
        vo.setHumanAuditCount(audits.stream().filter(item -> "HUMAN".equalsIgnoreCase(item.getOperatorType())).count());

        vo.setAgentExecutionCount((long) agentExecutions.size());
        vo.setHumanReviewAgentCount(agentExecutions.stream()
                .filter(item -> item.getNeedHumanReview() != null && item.getNeedHumanReview() == 1)
                .count());

        vo.setWorkflowInstanceId(workflow == null ? null : workflow.getWorkflowInstanceId());
        vo.setWorkflowCode(workflow == null ? null : workflow.getWorkflowCode());
        vo.setWorkflowVersion(workflow == null ? null : workflow.getWorkflowVersion());
        vo.setWorkflowStatus(workflow == null ? null : workflow.getStatus());
        vo.setCurrentNode(workflow == null ? null : workflow.getCurrentNode());

        vo.setHasInputLog(agentExecutions.stream().anyMatch(item -> hasText(item.getInputRef())));
        vo.setHasOutputLog(agentExecutions.stream().anyMatch(item -> hasText(item.getOutputRef())));
        vo.setHasDecisionLog(agentExecutions.stream().anyMatch(item -> hasText(item.getDecisionRef())));

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

    private boolean matchesAuditComplianceQuery(AuditComplianceListItemVO item, AuditCompliancePageQueryDTO queryDTO) {
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
        if (Boolean.TRUE.equals(queryDTO.getOnlyIntercepted()) && !Boolean.TRUE.equals(item.getIntercepted())) {
            return false;
        }
        return true;
    }

    private boolean hasDecisionTrace(AuditComplianceListItemVO item) {
        return item != null
                && (hasText(item.getWorkflowInstanceId())
                || (item.getAgentExecutionCount() != null && item.getAgentExecutionCount() > 0));
    }

    private boolean hasPromptAuditTrail(AuditComplianceListItemVO item) {
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

private Map<String, RiskWarningDO> loadLatestRiskWarningMapByTaskIds(Set<String> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return riskWarningMapper.selectList(
                new LambdaQueryWrapper<RiskWarningDO>()
                        .eq(RiskWarningDO::getDeleted, 0)
                        .in(RiskWarningDO::getTaskId, taskIds)
                        .orderByDesc(RiskWarningDO::getCreatedAt, RiskWarningDO::getId)
        ).stream().collect(Collectors.toMap(
                RiskWarningDO::getTaskId,
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new
        ));
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
}
