package com.quant.strategy.service;

import com.quant.aiorchestrator.domain.entity.StrategySignalDO;
import com.quant.aiorchestrator.manager.StrategySignalTaskReadManager;
import com.quant.task.workbench.ResearchWorkbenchStrategyProjection;
import com.quant.task.workbench.ResearchWorkbenchStrategyProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class StrategyResearchWorkbenchProvider implements ResearchWorkbenchStrategyProvider {

    private final StrategySignalTaskReadManager strategySignalTaskReadManager;

    @Override
    public Map<String, ResearchWorkbenchStrategyProjection> loadLatestStrategySignalMapByTaskIds(Set<String> taskIds) {
        return strategySignalTaskReadManager.loadLatestStrategySignalMapByTaskIds(taskIds)
                .values()
                .stream()
                .collect(Collectors.toMap(
                        StrategySignalDO::getTaskId,
                        this::toProjection,
                        (left, right) -> left
                ));
    }

    private ResearchWorkbenchStrategyProjection toProjection(StrategySignalDO signal) {
        return new ResearchWorkbenchStrategyProjection(
                signal.getTaskId(),
                signal.getSignalDirection(),
                signal.getSignalLevel(),
                signal.getSignalScore(),
                signal.getConfidenceScore(),
                signal.getReasonSummary()
        );
    }
}
