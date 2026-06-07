package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.domain.dto.MarketEventBatchImportDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventCreateDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventMockIngestDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventPageQueryDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventSourceSyncDTO;
import com.quant.aiorchestrator.domain.vo.CninfoProxyAnnouncementResponseVO;
import com.quant.aiorchestrator.domain.vo.MarketEventBatchImportResultVO;
import com.quant.aiorchestrator.domain.vo.MarketEventBatchPreviewResultVO;
import com.quant.aiorchestrator.domain.vo.MarketEventCreateResultVO;
import com.quant.aiorchestrator.domain.vo.MarketEventIngestHistoryItemVO;
import com.quant.aiorchestrator.domain.vo.MarketEventListItemVO;
import com.quant.aiorchestrator.domain.vo.MarketEventPageVO;
import com.quant.aiorchestrator.domain.vo.MarketEventStatsVO;
import com.quant.aiorchestrator.manager.MarketEventBatchPreviewManager;
import com.quant.aiorchestrator.manager.MarketEventCommandManager;
import com.quant.aiorchestrator.manager.MarketEventCreateManager;
import com.quant.aiorchestrator.manager.MarketEventIngestOrchestrationManager;
import com.quant.aiorchestrator.manager.MarketEventQueryManager;
import com.quant.aiorchestrator.manager.MarketEventStatsManager;
import com.quant.aiorchestrator.service.CninfoProxyAnnouncementService;
import com.quant.aiorchestrator.service.MarketEventIngestHistoryService;
import com.quant.aiorchestrator.service.MarketEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketEventServiceImpl implements MarketEventService {

    private final MarketEventIngestHistoryService marketEventIngestHistoryService;
    private final CninfoProxyAnnouncementService cninfoProxyAnnouncementService;
    private final MarketEventBatchPreviewManager marketEventBatchPreviewManager;
    private final MarketEventCommandManager marketEventCommandManager;
    private final MarketEventCreateManager marketEventCreateManager;
    private final MarketEventIngestOrchestrationManager marketEventIngestOrchestrationManager;
    private final MarketEventQueryManager marketEventQueryManager;
    private final MarketEventStatsManager marketEventStatsManager;

    @Value("${quant.ai.mock-ingest.enabled:false}")
    private boolean mockIngestEnabled;

    @Override
    public MarketEventPageVO pageMarketEvents(MarketEventPageQueryDTO queryDTO) {
        return marketEventQueryManager.pageMarketEvents(queryDTO);
    }

    @Override
    public MarketEventStatsVO getMarketEventStats() {
        return marketEventStatsManager.getMarketEventStats();
    }

    @Override
    public MarketEventListItemVO getMarketEvent(String eventId) {
        return marketEventQueryManager.getMarketEvent(eventId);
    }

    @Override
    public List<MarketEventIngestHistoryItemVO> listMarketEventIngestHistory() {
        return marketEventIngestHistoryService.loadRecentHistory();
    }

    @Override
    public MarketEventCreateResultVO createMarketEvent(MarketEventCreateDTO dto) {
        return executeCreateMarketEvent(dto, true);
    }

    @Override
    public MarketEventBatchPreviewResultVO previewImportMarketEvents(MarketEventBatchImportDTO dto) {
        return marketEventBatchPreviewManager.previewImportMarketEvents(
                dto,
                marketEventCreateManager::findDuplicatedEvent,
                marketEventIngestOrchestrationManager::resolveExceptionMessage
        );
    }

    @Override
    public MarketEventBatchImportResultVO importMarketEvents(MarketEventBatchImportDTO dto) {
        return marketEventIngestOrchestrationManager.importMarketEvents(dto, item -> executeCreateMarketEvent(item, false));
    }

    @Override
    public MarketEventBatchImportResultVO mockIngestMarketEvents(MarketEventMockIngestDTO dto) {
        return marketEventIngestOrchestrationManager.mockIngestMarketEvents(
                dto,
                mockIngestEnabled,
                item -> executeCreateMarketEvent(item, false)
        );
    }

    @Override
    public MarketEventBatchImportResultVO syncMarketEventSource(String sourceCode, MarketEventSourceSyncDTO dto) {
        return marketEventIngestOrchestrationManager.syncMarketEventSource(sourceCode, dto, item -> executeCreateMarketEvent(item, false));
    }

    @Override
    public CninfoProxyAnnouncementResponseVO previewCninfoProxyAnnouncements(MarketEventSourceSyncDTO dto) {
        return cninfoProxyAnnouncementService.previewAnnouncements(dto);
    }

    private MarketEventCreateResultVO executeCreateMarketEvent(MarketEventCreateDTO dto, boolean recordHistory) {
        return marketEventCommandManager.createMarketEvent(dto, recordHistory);
    }
}
