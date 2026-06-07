package com.quant.aiorchestrator.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.entity.ReportEvidenceRefDO;
import com.quant.aiorchestrator.domain.entity.ResearchReportDO;
import com.quant.aiorchestrator.domain.entity.ResearchReportSectionDO;
import com.quant.aiorchestrator.mapper.ReportEvidenceRefMapper;
import com.quant.aiorchestrator.mapper.ResearchReportSectionMapper;
import com.quant.common.model.enums.ReportReviewStatusEnum;
import com.quant.common.model.message.AiTaskResultMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AiResultReportProjectionManager {

    private final ReportEvidenceRefMapper reportEvidenceRefMapper;
    private final ResearchReportSectionMapper researchReportSectionMapper;
    private final ObjectMapper objectMapper;

    public void saveReportProjection(AiTaskResultMessage message, ResearchReportDO report) {
        saveReportEvidenceRefs(message, report);
        saveReportSections(message, report);
    }

    private void saveReportEvidenceRefs(AiTaskResultMessage message, ResearchReportDO report) {
        AiTaskResultMessage.ResultPayload payload = message.getPayload();
        reportEvidenceRefMapper.delete(
                new LambdaQueryWrapper<ReportEvidenceRefDO>()
                        .eq(ReportEvidenceRefDO::getReportId, report.getReportId())
        );

        Object evidenceItems = approvedPayloadValue(payload, "evidenceItems");
        if (evidenceItems instanceof List<?> items && !items.isEmpty()) {
            for (Object item : items) {
                if (!(item instanceof Map<?, ?> evidenceItem)) {
                    continue;
                }
                ReportEvidenceRefDO entity = new ReportEvidenceRefDO();
                entity.setEvidenceId(UUID.randomUUID().toString());
                entity.setReportId(report.getReportId());
                entity.setTaskId(message.getTaskId());
                entity.setSourceType(limit(defaultValue(normalizeText(evidenceItem.get("evidenceType")), "REPORT_META"), 64));
                entity.setSourceRefId(limit(firstNonBlank(
                        normalizeText(evidenceItem.get("referenceId")),
                        normalizeText(evidenceItem.get("evidenceId"))
                ), 128));
                entity.setEvidenceSummary(firstNonBlank(
                        normalizeText(evidenceItem.get("summary")),
                        normalizeText(evidenceItem.get("title"))
                ));
                entity.setEvidenceUrl(limit(normalizeText(evidenceItem.get("url")), 512));
                entity.setDeleted(0);
                reportEvidenceRefMapper.insert(entity);
            }
            return;
        }

        for (String ref : normalizeTextList(approvedPayloadValue(payload, "evidenceRefs"))) {
            ReportEvidenceRefDO entity = new ReportEvidenceRefDO();
            entity.setEvidenceId(UUID.randomUUID().toString());
            entity.setReportId(report.getReportId());
            entity.setTaskId(message.getTaskId());
            entity.setSourceType(limit("EVIDENCE_REF", 64));
            entity.setSourceRefId(limit(ref, 128));
            entity.setEvidenceSummary(ref);
            entity.setDeleted(0);
            reportEvidenceRefMapper.insert(entity);
        }
    }

    private void saveReportSections(AiTaskResultMessage message, ResearchReportDO report) {
        AiTaskResultMessage.ResultPayload payload = message.getPayload();
        researchReportSectionMapper.delete(
                new LambdaQueryWrapper<ResearchReportSectionDO>()
                        .eq(ResearchReportSectionDO::getReportId, report.getReportId())
        );

        List<String> highlights = normalizeTextList(approvedPayloadValue(payload, "highlights"));
        List<String> riskPoints = normalizeTextList(approvedPayloadValue(payload, "riskPoints"));
        List<String> riskWarnings = approvedTextList(payload, "riskWarnings", payload.getRiskWarnings());
        List<String> evidenceRefs = normalizeTextList(approvedPayloadValue(payload, "evidenceRefs"));

        int order = 10;
        order = insertReportSection(
                message,
                report,
                "SUMMARY",
                "investment summary",
                order,
                firstNonBlank(normalizeText(approvedPayloadValue(payload, "summary")), normalizeText(payload.getSummary())),
                List.of()
        );
        order = insertReportSection(
                message,
                report,
                "HIGHLIGHTS",
                "key highlights",
                order,
                null,
                highlights
        );
        order = insertReportSection(
                message,
                report,
                "RISK_POINTS",
                "risk points",
                order,
                null,
                mergeTextList(riskWarnings, riskPoints)
        );
        insertReportSection(
                message,
                report,
                "EVIDENCE",
                "evidence refs",
                order,
                null,
                evidenceRefs
        );
    }

    private int insertReportSection(AiTaskResultMessage message,
                                    ResearchReportDO report,
                                    String sectionCode,
                                    String sectionTitle,
                                    int sectionOrder,
                                    String content,
                                    List<String> items) {
        List<String> safeItems = items == null ? List.of() : items;
        if (!StringUtils.hasText(content) && safeItems.isEmpty()) {
            return sectionOrder;
        }

        ResearchReportSectionDO section = new ResearchReportSectionDO();
        section.setSectionId(UUID.randomUUID().toString());
        section.setReportId(report.getReportId());
        section.setTaskId(message.getTaskId());
        section.setVersionNo(defaultVersionNo(report.getVersionNo()));
        section.setSectionCode(limit(sectionCode, 64));
        section.setSectionTitle(limit(sectionTitle, 128));
        section.setSectionOrder(sectionOrder);
        section.setSectionContent(content);
        section.setSectionItems(toJson(safeItems));
        section.setReviewStatus(ReportReviewStatusEnum.PENDING.name());
        section.setConfidenceScore(toBigDecimal(message.getPayload().getConfidenceScore()));
        section.setTraceId(limit(message.getTraceId(), 128));
        section.setTenantId(limit(defaultValue(message.getTenantId(), "default"), 64));
        section.setDeleted(0);
        researchReportSectionMapper.insert(section);
        return sectionOrder + 10;
    }

    private int defaultVersionNo(Integer versionNo) {
        return versionNo == null || versionNo < 1 ? 1 : versionNo;
    }

    private String toJson(List<String> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(items);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private Object approvedPayloadValue(AiTaskResultMessage.ResultPayload payload, String key) {
        Map<String, Object> reportMeta = payload.getReportMeta();
        if (reportMeta == null) {
            return null;
        }
        Object approvedPayload = reportMeta.get("approvedPayload");
        if (!(approvedPayload instanceof Map<?, ?> approvedPayloadMap)) {
            return null;
        }
        return approvedPayloadMap.get(key);
    }

    private List<String> approvedTextList(AiTaskResultMessage.ResultPayload payload, String key, List<String> fallback) {
        List<String> approved = normalizeTextList(approvedPayloadValue(payload, key));
        if (!approved.isEmpty()) {
            return approved;
        }
        return normalizeTextList(fallback);
    }

    private List<String> mergeTextList(List<String> left, List<String> right) {
        List<String> result = new ArrayList<>();
        result.addAll(left);
        for (String item : right) {
            if (!result.contains(item)) {
                result.add(item);
            }
        }
        return result;
    }

    private List<String> normalizeTextList(Object value) {
        if (!(value instanceof List<?> values)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object item : values) {
            String text = normalizeText(item);
            if (StringUtils.hasText(text) && !result.contains(text)) {
                result.add(text);
            }
        }
        return result;
    }

    private String firstNonBlank(String first, String second) {
        if (StringUtils.hasText(first)) {
            return first;
        }
        if (StringUtils.hasText(second)) {
            return second;
        }
        return null;
    }

    private String normalizeText(Object value) {
        if (value == null) {
            return null;
        }
        String normalized = String.valueOf(value).trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private BigDecimal toBigDecimal(Double value) {
        if (value == null) {
            return null;
        }
        return BigDecimal.valueOf(Math.max(0.0, Math.min(1.0, value)));
    }

    private String defaultValue(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String limit(String value, int maxLength) {
        if (!StringUtils.hasText(value) || maxLength <= 0) {
            return value;
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
