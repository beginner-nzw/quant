package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.entity.MarketEventDO;

public interface MarketEventAutoTriggerService {

    String AUTO_TRIGGER_DISABLED = "DISABLED";
    String AUTO_TRIGGER_NO_MATCH = "NO_MATCH";
    String AUTO_TRIGGER_SUCCESS = "SUCCESS";
    String AUTO_TRIGGER_FAILED = "FAILED";
    String AUTO_TRIGGER_WILL_TRIGGER = "WILL_TRIGGER";

    MarketEventDO prepareAutoTrigger(MarketEventDO event);

    MarketEventDO loadEvent(String eventId);

    boolean isPendingAutoTrigger(MarketEventDO event);

    String executePendingAutoTrigger(MarketEventDO event);

    boolean shouldCountAsQueued(String autoTriggerStatus);
}
