package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.dto.MarketEventSourceSyncDTO;
import com.quant.aiorchestrator.domain.vo.EventSourcePreviewResultVO;
import com.quant.aiorchestrator.domain.vo.EventSourceRequestDiagnosticResultVO;

public interface EventSourcePreviewService {
    EventSourcePreviewResultVO previewSource(String sourceCode, MarketEventSourceSyncDTO dto);

    EventSourceRequestDiagnosticResultVO diagnoseSource(String sourceCode, MarketEventSourceSyncDTO dto);
}
