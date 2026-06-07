package com.quant.aiorchestrator.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.dto.MarketEventPageQueryDTO;
import com.quant.aiorchestrator.domain.entity.MarketEventAnalysisDO;
import com.quant.aiorchestrator.domain.entity.MarketEventDO;
import com.quant.aiorchestrator.domain.entity.MarketEventRelationDO;
import com.quant.aiorchestrator.domain.entity.ResearchReportDO;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.aiorchestrator.domain.entity.RiskWarningDO;
import com.quant.aiorchestrator.domain.entity.RiskWarningDetailDO;
import com.quant.aiorchestrator.domain.vo.MarketEventListItemVO;
import com.quant.aiorchestrator.mapper.MarketEventAnalysisMapper;
import com.quant.aiorchestrator.mapper.MarketEventMapper;
import com.quant.aiorchestrator.mapper.MarketEventRelationMapper;
import com.quant.aiorchestrator.mapper.ResearchReportMapper;
import com.quant.aiorchestrator.mapper.ResearchTaskMapper;
import com.quant.aiorchestrator.mapper.RiskWarningDetailMapper;
import com.quant.aiorchestrator.mapper.RiskWarningMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
public class MarketEventProjectionManager {

    private final MarketEventReadManager marketEventReadManager;
    private final MarketEventListItemAssembler listItemAssembler;

    @Autowired
    public MarketEventProjectionManager(ObjectMapper objectMapper,
                                        MarketEventNormalizationManager normalizationManager,
                                        MarketEventReadManager marketEventReadManager,
                                        MarketEventListItemAssembler listItemAssembler) {
        this.marketEventReadManager = marketEventReadManager;
        this.listItemAssembler = listItemAssembler;
    }

    public MarketEventProjectionManager(MarketEventMapper marketEventMapper,
                                        MarketEventAnalysisMapper marketEventAnalysisMapper,
                                        MarketEventRelationMapper marketEventRelationMapper,
                                        ResearchTaskMapper researchTaskMapper,
                                        ResearchReportMapper researchReportMapper,
                                        RiskWarningMapper riskWarningMapper,
                                        RiskWarningDetailMapper riskWarningDetailMapper,
                                        ObjectMapper objectMapper,
                                        MarketEventNormalizationManager normalizationManager) {
        this(
                objectMapper,
                normalizationManager,
                new MarketEventReadManager(
                        marketEventMapper,
                        marketEventAnalysisMapper,
                        marketEventRelationMapper,
                        researchTaskMapper,
                        researchReportMapper,
                        riskWarningMapper,
                        riskWarningDetailMapper
                ),
                new MarketEventListItemAssembler(objectMapper, normalizationManager)
        );
    }

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

        Map<String, List<ResearchTaskDO>> followUpTaskMap = marketEventReadManager.loadFollowUpTaskMap(eventIds);
        Map<String, List<MarketEventRelationDO>> relationMap = marketEventReadManager.loadRelationMap(eventIds);
        Map<String, MarketEventAnalysisDO> analysisMap = marketEventReadManager.loadLatestAnalysisMap(eventIds);
        List<ResearchTaskDO> allFollowUpTasks = followUpTaskMap.values().stream().flatMap(List::stream).toList();
        Map<String, ResearchReportDO> latestReportMap = marketEventReadManager.loadLatestReportMap(allFollowUpTasks);
        Map<String, RiskWarningDO> latestRiskWarningMap = marketEventReadManager.loadLatestRiskWarningMap(allFollowUpTasks);
        Map<String, List<RiskWarningDetailDO>> riskWarningDetailMap = marketEventReadManager.loadRiskWarningDetailMap(latestRiskWarningMap.values().stream()
                .map(RiskWarningDO::getWarningId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet()));

        return events.stream()
                .map(item -> listItemAssembler.toMarketEventItem(
                        item,
                        relationMap.getOrDefault(item.getEventId(), List.of()),
                        followUpTaskMap.getOrDefault(item.getEventId(), List.of()),
                        latestReportMap,
                        latestRiskWarningMap,
                        riskWarningDetailMap,
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
        Map<String, List<ResearchTaskDO>> followUpTaskMap = marketEventReadManager.loadFollowUpTaskMap(List.of(eventId));
        Map<String, List<MarketEventRelationDO>> relationMap = marketEventReadManager.loadRelationMap(List.of(eventId));
        Map<String, MarketEventAnalysisDO> analysisMap = marketEventReadManager.loadLatestAnalysisMap(List.of(eventId));
        List<ResearchTaskDO> followUpTasks = followUpTaskMap.getOrDefault(eventId, List.of());
        Map<String, ResearchReportDO> latestReportMap = marketEventReadManager.loadLatestReportMap(followUpTasks);
        Map<String, RiskWarningDO> latestRiskWarningMap = marketEventReadManager.loadLatestRiskWarningMap(followUpTasks);
        Map<String, List<RiskWarningDetailDO>> riskWarningDetailMap = marketEventReadManager.loadRiskWarningDetailMap(latestRiskWarningMap.values().stream()
                .map(RiskWarningDO::getWarningId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet()));
        return listItemAssembler.toMarketEventItem(
                event,
                relationMap.getOrDefault(eventId, List.of()),
                followUpTasks,
                latestReportMap,
                latestRiskWarningMap,
                riskWarningDetailMap,
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
