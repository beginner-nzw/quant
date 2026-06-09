package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.report.TaskReportRiskDetailProjection;
import com.quant.aiorchestrator.report.TaskReportRiskProjection;
import com.quant.aiorchestrator.report.TaskReportRiskProjectionProvider;
import com.quant.aiorchestrator.risk.StrategySignalReadPort;
import com.quant.aiorchestrator.risk.StrategySignalReadProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class MarketIntelligenceReadManager {

    private final TaskReportRiskProjectionProvider taskReportRiskProjectionProvider;
    private final StrategySignalReadPort strategySignalReadPort;

    public Map<String, TaskReportRiskProjection> loadLatestRiskWarningMapByTaskIds(Set<String> taskIds) {
        return taskReportRiskProjectionProvider.loadLatestRiskWarningMapByTaskIds(taskIds);
    }

    public Map<String, StrategySignalReadProjection> loadLatestStrategySignalMapByTaskIds(Set<String> taskIds) {
        return strategySignalReadPort.loadLatestStrategySignalMapByTaskIds(taskIds);
    }

    public Map<String, List<TaskReportRiskDetailProjection>> loadRiskWarningDetailMapByWarningIds(Set<String> warningIds) {
        return taskReportRiskProjectionProvider.loadRiskWarningDetailMapByWarningIds(warningIds);
    }
}
