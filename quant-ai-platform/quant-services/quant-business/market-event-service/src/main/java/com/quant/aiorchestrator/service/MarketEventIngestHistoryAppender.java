package com.quant.aiorchestrator.service;

public interface MarketEventIngestHistoryAppender {
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
}
