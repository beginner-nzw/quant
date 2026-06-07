package com.quant.aiorchestrator.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.entity.MarketEventAnalysisDO;
import com.quant.aiorchestrator.domain.entity.MarketEventDO;
import com.quant.aiorchestrator.domain.entity.MarketEventRelationDO;
import com.quant.aiorchestrator.domain.entity.ResearchReportDO;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.aiorchestrator.domain.entity.RiskWarningDO;
import com.quant.aiorchestrator.domain.entity.RiskWarningDetailDO;
import com.quant.aiorchestrator.domain.vo.MarketEventListItemVO;
import com.quant.aiorchestrator.domain.vo.MarketEventRelationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class MarketEventListItemAssembler {

    private static final String FOLLOW_UP_STATUS_NOT_TRACKED = "NOT_TRACKED";
    private static final String FOLLOW_UP_STATUS_TRACKING = "TRACKING";
    private static final String FOLLOW_UP_STATUS_COMPLETED = "COMPLETED";
    private static final String FOLLOW_UP_STATUS_FAILED = "FAILED";

    private final MarketEventNormalizationManager normalizationManager;
    private final MarketEventDerivedResultAssembler derivedResultAssembler;

    public MarketEventListItemAssembler(MarketEventNormalizationManager normalizationManager,
                                        MarketEventDerivedResultAssembler derivedResultAssembler) {
        this.normalizationManager = normalizationManager;
        this.derivedResultAssembler = derivedResultAssembler;
    }

    public MarketEventListItemAssembler(ObjectMapper objectMapper,
                                        MarketEventNormalizationManager normalizationManager) {
        this(
                normalizationManager,
                new MarketEventDerivedResultAssembler(objectMapper, normalizationManager)
        );
    }

    public MarketEventListItemVO toMarketEventItem(MarketEventDO event,
                                                   List<MarketEventRelationDO> relations,
                                                   List<ResearchTaskDO> followUpTasks,
                                                   Map<String, ResearchReportDO> latestReportMap,
                                                   Map<String, RiskWarningDO> latestRiskWarningMap,
                                                   Map<String, List<RiskWarningDetailDO>> riskWarningDetailMap,
                                                   MarketEventAnalysisDO analysis) {
        MarketEventListItemVO vo = new MarketEventListItemVO();
        BeanUtils.copyProperties(event, vo);
        vo.setTargetType(normalizationManager.normalizeTargetType(vo.getTargetType()));
        vo.setTargetCode(normalizationManager.normalizeTargetCode(vo.getTargetCode()));
        vo.setEventType(normalizationManager.normalizeEventType(vo.getEventType()));
        vo.setSourceChannel(normalizationManager.normalizeSourceChannel(vo.getSourceChannel(), vo.getEventType()));
        vo.setImpactLevel(normalizationManager.normalizeImpactLevel(vo.getImpactLevel()));
        vo.setEventStatus(normalizationManager.normalizeEventStatus(vo.getEventStatus()));
        List<MarketEventRelationVO> relationVOs = normalizationManager.toRelationVOs(relations);
        vo.setRelations(relationVOs);
        vo.setRelationCount(relationVOs.size());
        populateEventAnalysisFields(vo, analysis);

        List<ResearchTaskDO> safeFollowUpTasks = followUpTasks == null ? List.of() : followUpTasks;
        ResearchTaskDO latestFollowUp = safeFollowUpTasks.stream()
                .filter(Objects::nonNull)
                .max(Comparator
                        .comparing(ResearchTaskDO::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo))
                        .thenComparing(ResearchTaskDO::getId, Comparator.nullsLast(Long::compareTo)))
                .orElse(null);

        vo.setFollowUpTaskCount(safeFollowUpTasks.size());
        vo.setFollowUpStatus(resolveFollowUpStatus(latestFollowUp));
        if (latestFollowUp != null) {
            vo.setLatestFollowUpTaskId(latestFollowUp.getTaskId());
            vo.setLatestFollowUpTaskTitle(latestFollowUp.getTaskTitle());
            vo.setLatestFollowUpTaskStatus(latestFollowUp.getStatus());
            vo.setLatestFollowUpCreatedAt(latestFollowUp.getCreatedAt());
        }

        List<ResearchReportDO> relatedReports = safeFollowUpTasks.stream()
                .map(ResearchTaskDO::getTaskId)
                .map(latestReportMap::get)
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing(ResearchReportDO::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ResearchReportDO::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        vo.setRelatedReportCount(relatedReports.size());
        if (!relatedReports.isEmpty()) {
            ResearchReportDO latestReport = relatedReports.get(0);
            RiskWarningDO latestWarning = latestRiskWarningMap.get(latestReport.getTaskId());
            derivedResultAssembler.populateDerivedResultFields(
                    vo,
                    latestReport,
                    latestWarning,
                    latestWarning == null ? List.of() : riskWarningDetailMap.getOrDefault(latestWarning.getWarningId(), List.of())
            );
        }
        return vo;
    }

    private void populateEventAnalysisFields(MarketEventListItemVO vo, MarketEventAnalysisDO analysis) {
        if (vo == null || analysis == null) {
            return;
        }
        vo.setAnalysisId(analysis.getAnalysisId());
        vo.setAnalysisSummary(normalizationManager.trimToNull(analysis.getAnalysisSummary()));
        vo.setAnalysisImpactDirection(normalizationManager.trimToNull(analysis.getImpactDirection()));
        vo.setAnalysisImpactLevel(normalizationManager.trimToNull(analysis.getImpactLevel()));
        vo.setAnalysisRiskFlag(analysis.getRiskFlag() != null && analysis.getRiskFlag() == 1);
        vo.setAnalysisConfidenceScore(analysis.getConfidenceScore());
    }

    private String resolveFollowUpStatus(ResearchTaskDO latestFollowUp) {
        if (latestFollowUp == null || !StringUtils.hasText(latestFollowUp.getStatus())) {
            return FOLLOW_UP_STATUS_NOT_TRACKED;
        }
        String status = latestFollowUp.getStatus().trim().toUpperCase();
        if ("SUCCESS".equals(status)) {
            return FOLLOW_UP_STATUS_COMPLETED;
        }
        if ("FAILED".equals(status) || "CANCELLED".equals(status)) {
            return FOLLOW_UP_STATUS_FAILED;
        }
        return FOLLOW_UP_STATUS_TRACKING;
    }
}
