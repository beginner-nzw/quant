package com.quant.aiorchestrator.dataingest;

import com.quant.aiorchestrator.domain.dto.MarketEventBatchImportDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventSourceSyncDTO;
import com.quant.aiorchestrator.domain.vo.MarketEventBatchImportResultVO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;

public interface DataIngestService {
    SourceIngestResult ingestMarketEventSource(EventSourceConfigItemVO sourceConfig,
                                               MarketEventSourceSyncDTO request,
                                               SourceImportHandler importHandler);

    @FunctionalInterface
    interface SourceImportHandler {
        MarketEventBatchImportResultVO importEvents(MarketEventBatchImportDTO importDTO, String sourceDetail);
    }
}
