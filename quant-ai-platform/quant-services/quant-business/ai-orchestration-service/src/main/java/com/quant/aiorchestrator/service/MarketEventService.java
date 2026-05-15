package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.dto.MarketEventBatchImportDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventCreateDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventMockIngestDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventPageQueryDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventSourceSyncDTO;
import com.quant.aiorchestrator.domain.vo.MarketEventBatchImportResultVO;
import com.quant.aiorchestrator.domain.vo.MarketEventBatchPreviewResultVO;
import com.quant.aiorchestrator.domain.vo.CninfoProxyAnnouncementResponseVO;
import com.quant.aiorchestrator.domain.vo.MarketEventCreateResultVO;
import com.quant.aiorchestrator.domain.vo.MarketEventIngestHistoryItemVO;
import com.quant.aiorchestrator.domain.vo.MarketEventListItemVO;
import com.quant.aiorchestrator.domain.vo.MarketEventPageVO;
import com.quant.aiorchestrator.domain.vo.MarketEventStatsVO;

import java.util.List;

public interface MarketEventService {
    MarketEventPageVO pageMarketEvents(MarketEventPageQueryDTO queryDTO);

    MarketEventStatsVO getMarketEventStats();

    MarketEventListItemVO getMarketEvent(String eventId);

    List<MarketEventIngestHistoryItemVO> listMarketEventIngestHistory();

    MarketEventCreateResultVO createMarketEvent(MarketEventCreateDTO dto);

    MarketEventBatchPreviewResultVO previewImportMarketEvents(MarketEventBatchImportDTO dto);

    MarketEventBatchImportResultVO importMarketEvents(MarketEventBatchImportDTO dto);

    MarketEventBatchImportResultVO mockIngestMarketEvents(MarketEventMockIngestDTO dto);

    MarketEventBatchImportResultVO syncMarketEventSource(String sourceCode, MarketEventSourceSyncDTO dto);

    CninfoProxyAnnouncementResponseVO previewCninfoProxyAnnouncements(MarketEventSourceSyncDTO dto);
}
