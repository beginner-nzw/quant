package com.quant.risk.service;

import com.quant.aiorchestrator.domain.entity.RiskWarningDO;
import com.quant.aiorchestrator.manager.TaskReportRiskReadManager;
import com.quant.aiorchestrator.report.ReportCenterRiskProjection;
import com.quant.aiorchestrator.report.ReportCenterRiskProjectionProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RiskReportCenterRiskProjectionProvider implements ReportCenterRiskProjectionProvider {

    private final TaskReportRiskReadManager taskReportRiskReadManager;

    @Override
    public Map<String, ReportCenterRiskProjection> loadLatestRiskWarningMapByTaskIds(Set<String> taskIds) {
        return taskReportRiskReadManager.loadLatestRiskWarningMapByTaskIds(taskIds)
                .values()
                .stream()
                .collect(Collectors.toMap(
                        RiskWarningDO::getTaskId,
                        this::toProjection,
                        (left, right) -> left
                ));
    }

    private ReportCenterRiskProjection toProjection(RiskWarningDO riskWarning) {
        return new ReportCenterRiskProjection(
                riskWarning.getTaskId(),
                riskWarning.getWarningLevel(),
                riskWarning.getSuggestAction(),
                riskWarning.getReviewStatus(),
                riskWarning.getCreatedAt()
        );
    }
}
