package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.dataingest.DataIngestService;
import com.quant.aiorchestrator.dataingest.SourceIngestResult;
import com.quant.aiorchestrator.domain.dto.MarketEventBatchImportDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventCreateDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventMockIngestDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventSourceSyncDTO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.aiorchestrator.domain.vo.MarketEventBatchImportResultVO;
import com.quant.aiorchestrator.domain.vo.MarketEventCreateResultVO;
import com.quant.aiorchestrator.service.EventSourceConfigService;
import com.quant.aiorchestrator.service.MarketEventIngestHistoryService;
import com.quant.aiorchestrator.service.MarketEventMockIngestGenerator;
import com.quant.common.core.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class MarketEventIngestOrchestrationManager {

    private final MarketEventMockIngestGenerator marketEventMockIngestGenerator;
    private final MarketEventIngestHistoryService marketEventIngestHistoryService;
    private final EventSourceConfigService eventSourceConfigService;
    private final MarketEventNormalizationManager marketEventNormalizationManager;
    private final MarketEventBatchImportManager marketEventBatchImportManager;
    private final DataIngestService dataIngestService;

    public MarketEventBatchImportResultVO importMarketEvents(MarketEventBatchImportDTO dto,
                                                             Function<MarketEventCreateDTO, MarketEventCreateResultVO> createEvent) {
        return executeBatchImport(
                dto,
                "BATCH_IMPORT",
                "batch import",
                "BATCH_IMPORT",
                "batch import",
                "IMPORT",
                resolveBatchSourceChannel(dto == null ? null : dto.getEvents()),
                null,
                createEvent
        );
    }

    public MarketEventBatchImportResultVO mockIngestMarketEvents(MarketEventMockIngestDTO dto,
                                                                 boolean mockIngestEnabled,
                                                                 Function<MarketEventCreateDTO, MarketEventCreateResultVO> createEvent) {
        if (!mockIngestEnabled) {
            throw new BizException("MARKET_EVENT_MOCK_INGEST_LOCAL_ONLY", "mock ingest is only available in local/demo/test mode");
        }
        validateMockIngestRequest(dto);
        EventSourceConfigItemVO sourceConfig = eventSourceConfigService.findSource(dto.getSourcePreset());
        if (sourceConfig == null) {
            throw new BizException("MARKET_EVENT_SOURCE_PRESET_NOT_FOUND", "mock ingest source config not found");
        }
        if (!Boolean.TRUE.equals(sourceConfig.getEnabled())) {
            throw new BizException("MARKET_EVENT_SOURCE_PRESET_DISABLED", "mock ingest source disabled");
        }
        if (!Boolean.TRUE.equals(sourceConfig.getSupportsMockIngest())) {
            throw new BizException("MARKET_EVENT_SOURCE_PRESET_UNSUPPORTED", "source does not support mock ingest");
        }
        MarketEventBatchImportDTO importDTO = new MarketEventBatchImportDTO();
        importDTO.setEvents(marketEventMockIngestGenerator.generate(dto));
        return executeBatchImport(
                importDTO,
                "MOCK_INGEST",
                "mock ingest",
                marketEventNormalizationManager.defaultIfBlank(sourceConfig.getSourceCode(), dto.getSourcePreset()),
                marketEventNormalizationManager.defaultIfBlank(sourceConfig.getSourceName(), "mock ingest"),
                marketEventNormalizationManager.defaultIfBlank(sourceConfig.getSourceCategory(), "MOCK"),
                marketEventNormalizationManager.defaultIfBlank(sourceConfig.getSourceChannel(), null),
                buildMockSourceDetail(dto),
                createEvent
        );
    }

    public MarketEventBatchImportResultVO syncMarketEventSource(String sourceCode,
                                                               MarketEventSourceSyncDTO dto,
                                                               Function<MarketEventCreateDTO, MarketEventCreateResultVO> createEvent) {
        validateSourceSyncRequest(sourceCode, dto);
        EventSourceConfigItemVO sourceConfig = eventSourceConfigService.findSource(sourceCode);
        if (sourceConfig == null) {
            throw new BizException("MARKET_EVENT_SOURCE_NOT_FOUND", "event source config not found");
        }
        if (!Boolean.TRUE.equals(sourceConfig.getEnabled())) {
            throw new BizException("MARKET_EVENT_SOURCE_DISABLED", "event source disabled");
        }

        SourceIngestResult ingestResult = dataIngestService.ingestMarketEventSource(
                sourceConfig,
                dto,
                (importDTO, sourceDetail) -> executeBatchImport(
                        importDTO,
                        "SOURCE_SYNC",
                        marketEventNormalizationManager.defaultIfBlank(sourceConfig.getSourceName(), "event source sync"),
                        marketEventNormalizationManager.defaultIfBlank(sourceConfig.getSourceCode(), sourceCode),
                        marketEventNormalizationManager.defaultIfBlank(sourceConfig.getSourceName(), "event source sync"),
                        marketEventNormalizationManager.defaultIfBlank(sourceConfig.getSourceCategory(), "SOURCE"),
                        marketEventNormalizationManager.defaultIfBlank(sourceConfig.getSourceChannel(), null),
                        sourceDetail,
                        createEvent
                )
        );
        return ingestResult.getImportResult();
    }

    public String resolveExceptionMessage(Exception e) {
        if (e instanceof BizException bizException && StringUtils.hasText(bizException.getMessage())) {
            return bizException.getMessage();
        }
        return e == null ? "batch import failed" : marketEventNormalizationManager.defaultIfBlank(e.getMessage(), "batch import failed");
    }

    private MarketEventBatchImportResultVO executeBatchImport(MarketEventBatchImportDTO dto,
                                                             String sourceType,
                                                             String sourceLabel,
                                                             String sourceCode,
                                                             String sourceName,
                                                             String sourceCategory,
                                                             String sourceChannel,
                                                             String sourceDetail,
                                                             Function<MarketEventCreateDTO, MarketEventCreateResultVO> createEvent) {
        return marketEventBatchImportManager.executeBatchImport(
                dto,
                new MarketEventBatchImportManager.BatchImportSource(
                        sourceType,
                        sourceLabel,
                        sourceCode,
                        sourceName,
                        sourceCategory,
                        sourceChannel,
                        sourceDetail
                ),
                createEvent,
                this::buildBatchSourceDetail,
                this::resolveExceptionMessage,
                this::appendBatchIngestHistory
        );
    }

    private void validateMockIngestRequest(MarketEventMockIngestDTO dto) {
        if (dto == null) {
            throw new BizException("MARKET_EVENT_MOCK_INGEST_EMPTY", "mock ingest request cannot be empty");
        }
        if (!StringUtils.hasText(dto.getTargetCode())) {
            throw new BizException("MARKET_EVENT_TARGET_CODE_EMPTY", "target code cannot be empty");
        }
        if (!StringUtils.hasText(dto.getTargetName())) {
            throw new BizException("MARKET_EVENT_TARGET_NAME_EMPTY", "target name cannot be empty");
        }
        if (!StringUtils.hasText(dto.getSourcePreset())) {
            throw new BizException("MARKET_EVENT_SOURCE_PRESET_EMPTY", "mock ingest source cannot be empty");
        }
    }

    private void validateSourceSyncRequest(String sourceCode, MarketEventSourceSyncDTO dto) {
        if (!StringUtils.hasText(sourceCode)) {
            throw new BizException("MARKET_EVENT_SOURCE_CODE_EMPTY", "event source code cannot be empty");
        }
        if (dto == null) {
            throw new BizException("MARKET_EVENT_SOURCE_SYNC_EMPTY", "event source sync request cannot be empty");
        }
        if (!StringUtils.hasText(dto.getTargetCode())) {
            throw new BizException("MARKET_EVENT_TARGET_CODE_EMPTY", "target code cannot be empty");
        }
        if (!StringUtils.hasText(dto.getTargetName())) {
            throw new BizException("MARKET_EVENT_TARGET_NAME_EMPTY", "target name cannot be empty");
        }
    }

    private void appendBatchIngestHistory(String sourceType,
                                          String sourceLabel,
                                          String sourceCode,
                                          String sourceName,
                                          String sourceCategory,
                                          String sourceChannel,
                                          String sourceDetail,
                                          MarketEventBatchImportResultVO result) {
        if (result == null) {
            return;
        }
        String summary = String.format(
                "%s, total %d, success %d, failed %d, auto queued %d",
                marketEventNormalizationManager.defaultIfBlank(sourceLabel, "event ingest"),
                defaultIfNull(result.getTotalCount()),
                defaultIfNull(result.getSuccessCount()),
                defaultIfNull(result.getFailedCount()),
                defaultIfNull(result.getAutoTriggeredCount())
        );
        marketEventIngestHistoryService.appendHistory(
                sourceType,
                sourceLabel,
                sourceCode,
                sourceName,
                sourceCategory,
                sourceChannel,
                sourceDetail,
                result.getTotalCount(),
                result.getSuccessCount(),
                result.getFailedCount(),
                result.getDuplicateCount(),
                result.getAutoTriggeredCount(),
                summary
        );
    }

    private String buildBatchSourceDetail(List<MarketEventCreateDTO> events) {
        if (events == null || events.isEmpty()) {
            return "batch import";
        }
        MarketEventCreateDTO first = events.get(0);
        String base = String.format(
                "%s / %s",
                marketEventNormalizationManager.defaultIfBlank(first == null ? null : marketEventNormalizationManager.trimToNull(first.getTargetCode()), "-"),
                marketEventNormalizationManager.defaultIfBlank(first == null ? null : marketEventNormalizationManager.trimToNull(first.getTargetName()), "-")
        );
        if (events.size() == 1) {
            return base;
        }
        return base + " and " + events.size() + " events";
    }

    private String resolveBatchSourceChannel(List<MarketEventCreateDTO> events) {
        if (events == null || events.isEmpty()) {
            return null;
        }
        for (MarketEventCreateDTO item : events) {
            String sourceChannel = marketEventNormalizationManager.normalizeSourceChannel(item == null ? null : item.getSourceChannel(), item == null ? null : item.getEventType());
            if (StringUtils.hasText(sourceChannel)) {
                return sourceChannel;
            }
        }
        return null;
    }

    private String buildMockSourceDetail(MarketEventMockIngestDTO dto) {
        EventSourceConfigItemVO source = eventSourceConfigService.findSource(dto == null ? null : dto.getSourcePreset());
        String presetLabel = source == null ? resolveMockPresetLabel(dto == null ? null : dto.getSourcePreset()) : marketEventNormalizationManager.defaultIfBlank(source.getSourceName(), "mock ingest");
        return String.format(
                "%s / %s / %s",
                presetLabel,
                marketEventNormalizationManager.defaultIfBlank(dto == null ? null : marketEventNormalizationManager.trimToNull(dto.getTargetCode()), "-"),
                marketEventNormalizationManager.defaultIfBlank(dto == null ? null : marketEventNormalizationManager.trimToNull(dto.getTargetName()), "-")
        );
    }

    private String resolveMockPresetLabel(String sourcePreset) {
        if (!StringUtils.hasText(sourcePreset)) {
            return "mock source";
        }
        return switch (sourcePreset.trim().toUpperCase(Locale.ROOT)) {
            case "EXCHANGE_ANNOUNCEMENT" -> "exchange announcement source";
            case "POLICY_TRACKER" -> "policy tracker source";
            case "RISK_MONITOR" -> "risk monitor source";
            case "NEWS_WIRE" -> "news wire source";
            default -> "mock source";
        };
    }

    private int defaultIfNull(Integer value) {
        return value == null ? 0 : value;
    }
}
