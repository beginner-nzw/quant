package com.quant.aiorchestrator.risk;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.dto.ResearchReportSnapshot;
import com.quant.aiorchestrator.domain.entity.StrategySignalDO;
import com.quant.aiorchestrator.domain.entity.StrategySignalFactorDO;
import com.quant.aiorchestrator.mapper.StrategySignalFactorMapper;
import com.quant.aiorchestrator.mapper.StrategySignalMapper;
import com.quant.common.model.enums.SignalDirectionEnum;
import com.quant.common.model.enums.SignalStrengthEnum;
import com.quant.common.model.message.AiTaskResultMessage;
import com.quant.common.redis.RedisKeyBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.quant.common.model.projection.RiskStrategyProjectionPayloadSupport.approvedObjectList;
import static com.quant.common.model.projection.RiskStrategyProjectionPayloadSupport.approvedObjectMap;
import static com.quant.common.model.projection.RiskStrategyProjectionPayloadSupport.approvedPayloadValue;
import static com.quant.common.model.projection.RiskStrategyProjectionPayloadSupport.approvedTextList;
import static com.quant.common.model.projection.RiskStrategyProjectionPayloadSupport.defaultValue;
import static com.quant.common.model.projection.RiskStrategyProjectionPayloadSupport.firstNonBlank;
import static com.quant.common.model.projection.RiskStrategyProjectionPayloadSupport.hasApprovedPayload;
import static com.quant.common.model.projection.RiskStrategyProjectionPayloadSupport.limit;
import static com.quant.common.model.projection.RiskStrategyProjectionPayloadSupport.mergeTextList;
import static com.quant.common.model.projection.RiskStrategyProjectionPayloadSupport.normalizeText;
import static com.quant.common.model.projection.RiskStrategyProjectionPayloadSupport.normalizeTextList;

@Slf4j
@Service
@RequiredArgsConstructor
public class StrategySignalProjectionService implements StrategySignalProjectionPort {

    private final StrategySignalMapper strategySignalMapper;
    private final StrategySignalFactorMapper strategySignalFactorMapper;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void project(AiTaskResultMessage message, ResearchReportSnapshot report) {
        AiTaskResultMessage.ResultPayload payload = message.getPayload();
        String taskId = message.getTaskId();
        String signalId = buildStrategySignalId(taskId);
        if (!hasApprovedPayload(payload)) {
            markDeleted(signalId);
            return;
        }
        Map<String, Object> strategyCandidate = approvedObjectMap(payload, "strategyCandidate");
        String summary = firstNonBlank(
                normalizeText(strategyCandidate.get("summary")),
                normalizeText(approvedPayloadValue(payload, "summary"))
        );
        Double confidenceScore = firstNonNullDouble(
                toDouble(strategyCandidate.get("confidence")),
                toDouble(approvedPayloadValue(payload, "confidenceScore"))
        );
        List<String> reasons = mergeTextList(
                approvedTextList(payload, "riskWarnings"),
                normalizeTextList(approvedPayloadValue(payload, "riskPoints"))
        );
        boolean needHumanReview = Boolean.TRUE.equals(approvedPayloadValue(payload, "needHumanReview"));

        if (!StringUtils.hasText(summary) && confidenceScore == null && reasons.isEmpty()) {
            markDeleted(signalId);
            return;
        }

        StrategySignalDO entity = strategySignalMapper.selectOne(
                new LambdaQueryWrapper<StrategySignalDO>()
                        .eq(StrategySignalDO::getSignalId, signalId)
                        .last("limit 1")
        );
        boolean isNew = entity == null;
        if (entity == null) {
            entity = new StrategySignalDO();
            entity.setSignalId(signalId);
        }

        int signalScore = calculateSignalScore(confidenceScore, reasons.size(), needHumanReview);
        SignalDirectionEnum direction = resolveSignalDirection(
                normalizeText(strategyCandidate.get("direction")),
                confidenceScore,
                reasons.size(),
                needHumanReview
        );

        entity.setTaskId(taskId);
        entity.setSignalType(limit(resolveReportType(report, payload), 64));
        entity.setEntityCode(limit(defaultValue(payload.getTargetCode(), "UNKNOWN"), 64));
        entity.setEntityName(limit(payload.getTargetName(), 255));
        entity.setSignalDate(LocalDate.now());
        entity.setSignalScore(signalScore);
        entity.setSignalLevel(resolveSignalStrength(signalScore).name());
        entity.setSignalDirection(direction.name());
        entity.setReasonSummary(summary);
        entity.setConfidenceScore(toBigDecimal(confidenceScore));
        entity.setSourceEventId(limit(payload.getSourceEventId(), 64));
        entity.setStatus(limit("ACTIVE", 32));
        entity.setTraceId(limit(message.getTraceId(), 128));
        entity.setTenantId(limit(defaultValue(message.getTenantId(), "default"), 64));
        entity.setDeleted(0);

        if (isNew) {
            strategySignalMapper.insert(entity);
        } else {
            strategySignalMapper.updateById(entity);
        }
        refreshCache(entity);

        strategySignalFactorMapper.delete(
                new LambdaQueryWrapper<StrategySignalFactorDO>()
                        .eq(StrategySignalFactorDO::getSignalId, signalId)
        );
        List<Map<String, Object>> approvedFactors = approvedObjectList(payload, "strategyFactors");
        if (!approvedFactors.isEmpty()) {
            for (Map<String, Object> factor : approvedFactors) {
                insertFactor(
                        signalId,
                        defaultValue(normalizeText(factor.get("factorCode")), "AI_STRATEGY_FACTOR"),
                        defaultValue(normalizeText(factor.get("factorName")), "AI strategy factor"),
                        normalizeText(factor.get("factorValue")),
                        firstNonBlank(
                                normalizeText(factor.get("factorConclusion")),
                                normalizeText(factor.get("evidenceRefs"))
                        ),
                        firstNonNullDouble(toDouble(factor.get("factorWeight")), 0.1)
                );
            }
        } else {
            insertFactor(signalId, "CONFIDENCE", "Confidence score", confidenceScore == null ? null : String.valueOf(confidenceScore), "Model confidence projection", 0.5);
            insertFactor(signalId, "RISK_COUNT", "Risk count", String.valueOf(reasons.size()), "Risk pressure deducted from signal score", 0.3);
            if (needHumanReview) {
                insertFactor(signalId, "HUMAN_REVIEW", "Human review", "true", "Manual review requirement deducted from signal score", 0.2);
            }
        }
    }

