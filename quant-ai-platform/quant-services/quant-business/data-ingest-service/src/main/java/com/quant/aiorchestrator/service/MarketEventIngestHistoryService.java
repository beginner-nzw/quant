package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.vo.MarketEventIngestHistoryItemVO;
import java.util.List;

public interface MarketEventIngestHistoryService extends MarketEventIngestHistoryAppender {
    @Override
    void appendHistory(String sourceType,
                       String sourceLabel,
                       String sourceCode,
                       String sourceName,
                       String sourceCategory,
                       String sourceChannel,
                       String sourceDetail,
                       Integer totalCount,
                       Integer successCount,
                       Integer failedCount,
                       Integer duplicateCount,
                       Integer autoTriggeredCount,
                       String summary);

    void appendHistory(String sourceType,
                       String sourceLabel,
                       String sourceCode,
                       String sourceName,
                       String sourceCategory,
                       String sourceChannel,
                       String sourceDetail,
                       Integer totalCount,
                       Integer successCount,
                       Integer failedCount,
                       Integer duplicateCount,
                       Integer autoTriggeredCount,
                       String resultStatus,
                       String errorMessage,
                       String summary);

    List<MarketEventIngestHistoryItemVO> loadRecentHistory();
}
