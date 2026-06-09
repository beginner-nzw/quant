package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigVO;
import com.quant.aiorchestrator.domain.vo.MarketEventIngestHistoryItemVO;
import com.quant.aiorchestrator.market.EventSourceIngestStatsProvider;
import com.quant.aiorchestrator.service.MarketEventIngestHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class EventSourceIngestStatsManager implements EventSourceIngestStatsProvider {

    private final MarketEventIngestHistoryService marketEventIngestHistoryService;

    @Override
    public void enrichEventSourceConfigStats(EventSourceConfigVO eventSourceConfig) {
        if (eventSourceConfig == null || eventSourceConfig.getSources() == null || eventSourceConfig.getSources().isEmpty()) {
            return;
        }
        List<MarketEventIngestHistoryItemVO> histories = marketEventIngestHistoryService.loadRecentHistory();
        if (histories.isEmpty()) {
            return;
        }

        Map<String, List<MarketEventIngestHistoryItemVO>> grouped = histories.stream()
                .filter(item -> item.getSourceCode() != null && !item.getSourceCode().isBlank())
                .collect(Collectors.groupingBy(MarketEventIngestHistoryItemVO::getSourceCode, LinkedHashMap::new, Collectors.toList()));

        for (EventSourceConfigItemVO source : eventSourceConfig.getSources()) {
            if (source == null || source.getSourceCode() == null || source.getSourceCode().isBlank()) {
                continue;
            }
            List<MarketEventIngestHistoryItemVO> sourceHistories = grouped.get(source.getSourceCode());
            if (sourceHistories == null || sourceHistories.isEmpty()) {
                source.setIngestRecordCount(0);
                source.setTotalCount(0);
                source.setSuccessCount(0);
                source.setFailedCount(0);
                source.setDuplicateCount(0);
                source.setAutoTriggeredCount(0);
                source.setLastIngestAt(null);
                source.setLastResultStatus(null);
                source.setLastErrorMessage(null);
                continue;
            }
            MarketEventIngestHistoryItemVO latestHistory = sourceHistories.get(0);
            source.setIngestRecordCount(sourceHistories.size());
            source.setTotalCount(sourceHistories.stream().mapToInt(item -> defaultInt(item.getTotalCount())).sum());
            source.setSuccessCount(sourceHistories.stream().mapToInt(item -> defaultInt(item.getSuccessCount())).sum());
            source.setFailedCount(sourceHistories.stream().mapToInt(item -> defaultInt(item.getFailedCount())).sum());
            source.setDuplicateCount(sourceHistories.stream().mapToInt(item -> defaultInt(item.getDuplicateCount())).sum());
            source.setAutoTriggeredCount(sourceHistories.stream().mapToInt(item -> defaultInt(item.getAutoTriggeredCount())).sum());
            source.setLastIngestAt(sourceHistories.stream()
                    .map(MarketEventIngestHistoryItemVO::getCreatedAt)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null));
            source.setLastResultStatus(latestHistory == null ? null : latestHistory.getResultStatus());
            source.setLastErrorMessage(latestHistory == null ? null : latestHistory.getErrorMessage());
        }
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }
}