    private void markDeleted(String signalId) {
        StrategySignalDO entity = strategySignalMapper.selectOne(
                new LambdaQueryWrapper<StrategySignalDO>()
                        .eq(StrategySignalDO::getSignalId, signalId)
                        .last("limit 1")
        );
        if (entity == null) {
            return;
        }
        entity.setDeleted(1);
        strategySignalMapper.updateById(entity);
        strategySignalFactorMapper.delete(
                new LambdaQueryWrapper<StrategySignalFactorDO>()
                        .eq(StrategySignalFactorDO::getSignalId, signalId)
        );
        evictCache(entity);
    }

    private void refreshCache(StrategySignalDO signal) {
        if (signal == null || !StringUtils.hasText(signal.getSignalId())) {
            return;
        }
        try {
            if (StringUtils.hasText(signal.getEntityCode())) {
                stringRedisTemplate.opsForValue().set(
                        RedisKeyBuilder.signalLatest(signal.getEntityCode()),
                        serialize(signal)
                );
            }
            if (signal.getSignalDate() != null && signal.getSignalScore() != null) {
                stringRedisTemplate.opsForZSet().add(
                        RedisKeyBuilder.signalRanking(signal.getSignalDate().toString()),
                        signal.getSignalId(),
                        signal.getSignalScore().doubleValue()
                );
            }
        } catch (Exception e) {
            log.warn("refresh strategy signal redis cache failed, signalId={}, taskId={}",
                    signal.getSignalId(),
                    signal.getTaskId(),
                    e);
        }
    }

    private void evictCache(StrategySignalDO signal) {
        if (signal == null || !StringUtils.hasText(signal.getSignalId())) {
            return;
        }
        try {
            if (StringUtils.hasText(signal.getEntityCode())) {
                stringRedisTemplate.delete(RedisKeyBuilder.signalLatest(signal.getEntityCode()));
            }
            if (signal.getSignalDate() != null) {
                stringRedisTemplate.opsForZSet().remove(
                        RedisKeyBuilder.signalRanking(signal.getSignalDate().toString()),
                        signal.getSignalId()
                );
            }
        } catch (Exception e) {
            log.warn("evict strategy signal redis cache failed, signalId={}, taskId={}",
                    signal.getSignalId(),
                    signal.getTaskId(),
                    e);
        }
    }

