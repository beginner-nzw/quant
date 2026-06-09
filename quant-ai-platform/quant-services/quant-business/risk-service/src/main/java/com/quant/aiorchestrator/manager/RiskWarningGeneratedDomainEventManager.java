package com.quant.aiorchestrator.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quant.aiorchestrator.domain.entity.RiskWarningDO;
import com.quant.aiorchestrator.domain.entity.RiskWarningDetailDO;
import com.quant.aiorchestrator.mapper.RiskWarningDetailMapper;
import com.quant.aiorchestrator.mapper.RiskWarningMapper;
import com.quant.aiorchestrator.risk.RiskStrategyStableContract;
import com.quant.common.model.message.AiTaskResultMessage;
import com.quant.common.model.message.GeneratedDomainEvent;
import com.quant.common.model.message.MessageEnvelope;
import com.quant.common.model.message.RiskWarningGeneratedMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RiskWarningGeneratedDomainEventManager implements RiskWarningGeneratedDomainEventPort {

    private static final String SERVICE_NAME = "ai-orchestration-service";
    private static final String TARGET_SERVICE = "domain-event-subscribers";

    private final RiskWarningMapper riskWarningMapper;
    private final RiskWarningDetailMapper riskWarningDetailMapper;

    @Override
    public Optional<GeneratedDomainEvent> buildRiskWarningGeneratedEvent(AiTaskResultMessage message) {
        RiskWarningDO warning = riskWarningMapper.selectOne(
                new LambdaQueryWrapper<RiskWarningDO>()
                        .eq(RiskWarningDO::getTaskId, message.getTaskId())
                        .eq(RiskWarningDO::getDeleted, 0)
                        .last("limit 1")
        );
        if (warning == null) {
            return Optional.empty();
        }

        long detailCount = riskWarningDetailMapper.selectCount(
                new LambdaQueryWrapper<RiskWarningDetailDO>()
                        .eq(RiskWarningDetailDO::getWarningId, warning.getWarningId())
        );

        RiskWarningGeneratedMessage outbound = new RiskWarningGeneratedMessage();
        copyEnvelope(
                message,
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
        return Optional.of(new GeneratedDomainEvent(
                RiskStrategyStableContract.RISK_WARNING_GENERATED_TOPIC,
                warning.getWarningId(),
                outbound
        ));
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
        target.setTimestamp(source.getTimestamp() == null ? 0L : source.getTimestamp());
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

    private String defaultValue(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}
