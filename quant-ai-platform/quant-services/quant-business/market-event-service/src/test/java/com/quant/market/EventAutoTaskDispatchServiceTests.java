package com.quant.market;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.entity.MarketEventDO;
import com.quant.aiorchestrator.manager.EventAutoTaskHttpDispatchManager;
import com.quant.aiorchestrator.manager.EventAutoTaskPayloadManager;
import com.quant.aiorchestrator.manager.EventAutoTaskServiceActorManager;
import com.quant.aiorchestrator.service.impl.EventAutoTaskDispatchServiceImpl;
import com.quant.common.core.exception.BizException;
import com.quant.common.security.SecurityConstants;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventAutoTaskDispatchServiceTests {

    @Test
    void autoDispatchFailsClosedWhenServiceActorSecretMissing() {
        EventAutoTaskDispatchServiceImpl service = newService();

        BizException ex = assertThrows(BizException.class, () -> service.buildServiceActorHeaders(event()));

        assertEquals("EVENT_AUTO_TRIGGER_SERVICE_IDENTITY_NOT_CONFIGURED", ex.getCode());
    }

    @Test
    void autoDispatchBuildsSignedServiceActorHeadersWhenSecretConfigured() {
        EventAutoTaskDispatchServiceImpl service = newService();
        ReflectionTestUtils.setField(service, "serviceActorSecret", "local-dev-service-actor-secret");

        String[] headers = service.buildServiceActorHeaders(event());

        assertContainsPair(headers, SecurityConstants.HEADER_SERVICE_PRINCIPAL, "market-event-service");
        assertContainsPair(headers, SecurityConstants.HEADER_SERVICE_ACTOR_ID, "market-event-auto-dispatcher");
        assertContainsPair(headers, SecurityConstants.HEADER_SERVICE_ACTOR_ROLE, "EVENT_AUTO_DISPATCHER");
        assertContainsPair(headers, SecurityConstants.HEADER_SERVICE_ORIGINAL_ACTOR_ID, "market-ingest");
        assertContainsPair(headers, SecurityConstants.HEADER_SERVICE_ORIGINAL_ACTOR_ROLE, "SYSTEM");
        assertTrue(indexOf(headers, SecurityConstants.HEADER_SERVICE_TIMESTAMP) >= 0);
        assertTrue(indexOf(headers, SecurityConstants.HEADER_SERVICE_SIGNATURE) >= 0);
    }

    private MarketEventDO event() {
        MarketEventDO event = new MarketEventDO();
        event.setCreatedBy("market-ingest");
        return event;
    }

    private EventAutoTaskDispatchServiceImpl newService() {
        EventAutoTaskServiceActorManager serviceActorManager = new EventAutoTaskServiceActorManager();
        return new EventAutoTaskDispatchServiceImpl(
                new EventAutoTaskHttpDispatchManager(
                        new ObjectMapper(),
                        new EventAutoTaskPayloadManager(),
                        serviceActorManager
                ),
                serviceActorManager
        );
    }

    private void assertContainsPair(String[] headers, String name, String value) {
        int index = indexOf(headers, name);
        assertTrue(index >= 0, "missing header " + name);
        assertEquals(value, headers[index + 1]);
    }

    private int indexOf(String[] headers, String name) {
        for (int i = 0; i < headers.length - 1; i += 2) {
            if (name.equals(headers[i])) {
                return i;
            }
        }
        return -1;
    }
}
