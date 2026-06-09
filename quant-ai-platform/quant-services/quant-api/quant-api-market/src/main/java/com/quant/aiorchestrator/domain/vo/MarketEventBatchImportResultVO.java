package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class MarketEventBatchImportResultVO {

    private Integer totalCount;
    private Integer successCount;
    private Integer failedCount;
    private Integer duplicateCount;
    private Integer autoTriggeredCount;
    private List<MarketEventBatchImportItemVO> items;
}
