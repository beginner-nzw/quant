package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.dto.MarketEventCreateDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventSourceSyncDTO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.aiorchestrator.domain.vo.EventSourceRequestDiagnosticItemVO;

import java.util.List;

public interface EventSourceSyncAdapter {

    boolean supports(EventSourceConfigItemVO sourceConfig);

    List<MarketEventCreateDTO> sync(EventSourceConfigItemVO sourceConfig, MarketEventSourceSyncDTO request);

    default List<EventSourceRequestDiagnosticItemVO> diagnose(EventSourceConfigItemVO sourceConfig, MarketEventSourceSyncDTO request) {
        return List.of();
    }
}
