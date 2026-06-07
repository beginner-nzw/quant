package com.quant.aiorchestrator.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.vo.ReportVersionCompareVO;
import com.quant.aiorchestrator.domain.vo.ReportVersionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ReportVersionCompareManager {

    private final ObjectMapper objectMapper;

    public ReportVersionCompareVO compare(String taskId, ReportVersionVO from, ReportVersionVO to) {
        ReportVersionCompareVO vo = new ReportVersionCompareVO();
        vo.setTaskId(taskId);
        vo.setReportId(firstNonBlank(from.getReportId(), to.getReportId()));
        vo.setFromVersionNo(defaultVersionNo(from.getVersionNo()));
        vo.setToVersionNo(defaultVersionNo(to.getVersionNo()));
        vo.setSameVersion(Objects.equals(vo.getFromVersionNo(), vo.getToVersionNo()));
        vo.setFromVersion(toSummary(from));
        vo.setToVersion(toSummary(to));

        Map<String, Object> fromSnapshot = safeMap(from.getSnapshot());
        Map<String, Object> toSnapshot = safeMap(to.getSnapshot());
        Map<String, Object> fromReport = childMap(fromSnapshot, "report");
        Map<String, Object> toReport = childMap(toSnapshot, "report");

        List<ReportVersionCompareVO.FieldChange> reportChanges = diffFields(
                "report",
                fromReport,
                toReport,
                List.of("reportId", "taskId", "versionNo", "taskType", "finalStatus", "summary",
                        "confidenceScore", "needHumanReview", "reportType", "highlights", "riskPoints",
                        "riskWarnings", "resultRef", "rawPayload")
        );
        List<ReportVersionCompareVO.FieldChange> reviewChanges = diffFields(
                "report.review",
                fromReport,
                toReport,
                List.of("reviewStatus", "reviewedBy", "reviewedAt", "revisedSummary",
                        "revisedHighlights", "revisedRiskPoints", "reviewComment")
        );

        DiffResult sectionDiff = diffItemList(
                "sections",
                childList(fromSnapshot, "sections"),
                childList(toSnapshot, "sections"),
                this::sectionKey,
                List.of("sectionTitle", "sectionOrder", "sectionContent", "sectionItems",
                        "revisedContent", "revisedItems", "reviewStatus", "reviewedBy",
                        "reviewedAt", "reviewComment", "confidenceScore")
        );
        DiffResult evidenceDiff = diffItemList(
                "evidenceRefs",
                childList(fromSnapshot, "evidenceRefs"),
                childList(toSnapshot, "evidenceRefs"),
                this::evidenceKey,
                List.of("sourceType", "sourceRefId", "evidenceSummary", "evidenceUrl")
        );

        vo.setReportFieldsChanged(reportChanges);
        vo.setReviewFieldsChanged(reviewChanges);
        vo.setSectionsAdded(sectionDiff.added());
        vo.setSectionsRemoved(sectionDiff.removed());
        vo.setSectionsChanged(sectionDiff.changed());
        vo.setEvidenceRefsAdded(evidenceDiff.added());
        vo.setEvidenceRefsRemoved(evidenceDiff.removed());
        vo.setEvidenceRefsChanged(evidenceDiff.changed());
        vo.setChanged(!(reportChanges.isEmpty()
                && reviewChanges.isEmpty()
                && sectionDiff.isEmpty()
                && evidenceDiff.isEmpty()));
        return vo;
    }

    private ReportVersionCompareVO.VersionSummary toSummary(ReportVersionVO version) {
        ReportVersionCompareVO.VersionSummary summary = new ReportVersionCompareVO.VersionSummary();
        summary.setVersionId(version.getVersionId());
        summary.setVersionNo(defaultVersionNo(version.getVersionNo()));
        summary.setSnapshotSource(version.getSnapshotSource());
        summary.setCreatedAt(version.getCreatedAt());
        return summary;
    }

    private List<ReportVersionCompareVO.FieldChange> diffFields(String path,
                                                               Map<String, Object> from,
                                                               Map<String, Object> to,
                                                               List<String> fields) {
        List<ReportVersionCompareVO.FieldChange> changes = new ArrayList<>();
        for (String field : fields) {
            Object fromValue = from.get(field);
            Object toValue = to.get(field);
            if (!canonicalEquals(fromValue, toValue)) {
                changes.add(fieldChange(path + "." + field, field, fromValue, toValue));
            }
        }
        return changes;
    }

    private DiffResult diffItemList(String path,
                                    List<Map<String, Object>> fromItems,
                                    List<Map<String, Object>> toItems,
                                    Function<Map<String, Object>, String> keyResolver,
                                    List<String> fields) {
        Map<String, Map<String, Object>> fromByKey = fromItems.stream()
                .collect(Collectors.toMap(keyResolver, Function.identity(), (first, second) -> first, LinkedHashMap::new));
        Map<String, Map<String, Object>> toByKey = toItems.stream()
                .collect(Collectors.toMap(keyResolver, Function.identity(), (first, second) -> first, LinkedHashMap::new));

        List<ReportVersionCompareVO.ItemChange> added = new ArrayList<>();
        List<ReportVersionCompareVO.ItemChange> removed = new ArrayList<>();
        List<ReportVersionCompareVO.FieldChange> changed = new ArrayList<>();

        for (Map.Entry<String, Map<String, Object>> entry : toByKey.entrySet()) {
            if (!fromByKey.containsKey(entry.getKey())) {
                added.add(itemChange(entry.getKey(), entry.getValue()));
            }
        }
        for (Map.Entry<String, Map<String, Object>> entry : fromByKey.entrySet()) {
            if (!toByKey.containsKey(entry.getKey())) {
                removed.add(itemChange(entry.getKey(), entry.getValue()));
            }
        }
        for (Map.Entry<String, Map<String, Object>> entry : fromByKey.entrySet()) {
            Map<String, Object> toItem = toByKey.get(entry.getKey());
            if (toItem != null) {
                changed.addAll(diffFields(path + "[" + entry.getKey() + "]", entry.getValue(), toItem, fields));
            }
        }
        return new DiffResult(added, removed, changed);
    }

    private ReportVersionCompareVO.FieldChange fieldChange(String path, String field, Object fromValue, Object toValue) {
        ReportVersionCompareVO.FieldChange change = new ReportVersionCompareVO.FieldChange();
        change.setPath(path);
        change.setField(field);
        change.setFromValue(fromValue);
        change.setToValue(toValue);
        return change;
    }

    private ReportVersionCompareVO.ItemChange itemChange(String key, Object value) {
        ReportVersionCompareVO.ItemChange change = new ReportVersionCompareVO.ItemChange();
        change.setKey(key);
        change.setValue(value);
        return change;
    }

    private String sectionKey(Map<String, Object> item) {
        return firstNonBlank(stringValue(item.get("sectionCode")), stringValue(item.get("sectionId")), canonicalValue(item));
    }

    private String evidenceKey(Map<String, Object> item) {
        return firstNonBlank(stringValue(item.get("sourceRefId")), stringValue(item.get("evidenceId")), canonicalValue(item));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> childMap(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> childList(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                result.add((Map<String, Object>) map);
            }
        }
        return result;
    }

    private Map<String, Object> safeMap(Map<String, Object> data) {
        return data == null ? Map.of() : data;
    }

    private boolean canonicalEquals(Object fromValue, Object toValue) {
        return Objects.equals(canonicalValue(fromValue), canonicalValue(toValue));
    }

    private String canonicalValue(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private String firstNonBlank(String first, String second) {
        return firstNonBlank(first, second, null);
    }

    private String firstNonBlank(String first, String second, String third) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return third;
    }

    private int defaultVersionNo(Integer versionNo) {
        return versionNo == null || versionNo < 1 ? 1 : versionNo;
    }

    private record DiffResult(List<ReportVersionCompareVO.ItemChange> added,
                              List<ReportVersionCompareVO.ItemChange> removed,
                              List<ReportVersionCompareVO.FieldChange> changed) {
        private boolean isEmpty() {
            return added.isEmpty() && removed.isEmpty() && changed.isEmpty();
        }
    }
}
