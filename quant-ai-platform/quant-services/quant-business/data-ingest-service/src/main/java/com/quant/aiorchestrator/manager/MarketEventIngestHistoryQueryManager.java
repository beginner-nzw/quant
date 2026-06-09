package com.quant.aiorchestrator.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quant.aiorchestrator.domain.entity.MarketEventIngestRunDO;
import com.quant.aiorchestrator.domain.vo.MarketEventIngestHistoryItemVO;
import com.quant.aiorchestrator.mapper.MarketEventIngestRunMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MarketEventIngestHistoryQueryManager {

    private final MarketEventIngestRunMapper marketEventIngestRunMapper;
    private final MarketEventIngestHistoryItemManager historyItemManager;
    private final MarketEventIngestHistoryStoreManager historyStoreManager;

    public List<MarketEventIngestHistoryItemVO> loadRecentHistory(String eventIngestHistoryPath) {
        List<MarketEventIngestHistoryItemVO> dbHistory = loadRecentDbHistory();
        if (!dbHistory.isEmpty()) {
            return dbHistory;
        }
        List<Map<String, Object>> items = historyStoreManager.loadFileHistoryItems(eventIngestHistoryPath);
        List<MarketEventIngestHistoryItemVO> result = new ArrayList<>();
        for (Map<String, Object> item : items) {
            result.add(historyItemManager.toHistoryItem(item));
        }
        return result;
    }

    private List<MarketEventIngestHistoryItemVO> loadRecentDbHistory() {
        try {
            List<MarketEventIngestRunDO> runs = marketEventIngestRunMapper.selectList(
                    new LambdaQueryWrapper<MarketEventIngestRunDO>()
                            .orderByDesc(MarketEventIngestRunDO::getCreatedAt)
                            .last("limit 100")
            );
            if (runs == null || runs.isEmpty()) {
                return List.of();
            }
            List<MarketEventIngestHistoryItemVO> result = new ArrayList<>();
            for (MarketEventIngestRunDO run : runs) {
                result.add(historyItemManager.toHistoryItem(run));
            }
            return result;
        } catch (Exception ignored) {
            return List.of();
        }
    }
}
