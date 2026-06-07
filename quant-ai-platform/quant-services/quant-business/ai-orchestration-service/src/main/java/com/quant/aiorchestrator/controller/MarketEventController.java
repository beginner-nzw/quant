package com.quant.aiorchestrator.controller;

import com.quant.aiorchestrator.domain.dto.MarketEventBatchImportDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventCreateDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventMockIngestDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventPageQueryDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventSourceSyncDTO;
import com.quant.aiorchestrator.domain.vo.CninfoProxyAnnouncementResponseVO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.aiorchestrator.domain.vo.EventSourcePreviewResultVO;
import com.quant.aiorchestrator.domain.vo.EventSourceRequestDiagnosticResultVO;
import com.quant.aiorchestrator.domain.vo.MarketEventBatchImportResultVO;
import com.quant.aiorchestrator.domain.vo.MarketEventBatchPreviewResultVO;
import com.quant.aiorchestrator.domain.vo.MarketEventCreateResultVO;
import com.quant.aiorchestrator.domain.vo.MarketEventIngestHistoryItemVO;
import com.quant.aiorchestrator.domain.vo.MarketEventListItemVO;
import com.quant.aiorchestrator.domain.vo.MarketEventPageVO;
import com.quant.aiorchestrator.domain.vo.MarketEventStatsVO;
import com.quant.aiorchestrator.market.MarketDataIngestStableContract;
import com.quant.aiorchestrator.service.AuditConfigDashboardQueryService;
import com.quant.aiorchestrator.service.EventSourcePreviewService;
import com.quant.aiorchestrator.service.MarketEventService;
import com.quant.aiorchestrator.service.MarketQueryService;
import com.quant.aiorchestrator.service.RoleAccessConfigService;
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
public class MarketEventController {

    private final MarketQueryService marketQueryService;
    private final AuditConfigDashboardQueryService auditConfigDashboardQueryService;
    private final MarketEventService marketEventService;
    private final EventSourcePreviewService eventSourcePreviewService;
    private final RoleAccessConfigService roleAccessConfigService;

    @GetMapping(MarketDataIngestStableContract.MARKET_EVENTS)
    public Result<MarketEventPageVO> pageMarketEvents(MarketEventPageQueryDTO queryDTO) {
        return Result.success(marketQueryService.pageMarketEvents(queryDTO));
    }

    @GetMapping(MarketDataIngestStableContract.MARKET_EVENT_STATS)
    public Result<MarketEventStatsVO> getMarketEventStats() {
        return Result.success(marketQueryService.getMarketEventStats());
    }

    @GetMapping(MarketDataIngestStableContract.MARKET_EVENT_DETAIL)
    public Result<MarketEventListItemVO> getMarketEvent(@PathVariable("eventId") String eventId) {
        return Result.success(marketQueryService.getMarketEvent(eventId));
    }

    @GetMapping(MarketDataIngestStableContract.MARKET_EVENT_INGEST_HISTORY)
    public Result<List<MarketEventIngestHistoryItemVO>> listMarketEventIngestHistory() {
        return Result.success(marketQueryService.listMarketEventIngestHistory());
    }

    @GetMapping(MarketDataIngestStableContract.MARKET_EVENT_SOURCE_CONFIGS)
    public Result<List<EventSourceConfigItemVO>> listMarketEventSourceConfigs() {
        return Result.success(auditConfigDashboardQueryService.listMarketEventSourceConfigs());
    }

    @PostMapping(MarketDataIngestStableContract.MARKET_EVENTS)
    public Result<MarketEventCreateResultVO> createMarketEvent(@RequestBody MarketEventCreateDTO dto) {
        roleAccessConfigService.requirePermission(RoleAccessConfigService.PERMISSION_TASK_CREATE);
        return Result.success(marketEventService.createMarketEvent(dto));
    }

    @PostMapping(MarketDataIngestStableContract.MARKET_EVENT_BATCH_IMPORT_PREVIEW)
    public Result<MarketEventBatchPreviewResultVO> previewImportMarketEvents(@RequestBody MarketEventBatchImportDTO dto) {
        roleAccessConfigService.requirePermission(RoleAccessConfigService.PERMISSION_TASK_CREATE);
        return Result.success(marketEventService.previewImportMarketEvents(dto));
    }

    @PostMapping(MarketDataIngestStableContract.MARKET_EVENT_BATCH_IMPORT)
    public Result<MarketEventBatchImportResultVO> importMarketEvents(@RequestBody MarketEventBatchImportDTO dto) {
        roleAccessConfigService.requirePermission(RoleAccessConfigService.PERMISSION_TASK_CREATE);
        return Result.success(marketEventService.importMarketEvents(dto));
    }

    @PostMapping(MarketDataIngestStableContract.MARKET_EVENT_MOCK_INGEST)
    public Result<MarketEventBatchImportResultVO> mockIngestMarketEvents(@RequestBody MarketEventMockIngestDTO dto) {
        roleAccessConfigService.requirePermission(RoleAccessConfigService.PERMISSION_TASK_CREATE);
        return Result.success(marketEventService.mockIngestMarketEvents(dto));
    }

    @PostMapping(MarketDataIngestStableContract.MARKET_EVENT_SOURCE_SYNC)
    public Result<MarketEventBatchImportResultVO> syncMarketEventSource(@PathVariable("sourceCode") String sourceCode,
                                                                        @RequestBody MarketEventSourceSyncDTO dto) {
        roleAccessConfigService.requirePermission(RoleAccessConfigService.PERMISSION_TASK_CREATE);
        return Result.success(marketEventService.syncMarketEventSource(sourceCode, dto));
    }

    @PostMapping(MarketDataIngestStableContract.MARKET_EVENT_SOURCE_PREVIEW)
    public Result<EventSourcePreviewResultVO> previewMarketEventSource(@PathVariable("sourceCode") String sourceCode,
                                                                       @RequestBody MarketEventSourceSyncDTO dto) {
        roleAccessConfigService.requirePermission(RoleAccessConfigService.PERMISSION_MODEL_AGENT_CONFIG_VIEW);
        return Result.success(eventSourcePreviewService.previewSource(sourceCode, dto));
    }

    @PostMapping(MarketDataIngestStableContract.MARKET_EVENT_SOURCE_DIAGNOSE)
    public Result<EventSourceRequestDiagnosticResultVO> diagnoseMarketEventSource(@PathVariable("sourceCode") String sourceCode,
                                                                                  @RequestBody MarketEventSourceSyncDTO dto) {
        roleAccessConfigService.requirePermission(RoleAccessConfigService.PERMISSION_MODEL_AGENT_CONFIG_VIEW);
        return Result.success(eventSourcePreviewService.diagnoseSource(sourceCode, dto));
    }

    @GetMapping(MarketDataIngestStableContract.MARKET_EVENT_CNINFO_PROXY)
    public Result<CninfoProxyAnnouncementResponseVO> previewCninfoProxyAnnouncements(MarketEventSourceSyncDTO dto) {
        return Result.success(marketEventService.previewCninfoProxyAnnouncements(dto));
    }
}
