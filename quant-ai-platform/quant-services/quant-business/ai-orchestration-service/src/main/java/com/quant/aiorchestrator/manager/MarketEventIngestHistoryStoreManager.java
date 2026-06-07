package com.quant.aiorchestrator.manager;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MarketEventIngestHistoryStoreManager {

    private static final int MAX_HISTORY_ITEMS = 100;

    private final MarketEventIngestHistoryFileManager historyFileManager;

    public void prependHistoryItem(String eventIngestHistoryPath, Map<String, Object> item) {
        List<Map<String, Object>> items = historyFileManager.readHistoryItems(eventIngestHistoryPath);
        items.add(0, item);
        if (items.size() > MAX_HISTORY_ITEMS) {
            items = new ArrayList<>(items.subList(0, MAX_HISTORY_ITEMS));
        }
        historyFileManager.writeHistoryItems(eventIngestHistoryPath, items);
    }

    public List<Map<String, Object>> loadFileHistoryItems(String eventIngestHistoryPath) {
        return historyFileManager.readHistoryItems(eventIngestHistoryPath);
    }
}
