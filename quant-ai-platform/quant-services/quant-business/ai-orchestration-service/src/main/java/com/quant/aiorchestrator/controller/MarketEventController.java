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
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class MarketEventController {

    private final MarketQueryService marketQueryService;
    private final AuditConfigDashboardQueryService auditConfigDashboardQueryService;
    private final MarketEventService marketEventService;
    private final EventSourcePreviewService eventSourcePreviewService;
    private final RoleAccessConfigService roleAccessConfigService;

    @GetMapping("/market-events")
    public Result<MarketEventPageVO> pageMarketEvents(MarketEventPageQueryDTO queryDTO) {
        return Result.success(marketQueryService.pageMarketEvents(queryDTO));
    }

    @GetMapping("/market-event-stats")
    public Result<MarketEventStatsVO> getMarketEventStats() {
        return Result.success(marketQueryService.getMarketEventStats());
    }

    @GetMapping("/market-events/{eventId}")
    public Result<MarketEventListItemVO> getMarketEvent(@PathVariable("eventId") String eventId) {
        return Result.success(marketQueryService.getMarketEvent(eventId));
    }

    @GetMapping("/market-events/ingest-history")
    public Result<List<MarketEventIngestHistoryItemVO>> listMarketEventIngestHistory() {
        return Result.success(marketQueryService.listMarketEventIngestHistory());
    }

    @GetMapping("/market-event-source-configs")
    public Result<List<EventSourceConfigItemVO>> listMarketEventSourceConfigs() {
        return Result.success(auditConfigDashboardQueryService.listMarketEventSourceConfigs());
    }

    @PostMapping("/market-events")
    public Result<MarketEventCreateResultVO> createMarketEvent(@RequestBody MarketEventCreateDTO dto) {
        roleAccessConfigService.requirePermission(RoleAccessConfigService.PERMISSION_TASK_CREATE);
        return Result.success(marketEventService.createMarketEvent(dto));
    }

    @PostMapping("/market-events/batch-import/preview")
    public Result<MarketEventBatchPreviewResultVO> previewImportMarketEvents(@RequestBody MarketEventBatchImportDTO dto) {
        roleAccessConfigService.requirePermission(RoleAccessConfigService.PERMISSION_TASK_CREATE);
        return Result.success(marketEventService.previewImportMarketEvents(dto));
    }

    @PostMapping("/market-events/batch-import")
    public Result<MarketEventBatchImportResultVO> importMarketEvents(@RequestBody MarketEventBatchImportDTO dto) {
        roleAccessConfigService.requirePermission(RoleAccessConfigService.PERMISSION_TASK_CREATE);
        return Result.success(marketEventService.importMarketEvents(dto));
    }

    @PostMapping("/market-events/mock-ingest")
    public Result<MarketEventBatchImportResultVO> mockIngestMarketEvents(@RequestBody MarketEventMockIngestDTO dto) {
        roleAccessConfigService.requirePermission(RoleAccessConfigService.PERMISSION_TASK_CREATE);
        return Result.success(marketEventService.mockIngestMarketEvents(dto));
    }

    @PostMapping("/market-events/source-sync/{sourceCode}")
    public Result<MarketEventBatchImportResultVO> syncMarketEventSource(@PathVariable("sourceCode") String sourceCode,
                                                                        @RequestBody MarketEventSourceSyncDTO dto) {
        roleAccessConfigService.requirePermission(RoleAccessConfigService.PERMISSION_TASK_CREATE);
        return Result.success(marketEventService.syncMarketEventSource(sourceCode, dto));
    }

    @PostMapping("/market-events/source-preview/{sourceCode}")
    public Result<EventSourcePreviewResultVO> previewMarketEventSource(@PathVariable("sourceCode") String sourceCode,
                                                                       @RequestBody MarketEventSourceSyncDTO dto) {
        roleAccessConfigService.requirePermission(RoleAccessConfigService.PERMISSION_MODEL_AGENT_CONFIG_VIEW);
        return Result.success(eventSourcePreviewService.previewSource(sourceCode, dto));
    }

    @PostMapping("/market-events/source-diagnose/{sourceCode}")
    public Result<EventSourceRequestDiagnosticResultVO> diagnoseMarketEventSource(@PathVariable("sourceCode") String sourceCode,
                                                                                  @RequestBody MarketEventSourceSyncDTO dto) {
        roleAccessConfigService.requirePermission(RoleAccessConfigService.PERMISSION_MODEL_AGENT_CONFIG_VIEW);
        return Result.success(eventSourcePreviewService.diagnoseSource(sourceCode, dto));
    }

    @GetMapping("/market-events/cninfo-proxy")
    public Result<CninfoProxyAnnouncementResponseVO> previewCninfoProxyAnnouncements(MarketEventSourceSyncDTO dto) {
        return Result.success(marketEventService.previewCninfoProxyAnnouncements(dto));
    }
}
