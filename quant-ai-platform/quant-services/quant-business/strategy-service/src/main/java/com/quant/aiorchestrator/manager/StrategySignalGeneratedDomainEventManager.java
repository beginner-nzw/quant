package com.quant.aiorchestrator.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quant.aiorchestrator.domain.entity.StrategySignalDO;
import com.quant.aiorchestrator.domain.entity.StrategySignalFactorDO;
import com.quant.aiorchestrator.mapper.StrategySignalFactorMapper;
import com.quant.aiorchestrator.mapper.StrategySignalMapper;
import com.quant.aiorchestrator.risk.RiskStrategyStableContract;
import com.quant.common.model.message.AiTaskResultMessage;
import com.quant.common.model.message.GeneratedDomainEvent;
import com.quant.common.model.message.MessageEnvelope;
import com.quant.common.model.message.StrategySignalGeneratedMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StrategySignalGeneratedDomainEventManager implements StrategySignalGeneratedDomainEventPort {

    private static final String SERVICE_NAME = "ai-orchestration-service";
    private static final String TARGET_SERVICE = "domain-event-subscribers";

    private final StrategySignalMapper strategySignalMapper;
    private final StrategySignalFactorMapper strategySignalFactorMapper;

    @Override
    public Optional<GeneratedDomainEvent> buildStrategySignalGeneratedEvent(AiTaskResultMessage message) {
        StrategySignalDO signal = strategySignalMapper.selectOne(
                new LambdaQueryWrapper<StrategySignalDO>()
                        .eq(StrategySignalDO::getTaskId, message.getTaskId())
                        .eq(StrategySignalDO::getDeleted, 0)
                        .last("limit 1")
        );
        if (signal == null) {
            return Optional.empty();
        }

        long factorCount = strategySignalFactorMapper.selectCount(
                new LambdaQueryWrapper<StrategySignalFactorDO>()
                        .eq(StrategySignalFactorDO::getSignalId, signal.getSignalId())
        );

        StrategySignalGeneratedMessage outbound = new StrategySignalGeneratedMessage();
        copyEnvelope(
                message,
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
        return Optional.of(new GeneratedDomainEvent(
                RiskStrategyStableContract.STRATEGY_SIGNAL_GENERATED_TOPIC,
                signal.getSignalId(),
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

    private String formatDate(LocalDate signalDate) {
        return signalDate == null ? null : signalDate.toString();
    }

    private String defaultValue(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}
