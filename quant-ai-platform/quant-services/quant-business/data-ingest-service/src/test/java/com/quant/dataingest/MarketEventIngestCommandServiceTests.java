package com.quant.dataingest;

import com.quant.aiorchestrator.domain.dto.MarketEventMockIngestDTO;
import com.quant.aiorchestrator.manager.MarketEventIngestOrchestrationManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class MarketEventIngestCommandServiceTests {

    @Test
    void mockIngestIsDisabledUnlessLocalDemoFlagEnablesIt() {
        MarketEventIngestOrchestrationManager manager = new MarketEventIngestOrchestrationManager(
                null,
                null,
                null,
                null,
                null,
                null
        );

        MarketEventMockIngestDTO dto = new MarketEventMockIngestDTO();
        dto.setTargetCode("600519");
        dto.setTargetName("Kweichow Moutai");
        dto.setSourcePreset("LOCAL_DEMO_EXCHANGE_ANNOUNCEMENT");

        assertThrows(Exception.class, () -> manager.mockIngestMarketEvents(dto, false, event -> null));
    }
}
