package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.entity.ResearchReportDO;
import com.quant.common.model.message.AiTaskResultMessage;

public interface AiResultDomainProjectionService {
    void project(AiTaskResultMessage message, ResearchReportDO report);
}
