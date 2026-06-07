package com.quant.aiorchestrator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.dataingest.SourceProvenance;
import com.quant.aiorchestrator.dataingest.SourceRawPayload;
import com.quant.aiorchestrator.domain.dto.MarketEventCreateDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventMockIngestDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventSourceSyncDTO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
public class MockEventSourceSyncAdapter implements EventSourceSyncAdapter {

    private final MarketEventMockIngestGenerator marketEventMockIngestGenerator;
    private final ObjectMapper objectMapper;

    public MockEventSourceSyncAdapter(MarketEventMockIngestGenerator marketEventMockIngestGenerator,
                                      ObjectMapper objectMapper) {
        this.marketEventMockIngestGenerator = marketEventMockIngestGenerator;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(EventSourceConfigItemVO sourceConfig) {
        return sourceConfig != null
                && StringUtils.hasText(sourceConfig.getIngestMode())
                && "MOCK".equalsIgnoreCase(sourceConfig.getIngestMode().trim());
    }

    @Override
    public SourceRawPayload fetchRaw(EventSourceConfigItemVO sourceConfig, MarketEventSourceSyncDTO request) {
        try {
            return SourceRawPayload.builder()
                    .provenance(SourceProvenance.from(sourceConfig, request == null ? null : request.getTargetCode()))
                    .requestMethod("LOCAL_DEMO")
                    .requestUrl("local-demo://mock-event-source/" + (sourceConfig == null ? "unknown" : sourceConfig.getSourceCode()))
                    .body(objectMapper.writeValueAsString(request))
                    .build();
        } catch (Exception e) {
            return SourceRawPayload.builder()
                    .provenance(SourceProvenance.from(sourceConfig, request == null ? null : request.getTargetCode()))
                    .requestMethod("LOCAL_DEMO")
                    .requestUrl("local-demo://mock-event-source/" + (sourceConfig == null ? "unknown" : sourceConfig.getSourceCode()))
                    .body("{}")
                    .build();
        }
    }

    @Override
    public List<MarketEventCreateDTO> standardize(SourceRawPayload rawPayload,
                                                  EventSourceConfigItemVO sourceConfig,
                                                  MarketEventSourceSyncDTO request) {
        MarketEventMockIngestDTO dto = new MarketEventMockIngestDTO();
        dto.setTargetType(request == null ? null : request.getTargetType());
        dto.setTargetCode(request == null ? null : request.getTargetCode());
        dto.setTargetName(request == null ? null : request.getTargetName());
        dto.setItemCount(request == null ? null : request.getItemCount());
        dto.setSourcePreset(sourceConfig == null ? null : sourceConfig.getSourceCode());
        return marketEventMockIngestGenerator.generate(dto);
    }
}
