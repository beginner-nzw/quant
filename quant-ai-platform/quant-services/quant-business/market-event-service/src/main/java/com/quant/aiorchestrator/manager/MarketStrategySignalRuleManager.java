package com.quant.aiorchestrator.manager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.report.TaskReportProjection;
import com.quant.aiorchestrator.report.TaskReportRiskDetailProjection;
import com.quant.aiorchestrator.report.TaskReportRiskProjection;
import com.quant.aiorchestrator.risk.StrategySignalReadProjection;
import com.quant.common.model.enums.ReportReviewStatusEnum;
import com.quant.common.model.enums.RiskLevelEnum;
import com.quant.common.model.enums.SignalDirectionEnum;
import com.quant.task.market.MarketEventTaskProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class MarketStrategySignalRuleManager {

    private static final List<String> POSITIVE_SIGNAL_HINTS = List.of(
            "\u589e\u957f", "\u6539\u5584", "\u63d0\u5347", "\u5229\u597d", "\u770b\u597d",
            "\u5f3a\u52b2", "\u7a33\u5065", "\u4fee\u590d", "\u673a\u4f1a", "\u53d7\u76ca",
            "positive", "upside", "beat"
    );
    private static final List<String> NEGATIVE_SIGNAL_HINTS = List.of(
            "\u98ce\u9669", "\u4e0b\u6ed1", "\u627f\u538b", "\u5229\u7a7a", "\u8c28\u614e",
            "\u6ce2\u52a8", "\u56de\u843d", "\u4e0b\u884c", "\u4e0d\u786e\u5b9a", "\u4e8f\u635f",
            "negative", "downside", "miss"
    );

    private final ObjectMapper objectMapper;

    public RiskProjection resolveRiskProjection(TaskReportProjection report,
                                                TaskReportRiskProjection warning,
                                                List<TaskReportRiskDetailProjection> details) {
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
        int warningCount = report == null ? 0 : readTextList(report.riskWarnings()).size();
        int riskPointCount = report == null ? 0 : readPreferredTextList(report.revisedRiskPoints(), report.riskPoints()).size();
        boolean needHumanReview = report != null && report.needHumanReview() != null && report.needHumanReview() == 1;
        int totalRiskCount = warningCount + riskPointCount;
        return new RiskProjection(
                needHumanReview,
                warningCount,
                riskPointCount,
                totalRiskCount,
                totalRiskCount > 0 || needHumanReview ? resolveRiskLevel(totalRiskCount, needHumanReview) : null
        );
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

    public SignalDirectionEnum resolveDomainSignalDirection(StrategySignalReadProjection signal) {
        SignalDirectionEnum resolved = signal == null ? null : SignalDirectionEnum.from(signal.signalDirection());
        if (resolved != null) {
            return resolved;
        }
        Double confidenceScore = signal == null || signal.confidenceScore() == null
                ? null
                : signal.confidenceScore().doubleValue();
        return resolveSignalDirection(signal == null ? null : signal.reasonSummary(), 0, false, confidenceScore);
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
        return !Objects.equals(
                normalizeText(report.summary()),
                resolveDisplaySummary(report.revisedSummary(), report.summary())
        );
    }

    public boolean isHighlightsRevised(TaskReportProjection report) {
        if (report == null) {
            return false;
        }
        return !readTextList(report.highlights()).equals(
                readPreferredTextList(report.revisedHighlights(), report.highlights())
        );
    }

    public boolean isRiskPointsRevised(TaskReportProjection report) {
        if (report == null) {
            return false;
        }
        return !readTextList(report.riskPoints()).equals(
                readPreferredTextList(report.revisedRiskPoints(), report.riskPoints())
        );
    }

    public String resolveReportType(TaskReportProjection report, MarketEventTaskProjection task) {
        if (report.reportType() != null && !report.reportType().isBlank()) {
            return report.reportType().trim();
        }
        return task == null ? null : task.taskType();
    }

    public String resolveDisplaySummary(String preferredSummary, String fallbackSummary) {
        String normalizedPreferred = normalizeText(preferredSummary);
        return normalizedPreferred != null ? normalizedPreferred : normalizeText(fallbackSummary);
    }

    public String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
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

    private boolean isDomainRiskHumanReview(TaskReportRiskProjection warning) {
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

    private RiskLevelEnum resolveDomainRiskLevel(TaskReportRiskProjection warning) {
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
