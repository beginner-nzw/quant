package com.quant.aiorchestrator.manager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.report.TaskReportProjection;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.common.model.enums.ReportReviewStatusEnum;
import com.quant.common.model.enums.RiskLevelEnum;
import com.quant.common.model.enums.SignalDirectionEnum;
import com.quant.common.model.enums.SignalStrengthEnum;
import com.quant.task.workbench.ResearchWorkbenchRiskDetailProjection;
import com.quant.task.workbench.ResearchWorkbenchRiskProjection;
import com.quant.task.workbench.ResearchWorkbenchStrategyProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class ResearchWorkbenchRuleManager {

    private static final List<String> POSITIVE_SIGNAL_HINTS = List.of(
            "growth", "improve", "increase", "benefit", "bullish", "strong", "stable", "recovery", "opportunity",
            "positive", "upside", "beat"
    );
    private static final List<String> NEGATIVE_SIGNAL_HINTS = List.of(
            "risk", "decline", "pressure", "bearish", "cautious", "volatile", "pullback", "downtrend", "uncertain", "loss",
            "negative", "downside", "miss"
    );

    private final ObjectMapper objectMapper;

    public RiskProjection resolveRiskProjection(TaskReportProjection report,
                                                ResearchWorkbenchRiskProjection warning,
                                                List<ResearchWorkbenchRiskDetailProjection> details) {
        if (warning != null) {
            int warningCount = 1;
            int riskPointCount = details == null ? 0 : details.size();
            return new RiskProjection(
                    isDomainRiskHumanReview(warning),
                    warningCount,
                    riskPointCount,
                    warningCount + riskPointCount,
                    resolveDomainRiskLevel(warning)
            );
        }
        int warningCount = report == null ? 0 : readTextList(report.getRiskWarnings()).size();
        int riskPointCount = report == null ? 0 : readPreferredTextList(report.getRevisedRiskPoints(), report.getRiskPoints()).size();
        boolean needHumanReview = report != null && report.getNeedHumanReview() != null && report.getNeedHumanReview() == 1;
        int totalRiskCount = warningCount + riskPointCount;
        return new RiskProjection(
                needHumanReview,
                warningCount,
                riskPointCount,
                totalRiskCount,
                totalRiskCount > 0 || needHumanReview ? resolveRiskLevel(totalRiskCount, needHumanReview) : null
        );
    }

    public RiskProjection resolveRiskProjection(TaskReportProjection report, ResearchWorkbenchRiskProjection warning) {
        return resolveRiskProjection(report, warning, List.of());
    }

    public ReportReviewStatusEnum resolveReviewStatus(String reviewStatus) {
        ReportReviewStatusEnum resolved = ReportReviewStatusEnum.from(reviewStatus);
        return resolved == null ? ReportReviewStatusEnum.PENDING : resolved;
    }

    public SignalDirectionEnum resolveSignalDirection(String strategySummary,
                                                      int totalRiskCount,
                                                      boolean needHumanReview,
                                                      Double confidenceScore) {
        String normalizedSummary = strategySummary == null ? "" : strategySummary.toLowerCase();
        int positiveHit = countKeywords(normalizedSummary, POSITIVE_SIGNAL_HINTS);
        int negativeHit = countKeywords(normalizedSummary, NEGATIVE_SIGNAL_HINTS) + totalRiskCount + (needHumanReview ? 1 : 0);
        if (negativeHit >= positiveHit + 2) {
            return SignalDirectionEnum.NEGATIVE;
        }
        if (positiveHit >= negativeHit + 2 && !needHumanReview && totalRiskCount <= 1 && isHighConfidence(confidenceScore)) {
            return SignalDirectionEnum.POSITIVE;
        }
        if (needHumanReview || totalRiskCount >= 3) {
            return SignalDirectionEnum.NEGATIVE;
        }
        if (isHighConfidence(confidenceScore) && totalRiskCount == 0) {
            return SignalDirectionEnum.POSITIVE;
        }
        return SignalDirectionEnum.NEUTRAL;
    }

    public SignalDirectionEnum resolveDomainSignalDirection(ResearchWorkbenchStrategyProjection signal) {
        SignalDirectionEnum resolved = signal == null ? null : SignalDirectionEnum.from(signal.signalDirection());
        if (resolved != null) {
            return resolved;
        }
        Double confidenceScore = signal == null || signal.confidenceScore() == null ? null : signal.confidenceScore().doubleValue();
        return resolveSignalDirection(signal == null ? null : signal.reasonSummary(), 0, false, confidenceScore);
    }

    public SignalStrengthEnum resolveDomainSignalStrength(ResearchWorkbenchStrategyProjection signal) {
        SignalStrengthEnum resolved = signal == null ? null : SignalStrengthEnum.from(signal.signalLevel());
        if (resolved != null) {
            return resolved;
        }
        if (signal != null && signal.signalScore() != null) {
            return resolveSignalStrength(signal.signalScore());
        }
        Double confidenceScore = signal == null || signal.confidenceScore() == null ? null : signal.confidenceScore().doubleValue();
        int fallbackScore = confidenceScore == null ? 60 : (int) Math.round(Math.max(0D, Math.min(1D, confidenceScore)) * 100D);
        return resolveSignalStrength(fallbackScore);
    }

    public SignalStrengthEnum resolveSignalStrength(int signalScore) {
        if (signalScore >= 80) {
            return SignalStrengthEnum.STRONG;
        }
        if (signalScore >= 60) {
            return SignalStrengthEnum.MEDIUM;
        }
        return SignalStrengthEnum.WEAK;
    }

    public int calculateSignalScore(Double confidenceScore,
                                    int totalRiskCount,
                                    boolean needHumanReview,
                                    ReportReviewStatusEnum reviewStatus,
                                    SignalDirectionEnum signalDirection) {
        int score = confidenceScore == null ? 60 : (int) Math.round(Math.max(0D, Math.min(1D, confidenceScore)) * 100D);
        score -= totalRiskCount * 8;
        if (needHumanReview) {
            score -= 12;
        }
        if (reviewStatus == ReportReviewStatusEnum.REJECTED) {
            score -= 10;
        }
        if (signalDirection == SignalDirectionEnum.POSITIVE) {
            score += 5;
        }
        if (signalDirection == SignalDirectionEnum.NEGATIVE) {
            score -= 5;
        }
        return Math.max(0, Math.min(100, score));
    }

    public boolean isHighConfidence(Double confidenceScore) {
        return confidenceScore != null && confidenceScore >= 0.8D;
    }

    public List<String> readTextList(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(rawJson, new TypeReference<List<String>>() {})
                    .stream()
                    .filter(item -> item != null && !item.isBlank())
                    .map(String::trim)
                    .toList();
        } catch (Exception ignored) {
            return List.of();
        }
    }

    public List<String> readPreferredTextList(String preferredRawJson, String fallbackRawJson) {
        List<String> preferred = readTextList(preferredRawJson);
        return preferred.isEmpty() ? readTextList(fallbackRawJson) : preferred;
    }

    public boolean isReportRevised(TaskReportProjection report) {
        return isSummaryRevised(report) || isHighlightsRevised(report) || isRiskPointsRevised(report);
    }

    public boolean isSummaryRevised(TaskReportProjection report) {
        if (report == null) {
            return false;
        }
        return !Objects.equals(normalizeText(report.getSummary()), resolveDisplaySummary(report.getRevisedSummary(), report.getSummary()));
    }

    public boolean isHighlightsRevised(TaskReportProjection report) {
        if (report == null) {
            return false;
        }
        return !readTextList(report.getHighlights()).equals(readPreferredTextList(report.getRevisedHighlights(), report.getHighlights()));
    }

    public boolean isRiskPointsRevised(TaskReportProjection report) {
        if (report == null) {
            return false;
        }
        return !readTextList(report.getRiskPoints()).equals(readPreferredTextList(report.getRevisedRiskPoints(), report.getRiskPoints()));
    }

    public String resolveReportType(TaskReportProjection report, ResearchTaskDO task) {
        if (report.getReportType() != null && !report.getReportType().isBlank()) {
            return report.getReportType().trim();
        }
        return task == null ? null : task.getTaskType();
    }

    public String resolveDisplaySummary(String preferredSummary, String fallbackSummary) {
        String normalizedPreferred = normalizeText(preferredSummary);
        return normalizedPreferred != null ? normalizedPreferred : normalizeText(fallbackSummary);
    }

    public String normalizeText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private int countKeywords(String content, List<String> keywords) {
        if (content == null || content.isBlank() || keywords == null || keywords.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String keyword : keywords) {
            if (keyword != null && !keyword.isBlank() && content.contains(keyword.toLowerCase())) {
                count++;
            }
        }
        return count;
    }

    private boolean isDomainRiskHumanReview(ResearchWorkbenchRiskProjection warning) {
        if (warning == null) {
            return false;
        }
        if ("NEED_HUMAN_REVIEW".equalsIgnoreCase(warning.suggestAction())) {
            return true;
        }
        RiskLevelEnum riskLevel = RiskLevelEnum.from(warning.warningLevel());
        return riskLevel == RiskLevelEnum.HIGH
                && ReportReviewStatusEnum.PENDING.name().equalsIgnoreCase(warning.reviewStatus());
    }

    private RiskLevelEnum resolveDomainRiskLevel(ResearchWorkbenchRiskProjection warning) {
        RiskLevelEnum resolved = warning == null ? null : RiskLevelEnum.from(warning.warningLevel());
        return resolved == null ? RiskLevelEnum.LOW : resolved;
    }

    private RiskLevelEnum resolveRiskLevel(int totalRiskCount, boolean needHumanReview) {
        if (needHumanReview || totalRiskCount >= 4) {
            return RiskLevelEnum.HIGH;
        }
        if (totalRiskCount >= 2) {
            return RiskLevelEnum.MEDIUM;
        }
        return RiskLevelEnum.LOW;
    }

    public record RiskProjection(
            boolean needHumanReview,
            int warningCount,
            int riskPointCount,
            int totalRiskCount,
            RiskLevelEnum riskLevel
    ) {
    }
}
