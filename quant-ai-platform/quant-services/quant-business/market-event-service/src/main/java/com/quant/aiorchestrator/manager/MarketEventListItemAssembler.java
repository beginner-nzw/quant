package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.entity.MarketEventAnalysisDO;
import com.quant.aiorchestrator.domain.entity.MarketEventDO;
import com.quant.aiorchestrator.domain.entity.MarketEventRelationDO;
import com.quant.aiorchestrator.domain.projection.MarketEventFollowUpProjection;
import com.quant.aiorchestrator.domain.vo.MarketEventListItemVO;
import com.quant.aiorchestrator.domain.vo.MarketEventRelationVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MarketEventListItemAssembler {

    private final MarketEventNormalizationManager normalizationManager;

    public MarketEventListItemAssembler(MarketEventNormalizationManager normalizationManager) {
        this.normalizationManager = normalizationManager;
    }

    public MarketEventListItemVO toMarketEventItem(MarketEventDO event,
                                                   List<MarketEventRelationDO> relations,
                                                   MarketEventFollowUpProjection followUpProjection,
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
        populateFollowUpFields(vo, followUpProjection);
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

    private void populateFollowUpFields(MarketEventListItemVO vo, MarketEventFollowUpProjection projection) {
        if (vo == null) {
            return;
        }
        if (projection == null) {
            vo.setFollowUpTaskCount(0);
            vo.setFollowUpStatus("NOT_TRACKED");
            vo.setRelatedReportCount(0);
            return;
        }
        vo.setFollowUpTaskCount(projection.getFollowUpTaskCount());
        vo.setFollowUpStatus(projection.getFollowUpStatus());
        vo.setLatestFollowUpTaskId(projection.getLatestFollowUpTaskId());
        vo.setLatestFollowUpTaskTitle(projection.getLatestFollowUpTaskTitle());
        vo.setLatestFollowUpTaskStatus(projection.getLatestFollowUpTaskStatus());
        vo.setLatestFollowUpCreatedAt(projection.getLatestFollowUpCreatedAt());
        vo.setRelatedReportCount(projection.getRelatedReportCount());
        vo.setLatestReportTaskId(projection.getLatestReportTaskId());
        vo.setLatestReportId(projection.getLatestReportId());
        vo.setLatestReportType(projection.getLatestReportType());
        vo.setLatestReportReviewStatus(projection.getLatestReportReviewStatus());
        vo.setLatestReportSummary(projection.getLatestReportSummary());
        vo.setLatestReportConfidenceScore(projection.getLatestReportConfidenceScore());
        vo.setLatestNeedHumanReview(projection.getLatestNeedHumanReview());
        vo.setLatestReportCreatedAt(projection.getLatestReportCreatedAt());
        vo.setDerivedRiskLevel(projection.getDerivedRiskLevel());
        vo.setDerivedWarningCount(projection.getDerivedWarningCount());
        vo.setDerivedRiskPointCount(projection.getDerivedRiskPointCount());
        vo.setDerivedRiskCount(projection.getDerivedRiskCount());
        vo.setDerivedSignalDirection(projection.getDerivedSignalDirection());
        vo.setDerivedSignalStrength(projection.getDerivedSignalStrength());
        vo.setDerivedSignalScore(projection.getDerivedSignalScore());
        vo.setDerivedIntelligenceType(projection.getDerivedIntelligenceType());
    }
}
