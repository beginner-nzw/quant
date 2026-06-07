package com.quant.aiorchestrator.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.dto.*;
import com.quant.aiorchestrator.domain.entity.*;
import com.quant.aiorchestrator.domain.vo.*;
import com.quant.aiorchestrator.mapper.*;
import com.quant.common.model.enums.ReportReviewStatusEnum;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AuditComplianceProjectionManager {

    private final ResearchTaskMapper researchTaskMapper;
    private final ResearchReportMapper researchReportMapper;
    private final AiWorkflowInstanceMapper aiWorkflowInstanceMapper;
    private final AiAgentExecutionMapper aiAgentExecutionMapper;
    private final AuditRecordMapper auditRecordMapper;
    private final RiskWarningMapper riskWarningMapper;
    private final AuditComplianceItemAssembler itemAssembler;

    public AuditComplianceProjectionManager(ResearchTaskMapper researchTaskMapper,
                                            ResearchReportMapper researchReportMapper,
                                            AiWorkflowInstanceMapper aiWorkflowInstanceMapper,
                                            AiAgentExecutionMapper aiAgentExecutionMapper,
                                            AuditRecordMapper auditRecordMapper,
                                            RiskWarningMapper riskWarningMapper,
                                            ObjectMapper objectMapper) {
        this(
                researchTaskMapper,
                researchReportMapper,
                aiWorkflowInstanceMapper,
                aiAgentExecutionMapper,
                auditRecordMapper,
                riskWarningMapper,
                objectMapper,
                new AuditComplianceItemAssembler(objectMapper)
        );
    }

    public AuditComplianceProjectionManager(ResearchTaskMapper researchTaskMapper,
                                            ResearchReportMapper researchReportMapper,
                                            AiWorkflowInstanceMapper aiWorkflowInstanceMapper,
                                            AiAgentExecutionMapper aiAgentExecutionMapper,
                                            AuditRecordMapper auditRecordMapper,
                                            RiskWarningMapper riskWarningMapper,
                                            ObjectMapper objectMapper,
                                            AuditComplianceItemAssembler itemAssembler) {
        this.researchTaskMapper = researchTaskMapper;
        this.researchReportMapper = researchReportMapper;
        this.aiWorkflowInstanceMapper = aiWorkflowInstanceMapper;
        this.aiAgentExecutionMapper = aiAgentExecutionMapper;
        this.auditRecordMapper = auditRecordMapper;
        this.riskWarningMapper = riskWarningMapper;
        this.itemAssembler = itemAssembler;
    }

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

        public AuditComplianceStatsVO getAuditComplianceStats() {
        List<AuditComplianceListItemVO> records = listAuditComplianceRecords(new AuditCompliancePageQueryDTO());
        AuditComplianceStatsVO vo = new AuditComplianceStatsVO();
        vo.setTotalCount((long) records.size());
        vo.setPendingReviewCount(records.stream().filter(item -> ReportReviewStatusEnum.PENDING.name().equals(item.getReviewStatus())).count());
        vo.setInterceptedCount(records.stream().filter(item -> Boolean.TRUE.equals(item.getIntercepted())).count());
        vo.setRevisedReportCount(records.stream().filter(item -> Boolean.TRUE.equals(item.getRevised())).count());
        vo.setHumanReviewCount(records.stream().filter(item -> Boolean.TRUE.equals(item.getNeedHumanReview())).count());
        vo.setDecisionTraceCount(records.stream().filter(itemAssembler::hasDecisionTrace).count());
        vo.setPromptAuditCount(records.stream().filter(itemAssembler::hasPromptAuditTrail).count());
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
                .map(report -> itemAssembler.toAuditComplianceItem(
                        report,
                        taskMap.get(report.getTaskId()),
                        auditMap.getOrDefault(report.getTaskId(), List.of()),
                        workflowMap.get(report.getTaskId()),
                        agentExecutionMap.getOrDefault(report.getTaskId(), List.of()),
                        riskWarningMap.get(report.getTaskId())
                ))
                .filter(Objects::nonNull)
                .filter(item -> itemAssembler.matchesAuditComplianceQuery(item, queryDTO))
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
}
