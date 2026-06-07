package com.quant.aiorchestrationservice;

import com.quant.aiorchestrator.controller.MarketEventController;
import com.quant.aiorchestrator.dataingest.DataIngestService;
import com.quant.aiorchestrator.dataingest.RawPayloadStore;
import com.quant.aiorchestrator.dataingest.SourceRawPayload;
import com.quant.aiorchestrator.domain.entity.MarketEventAnalysisDO;
import com.quant.aiorchestrator.domain.entity.MarketEventDO;
import com.quant.aiorchestrator.domain.entity.MarketEventIngestRunDO;
import com.quant.aiorchestrator.domain.entity.MarketEventRelationDO;
import com.quant.aiorchestrator.domain.dto.MarketEventCreateDTO;
import com.quant.aiorchestrator.market.MarketDataIngestStableContract;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketDataIngestStableContractTests {

    @Test
    void marketEventAndDataIngestAuthorityFactsStaySeparated() {
        assertEquals("market_event", MarketDataIngestStableContract.MARKET_EVENT_TABLE);
        assertEquals("market_event_relation", MarketDataIngestStableContract.MARKET_EVENT_RELATION_TABLE);
        assertEquals("market_event_analysis", MarketDataIngestStableContract.MARKET_EVENT_ANALYSIS_TABLE);
        assertEquals("market_event_ingest_run", MarketDataIngestStableContract.DATA_INGEST_RUN_TABLE);
        assertEquals("event-ingest-histories.json", MarketDataIngestStableContract.DATA_INGEST_HISTORY_STORE);

        assertTrue(hasField(MarketEventDO.class, "eventId"));
        assertTrue(hasField(MarketEventRelationDO.class, "eventId"));
        assertTrue(hasField(MarketEventAnalysisDO.class, "eventId"));
        assertTrue(hasField(MarketEventIngestRunDO.class, "rawPayloadRef"));
        assertTrue(hasField(MarketEventIngestRunDO.class, "fetchStatus"));
        assertTrue(hasField(SourceRawPayload.class, "body"));
        assertTrue(hasMethod(RawPayloadStore.class, "save"));
        assertTrue(hasMethod(DataIngestService.class, "ingestMarketEventSource"));

        assertFalse(hasField(MarketEventDO.class, "rawPayloadBody"));
        assertFalse(hasField(MarketEventDO.class, "fetchStatus"));
        assertFalse(hasField(MarketEventCreateDTO.class, "rawPayloadBody"));
        assertFalse(hasField(MarketEventCreateDTO.class, "fetchStatus"));
    }

    @Test
    void marketDataIngestLegacyRoutesRemainCompatible() {
        assertEquals("/api/tasks", MarketDataIngestStableContract.LEGACY_TASK_API_BASE);

        Set<String> expected = new TreeSet<>(Set.of(
                "GET /api/tasks/market-event-source-configs",
                "GET /api/tasks/market-event-stats",
                "GET /api/tasks/market-events",
                "GET /api/tasks/market-events/cninfo-proxy",
                "GET /api/tasks/market-events/ingest-history",
                "GET /api/tasks/market-events/{eventId}",
                "POST /api/tasks/market-events",
                "POST /api/tasks/market-events/batch-import",
                "POST /api/tasks/market-events/batch-import/preview",
                "POST /api/tasks/market-events/mock-ingest",
                "POST /api/tasks/market-events/source-diagnose/{sourceCode}",
                "POST /api/tasks/market-events/source-preview/{sourceCode}",
                "POST /api/tasks/market-events/source-sync/{sourceCode}"
        ));

        assertEquals(expected, marketEventMappings());
    }

    private static boolean hasField(Class<?> type, String fieldName) {
        return Arrays.stream(type.getDeclaredFields()).anyMatch(field -> field.getName().equals(fieldName));
    }

    private static boolean hasMethod(Class<?> type, String methodName) {
        return Arrays.stream(type.getDeclaredMethods()).anyMatch(method -> method.getName().equals(methodName));
    }

    private static Set<String> marketEventMappings() {
        String basePath = MarketEventController.class.getAnnotation(RequestMapping.class).value()[0];
        Set<String> mappings = new TreeSet<>();
        for (Method method : MarketEventController.class.getDeclaredMethods()) {
            GetMapping getMapping = method.getAnnotation(GetMapping.class);
            if (getMapping != null) {
                mappings.add("GET " + basePath + getMapping.value()[0]);
            }
            PostMapping postMapping = method.getAnnotation(PostMapping.class);
            if (postMapping != null) {
                mappings.add("POST " + basePath + postMapping.value()[0]);
            }
        }
        return mappings;
    }
}
