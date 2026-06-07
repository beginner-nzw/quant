package com.quant.aiorchestrator.manager;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class MarketEventIngestHistoryCommandManager {

    private final MarketEventIngestHistoryItemManager historyItemManager;
    private final MarketEventIngestHistoryStoreManager historyStoreManager;

    public void appendHistory(String eventIngestHistoryPath,
                              String sourceType,
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
                              String summary) {
        appendHistory(
                eventIngestHistoryPath,
                sourceType,
                sourceLabel,
                sourceCode,
                sourceName,
                sourceCategory,
                sourceChannel,
                sourceDetail,
                totalCount,
                successCount,
                failedCount,
                duplicateCount,
                autoTriggeredCount,
                historyItemManager.resolveResultStatus(successCount, failedCount),
                null,
                summary
        );
    }

    public void appendHistory(String eventIngestHistoryPath,
                              String sourceType,
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
                              String summary) {
        Map<String, Object> item = historyItemManager.buildHistoryItem(
                sourceType,
                sourceLabel,
                sourceCode,
                sourceName,
                sourceCategory,
                sourceChannel,
                sourceDetail,
                totalCount,
                successCount,
                failedCount,
                duplicateCount,
                autoTriggeredCount,
                resultStatus,
                errorMessage,
                summary
        );
        historyStoreManager.prependHistoryItem(eventIngestHistoryPath, item);
    }
}
