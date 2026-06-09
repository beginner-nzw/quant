package com.quant.aiorchestrator.risk;

import com.quant.aiorchestrator.domain.dto.ResearchReportSnapshot;
import com.quant.common.model.message.AiTaskResultMessage;

public interface StrategySignalProjectionPort {

    void project(AiTaskResultMessage message, ResearchReportSnapshot report);
}
