package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.dto.MarketIntelligencePageQueryDTO;
import com.quant.aiorchestrator.domain.vo.MarketIntelligenceListItemVO;
import com.quant.aiorchestrator.domain.vo.MarketIntelligencePageVO;
import com.quant.aiorchestrator.domain.vo.MarketIntelligenceStatsVO;
import com.quant.aiorchestrator.report.TaskReportProjection;
import com.quant.aiorchestrator.report.TaskReportReadPort;
import com.quant.aiorchestrator.report.TaskReportRiskDetailProjection;
import com.quant.aiorchestrator.report.TaskReportRiskProjection;
import com.quant.aiorchestrator.risk.StrategySignalReadProjection;
import com.quant.common.model.enums.MarketIntelligenceTypeEnum;
import com.quant.common.model.enums.ReportReviewStatusEnum;
import com.quant.task.market.MarketEventTaskProjection;
import com.quant.task.market.MarketEventTaskReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MarketIntelligenceProjectionManager {

    private final MarketEventTaskReadPort marketEventTaskReadPort;
    private final TaskReportReadPort taskReportReadPort;
    private final MarketEventFollowUpTaskSummaryManager followUpManager;
    private final MarketIntelligenceReadManager readManager;
    private final MarketIntelligenceItemAssembler itemAssembler;

    public MarketIntelligencePageVO pageMarketIntelligence(MarketIntelligencePageQueryDTO queryDTO) {
        MarketIntelligencePageQueryDTO safeQuery = queryDTO == null ? new MarketIntelligencePageQueryDTO() : queryDTO;
        int pageNum = safeQuery.getPageNum() == null || safeQuery.getPageNum() < 1 ? 1 : safeQuery.getPageNum();
        int pageSize = safeQuery.getPageSize() == null || safeQuery.getPageSize() < 1 ? 10 : safeQuery.getPageSize();

        List<MarketIntelligenceListItemVO> matchedRecords = listMarketIntelligenceRecords(safeQuery);
        int fromIndex = Math.min((pageNum - 1) * pageSize, matchedRecords.size());
        int toIndex = Math.min(fromIndex + pageSize, matchedRecords.size());

        MarketIntelligencePageVO vo = new MarketIntelligencePageVO();
        vo.setTotal((long) matchedRecords.size());
        vo.setPageNum((long) pageNum);
        vo.setPageSize((long) pageSize);
        vo.setRecords(fromIndex >= toIndex ? List.of() : matchedRecords.subList(fromIndex, toIndex));
        return vo;
    }
    public MarketIntelligenceStatsVO getMarketIntelligenceStats() {
        List<MarketIntelligenceListItemVO> records = listMarketIntelligenceRecords(new MarketIntelligencePageQueryDTO());
        MarketIntelligenceStatsVO vo = new MarketIntelligenceStatsVO();
        vo.setTotalCount((long) records.size());
        vo.setRiskAlertCount(records.stream().filter(item -> MarketIntelligenceTypeEnum.RISK_ALERT.name().equals(item.getIntelligenceType())).count());
        vo.setStrategySignalCount(records.stream().filter(item -> MarketIntelligenceTypeEnum.STRATEGY_SIGNAL.name().equals(item.getIntelligenceType())).count());
        vo.setReportInsightCount(records.stream().filter(item -> MarketIntelligenceTypeEnum.REPORT_INSIGHT.name().equals(item.getIntelligenceType())).count());
        vo.setHighPriorityCount(records.stream().filter(item -> "HIGH".equalsIgnoreCase(item.getPriority())).count());
        vo.setPendingReviewCount(records.stream().filter(item -> ReportReviewStatusEnum.PENDING.name().equals(item.getReviewStatus())).count());
        return vo;
    }

    private List<MarketIntelligenceListItemVO> listMarketIntelligenceRecords(MarketIntelligencePageQueryDTO queryDTO) {
        List<TaskReportProjection> reports = taskReportReadPort.listActiveReports();

        if (reports.isEmpty()) {
                return List.of();
        }

        Set<String> taskIds = reports.stream()
                .map(TaskReportProjection::taskId)
                .filter(taskId -> taskId != null && !taskId.isBlank())
                .collect(Collectors.toSet());

        if (taskIds.isEmpty()) {
            return List.of();
        }

        Map<String, MarketEventTaskProjection> taskMap = marketEventTaskReadPort.loadTaskMapByTaskIds(taskIds);
        Map<String, TaskReportRiskProjection> riskWarningMap = readManager.loadLatestRiskWarningMapByTaskIds(taskIds);
        Map<String, List<TaskReportRiskDetailProjection>> riskWarningDetailMap = readManager.loadRiskWarningDetailMapByWarningIds(
                riskWarningMap.values().stream()
                        .map(TaskReportRiskProjection::warningId)
                        .filter(warningId -> warningId != null && !warningId.isBlank())
                        .collect(Collectors.toSet())
        );
        Map<String, StrategySignalReadProjection> strategySignalMap = readManager.loadLatestStrategySignalMapByTaskIds(taskIds);

        List<MarketEventTaskProjection> followUpTasks = marketEventTaskReadPort.loadFollowUpTasks(
                "MARKET_INTELLIGENCE",
                taskIds,
                reports.stream()
                        .map(TaskReportProjection::reportId)
                        .filter(reportId -> reportId != null && !reportId.isBlank())
                        .collect(Collectors.toSet())
        );

        Map<String, List<MarketEventTaskProjection>> followUpTaskMapBySourceTaskId =
                groupFollowUpTasksBySourceTaskId(followUpTasks);

        Map<String, List<MarketEventTaskProjection>> followUpTaskMapBySourceReportId =
                groupFollowUpTasksBySourceReportId(followUpTasks);

        return reports.stream()
                .map(report -> {
                    TaskReportRiskProjection warning = riskWarningMap.get(report.taskId());
                    StrategySignalReadProjection signal = strategySignalMap.get(report.taskId());
                    return itemAssembler.toMarketIntelligenceItem(
                            report,
                            taskMap.get(report.taskId()),
                            resolveMarketIntelligenceFollowUpSummary(
                                    taskMap.get(report.taskId()),
                                    report,
                                    followUpTaskMapBySourceTaskId,
                                    followUpTaskMapBySourceReportId
                            ),
                            warning,
                            warning == null ? List.of() : riskWarningDetailMap.getOrDefault(warning.warningId(), List.of()),
                            signal
                    );
                })
                .filter(Objects::nonNull)
                .filter(item -> itemAssembler.matchesMarketIntelligenceQuery(item, queryDTO))
                .sorted(Comparator
                        .comparing(MarketIntelligenceListItemVO::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(MarketIntelligenceListItemVO::getConfidenceScore, Comparator.nullsLast(Comparator.reverseOrder()))
                )
                .toList();
    }

    private MarketEventFollowUpTaskSummaryManager.FollowUpSummary resolveMarketIntelligenceFollowUpSummary(
            MarketEventTaskProjection sourceTask,
            TaskReportProjection sourceReport,
            Map<String, List<MarketEventTaskProjection>> followUpTaskMapBySourceTaskId,
            Map<String, List<MarketEventTaskProjection>> followUpTaskMapBySourceReportId) {
        return followUpManager.resolveSummary(
                sourceTask,
                sourceReport,
                followUpTaskMapBySourceTaskId,
                followUpTaskMapBySourceReportId
        );
    }

    private Map<String, List<MarketEventTaskProjection>> groupFollowUpTasksBySourceTaskId(
            List<MarketEventTaskProjection> followUpTasks) {
        if (followUpTasks == null || followUpTasks.isEmpty()) {
            return Map.of();
        }
        return followUpTasks.stream()
                .filter(item -> item.sourceTaskId() != null && !item.sourceTaskId().isBlank())
                .collect(Collectors.groupingBy(MarketEventTaskProjection::sourceTaskId));
    }

    private Map<String, List<MarketEventTaskProjection>> groupFollowUpTasksBySourceReportId(
            List<MarketEventTaskProjection> followUpTasks) {
        if (followUpTasks == null || followUpTasks.isEmpty()) {
            return Map.of();
        }
        return followUpTasks.stream()
                .filter(item -> item.sourceReportId() != null && !item.sourceReportId().isBlank())
                .collect(Collectors.groupingBy(MarketEventTaskProjection::sourceReportId));
    }
}
