package com.quant.aiorchestrator.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.entity.ReportEvidenceRefDO;
import com.quant.aiorchestrator.domain.entity.ResearchReportDO;
import com.quant.aiorchestrator.domain.entity.ResearchReportSectionDO;
import com.quant.aiorchestrator.domain.entity.ResearchReportVersionDO;
import com.quant.aiorchestrator.domain.vo.ReportVersionVO;
import com.quant.aiorchestrator.mapper.ReportEvidenceRefMapper;
import com.quant.aiorchestrator.mapper.ResearchReportSectionMapper;
import com.quant.aiorchestrator.mapper.ResearchReportVersionMapper;
import com.quant.aiorchestrator.service.ReportVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
        ResearchReportVersionDO entity = researchReportVersionMapper.selectOne(
                new LambdaQueryWrapper<ResearchReportVersionDO>()
                        .eq(ResearchReportVersionDO::getTaskId, taskId)
                        .eq(ResearchReportVersionDO::getVersionNo, versionNo)
                        .eq(ResearchReportVersionDO::getDeleted, 0)
                        .last("limit 1")
        );
        return entity == null ? null : toVO(entity);
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
