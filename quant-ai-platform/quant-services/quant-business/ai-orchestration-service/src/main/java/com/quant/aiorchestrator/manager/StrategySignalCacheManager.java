package com.quant.aiorchestrator.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.entity.StrategySignalDO;
import com.quant.common.redis.RedisKeyBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class StrategySignalCacheManager {

    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public void refreshCache(StrategySignalDO signal) {
        if (signal == null || !StringUtils.hasText(signal.getSignalId())) {
            return;
        }
        try {
            if (StringUtils.hasText(signal.getEntityCode())) {
                stringRedisTemplate.opsForValue().set(
                        RedisKeyBuilder.signalLatest(signal.getEntityCode()),
                        serializeSignal(signal)
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
            log.warn("refresh strategy signal cache failed, signalId={}", signal.getSignalId(), e);
        }
    }

    public void evictCache(StrategySignalDO signal) {
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
            log.warn("evict strategy signal cache failed, signalId={}", signal.getSignalId(), e);
        }
    }

    private String serializeSignal(StrategySignalDO signal) throws JsonProcessingException {
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
}
