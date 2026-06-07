package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.domain.dto.MarketEventPageQueryDTO;
import com.quant.aiorchestrator.domain.dto.MarketIntelligencePageQueryDTO;
import com.quant.aiorchestrator.domain.vo.MarketEventIngestHistoryItemVO;
import com.quant.aiorchestrator.domain.vo.MarketEventListItemVO;
import com.quant.aiorchestrator.domain.vo.MarketEventPageVO;
import com.quant.aiorchestrator.domain.vo.MarketEventStatsVO;
import com.quant.aiorchestrator.domain.vo.MarketIntelligencePageVO;
import com.quant.aiorchestrator.domain.vo.MarketIntelligenceStatsVO;
import com.quant.aiorchestrator.manager.MarketIntelligenceProjectionManager;
import com.quant.aiorchestrator.service.MarketEventService;
import com.quant.aiorchestrator.service.MarketQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MarketQueryServiceImpl implements MarketQueryService {

    private final MarketEventService marketEventService;
    private final MarketIntelligenceProjectionManager marketIntelligenceProjectionManager;

    @Override
    public MarketEventPageVO pageMarketEvents(MarketEventPageQueryDTO queryDTO) {
        return marketEventService.pageMarketEvents(queryDTO);
    }

    @Override
    public MarketEventStatsVO getMarketEventStats() {
        return marketEventService.getMarketEventStats();
    }

    @Override
    public MarketEventListItemVO getMarketEvent(String eventId) {
        return marketEventService.getMarketEvent(eventId);
    }

    @Override
    public List<MarketEventIngestHistoryItemVO> listMarketEventIngestHistory() {
        return marketEventService.listMarketEventIngestHistory();
    }

    @Override
    public MarketIntelligencePageVO pageMarketIntelligence(MarketIntelligencePageQueryDTO queryDTO) {
        return marketIntelligenceProjectionManager.pageMarketIntelligence(queryDTO);
    }

    @Override
    public MarketIntelligenceStatsVO getMarketIntelligenceStats() {
        return marketIntelligenceProjectionManager.getMarketIntelligenceStats();
    }
}