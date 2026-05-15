package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.service.TaskDomainEventPublisherService;
import com.quant.aiorchestrator.service.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.entity.ReportEvidenceRefDO;
import com.quant.aiorchestrator.domain.entity.ResearchReportDO;
import com.quant.aiorchestrator.domain.entity.RiskWarningDO;
import com.quant.aiorchestrator.domain.entity.RiskWarningDetailDO;
import com.quant.aiorchestrator.domain.entity.StrategySignalDO;
import com.quant.aiorchestrator.domain.entity.StrategySignalFactorDO;
import com.quant.aiorchestrator.mapper.ReportEvidenceRefMapper;
import com.quant.aiorchestrator.mapper.RiskWarningDetailMapper;
import com.quant.aiorchestrator.mapper.RiskWarningMapper;
import com.quant.aiorchestrator.mapper.StrategySignalFactorMapper;
import com.quant.aiorchestrator.mapper.StrategySignalMapper;
import com.quant.common.messaging.KafkaTopicConstants;
import com.quant.common.messaging.MessageTypeConstants;
import com.quant.common.model.enums.ReportReviewStatusEnum;
import com.quant.common.model.enums.TaskStatusEnum;
import com.quant.common.model.message.AiTaskResultMessage;
import com.quant.common.model.message.MessageEnvelope;
import com.quant.common.model.message.ReportGeneratedMessage;
import com.quant.common.model.message.RiskWarningGeneratedMessage;
import com.quant.common.model.message.StrategySignalGeneratedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskDomainEventPublisherServiceImpl implements TaskDomainEventPublisherService {

    private static final String SERVICE_NAME = "ai-orchestration-service";
    private static final String TARGET_SERVICE = "domain-event-subscribers";

    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final RiskWarningMapper riskWarningMapper;
    private final RiskWarningDetailMapper riskWarningDetailMapper;
    private final StrategySignalMapper strategySignalMapper;
    private final StrategySignalFactorMapper strategySignalFactorMapper;
    private final ReportEvidenceRefMapper reportEvidenceRefMapper;
    private final TaskMessageLogService taskMessageLogService;

    public void publishGeneratedEvents(AiTaskResultMessage message, ResearchReportDO report) {
        if (message == null || message.getPayload() == null || report == null) {
            return;
        }
        if (!TaskStatusEnum.SUCCESS.name().equals(message.getPayload().getFinalStatus())) {
            return;
        }

        publishRiskWarningGenerated(message);
        publishStrategySignalGenerated(message);
        publishReportGenerated(message, report);
    }

    private void publishRiskWarningGenerated(AiTaskResultMessage message) {
        RiskWarningDO warning = riskWarningMapper.selectOne(
                new LambdaQueryWrapper<RiskWarningDO>()
                        .eq(RiskWarningDO::getTaskId, message.getTaskId())
                        .eq(RiskWarningDO::getDeleted, 0)
                        .last("limit 1")
        );
        if (warning == null) {
            return;
        }

        long detailCount = riskWarningDetailMapper.selectCount(
                new LambdaQueryWrapper<RiskWarningDetailDO>()
                        .eq(RiskWarningDetailDO::getWarningId, warning.getWarningId())
        );

        RiskWarningGeneratedMessage outbound = buildRiskWarningGeneratedMessage(message, warning, detailCount);
        send(KafkaTopicConstants.RISK_WARNING_GENERATED, warning.getWarningId(), outbound);
    }

    private void publishStrategySignalGenerated(AiTaskResultMessage message) {
        StrategySignalDO signal = strategySignalMapper.selectOne(
                new LambdaQueryWrapper<StrategySignalDO>()
                        .eq(StrategySignalDO::getTaskId, message.getTaskId())
                        .eq(StrategySignalDO::getDeleted, 0)
                        .last("limit 1")
        );
        if (signal == null) {
            return;
        }

        long factorCount = strategySignalFactorMapper.selectCount(
                new LambdaQueryWrapper<StrategySignalFactorDO>()
                        .eq(StrategySignalFactorDO::getSignalId, signal.getSignalId())
        );

        StrategySignalGeneratedMessage outbound = buildStrategySignalGeneratedMessage(message, signal, factorCount);
        send(KafkaTopicConstants.STRATEGY_SIGNAL_GENERATED, signal.getSignalId(), outbound);
    }

    private void publishReportGenerated(AiTaskResultMessage message, ResearchReportDO report) {
        long evidenceCount = reportEvidenceRefMapper.selectCount(
                new LambdaQueryWrapper<ReportEvidenceRefDO>()
                        .eq(ReportEvidenceRefDO::getReportId, report.getReportId())
        );

        ReportGeneratedMessage outbound = buildReportGeneratedMessage(message, report, evidenceCount);
        send(KafkaTopicConstants.REPORT_GENERATED, report.getReportId(), outbound);
    }

    private RiskWarningGeneratedMessage buildRiskWarningGeneratedMessage(AiTaskResultMessage source,
                                                                         RiskWarningDO warning,
                                                                         long detailCount) {
        RiskWarningGeneratedMessage outbound = new RiskWarningGeneratedMessage();
        copyEnvelope(source, outbound, MessageTypeConstants.RISK_WARNING_GENERATED, warning.getEntityType() + ":" + warning.getEntityCode());

        RiskWarningGeneratedMessage.Payload payload = new RiskWarningGeneratedMessage.Payload();
        payload.setWarningId(warning.getWarningId());
        payload.setWarningType(warning.getWarningType());
        payload.setWarningLevel(warning.getWarningLevel());
        payload.setEntityType(warning.getEntityType());
        payload.setEntityCode(warning.getEntityCode());
        payload.setEntityName(warning.getEntityName());
        payload.setTriggerSource(warning.getTriggerSource());
        payload.setTriggerEventId(warning.getTriggerEventId());
        payload.setWarningSummary(warning.getWarningSummary());
        payload.setWarningStatus(warning.getStatus());
        payload.setReviewStatus(warning.getReviewStatus());
        payload.setDetailCount((int) detailCount);
        payload.setConfidenceScore(warning.getConfidenceScore());
        outbound.setPayload(payload);
        return outbound;
    }

    private StrategySignalGeneratedMessage buildStrategySignalGeneratedMessage(AiTaskResultMessage source,
                                                                               StrategySignalDO signal,
                                                                               long factorCount) {
        StrategySignalGeneratedMessage outbound = new StrategySignalGeneratedMessage();
        copyEnvelope(source, outbound, MessageTypeConstants.STRATEGY_SIGNAL_GENERATED, "SIGNAL:" + signal.getSignalId());

        StrategySignalGeneratedMessage.Payload payload = new StrategySignalGeneratedMessage.Payload();
        payload.setSignalId(signal.getSignalId());
        payload.setSignalType(signal.getSignalType());
        payload.setEntityCode(signal.getEntityCode());
        payload.setEntityName(signal.getEntityName());
        payload.setSignalDate(formatDate(signal.getSignalDate()));
        payload.setSignalScore(signal.getSignalScore());
        payload.setSignalLevel(signal.getSignalLevel());
        payload.setSignalDirection(signal.getSignalDirection());
        payload.setSignalStatus(signal.getStatus());
        payload.setSourceEventId(signal.getSourceEventId());
        payload.setFactorCount((int) factorCount);
        payload.setConfidenceScore(signal.getConfidenceScore());
        outbound.setPayload(payload);
        return outbound;
    }

    private ReportGeneratedMessage buildReportGeneratedMessage(AiTaskResultMessage source,
                                                               ResearchReportDO report,
                                                               long evidenceCount) {
        ReportGeneratedMessage outbound = new ReportGeneratedMessage();
        copyEnvelope(source, outbound, MessageTypeConstants.REPORT_GENERATED, "REPORT:" + report.getReportId());

        ReportGeneratedMessage.Payload payload = new ReportGeneratedMessage.Payload();
        payload.setReportId(report.getReportId());
        payload.setReportType(report.getReportType());
        payload.setFinalStatus(report.getFinalStatus());
        payload.setReviewStatus(resolveReviewStatus(report.getReviewStatus()));
        payload.setNeedHumanReview(report.getNeedHumanReview() != null && report.getNeedHumanReview() == 1);
        payload.setEvidenceCount((int) evidenceCount);
        payload.setConfidenceScore(report.getConfidenceScore());
        payload.setResultRef(report.getResultRef());
        payload.setSourceTaskId(source.getPayload().getSourceTaskId());
        payload.setSourceReportId(source.getPayload().getSourceReportId());
        payload.setSourceEventId(source.getPayload().getSourceEventId());
        payload.setSourceDomain(source.getPayload().getSourceDomain());
        outbound.setPayload(payload);
        return outbound;
    }

    private void send(String topicName, String key, MessageEnvelope message) {
        try {
            String messageJson = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(topicName, key, messageJson);
            taskMessageLogService.recordProduced(topicName, message);
        } catch (Exception e) {
            taskMessageLogService.recordFailed(topicName, message, e.getMessage());
            log.warn("publish domain event failed, topic={}, taskId={}, messageId={}",
                    topicName,
                    message == null ? null : message.getTaskId(),
                    message == null ? null : message.getMessageId(),
                    e);
        }
    }

    private void copyEnvelope(AiTaskResultMessage source,
                              MessageEnvelope target,
                              String messageType,
                              String bizKey) {
        target.setMessageId(UUID.randomUUID().toString());
        target.setTraceId(source.getTraceId());
        target.setTaskId(source.getTaskId());
        target.setEventId(source.getPayload().getSourceEventId());
        target.setMessageType(messageType);
        target.setSourceService(SERVICE_NAME);
        target.setTargetService(TARGET_SERVICE);
        target.setTenantId(defaultValue(source.getTenantId(), "default"));
        target.setBizKey(bizKey);
        target.setTimestamp(System.currentTimeMillis());
        target.setVersion(defaultValue(source.getVersion(), "1.0"));
        target.setRetryCount(source.getRetryCount() == null ? 0 : source.getRetryCount());
    }

    private String resolveReviewStatus(String reviewStatus) {
        return StringUtils.hasText(reviewStatus) ? reviewStatus : ReportReviewStatusEnum.PENDING.name();
    }

    private String formatDate(LocalDate signalDate) {
        return signalDate == null ? null : signalDate.toString();
    }

    private String defaultValue(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}
