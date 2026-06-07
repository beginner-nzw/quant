package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.entity.MarketEventDO;

public interface MarketEventStandardizedPublisherService {
    void publish(MarketEventDO event);
}
