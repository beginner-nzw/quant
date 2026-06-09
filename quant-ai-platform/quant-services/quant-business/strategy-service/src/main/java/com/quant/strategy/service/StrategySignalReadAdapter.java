package com.quant.strategy.service;

import com.quant.aiorchestrator.domain.entity.StrategySignalDO;
import com.quant.aiorchestrator.risk.StrategySignalReadPort;
import com.quant.aiorchestrator.risk.StrategySignalReadProjection;
import com.quant.aiorchestrator.manager.StrategySignalTaskReadManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class StrategySignalReadAdapter implements StrategySignalReadPort {

    private final StrategySignalTaskReadManager strategySignalTaskReadManager;

    @Override
    public Map<String, StrategySignalReadProjection> loadLatestStrategySignalMapByTaskIds(Set<String> taskIds) {
        return strategySignalTaskReadManager.loadLatestStrategySignalMapByTaskIds(taskIds)
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> toProjection(entry.getValue()),
                        (left, right) -> left
                ));
    }

    private StrategySignalReadProjection toProjection(StrategySignalDO signal) {
        return new StrategySignalReadProjection(
                signal.getId(),
                signal.getSignalId(),
                signal.getTaskId(),
                signal.getSignalType(),
                signal.getEntityCode(),
                signal.getEntityName(),
                signal.getSignalDate(),
                signal.getSignalScore(),
                signal.getSignalLevel(),
                signal.getSignalDirection(),
                signal.getReasonSummary(),
                signal.getConfidenceScore(),
                signal.getSourceEventId(),
                signal.getStatus(),
                signal.getCreatedAt()
        );
    }
}
