package com.quant.aiorchestrator.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quant.aiorchestrator.domain.entity.MarketEventDO;
import com.quant.aiorchestrator.mapper.MarketEventMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class MarketEventAutoTriggerEventLoaderManager {

    private final MarketEventMapper marketEventMapper;

    public MarketEventDO loadEvent(String eventId) {
        if (!StringUtils.hasText(eventId)) {
            return null;
        }
        return marketEventMapper.selectOne(
                new LambdaQueryWrapper<MarketEventDO>()
                        .eq(MarketEventDO::getEventId, eventId)
                        .eq(MarketEventDO::getDeleted, 0)
                        .last("limit 1")
        );
    }
}
