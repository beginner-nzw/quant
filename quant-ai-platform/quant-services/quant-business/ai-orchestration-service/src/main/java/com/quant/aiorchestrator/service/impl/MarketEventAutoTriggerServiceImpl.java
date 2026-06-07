package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.domain.entity.MarketEventDO;
import com.quant.aiorchestrator.manager.MarketEventAutoTriggerEventLoaderManager;
import com.quant.aiorchestrator.manager.MarketEventAutoTriggerOrchestrationManager;
import com.quant.aiorchestrator.service.MarketEventAutoTriggerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MarketEventAutoTriggerServiceImpl implements MarketEventAutoTriggerService {

    private final MarketEventAutoTriggerEventLoaderManager eventLoaderManager;
    private final MarketEventAutoTriggerOrchestrationManager orchestrationManager;

    @Override
    public MarketEventDO prepareAutoTrigger(MarketEventDO event) {
        return orchestrationManager.prepareAutoTrigger(event);
    }

    @Override
    public MarketEventDO loadEvent(String eventId) {
        return eventLoaderManager.loadEvent(eventId);
    }

    @Override
    public boolean isPendingAutoTrigger(MarketEventDO event) {
        return orchestrationManager.isPendingAutoTrigger(event);
    }

    @Override
    public String executePendingAutoTrigger(MarketEventDO event) {
        return orchestrationManager.executePendingAutoTrigger(event);
    }

    @Override
    public boolean shouldCountAsQueued(String autoTriggerStatus) {
        return orchestrationManager.shouldCountAsQueued(autoTriggerStatus);
    }
}
