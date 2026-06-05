package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.dto.MarketEventCreateDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventSourceSyncDTO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.aiorchestrator.domain.vo.EventSourceRequestDiagnosticItemVO;
import com.quant.aiorchestrator.dataingest.SourceFetchResult;
import com.quant.aiorchestrator.dataingest.SourceFetchStatus;
import com.quant.aiorchestrator.dataingest.SourceProvenance;

import java.util.List;

public interface EventSourceSyncAdapter {

    boolean supports(EventSourceConfigItemVO sourceConfig);

    List<MarketEventCreateDTO> sync(EventSourceConfigItemVO sourceConfig, MarketEventSourceSyncDTO request);

    default SourceFetchResult fetch(EventSourceConfigItemVO sourceConfig, MarketEventSourceSyncDTO request) {
        return SourceFetchResult.builder()
                .status(SourceFetchStatus.FETCHED)
                .provenance(SourceProvenance.from(sourceConfig, request == null ? null : request.getTargetCode()))
                .standardizedEvents(sync(sourceConfig, request))
                .attemptNo(1)
                .maxAttempts(1)
                .build();
    }

    default List<EventSourceRequestDiagnosticItemVO> diagnose(EventSourceConfigItemVO sourceConfig, MarketEventSourceSyncDTO request) {
        return List.of();
    }
}
