package com.quant.aiorchestrator.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quant.aiorchestrator.domain.entity.StrategySignalDO;
import com.quant.aiorchestrator.domain.entity.StrategySignalFactorDO;
import com.quant.aiorchestrator.mapper.StrategySignalFactorMapper;
import com.quant.aiorchestrator.mapper.StrategySignalMapper;
import com.quant.aiorchestrator.report.TaskReportProjection;
import com.quant.aiorchestrator.report.TaskReportReadPort;
import com.quant.aiorchestrator.report.TaskReportRiskDetailProjection;
import com.quant.aiorchestrator.report.TaskReportRiskProjection;
import com.quant.aiorchestrator.report.TaskReportRiskProjectionProvider;
import com.quant.task.risk.RiskWarningTaskProjection;
import com.quant.task.risk.RiskWarningTaskReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class StrategySignalReadManager {

    private static final String SOURCE_DOMAIN_STRATEGY_SIGNAL = "STRATEGY_SIGNAL";

    private final RiskWarningTaskReadPort taskReadPort;
    private final TaskReportReadPort taskReportReadPort;
    private final TaskReportRiskProjectionProvider riskProjectionProvider;
    private final StrategySignalMapper strategySignalMapper;
    private final StrategySignalFactorMapper strategySignalFactorMapper;

    public List<StrategySignalDO> loadActiveStrategySignals() {
        return strategySignalMapper.selectList(
                new LambdaQueryWrapper<StrategySignalDO>()
                        .eq(StrategySignalDO::getDeleted, 0)
                        .orderByDesc(StrategySignalDO::getSignalDate, StrategySignalDO::getCreatedAt, StrategySignalDO::getId)
        );
    }

    public List<TaskReportProjection> loadActiveReports() {
        return taskReportReadPort.listActiveReports();
    }

    public Map<String, RiskWarningTaskProjection> loadTaskMap(Set<String> taskIds) {
        return taskReadPort.loadTaskMapByTaskIds(taskIds);
    }

    public Map<String, TaskReportProjection> loadReportMapByTaskIds(Set<String> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return taskReportReadPort.listReportsByTaskIdSet(taskIds).stream().collect(Collectors.toMap(
                TaskReportProjection::taskId,
                item -> item,
                (left, right) -> left
        ));
    }

    public Map<String, TaskReportRiskProjection> loadLatestRiskWarningMapByTaskIds(Set<String> taskIds) {
        return riskProjectionProvider.loadLatestRiskWarningMapByTaskIds(taskIds);
    }

    public Map<String, List<TaskReportRiskDetailProjection>> loadRiskWarningDetailMapByWarningIds(Set<String> warningIds) {
        return riskProjectionProvider.loadRiskWarningDetailMapByWarningIds(warningIds);
    }

    public Map<String, List<StrategySignalFactorDO>> loadFactorMapBySignalIds(Set<String> signalIds) {
        if (signalIds == null || signalIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return strategySignalFactorMapper.selectList(
                new LambdaQueryWrapper<StrategySignalFactorDO>()
                        .eq(StrategySignalFactorDO::getDeleted, 0)
                        .in(StrategySignalFactorDO::getSignalId, signalIds)
                        .orderByAsc(StrategySignalFactorDO::getId)
        ).stream().collect(Collectors.groupingBy(StrategySignalFactorDO::getSignalId));
    }

    public List<RiskWarningTaskProjection> loadStrategySignalFollowUpTasks() {
        return taskReadPort.loadFollowUpTasksBySourceDomain(SOURCE_DOMAIN_STRATEGY_SIGNAL);
    }

    public Map<String, List<RiskWarningTaskProjection>> groupFollowUpTasksBySourceTaskId(List<RiskWarningTaskProjection> followUpTasks) {
        if (followUpTasks == null || followUpTasks.isEmpty()) {
            return Collections.emptyMap();
        }
        return followUpTasks.stream()
                .filter(item -> item.sourceTaskId() != null && !item.sourceTaskId().isBlank())
                .collect(Collectors.groupingBy(RiskWarningTaskProjection::sourceTaskId));
    }

    public Map<String, List<RiskWarningTaskProjection>> groupFollowUpTasksBySourceReportId(List<RiskWarningTaskProjection> followUpTasks) {
        if (followUpTasks == null || followUpTasks.isEmpty()) {
            return Collections.emptyMap();
        }
        return followUpTasks.stream()
                .filter(item -> item.sourceReportId() != null && !item.sourceReportId().isBlank())
                .collect(Collectors.groupingBy(RiskWarningTaskProjection::sourceReportId));
    }
}
