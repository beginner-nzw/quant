package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.domain.entity.ResearchReportDO;
import com.quant.aiorchestrator.manager.AiResultReportProjectionManager;
import com.quant.aiorchestrator.risk.RiskWarningProjectionService;
import com.quant.aiorchestrator.risk.StrategySignalProjectionService;
import com.quant.aiorchestrator.service.AiResultDomainProjectionService;
import com.quant.aiorchestrator.service.ReportVersionService;
import com.quant.common.model.enums.TaskStatusEnum;
import com.quant.common.model.message.AiTaskResultMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiResultDomainProjectionServiceImpl implements AiResultDomainProjectionService {

    private final RiskWarningProjectionService riskWarningProjectionService;
    private final StrategySignalProjectionService strategySignalProjectionService;
    private final AiResultReportProjectionManager aiResultReportProjectionManager;

    @Autowired(required = false)
    private ReportVersionService reportVersionService;

    @Override
    public void project(AiTaskResultMessage message, ResearchReportDO report) {
        if (message == null || message.getPayload() == null || report == null) {
            return;
        }
        if (!TaskStatusEnum.SUCCESS.name().equals(message.getPayload().getFinalStatus())) {
            return;
        }

        riskWarningProjectionService.project(message);
        strategySignalProjectionService.project(message, report);
        aiResultReportProjectionManager.saveReportProjection(message, report);
        if (reportVersionService != null) {
            reportVersionService.createSnapshot(report, "AI_RESULT");
        }
    }
}
