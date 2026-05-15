package com.quant.aiorchestrationservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.entity.ResearchReportDO;
import com.quant.aiorchestrator.domain.entity.RiskWarningDO;
import com.quant.aiorchestrator.domain.entity.StrategySignalDO;
import com.quant.aiorchestrator.mapper.ReportEvidenceRefMapper;
import com.quant.aiorchestrator.mapper.RiskWarningDetailMapper;
import com.quant.aiorchestrator.mapper.RiskWarningMapper;
import com.quant.aiorchestrator.mapper.StrategySignalFactorMapper;
import com.quant.aiorchestrator.mapper.StrategySignalMapper;
import com.quant.aiorchestrator.service.TaskDomainEventPublisherService;
import com.quant.aiorchestrator.service.TaskMessageLogService;
import com.quant.common.messaging.KafkaTopicConstants;
import com.quant.common.messaging.MessageTypeConstants;
import com.quant.common.model.enums.TaskStatusEnum;
import com.quant.common.model.message.AiTaskResultMessage;
import com.quant.common.model.message.MessageEnvelope;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskDomainEventPublisherServiceTests {

    @Test
    void publishesRiskSignalAndReportEventsForSuccessfulResult() throws Exception {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        RiskWarningMapper riskWarningMapper = mock(RiskWarningMapper.class);
        RiskWarningDetailMapper riskWarningDetailMapper = mock(RiskWarningDetailMapper.class);
        StrategySignalMapper strategySignalMapper = mock(StrategySignalMapper.class);
        StrategySignalFactorMapper strategySignalFactorMapper = mock(StrategySignalFactorMapper.class);
        ReportEvidenceRefMapper reportEvidenceRefMapper = mock(ReportEvidenceRefMapper.class);
        TaskMessageLogService taskMessageLogService = mock(TaskMessageLogService.class);

        TaskDomainEventPublisherService service = new TaskDomainEventPublisherService(
                objectMapper,
                kafkaTemplate,
                riskWarningMapper,
                riskWarningDetailMapper,
                strategySignalMapper,
                strategySignalFactorMapper,
                reportEvidenceRefMapper,
                taskMessageLogService
        );

        AiTaskResultMessage message = buildSuccessMessage();
        ResearchReportDO report = buildReport();
        RiskWarningDO warning = buildWarning();
        StrategySignalDO signal = buildSignal();

        when(riskWarningMapper.selectOne(any())).thenReturn(warning);
        when(riskWarningDetailMapper.selectCount(any())).thenReturn(4L);
        when(strategySignalMapper.selectOne(any())).thenReturn(signal);
        when(strategySignalFactorMapper.selectCount(any())).thenReturn(3L);
        when(reportEvidenceRefMapper.selectCount(any())).thenReturn(6L);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        doReturn(CompletableFuture.completedFuture(null)).when(kafkaTemplate).send(anyString(), anyString(), anyString());

        service.publishGeneratedEvents(message, report);

        verify(kafkaTemplate, times(3)).send(anyString(), anyString(), anyString());

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MessageEnvelope> messageCaptor = ArgumentCaptor.forClass(MessageEnvelope.class);
        verify(taskMessageLogService, times(3)).recordProduced(topicCaptor.capture(), messageCaptor.capture());

        assertEquals(KafkaTopicConstants.RISK_WARNING_GENERATED, topicCaptor.getAllValues().get(0));
        assertEquals(KafkaTopicConstants.STRATEGY_SIGNAL_GENERATED, topicCaptor.getAllValues().get(1));
        assertEquals(KafkaTopicConstants.REPORT_GENERATED, topicCaptor.getAllValues().get(2));

        assertEquals(MessageTypeConstants.RISK_WARNING_GENERATED, messageCaptor.getAllValues().get(0).getMessageType());
        assertEquals(MessageTypeConstants.STRATEGY_SIGNAL_GENERATED, messageCaptor.getAllValues().get(1).getMessageType());
        assertEquals(MessageTypeConstants.REPORT_GENERATED, messageCaptor.getAllValues().get(2).getMessageType());
        assertTrue(messageCaptor.getAllValues().stream().allMatch(item -> "task-1".equals(item.getTaskId())));
    }

    private AiTaskResultMessage buildSuccessMessage() {
        AiTaskResultMessage message = new AiTaskResultMessage();
        message.setTraceId("trace-1");
        message.setTaskId("task-1");
        message.setTenantId("tenant-a");
        message.setVersion("1.0");
        message.setRetryCount(0);

        AiTaskResultMessage.ResultPayload payload = new AiTaskResultMessage.ResultPayload();
        payload.setFinalStatus(TaskStatusEnum.SUCCESS.name());
        payload.setSourceTaskId("task-source");
        payload.setSourceReportId("report-source");
        payload.setSourceEventId("event-source");
        payload.setSourceDomain("REPORT_WORKBENCH");
        message.setPayload(payload);
        return message;
    }

    private ResearchReportDO buildReport() {
        ResearchReportDO report = new ResearchReportDO();
        report.setReportId("report-1");
        report.setTaskId("task-1");
        report.setReportType("STOCK_RESEARCH");
        report.setFinalStatus(TaskStatusEnum.SUCCESS.name());
        report.setNeedHumanReview(1);
        report.setConfidenceScore(BigDecimal.valueOf(0.91));
        report.setResultRef("result://report-1");
        return report;
    }

    private RiskWarningDO buildWarning() {
        RiskWarningDO warning = new RiskWarningDO();
        warning.setWarningId("risk-task-1");
        warning.setTaskId("task-1");
        warning.setWarningType("REPORT");
        warning.setWarningLevel("HIGH");
        warning.setEntityType("STOCK");
        warning.setEntityCode("600519");
        warning.setEntityName("贵州茅台");
        warning.setTriggerSource("REPORT_WORKBENCH");
        warning.setTriggerEventId("event-source");
        warning.setWarningSummary("risk summary");
        warning.setStatus("ACTIVE");
        warning.setReviewStatus("PENDING");
        warning.setConfidenceScore(BigDecimal.valueOf(0.91));
        return warning;
    }

    private StrategySignalDO buildSignal() {
        StrategySignalDO signal = new StrategySignalDO();
        signal.setSignalId("signal-task-1");
        signal.setTaskId("task-1");
        signal.setSignalType("STOCK_RESEARCH");
        signal.setEntityCode("600519");
        signal.setEntityName("贵州茅台");
        signal.setSignalDate(LocalDate.of(2026, 5, 7));
        signal.setSignalScore(72);
        signal.setSignalLevel("MEDIUM");
        signal.setSignalDirection("NEUTRAL");
        signal.setStatus("ACTIVE");
        signal.setSourceEventId("event-source");
        signal.setConfidenceScore(BigDecimal.valueOf(0.91));
        return signal;
    }
}
