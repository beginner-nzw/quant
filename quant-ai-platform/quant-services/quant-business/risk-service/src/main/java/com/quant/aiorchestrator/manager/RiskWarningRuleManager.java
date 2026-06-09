package com.quant.aiorchestrator.manager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.entity.RiskWarningDO;
import com.quant.aiorchestrator.domain.entity.RiskWarningDetailDO;
import com.quant.aiorchestrator.report.TaskReportProjection;
import com.quant.common.model.enums.ReportReviewStatusEnum;
import com.quant.common.model.enums.RiskLevelEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class RiskWarningRuleManager {

    private final ObjectMapper objectMapper;

    public RiskProjection resolveRiskProjection(TaskReportProjection report,
                                                RiskWarningDO warning,
                                                List<RiskWarningDetailDO> details) {
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

    public RiskProjection resolveRiskProjection(TaskReportProjection report, RiskWarningDO warning) {
        return resolveRiskProjection(report, warning, List.of());
    }

    public ReportReviewStatusEnum resolveReviewStatus(String reviewStatus) {
        ReportReviewStatusEnum resolved = ReportReviewStatusEnum.from(reviewStatus);
        return resolved == null ? ReportReviewStatusEnum.PENDING : resolved;
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
                normalizeText(report.getSummary()),
                resolveDisplaySummary(report.getRevisedSummary(), report.getSummary())
        );
    }

    public boolean isHighlightsRevised(TaskReportProjection report) {
        if (report == null) {
            return false;
        }
        return !readTextList(report.getHighlights()).equals(
                readPreferredTextList(report.getRevisedHighlights(), report.getHighlights())
        );
    }

    public boolean isRiskPointsRevised(TaskReportProjection report) {
        if (report == null) {
            return false;
        }
        return !readTextList(report.getRiskPoints()).equals(
                readPreferredTextList(report.getRevisedRiskPoints(), report.getRiskPoints())
        );
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

    private boolean isDomainRiskHumanReview(RiskWarningDO warning) {
        if (warning == null) {
            return false;
        }
        if ("NEED_HUMAN_REVIEW".equalsIgnoreCase(warning.getSuggestAction())) {
            return true;
        }
        RiskLevelEnum riskLevel = RiskLevelEnum.from(warning.getWarningLevel());
        return riskLevel == RiskLevelEnum.HIGH
                && ReportReviewStatusEnum.PENDING.name().equalsIgnoreCase(warning.getReviewStatus());
    }

    private RiskLevelEnum resolveDomainRiskLevel(RiskWarningDO warning) {
        RiskLevelEnum resolved = warning == null ? null : RiskLevelEnum.from(warning.getWarningLevel());
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
