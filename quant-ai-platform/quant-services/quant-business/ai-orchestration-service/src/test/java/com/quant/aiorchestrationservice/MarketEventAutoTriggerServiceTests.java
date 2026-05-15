package com.quant.aiorchestrationservice;

import com.quant.aiorchestrator.domain.entity.MarketEventDO;
import com.quant.aiorchestrator.mapper.MarketEventMapper;
import com.quant.aiorchestrator.service.EventAutoTaskDispatchService;
import com.quant.aiorchestrator.service.EventAutoTriggerConfigService;
import com.quant.aiorchestrator.service.MarketEventAutoTriggerService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MarketEventAutoTriggerServiceTests {

    @Test
    void prepareAutoTriggerShouldPersistWillTriggerWhenRuleMatched() {
        MarketEventMapper marketEventMapper = mock(MarketEventMapper.class);
        EventAutoTriggerConfigService configService = mock(EventAutoTriggerConfigService.class);
        EventAutoTaskDispatchService dispatchService = mock(EventAutoTaskDispatchService.class);

        MarketEventAutoTriggerService service = new MarketEventAutoTriggerService(
                marketEventMapper,
                configService,
                dispatchService
        );

        EventAutoTriggerConfigService.EventAutoTriggerConfig config = new EventAutoTriggerConfigService.EventAutoTriggerConfig();
        config.setEnabled(true);
        EventAutoTriggerConfigService.EventAutoTriggerRule rule = new EventAutoTriggerConfigService.EventAutoTriggerRule();
        rule.setRuleCode("HIGH_IMPACT");
        when(configService.loadConfig()).thenReturn(config);
        when(configService.resolveMatchedRule("ANNOUNCEMENT", "HIGH")).thenReturn(rule);
        doAnswer(invocation -> 1).when(marketEventMapper).updateById(any(MarketEventDO.class));

        MarketEventDO event = new MarketEventDO();
        event.setId(1L);
        event.setEventId("event-1");
        event.setEventType("ANNOUNCEMENT");
        event.setImpactLevel("HIGH");

        service.prepareAutoTrigger(event);

        assertEquals("HIGH_IMPACT", event.getAutoTriggerRuleCode());
        assertEquals(MarketEventAutoTriggerService.AUTO_TRIGGER_WILL_TRIGGER, event.getAutoTriggerStatus());
        assertEquals("已进入自动触发队列", event.getAutoTriggerMessage());
    }

    @Test
    void executePendingAutoTriggerShouldPersistSuccessTaskId() {
        MarketEventMapper marketEventMapper = mock(MarketEventMapper.class);
        EventAutoTriggerConfigService configService = mock(EventAutoTriggerConfigService.class);
        EventAutoTaskDispatchService dispatchService = mock(EventAutoTaskDispatchService.class);

        MarketEventAutoTriggerService service = new MarketEventAutoTriggerService(
                marketEventMapper,
                configService,
                dispatchService
        );

        EventAutoTriggerConfigService.EventAutoTriggerRule rule = new EventAutoTriggerConfigService.EventAutoTriggerRule();
        rule.setRuleCode("HIGH_IMPACT");
        when(configService.findEnabledRuleByCode("HIGH_IMPACT")).thenReturn(rule);
        when(dispatchService.createFollowUpTask(any(MarketEventDO.class), any(EventAutoTriggerConfigService.EventAutoTriggerRule.class)))
                .thenReturn("task-1");
        doAnswer(invocation -> 1).when(marketEventMapper).updateById(any(MarketEventDO.class));

        MarketEventDO event = new MarketEventDO();
        event.setId(1L);
        event.setEventId("event-1");
        event.setAutoTriggerRuleCode("HIGH_IMPACT");
        event.setAutoTriggerStatus(MarketEventAutoTriggerService.AUTO_TRIGGER_WILL_TRIGGER);

        String taskId = service.executePendingAutoTrigger(event);

        assertEquals("task-1", taskId);
        assertEquals(MarketEventAutoTriggerService.AUTO_TRIGGER_SUCCESS, event.getAutoTriggerStatus());
        assertEquals("task-1", event.getAutoTriggerTaskId());
        assertEquals("已自动创建跟踪研究任务", event.getAutoTriggerMessage());
    }
}
