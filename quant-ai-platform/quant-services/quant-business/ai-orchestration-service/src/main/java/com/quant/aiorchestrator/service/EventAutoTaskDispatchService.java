package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.entity.MarketEventDO;

public interface EventAutoTaskDispatchService {

    String createFollowUpTask(MarketEventDO event, EventAutoTriggerConfigService.EventAutoTriggerRule rule);
}
