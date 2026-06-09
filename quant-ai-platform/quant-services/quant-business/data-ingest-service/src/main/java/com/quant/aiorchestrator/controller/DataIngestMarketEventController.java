package com.quant.aiorchestrator.controller;

import com.quant.aiorchestrator.domain.dto.MarketEventBatchImportDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventMockIngestDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventSourceSyncDTO;
import com.quant.aiorchestrator.domain.vo.CninfoProxyAnnouncementResponseVO;
import com.quant.aiorchestrator.domain.vo.EventSourcePreviewResultVO;
import com.quant.aiorchestrator.domain.vo.EventSourceRequestDiagnosticResultVO;
import com.quant.aiorchestrator.domain.vo.MarketEventBatchImportResultVO;
import com.quant.aiorchestrator.domain.vo.MarketEventBatchPreviewResultVO;
import com.quant.aiorchestrator.domain.vo.MarketEventIngestHistoryItemVO;
import com.quant.aiorchestrator.market.MarketDataIngestStableContract;
import com.quant.aiorchestrator.market.MarketEventIngestCommandPort;
import com.quant.aiorchestrator.service.CninfoProxyAnnouncementService;
import com.quant.aiorchestrator.service.EventSourcePreviewService;
import com.quant.aiorchestrator.service.MarketEventIngestHistoryService;
import com.quant.config.api.RoleAccessPermissions;
import com.quant.config.port.RoleAccessPermissionPort;
import com.quant.common.core.model.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(MarketDataIngestStableContract.LEGACY_TASK_API_BASE)
@RequiredArgsConstructor
public class DataIngestMarketEventController {

    private final MarketEventIngestHistoryService marketEventIngestHistoryService;
    private final EventSourcePreviewService eventSourcePreviewService;
    private final CninfoProxyAnnouncementService cninfoProxyAnnouncementService;
    private final MarketEventIngestCommandPort marketEventIngestCommandPort;
    private final RoleAccessPermissionPort roleAccessPermissionPort;

    @GetMapping(MarketDataIngestStableContract.MARKET_EVENT_INGEST_HISTORY)
    public Result<List<MarketEventIngestHistoryItemVO>> listMarketEventIngestHistory() {
        return Result.success(marketEventIngestHistoryService.loadRecentHistory());
    }

    @PostMapping(MarketDataIngestStableContract.MARKET_EVENT_BATCH_IMPORT_PREVIEW)
    public Result<MarketEventBatchPreviewResultVO> previewImportMarketEvents(@RequestBody MarketEventBatchImportDTO dto) {
        roleAccessPermissionPort.requirePermission(RoleAccessPermissions.TASK_CREATE);
        return Result.success(marketEventIngestCommandPort.previewImportMarketEvents(dto));
    }

    @PostMapping(MarketDataIngestStableContract.MARKET_EVENT_BATCH_IMPORT)
    public Result<MarketEventBatchImportResultVO> importMarketEvents(@RequestBody MarketEventBatchImportDTO dto) {
        roleAccessPermissionPort.requirePermission(RoleAccessPermissions.TASK_CREATE);
        return Result.success(marketEventIngestCommandPort.importMarketEvents(dto));
    }

    @PostMapping(MarketDataIngestStableContract.MARKET_EVENT_MOCK_INGEST)
    public Result<MarketEventBatchImportResultVO> mockIngestMarketEvents(@RequestBody MarketEventMockIngestDTO dto) {
        roleAccessPermissionPort.requirePermission(RoleAccessPermissions.TASK_CREATE);
        return Result.success(marketEventIngestCommandPort.mockIngestMarketEvents(dto));
    }

    @PostMapping(MarketDataIngestStableContract.MARKET_EVENT_SOURCE_SYNC)
    public Result<MarketEventBatchImportResultVO> syncMarketEventSource(@PathVariable("sourceCode") String sourceCode,
                                                                        @RequestBody MarketEventSourceSyncDTO dto) {
        roleAccessPermissionPort.requirePermission(RoleAccessPermissions.TASK_CREATE);
        return Result.success(marketEventIngestCommandPort.syncMarketEventSource(sourceCode, dto));
    }

    @PostMapping(MarketDataIngestStableContract.MARKET_EVENT_SOURCE_PREVIEW)
    public Result<EventSourcePreviewResultVO> previewMarketEventSource(@PathVariable("sourceCode") String sourceCode,
                                                                       @RequestBody MarketEventSourceSyncDTO dto) {
        roleAccessPermissionPort.requirePermission(RoleAccessPermissions.MODEL_AGENT_CONFIG_VIEW);
        return Result.success(eventSourcePreviewService.previewSource(sourceCode, dto));
    }

    @PostMapping(MarketDataIngestStableContract.MARKET_EVENT_SOURCE_DIAGNOSE)
    public Result<EventSourceRequestDiagnosticResultVO> diagnoseMarketEventSource(@PathVariable("sourceCode") String sourceCode,
                                                                                  @RequestBody MarketEventSourceSyncDTO dto) {
        roleAccessPermissionPort.requirePermission(RoleAccessPermissions.MODEL_AGENT_CONFIG_VIEW);
        return Result.success(eventSourcePreviewService.diagnoseSource(sourceCode, dto));
    }

    @GetMapping(MarketDataIngestStableContract.MARKET_EVENT_CNINFO_PROXY)
    public Result<CninfoProxyAnnouncementResponseVO> previewCninfoProxyAnnouncements(MarketEventSourceSyncDTO dto) {
        return Result.success(cninfoProxyAnnouncementService.previewAnnouncements(dto));
    }
}
