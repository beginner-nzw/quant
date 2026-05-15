package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.service.StrategySignalService;
import com.quant.aiorchestrator.service.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.dto.StrategySignalCreateDTO;
import com.quant.aiorchestrator.domain.dto.StrategySignalStatusUpdateDTO;
import com.quant.aiorchestrator.domain.entity.StrategySignalDO;
import com.quant.aiorchestrator.domain.entity.StrategySignalFactorDO;
import com.quant.aiorchestrator.domain.vo.StrategySignalFactorItemVO;
import com.quant.aiorchestrator.mapper.StrategySignalFactorMapper;
import com.quant.aiorchestrator.mapper.StrategySignalMapper;
import com.quant.common.core.exception.BizException;
import com.quant.common.redis.RedisKeyBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StrategySignalServiceImpl implements StrategySignalService {

    private static final Set<String> ALLOWED_STATUS = Set.of("ACTIVE", "ARCHIVED", "DISABLED");

    private final StrategySignalMapper strategySignalMapper;
    private final StrategySignalFactorMapper strategySignalFactorMapper;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public String createOrUpdate(StrategySignalCreateDTO dto) {
        if (dto == null) {
            throw new BizException("STRATEGY_SIGNAL_EMPTY", "策略信号内容不能为空");
        }
        if (!StringUtils.hasText(dto.getEntityCode())) {
            throw new BizException("STRATEGY_SIGNAL_ENTITY_EMPTY", "策略信号标的代码不能为空");
        }
        String signalId = StringUtils.hasText(dto.getSignalId())
                ? dto.getSignalId().trim()
                : "signal-manual-" + UUID.randomUUID();
        StrategySignalDO signal = strategySignalMapper.selectOne(
                new LambdaQueryWrapper<StrategySignalDO>()
                        .eq(StrategySignalDO::getSignalId, signalId)
                        .last("limit 1")
        );
        boolean isNew = signal == null;
        if (signal == null) {
            signal = new StrategySignalDO();
            signal.setSignalId(signalId);
            signal.setCreatedAt(LocalDateTime.now());
        }

        applySignal(signal, dto);
        if (isNew) {
            strategySignalMapper.insert(signal);
        } else {
            strategySignalMapper.updateById(signal);
        }
        replaceFactors(signalId, dto.getFactors());

        if ("ACTIVE".equals(signal.getStatus())) {
            refreshCache(signal);
        } else {
            evictCache(signal);
        }
        return signalId;
    }

    public List<StrategySignalFactorItemVO> listFactors(String signalId) {
        if (!StringUtils.hasText(signalId)) {
            throw new BizException("STRATEGY_SIGNAL_ID_EMPTY", "策略信号 ID 不能为空");
        }
        ensureSignalExists(signalId);
        return strategySignalFactorMapper.selectList(
                new LambdaQueryWrapper<StrategySignalFactorDO>()
                        .eq(StrategySignalFactorDO::getDeleted, 0)
                        .eq(StrategySignalFactorDO::getSignalId, signalId.trim())
                        .orderByAsc(StrategySignalFactorDO::getId)
        ).stream().map(this::toFactorItem).toList();
    }

    public String updateStatus(String signalId, StrategySignalStatusUpdateDTO dto) {
        if (!StringUtils.hasText(signalId)) {
            throw new BizException("STRATEGY_SIGNAL_ID_EMPTY", "策略信号 ID 不能为空");
        }
        if (dto == null || !StringUtils.hasText(dto.getStatus())) {
            throw new BizException("STRATEGY_SIGNAL_STATUS_EMPTY", "策略信号状态不能为空");
        }
        String normalizedStatus = dto.getStatus().trim().toUpperCase();
        if (!ALLOWED_STATUS.contains(normalizedStatus)) {
            throw new BizException("STRATEGY_SIGNAL_STATUS_INVALID", "策略信号状态仅支持 ACTIVE、ARCHIVED、DISABLED");
        }

        StrategySignalDO signal = ensureSignalExists(signalId);
        signal.setStatus(normalizedStatus);
        signal.setUpdatedAt(LocalDateTime.now());
        strategySignalMapper.updateById(signal);

        if ("ACTIVE".equals(normalizedStatus)) {
            refreshCache(signal);
        } else {
            evictCache(signal);
        }
        return normalizedStatus;
    }

    private void applySignal(StrategySignalDO signal, StrategySignalCreateDTO dto) {
        signal.setTaskId(limit(trimToNull(dto.getTaskId()), 64));
        signal.setSignalType(limit(defaultValue(dto.getSignalType(), "MANUAL"), 64));
        signal.setEntityCode(limit(dto.getEntityCode().trim(), 64));
        signal.setEntityName(limit(trimToNull(dto.getEntityName()), 255));
        signal.setSignalDate(dto.getSignalDate() == null ? java.time.LocalDate.now() : dto.getSignalDate());
        signal.setSignalScore(resolveSignalScore(dto.getSignalScore(), dto.getConfidenceScore()));
        signal.setSignalDirection(resolveSignalDirection(dto.getSignalDirection()));
        signal.setSignalLevel(resolveSignalLevel(dto.getSignalLevel(), signal.getSignalScore()));
        signal.setReasonSummary(trimToNull(dto.getReasonSummary()));
        signal.setConfidenceScore(clampConfidence(dto.getConfidenceScore()));
        signal.setSourceEventId(limit(trimToNull(dto.getSourceEventId()), 64));
        signal.setStatus(resolveStatus(dto.getStatus()));
        signal.setTraceId(limit(trimToNull(dto.getTraceId()), 128));
        signal.setTenantId(limit(defaultValue(dto.getTenantId(), "default"), 64));
        signal.setDeleted(0);
        signal.setUpdatedAt(LocalDateTime.now());
    }

    private void replaceFactors(String signalId, List<StrategySignalCreateDTO.FactorDTO> factors) {
        strategySignalFactorMapper.delete(
                new LambdaQueryWrapper<StrategySignalFactorDO>()
                        .eq(StrategySignalFactorDO::getSignalId, signalId)
        );
        List<StrategySignalCreateDTO.FactorDTO> safeFactors = factors == null ? List.of() : factors;
        for (StrategySignalCreateDTO.FactorDTO item : safeFactors) {
            if (item == null || isBlankFactor(item)) {
                continue;
            }
            StrategySignalFactorDO factor = new StrategySignalFactorDO();
            factor.setFactorId(UUID.randomUUID().toString());
            factor.setSignalId(signalId);
            factor.setFactorCode(limit(trimToNull(item.getFactorCode()), 64));
            factor.setFactorName(limit(trimToNull(item.getFactorName()), 128));
            factor.setFactorValue(limit(trimToNull(item.getFactorValue()), 128));
            factor.setFactorWeight(item.getFactorWeight());
            factor.setFactorConclusion(trimToNull(item.getFactorConclusion()));
            factor.setDeleted(0);
            factor.setCreatedAt(LocalDateTime.now());
            factor.setUpdatedAt(LocalDateTime.now());
            strategySignalFactorMapper.insert(factor);
        }
    }

    private StrategySignalDO ensureSignalExists(String signalId) {
        StrategySignalDO signal = strategySignalMapper.selectOne(
                new LambdaQueryWrapper<StrategySignalDO>()
                        .eq(StrategySignalDO::getSignalId, signalId.trim())
                        .eq(StrategySignalDO::getDeleted, 0)
                        .last("limit 1")
        );
        if (signal == null) {
            throw new BizException("STRATEGY_SIGNAL_NOT_FOUND", "策略信号不存在");
        }
        return signal;
    }

    private StrategySignalFactorItemVO toFactorItem(StrategySignalFactorDO factor) {
        StrategySignalFactorItemVO vo = new StrategySignalFactorItemVO();
        vo.setFactorId(factor.getFactorId());
        vo.setSignalId(factor.getSignalId());
        vo.setFactorCode(factor.getFactorCode());
        vo.setFactorName(factor.getFactorName());
        vo.setFactorValue(factor.getFactorValue());
        vo.setFactorWeight(factor.getFactorWeight());
        vo.setFactorConclusion(factor.getFactorConclusion());
        vo.setCreatedAt(factor.getCreatedAt());
        return vo;
    }

    private boolean isBlankFactor(StrategySignalCreateDTO.FactorDTO factor) {
        return !StringUtils.hasText(factor.getFactorCode())
                && !StringUtils.hasText(factor.getFactorName())
                && !StringUtils.hasText(factor.getFactorValue())
                && factor.getFactorWeight() == null
                && !StringUtils.hasText(factor.getFactorConclusion());
    }

    private String resolveStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return "ACTIVE";
        }
        String normalized = status.trim().toUpperCase();
        if (!ALLOWED_STATUS.contains(normalized)) {
            throw new BizException("STRATEGY_SIGNAL_STATUS_INVALID", "策略信号状态仅支持 ACTIVE、ARCHIVED、DISABLED");
        }
        return normalized;
    }

    private String resolveSignalDirection(String direction) {
        if (!StringUtils.hasText(direction)) {
            return "NEUTRAL";
        }
        String normalized = direction.trim().toUpperCase();
        if (!Set.of("POSITIVE", "NEUTRAL", "NEGATIVE").contains(normalized)) {
            throw new BizException("STRATEGY_SIGNAL_DIRECTION_INVALID", "策略信号方向仅支持 POSITIVE、NEUTRAL、NEGATIVE");
        }
        return normalized;
    }

    private String resolveSignalLevel(String level, Integer signalScore) {
        if (StringUtils.hasText(level)) {
            String normalized = level.trim().toUpperCase();
            if (!Set.of("STRONG", "MEDIUM", "WEAK").contains(normalized)) {
                throw new BizException("STRATEGY_SIGNAL_LEVEL_INVALID", "策略信号强度仅支持 STRONG、MEDIUM、WEAK");
            }
            return normalized;
        }
        int score = signalScore == null ? 60 : signalScore;
        if (score >= 80) {
            return "STRONG";
        }
        if (score >= 60) {
            return "MEDIUM";
        }
        return "WEAK";
    }

    private Integer resolveSignalScore(Integer signalScore, java.math.BigDecimal confidenceScore) {
        if (signalScore != null) {
            return Math.max(0, Math.min(100, signalScore));
        }
        if (confidenceScore == null) {
            return 60;
        }
        return Math.max(0, Math.min(100, confidenceScore.multiply(java.math.BigDecimal.valueOf(100)).intValue()));
    }

    private java.math.BigDecimal clampConfidence(java.math.BigDecimal confidenceScore) {
        if (confidenceScore == null) {
            return null;
        }
        if (confidenceScore.compareTo(java.math.BigDecimal.ZERO) < 0) {
            return java.math.BigDecimal.ZERO;
        }
        if (confidenceScore.compareTo(java.math.BigDecimal.ONE) > 0) {
            return java.math.BigDecimal.ONE;
        }
        return confidenceScore;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String defaultValue(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String limit(String value, int maxLength) {
        if (!StringUtils.hasText(value) || maxLength <= 0 || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private void refreshCache(StrategySignalDO signal) {
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
