package com.quant.aiorchestrator.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quant.aiorchestrator.domain.dto.StrategySignalCreateDTO;
import com.quant.aiorchestrator.domain.entity.StrategySignalFactorDO;
import com.quant.aiorchestrator.domain.vo.StrategySignalFactorItemVO;
import com.quant.aiorchestrator.mapper.StrategySignalFactorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StrategySignalFactorManager {

    private final StrategySignalFactorMapper strategySignalFactorMapper;

    public void replaceFactors(String signalId, List<StrategySignalCreateDTO.FactorDTO> factors) {
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

    public List<StrategySignalFactorItemVO> listFactors(String signalId) {
        return strategySignalFactorMapper.selectList(
                new LambdaQueryWrapper<StrategySignalFactorDO>()
                        .eq(StrategySignalFactorDO::getDeleted, 0)
                        .eq(StrategySignalFactorDO::getSignalId, signalId)
                        .orderByAsc(StrategySignalFactorDO::getId)
        ).stream().map(this::toFactorItem).toList();
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

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String limit(String value, int maxLength) {
        if (!StringUtils.hasText(value) || maxLength <= 0 || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
