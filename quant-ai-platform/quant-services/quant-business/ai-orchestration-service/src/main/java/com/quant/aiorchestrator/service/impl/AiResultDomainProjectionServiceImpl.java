package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.domain.dto.ResearchReportSnapshot;
import com.quant.aiorchestrator.risk.RiskWarningProjectionPort;
import com.quant.aiorchestrator.risk.StrategySignalProjectionPort;
import com.quant.aiorchestrator.service.AiResultReportProjectionService;
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

    private final RiskWarningProjectionPort riskWarningProjectionService;
    private final StrategySignalProjectionPort strategySignalProjectionService;
    private final AiResultReportProjectionService aiResultReportProjectionService;

    @Autowired(required = false)
    private ReportVersionService reportVersionService;

    @Override
    public void project(AiTaskResultMessage message, ResearchReportSnapshot report) {
        if (message == null || message.getPayload() == null || report == null) {
            return;
        }
        if (!TaskStatusEnum.SUCCESS.name().equals(message.getPayload().getFinalStatus())) {
            return;
        }

        riskWarningProjectionService.project(message);
        strategySignalProjectionService.project(message, report);
        aiResultReportProjectionService.saveReportProjection(message, report);
        if (reportVersionService != null) {
            reportVersionService.createSnapshot(report, "AI_RESULT");
        }
    }
}
