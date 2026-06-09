package com.quant.dataingest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.dataingest.MarketEventDataIngestService;
import com.quant.aiorchestrator.dataingest.RawPayloadStore;
import com.quant.aiorchestrator.dataingest.SourceFetchStatus;
import com.quant.aiorchestrator.dataingest.SourceProvenance;
import com.quant.aiorchestrator.dataingest.SourceRawPayload;
import com.quant.aiorchestrator.domain.dto.MarketEventBatchImportDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventCreateDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventSourceSyncDTO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.aiorchestrator.domain.vo.MarketEventBatchImportResultVO;
import com.quant.aiorchestrator.mapper.MarketEventIngestRunMapper;
import com.quant.aiorchestrator.service.EventSourceSyncAdapter;
import com.quant.aiorchestrator.service.MarketEventIngestHistoryService;
import com.quant.common.core.exception.BizException;
import com.quant.common.messaging.KafkaTopicConstants;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketEventDataIngestServiceTests {

    @Test
    void successfulFetchStoresRawPayloadAndImportsStandardizedEvents() {
        EventSourceConfigItemVO source = buildSource("NEWS_WIRE", "RSS_XML");
        MarketEventSourceSyncDTO request = buildRequest();
        EventSourceSyncAdapter adapter = mock(EventSourceSyncAdapter.class);
        RawPayloadStore rawPayloadStore = mock(RawPayloadStore.class);
        MarketEventIngestHistoryService historyService = mock(MarketEventIngestHistoryService.class);
        MarketEventIngestRunMapper ingestRunMapper = mock(MarketEventIngestRunMapper.class);
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        MarketEventCreateDTO event = buildEvent();
        SourceRawPayload rawPayload = SourceRawPayload.builder()
                .provenance(SourceProvenance.from(source, request.getTargetCode()))
                .httpStatus(200)
                .requestMethod("GET")
                .requestUrl("https://source.example/rss")
                .body("<rss><channel><item><title>Market event</title></item></channel></rss>")
                .build();

        when(adapter.supports(source)).thenReturn(true);
        when(adapter.fetchRaw(source, request)).thenReturn(rawPayload);
        when(adapter.standardize(rawPayload, source, request)).thenReturn(List.of(event));
        when(rawPayloadStore.save(eq("NEWS_WIRE"), eq("FETCHED"), any())).thenReturn("file:///raw/news.json");

        MarketEventDataIngestService service = new MarketEventDataIngestService(
                List.of(adapter),
                rawPayloadStore,
                historyService,
                ingestRunMapper,
                kafkaTemplate,
                new ObjectMapper()
        );
        ReflectionTestUtils.setField(service, "maxAttempts", 3);

        MarketEventBatchImportResultVO importResult = new MarketEventBatchImportResultVO();
        importResult.setSuccessCount(1);

        var result = service.ingestMarketEventSource(source, request, (MarketEventBatchImportDTO importDTO, String sourceDetail) -> {
            assertEquals(1, importDTO.getEvents().size());
            assertTrue(sourceDetail.contains("rawPayloadRef=file:///raw/news.json"));
            return importResult;
        });

        assertEquals(SourceFetchStatus.STANDARDIZED, result.getFetchResult().getStatus());
        assertEquals(importResult, result.getImportResult());
        verify(kafkaTemplate, never()).send(eq(KafkaTopicConstants.MARKET_EVENT_DEADLETTER), any(), any());
    }

    @Test
    void failedFetchRetriesRecordsHistoryAndDeadlettersAfterMaxAttempts() {
        EventSourceConfigItemVO source = buildSource("POLICY_TRACKER", "RSS_XML");
        MarketEventSourceSyncDTO request = buildRequest();
        EventSourceSyncAdapter adapter = mock(EventSourceSyncAdapter.class);
        RawPayloadStore rawPayloadStore = mock(RawPayloadStore.class);
        MarketEventIngestHistoryService historyService = mock(MarketEventIngestHistoryService.class);
        MarketEventIngestRunMapper ingestRunMapper = mock(MarketEventIngestRunMapper.class);
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        AtomicInteger rawCounter = new AtomicInteger();

        when(adapter.supports(source)).thenReturn(true);
        when(adapter.fetchRaw(source, request)).thenThrow(new BizException("UPSTREAM_DOWN", "upstream unavailable"));
        when(rawPayloadStore.save(eq("POLICY_TRACKER"), any(), any()))
                .thenAnswer(invocation -> "file:///raw/failure-" + rawCounter.incrementAndGet() + ".json");

        MarketEventDataIngestService service = new MarketEventDataIngestService(
                List.of(adapter),
                rawPayloadStore,
                historyService,
                ingestRunMapper,
                kafkaTemplate,
                new ObjectMapper()
        );
        ReflectionTestUtils.setField(service, "maxAttempts", 2);

        assertThrows(BizException.class, () -> service.ingestMarketEventSource(
                source,
                request,
                (importDTO, sourceDetail) -> {
                    throw new AssertionError("failed fetch must not import");
                }
        ));

        verify(adapter, times(2)).fetchRaw(source, request);
        verify(historyService).appendHistory(
                eq("SOURCE_SYNC"),
                eq("Policy Source"),
                eq("POLICY_TRACKER"),
                eq("Policy Source"),
                eq("POLICY"),
                eq("POLICY_FEED"),
                any(),
                eq(0),
                eq(0),
                eq(1),
                eq(0),
                eq(0),
                eq("FETCH_FAILED"),
                eq("upstream unavailable"),
                eq("source fetch failed; retry=1/2")
        );
        verify(historyService).appendHistory(
                eq("SOURCE_SYNC"),
                eq("Policy Source"),
                eq("POLICY_TRACKER"),
                eq("Policy Source"),
                eq("POLICY"),
                eq("POLICY_FEED"),
                any(),
                eq(0),
                eq(0),
                eq(1),
                eq(0),
                eq(0),
                eq("DEADLETTERED"),
                eq("upstream unavailable"),
                eq("source fetch failed; retry=2/2")
        );
        verify(kafkaTemplate).send(eq(KafkaTopicConstants.MARKET_EVENT_DEADLETTER), eq("POLICY_TRACKER"), any());
    }

    private EventSourceConfigItemVO buildSource(String sourceCode, String ingestMode) {
        EventSourceConfigItemVO source = new EventSourceConfigItemVO();
        source.setSourceCode(sourceCode);
        source.setSourceName(sourceCode.equals("POLICY_TRACKER") ? "Policy Source" : "News Source");
        source.setSourceCategory(sourceCode.equals("POLICY_TRACKER") ? "POLICY" : "NEWS");
        source.setSourceChannel(sourceCode.equals("POLICY_TRACKER") ? "POLICY_FEED" : "NEWS_FEED");
        source.setIngestMode(ingestMode);
        source.setEnabled(true);
        return source;
    }

    private MarketEventSourceSyncDTO buildRequest() {
        MarketEventSourceSyncDTO request = new MarketEventSourceSyncDTO();
        request.setTargetType("STOCK");
        request.setTargetCode("600519");
        request.setTargetName("Kweichow Moutai");
        request.setItemCount(3);
        return request;
    }

    private MarketEventCreateDTO buildEvent() {
        MarketEventCreateDTO event = new MarketEventCreateDTO();
        event.setTargetCode("600519");
        event.setTargetName("Kweichow Moutai");
        event.setEventType("NEWS");
        event.setEventTitle("Market event");
        event.setEventSummary("Market event summary");
        event.setSourceChannel("NEWS_FEED");
        event.setImpactLevel("MEDIUM");
        return event;
    }
}
