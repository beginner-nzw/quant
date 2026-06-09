package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.audit.AuditComplianceAgentExecutionProjection;
import com.quant.aiorchestrator.audit.AuditComplianceReportProjection;
import com.quant.aiorchestrator.audit.AuditComplianceReportProvider;
import com.quant.aiorchestrator.audit.AuditComplianceRiskProjection;
import com.quant.aiorchestrator.audit.AuditComplianceRiskProvider;
import com.quant.aiorchestrator.audit.AuditComplianceTaskProjection;
import com.quant.aiorchestrator.audit.AuditComplianceTaskProvider;
import com.quant.aiorchestrator.audit.AuditComplianceWorkflowProjection;
import com.quant.aiorchestrator.audit.AuditComplianceWorkflowProvider;
import com.quant.aiorchestrator.domain.dto.AuditCompliancePageQueryDTO;
import com.quant.aiorchestrator.domain.entity.AuditRecordDO;
import com.quant.aiorchestrator.domain.vo.AuditComplianceListItemVO;
import com.quant.aiorchestrator.domain.vo.AuditCompliancePageVO;
import com.quant.aiorchestrator.domain.vo.AuditComplianceStatsVO;
import com.quant.aiorchestrator.service.AuditComplianceProjectionProvider;
import com.quant.common.model.enums.ReportReviewStatusEnum;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AuditComplianceProjectionManager implements AuditComplianceProjectionProvider {

    private final AuditComplianceTaskProvider taskProvider;
    private final AuditComplianceReportProvider reportProvider;
    private final AuditComplianceWorkflowProvider workflowProvider;
    private final AuditTaskReadManager auditTaskReadManager;
    private final AuditComplianceRiskProvider riskProvider;
    private final AuditComplianceItemAssembler itemAssembler;

    public AuditComplianceProjectionManager(AuditComplianceTaskProvider taskProvider,
                                            AuditComplianceReportProvider reportProvider,
                                            AuditComplianceWorkflowProvider workflowProvider,
                                            AuditTaskReadManager auditTaskReadManager,
                                            AuditComplianceRiskProvider riskProvider,
                                            AuditComplianceItemAssembler itemAssembler) {
        this.taskProvider = taskProvider;
        this.reportProvider = reportProvider;
        this.workflowProvider = workflowProvider;
        this.auditTaskReadManager = auditTaskReadManager;
        this.riskProvider = riskProvider;
        this.itemAssembler = itemAssembler;
    }

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
        vo.setDecisionTraceCount(records.stream().filter(itemAssembler::hasDecisionTrace).count());
        vo.setPromptAuditCount(records.stream().filter(itemAssembler::hasPromptAuditTrail).count());
        return vo;
    }

    private List<AuditComplianceListItemVO> listAuditComplianceRecords(AuditCompliancePageQueryDTO queryDTO) {
        List<AuditComplianceReportProjection> reports = reportProvider.listAuditComplianceReports();

        if (reports.isEmpty()) {
            return List.of();
        }

        Set<String> taskIds = reports.stream()
                .map(AuditComplianceReportProjection::taskId)
                .filter(taskId -> taskId != null && !taskId.isBlank())
                .collect(Collectors.toSet());

        if (taskIds.isEmpty()) {
            return List.of();
        }

        Map<String, AuditComplianceTaskProjection> taskMap = taskProvider.loadTaskMapByTaskIds(taskIds);
        Map<String, List<AuditRecordDO>> auditMap = auditTaskReadManager.loadAuditRecordMapByTaskIds(taskIds);
        Map<String, AuditComplianceWorkflowProjection> workflowMap = workflowProvider.loadLatestWorkflowInstanceMapByTaskIds(taskIds);
        Map<String, List<AuditComplianceAgentExecutionProjection>> agentExecutionMap = workflowProvider.loadAgentExecutionMapByTaskIds(taskIds);
        Map<String, AuditComplianceRiskProjection> riskWarningMap = riskProvider.loadLatestRiskWarningMapByTaskIds(taskIds);

        return reports.stream()
                .map(report -> itemAssembler.toAuditComplianceItem(
                        report,
                        taskMap.get(report.taskId()),
                        auditMap.getOrDefault(report.taskId(), List.of()),
                        workflowMap.get(report.taskId()),
                        agentExecutionMap.getOrDefault(report.taskId(), List.of()),
                        riskWarningMap.get(report.taskId())
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
}
