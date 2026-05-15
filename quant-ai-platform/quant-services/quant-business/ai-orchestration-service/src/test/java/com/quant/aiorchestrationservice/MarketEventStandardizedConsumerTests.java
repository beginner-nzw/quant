package com.quant.aiorchestrationservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.consumer.MarketEventStandardizedConsumer;
import com.quant.aiorchestrator.domain.entity.MarketEventDO;
import com.quant.aiorchestrator.service.MarketEventAutoTriggerService;
import com.quant.aiorchestrator.service.TaskMessageLogService;
import com.quant.common.messaging.KafkaTopicConstants;
import com.quant.common.model.message.MarketEventStandardizedMessage;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketEventStandardizedConsumerTests {

    @Test
    void pendingEventShouldExecuteAndRecordConsumed() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        MarketEventAutoTriggerService autoTriggerService = mock(MarketEventAutoTriggerService.class);
        TaskMessageLogService taskMessageLogService = mock(TaskMessageLogService.class);

        MarketEventStandardizedConsumer consumer = new MarketEventStandardizedConsumer(
                objectMapper,
                autoTriggerService,
                taskMessageLogService
        );

        MarketEventStandardizedMessage message = buildMessage();
        String rawMessage = objectMapper.writeValueAsString(message);
        MarketEventDO event = new MarketEventDO();
        event.setEventId("event-1");
        event.setAutoTriggerStatus(MarketEventAutoTriggerService.AUTO_TRIGGER_WILL_TRIGGER);

        when(taskMessageLogService.beginConsume(KafkaTopicConstants.MARKET_EVENT_STANDARDIZED, message, "ai-orchestration-service")).thenReturn(true);
        when(autoTriggerService.loadEvent("event-1")).thenReturn(event);
        when(autoTriggerService.isPendingAutoTrigger(event)).thenReturn(true);

        consumer.onMessage(rawMessage);

        verify(autoTriggerService).executePendingAutoTrigger(event);
        verify(taskMessageLogService).recordConsumed(KafkaTopicConstants.MARKET_EVENT_STANDARDIZED, message, "ai-orchestration-service");
        verify(taskMessageLogService, never()).recordSkipped(eq(KafkaTopicConstants.MARKET_EVENT_STANDARDIZED), any(), eq("ai-orchestration-service"), any());
    }

    @Test
    void nonPendingEventShouldSkip() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        MarketEventAutoTriggerService autoTriggerService = mock(MarketEventAutoTriggerService.class);
        TaskMessageLogService taskMessageLogService = mock(TaskMessageLogService.class);

        MarketEventStandardizedConsumer consumer = new MarketEventStandardizedConsumer(
                objectMapper,
                autoTriggerService,
                taskMessageLogService
        );

        MarketEventStandardizedMessage message = buildMessage();
        String rawMessage = objectMapper.writeValueAsString(message);
        MarketEventDO event = new MarketEventDO();
        event.setEventId("event-1");
        event.setAutoTriggerStatus(MarketEventAutoTriggerService.AUTO_TRIGGER_SUCCESS);

        when(taskMessageLogService.beginConsume(KafkaTopicConstants.MARKET_EVENT_STANDARDIZED, message, "ai-orchestration-service")).thenReturn(true);
        when(autoTriggerService.loadEvent("event-1")).thenReturn(event);
        when(autoTriggerService.isPendingAutoTrigger(event)).thenReturn(false);

        consumer.onMessage(rawMessage);

        verify(autoTriggerService, never()).executePendingAutoTrigger(any());
        verify(taskMessageLogService).recordSkipped(
                KafkaTopicConstants.MARKET_EVENT_STANDARDIZED,
                message,
                "ai-orchestration-service",
                "AUTO_TRIGGER_NOT_PENDING"
        );
    }

    private MarketEventStandardizedMessage buildMessage() {
        MarketEventStandardizedMessage message = new MarketEventStandardizedMessage();
        message.setMessageId("msg-1");
        message.setTraceId("trace-1");
        message.setEventId("event-1");
        message.setMessageType("MARKET_EVENT_STANDARDIZED");
        message.setSourceService("ai-orchestration-service");
        message.setTargetService("market-event-subscribers");
        message.setTenantId("default");
        message.setBizKey("MARKET_EVENT:event-1");
        message.setTimestamp(System.currentTimeMillis());
        message.setVersion("1.0");
        message.setRetryCount(0);

        MarketEventStandardizedMessage.Payload payload = new MarketEventStandardizedMessage.Payload();
        payload.setEventId("event-1");
        message.setPayload(payload);
        return message;
    }
}
