package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.task.workbench.ResearchWorkbenchRiskDetailProjection;
import com.quant.task.workbench.ResearchWorkbenchRiskProjection;
import com.quant.task.workbench.ResearchWorkbenchRiskProvider;
import com.quant.task.workbench.ResearchWorkbenchStrategyProjection;
import com.quant.task.workbench.ResearchWorkbenchStrategyProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ResearchWorkbenchReadManager {

    private final TaskQueryReadManager taskQueryReadManager;
    private final ResearchWorkbenchRiskProvider riskProvider;
    private final ResearchWorkbenchStrategyProvider strategyProvider;

    public Map<String, ResearchWorkbenchRiskProjection> loadLatestRiskWarningMapByTaskIds(Set<String> taskIds) {
        return riskProvider.loadLatestRiskWarningMapByTaskIds(taskIds);
    }

    public Map<String, ResearchWorkbenchStrategyProjection> loadLatestStrategySignalMapByTaskIds(Set<String> taskIds) {
        return strategyProvider.loadLatestStrategySignalMapByTaskIds(taskIds);
    }

    public Map<String, List<ResearchWorkbenchRiskDetailProjection>> loadRiskWarningDetailMapByWarningIds(Set<String> warningIds) {
        return riskProvider.loadRiskWarningDetailMapByWarningIds(warningIds);
    }

    public List<ResearchTaskDO> loadFollowUpTasks(String sourceDomain,
                                                  Set<String> sourceTaskIds,
                                                  Set<String> sourceReportIds) {
        return taskQueryReadManager.loadFollowUpTasks(sourceDomain, sourceTaskIds, sourceReportIds);
    }

    public Map<String, List<ResearchTaskDO>> groupFollowUpTasksBySourceTaskId(List<ResearchTaskDO> followUpTasks) {
        return taskQueryReadManager.groupFollowUpTasksBySourceTaskId(followUpTasks);
    }

    public Map<String, List<ResearchTaskDO>> groupFollowUpTasksBySourceReportId(List<ResearchTaskDO> followUpTasks) {
        return taskQueryReadManager.groupFollowUpTasksBySourceReportId(followUpTasks);
    }
}
