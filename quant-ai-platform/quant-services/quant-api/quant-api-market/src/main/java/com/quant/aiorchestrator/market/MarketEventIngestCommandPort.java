package com.quant.aiorchestrator.market;

import com.quant.aiorchestrator.domain.dto.MarketEventBatchImportDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventMockIngestDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventSourceSyncDTO;
import com.quant.aiorchestrator.domain.vo.MarketEventBatchImportResultVO;
import com.quant.aiorchestrator.domain.vo.MarketEventBatchPreviewResultVO;

public interface MarketEventIngestCommandPort {

    MarketEventBatchPreviewResultVO previewImportMarketEvents(MarketEventBatchImportDTO dto);

    MarketEventBatchImportResultVO importMarketEvents(MarketEventBatchImportDTO dto);

    MarketEventBatchImportResultVO mockIngestMarketEvents(MarketEventMockIngestDTO dto);

    MarketEventBatchImportResultVO syncMarketEventSource(String sourceCode, MarketEventSourceSyncDTO dto);
}
