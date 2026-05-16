package com.quant.aiorchestrator.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.entity.ReportEvidenceRefDO;
import com.quant.aiorchestrator.domain.entity.ResearchReportDO;
import com.quant.aiorchestrator.domain.entity.ResearchReportSectionDO;
import com.quant.aiorchestrator.domain.entity.ResearchReportVersionDO;
import com.quant.aiorchestrator.domain.vo.ReportVersionCompareVO;
import com.quant.aiorchestrator.domain.vo.ReportVersionVO;
import com.quant.aiorchestrator.mapper.ReportEvidenceRefMapper;
import com.quant.aiorchestrator.mapper.ResearchReportSectionMapper;
import com.quant.aiorchestrator.mapper.ResearchReportVersionMapper;
import com.quant.aiorchestrator.service.ReportVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportVersionServiceImpl implements ReportVersionService {

    private final ResearchReportVersionMapper researchReportVersionMapper;
    private final ResearchReportSectionMapper researchReportSectionMapper;
    private final ReportEvidenceRefMapper reportEvidenceRefMapper;
    private final ObjectMapper objectMapper;

    @Override
    public void createSnapshot(ResearchReportDO report, String snapshotSource) {
        if (report == null || report.getReportId() == null || report.getTaskId() == null) {
            return;
        }
        int versionNo = defaultVersionNo(report.getVersionNo());
        ResearchReportVersionDO existing = researchReportVersionMapper.selectOne(
                new LambdaQueryWrapper<ResearchReportVersionDO>()
                        .eq(ResearchReportVersionDO::getReportId, report.getReportId())
                        .eq(ResearchReportVersionDO::getVersionNo, versionNo)
                        .eq(ResearchReportVersionDO::getDeleted, 0)
                        .last("limit 1")
        );
        if (existing != null) {
            return;
        }

        ResearchReportVersionDO entity = new ResearchReportVersionDO();
        entity.setVersionId(UUID.randomUUID().toString());
        entity.setReportId(report.getReportId());
        entity.setTaskId(report.getTaskId());
        entity.setVersionNo(versionNo);
        entity.setSnapshotSource(normalizeSnapshotSource(snapshotSource));
        entity.setSnapshotPayload(toJson(buildSnapshot(report, versionNo)));
        entity.setDeleted(0);
        researchReportVersionMapper.insert(entity);
    }

    @Override
    public List<ReportVersionVO> listVersions(String taskId) {
        if (taskId == null || taskId.isBlank()) {
            return List.of();
        }
        return researchReportVersionMapper.selectList(
                new LambdaQueryWrapper<ResearchReportVersionDO>()
                        .eq(ResearchReportVersionDO::getTaskId, taskId)
                        .eq(ResearchReportVersionDO::getDeleted, 0)
                        .orderByDesc(ResearchReportVersionDO::getVersionNo)
                        .orderByDesc(ResearchReportVersionDO::getId)
        ).stream().map(this::toVO).toList();
    }

    @Override
    public ReportVersionVO getVersion(String taskId, Integer versionNo) {
        if (taskId == null || taskId.isBlank() || versionNo == null || versionNo < 1) {
            return null;
        }
        ResearchReportVersionDO entity = selectVersion(taskId, versionNo);
        return entity == null ? null : toVO(entity);
    }

    @Override
    public ReportVersionCompareVO compareVersions(String taskId, Integer fromVersionNo, Integer toVersionNo) {
        if (taskId == null || taskId.isBlank()
                || fromVersionNo == null || fromVersionNo < 1
                || toVersionNo == null || toVersionNo < 1) {
            return null;
        }
        ResearchReportVersionDO fromEntity = selectVersion(taskId, fromVersionNo);
        ResearchReportVersionDO toEntity = selectVersion(taskId, toVersionNo);
        if (fromEntity == null || toEntity == null) {
            return null;
        }

        ReportVersionVO from = toVO(fromEntity);
        ReportVersionVO to = toVO(toEntity);
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

    private ResearchReportVersionDO selectVersion(String taskId, Integer versionNo) {
        return researchReportVersionMapper.selectOne(
                new LambdaQueryWrapper<ResearchReportVersionDO>()
                        .eq(ResearchReportVersionDO::getTaskId, taskId)
                        .eq(ResearchReportVersionDO::getVersionNo, versionNo)
                        .eq(ResearchReportVersionDO::getDeleted, 0)
                        .last("limit 1")
        );
    }

    private Map<String, Object> buildSnapshot(ResearchReportDO report, int versionNo) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("versionNo", versionNo);
        snapshot.put("report", buildReportSnapshot(report, versionNo));
        snapshot.put("sections", buildSectionSnapshots(report.getReportId()));
        snapshot.put("evidenceRefs", buildEvidenceSnapshots(report.getReportId()));
        return snapshot;
    }

    private Map<String, Object> buildReportSnapshot(ResearchReportDO report, int versionNo) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("reportId", report.getReportId());
        data.put("taskId", report.getTaskId());
        data.put("versionNo", versionNo);
        data.put("taskType", report.getTaskType());
        data.put("finalStatus", report.getFinalStatus());
        data.put("summary", report.getSummary());
        data.put("confidenceScore", report.getConfidenceScore());
        data.put("needHumanReview", report.getNeedHumanReview());
        data.put("reportType", report.getReportType());
        data.put("highlights", readJsonValue(report.getHighlights()));
        data.put("riskPoints", readJsonValue(report.getRiskPoints()));
        data.put("riskWarnings", readJsonValue(report.getRiskWarnings()));
        data.put("resultRef", report.getResultRef());
        data.put("rawPayload", readJsonValue(report.getRawPayload()));
        data.put("reviewStatus", report.getReviewStatus());
        data.put("reviewedBy", report.getReviewedBy());
        data.put("reviewedAt", report.getReviewedAt());
        data.put("revisedSummary", report.getRevisedSummary());
        data.put("revisedHighlights", readJsonValue(report.getRevisedHighlights()));
        data.put("revisedRiskPoints", readJsonValue(report.getRevisedRiskPoints()));
        data.put("reviewComment", report.getReviewComment());
        return data;
    }

    private List<Map<String, Object>> buildSectionSnapshots(String reportId) {
        return researchReportSectionMapper.selectList(
                new LambdaQueryWrapper<ResearchReportSectionDO>()
                        .eq(ResearchReportSectionDO::getReportId, reportId)
                        .eq(ResearchReportSectionDO::getDeleted, 0)
                        .orderByAsc(ResearchReportSectionDO::getSectionOrder)
                        .orderByAsc(ResearchReportSectionDO::getId)
        ).stream().map(section -> {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("sectionId", section.getSectionId());
            data.put("versionNo", defaultVersionNo(section.getVersionNo()));
            data.put("sectionCode", section.getSectionCode());
            data.put("sectionTitle", section.getSectionTitle());
            data.put("sectionOrder", section.getSectionOrder());
            data.put("sectionContent", section.getSectionContent());
            data.put("sectionItems", readJsonValue(section.getSectionItems()));
            data.put("revisedContent", section.getRevisedContent());
            data.put("revisedItems", readJsonValue(section.getRevisedItems()));
            data.put("reviewStatus", section.getReviewStatus());
            data.put("reviewedBy", section.getReviewedBy());
            data.put("reviewedAt", section.getReviewedAt());
            data.put("reviewComment", section.getReviewComment());
            data.put("confidenceScore", section.getConfidenceScore());
            return data;
        }).toList();
    }

    private List<Map<String, Object>> buildEvidenceSnapshots(String reportId) {
        return reportEvidenceRefMapper.selectList(
                new LambdaQueryWrapper<ReportEvidenceRefDO>()
                        .eq(ReportEvidenceRefDO::getReportId, reportId)
                        .eq(ReportEvidenceRefDO::getDeleted, 0)
                        .orderByAsc(ReportEvidenceRefDO::getId)
        ).stream().map(ref -> {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("evidenceId", ref.getEvidenceId());
            data.put("sourceType", ref.getSourceType());
            data.put("sourceRefId", ref.getSourceRefId());
            data.put("evidenceSummary", ref.getEvidenceSummary());
            data.put("evidenceUrl", ref.getEvidenceUrl());
            return data;
        }).toList();
    }

    private ReportVersionVO toVO(ResearchReportVersionDO entity) {
        ReportVersionVO vo = new ReportVersionVO();
        vo.setVersionId(entity.getVersionId());
        vo.setReportId(entity.getReportId());
        vo.setTaskId(entity.getTaskId());
        vo.setVersionNo(defaultVersionNo(entity.getVersionNo()));
        vo.setSnapshotSource(entity.getSnapshotSource());
        vo.setSnapshot(readSnapshot(entity.getSnapshotPayload()));
        vo.setCreatedAt(entity.getCreatedAt() == null ? null : entity.getCreatedAt().toString());
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

    private record DiffResult(List<ReportVersionCompareVO.ItemChange> added,
                              List<ReportVersionCompareVO.ItemChange> removed,
                              List<ReportVersionCompareVO.FieldChange> changed) {
        private boolean isEmpty() {
            return added.isEmpty() && removed.isEmpty() && changed.isEmpty();
        }
    }

    private Map<String, Object> readSnapshot(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private Object readJsonValue(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            return json;
        }
    }

    private String toJson(Map<String, Object> snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String normalizeSnapshotSource(String snapshotSource) {
        if (snapshotSource == null || snapshotSource.isBlank()) {
            return "UNKNOWN";
        }
        return snapshotSource.length() <= 32 ? snapshotSource : snapshotSource.substring(0, 32);
    }

    private int defaultVersionNo(Integer versionNo) {
        return versionNo == null || versionNo < 1 ? 1 : versionNo;
    }
}
