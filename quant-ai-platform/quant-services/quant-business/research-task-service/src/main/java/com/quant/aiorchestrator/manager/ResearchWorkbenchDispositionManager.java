package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.report.TaskReportProjection;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.aiorchestrator.domain.vo.ResearchWorkbenchDispositionSummaryVO;
import com.quant.aiorchestrator.domain.vo.ResearchWorkbenchVO;
import com.quant.task.workbench.ResearchWorkbenchRiskProjection;
import com.quant.task.workbench.ResearchWorkbenchStrategyProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ResearchWorkbenchDispositionManager {

    private final ResearchWorkbenchReadManager readManager;
    private final FollowUpTaskSummaryManager followUpManager;
    private final ResearchWorkbenchRuleManager ruleManager;

    public void populateDispositionSummaries(ResearchWorkbenchVO vo,
                                             List<ResearchTaskDO> tasks,
                                             List<TaskReportProjection> reports,
                                             Map<String, ResearchTaskDO> taskMap,
                                             Map<String, ResearchWorkbenchRiskProjection> riskWarningMap,
                                             Map<String, ResearchWorkbenchStrategyProjection> strategySignalMap) {
        if (vo == null || tasks == null || tasks.isEmpty()) {
            return;
        }

        Set<String> taskIds = tasks.stream()
                .map(ResearchTaskDO::getTaskId)
                .filter(taskId -> taskId != null && !taskId.isBlank())
                .collect(Collectors.toSet());
        List<TaskReportProjection> safeReports = reports == null ? List.of() : reports;
        Set<String> reportIds = safeReports.stream()
                .map(TaskReportProjection::getReportId)
                .filter(reportId -> reportId != null && !reportId.isBlank())
                .collect(Collectors.toSet());
        Map<String, TaskReportProjection> latestReportMap = safeReports.stream().collect(Collectors.toMap(
                TaskReportProjection::getTaskId,
                item -> item,
                (left, right) -> left
        ));

        vo.setRiskDispositionSummary(buildDispositionSummary("RISK_WARNING",
                collectRiskStatuses(taskIds, reportIds, safeReports, taskMap, latestReportMap, riskWarningMap)));
        vo.setStrategySignalDispositionSummary(buildDispositionSummary("STRATEGY_SIGNAL",
                collectStrategyStatuses(taskIds, reportIds, safeReports, taskMap, latestReportMap, strategySignalMap)));
        vo.setMarketIntelligenceDispositionSummary(buildDispositionSummary("MARKET_INTELLIGENCE",
                collectMarketIntelligenceStatuses(taskIds, reportIds, safeReports, taskMap)));
    }

    public ResearchWorkbenchDispositionSummaryVO emptyDispositionSummary(String domainCode) {
        return buildDispositionSummary(domainCode, List.of());
    }

    private List<String> collectRiskStatuses(Set<String> taskIds,
                                             Set<String> reportIds,
                                             List<TaskReportProjection> reports,
                                             Map<String, ResearchTaskDO> taskMap,
                                             Map<String, TaskReportProjection> latestReportMap,
                                             Map<String, ResearchWorkbenchRiskProjection> riskWarningMap) {
        List<ResearchTaskDO> followUpTasks = readManager.loadFollowUpTasks("RISK_WARNING", taskIds, reportIds);
        Map<String, List<ResearchTaskDO>> followUpTaskMapBySourceTaskId = readManager.groupFollowUpTasksBySourceTaskId(followUpTasks);
        Map<String, List<ResearchTaskDO>> followUpTaskMapBySourceReportId = readManager.groupFollowUpTasksBySourceReportId(followUpTasks);
        Set<String> coveredRiskTaskIds = riskWarningMap == null
                ? Collections.emptySet()
                : riskWarningMap.keySet().stream()
                .filter(taskId -> taskId != null && !taskId.isBlank())
                .collect(Collectors.toSet());
        List<String> statuses = new ArrayList<>();
        if (riskWarningMap != null && !riskWarningMap.isEmpty()) {
            statuses.addAll(riskWarningMap.values().stream()
                    .map(warning -> followUpManager.resolveSummary(
                                    taskMap.get(warning.taskId()),
                                    latestReportMap.get(warning.taskId()),
                                    followUpTaskMapBySourceTaskId,
                                    followUpTaskMapBySourceReportId
                            ).followUpStatus()
                    )
                    .filter(status -> status != null && !status.isBlank())
                    .toList());
        }
        statuses.addAll(reports.stream()
                .filter(report -> report.getTaskId() == null || !coveredRiskTaskIds.contains(report.getTaskId()))
                .filter(this::hasReportRiskDisposition)
                .map(report -> followUpManager.resolveSummary(
                                taskMap.get(report.getTaskId()),
                                report,
                                followUpTaskMapBySourceTaskId,
                                followUpTaskMapBySourceReportId
                        ).followUpStatus()
                )
                .filter(status -> status != null && !status.isBlank())
                .toList());
        return statuses;
    }

    private List<String> collectStrategyStatuses(Set<String> taskIds,
                                                 Set<String> reportIds,
                                                 List<TaskReportProjection> reports,
                                                 Map<String, ResearchTaskDO> taskMap,
                                                 Map<String, TaskReportProjection> latestReportMap,
                                                 Map<String, ResearchWorkbenchStrategyProjection> strategySignalMap) {
        List<ResearchTaskDO> followUpTasks = readManager.loadFollowUpTasks("STRATEGY_SIGNAL", taskIds, reportIds);
        Map<String, List<ResearchTaskDO>> followUpTaskMapBySourceTaskId = readManager.groupFollowUpTasksBySourceTaskId(followUpTasks);
        Map<String, List<ResearchTaskDO>> followUpTaskMapBySourceReportId = readManager.groupFollowUpTasksBySourceReportId(followUpTasks);
        Set<String> coveredStrategyTaskIds = strategySignalMap == null
                ? Collections.emptySet()
                : strategySignalMap.keySet().stream()
                .filter(taskId -> taskId != null && !taskId.isBlank())
                .collect(Collectors.toSet());
        List<String> statuses = new ArrayList<>();
        if (strategySignalMap != null && !strategySignalMap.isEmpty()) {
            statuses.addAll(strategySignalMap.values().stream()
                    .map(signal -> followUpManager.resolveSummary(
                                    taskMap.get(signal.taskId()),
                                    latestReportMap.get(signal.taskId()),
                                    followUpTaskMapBySourceTaskId,
                                    followUpTaskMapBySourceReportId
                            ).followUpStatus()
                    )
                    .filter(status -> status != null && !status.isBlank())
                    .toList());
        }
        statuses.addAll(reports.stream()
                .filter(report -> report.getTaskId() == null || !coveredStrategyTaskIds.contains(report.getTaskId()))
                .filter(this::hasReportStrategySignalDisposition)
                .map(report -> followUpManager.resolveSummary(
                                taskMap.get(report.getTaskId()),
                                report,
                                followUpTaskMapBySourceTaskId,
                                followUpTaskMapBySourceReportId
                        ).followUpStatus()
                )
                .filter(status -> status != null && !status.isBlank())
                .toList());
        return statuses;
    }

    private List<String> collectMarketIntelligenceStatuses(Set<String> taskIds,
                                                           Set<String> reportIds,
                                                           List<TaskReportProjection> reports,
                                                           Map<String, ResearchTaskDO> taskMap) {
        List<ResearchTaskDO> followUpTasks = readManager.loadFollowUpTasks("MARKET_INTELLIGENCE", taskIds, reportIds);
        Map<String, List<ResearchTaskDO>> followUpTaskMapBySourceTaskId = readManager.groupFollowUpTasksBySourceTaskId(followUpTasks);
        Map<String, List<ResearchTaskDO>> followUpTaskMapBySourceReportId = readManager.groupFollowUpTasksBySourceReportId(followUpTasks);
        return reports.stream()
                .filter(report -> hasMarketIntelligenceDisposition(report, taskMap.get(report.getTaskId())))
                .map(report -> followUpManager.resolveSummary(
                                taskMap.get(report.getTaskId()),
                                report,
                                followUpTaskMapBySourceTaskId,
                                followUpTaskMapBySourceReportId
                        ).followUpStatus()
                )
                .filter(status -> status != null && !status.isBlank())
                .toList();
    }

    private boolean hasReportRiskDisposition(TaskReportProjection report) {
        if (report == null) {
            return false;
        }
        boolean needHumanReview = report.getNeedHumanReview() != null && report.getNeedHumanReview() == 1;
        return needHumanReview
                || !ruleManager.readTextList(report.getRiskWarnings()).isEmpty()
                || !ruleManager.readPreferredTextList(report.getRevisedRiskPoints(), report.getRiskPoints()).isEmpty();
    }

    private boolean hasReportStrategySignalDisposition(TaskReportProjection report) {
        if (report == null) {
            return false;
        }
        String strategySummary = ruleManager.resolveDisplaySummary(report.getRevisedSummary(), report.getSummary());
        return (strategySummary != null && !strategySummary.isBlank())
                || !ruleManager.readTextList(report.getRevisedHighlights()).isEmpty()
                || !ruleManager.readTextList(report.getHighlights()).isEmpty()
                || report.getConfidenceScore() != null;
    }

    private boolean hasMarketIntelligenceDisposition(TaskReportProjection report, ResearchTaskDO task) {
        if (report == null) {
            return false;
        }
        String summary = ruleManager.resolveDisplaySummary(report.getRevisedSummary(), report.getSummary());
        String reportType = ruleManager.resolveReportType(report, task);
        return (summary != null && !summary.isBlank())
                || (report.getResultRef() != null && !report.getResultRef().isBlank())
                || (reportType != null && !reportType.isBlank());
    }

    private ResearchWorkbenchDispositionSummaryVO buildDispositionSummary(String domainCode, List<String> followUpStatuses) {
        List<String> safeStatuses = followUpStatuses == null ? List.of() : followUpStatuses;
        ResearchWorkbenchDispositionSummaryVO vo = new ResearchWorkbenchDispositionSummaryVO();
        vo.setDomainCode(domainCode);
        vo.setTotalCount((long) safeStatuses.size());
        vo.setNotTrackedCount(safeStatuses.stream().filter("NOT_TRACKED"::equals).count());
        vo.setTrackingCount(safeStatuses.stream().filter("TRACKING"::equals).count());
        vo.setCompletedCount(safeStatuses.stream().filter("COMPLETED"::equals).count());
        vo.setFailedCount(safeStatuses.stream().filter("FAILED"::equals).count());
        return vo;
    }
}