    private String serialize(StrategySignalDO signal) throws JsonProcessingException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("signalId", signal.getSignalId());
        payload.put("taskId", signal.getTaskId());
        payload.put("signalType", signal.getSignalType());
        payload.put("entityCode", signal.getEntityCode());
        payload.put("entityName", signal.getEntityName());
        payload.put("signalDate", signal.getSignalDate() == null ? null : signal.getSignalDate().toString());
        payload.put("signalScore", signal.getSignalScore());
        payload.put("signalLevel", signal.getSignalLevel());
        payload.put("signalDirection", signal.getSignalDirection());
        payload.put("reasonSummary", signal.getReasonSummary());
        payload.put("confidenceScore", signal.getConfidenceScore());
        payload.put("sourceEventId", signal.getSourceEventId());
        payload.put("status", signal.getStatus());
        payload.put("traceId", signal.getTraceId());
        payload.put("tenantId", signal.getTenantId());
        return objectMapper.writeValueAsString(payload);
    }

    private void insertFactor(String signalId,
                              String factorCode,
                              String factorName,
                              String factorValue,
                              String factorConclusion,
                              double factorWeight) {
        StrategySignalFactorDO factor = new StrategySignalFactorDO();
        factor.setFactorId(UUID.randomUUID().toString());
        factor.setSignalId(signalId);
        factor.setFactorCode(limit(factorCode, 64));
        factor.setFactorName(limit(factorName, 128));
        factor.setFactorValue(limit(factorValue, 128));
        factor.setFactorWeight(BigDecimal.valueOf(factorWeight));
        factor.setFactorConclusion(factorConclusion);
        factor.setDeleted(0);
        strategySignalFactorMapper.insert(factor);
    }

    private SignalDirectionEnum resolveSignalDirection(String preferredDirection,
                                                       Double confidenceScore,
                                                       int riskCount,
                                                       boolean needHumanReview) {
        if (StringUtils.hasText(preferredDirection)) {
            try {
                return SignalDirectionEnum.valueOf(preferredDirection.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (needHumanReview || riskCount >= 3) {
            return SignalDirectionEnum.NEGATIVE;
        }
        if (confidenceScore != null && confidenceScore >= 0.85 && riskCount == 0) {
            return SignalDirectionEnum.POSITIVE;
        }
        return SignalDirectionEnum.NEUTRAL;
    }

    private int calculateSignalScore(Double confidenceScore, int riskCount, boolean needHumanReview) {
        int score = confidenceScore == null ? 60 : (int) Math.round(Math.max(0.0, Math.min(1.0, confidenceScore)) * 100);
        score -= Math.min(40, riskCount * 8);
        if (needHumanReview) {
            score -= 10;
        }
        return Math.max(0, Math.min(100, score));
    }

    private SignalStrengthEnum resolveSignalStrength(int signalScore) {
        if (signalScore >= 80) {
            return SignalStrengthEnum.STRONG;
        }
        if (signalScore >= 60) {
            return SignalStrengthEnum.MEDIUM;
        }
        return SignalStrengthEnum.WEAK;
    }

    private String resolveReportType(ResearchReportSnapshot report, AiTaskResultMessage.ResultPayload payload) {
        if (StringUtils.hasText(report.getReportType())) {
            return report.getReportType();
        }
        if (StringUtils.hasText(payload.getTaskType())) {
            return payload.getTaskType();
        }
        return "AI_RESEARCH_SIGNAL";
    }

    private BigDecimal toBigDecimal(Double value) {
        if (value == null) {
            return null;
        }
        return BigDecimal.valueOf(Math.max(0.0, Math.min(1.0, value)));
    }

    private Double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            try {
                return Double.parseDouble(text.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private Double firstNonNullDouble(Double first, Double second) {
        return first != null ? first : second;
    }

    private String buildStrategySignalId(String taskId) {
        return "signal-" + taskId;
    }
}
