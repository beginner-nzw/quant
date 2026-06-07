package com.quant.aiorchestrator.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quant.aiorchestrator.domain.dto.StrategySignalCreateDTO;
import com.quant.aiorchestrator.domain.dto.StrategySignalStatusUpdateDTO;
import com.quant.aiorchestrator.domain.entity.StrategySignalDO;
import com.quant.aiorchestrator.domain.vo.StrategySignalFactorItemVO;
import com.quant.aiorchestrator.mapper.StrategySignalMapper;
import com.quant.common.core.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StrategySignalCommandManager {

    private static final Set<String> ALLOWED_STATUS = Set.of("ACTIVE", "ARCHIVED", "DISABLED");

    private final StrategySignalMapper strategySignalMapper;
    private final StrategySignalFactorManager strategySignalFactorManager;
    private final StrategySignalCacheManager strategySignalCacheManager;

    public String createOrUpdate(StrategySignalCreateDTO dto) {
        if (dto == null) {
            throw new BizException("STRATEGY_SIGNAL_EMPTY", "绛栫暐淇″彿鍐呭涓嶈兘涓虹┖");
        }
        if (!StringUtils.hasText(dto.getEntityCode())) {
            throw new BizException("STRATEGY_SIGNAL_ENTITY_EMPTY", "绛栫暐淇″彿鏍囩殑浠ｇ爜涓嶈兘涓虹┖");
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
        strategySignalFactorManager.replaceFactors(signalId, dto.getFactors());
        syncSignalCache(signal);
        return signalId;
    }

    public List<StrategySignalFactorItemVO> listFactors(String signalId) {
        if (!StringUtils.hasText(signalId)) {
            throw new BizException("STRATEGY_SIGNAL_ID_EMPTY", "绛栫暐淇″彿 ID 涓嶈兘涓虹┖");
        }
        ensureSignalExists(signalId);
        return strategySignalFactorManager.listFactors(signalId.trim());
    }

    public String updateStatus(String signalId, StrategySignalStatusUpdateDTO dto) {
        if (!StringUtils.hasText(signalId)) {
            throw new BizException("STRATEGY_SIGNAL_ID_EMPTY", "绛栫暐淇″彿 ID 涓嶈兘涓虹┖");
        }
        if (dto == null || !StringUtils.hasText(dto.getStatus())) {
            throw new BizException("STRATEGY_SIGNAL_STATUS_EMPTY", "绛栫暐淇″彿鐘舵€佷笉鑳戒负绌?");
        }
        String normalizedStatus = normalizeStatus(dto.getStatus());

        StrategySignalDO signal = ensureSignalExists(signalId);
        signal.setStatus(normalizedStatus);
        signal.setUpdatedAt(LocalDateTime.now());
        strategySignalMapper.updateById(signal);
        syncSignalCache(signal);
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

    private StrategySignalDO ensureSignalExists(String signalId) {
        StrategySignalDO signal = strategySignalMapper.selectOne(
                new LambdaQueryWrapper<StrategySignalDO>()
                        .eq(StrategySignalDO::getSignalId, signalId.trim())
                        .eq(StrategySignalDO::getDeleted, 0)
                        .last("limit 1")
        );
        if (signal == null) {
            throw new BizException("STRATEGY_SIGNAL_NOT_FOUND", "绛栫暐淇″彿涓嶅瓨鍦?");
        }
        return signal;
    }

    private void syncSignalCache(StrategySignalDO signal) {
        if ("ACTIVE".equals(signal.getStatus())) {
            strategySignalCacheManager.refreshCache(signal);
        } else {
            strategySignalCacheManager.evictCache(signal);
        }
    }

    private String resolveStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return "ACTIVE";
        }
        return normalizeStatus(status);
    }

    private String normalizeStatus(String status) {
        String normalized = status.trim().toUpperCase();
        if (!ALLOWED_STATUS.contains(normalized)) {
            throw new BizException("STRATEGY_SIGNAL_STATUS_INVALID", "绛栫暐淇″彿鐘舵€佷粎鏀寔 ACTIVE銆丄RCHIVED銆丏ISABLED");
        }
        return normalized;
    }

    private String resolveSignalDirection(String direction) {
        if (!StringUtils.hasText(direction)) {
            return "NEUTRAL";
        }
        String normalized = direction.trim().toUpperCase();
        if (!Set.of("POSITIVE", "NEUTRAL", "NEGATIVE").contains(normalized)) {
            throw new BizException("STRATEGY_SIGNAL_DIRECTION_INVALID", "绛栫暐淇″彿鏂瑰悜浠呮敮鎸?POSITIVE銆丯EUTRAL銆丯EGATIVE");
        }
        return normalized;
    }

    private String resolveSignalLevel(String level, Integer signalScore) {
        if (StringUtils.hasText(level)) {
            String normalized = level.trim().toUpperCase();
            if (!Set.of("STRONG", "MEDIUM", "WEAK").contains(normalized)) {
                throw new BizException("STRATEGY_SIGNAL_LEVEL_INVALID", "绛栫暐淇″彿寮哄害浠呮敮鎸?STRONG銆丮EDIUM銆乄EAK");
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
}
