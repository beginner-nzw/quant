package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.projection.MarketEventFollowUpProjection;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@ConditionalOnMissingBean(MarketEventFollowUpProjectionProvider.class)
public class NoopMarketEventFollowUpProjectionProvider implements MarketEventFollowUpProjectionProvider {

    @Override
    public Map<String, MarketEventFollowUpProjection> loadFollowUpProjectionMap(List<String> eventIds) {
        return Map.of();
    }
}
