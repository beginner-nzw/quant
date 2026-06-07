package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.dto.MarketEventCreateDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventSourceSyncDTO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.aiorchestrator.domain.vo.EventSourceRequestDiagnosticItemVO;
import com.quant.aiorchestrator.dataingest.SourceRawPayload;

import java.util.List;

public interface EventSourceSyncAdapter {

    boolean supports(EventSourceConfigItemVO sourceConfig);

    SourceRawPayload fetchRaw(EventSourceConfigItemVO sourceConfig, MarketEventSourceSyncDTO request);

    List<MarketEventCreateDTO> standardize(SourceRawPayload rawPayload,
                                           EventSourceConfigItemVO sourceConfig,
                                           MarketEventSourceSyncDTO request);

    default List<MarketEventCreateDTO> sync(EventSourceConfigItemVO sourceConfig, MarketEventSourceSyncDTO request) {
        return standardize(fetchRaw(sourceConfig, request), sourceConfig, request);
    }

    default List<EventSourceRequestDiagnosticItemVO> diagnose(EventSourceConfigItemVO sourceConfig, MarketEventSourceSyncDTO request) {
        return List.of();
    }
}
