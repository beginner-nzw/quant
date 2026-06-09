package com.quant.aiorchestrator.dataingest;

import com.quant.aiorchestrator.domain.vo.MarketEventBatchImportResultVO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SourceIngestResult {
    private SourceFetchResult fetchResult;
    private MarketEventBatchImportResultVO importResult;
}
