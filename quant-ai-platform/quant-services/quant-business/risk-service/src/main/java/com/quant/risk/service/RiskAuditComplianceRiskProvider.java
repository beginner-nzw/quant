package com.quant.risk.service;

import com.quant.aiorchestrator.audit.AuditComplianceRiskProjection;
import com.quant.aiorchestrator.audit.AuditComplianceRiskProvider;
import com.quant.aiorchestrator.domain.entity.RiskWarningDO;
import com.quant.aiorchestrator.manager.TaskReportRiskReadManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RiskAuditComplianceRiskProvider implements AuditComplianceRiskProvider {

    private final TaskReportRiskReadManager taskReportRiskReadManager;

    @Override
    public Map<String, AuditComplianceRiskProjection> loadLatestRiskWarningMapByTaskIds(Set<String> taskIds) {
        return taskReportRiskReadManager.loadLatestRiskWarningMapByTaskIds(taskIds)
                .values()
                .stream()
                .collect(Collectors.toMap(
                        RiskWarningDO::getTaskId,
                        this::toProjection,
                        (left, right) -> left
                ));
    }

    private AuditComplianceRiskProjection toProjection(RiskWarningDO warning) {
        return new AuditComplianceRiskProjection(
                warning.getTaskId(),
                warning.getWarningLevel(),
                warning.getSuggestAction(),
                warning.getReviewStatus()
        );
    }
}
