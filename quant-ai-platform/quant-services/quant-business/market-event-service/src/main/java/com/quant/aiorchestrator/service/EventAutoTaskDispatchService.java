package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.entity.MarketEventDO;
import com.quant.config.port.EventAutoTriggerConfigPort;

public interface EventAutoTaskDispatchService {

    String createFollowUpTask(MarketEventDO event, EventAutoTriggerConfigPort.EventAutoTriggerRule rule);
}
