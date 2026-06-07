package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.dto.MarketEventCreateDTO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.aiorchestrator.domain.vo.EventSourcePreviewItemVO;
import com.quant.aiorchestrator.domain.vo.EventSourcePreviewResultVO;
import com.quant.aiorchestrator.domain.vo.EventSourceRequestDiagnosticItemVO;
import com.quant.aiorchestrator.domain.vo.EventSourceRequestDiagnosticResultVO;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class EventSourcePreviewProjectionManager {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public EventSourcePreviewResultVO toPreviewResult(EventSourceConfigItemVO sourceConfig, List<MarketEventCreateDTO> events) {
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

    public EventSourceRequestDiagnosticResultVO toDiagnosticResult(
            EventSourceConfigItemVO sourceConfig,
            List<EventSourceRequestDiagnosticItemVO> items
    ) {
        EventSourceRequestDiagnosticResultVO result = new EventSourceRequestDiagnosticResultVO();
        result.setSourceCode(sourceConfig.getSourceCode());
        result.setSourceName(sourceConfig.getSourceName());
        result.setIngestMode(sourceConfig.getIngestMode());
        result.setDiagnosedAt(LocalDateTime.now().format(DATETIME_FORMATTER));
        result.setItems(items);
        return result;
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
