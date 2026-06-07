package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.domain.entity.MarketEventDO;
import com.quant.aiorchestrator.manager.EventAutoTaskHttpDispatchManager;
import com.quant.aiorchestrator.manager.EventAutoTaskServiceActorManager;
import com.quant.aiorchestrator.service.EventAutoTaskDispatchService;
import com.quant.aiorchestrator.service.EventAutoTriggerConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventAutoTaskDispatchServiceImpl implements EventAutoTaskDispatchService {

    private final EventAutoTaskHttpDispatchManager httpDispatchManager;
    private final EventAutoTaskServiceActorManager serviceActorManager;

    @Value("${quant.ai.research-task-service-base-url:http://127.0.0.1:8081}")
    private String researchTaskServiceBaseUrl;

    @Value("${quant.security.service-actor.secret:}")
    private String serviceActorSecret;

    @Override
    public String createFollowUpTask(MarketEventDO event, EventAutoTriggerConfigService.EventAutoTriggerRule rule) {
        if (event == null || rule == null) {
            return null;
        }
        return httpDispatchManager.createFollowUpTask(
                event,
                rule,
                researchTaskServiceBaseUrl,
                serviceActorSecret
        );
    }

    public String[] buildServiceActorHeaders(MarketEventDO event) {
        return serviceActorManager.buildServiceActorHeaders(event, serviceActorSecret);
    }
}
