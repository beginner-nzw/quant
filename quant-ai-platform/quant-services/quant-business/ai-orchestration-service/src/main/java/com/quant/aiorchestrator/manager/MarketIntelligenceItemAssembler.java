package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.dto.MarketIntelligencePageQueryDTO;
import com.quant.aiorchestrator.domain.entity.ResearchReportDO;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.aiorchestrator.domain.entity.RiskWarningDO;
import com.quant.aiorchestrator.domain.entity.RiskWarningDetailDO;
import com.quant.aiorchestrator.domain.entity.StrategySignalDO;
import com.quant.aiorchestrator.domain.vo.MarketIntelligenceListItemVO;
import com.quant.common.model.enums.MarketIntelligenceTypeEnum;
import com.quant.common.model.enums.ReportReviewStatusEnum;
import com.quant.common.model.enums.RiskLevelEnum;
import com.quant.common.model.enums.SignalDirectionEnum;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Component
public class MarketIntelligenceItemAssembler {

    private final StrategySignalRuleManager ruleManager;

    public MarketIntelligenceItemAssembler(StrategySignalRuleManager ruleManager) {
        this.ruleManager = ruleManager;
    }

    public MarketIntelligenceListItemVO toMarketIntelligenceItem(ResearchReportDO report,
                                                                 ResearchTaskDO task,
                                                                 FollowUpTaskSummaryManager.FollowUpSummary followUpSummary,
                                                                 RiskWarningDO warning,
                                                                 List<RiskWarningDetailDO> warningDetails,
                                                                 StrategySignalDO strategySignal) {
        if (report == null || task == null) {
            return null;
        }

        String summary = ruleManager.resolveDisplaySummary(report.getRevisedSummary(), report.getSummary());
        String reportType = ruleManager.resolveReportType(report, task);
        StrategySignalRuleManager.RiskProjection riskProjection = ruleManager.resolveRiskProjection(report, warning, warningDetails);
        int totalRiskCount = riskProjection.totalRiskCount();
        boolean needHumanReview = riskProjection.needHumanReview();
        Double reportConfidenceScore = report.getConfidenceScore() == null ? null : report.getConfidenceScore().doubleValue();
        Double signalConfidenceScore = strategySignal == null || strategySignal.getConfidenceScore() == null
                ? null
                : strategySignal.getConfidenceScore().doubleValue();
        Double confidenceScore = signalConfidenceScore == null ? reportConfidenceScore : signalConfidenceScore;
        SignalDirectionEnum signalDirection = strategySignal == null
                ? ruleManager.resolveSignalDirection(summary, totalRiskCount, needHumanReview, confidenceScore)
                : ruleManager.resolveDomainSignalDirection(strategySignal);
        RiskLevelEnum riskLevel = riskProjection.riskLevel();
        MarketIntelligenceTypeEnum intelligenceType = resolveMarketIntelligenceType(
                totalRiskCount,
                needHumanReview,
                confidenceScore,
                signalDirection,
                strategySignal != null
        );

        if ((summary == null || summary.isBlank())
                && (report.getResultRef() == null || report.getResultRef().isBlank())
                && (reportType == null || reportType.isBlank())) {
            return null;
        }

        ReportReviewStatusEnum reviewStatus = ruleManager.resolveReviewStatus(report.getReviewStatus());

        MarketIntelligenceListItemVO vo = new MarketIntelligenceListItemVO();
        vo.setTaskId(task.getTaskId());
        vo.setTaskTitle(task.getTaskTitle());
        vo.setTaskType(task.getTaskType());
        vo.setTargetCode(task.getTargetCode());
        vo.setTargetName(task.getTargetName());
        vo.setPriority(task.getPriority());
        vo.setSourceChannel(task.getSourceChannel());
        vo.setIntelligenceType(intelligenceType.name());
        vo.setReportId(report.getReportId());
        vo.setReportType(reportType);
        vo.setFinalStatus(report.getFinalStatus());
        vo.setConfidenceScore(confidenceScore);
        vo.setNeedHumanReview(needHumanReview);
        vo.setReviewStatus(reviewStatus.name());
        vo.setReviewedBy(report.getReviewedBy());
        vo.setReviewedAt(report.getReviewedAt());
        vo.setReviewComment(report.getReviewComment());
        vo.setRevised(ruleManager.isReportRevised(report));
        vo.setSummaryRevised(ruleManager.isSummaryRevised(report));
        vo.setHighlightsRevised(ruleManager.isHighlightsRevised(report));
        vo.setRiskPointsRevised(ruleManager.isRiskPointsRevised(report));
        applyFollowUpSummary(vo, followUpSummary);
        vo.setSignalDirection(signalDirection.name());
        vo.setRiskLevel(riskLevel == null ? null : riskLevel.name());
        vo.setIntelligenceSourceTags(buildMarketIntelligenceSourceTags(
                task,
                intelligenceType,
                summary,
                totalRiskCount,
                needHumanReview,
                reviewStatus,
                confidenceScore,
                signalDirection,
                strategySignal != null
        ));
        vo.setSummary(summary);
        vo.setCreatedAt(firstNonNull(report.getCreatedAt(), task.getCreatedAt()));
        return vo;
    }

