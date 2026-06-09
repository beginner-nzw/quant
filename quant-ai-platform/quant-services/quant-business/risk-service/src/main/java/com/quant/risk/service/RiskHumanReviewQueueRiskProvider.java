package com.quant.risk.service;

import com.quant.aiorchestrator.audit.HumanReviewQueueRiskProjection;
import com.quant.aiorchestrator.audit.HumanReviewQueueRiskProvider;
import com.quant.aiorchestrator.domain.entity.RiskWarningDO;
import com.quant.aiorchestrator.manager.TaskReportRiskReadManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RiskHumanReviewQueueRiskProvider implements HumanReviewQueueRiskProvider {

    private final TaskReportRiskReadManager taskReportRiskReadManager;

    @Override
    public List<HumanReviewQueueRiskProjection> listHumanReviewQueueRisks() {
        return taskReportRiskReadManager.listHumanReviewQueueRisks()
                .stream()
                .map(this::toProjection)
                .toList();
    }

    private HumanReviewQueueRiskProjection toProjection(RiskWarningDO warning) {
        return new HumanReviewQueueRiskProjection(
                warning.getWarningId(),
                warning.getTaskId(),
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
}
