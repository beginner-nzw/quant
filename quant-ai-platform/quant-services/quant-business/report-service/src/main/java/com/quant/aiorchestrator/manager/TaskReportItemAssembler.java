package com.quant.aiorchestrator.manager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.entity.ResearchReportDO;
import com.quant.aiorchestrator.domain.vo.TaskReportVO;
import com.quant.aiorchestrator.report.TaskReportRiskDetailProjection;
import com.quant.aiorchestrator.report.TaskReportRiskProjection;
import com.quant.common.model.enums.ReportReviewStatusEnum;
import com.quant.common.model.enums.RiskLevelEnum;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Component
public class TaskReportItemAssembler {

    private final ObjectMapper objectMapper;

    public TaskReportItemAssembler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public TaskReportVO toTaskReportVO(ResearchReportDO report,
                                       TaskReportRiskProjection warning,
                                       List<TaskReportRiskDetailProjection> warningDetails) {
        String originalSummary = normalizeText(report.getSummary());
        String revisedSummary = normalizeText(report.getRevisedSummary());
        String displaySummary = resolveDisplaySummary(report.getRevisedSummary(), report.getSummary());

        List<String> originalHighlights = readTextList(report.getHighlights());
        List<String> revisedHighlights = readTextList(report.getRevisedHighlights());
        List<String> displayHighlights = resolveDisplayList(revisedHighlights, originalHighlights);

        List<String> fallbackRiskWarnings = readTextList(report.getRiskWarnings());
        List<String> domainRiskWarnings = warning == null ? List.of() : buildDomainRiskWarningMessages(warning);
        List<String> originalRiskPoints = readTextList(report.getRiskPoints());
        List<String> revisedRiskPoints = readTextList(report.getRevisedRiskPoints());
        List<String> displayRiskPoints = resolveDisplayList(revisedRiskPoints, originalRiskPoints);

        TaskReportVO vo = new TaskReportVO();
        vo.setTaskType(report.getTaskType());
        vo.setFinalStatus(report.getFinalStatus());
        vo.setReportId(report.getReportId());
        vo.setVersionNo(defaultVersionNo(report.getVersionNo()));
        vo.setReportType(resolveTaskReportType(report));
        vo.setSummary(displaySummary);
        vo.setOriginalSummary(originalSummary);
        vo.setDisplaySummary(displaySummary);
        vo.setConfidenceScore(report.getConfidenceScore() == null ? null : report.getConfidenceScore().doubleValue());
        vo.setNeedHumanReview(warning == null
                ? report.getNeedHumanReview() != null && report.getNeedHumanReview() == 1
                : resolveRiskProjection(report, warning, warningDetails).needHumanReview());
        vo.setRiskWarnings(domainRiskWarnings.isEmpty() ? fallbackRiskWarnings : domainRiskWarnings);
        vo.setOriginalHighlights(originalHighlights);
        vo.setDisplayHighlights(displayHighlights);
        vo.setOriginalRiskPoints(originalRiskPoints);
        vo.setDisplayRiskPoints(displayRiskPoints);
        vo.setResultRef(report.getResultRef());
        vo.setRawPayload(report.getRawPayload());
        vo.setReviewStatus(report.getReviewStatus());
        vo.setReviewedBy(report.getReviewedBy());
        vo.setReviewedAt(report.getReviewedAt() == null ? null : report.getReviewedAt().toString());
        vo.setRevisedSummary(revisedSummary);
        vo.setRevisedHighlights(revisedHighlights);
        vo.setRevisedRiskPoints(revisedRiskPoints);
        vo.setReviewComment(report.getReviewComment());

        TaskReportVO.ReportMetaVO meta = new TaskReportVO.ReportMetaVO();
        meta.setReportId(report.getReportId());
        meta.setReportType(resolveTaskReportType(report));
        meta.setHighlights(originalHighlights);
        meta.setRiskPoints(originalRiskPoints);
        meta.setSummary(originalSummary);
        vo.setReportMeta(meta);
        return vo;
    }

    private List<String> buildDomainRiskWarningMessages(TaskReportRiskProjection warning) {
        LinkedHashSet<String> messages = new LinkedHashSet<>();
        if (warning == null) {
            return List.of();
        }
        String summary = normalizeText(warning.warningSummary());
        if (summary != null) {
            messages.add(summary);
        }
        String reason = normalizeText(warning.warningReason());
        if (reason != null) {
            for (String item : reason.split("\\R")) {
                if (item != null && !item.isBlank()) {
                    messages.add(item.trim());
                }
            }
        }
        return new ArrayList<>(messages);
    }

    private RiskProjection resolveRiskProjection(ResearchReportDO report,
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

    private String resolveTaskReportType(ResearchReportDO report) {
        if (report.getReportType() != null && !report.getReportType().isBlank()) {
            return report.getReportType().trim();
        }
        if (report.getTaskType() != null && !report.getTaskType().isBlank()) {
            return report.getTaskType().trim();
        }
        return null;
    }

    private String resolveDisplaySummary(String preferredSummary, String fallbackSummary) {
        String normalizedPreferred = normalizeText(preferredSummary);
        return normalizedPreferred != null ? normalizedPreferred : normalizeText(fallbackSummary);
    }

    private List<String> resolveDisplayList(List<String> preferredItems, List<String> fallbackItems) {
        return preferredItems.isEmpty() ? fallbackItems : preferredItems;
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private List<String> readTextList(String rawJson) {
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

    private List<String> readPreferredTextList(String preferredRawJson, String fallbackRawJson) {
        List<String> preferred = readTextList(preferredRawJson);
        return preferred.isEmpty() ? readTextList(fallbackRawJson) : preferred;
    }

    private int defaultVersionNo(Integer versionNo) {
        return versionNo == null || versionNo < 1 ? 1 : versionNo;
    }

    private record RiskProjection(
            boolean needHumanReview,
            int warningCount,
            int riskPointCount,
            int totalRiskCount,
            RiskLevelEnum riskLevel
    ) {
    }
}
