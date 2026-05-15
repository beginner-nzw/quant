package com.quant.aiorchestrator.service;

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

    public MockEventSourceSyncAdapter(MarketEventMockIngestGenerator marketEventMockIngestGenerator) {
        this.marketEventMockIngestGenerator = marketEventMockIngestGenerator;
    }

    @Override
    public boolean supports(EventSourceConfigItemVO sourceConfig) {
        return sourceConfig != null
                && StringUtils.hasText(sourceConfig.getIngestMode())
                && "MOCK".equalsIgnoreCase(sourceConfig.getIngestMode().trim());
    }

    @Override
    public List<MarketEventCreateDTO> sync(EventSourceConfigItemVO sourceConfig, MarketEventSourceSyncDTO request) {
        MarketEventMockIngestDTO dto = new MarketEventMockIngestDTO();
        dto.setTargetType(request == null ? null : request.getTargetType());
        dto.setTargetCode(request == null ? null : request.getTargetCode());
        dto.setTargetName(request == null ? null : request.getTargetName());
        dto.setItemCount(request == null ? null : request.getItemCount());
        dto.setSourcePreset(sourceConfig == null ? null : sourceConfig.getSourceCode());
        return marketEventMockIngestGenerator.generate(dto);
    }
}