    public boolean matchesMarketIntelligenceQuery(MarketIntelligenceListItemVO item,
                                                  MarketIntelligencePageQueryDTO queryDTO) {
        if (item == null) {
            return false;
        }
        if (queryDTO == null) {
            return true;
        }
        if (queryDTO.getTargetCode() != null && !queryDTO.getTargetCode().isBlank()
                && !containsIgnoreCase(item.getTargetCode(), queryDTO.getTargetCode())) {
            return false;
        }
        if (queryDTO.getTargetName() != null && !queryDTO.getTargetName().isBlank()
                && !containsIgnoreCase(item.getTargetName(), queryDTO.getTargetName())) {
            return false;
        }
        MarketIntelligenceTypeEnum intelligenceType = MarketIntelligenceTypeEnum.from(queryDTO.getIntelligenceType());
        if (intelligenceType != null && !intelligenceType.name().equals(item.getIntelligenceType())) {
            return false;
        }
        ReportReviewStatusEnum reviewStatus = ReportReviewStatusEnum.from(queryDTO.getReviewStatus());
        if (reviewStatus != null && !reviewStatus.name().equals(item.getReviewStatus())) {
            return false;
        }
        if (Boolean.TRUE.equals(queryDTO.getOnlyHighPriority()) && !"HIGH".equalsIgnoreCase(item.getPriority())) {
            return false;
        }
        return queryDTO.getNeedHumanReview() == null || queryDTO.getNeedHumanReview().equals(item.getNeedHumanReview());
    }

    private void applyFollowUpSummary(MarketIntelligenceListItemVO vo,
                                      FollowUpTaskSummaryManager.FollowUpSummary followUpSummary) {
        if (followUpSummary == null) {
            return;
        }
        vo.setFollowUpStatus(followUpSummary.followUpStatus());
        vo.setFollowUpTaskCount(followUpSummary.followUpTaskCount());
        vo.setLatestFollowUpTaskId(followUpSummary.latestFollowUpTaskId());
        vo.setLatestFollowUpTaskTitle(followUpSummary.latestFollowUpTaskTitle());
        vo.setLatestFollowUpTaskStatus(followUpSummary.latestFollowUpTaskStatus());
        vo.setLatestFollowUpCreatedAt(followUpSummary.latestFollowUpCreatedAt());
    }

    private MarketIntelligenceTypeEnum resolveMarketIntelligenceType(int totalRiskCount,
                                                                     boolean needHumanReview,
                                                                     Double confidenceScore,
                                                                     SignalDirectionEnum signalDirection,
                                                                     boolean hasDomainStrategySignal) {
        if (needHumanReview || totalRiskCount > 0) {
            return MarketIntelligenceTypeEnum.RISK_ALERT;
        }
        if (hasDomainStrategySignal) {
            return MarketIntelligenceTypeEnum.STRATEGY_SIGNAL;
        }
        if (signalDirection == SignalDirectionEnum.POSITIVE
                || signalDirection == SignalDirectionEnum.NEGATIVE
                || ruleManager.isHighConfidence(confidenceScore)) {
            return MarketIntelligenceTypeEnum.STRATEGY_SIGNAL;
        }
        return MarketIntelligenceTypeEnum.REPORT_INSIGHT;
    }

    private List<String> buildMarketIntelligenceSourceTags(ResearchTaskDO task,
                                                           MarketIntelligenceTypeEnum intelligenceType,
                                                           String summary,
                                                           int totalRiskCount,
                                                           boolean needHumanReview,
                                                           ReportReviewStatusEnum reviewStatus,
                                                           Double confidenceScore,
                                                           SignalDirectionEnum signalDirection,
                                                           boolean hasDomainStrategySignal) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        if (task != null && task.getSourceChannel() != null && !task.getSourceChannel().isBlank()) {
            tags.add("SOURCE_CHANNEL");
        }
        if (summary != null && !summary.isBlank()) {
            tags.add("REPORT_SUMMARY");
        }
        if (intelligenceType == MarketIntelligenceTypeEnum.RISK_ALERT || totalRiskCount > 0) {
            tags.add("RISK_ALERT");
        }
        if (hasDomainStrategySignal
                || intelligenceType == MarketIntelligenceTypeEnum.STRATEGY_SIGNAL
                || signalDirection == SignalDirectionEnum.POSITIVE
                || signalDirection == SignalDirectionEnum.NEGATIVE
                || ruleManager.isHighConfidence(confidenceScore)) {
            tags.add("STRATEGY_SIGNAL");
        }
        if (intelligenceType == MarketIntelligenceTypeEnum.REPORT_INSIGHT) {
            tags.add("REPORT_INSIGHT");
        }
        if (needHumanReview) {
            tags.add("HUMAN_REVIEW");
        }
        if (reviewStatus == ReportReviewStatusEnum.REJECTED) {
            tags.add("REVIEW_REJECTED");
        }
        return new ArrayList<>(tags);
    }

    private boolean containsIgnoreCase(String source, String target) {
        return source != null && target != null && source.toLowerCase().contains(target.toLowerCase());
    }

    private LocalDateTime firstNonNull(LocalDateTime left, LocalDateTime right) {
        return left != null ? left : right;
    }
}
