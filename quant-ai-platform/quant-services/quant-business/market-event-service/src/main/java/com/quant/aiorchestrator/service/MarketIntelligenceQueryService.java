package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.dto.MarketIntelligencePageQueryDTO;
import com.quant.aiorchestrator.domain.vo.MarketIntelligencePageVO;
import com.quant.aiorchestrator.domain.vo.MarketIntelligenceStatsVO;

public interface MarketIntelligenceQueryService {
    MarketIntelligencePageVO pageMarketIntelligence(MarketIntelligencePageQueryDTO queryDTO);

    MarketIntelligenceStatsVO getMarketIntelligenceStats();
}
