package com.quant.aiorchestrator.manager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.entity.ResearchReportDO;
import com.quant.aiorchestrator.domain.entity.RiskWarningDO;
import com.quant.aiorchestrator.domain.entity.RiskWarningDetailDO;
import com.quant.aiorchestrator.domain.vo.MarketEventListItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MarketEventDerivedResultAssembler {

    private final ObjectMapper objectMapper;
    private final MarketEventNormalizationManager normalizationManager;

    public void populateDerivedResultFields(MarketEventListItemVO vo,
                                            ResearchReportDO report,
                                            RiskWarningDO warning,
                                            List<RiskWarningDetailDO> details) {
        if (vo == null || report == null) {
            return;
        }

        String summary = resolveReportSummary(report);
        List<String> warningList = warning == null ? readTextList(report.getRiskWarnings()) : buildDomainRiskWarningMessages(warning);
        int warningCount = warning == null ? warningList.size() : 1;
        int riskPointCount = warning == null
                ? readPreferredTextList(report.getRevisedRiskPoints(), report.getRiskPoints()).size()
                : (details == null ? 0 : details.size());
        int totalRiskCount = warningCount + riskPointCount;
        boolean needHumanReview = warning == null
                ? report.getNeedHumanReview() != null && report.getNeedHumanReview() == 1
                : isDomainRiskHumanReview(warning);
        Double confidenceScore = report.getConfidenceScore() == null ? null : report.getConfidenceScore().doubleValue();
        String reviewStatus = normalizeReviewStatus(report.getReviewStatus());
        String riskLevel = warning == null
                ? (totalRiskCount > 0 || needHumanReview ? resolveRiskLevel(totalRiskCount, needHumanReview) : null)
                : resolveDomainRiskLevel(warning);
        String signalDirection = resolveSignalDirection(summary, totalRiskCount, needHumanReview, confidenceScore);
        int signalScore = calculateSignalScore(confidenceScore, totalRiskCount, needHumanReview, reviewStatus, signalDirection);

        vo.setLatestReportTaskId(report.getTaskId());
        vo.setLatestReportId(report.getReportId());
        vo.setLatestReportType(normalizationManager.trimToNull(report.getReportType()));
        vo.setLatestReportReviewStatus(reviewStatus);
        vo.setLatestReportSummary(summary);
        vo.setLatestReportConfidenceScore(report.getConfidenceScore());
        vo.setLatestNeedHumanReview(needHumanReview);
        vo.setLatestReportCreatedAt(report.getCreatedAt());
        vo.setDerivedRiskLevel(riskLevel);
        vo.setDerivedWarningCount(warningCount);
        vo.setDerivedRiskPointCount(riskPointCount);
        vo.setDerivedRiskCount(totalRiskCount);
        vo.setDerivedSignalDirection(signalDirection);
        vo.setDerivedSignalStrength(resolveSignalStrength(signalScore));
        vo.setDerivedSignalScore(signalScore);
        vo.setDerivedIntelligenceType(resolveMarketIntelligenceType(totalRiskCount, needHumanReview, confidenceScore, signalDirection));
    }

    private List<String> buildDomainRiskWarningMessages(RiskWarningDO warning) {
        LinkedHashSet<String> messages = new LinkedHashSet<>();
        if (warning == null) {
            return List.of();
        }
        String summary = normalizationManager.trimToNull(warning.getWarningSummary());
        if (summary != null) {
            messages.add(summary);
        }
        String reason = normalizationManager.trimToNull(warning.getWarningReason());
        if (reason != null) {
            for (String item : reason.split("\\R")) {
                if (item != null && !item.isBlank()) {
                    messages.add(item.trim());
                }
            }
        }
        return List.copyOf(messages);
    }

    private boolean isDomainRiskHumanReview(RiskWarningDO warning) {
        if (warning == null) {
            return false;
        }
        if ("NEED_HUMAN_REVIEW".equalsIgnoreCase(normalizationManager.trimToNull(warning.getSuggestAction()))) {
            return true;
        }
        return "HIGH".equalsIgnoreCase(normalizationManager.trimToNull(warning.getWarningLevel()))
                && "PENDING".equalsIgnoreCase(normalizationManager.trimToNull(warning.getReviewStatus()));
    }

    private String resolveDomainRiskLevel(RiskWarningDO warning) {
        String warningLevel = normalizationManager.trimToNull(warning == null ? null : warning.getWarningLevel());
        if ("HIGH".equalsIgnoreCase(warningLevel)) {
            return "HIGH";
        }
        if ("MEDIUM".equalsIgnoreCase(warningLevel)) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String resolveReportSummary(ResearchReportDO report) {
        if (report == null) {
            return null;
        }
        if (StringUtils.hasText(report.getRevisedSummary())) {
            return report.getRevisedSummary().trim();
        }
        if (StringUtils.hasText(report.getSummary())) {
            return report.getSummary().trim();
        }
        return null;
    }

    private List<String> readTextList(String rawJson) {
        if (!StringUtils.hasText(rawJson)) {
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

    private List<String> readPreferredTextList(String preferredJson, String fallbackJson) {
        List<String> preferred = readTextList(preferredJson);
        return preferred.isEmpty() ? readTextList(fallbackJson) : preferred;
    }

    private String normalizeReviewStatus(String reviewStatus) {
        if ("APPROVED".equalsIgnoreCase(reviewStatus)) {
            return "APPROVED";
        }
        if ("REJECTED".equalsIgnoreCase(reviewStatus)) {
            return "REJECTED";
        }
        return "PENDING";
    }

    private String resolveRiskLevel(int totalRiskCount, boolean needHumanReview) {
        if (needHumanReview || totalRiskCount >= 4) {
            return "HIGH";
        }
        if (totalRiskCount >= 2) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String resolveSignalDirection(String summary,
                                          int totalRiskCount,
                                          boolean needHumanReview,
                                          Double confidenceScore) {
        String normalizedSummary = summary == null ? "" : summary.toLowerCase();
        int positiveHit = countKeywords(normalizedSummary, List.of("增长", "改善", "受益", "修复", "上行", "提振", "利好", "突破"));
        int negativeHit = countKeywords(normalizedSummary, List.of("下滑", "承压", "不及预期", "减值", "下行", "风险", "波动", "利空"))
                + totalRiskCount
                + (needHumanReview ? 1 : 0);

        if (negativeHit >= positiveHit + 2) {
            return "NEGATIVE";
        }
        if (positiveHit >= negativeHit + 2 && !needHumanReview && totalRiskCount <= 1 && isHighConfidence(confidenceScore)) {
            return "POSITIVE";
        }
        if (needHumanReview || totalRiskCount >= 3) {
            return "NEGATIVE";
        }
        if (isHighConfidence(confidenceScore) && totalRiskCount == 0) {
            return "POSITIVE";
        }
        return "NEUTRAL";
    }

    private int calculateSignalScore(Double confidenceScore,
                                     int totalRiskCount,
                                     boolean needHumanReview,
                                     String reviewStatus,
                                     String signalDirection) {
        int score = confidenceScore == null ? 60 : (int) Math.round(Math.max(0D, Math.min(1D, confidenceScore)) * 100D);
        score -= totalRiskCount * 8;
        if (needHumanReview) {
            score -= 12;
        }
        if ("REJECTED".equalsIgnoreCase(reviewStatus)) {
            score -= 10;
        }
        if ("POSITIVE".equalsIgnoreCase(signalDirection)) {
            score += 5;
        }
        if ("NEGATIVE".equalsIgnoreCase(signalDirection)) {
            score -= 5;
        }
        return Math.max(0, Math.min(100, score));
    }

    private String resolveSignalStrength(int signalScore) {
        if (signalScore >= 80) {
            return "STRONG";
        }
        if (signalScore >= 60) {
            return "MEDIUM";
        }
        return "WEAK";
    }

    private String resolveMarketIntelligenceType(int totalRiskCount,
                                                 boolean needHumanReview,
                                                 Double confidenceScore,
                                                 String signalDirection) {
        if (needHumanReview || totalRiskCount > 0) {
            return "RISK_ALERT";
        }
        if ("POSITIVE".equalsIgnoreCase(signalDirection)
                || "NEGATIVE".equalsIgnoreCase(signalDirection)
                || isHighConfidence(confidenceScore)) {
            return "STRATEGY_SIGNAL";
        }
        return "REPORT_INSIGHT";
    }

    private boolean isHighConfidence(Double confidenceScore) {
        return confidenceScore != null && confidenceScore >= 0.8D;
    }

    private int countKeywords(String content, List<String> keywords) {
        if (!StringUtils.hasText(content) || keywords == null || keywords.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String keyword : keywords) {
            if (StringUtils.hasText(keyword) && content.contains(keyword.toLowerCase())) {
                count++;
            }
        }
        return count;
    }
}
