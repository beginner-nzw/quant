package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.dto.MarketEventPageQueryDTO;
import com.quant.aiorchestrator.domain.dto.MarketIntelligencePageQueryDTO;
import com.quant.aiorchestrator.domain.vo.MarketEventIngestHistoryItemVO;
import com.quant.aiorchestrator.domain.vo.MarketEventListItemVO;
import com.quant.aiorchestrator.domain.vo.MarketEventPageVO;
import com.quant.aiorchestrator.domain.vo.MarketEventStatsVO;
import com.quant.aiorchestrator.domain.vo.MarketIntelligencePageVO;
import com.quant.aiorchestrator.domain.vo.MarketIntelligenceStatsVO;

import java.util.List;

public interface MarketQueryService {
    MarketEventPageVO pageMarketEvents(MarketEventPageQueryDTO queryDTO);

    MarketEventStatsVO getMarketEventStats();

    MarketEventListItemVO getMarketEvent(String eventId);

    List<MarketEventIngestHistoryItemVO> listMarketEventIngestHistory();

    MarketIntelligencePageVO pageMarketIntelligence(MarketIntelligencePageQueryDTO queryDTO);

    MarketIntelligenceStatsVO getMarketIntelligenceStats();
}
