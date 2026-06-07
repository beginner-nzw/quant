package com.quant.aiorchestrator.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.quant.aiorchestrator.report.StableReportContract;
import com.quant.aiorchestrator.risk.RiskStrategyStableContract;
import com.quant.common.model.enums.ReportReviewStatusEnum;
import com.quant.common.model.message.AiTaskResultMessage;
import com.quant.common.model.message.MessageEnvelope;
import com.quant.common.model.message.ReportGeneratedMessage;
import com.quant.common.model.message.RiskWarningGeneratedMessage;
import com.quant.common.model.message.StrategySignalGeneratedMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TaskGeneratedDomainEventManager {

    private static final String SERVICE_NAME = "ai-orchestration-service";
    private static final String TARGET_SERVICE = "domain-event-subscribers";

    private final RiskWarningMapper riskWarningMapper;
    private final RiskWarningDetailMapper riskWarningDetailMapper;
    private final StrategySignalMapper strategySignalMapper;
    private final StrategySignalFactorMapper strategySignalFactorMapper;
    private final ReportEvidenceRefMapper reportEvidenceRefMapper;

    public List<GeneratedDomainEvent> buildGeneratedEvents(AiTaskResultMessage message, ResearchReportDO report) {
        List<GeneratedDomainEvent> events = new ArrayList<>();
        GeneratedDomainEvent riskEvent = buildRiskWarningGeneratedEvent(message);
        if (riskEvent != null) {
            events.add(riskEvent);
        }
        GeneratedDomainEvent signalEvent = buildStrategySignalGeneratedEvent(message);
        if (signalEvent != null) {
            events.add(signalEvent);
        }
        events.add(buildReportGeneratedEvent(message, report));
        return events;
    }

    private GeneratedDomainEvent buildRiskWarningGeneratedEvent(AiTaskResultMessage message) {
        RiskWarningDO warning = riskWarningMapper.selectOne(
                new LambdaQueryWrapper<RiskWarningDO>()
                        .eq(RiskWarningDO::getTaskId, message.getTaskId())
                        .eq(RiskWarningDO::getDeleted, 0)
                        .last("limit 1")
        );
        if (warning == null) {
            return null;
        }

        long detailCount = riskWarningDetailMapper.selectCount(
                new LambdaQueryWrapper<RiskWarningDetailDO>()
                        .eq(RiskWarningDetailDO::getWarningId, warning.getWarningId())
        );

        RiskWarningGeneratedMessage outbound = buildRiskWarningGeneratedMessage(message, warning, detailCount);
        return new GeneratedDomainEvent(
                RiskStrategyStableContract.RISK_WARNING_GENERATED_TOPIC,
                warning.getWarningId(),
                outbound
        );
    }

    private GeneratedDomainEvent buildStrategySignalGeneratedEvent(AiTaskResultMessage message) {
        StrategySignalDO signal = strategySignalMapper.selectOne(
                new LambdaQueryWrapper<StrategySignalDO>()
                        .eq(StrategySignalDO::getTaskId, message.getTaskId())
                        .eq(StrategySignalDO::getDeleted, 0)
                        .last("limit 1")
        );
        if (signal == null) {
            return null;
        }

        long factorCount = strategySignalFactorMapper.selectCount(
                new LambdaQueryWrapper<StrategySignalFactorDO>()
                        .eq(StrategySignalFactorDO::getSignalId, signal.getSignalId())
        );

        StrategySignalGeneratedMessage outbound = buildStrategySignalGeneratedMessage(message, signal, factorCount);
        return new GeneratedDomainEvent(
                RiskStrategyStableContract.STRATEGY_SIGNAL_GENERATED_TOPIC,
                signal.getSignalId(),
                outbound
        );
    }

    private GeneratedDomainEvent buildReportGeneratedEvent(AiTaskResultMessage message, ResearchReportDO report) {
        long evidenceCount = reportEvidenceRefMapper.selectCount(
                new LambdaQueryWrapper<ReportEvidenceRefDO>()
                        .eq(ReportEvidenceRefDO::getReportId, report.getReportId())
        );

        ReportGeneratedMessage outbound = buildReportGeneratedMessage(message, report, evidenceCount);
        return new GeneratedDomainEvent(StableReportContract.REPORT_GENERATED_TOPIC, report.getReportId(), outbound);
    }

    private RiskWarningGeneratedMessage buildRiskWarningGeneratedMessage(AiTaskResultMessage source,
                                                                         RiskWarningDO warning,
                                                                         long detailCount) {
        RiskWarningGeneratedMessage outbound = new RiskWarningGeneratedMessage();
        copyEnvelope(
                source,
                outbound,
                RiskStrategyStableContract.RISK_WARNING_GENERATED_TOPIC,
                RiskStrategyStableContract.RISK_WARNING_GENERATED_MESSAGE_TYPE,
                warning.getWarningId(),
                warning.getEntityType() + ":" + warning.getEntityCode()
        );

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
        copyEnvelope(
                source,
                outbound,
                RiskStrategyStableContract.STRATEGY_SIGNAL_GENERATED_TOPIC,
                RiskStrategyStableContract.STRATEGY_SIGNAL_GENERATED_MESSAGE_TYPE,
                signal.getSignalId(),
                "SIGNAL:" + signal.getSignalId()
        );

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
        copyEnvelope(
                source,
                outbound,
                StableReportContract.REPORT_GENERATED_TOPIC,
                StableReportContract.REPORT_GENERATED_MESSAGE_TYPE,
                report.getReportId(),
                StableReportContract.REPORT_GENERATED_BIZ_KEY_PREFIX + report.getReportId()
        );

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

    private void copyEnvelope(AiTaskResultMessage source,
                              MessageEnvelope target,
                              String topicName,
                              String messageType,
                              String objectId,
                              String bizKey) {
        target.setMessageId(stableMessageId(topicName, source.getTaskId(), objectId, source.getVersion()));
        target.setTraceId(source.getTraceId());
        target.setTaskId(source.getTaskId());
        target.setEventId(source.getPayload().getSourceEventId());
        target.setMessageType(messageType);
        target.setSourceService(SERVICE_NAME);
        target.setTargetService(TARGET_SERVICE);
        target.setTenantId(defaultValue(source.getTenantId(), "default"));
        target.setBizKey(bizKey);
        target.setTimestamp(stableTimestamp(source));
        target.setVersion(defaultValue(source.getVersion(), "1.0"));
        target.setRetryCount(source.getRetryCount() == null ? 0 : source.getRetryCount());
    }

    private String stableMessageId(String topicName, String taskId, String objectId, String version) {
        String seed = defaultValue(topicName, "UNKNOWN_TOPIC")
                + "|" + defaultValue(taskId, "UNKNOWN_TASK")
                + "|" + defaultValue(objectId, "UNKNOWN_OBJECT")
                + "|" + defaultValue(version, "1.0");
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private Long stableTimestamp(AiTaskResultMessage source) {
        if (source.getTimestamp() != null) {
            return source.getTimestamp();
        }
        return 0L;
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

    public record GeneratedDomainEvent(String topicName, String key, MessageEnvelope message) {
    }
}
