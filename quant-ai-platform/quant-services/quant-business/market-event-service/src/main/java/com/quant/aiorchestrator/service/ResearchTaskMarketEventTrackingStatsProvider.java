package com.quant.aiorchestrator.service;

import com.quant.task.market.MarketEventTaskReadPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResearchTaskMarketEventTrackingStatsProvider implements MarketEventTrackingStatsProvider {

    private static final String SOURCE_DOMAIN_MARKET_EVENT = "MARKET_EVENT";

    private final MarketEventTaskReadPort marketEventTaskReadPort;

    @Override
    public long countTrackedMarketEvents() {
        try {
            return marketEventTaskReadPort.countDistinctSourceEvents(SOURCE_DOMAIN_MARKET_EVENT);
        } catch (Exception e) {
            log.warn("Failed to count tracked market events, fallback to 0", e);
            return 0L;
        }
    }
}
