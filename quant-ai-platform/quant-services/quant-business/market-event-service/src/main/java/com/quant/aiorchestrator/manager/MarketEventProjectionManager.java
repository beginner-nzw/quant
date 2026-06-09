package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.dto.MarketEventPageQueryDTO;
import com.quant.aiorchestrator.domain.entity.MarketEventAnalysisDO;
import com.quant.aiorchestrator.domain.entity.MarketEventDO;
import com.quant.aiorchestrator.domain.entity.MarketEventRelationDO;
import com.quant.aiorchestrator.domain.projection.MarketEventFollowUpProjection;
import com.quant.aiorchestrator.domain.vo.MarketEventListItemVO;
import com.quant.aiorchestrator.service.MarketEventFollowUpProjectionProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Component
@RequiredArgsConstructor
public class MarketEventProjectionManager {

    private final MarketEventReadManager marketEventReadManager;
    private final MarketEventListItemAssembler listItemAssembler;
    private final MarketEventFollowUpProjectionProvider marketEventFollowUpProjectionProvider;

    public List<MarketEventListItemVO> listMatchedEvents(MarketEventPageQueryDTO queryDTO) {
        MarketEventPageQueryDTO safeQuery = queryDTO == null ? new MarketEventPageQueryDTO() : queryDTO;
        List<MarketEventDO> events = marketEventReadManager.listEventsForProjection();
        if (events.isEmpty()) {
            return List.of();
        }

        List<String> eventIds = events.stream()
                .map(MarketEventDO::getEventId)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();

        Map<String, List<MarketEventRelationDO>> relationMap = marketEventReadManager.loadRelationMap(eventIds);
        Map<String, MarketEventAnalysisDO> analysisMap = marketEventReadManager.loadLatestAnalysisMap(eventIds);
        Map<String, MarketEventFollowUpProjection> followUpProjectionMap = marketEventFollowUpProjectionProvider.loadFollowUpProjectionMap(eventIds);

        return events.stream()
                .map(item -> listItemAssembler.toMarketEventItem(
                        item,
                        relationMap.getOrDefault(item.getEventId(), List.of()),
                        followUpProjectionMap.get(item.getEventId()),
                        analysisMap.get(item.getEventId())
                ))
                .filter(matchesTargetCode(safeQuery.getTargetCode()))
                .filter(matchesTargetName(safeQuery.getTargetName()))
                .filter(matchesIgnoreCase(MarketEventListItemVO::getEventType, safeQuery.getEventType()))
                .filter(matchesIgnoreCase(MarketEventListItemVO::getImpactLevel, safeQuery.getImpactLevel()))
                .filter(matchesIgnoreCase(MarketEventListItemVO::getEventStatus, safeQuery.getEventStatus()))
                .toList();
    }

    public MarketEventListItemVO buildMarketEventDetail(MarketEventDO event) {
        if (event == null || !StringUtils.hasText(event.getEventId())) {
            return null;
        }
        String eventId = event.getEventId();
        Map<String, List<MarketEventRelationDO>> relationMap = marketEventReadManager.loadRelationMap(List.of(eventId));
        Map<String, MarketEventAnalysisDO> analysisMap = marketEventReadManager.loadLatestAnalysisMap(List.of(eventId));
        Map<String, MarketEventFollowUpProjection> followUpProjectionMap = marketEventFollowUpProjectionProvider.loadFollowUpProjectionMap(List.of(eventId));
        return listItemAssembler.toMarketEventItem(
                event,
                relationMap.getOrDefault(eventId, List.of()),
                followUpProjectionMap.get(eventId),
                analysisMap.get(eventId)
        );
    }


    private Predicate<MarketEventListItemVO> matchesTargetCode(String targetCode) {
        if (!StringUtils.hasText(targetCode)) {
            return item -> true;
        }
        String keyword = targetCode.trim().toUpperCase();
        return item -> item.getTargetCode() != null && item.getTargetCode().toUpperCase().contains(keyword)
                || (item.getRelations() != null && item.getRelations().stream()
                .anyMatch(relation -> relation.getRelationCode() != null
                        && relation.getRelationCode().toUpperCase().contains(keyword)));
    }

    private Predicate<MarketEventListItemVO> matchesTargetName(String targetName) {
        if (!StringUtils.hasText(targetName)) {
            return item -> true;
        }
        String keyword = targetName.trim().toUpperCase();
        return item -> item.getTargetName() != null && item.getTargetName().toUpperCase().contains(keyword);
    }

    private Predicate<MarketEventListItemVO> matchesIgnoreCase(
            java.util.function.Function<MarketEventListItemVO, String> extractor,
            String expected
    ) {
        if (!StringUtils.hasText(expected)) {
            return item -> true;
        }
        String normalized = expected.trim().toUpperCase();
        return item -> {
            String value = extractor.apply(item);
            return value != null && normalized.equals(value.trim().toUpperCase());
        };
    }

}
