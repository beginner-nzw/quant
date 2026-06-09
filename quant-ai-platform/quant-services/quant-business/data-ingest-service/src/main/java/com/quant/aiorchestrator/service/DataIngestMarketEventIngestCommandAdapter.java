package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.dto.MarketEventBatchImportDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventCreateDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventMockIngestDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventSourceSyncDTO;
import com.quant.aiorchestrator.domain.vo.MarketEventBatchImportResultVO;
import com.quant.aiorchestrator.domain.vo.MarketEventBatchPreviewResultVO;
import com.quant.aiorchestrator.domain.vo.MarketEventCreateResultVO;
import com.quant.aiorchestrator.manager.MarketEventBatchPreviewManager;
import com.quant.aiorchestrator.manager.MarketEventIngestOrchestrationManager;
import com.quant.aiorchestrator.market.MarketEventCreateCommandPort;
import com.quant.aiorchestrator.market.MarketEventIngestCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DataIngestMarketEventIngestCommandAdapter implements MarketEventIngestCommandPort {

    private final MarketEventBatchPreviewManager marketEventBatchPreviewManager;
    private final MarketEventIngestOrchestrationManager marketEventIngestOrchestrationManager;
    private final MarketEventCreateCommandPort marketEventCreateCommandPort;

    @Value("${quant.ai.mock-ingest.enabled:false}")
    private boolean mockIngestEnabled;

    @Override
    public MarketEventBatchPreviewResultVO previewImportMarketEvents(MarketEventBatchImportDTO dto) {
        return marketEventBatchPreviewManager.previewImportMarketEvents(
                dto,
                marketEventCreateCommandPort::findDuplicatedEvent,
                marketEventIngestOrchestrationManager::resolveExceptionMessage
        );
    }

    @Override
    public MarketEventBatchImportResultVO importMarketEvents(MarketEventBatchImportDTO dto) {
        return marketEventIngestOrchestrationManager.importMarketEvents(dto, this::createMarketEvent);
    }

    @Override
    public MarketEventBatchImportResultVO mockIngestMarketEvents(MarketEventMockIngestDTO dto) {
        return marketEventIngestOrchestrationManager.mockIngestMarketEvents(dto, mockIngestEnabled, this::createMarketEvent);
    }

    @Override
    public MarketEventBatchImportResultVO syncMarketEventSource(String sourceCode, MarketEventSourceSyncDTO dto) {
        return marketEventIngestOrchestrationManager.syncMarketEventSource(sourceCode, dto, this::createMarketEvent);
    }

    private MarketEventCreateResultVO createMarketEvent(MarketEventCreateDTO dto) {
        return marketEventCreateCommandPort.createMarketEvent(dto, false);
    }
}
