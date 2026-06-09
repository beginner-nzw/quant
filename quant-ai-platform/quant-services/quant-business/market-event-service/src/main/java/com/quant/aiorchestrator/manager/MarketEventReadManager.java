package com.quant.aiorchestrator.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quant.aiorchestrator.domain.entity.MarketEventAnalysisDO;
import com.quant.aiorchestrator.domain.entity.MarketEventDO;
import com.quant.aiorchestrator.domain.entity.MarketEventRelationDO;
import com.quant.aiorchestrator.mapper.MarketEventAnalysisMapper;
import com.quant.aiorchestrator.mapper.MarketEventMapper;
import com.quant.aiorchestrator.mapper.MarketEventRelationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MarketEventReadManager {

    private final MarketEventMapper marketEventMapper;
    private final MarketEventAnalysisMapper marketEventAnalysisMapper;
    private final MarketEventRelationMapper marketEventRelationMapper;

    public List<MarketEventDO> listEventsForProjection() {
        return marketEventMapper.selectList(
                new LambdaQueryWrapper<MarketEventDO>()
                        .eq(MarketEventDO::getDeleted, 0)
                        .orderByDesc(MarketEventDO::getOccurredAt, MarketEventDO::getCreatedAt, MarketEventDO::getId)
        );
    }

    public Map<String, List<MarketEventRelationDO>> loadRelationMap(List<String> eventIds) {
        List<String> validEventIds = distinctText(eventIds);
        if (validEventIds.isEmpty()) {
            return Map.of();
        }
        return marketEventRelationMapper.selectList(
                new LambdaQueryWrapper<MarketEventRelationDO>()
                        .eq(MarketEventRelationDO::getDeleted, 0)
                        .in(MarketEventRelationDO::getEventId, validEventIds)
                        .orderByAsc(MarketEventRelationDO::getEventId, MarketEventRelationDO::getId)
        ).stream().collect(Collectors.groupingBy(MarketEventRelationDO::getEventId, LinkedHashMap::new, Collectors.toList()));
    }

    public Map<String, MarketEventAnalysisDO> loadLatestAnalysisMap(List<String> eventIds) {
        List<String> validEventIds = distinctText(eventIds);
        if (validEventIds.isEmpty()) {
            return Map.of();
        }
        return marketEventAnalysisMapper.selectList(
                new LambdaQueryWrapper<MarketEventAnalysisDO>()
                        .eq(MarketEventAnalysisDO::getDeleted, 0)
                        .in(MarketEventAnalysisDO::getEventId, validEventIds)
                        .orderByDesc(MarketEventAnalysisDO::getCreatedAt, MarketEventAnalysisDO::getId)
        ).stream()
                .filter(item -> StringUtils.hasText(item.getEventId()))
                .collect(Collectors.toMap(
                        MarketEventAnalysisDO::getEventId,
                        item -> item,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private List<String> distinctText(List<String> values) {
        return values == null ? List.of() : values.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }
}
