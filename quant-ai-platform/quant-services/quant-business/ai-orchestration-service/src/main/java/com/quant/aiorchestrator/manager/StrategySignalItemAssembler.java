package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.entity.ResearchReportDO;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.aiorchestrator.domain.entity.RiskWarningDO;
import com.quant.aiorchestrator.domain.entity.RiskWarningDetailDO;
import com.quant.aiorchestrator.domain.entity.StrategySignalDO;
import com.quant.aiorchestrator.domain.entity.StrategySignalFactorDO;
import com.quant.aiorchestrator.domain.vo.StrategySignalListItemVO;
import com.quant.common.model.enums.ReportReviewStatusEnum;
import com.quant.common.model.enums.SignalDirectionEnum;
import com.quant.common.model.enums.SignalStrengthEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class StrategySignalItemAssembler {

    private static final String BACKTEST_STATUS_NOT_READY = "NOT_READY";
    private static final String BACKTEST_SUMMARY_NOT_READY = "历史回测待接入";

    private final StrategySignalRuleManager ruleManager;

    public StrategySignalListItemVO fromDomainSignal(StrategySignalDO signal,
                                                     ResearchTaskDO task,
                                                     ResearchReportDO report,
                                                     FollowUpTaskSummaryManager.FollowUpSummary followUpSummary,
                                                     List<StrategySignalFactorDO> factors,
                                                     RiskWarningDO warning,
                                                     List<RiskWarningDetailDO> warningDetails) {
        if (signal == null) {
            return null;
        }

        Double confidenceScore = signal.getConfidenceScore() == null ? null : signal.getConfidenceScore().doubleValue();
        ReportReviewStatusEnum reviewStatus = ruleManager.resolveReviewStatus(report == null ? null : report.getReviewStatus());
        boolean needHumanReview = ruleManager.resolveRiskProjection(report, warning, warningDetails).needHumanReview();

        StrategySignalListItemVO vo = new StrategySignalListItemVO();
        vo.setSignalId(signal.getSignalId());
        vo.setTaskId(signal.getTaskId());
        vo.setTaskTitle(task == null ? signal.getReasonSummary() : task.getTaskTitle());
        vo.setTaskType(task == null ? null : task.getTaskType());
        vo.setTargetCode(signal.getEntityCode());
        vo.setTargetName(signal.getEntityName());
        vo.setPriority(task == null ? null : task.getPriority());
        vo.setReportId(report == null ? null : report.getReportId());
        vo.setReportType(report == null ? signal.getSignalType() : report.getReportType());
        vo.setFinalStatus(report == null ? null : report.getFinalStatus());
        vo.setSignalDirection(resolveDomainSignalDirection(signal).name());
        vo.setSignalStrength(resolveDomainSignalStrength(signal).name());
        vo.setSignalScore(signal.getSignalScore());
        vo.setConfidenceScore(confidenceScore);
        vo.setReportReviewStatus(reviewStatus.name());
        vo.setReportReviewedBy(report == null ? null : report.getReviewedBy());
        vo.setReportReviewedAt(report == null ? null : report.getReviewedAt());
        vo.setNeedHumanReview(needHumanReview);
        vo.setReviewComment(report == null ? null : report.getReviewComment());
        vo.setRevised(report != null && ruleManager.isReportRevised(report));
        vo.setSummaryRevised(report != null && ruleManager.isSummaryRevised(report));
        vo.setHighlightsRevised(report != null && ruleManager.isHighlightsRevised(report));
        vo.setRiskPointsRevised(report != null && ruleManager.isRiskPointsRevised(report));
        populateFollowUp(vo, followUpSummary);
        vo.setStrategySummary(signal.getReasonSummary());
        vo.setSignalSources(buildDomainSignalSources(signal, factors));
        vo.setSignalSourceTags(buildDomainSignalSourceTags(signal, factors, needHumanReview, reviewStatus, confidenceScore));
        vo.setBacktestStatus(BACKTEST_STATUS_NOT_READY);
        vo.setBacktestSummary(BACKTEST_SUMMARY_NOT_READY);
        vo.setCreatedAt(signal.getCreatedAt());
        return vo;
    }

    public StrategySignalListItemVO fromReport(ResearchReportDO report,
                                               ResearchTaskDO task,
                                               FollowUpTaskSummaryManager.FollowUpSummary followUpSummary,
                                               RiskWarningDO warning,
                                               List<RiskWarningDetailDO> warningDetails) {
        if (report == null || task == null) {
            return null;
        }

        String strategySummary = ruleManager.resolveStrategySummary(report);
        List<String> signalSources = ruleManager.resolveSignalSources(report, strategySummary);
        Double confidenceScore = report.getConfidenceScore() == null ? null : report.getConfidenceScore().doubleValue();

        if ((strategySummary == null || strategySummary.isBlank()) && signalSources.isEmpty() && confidenceScore == null) {
            return null;
        }

        StrategySignalRuleManager.RiskProjection riskProjection = ruleManager.resolveRiskProjection(report, warning, warningDetails);
        int totalRiskCount = riskProjection.totalRiskCount();
        boolean needHumanReview = riskProjection.needHumanReview();
        ReportReviewStatusEnum reviewStatus = ruleManager.resolveReviewStatus(report.getReviewStatus());
        SignalDirectionEnum signalDirection = ruleManager.resolveSignalDirection(strategySummary, totalRiskCount, needHumanReview, confidenceScore);
        int signalScore = ruleManager.calculateSignalScore(confidenceScore, totalRiskCount, needHumanReview, reviewStatus, signalDirection);
        SignalStrengthEnum signalStrength = ruleManager.resolveSignalStrength(signalScore);

        StrategySignalListItemVO vo = new StrategySignalListItemVO();
        vo.setSignalId(null);
        vo.setTaskId(task.getTaskId());
        vo.setTaskTitle(task.getTaskTitle());
        vo.setTaskType(task.getTaskType());
        vo.setTargetCode(task.getTargetCode());
        vo.setTargetName(task.getTargetName());
        vo.setPriority(task.getPriority());
        vo.setReportId(report.getReportId());
        vo.setReportType(report.getReportType());
        vo.setFinalStatus(report.getFinalStatus());
        vo.setSignalDirection(signalDirection.name());
        vo.setSignalStrength(signalStrength.name());
        vo.setSignalScore(signalScore);
        vo.setConfidenceScore(confidenceScore);
        vo.setReportReviewStatus(reviewStatus.name());
        vo.setReportReviewedBy(report.getReviewedBy());
        vo.setReportReviewedAt(report.getReviewedAt());
        vo.setNeedHumanReview(needHumanReview);
        vo.setReviewComment(report.getReviewComment());
        vo.setRevised(ruleManager.isReportRevised(report));
        vo.setSummaryRevised(ruleManager.isSummaryRevised(report));
        vo.setHighlightsRevised(ruleManager.isHighlightsRevised(report));
        vo.setRiskPointsRevised(ruleManager.isRiskPointsRevised(report));
        populateFollowUp(vo, followUpSummary);
        vo.setStrategySummary(strategySummary);
        vo.setSignalSources(signalSources);
        vo.setSignalSourceTags(ruleManager.buildSignalSourceTags(report, signalSources, totalRiskCount, needHumanReview, reviewStatus, confidenceScore));
        vo.setBacktestStatus(BACKTEST_STATUS_NOT_READY);
        vo.setBacktestSummary(BACKTEST_SUMMARY_NOT_READY);
        vo.setCreatedAt(firstNonNull(report.getCreatedAt(), task.getCreatedAt()));
        return vo;
    }

    private void populateFollowUp(StrategySignalListItemVO vo,
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

    private List<String> buildDomainSignalSources(StrategySignalDO signal, List<StrategySignalFactorDO> factors) {
        LinkedHashSet<String> sources = new LinkedHashSet<>();
        if (signal != null && signal.getReasonSummary() != null && !signal.getReasonSummary().isBlank()) {
            sources.add(signal.getReasonSummary().trim());
        }
        if (factors != null) {
            for (StrategySignalFactorDO factor : factors) {
                String conclusion = ruleManager.normalizeText(factor.getFactorConclusion());
                if (conclusion != null) {
                    sources.add(conclusion);
                    continue;
                }
                String factorName = ruleManager.normalizeText(factor.getFactorName());
                String factorValue = ruleManager.normalizeText(factor.getFactorValue());
                if (factorName != null && factorValue != null) {
                    sources.add(factorName + ": " + factorValue);
                } else if (factorName != null) {
                    sources.add(factorName);
                } else if (factorValue != null) {
                    sources.add(factorValue);
                }
            }
        }
        return new ArrayList<>(sources);
    }

    private List<String> buildDomainSignalSourceTags(StrategySignalDO signal,
                                                     List<StrategySignalFactorDO> factors,
                                                     boolean needHumanReview,
                                                     ReportReviewStatusEnum reviewStatus,
                                                     Double confidenceScore) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        tags.add("STRATEGY_SIGNAL");
        if (signal != null && signal.getSignalType() != null && !signal.getSignalType().isBlank()) {
            tags.add(signal.getSignalType().trim());
        }
        if (signal != null && signal.getSourceEventId() != null && !signal.getSourceEventId().isBlank()) {
            tags.add("EVENT_TRIGGERED");
        }
        if (factors != null && !factors.isEmpty()) {
            tags.add("FACTOR_EXPLAINED");
            boolean hasRiskFactor = factors.stream()
                    .map(StrategySignalFactorDO::getFactorCode)
                    .filter(Objects::nonNull)
                    .anyMatch(code -> "RISK_COUNT".equalsIgnoreCase(code) || "HUMAN_REVIEW".equalsIgnoreCase(code));
            if (hasRiskFactor) {
                tags.add("RISK_ADJUSTED");
            }
        }
        if (ruleManager.isHighConfidence(confidenceScore)) {
            tags.add("HIGH_CONFIDENCE");
        }
        if (needHumanReview) {
            tags.add("HUMAN_REVIEW");
        }
        if (reviewStatus == ReportReviewStatusEnum.REJECTED) {
            tags.add("REVIEW_REJECTED");
        }
        return new ArrayList<>(tags);
    }

    private LocalDateTime firstNonNull(LocalDateTime left, LocalDateTime right) {
        return left != null ? left : right;
    }
}
