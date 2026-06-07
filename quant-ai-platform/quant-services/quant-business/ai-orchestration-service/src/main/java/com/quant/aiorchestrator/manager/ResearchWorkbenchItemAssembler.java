package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.entity.ResearchReportDO;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.aiorchestrator.domain.entity.RiskWarningDO;
import com.quant.aiorchestrator.domain.entity.RiskWarningDetailDO;
import com.quant.aiorchestrator.domain.entity.StrategySignalDO;
import com.quant.aiorchestrator.domain.vo.ResearchWorkbenchInsightVO;
import com.quant.aiorchestrator.domain.vo.ResearchWorkbenchRecentTaskVO;
import com.quant.common.model.enums.ReportReviewStatusEnum;
import com.quant.common.model.enums.RiskLevelEnum;
import com.quant.common.model.enums.SignalDirectionEnum;
import com.quant.common.model.enums.SignalStrengthEnum;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Component
public class ResearchWorkbenchItemAssembler {

    private final StrategySignalRuleManager ruleManager;

    public ResearchWorkbenchItemAssembler(StrategySignalRuleManager ruleManager) {
        this.ruleManager = ruleManager;
    }

    public ResearchWorkbenchInsightVO toResearchWorkbenchInsight(ResearchReportDO report,
                                                                 ResearchTaskDO task,
                                                                 RiskWarningDO warning,
                                                                 List<RiskWarningDetailDO> details,
                                                                 StrategySignalDO strategySignal) {
        if (report == null && warning == null) {
            return null;
        }
        String summary = report == null
                ? ruleManager.normalizeText(warning == null ? null : warning.getWarningSummary())
                : ruleManager.resolveDisplaySummary(report.getRevisedSummary(), report.getSummary());
        if ((summary == null || summary.isBlank()) && strategySignal != null) {
            summary = ruleManager.normalizeText(strategySignal.getReasonSummary());
        }
        List<String> highlights = report == null ? List.of() : ruleManager.readPreferredTextList(report.getRevisedHighlights(), report.getHighlights());
        List<String> fallbackRiskPoints = report == null ? List.of() : ruleManager.readPreferredTextList(report.getRevisedRiskPoints(), report.getRiskPoints());
        List<String> fallbackRiskWarnings = report == null ? List.of() : ruleManager.readTextList(report.getRiskWarnings());
        List<String> domainRiskPoints = warning == null ? List.of() : buildDomainRiskInsightPoints(warning, details);
        int totalRiskCount = warning != null
                ? 1 + (details == null ? 0 : details.size())
                : fallbackRiskPoints.size() + fallbackRiskWarnings.size();
        Double reportConfidenceScore = report == null || report.getConfidenceScore() == null ? null : report.getConfidenceScore().doubleValue();
        Double signalConfidenceScore = strategySignal == null || strategySignal.getConfidenceScore() == null
                ? null
                : strategySignal.getConfidenceScore().doubleValue();
        Double confidenceScore = signalConfidenceScore == null ? reportConfidenceScore : signalConfidenceScore;
        boolean needHumanReview = warning != null
                ? ruleManager.resolveRiskProjection(report, warning).needHumanReview()
                : report.getNeedHumanReview() != null && report.getNeedHumanReview() == 1;
        ReportReviewStatusEnum reviewStatus = ruleManager.resolveReviewStatus(
                warning != null && warning.getReviewStatus() != null ? warning.getReviewStatus() : (report == null ? null : report.getReviewStatus())
        );
        SignalDirectionEnum signalDirection = strategySignal == null
                ? ruleManager.resolveSignalDirection(summary, totalRiskCount, needHumanReview, confidenceScore)
                : resolveDomainSignalDirection(strategySignal);
        SignalStrengthEnum signalStrength = strategySignal == null
                ? ruleManager.resolveSignalStrength(ruleManager.calculateSignalScore(confidenceScore, totalRiskCount, needHumanReview, reviewStatus, signalDirection))
                : resolveDomainSignalStrength(strategySignal);
        RiskLevelEnum riskLevel = warning != null
                ? ruleManager.resolveRiskProjection(report, warning).riskLevel()
                : (totalRiskCount > 0 || needHumanReview ? ruleManager.resolveRiskProjection(report, null).riskLevel() : null);

        ResearchWorkbenchInsightVO vo = new ResearchWorkbenchInsightVO();
        vo.setTaskId(report == null ? (warning == null ? null : warning.getTaskId()) : report.getTaskId());
        vo.setTaskTitle(task == null ? null : task.getTaskTitle());
        vo.setReportId(report == null ? null : report.getReportId());
        vo.setReportType(report == null ? (task == null ? null : task.getTaskType()) : ruleManager.resolveReportType(report, task));
        vo.setFinalStatus(report == null ? (task == null ? null : task.getStatus()) : report.getFinalStatus());
        vo.setConfidenceScore(confidenceScore);
        vo.setNeedHumanReview(needHumanReview);
        vo.setReviewStatus(reviewStatus.name());
        vo.setReviewedBy(warning != null && warning.getReviewerId() != null ? warning.getReviewerId() : (report == null ? null : report.getReviewedBy()));
        vo.setReviewedAt(warning != null && warning.getReviewTime() != null ? warning.getReviewTime() : (report == null ? null : report.getReviewedAt()));
        vo.setRevised(report != null && ruleManager.isReportRevised(report));
        vo.setSummaryRevised(report != null && ruleManager.isSummaryRevised(report));
        vo.setHighlightsRevised(report != null && ruleManager.isHighlightsRevised(report));
        vo.setRiskPointsRevised(report != null && ruleManager.isRiskPointsRevised(report));
        vo.setSignalDirection(signalDirection.name());
        vo.setSignalStrength(signalStrength.name());
        vo.setRiskLevel(riskLevel == null ? null : riskLevel.name());
        vo.setSummary(summary);
        vo.setHighlights(highlights);
        vo.setRiskPoints(domainRiskPoints.isEmpty() ? (fallbackRiskPoints.isEmpty() ? fallbackRiskWarnings : fallbackRiskPoints) : domainRiskPoints);
        vo.setCreatedAt(firstNonNull(
                report == null ? (warning == null ? null : warning.getCreatedAt()) : report.getCreatedAt(),
                task == null ? null : task.getCreatedAt()
        ));
        return vo;
    }

