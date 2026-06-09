package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class MarketEventBatchPreviewResultVO {

    private Integer totalCount;
    private Integer validCount;
    private Integer invalidCount;
    private Integer duplicateCount;
    private Integer autoTriggerCandidateCount;
    private List<MarketEventBatchPreviewItemVO> items;
}
