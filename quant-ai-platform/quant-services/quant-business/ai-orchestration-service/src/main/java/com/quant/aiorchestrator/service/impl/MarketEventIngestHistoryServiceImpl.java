package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.manager.MarketEventIngestHistoryCommandManager;
import com.quant.aiorchestrator.manager.MarketEventIngestHistoryQueryManager;
import com.quant.aiorchestrator.service.MarketEventIngestHistoryService;
import com.quant.aiorchestrator.domain.vo.MarketEventIngestHistoryItemVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MarketEventIngestHistoryServiceImpl implements MarketEventIngestHistoryService {

    private final String eventIngestHistoryPath;
    private final MarketEventIngestHistoryCommandManager historyCommandManager;
    private final MarketEventIngestHistoryQueryManager historyQueryManager;

    public MarketEventIngestHistoryServiceImpl(
            @Value("${quant.ai.event-ingest-history:../../../ai-config/event-ingest-histories.json}") String eventIngestHistoryPath,
            MarketEventIngestHistoryCommandManager historyCommandManager,
            MarketEventIngestHistoryQueryManager historyQueryManager
    ) {
        this.eventIngestHistoryPath = eventIngestHistoryPath;
        this.historyCommandManager = historyCommandManager;
        this.historyQueryManager = historyQueryManager;
    }

    public void appendHistory(String sourceType,
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
        historyCommandManager.appendHistory(
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
                summary
        );
    }

    public void appendHistory(String sourceType,
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
        historyCommandManager.appendHistory(
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
                resultStatus,
                errorMessage,
                summary
        );
    }

    public List<MarketEventIngestHistoryItemVO> loadRecentHistory() {
        return historyQueryManager.loadRecentHistory(eventIngestHistoryPath);
    }
}
