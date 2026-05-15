package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.dto.MarketEventCreateDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventSourceSyncDTO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.aiorchestrator.domain.vo.EventSourcePreviewItemVO;
import com.quant.aiorchestrator.domain.vo.EventSourcePreviewResultVO;
import com.quant.aiorchestrator.domain.vo.EventSourceRequestDiagnosticResultVO;
import com.quant.common.core.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventSourcePreviewService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final EventSourceConfigService eventSourceConfigService;
    private final List<EventSourceSyncAdapter> eventSourceSyncAdapters;

    public EventSourcePreviewResultVO previewSource(String sourceCode, MarketEventSourceSyncDTO dto) {
        EventSourceConfigItemVO sourceConfig = resolveValidatedSourceConfig(sourceCode, dto);
        EventSourceSyncAdapter adapter = resolveAdapter(
                sourceConfig,
                "MARKET_EVENT_SOURCE_PREVIEW_UNSUPPORTED",
                "当前事件源不支持预览"
        );
        List<MarketEventCreateDTO> events = adapter.sync(sourceConfig, dto);
        EventSourcePreviewResultVO result = new EventSourcePreviewResultVO();
        result.setSourceCode(sourceConfig.getSourceCode());
        result.setSourceName(sourceConfig.getSourceName());
        result.setSourceCategory(sourceConfig.getSourceCategory());
        result.setIngestMode(sourceConfig.getIngestMode());
        result.setEndpointUrl(sourceConfig.getEndpointUrl());
        result.setUpstreamUrl(sourceConfig.getUpstreamUrl());
        result.setItemCount(events == null ? 0 : events.size());
        result.setPreviewedAt(LocalDateTime.now().format(DATETIME_FORMATTER));
        result.setItems(events == null ? List.of() : events.stream().map(this::toPreviewItem).toList());
        return result;
    }

    public EventSourceRequestDiagnosticResultVO diagnoseSource(String sourceCode, MarketEventSourceSyncDTO dto) {
        EventSourceConfigItemVO sourceConfig = resolveValidatedSourceConfig(sourceCode, dto);
        EventSourceSyncAdapter adapter = resolveAdapter(
                sourceConfig,
                "MARKET_EVENT_SOURCE_DIAGNOSE_UNSUPPORTED",
                "当前事件源不支持请求诊断"
        );
        EventSourceRequestDiagnosticResultVO result = new EventSourceRequestDiagnosticResultVO();
        result.setSourceCode(sourceConfig.getSourceCode());
        result.setSourceName(sourceConfig.getSourceName());
        result.setIngestMode(sourceConfig.getIngestMode());
        result.setDiagnosedAt(LocalDateTime.now().format(DATETIME_FORMATTER));
        result.setItems(adapter.diagnose(sourceConfig, dto));
        return result;
    }

    private EventSourceConfigItemVO resolveValidatedSourceConfig(String sourceCode, MarketEventSourceSyncDTO dto) {
        if (!StringUtils.hasText(sourceCode)) {
            throw new BizException("MARKET_EVENT_SOURCE_CODE_EMPTY", "事件源编码不能为空");
        }
        if (dto == null) {
            throw new BizException("MARKET_EVENT_SOURCE_PREVIEW_EMPTY", "事件源预览请求不能为空");
        }
        if (!StringUtils.hasText(dto.getTargetCode())) {
            throw new BizException("MARKET_EVENT_TARGET_CODE_EMPTY", "标的代码不能为空");
        }
        if (!StringUtils.hasText(dto.getTargetName())) {
            throw new BizException("MARKET_EVENT_TARGET_NAME_EMPTY", "标的名称不能为空");
        }

        EventSourceConfigItemVO sourceConfig = eventSourceConfigService.findSource(sourceCode);
        if (sourceConfig == null) {
            throw new BizException("MARKET_EVENT_SOURCE_NOT_FOUND", "事件源配置不存在");
        }
        return sourceConfig;
    }

    private EventSourceSyncAdapter resolveAdapter(EventSourceConfigItemVO sourceConfig, String errorCode, String errorMessage) {
        EventSourceSyncAdapter adapter = eventSourceSyncAdapters.stream()
                .filter(item -> item.supports(sourceConfig))
                .findFirst()
                .orElse(null);
        if (adapter == null) {
            throw new BizException(errorCode, errorMessage);
        }
        return adapter;
    }

    private EventSourcePreviewItemVO toPreviewItem(MarketEventCreateDTO dto) {
        EventSourcePreviewItemVO item = new EventSourcePreviewItemVO();
        item.setTargetType(dto == null ? null : dto.getTargetType());
        item.setTargetCode(dto == null ? null : dto.getTargetCode());
        item.setTargetName(dto == null ? null : dto.getTargetName());
        item.setEventType(dto == null ? null : dto.getEventType());
        item.setEventTitle(dto == null ? null : dto.getEventTitle());
        item.setEventSummary(dto == null ? null : dto.getEventSummary());
        item.setSourceChannel(dto == null ? null : dto.getSourceChannel());
        item.setSourceUrl(dto == null ? null : dto.getSourceUrl());
        item.setImpactLevel(dto == null ? null : dto.getImpactLevel());
        item.setEventStatus(dto == null ? null : dto.getEventStatus());
        item.setOccurredAt(dto == null || dto.getOccurredAt() == null ? null : dto.getOccurredAt().format(DATETIME_FORMATTER));
        return item;
    }
}
