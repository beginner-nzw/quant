package com.quant.aiorchestrator.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.quant.aiorchestrator.domain.entity.MarketEventDO;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.aiorchestrator.domain.vo.MarketEventStatsVO;
import com.quant.aiorchestrator.mapper.MarketEventMapper;
import com.quant.aiorchestrator.mapper.ResearchTaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketEventStatsManager {

    private final MarketEventMapper marketEventMapper;
    private final ResearchTaskMapper researchTaskMapper;

    public MarketEventStatsVO getMarketEventStats() {
        MarketEventStatsVO vo = new MarketEventStatsVO();
        vo.setTotalCount(countMarketEvents(new LambdaQueryWrapper<MarketEventDO>()
                .eq(MarketEventDO::getDeleted, 0)));
        vo.setActiveCount(countMarketEvents(new LambdaQueryWrapper<MarketEventDO>()
                .eq(MarketEventDO::getDeleted, 0)
                .eq(MarketEventDO::getEventStatus, "ACTIVE")));
        vo.setHighImpactCount(countMarketEvents(new LambdaQueryWrapper<MarketEventDO>()
                .eq(MarketEventDO::getDeleted, 0)
                .eq(MarketEventDO::getImpactLevel, "HIGH")));
        vo.setTrackedCount(countTrackedMarketEvents());
        LocalDate today = LocalDate.now();
        vo.setTodayCount(countMarketEvents(new LambdaQueryWrapper<MarketEventDO>()
                .eq(MarketEventDO::getDeleted, 0)
                .ge(MarketEventDO::getOccurredAt, today.atStartOfDay())
                .lt(MarketEventDO::getOccurredAt, today.plusDays(1).atStartOfDay())));
        return vo;
    }

    private Long countMarketEvents(LambdaQueryWrapper<MarketEventDO> wrapper) {
        try {
            return marketEventMapper.selectCount(wrapper);
        } catch (Exception e) {
            log.warn("Failed to count market events, fallback to 0", e);
            return 0L;
        }
    }

    private Long countTrackedMarketEvents() {
        try {
            List<Object> values = researchTaskMapper.selectObjs(
                    new QueryWrapper<ResearchTaskDO>()
                            .select("COUNT(DISTINCT source_event_id)")
                            .eq("deleted", 0)
                            .eq("source_domain", "MARKET_EVENT")
                            .isNotNull("source_event_id")
            );
            if (values == null || values.isEmpty() || values.get(0) == null) {
                return 0L;
            }
            Object value = values.get(0);
            if (value instanceof Number number) {
                return number.longValue();
            }
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            log.warn("Failed to count tracked market events, fallback to 0", e);
            return 0L;
        }
    }
}
