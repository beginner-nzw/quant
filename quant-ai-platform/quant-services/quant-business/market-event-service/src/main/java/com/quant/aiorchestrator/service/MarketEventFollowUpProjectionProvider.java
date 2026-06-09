package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.projection.MarketEventFollowUpProjection;

import java.util.List;
import java.util.Map;

public interface MarketEventFollowUpProjectionProvider {

    Map<String, MarketEventFollowUpProjection> loadFollowUpProjectionMap(List<String> eventIds);
}
