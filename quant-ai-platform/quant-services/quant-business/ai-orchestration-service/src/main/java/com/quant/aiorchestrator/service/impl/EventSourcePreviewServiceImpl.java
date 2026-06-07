package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.domain.dto.MarketEventCreateDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventSourceSyncDTO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.aiorchestrator.domain.vo.EventSourcePreviewResultVO;
import com.quant.aiorchestrator.domain.vo.EventSourceRequestDiagnosticResultVO;
import com.quant.aiorchestrator.manager.EventSourceAdapterManager;
import com.quant.aiorchestrator.manager.EventSourcePreviewProjectionManager;
import com.quant.aiorchestrator.manager.EventSourcePreviewValidationManager;
import com.quant.aiorchestrator.service.EventSourcePreviewService;
import com.quant.aiorchestrator.service.EventSourceSyncAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventSourcePreviewServiceImpl implements EventSourcePreviewService {

    private final EventSourcePreviewValidationManager previewValidationManager;
    private final EventSourceAdapterManager eventSourceAdapterManager;
    private final EventSourcePreviewProjectionManager previewProjectionManager;

    public EventSourcePreviewResultVO previewSource(String sourceCode, MarketEventSourceSyncDTO dto) {
        EventSourceConfigItemVO sourceConfig = previewValidationManager.resolveValidatedSourceConfig(sourceCode, dto);
        EventSourceSyncAdapter adapter = eventSourceAdapterManager.resolveAdapter(
                sourceConfig,
                "MARKET_EVENT_SOURCE_PREVIEW_UNSUPPORTED",
                "当前事件源不支持预览"
        );
        List<MarketEventCreateDTO> events = adapter.sync(sourceConfig, dto);
        return previewProjectionManager.toPreviewResult(sourceConfig, events);
    }

    public EventSourceRequestDiagnosticResultVO diagnoseSource(String sourceCode, MarketEventSourceSyncDTO dto) {
        EventSourceConfigItemVO sourceConfig = previewValidationManager.resolveValidatedSourceConfig(sourceCode, dto);
        EventSourceSyncAdapter adapter = eventSourceAdapterManager.resolveAdapter(
                sourceConfig,
                "MARKET_EVENT_SOURCE_DIAGNOSE_UNSUPPORTED",
                "当前事件源不支持请求诊断"
        );
        return previewProjectionManager.toDiagnosticResult(sourceConfig, adapter.diagnose(sourceConfig, dto));
    }
}
