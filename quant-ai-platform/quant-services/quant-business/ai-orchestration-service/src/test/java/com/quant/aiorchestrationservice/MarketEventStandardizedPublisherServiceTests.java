package com.quant.aiorchestrationservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.entity.MarketEventDO;
import com.quant.aiorchestrator.service.MarketEventStandardizedPublisherService;
import com.quant.aiorchestrator.service.TaskMessageLogService;
import com.quant.common.messaging.KafkaTopicConstants;
import com.quant.common.messaging.MessageTypeConstants;
import com.quant.common.model.message.MessageEnvelope;
import com.quant.common.web.TraceContext;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MarketEventStandardizedPublisherServiceTests {

    @Test
    void publishesStandardizedMarketEvent() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        TaskMessageLogService taskMessageLogService = mock(TaskMessageLogService.class);
        doReturn(CompletableFuture.completedFuture(null)).when(kafkaTemplate).send(anyString(), anyString(), anyString());

        MarketEventStandardizedPublisherService service = new MarketEventStandardizedPublisherService(
                objectMapper,
                kafkaTemplate,
                taskMessageLogService
        );

        MarketEventDO event = new MarketEventDO();
        event.setEventId("event-1");
        event.setTargetType("STOCK");
        event.setTargetCode("600519");
        event.setTargetName("贵州茅台");
        event.setEventType("ANNOUNCEMENT");
        event.setEventTitle("年报发布");
        event.setEventSummary("公司发布年度报告");
        event.setSourceChannel("MANUAL");
        event.setImpactLevel("HIGH");
        event.setEventStatus("ACTIVE");
        event.setAutoTriggerStatus("SUCCESS");
        event.setAutoTriggerTaskId("task-1");
        event.setOccurredAt(LocalDateTime.of(2026, 5, 7, 10, 30));
        event.setCreatedAt(LocalDateTime.of(2026, 5, 7, 10, 31));

        TraceContext.bind("trace-market-1");
        try {
            service.publish(event);
        } finally {
            TraceContext.clear();
        }

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(
                org.mockito.ArgumentMatchers.eq(KafkaTopicConstants.MARKET_EVENT_STANDARDIZED),
                org.mockito.ArgumentMatchers.eq("event-1"),
                payloadCaptor.capture()
        );

        JsonNode root = objectMapper.readTree(payloadCaptor.getValue());
        assertEquals("event-1", root.path("eventId").asText());
        assertEquals(MessageTypeConstants.MARKET_EVENT_STANDARDIZED, root.path("messageType").asText());
        assertEquals("trace-market-1", root.path("traceId").asText());
        assertEquals("600519", root.path("payload").path("targetCode").asText());
        assertEquals("SUCCESS", root.path("payload").path("autoTriggerStatus").asText());

        ArgumentCaptor<MessageEnvelope> logCaptor = ArgumentCaptor.forClass(MessageEnvelope.class);
        verify(taskMessageLogService).recordProduced(
                org.mockito.ArgumentMatchers.eq(KafkaTopicConstants.MARKET_EVENT_STANDARDIZED),
                logCaptor.capture()
        );
        assertEquals(MessageTypeConstants.MARKET_EVENT_STANDARDIZED, logCaptor.getValue().getMessageType());
        assertEquals("event-1", logCaptor.getValue().getEventId());
        assertNull(logCaptor.getValue().getTaskId());
        assertTrue(logCaptor.getValue().getBizKey().startsWith("MARKET_EVENT:"));
    }
}
