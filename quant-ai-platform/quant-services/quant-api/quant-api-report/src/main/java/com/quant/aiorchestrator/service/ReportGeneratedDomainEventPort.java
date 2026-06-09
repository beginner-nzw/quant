package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.dto.ResearchReportSnapshot;
import com.quant.common.model.message.AiTaskResultMessage;
import com.quant.common.model.message.GeneratedDomainEvent;

public interface ReportGeneratedDomainEventPort {

    GeneratedDomainEvent buildReportGeneratedEvent(AiTaskResultMessage message, ResearchReportSnapshot report);
}
