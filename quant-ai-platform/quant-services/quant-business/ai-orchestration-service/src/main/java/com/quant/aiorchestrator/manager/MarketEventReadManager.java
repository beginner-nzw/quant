package com.quant.aiorchestrator.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quant.aiorchestrator.domain.entity.MarketEventAnalysisDO;
import com.quant.aiorchestrator.domain.entity.MarketEventDO;
import com.quant.aiorchestrator.domain.entity.MarketEventRelationDO;
import com.quant.aiorchestrator.domain.entity.ResearchReportDO;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.aiorchestrator.domain.entity.RiskWarningDO;
import com.quant.aiorchestrator.domain.entity.RiskWarningDetailDO;
import com.quant.aiorchestrator.mapper.MarketEventAnalysisMapper;
import com.quant.aiorchestrator.mapper.MarketEventMapper;
import com.quant.aiorchestrator.mapper.MarketEventRelationMapper;
import com.quant.aiorchestrator.mapper.ResearchReportMapper;
import com.quant.aiorchestrator.mapper.ResearchTaskMapper;
import com.quant.aiorchestrator.mapper.RiskWarningDetailMapper;
import com.quant.aiorchestrator.mapper.RiskWarningMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MarketEventReadManager {

    private final MarketEventMapper marketEventMapper;
    private final MarketEventAnalysisMapper marketEventAnalysisMapper;
    private final MarketEventRelationMapper marketEventRelationMapper;
    private final ResearchTaskMapper researchTaskMapper;
    private final ResearchReportMapper researchReportMapper;
    private final RiskWarningMapper riskWarningMapper;
    private final RiskWarningDetailMapper riskWarningDetailMapper;

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

    public Map<String, List<ResearchTaskDO>> loadFollowUpTaskMap(List<String> eventIds) {
        List<String> validEventIds = distinctText(eventIds);
        if (validEventIds.isEmpty()) {
            return Map.of();
        }
        return researchTaskMapper.selectList(
                new LambdaQueryWrapper<ResearchTaskDO>()
                        .eq(ResearchTaskDO::getDeleted, 0)
                        .eq(ResearchTaskDO::getSourceDomain, "MARKET_EVENT")
                        .in(ResearchTaskDO::getSourceEventId, validEventIds)
                        .orderByDesc(ResearchTaskDO::getCreatedAt, ResearchTaskDO::getId)
        ).stream()
                .filter(item -> StringUtils.hasText(item.getSourceEventId()))
                .collect(Collectors.groupingBy(ResearchTaskDO::getSourceEventId));
    }

    public Map<String, ResearchReportDO> loadLatestReportMap(List<ResearchTaskDO> followUpTasks) {
        List<String> taskIds = followUpTasks == null ? List.of() : followUpTasks.stream()
                .map(ResearchTaskDO::getTaskId)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (taskIds.isEmpty()) {
            return Map.of();
        }
        return researchReportMapper.selectList(
                new LambdaQueryWrapper<ResearchReportDO>()
                        .eq(ResearchReportDO::getDeleted, 0)
                        .in(ResearchReportDO::getTaskId, taskIds)
                        .orderByDesc(ResearchReportDO::getCreatedAt, ResearchReportDO::getId)
        ).stream().collect(Collectors.toMap(
                ResearchReportDO::getTaskId,
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new
        ));
    }

    public Map<String, RiskWarningDO> loadLatestRiskWarningMap(List<ResearchTaskDO> followUpTasks) {
        List<String> taskIds = followUpTasks == null ? List.of() : followUpTasks.stream()
                .map(ResearchTaskDO::getTaskId)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (taskIds.isEmpty()) {
            return Map.of();
        }
        return riskWarningMapper.selectList(
                new LambdaQueryWrapper<RiskWarningDO>()
                        .eq(RiskWarningDO::getDeleted, 0)
                        .in(RiskWarningDO::getTaskId, taskIds)
                        .orderByDesc(RiskWarningDO::getCreatedAt, RiskWarningDO::getId)
        ).stream().collect(Collectors.toMap(
                RiskWarningDO::getTaskId,
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new
        ));
    }

    public Map<String, List<RiskWarningDetailDO>> loadRiskWarningDetailMap(Set<String> warningIds) {
        if (warningIds == null || warningIds.isEmpty()) {
            return Map.of();
        }
        return riskWarningDetailMapper.selectList(
                new LambdaQueryWrapper<RiskWarningDetailDO>()
                        .eq(RiskWarningDetailDO::getDeleted, 0)
                        .in(RiskWarningDetailDO::getWarningId, warningIds)
                        .orderByAsc(RiskWarningDetailDO::getId)
        ).stream().collect(Collectors.groupingBy(RiskWarningDetailDO::getWarningId, LinkedHashMap::new, Collectors.toList()));
    }

    private List<String> distinctText(List<String> values) {
        return values == null ? List.of() : values.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }
}
