package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.dto.*;
import com.quant.aiorchestrator.domain.entity.*;
import com.quant.aiorchestrator.domain.vo.*;
import com.quant.aiorchestrator.mapper.*;
import com.quant.common.model.enums.ReportReviewStatusEnum;
import com.quant.common.model.enums.RiskLevelEnum;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RiskWarningProjectionManager {

    private final RiskWarningReadManager readManager;
    private final FollowUpTaskSummaryManager followUpManager;
    private final RiskWarningListItemAssembler itemAssembler;

    public RiskWarningProjectionManager(RiskWarningReadManager readManager,
                                        FollowUpTaskSummaryManager followUpManager,
                                        StrategySignalRuleManager ruleManager) {
        this(readManager, followUpManager, new RiskWarningListItemAssembler(ruleManager));
    }

    public RiskWarningProjectionManager(RiskWarningReadManager readManager,
                                        FollowUpTaskSummaryManager followUpManager,
                                        RiskWarningListItemAssembler itemAssembler) {
        this.readManager = readManager;
        this.followUpManager = followUpManager;
        this.itemAssembler = itemAssembler;
    }

    public RiskWarningProjectionManager(ResearchTaskMapper researchTaskMapper,
                                        ResearchReportMapper researchReportMapper,
                                        RiskWarningMapper riskWarningMapper,
                                        RiskWarningDetailMapper riskWarningDetailMapper,
                                        StrategySignalMapper strategySignalMapper,
                                        FollowUpTaskSummaryManager followUpManager,
                                        StrategySignalRuleManager ruleManager) {
        this(
                new RiskWarningReadManager(
                        researchTaskMapper,
                        researchReportMapper,
                        riskWarningMapper,
                        riskWarningDetailMapper
                ),
                followUpManager,
                ruleManager
        );
    }

    public RiskWarningPageVO pageRiskWarnings(RiskWarningPageQueryDTO queryDTO) {
        RiskWarningPageQueryDTO safeQuery = queryDTO == null ? new RiskWarningPageQueryDTO() : queryDTO;
        int pageNum = safeQuery.getPageNum() == null || safeQuery.getPageNum() < 1 ? 1 : safeQuery.getPageNum();
        int pageSize = safeQuery.getPageSize() == null || safeQuery.getPageSize() < 1 ? 10 : safeQuery.getPageSize();

        List<RiskWarningListItemVO> matchedRecords = listRiskWarningRecords(safeQuery);
        int fromIndex = Math.min((pageNum - 1) * pageSize, matchedRecords.size());
        int toIndex = Math.min(fromIndex + pageSize, matchedRecords.size());

        RiskWarningPageVO vo = new RiskWarningPageVO();
        vo.setTotal((long) matchedRecords.size());
        vo.setPageNum((long) pageNum);
        vo.setPageSize((long) pageSize);
        vo.setRecords(fromIndex >= toIndex ? List.of() : matchedRecords.subList(fromIndex, toIndex));
        return vo;
    }
    public RiskWarningStatsVO getRiskWarningStats() {
        List<RiskWarningListItemVO> records = listRiskWarningRecords(new RiskWarningPageQueryDTO());
        RiskWarningStatsVO vo = new RiskWarningStatsVO();
        vo.setTotalCount((long) records.size());
        vo.setHighCount(records.stream().filter(item -> RiskLevelEnum.HIGH.name().equals(item.getRiskLevel())).count());
        vo.setMediumCount(records.stream().filter(item -> RiskLevelEnum.MEDIUM.name().equals(item.getRiskLevel())).count());
        vo.setLowCount(records.stream().filter(item -> RiskLevelEnum.LOW.name().equals(item.getRiskLevel())).count());
        vo.setPendingReviewCount(records.stream().filter(item -> ReportReviewStatusEnum.PENDING.name().equals(item.getReportReviewStatus())).count());
        vo.setHumanReviewCount(records.stream().filter(item -> Boolean.TRUE.equals(item.getNeedHumanReview())).count());
        return vo;
    }

    private List<RiskWarningListItemVO> listRiskWarningRecords(RiskWarningPageQueryDTO queryDTO) {
        List<RiskWarningDO> domainWarnings = readManager.loadActiveRiskWarnings();
        if (domainWarnings.isEmpty()) {
            return listRiskWarningRecordsFromReports(queryDTO, Collections.emptySet());
        }

        Set<String> coveredTaskIds = domainWarnings.stream()
                .map(RiskWarningDO::getTaskId)
                .filter(taskId -> taskId != null && !taskId.isBlank())
                .collect(Collectors.toSet());

        List<RiskWarningListItemVO> records = new ArrayList<>(listRiskWarningRecordsFromDomain(domainWarnings, queryDTO));
        records.addAll(listRiskWarningRecordsFromReports(queryDTO, coveredTaskIds));
        return sortRiskWarningRecords(records);
    }

    private List<RiskWarningListItemVO> listRiskWarningRecordsFromDomain(List<RiskWarningDO> warnings,
                                                                         RiskWarningPageQueryDTO queryDTO) {
        if (warnings.isEmpty()) {
            return List.of();
        }

        Set<String> taskIds = warnings.stream()
                .map(RiskWarningDO::getTaskId)
                .filter(taskId -> taskId != null && !taskId.isBlank())
                .collect(Collectors.toSet());
        Map<String, ResearchTaskDO> taskMap = readManager.loadTaskMap(taskIds);
        Map<String, ResearchReportDO> reportMap = readManager.loadReportMapByTaskIds(taskIds);

        List<ResearchTaskDO> followUpTasks = readManager.loadRiskWarningFollowUpTasks();
        Map<String, List<ResearchTaskDO>> followUpTaskMapBySourceTaskId = readManager.groupFollowUpTasksBySourceTaskId(followUpTasks);
        Map<String, List<ResearchTaskDO>> followUpTaskMapBySourceReportId = readManager.groupFollowUpTasksBySourceReportId(followUpTasks);

        Set<String> warningIds = warnings.stream()
                .map(RiskWarningDO::getWarningId)
                .filter(warningId -> warningId != null && !warningId.isBlank())
                .collect(Collectors.toSet());
        Map<String, List<RiskWarningDetailDO>> detailMap = readManager.loadRiskWarningDetailMapByWarningIds(warningIds);

        return warnings.stream()
                .map(warning -> {
                    ResearchTaskDO task = taskMap.get(warning.getTaskId());
                    ResearchReportDO report = reportMap.get(warning.getTaskId());
                    return itemAssembler.toRiskWarningItem(
                            warning,
                            task,
                            report,
                            followUpManager.resolveSummary(
                                    task,
                                    report,
                                    followUpTaskMapBySourceTaskId,
                                    followUpTaskMapBySourceReportId
                            ),
                            detailMap.getOrDefault(warning.getWarningId(), List.of())
                    );
                })
                .filter(Objects::nonNull)
                .filter(item -> itemAssembler.matchesRiskWarningQuery(item, queryDTO))
                .toList();
    }

    private List<RiskWarningListItemVO> listRiskWarningRecordsFromReports(RiskWarningPageQueryDTO queryDTO,
                                                                          Set<String> excludedTaskIds) {
        List<ResearchReportDO> reports = readManager.loadActiveReports();

        if (reports.isEmpty()) {
            return List.of();
        }

        if (excludedTaskIds != null && !excludedTaskIds.isEmpty()) {
            reports = reports.stream()
                    .filter(report -> report.getTaskId() == null || !excludedTaskIds.contains(report.getTaskId()))
                    .toList();
            if (reports.isEmpty()) {
                return List.of();
            }
        }

        Set<String> taskIds = reports.stream()
                .map(ResearchReportDO::getTaskId)
                .filter(taskId -> taskId != null && !taskId.isBlank())
                .collect(Collectors.toSet());

        if (taskIds.isEmpty()) {
            return List.of();
        }

        Map<String, ResearchTaskDO> taskMap = readManager.loadTaskMap(taskIds);

        List<ResearchTaskDO> followUpTasks = readManager.loadRiskWarningFollowUpTasks();
        Map<String, List<ResearchTaskDO>> followUpTaskMapBySourceTaskId = readManager.groupFollowUpTasksBySourceTaskId(followUpTasks);
        Map<String, List<ResearchTaskDO>> followUpTaskMapBySourceReportId = readManager.groupFollowUpTasksBySourceReportId(followUpTasks);

        return reports.stream()
                .map(report -> itemAssembler.toRiskWarningItem(
                        report,
                        taskMap.get(report.getTaskId()),
                        followUpManager.resolveSummary(
                                taskMap.get(report.getTaskId()),
                                report,
                                followUpTaskMapBySourceTaskId,
                                followUpTaskMapBySourceReportId
                        )
                ))
                .filter(Objects::nonNull)
                .filter(item -> itemAssembler.matchesRiskWarningQuery(item, queryDTO))
                .toList();
    }

    private List<RiskWarningListItemVO> sortRiskWarningRecords(List<RiskWarningListItemVO> records) {
        return itemAssembler.sortRiskWarningRecords(records);
    }

}

