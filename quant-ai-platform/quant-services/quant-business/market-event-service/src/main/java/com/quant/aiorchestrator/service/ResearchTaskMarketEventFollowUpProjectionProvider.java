package com.quant.aiorchestrator.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.projection.MarketEventFollowUpProjection;
import com.quant.aiorchestrator.manager.MarketEventNormalizationManager;
import com.quant.aiorchestrator.report.TaskReportProjection;
import com.quant.aiorchestrator.report.TaskReportReadPort;
import com.quant.aiorchestrator.report.TaskReportRiskDetailProjection;
import com.quant.aiorchestrator.report.TaskReportRiskProjection;
import com.quant.aiorchestrator.report.TaskReportRiskProjectionProvider;
import com.quant.task.market.MarketEventTaskProjection;
import com.quant.task.market.MarketEventTaskReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ResearchTaskMarketEventFollowUpProjectionProvider implements MarketEventFollowUpProjectionProvider {

    private static final String SOURCE_DOMAIN_MARKET_EVENT = "MARKET_EVENT";
    private static final String FOLLOW_UP_STATUS_NOT_TRACKED = "NOT_TRACKED";
    private static final String FOLLOW_UP_STATUS_TRACKING = "TRACKING";
    private static final String FOLLOW_UP_STATUS_COMPLETED = "COMPLETED";
    private static final String FOLLOW_UP_STATUS_FAILED = "FAILED";

    private final MarketEventTaskReadPort marketEventTaskReadPort;
    private final TaskReportReadPort taskReportReadPort;
    private final TaskReportRiskProjectionProvider taskReportRiskProjectionProvider;
    private final ObjectMapper objectMapper;
    private final MarketEventNormalizationManager normalizationManager;

    @Override
    public Map<String, MarketEventFollowUpProjection> loadFollowUpProjectionMap(List<String> eventIds) {
        List<String> validEventIds = eventIds == null ? List.of() : eventIds.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (validEventIds.isEmpty()) {
            return Map.of();
        }

        Map<String, List<MarketEventTaskProjection>> followUpTaskMap =
                marketEventTaskReadPort.loadFollowUpTasksBySourceEvents(SOURCE_DOMAIN_MARKET_EVENT, validEventIds);
        List<MarketEventTaskProjection> allFollowUpTasks = followUpTaskMap.values().stream().flatMap(List::stream).toList();
        Set<String> taskIds = allFollowUpTasks.stream()
                .map(MarketEventTaskProjection::taskId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        Map<String, TaskReportProjection> latestReportMap = loadLatestReportMap(taskIds);
        Map<String, TaskReportRiskProjection> latestRiskWarningMap =
                taskReportRiskProjectionProvider.loadLatestRiskWarningMapByTaskIds(taskIds);
        Map<String, List<TaskReportRiskDetailProjection>> riskWarningDetailMap =
                taskReportRiskProjectionProvider.loadRiskWarningDetailMapByWarningIds(
                        latestRiskWarningMap.values().stream()
                                .map(TaskReportRiskProjection::warningId)
                                .filter(StringUtils::hasText)
                                .collect(Collectors.toSet())
                );

        return validEventIds.stream().collect(Collectors.toMap(
                eventId -> eventId,
                eventId -> buildProjection(
                        eventId,
                        followUpTaskMap.getOrDefault(eventId, List.of()),
                        latestReportMap,
                        latestRiskWarningMap,
                        riskWarningDetailMap
                ),
                (left, right) -> left,
                java.util.LinkedHashMap::new
        ));
    }

    private Map<String, TaskReportProjection> loadLatestReportMap(Set<String> taskIds) {
        return taskReportReadPort.listReportsByTaskIdSet(taskIds).stream().collect(Collectors.toMap(
                TaskReportProjection::taskId,
                item -> item,
                (left, right) -> left,
                java.util.LinkedHashMap::new
        ));
    }

    private MarketEventFollowUpProjection buildProjection(String eventId,
                                                          List<MarketEventTaskProjection> followUpTasks,
                                                          Map<String, TaskReportProjection> latestReportMap,
                                                          Map<String, TaskReportRiskProjection> latestRiskWarningMap,
                                                          Map<String, List<TaskReportRiskDetailProjection>> riskWarningDetailMap) {
        List<MarketEventTaskProjection> safeFollowUpTasks = followUpTasks == null ? List.of() : followUpTasks;
        MarketEventTaskProjection latestFollowUp = safeFollowUpTasks.stream()
                .filter(Objects::nonNull)
                .max(Comparator
                        .comparing(MarketEventTaskProjection::createdAt, Comparator.nullsLast(LocalDateTime::compareTo))
                        .thenComparing(MarketEventTaskProjection::id, Comparator.nullsLast(Long::compareTo)))
                .orElse(null);
        List<TaskReportProjection> relatedReports = safeFollowUpTasks.stream()
                .map(MarketEventTaskProjection::taskId)
                .map(latestReportMap::get)
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing(TaskReportProjection::createdAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(TaskReportProjection::reportId, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        MarketEventFollowUpProjection.MarketEventFollowUpProjectionBuilder builder = MarketEventFollowUpProjection.builder()
                .eventId(eventId)
                .followUpTaskCount(safeFollowUpTasks.size())
                .followUpStatus(resolveFollowUpStatus(latestFollowUp))
                .relatedReportCount(relatedReports.size());
        if (latestFollowUp != null) {
            builder.latestFollowUpTaskId(latestFollowUp.taskId())
                    .latestFollowUpTaskTitle(latestFollowUp.taskTitle())
                    .latestFollowUpTaskStatus(latestFollowUp.status())
                    .latestFollowUpCreatedAt(latestFollowUp.createdAt());
        }
        if (!relatedReports.isEmpty()) {
            TaskReportProjection latestReport = relatedReports.get(0);
            TaskReportRiskProjection latestWarning = latestRiskWarningMap.get(latestReport.taskId());
            populateDerivedResultFields(
                    builder,
                    latestReport,
                    latestWarning,
                    latestWarning == null ? List.of() : riskWarningDetailMap.getOrDefault(latestWarning.warningId(), List.of())
            );
        }
        return builder.build();
    }

    private void populateDerivedResultFields(MarketEventFollowUpProjection.MarketEventFollowUpProjectionBuilder builder,
                                             TaskReportProjection report,
                                             TaskReportRiskProjection warning,
                                             List<TaskReportRiskDetailProjection> details) {
        if (builder == null || report == null) {
            return;
        }

        String summary = resolveReportSummary(report);
        List<String> warningList = warning == null ? readTextList(report.riskWarnings()) : buildDomainRiskWarningMessages(warning);
        int warningCount = warning == null ? warningList.size() : 1;
        int riskPointCount = warning == null
                ? readPreferredTextList(report.revisedRiskPoints(), report.riskPoints()).size()
                : (details == null ? 0 : details.size());
        int totalRiskCount = warningCount + riskPointCount;
        boolean needHumanReview = warning == null
                ? report.needHumanReview() != null && report.needHumanReview() == 1
                : isDomainRiskHumanReview(warning);
        Double confidenceScore = report.confidenceScore() == null ? null : report.confidenceScore().doubleValue();
        String reviewStatus = normalizeReviewStatus(report.reviewStatus());
        String riskLevel = warning == null
                ? (totalRiskCount > 0 || needHumanReview ? resolveRiskLevel(totalRiskCount, needHumanReview) : null)
                : resolveDomainRiskLevel(warning);
        String signalDirection = resolveSignalDirection(summary, totalRiskCount, needHumanReview, confidenceScore);
        int signalScore = calculateSignalScore(confidenceScore, totalRiskCount, needHumanReview, reviewStatus, signalDirection);

        builder.latestReportTaskId(report.taskId())
                .latestReportId(report.reportId())
                .latestReportType(normalizationManager.trimToNull(report.reportType()))
                .latestReportReviewStatus(reviewStatus)
                .latestReportSummary(summary)
                .latestReportConfidenceScore(report.confidenceScore())
                .latestNeedHumanReview(needHumanReview)
                .latestReportCreatedAt(report.createdAt())
                .derivedRiskLevel(riskLevel)
                .derivedWarningCount(warningCount)
                .derivedRiskPointCount(riskPointCount)
                .derivedRiskCount(totalRiskCount)
                .derivedSignalDirection(signalDirection)
                .derivedSignalStrength(resolveSignalStrength(signalScore))
                .derivedSignalScore(signalScore)
                .derivedIntelligenceType(resolveMarketIntelligenceType(totalRiskCount, needHumanReview, confidenceScore, signalDirection));
    }

    private String resolveFollowUpStatus(MarketEventTaskProjection latestFollowUp) {
        if (latestFollowUp == null || !StringUtils.hasText(latestFollowUp.status())) {
            return FOLLOW_UP_STATUS_NOT_TRACKED;
        }
        String status = latestFollowUp.status().trim().toUpperCase();
        if ("SUCCESS".equals(status)) {
            return FOLLOW_UP_STATUS_COMPLETED;
        }
        if ("FAILED".equals(status) || "CANCELLED".equals(status)) {
            return FOLLOW_UP_STATUS_FAILED;
        }
        return FOLLOW_UP_STATUS_TRACKING;
    }

    private List<String> buildDomainRiskWarningMessages(TaskReportRiskProjection warning) {
        LinkedHashSet<String> messages = new LinkedHashSet<>();
        if (warning == null) {
            return List.of();
        }
        String summary = normalizationManager.trimToNull(warning.warningSummary());
        if (summary != null) {
            messages.add(summary);
        }
        String reason = normalizationManager.trimToNull(warning.warningReason());
        if (reason != null) {
            for (String item : reason.split("\\R")) {
                if (item != null && !item.isBlank()) {
                    messages.add(item.trim());
                }
            }
        }
        return List.copyOf(messages);
    }

    private boolean isDomainRiskHumanReview(TaskReportRiskProjection warning) {
        if (warning == null) {
            return false;
        }
        if ("NEED_HUMAN_REVIEW".equalsIgnoreCase(normalizationManager.trimToNull(warning.suggestAction()))) {
            return true;
        }
        return "HIGH".equalsIgnoreCase(normalizationManager.trimToNull(warning.warningLevel()))
                && "PENDING".equalsIgnoreCase(normalizationManager.trimToNull(warning.reviewStatus()));
    }

    private String resolveDomainRiskLevel(TaskReportRiskProjection warning) {
        String warningLevel = normalizationManager.trimToNull(warning == null ? null : warning.warningLevel());
        if ("HIGH".equalsIgnoreCase(warningLevel)) {
            return "HIGH";
        }
        if ("MEDIUM".equalsIgnoreCase(warningLevel)) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String resolveReportSummary(TaskReportProjection report) {
        if (report == null) {
            return null;
        }
        if (StringUtils.hasText(report.revisedSummary())) {
            return report.revisedSummary().trim();
        }
        if (StringUtils.hasText(report.summary())) {
            return report.summary().trim();
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
        int positiveHit = countKeywords(normalizedSummary, List.of("growth", "improve", "benefit", "recover", "upside", "positive", "breakthrough", "policy support"));
        int negativeHit = countKeywords(normalizedSummary, List.of("decline", "pressure", "risk", "negative", "downside", "volatility", "default", "regulatory concern"))
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
