package com.quant.aiorchestrator.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quant.aiorchestrator.domain.entity.MarketEventDO;
import com.quant.aiorchestrator.domain.vo.MarketEventStatsVO;
import com.quant.aiorchestrator.mapper.MarketEventMapper;
import com.quant.aiorchestrator.service.MarketEventTrackingStatsProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketEventStatsManager {

    private final MarketEventMapper marketEventMapper;
    private final MarketEventTrackingStatsProvider marketEventTrackingStatsProvider;

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
        return marketEventTrackingStatsProvider == null ? 0L : marketEventTrackingStatsProvider.countTrackedMarketEvents();
    }
}
