package com.quant.aiorchestrator.manager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.vo.TaskReportVO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class TaskReportContextHydrationManager {

    private final ObjectMapper objectMapper;

    public TaskReportContextHydrationManager(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public boolean hydrateTaskReportContextFields(TaskReportVO report) {
        if (report == null || !hasText(report.getRawPayload())) {
            return false;
        }

        JsonNode reportMetaNode = extractReportMetaNode(report.getRawPayload());
        if (reportMetaNode == null) {
            return false;
        }

        boolean changed = false;

        Map<String, Object> contextSnapshot = readObjectMap(reportMetaNode.get("contextSnapshot"));
        if (!contextSnapshot.isEmpty()) {
            Map<String, Object> mergedContextSnapshot = mergeObjectMap(report.getContextSnapshot(), contextSnapshot);
            if (!Objects.equals(normalizeObjectMap(report.getContextSnapshot()), mergedContextSnapshot)) {
                report.setContextSnapshot(mergedContextSnapshot);
                changed = true;
            }
        }

        if (report.getEvidenceRefs() == null || report.getEvidenceRefs().isEmpty()) {
            List<String> evidenceRefs = readTextList(reportMetaNode.get("evidenceRefs"));
            if (!evidenceRefs.isEmpty()) {
                report.setEvidenceRefs(evidenceRefs);
                changed = true;
            }
        }

        if (report.getEvidenceItems() == null || report.getEvidenceItems().isEmpty()) {
            List<TaskReportVO.ReportEvidenceItemVO> evidenceItems = readEvidenceItems(reportMetaNode.get("evidenceItems"));
            if (!evidenceItems.isEmpty()) {
                report.setEvidenceItems(evidenceItems);
                changed = true;
            }
        }

        if (!hasText(report.getReviewSuggestion())) {
            String reviewSuggestion = normalizeText(reportMetaNode.path("reviewSuggestion").asText(null));
            if (reviewSuggestion != null) {
                report.setReviewSuggestion(reviewSuggestion);
                changed = true;
            }
        }

        JsonNode approvedPayloadNode = reportMetaNode.path("approvedPayload");
        if (approvedPayloadNode.isObject()) {
            Map<String, Object> strategyCandidate = readObjectMap(approvedPayloadNode.get("strategyCandidate"));
            if (!strategyCandidate.isEmpty() && !Objects.equals(report.getStrategyCandidate(), strategyCandidate)) {
                report.setStrategyCandidate(strategyCandidate);
                changed = true;
            }

            List<Map<String, Object>> strategyFactors = readObjectList(approvedPayloadNode.get("strategyFactors"));
            if (!strategyFactors.isEmpty() && !Objects.equals(report.getStrategyFactors(), strategyFactors)) {
                report.setStrategyFactors(strategyFactors);
                changed = true;
            }

            Map<String, Object> auditSupport = readObjectMap(approvedPayloadNode.get("auditSupport"));
            if (!auditSupport.isEmpty() && !Objects.equals(report.getAuditSupport(), auditSupport)) {
                report.setAuditSupport(auditSupport);
                changed = true;
            }
        }

        return changed;
    }

    private Map<String, Object> mergeObjectMap(Map<String, Object> current, Map<String, Object> latest) {
        Map<String, Object> merged = new java.util.LinkedHashMap<>(normalizeObjectMap(current));
        merged.putAll(normalizeObjectMap(latest));
        return merged;
    }

    private Map<String, Object> normalizeObjectMap(Map<String, Object> value) {
        return value == null ? Collections.emptyMap() : value;
    }

    private JsonNode extractReportMetaNode(String rawPayload) {
        if (!hasText(rawPayload)) {
            return null;
        }
        try {
            JsonNode payloadNode = objectMapper.readTree(rawPayload);
            JsonNode reportMetaNode = payloadNode.path("reportMeta");
            if (reportMetaNode.isMissingNode() || reportMetaNode.isNull() || !reportMetaNode.isObject()) {
                return null;
            }
            return reportMetaNode;
        } catch (Exception ignored) {
            return null;
        }
    }

    private Map<String, Object> readObjectMap(JsonNode node) {
        if (node == null || node.isNull() || !node.isObject()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.convertValue(node, new TypeReference<Map<String, Object>>() {});
        } catch (IllegalArgumentException ignored) {
            return Collections.emptyMap();
        }
    }

    private List<Map<String, Object>> readObjectList(JsonNode node) {
        if (node == null || node.isNull() || !node.isArray()) {
            return List.of();
        }
        try {
            return objectMapper.convertValue(node, new TypeReference<List<Map<String, Object>>>() {});
        } catch (IllegalArgumentException ignored) {
            return List.of();
        }
    }

    private List<String> readTextList(JsonNode node) {
        if (node == null || node.isNull() || !node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        node.forEach(item -> {
            String text = normalizeText(item.asText(null));
            if (text != null) {
                values.add(text);
            }
        });
        return values;
    }

    private List<TaskReportVO.ReportEvidenceItemVO> readEvidenceItems(JsonNode node) {
        if (node == null || node.isNull() || !node.isArray()) {
            return List.of();
        }
        List<TaskReportVO.ReportEvidenceItemVO> items = new ArrayList<>();
        node.forEach(item -> {
            if (item == null || item.isNull() || !item.isObject()) {
                return;
            }
            TaskReportVO.ReportEvidenceItemVO evidence = new TaskReportVO.ReportEvidenceItemVO();
            evidence.setEvidenceId(normalizeText(item.path("evidenceId").asText(null)));
            evidence.setEvidenceType(normalizeText(item.path("evidenceType").asText(null)));
            evidence.setSource(normalizeText(item.path("source").asText(null)));
            evidence.setTitle(normalizeText(item.path("title").asText(null)));
            evidence.setSummary(normalizeText(item.path("summary").asText(null)));
            evidence.setUrl(normalizeText(item.path("url").asText(null)));
            evidence.setOccurredAt(normalizeText(item.path("occurredAt").asText(null)));
            evidence.setReferenceId(normalizeText(item.path("referenceId").asText(null)));
            evidence.setRelevance(normalizeText(item.path("relevance").asText(null)));
            if (evidence.getEvidenceId() != null || evidence.getTitle() != null || evidence.getSummary() != null) {
                items.add(evidence);
            }
        });
        return items;
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
