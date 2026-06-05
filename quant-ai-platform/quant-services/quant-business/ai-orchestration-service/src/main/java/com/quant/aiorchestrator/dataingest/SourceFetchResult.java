package com.quant.aiorchestrator.dataingest;

import com.quant.aiorchestrator.domain.dto.MarketEventCreateDTO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SourceFetchResult {
    private SourceFetchStatus status;
    private SourceProvenance provenance;
    private List<MarketEventCreateDTO> standardizedEvents;
    private Integer httpStatus;
    private Integer attemptNo;
    private Integer maxAttempts;
    private String errorCode;
    private String errorMessage;

    public boolean successful() {
        return status == SourceFetchStatus.FETCHED || status == SourceFetchStatus.STANDARDIZED;
    }
}
