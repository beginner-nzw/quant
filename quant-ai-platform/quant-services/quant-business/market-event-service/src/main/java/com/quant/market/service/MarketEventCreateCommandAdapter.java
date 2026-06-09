package com.quant.market.service;

import com.quant.aiorchestrator.domain.dto.MarketEventCreateDTO;
import com.quant.aiorchestrator.domain.entity.MarketEventDO;
import com.quant.aiorchestrator.domain.vo.MarketEventCreateResultVO;
import com.quant.aiorchestrator.manager.MarketEventCommandManager;
import com.quant.aiorchestrator.manager.MarketEventCreateManager;
import com.quant.aiorchestrator.market.MarketEventCreateCommandPort;
import com.quant.aiorchestrator.market.MarketEventDuplicateProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarketEventCreateCommandAdapter implements MarketEventCreateCommandPort {

    private final MarketEventCommandManager marketEventCommandManager;
    private final MarketEventCreateManager marketEventCreateManager;

    @Override
    public MarketEventCreateResultVO createMarketEvent(MarketEventCreateDTO dto, boolean recordHistory) {
        return marketEventCommandManager.createMarketEvent(dto, recordHistory);
    }

    @Override
    public MarketEventDuplicateProjection findDuplicatedEvent(MarketEventCreateDTO dto) {
        MarketEventDO duplicated = marketEventCreateManager.findDuplicatedEvent(dto);
        return duplicated == null ? null : new MarketEventDuplicateProjection(duplicated.getEventId());
    }
}
