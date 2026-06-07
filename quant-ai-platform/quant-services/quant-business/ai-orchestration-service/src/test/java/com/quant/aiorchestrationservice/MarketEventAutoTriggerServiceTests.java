package com.quant.aiorchestrationservice;

import com.quant.aiorchestrator.domain.entity.MarketEventAutoTriggerAttemptDO;
import com.quant.aiorchestrator.domain.entity.MarketEventDO;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.aiorchestrator.manager.MarketEventAutoTriggerAttemptManager;
import com.quant.aiorchestrator.manager.MarketEventAutoTriggerEventLoaderManager;
import com.quant.aiorchestrator.manager.MarketEventAutoTriggerOrchestrationManager;
import com.quant.aiorchestrator.mapper.MarketEventAutoTriggerAttemptMapper;
import com.quant.aiorchestrator.mapper.MarketEventMapper;
import com.quant.aiorchestrator.mapper.ResearchTaskMapper;
import com.quant.aiorchestrator.service.EventAutoTaskDispatchService;
import com.quant.aiorchestrator.service.EventAutoTriggerConfigService;
import com.quant.aiorchestrator.service.MarketEventAutoTriggerService;
import com.quant.aiorchestrator.service.impl.MarketEventAutoTriggerServiceImpl;
import com.quant.common.core.exception.BizException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketEventAutoTriggerServiceTests {

    @Test
    void prepareAutoTriggerShouldPersistWillTriggerWhenRuleMatched() {
        Fixture fixture = new Fixture();
        EventAutoTriggerConfigService.EventAutoTriggerConfig config = new EventAutoTriggerConfigService.EventAutoTriggerConfig();
        config.setEnabled(true);
        EventAutoTriggerConfigService.EventAutoTriggerRule rule = new EventAutoTriggerConfigService.EventAutoTriggerRule();
        rule.setRuleCode("HIGH_IMPACT");
        when(fixture.configService.loadConfig()).thenReturn(config);
        when(fixture.configService.resolveMatchedRule("ANNOUNCEMENT", "HIGH")).thenReturn(rule);

        MarketEventDO event = buildPendingEvent();
        event.setEventType("ANNOUNCEMENT");
        event.setImpactLevel("HIGH");

        fixture.service.prepareAutoTrigger(event);

        assertEquals("HIGH_IMPACT", event.getAutoTriggerRuleCode());
        assertEquals(MarketEventAutoTriggerService.AUTO_TRIGGER_WILL_TRIGGER, event.getAutoTriggerStatus());
        assertEquals("MATCHED_RULE:HIGH_IMPACT", event.getAutoTriggerReason());
        assertEquals(0, event.getAutoTriggerRetryCount());
        ArgumentCaptor<MarketEventAutoTriggerAttemptDO> captor = ArgumentCaptor.forClass(MarketEventAutoTriggerAttemptDO.class);
        verify(fixture.attemptMapper).insert(captor.capture());
        assertEquals(MarketEventAutoTriggerService.AUTO_TRIGGER_WILL_TRIGGER, captor.getValue().getStatus());
    }

    @Test
    void executePendingAutoTriggerShouldPersistSuccessTaskId() {
        Fixture fixture = new Fixture();
        fixture.claimSucceeds();
        EventAutoTriggerConfigService.EventAutoTriggerRule rule = fixture.rule("HIGH_IMPACT");
        when(fixture.configService.findEnabledRuleByCode("HIGH_IMPACT")).thenReturn(rule);
        when(fixture.dispatchService.createFollowUpTask(any(MarketEventDO.class), any(EventAutoTriggerConfigService.EventAutoTriggerRule.class)))
                .thenReturn("task-1");

        MarketEventDO event = buildPendingEvent();
        String taskId = fixture.service.executePendingAutoTrigger(event);

        assertEquals("task-1", taskId);
        assertEquals(MarketEventAutoTriggerService.AUTO_TRIGGER_SUCCESS, event.getAutoTriggerStatus());
        assertEquals("task-1", event.getAutoTriggerTaskId());
        assertEquals("auto follow-up task created", event.getAutoTriggerMessage());
        assertEquals("DISPATCHED_BY_RULE:HIGH_IMPACT", event.getAutoTriggerReason());
        ArgumentCaptor<MarketEventAutoTriggerAttemptDO> captor = ArgumentCaptor.forClass(MarketEventAutoTriggerAttemptDO.class);
        verify(fixture.attemptMapper, times(2)).insert(captor.capture());
        assertEquals(MarketEventAutoTriggerService.AUTO_TRIGGER_SUCCESS, captor.getAllValues().get(1).getStatus());
    }

    @Test
    void executePendingAutoTriggerReusesExistingTaskForIdempotency() {
        Fixture fixture = new Fixture();
        EventAutoTriggerConfigService.EventAutoTriggerRule rule = fixture.rule("HIGH_IMPACT");
        when(fixture.configService.findEnabledRuleByCode("HIGH_IMPACT")).thenReturn(rule);
        ResearchTaskDO existingTask = new ResearchTaskDO();
        existingTask.setTaskId("task-existing");
        when(fixture.researchTaskMapper.selectOne(any())).thenReturn(existingTask);

        MarketEventDO event = buildPendingEvent();
        String taskId = fixture.service.executePendingAutoTrigger(event);

        assertEquals("task-existing", taskId);
        assertEquals(MarketEventAutoTriggerService.AUTO_TRIGGER_SUCCESS, event.getAutoTriggerStatus());
        assertEquals("IDEMPOTENT_EXISTING_TASK", event.getAutoTriggerReason());
        verify(fixture.dispatchService, never()).createFollowUpTask(any(), any());
    }

    @Test
    void concurrentClaimLoserDoesNotDispatchDuplicateTask() {
        Fixture fixture = new Fixture();
        fixture.claimFails();
        EventAutoTriggerConfigService.EventAutoTriggerRule rule = fixture.rule("HIGH_IMPACT");
        when(fixture.configService.findEnabledRuleByCode("HIGH_IMPACT")).thenReturn(rule);

        MarketEventDO event = buildPendingEvent();
        String taskId = fixture.service.executePendingAutoTrigger(event);

        assertEquals(null, taskId);
        verify(fixture.dispatchService, never()).createFollowUpTask(any(), any());
        ArgumentCaptor<MarketEventAutoTriggerAttemptDO> captor = ArgumentCaptor.forClass(MarketEventAutoTriggerAttemptDO.class);
        verify(fixture.attemptMapper).insert(captor.capture());
        assertEquals(MarketEventAutoTriggerAttemptManager.AUTO_TRIGGER_SKIPPED_DUPLICATE, captor.getValue().getStatus());
        assertEquals("CLAIM_ALREADY_HELD", captor.getValue().getReason());
    }

    @Test
    void rateLimitFailureIsPersistedAndAudited() {
        Fixture fixture = new Fixture();
        fixture.claimSucceeds();
        fixture.rateLimitGlobal();
        EventAutoTriggerConfigService.EventAutoTriggerRule rule = fixture.rule("HIGH_IMPACT");
        when(fixture.configService.findEnabledRuleByCode("HIGH_IMPACT")).thenReturn(rule);

        MarketEventDO event = buildPendingEvent();
        assertThrows(BizException.class, () -> fixture.service.executePendingAutoTrigger(event));

        assertEquals(MarketEventAutoTriggerService.AUTO_TRIGGER_FAILED, event.getAutoTriggerStatus());
        assertEquals("EVENT_AUTO_TRIGGER_RATE_LIMITED", event.getAutoTriggerFailureCode());
        assertEquals(1, event.getAutoTriggerRetryCount());
        verify(fixture.dispatchService, never()).createFollowUpTask(any(), any());
        ArgumentCaptor<MarketEventAutoTriggerAttemptDO> captor = ArgumentCaptor.forClass(MarketEventAutoTriggerAttemptDO.class);
        verify(fixture.attemptMapper, times(2)).insert(captor.capture());
        assertEquals(MarketEventAutoTriggerService.AUTO_TRIGGER_FAILED, captor.getAllValues().get(1).getStatus());
        assertEquals("EVENT_AUTO_TRIGGER_RATE_LIMITED", captor.getAllValues().get(1).getFailureCode());
    }

    @Test
    void failedAutoTriggerCanBeRetriedUntilCompensationLimit() {
        Fixture fixture = new Fixture();
        fixture.claimSucceeds();
        EventAutoTriggerConfigService.EventAutoTriggerRule rule = fixture.rule("HIGH_IMPACT");
        when(fixture.configService.findEnabledRuleByCode("HIGH_IMPACT")).thenReturn(rule);
        when(fixture.dispatchService.createFollowUpTask(any(), any()))
                .thenThrow(new BizException("DOWNSTREAM_UNAVAILABLE", "downstream unavailable"));

        MarketEventDO event = buildPendingEvent();
        assertThrows(BizException.class, () -> fixture.service.executePendingAutoTrigger(event));

        assertEquals(MarketEventAutoTriggerService.AUTO_TRIGGER_FAILED, event.getAutoTriggerStatus());
        assertEquals("DOWNSTREAM_UNAVAILABLE", event.getAutoTriggerFailureCode());
        assertEquals(1, event.getAutoTriggerRetryCount());
        assertEquals(true, fixture.service.isPendingAutoTrigger(event));

        event.setAutoTriggerRetryCount(3);
        assertEquals(false, fixture.service.isPendingAutoTrigger(event));
    }

    private static MarketEventDO buildPendingEvent() {
        MarketEventDO event = new MarketEventDO();
        event.setId(1L);
        event.setEventId("event-1");
        event.setAutoTriggerRuleCode("HIGH_IMPACT");
        event.setAutoTriggerStatus(MarketEventAutoTriggerService.AUTO_TRIGGER_WILL_TRIGGER);
        event.setAutoTriggerRetryCount(0);
        return event;
    }

    private static final class Fixture {
        private final MarketEventMapper marketEventMapper = mock(MarketEventMapper.class);
        private final MarketEventAutoTriggerAttemptMapper attemptMapper = mock(MarketEventAutoTriggerAttemptMapper.class);
        private final ResearchTaskMapper researchTaskMapper = mock(ResearchTaskMapper.class);
        private final StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
        private final EventAutoTriggerConfigService configService = mock(EventAutoTriggerConfigService.class);
        private final EventAutoTaskDispatchService dispatchService = mock(EventAutoTaskDispatchService.class);
        private final MarketEventAutoTriggerAttemptManager attemptManager = new MarketEventAutoTriggerAttemptManager(
                marketEventMapper,
                attemptMapper,
                researchTaskMapper,
                stringRedisTemplate
        );
        private final MarketEventAutoTriggerOrchestrationManager orchestrationManager = new MarketEventAutoTriggerOrchestrationManager(
                attemptManager,
                configService,
                dispatchService
        );
        private final MarketEventAutoTriggerServiceImpl service = new MarketEventAutoTriggerServiceImpl(
                new MarketEventAutoTriggerEventLoaderManager(marketEventMapper),
                orchestrationManager
        );

        private Fixture() {
            doAnswer(invocation -> 1).when(marketEventMapper).updateById(any(MarketEventDO.class));
            doAnswer(invocation -> 1).when(attemptMapper).insert(any(MarketEventAutoTriggerAttemptDO.class));
        }

        private EventAutoTriggerConfigService.EventAutoTriggerRule rule(String ruleCode) {
            EventAutoTriggerConfigService.EventAutoTriggerRule rule = new EventAutoTriggerConfigService.EventAutoTriggerRule();
            rule.setRuleCode(ruleCode);
            return rule;
        }

        private void claimSucceeds() {
            doAnswer(invocation -> 1).when(marketEventMapper).update(any(MarketEventDO.class), any());
        }

        private void claimFails() {
            doAnswer(invocation -> 0).when(marketEventMapper).update(any(MarketEventDO.class), any());
        }

        private void rateLimitGlobal() {
            @SuppressWarnings("unchecked")
            ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.increment(anyString())).thenReturn(61L);
        }
    }
}
