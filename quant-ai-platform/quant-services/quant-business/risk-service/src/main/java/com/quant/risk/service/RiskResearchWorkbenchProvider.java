package com.quant.risk.service;

import com.quant.aiorchestrator.domain.entity.RiskWarningDO;
import com.quant.aiorchestrator.domain.entity.RiskWarningDetailDO;
import com.quant.aiorchestrator.manager.TaskReportRiskReadManager;
import com.quant.task.workbench.ResearchWorkbenchRiskDetailProjection;
import com.quant.task.workbench.ResearchWorkbenchRiskProjection;
import com.quant.task.workbench.ResearchWorkbenchRiskProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RiskResearchWorkbenchProvider implements ResearchWorkbenchRiskProvider {

    private final TaskReportRiskReadManager taskReportRiskReadManager;

    @Override
    public Map<String, ResearchWorkbenchRiskProjection> loadLatestRiskWarningMapByTaskIds(Set<String> taskIds) {
        return taskReportRiskReadManager.loadLatestRiskWarningMapByTaskIds(taskIds)
                .values()
                .stream()
                .collect(Collectors.toMap(
                        RiskWarningDO::getTaskId,
                        this::toProjection,
                        (left, right) -> left
                ));
    }

    @Override
    public Map<String, List<ResearchWorkbenchRiskDetailProjection>> loadRiskWarningDetailMapByWarningIds(Set<String> warningIds) {
        return taskReportRiskReadManager.loadRiskWarningDetailMapByWarningIds(warningIds)
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream().map(this::toDetailProjection).toList(),
                        (left, right) -> left
                ));
    }

    private ResearchWorkbenchRiskProjection toProjection(RiskWarningDO warning) {
        return new ResearchWorkbenchRiskProjection(
                warning.getTaskId(),
                warning.getWarningId(),
                warning.getWarningLevel(),
                warning.getWarningSummary(),
                warning.getWarningReason(),
                warning.getSuggestAction(),
                warning.getReviewStatus(),
                warning.getReviewerId(),
                warning.getReviewTime(),
                warning.getCreatedAt()
        );
    }

    private ResearchWorkbenchRiskDetailProjection toDetailProjection(RiskWarningDetailDO detail) {
        return new ResearchWorkbenchRiskDetailProjection(
                detail.getWarningId(),
                detail.getDetailDesc(),
                detail.getIndicatorName(),
                detail.getIndicatorValue()
        );
    }
}
