package com.quant.risk.service;

import com.quant.aiorchestrator.domain.entity.RiskWarningDO;
import com.quant.aiorchestrator.domain.entity.RiskWarningDetailDO;
import com.quant.aiorchestrator.manager.TaskReportRiskReadManager;
import com.quant.aiorchestrator.report.TaskReportRiskDetailProjection;
import com.quant.aiorchestrator.report.TaskReportRiskProjection;
import com.quant.aiorchestrator.report.TaskReportRiskProjectionProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RiskTaskReportRiskProjectionProvider implements TaskReportRiskProjectionProvider {

    private final TaskReportRiskReadManager taskReportRiskReadManager;

    @Override
    public Map<String, TaskReportRiskProjection> loadLatestRiskWarningMapByTaskIds(Set<String> taskIds) {
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
    public Map<String, List<TaskReportRiskDetailProjection>> loadRiskWarningDetailMapByWarningIds(Set<String> warningIds) {
        return taskReportRiskReadManager.loadRiskWarningDetailMapByWarningIds(warningIds)
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream().map(this::toDetailProjection).toList(),
                        (left, right) -> left
                ));
    }

    private TaskReportRiskProjection toProjection(RiskWarningDO riskWarning) {
        return new TaskReportRiskProjection(
                riskWarning.getWarningId(),
                riskWarning.getTaskId(),
                riskWarning.getWarningLevel(),
                riskWarning.getWarningSummary(),
                riskWarning.getWarningReason(),
                riskWarning.getSuggestAction(),
                riskWarning.getReviewStatus()
        );
    }

    private TaskReportRiskDetailProjection toDetailProjection(RiskWarningDetailDO detail) {
        return new TaskReportRiskDetailProjection(
                detail.getWarningId(),
                detail.getDetailDesc()
        );
    }
}
