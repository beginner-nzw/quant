package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.dto.ResearchReportSnapshot;
import com.quant.common.model.message.AiTaskResultMessage;

public interface AiResultReportService {

    ResearchReportSnapshot saveReport(AiTaskResultMessage message);
}