    public ResearchWorkbenchRecentTaskVO toResearchWorkbenchRecentTask(ResearchTaskDO task, ResearchReportDO report) {
        ResearchWorkbenchRecentTaskVO vo = new ResearchWorkbenchRecentTaskVO();
        vo.setTaskId(task.getTaskId());
        vo.setTaskTitle(task.getTaskTitle());
        vo.setPriority(task.getPriority());
        vo.setStatus(task.getStatus());
        vo.setCurrentStage(task.getCurrentStage());
        vo.setRetryCount(task.getRetryCount());
        vo.setReportId(report == null ? null : report.getReportId());
        vo.setReportReviewStatus(report == null ? null : ruleManager.resolveReviewStatus(report.getReviewStatus()).name());
        vo.setRevised(report != null && ruleManager.isReportRevised(report));
        vo.setSummaryRevised(report != null && ruleManager.isSummaryRevised(report));
        vo.setHighlightsRevised(report != null && ruleManager.isHighlightsRevised(report));
        vo.setRiskPointsRevised(report != null && ruleManager.isRiskPointsRevised(report));
        vo.setConfidenceScore(report == null || report.getConfidenceScore() == null ? null : report.getConfidenceScore().doubleValue());
        vo.setFinishTime(task.getFinishTime());
        vo.setCreatedAt(task.getCreatedAt());
        return vo;
    }

    private SignalDirectionEnum resolveDomainSignalDirection(StrategySignalDO signal) {
        SignalDirectionEnum resolved = signal == null ? null : SignalDirectionEnum.from(signal.getSignalDirection());
        if (resolved != null) {
            return resolved;
        }
        Double confidenceScore = signal == null || signal.getConfidenceScore() == null
                ? null
                : signal.getConfidenceScore().doubleValue();
        return ruleManager.resolveSignalDirection(signal == null ? null : signal.getReasonSummary(), 0, false, confidenceScore);
    }

    private SignalStrengthEnum resolveDomainSignalStrength(StrategySignalDO signal) {
        SignalStrengthEnum resolved = signal == null ? null : SignalStrengthEnum.from(signal.getSignalLevel());
        if (resolved != null) {
            return resolved;
        }
        if (signal != null && signal.getSignalScore() != null) {
            return ruleManager.resolveSignalStrength(signal.getSignalScore());
        }
        Double confidenceScore = signal == null || signal.getConfidenceScore() == null
                ? null
                : signal.getConfidenceScore().doubleValue();
        int fallbackScore = confidenceScore == null ? 60 : (int) Math.round(Math.max(0D, Math.min(1D, confidenceScore)) * 100D);
        return ruleManager.resolveSignalStrength(fallbackScore);
    }

    private List<String> buildDomainRiskInsightPoints(RiskWarningDO warning, List<RiskWarningDetailDO> details) {
        LinkedHashSet<String> points = new LinkedHashSet<>();
        if (details != null) {
            for (RiskWarningDetailDO detail : details) {
                String description = ruleManager.normalizeText(detail.getDetailDesc());
                if (description != null) {
                    points.add(description);
                    continue;
                }
                String indicatorName = ruleManager.normalizeText(detail.getIndicatorName());
                String indicatorValue = ruleManager.normalizeText(detail.getIndicatorValue());
                if (indicatorName != null && indicatorValue != null) {
                    points.add(indicatorName + ": " + indicatorValue);
                } else if (indicatorValue != null) {
                    points.add(indicatorValue);
                }
            }
        }
        if (warning != null && warning.getWarningReason() != null && !warning.getWarningReason().isBlank()) {
            for (String item : warning.getWarningReason().split("\\R")) {
                if (item != null && !item.isBlank()) {
                    points.add(item.trim());
                }
            }
        }
        if (points.isEmpty() && warning != null && warning.getWarningSummary() != null && !warning.getWarningSummary().isBlank()) {
            points.add(warning.getWarningSummary().trim());
        }
        return new ArrayList<>(points);
    }

    private LocalDateTime firstNonNull(LocalDateTime left, LocalDateTime right) {
        return left != null ? left : right;
    }
}
