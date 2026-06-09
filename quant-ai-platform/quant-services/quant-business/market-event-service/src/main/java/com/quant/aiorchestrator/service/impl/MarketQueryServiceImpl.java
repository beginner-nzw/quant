package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.domain.dto.MarketIntelligencePageQueryDTO;
import com.quant.aiorchestrator.domain.vo.MarketIntelligencePageVO;
import com.quant.aiorchestrator.domain.vo.MarketIntelligenceStatsVO;
import com.quant.aiorchestrator.manager.MarketIntelligenceProjectionManager;
import com.quant.aiorchestrator.service.MarketIntelligenceQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MarketQueryServiceImpl implements MarketIntelligenceQueryService {

    private final MarketIntelligenceProjectionManager marketIntelligenceProjectionManager;

    @Override
    public MarketIntelligencePageVO pageMarketIntelligence(MarketIntelligencePageQueryDTO queryDTO) {
        return marketIntelligenceProjectionManager.pageMarketIntelligence(queryDTO);
    }

    @Override
    public MarketIntelligenceStatsVO getMarketIntelligenceStats() {
        return marketIntelligenceProjectionManager.getMarketIntelligenceStats();
    }
}
