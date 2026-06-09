package com.quant.aiorchestrator.manager;

import com.quant.common.model.message.AiTaskResultMessage;
import com.quant.common.model.message.GeneratedDomainEvent;

import java.util.Optional;

public interface StrategySignalGeneratedDomainEventPort {

    Optional<GeneratedDomainEvent> buildStrategySignalGeneratedEvent(AiTaskResultMessage message);
}
