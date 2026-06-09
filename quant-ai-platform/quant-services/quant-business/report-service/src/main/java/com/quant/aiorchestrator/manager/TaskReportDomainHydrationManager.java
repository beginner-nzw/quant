package com.quant.aiorchestrator.manager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quant.aiorchestrator.audit.ReportHumanReviewRecordProjection;
import com.quant.aiorchestrator.audit.ReportHumanReviewRecordReadPort;
import com.quant.aiorchestrator.domain.entity.ReportEvidenceRefDO;
import com.quant.aiorchestrator.domain.entity.ResearchReportSectionDO;
import com.quant.aiorchestrator.domain.vo.TaskReportVO;
import com.quant.aiorchestrator.mapper.ReportEvidenceRefMapper;
import com.quant.aiorchestrator.mapper.ResearchReportSectionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class TaskReportDomainHydrationManager {

    private final ReportEvidenceRefMapper reportEvidenceRefMapper;
    private final ReportHumanReviewRecordReadPort humanReviewRecordReadPort;
    private final ResearchReportSectionMapper researchReportSectionMapper;
    private final ObjectMapper objectMapper;

    public boolean hydrateTaskReportDomainFields(TaskReportVO report) {
        if (report == null || !hasText(report.getReportId())) {
            return false;
        }

        boolean changed = false;
        List<ResearchReportSectionDO> sections = researchReportSectionMapper.selectList(
                new LambdaQueryWrapper<ResearchReportSectionDO>()
                        .eq(ResearchReportSectionDO::getReportId, report.getReportId())
                        .eq(ResearchReportSectionDO::getDeleted, 0)
                        .orderByAsc(ResearchReportSectionDO::getSectionOrder)
                        .orderByAsc(ResearchReportSectionDO::getId)
        );
        sections = sections == null ? List.of() : sections;
        if (!sections.isEmpty()) {
            List<TaskReportVO.ReportSectionVO> sectionItems = sections.stream()
                    .map(this::toReportSection)
                    .toList();
            if (!Objects.equals(report.getSections(), sectionItems)) {
                report.setSections(sectionItems);
                changed = true;
            }
        }

        List<ReportEvidenceRefDO> evidenceRefs = reportEvidenceRefMapper.selectList(
                new LambdaQueryWrapper<ReportEvidenceRefDO>()
                        .eq(ReportEvidenceRefDO::getReportId, report.getReportId())
                        .eq(ReportEvidenceRefDO::getDeleted, 0)
                        .orderByAsc(ReportEvidenceRefDO::getId)
        );
        evidenceRefs = evidenceRefs == null ? List.of() : evidenceRefs;
        if (!evidenceRefs.isEmpty()) {
            List<TaskReportVO.ReportEvidenceItemVO> domainEvidenceItems = evidenceRefs.stream()
                    .map(this::toReportEvidenceItem)
                    .toList();
            List<TaskReportVO.ReportEvidenceItemVO> mergedEvidenceItems = mergeEvidenceItems(
                    domainEvidenceItems,
                    report.getEvidenceItems()
            );
            if (!Objects.equals(report.getEvidenceItems(), mergedEvidenceItems)) {
                report.setEvidenceItems(mergedEvidenceItems);
                changed = true;
            }

            List<String> domainRefs = evidenceRefs.stream()
                    .map(this::toEvidenceRefText)
                    .filter(Objects::nonNull)
                    .toList();
            List<String> mergedRefs = mergeTextRefs(domainRefs, report.getEvidenceRefs());
            if (!Objects.equals(report.getEvidenceRefs(), mergedRefs)) {
                report.setEvidenceRefs(mergedRefs);
                changed = true;
            }
        }

        List<ReportHumanReviewRecordProjection> reviewRecords = humanReviewRecordReadPort.listReportReviewRecords(report.getReportId());
        reviewRecords = reviewRecords == null ? List.of() : reviewRecords;
        List<TaskReportVO.HumanReviewRecordVO> humanReviews = reviewRecords.stream()
                .map(this::toHumanReviewRecord)
                .toList();
        if (!Objects.equals(report.getHumanReviewRecords(), humanReviews)) {
            report.setHumanReviewRecords(humanReviews);
            changed = true;
        }
        return changed;
    }

    private TaskReportVO.ReportSectionVO toReportSection(ResearchReportSectionDO section) {
        TaskReportVO.ReportSectionVO vo = new TaskReportVO.ReportSectionVO();
        vo.setSectionId(section.getSectionId());
        vo.setVersionNo(defaultVersionNo(section.getVersionNo()));
        vo.setSectionCode(section.getSectionCode());
        vo.setSectionTitle(section.getSectionTitle());
        vo.setSectionOrder(section.getSectionOrder());
        vo.setSectionContent(section.getSectionContent());
        vo.setSectionItems(readTextList(section.getSectionItems()));
        vo.setRevisedContent(section.getRevisedContent());
        vo.setRevisedItems(readTextList(section.getRevisedItems()));
        vo.setDisplayContent(resolveDisplaySummary(section.getRevisedContent(), section.getSectionContent()));
        vo.setDisplayItems(resolveDisplayList(vo.getRevisedItems(), vo.getSectionItems()));
        vo.setReviewStatus(section.getReviewStatus());
        vo.setReviewedBy(section.getReviewedBy());
        vo.setReviewedAt(section.getReviewedAt() == null ? null : section.getReviewedAt().toString());
        vo.setReviewComment(section.getReviewComment());
        vo.setConfidenceScore(section.getConfidenceScore() == null ? null : section.getConfidenceScore().doubleValue());
        return vo;
    }

    private TaskReportVO.ReportEvidenceItemVO toReportEvidenceItem(ReportEvidenceRefDO ref) {
        TaskReportVO.ReportEvidenceItemVO item = new TaskReportVO.ReportEvidenceItemVO();
        item.setEvidenceId(ref.getEvidenceId());
        item.setEvidenceType(ref.getSourceType());
        item.setSource(ref.getSourceType());
        item.setTitle(hasText(ref.getConclusionCode()) ? ref.getConclusionCode() : ref.getSourceRefId());
        item.setSummary(ref.getEvidenceSummary());
        item.setUrl(ref.getEvidenceUrl());
        item.setReferenceId(ref.getSourceRefId());
        item.setRelevance(ref.getConfidenceScore() == null ? null : ref.getConfidenceScore().toPlainString());
        return item;
    }

    private String toEvidenceRefText(ReportEvidenceRefDO ref) {
        String sourceType = normalizeText(ref.getSourceType());
        String sourceRefId = normalizeText(ref.getSourceRefId());
        if (sourceType == null && sourceRefId == null) {
            return null;
        }
        if (sourceType == null) {
            return sourceRefId;
        }
        return sourceType + ":" + (sourceRefId == null ? "" : sourceRefId);
    }

    private List<TaskReportVO.ReportEvidenceItemVO> mergeEvidenceItems(List<TaskReportVO.ReportEvidenceItemVO> preferred,
                                                                       List<TaskReportVO.ReportEvidenceItemVO> fallback) {
        Map<String, TaskReportVO.ReportEvidenceItemVO> merged = new LinkedHashMap<>();
        for (TaskReportVO.ReportEvidenceItemVO item : preferred == null ? List.<TaskReportVO.ReportEvidenceItemVO>of() : preferred) {
            merged.put(evidenceItemKey(item), item);
        }
        for (TaskReportVO.ReportEvidenceItemVO item : fallback == null ? List.<TaskReportVO.ReportEvidenceItemVO>of() : fallback) {
            merged.putIfAbsent(evidenceItemKey(item), item);
        }
        return new ArrayList<>(merged.values());
    }

    private String evidenceItemKey(TaskReportVO.ReportEvidenceItemVO item) {
        String evidenceId = normalizeText(item.getEvidenceId());
        if (evidenceId != null) {
            return evidenceId;
        }
        return normalizeText(item.getTitle()) + "::" + normalizeText(item.getSummary()) + "::" + normalizeText(item.getReferenceId());
    }

    private List<String> mergeTextRefs(List<String> preferred, List<String> fallback) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        for (String item : preferred == null ? List.<String>of() : preferred) {
            String normalized = normalizeText(item);
            if (normalized != null) {
                merged.add(normalized);
            }
        }
        for (String item : fallback == null ? List.<String>of() : fallback) {
            String normalized = normalizeText(item);
            if (normalized != null) {
                merged.add(normalized);
            }
        }
        return new ArrayList<>(merged);
    }

    private TaskReportVO.HumanReviewRecordVO toHumanReviewRecord(ReportHumanReviewRecordProjection record) {
        TaskReportVO.HumanReviewRecordVO vo = new TaskReportVO.HumanReviewRecordVO();
        vo.setReviewId(record.reviewId());
        vo.setReviewerId(record.reviewerId());
        vo.setReviewerRole(record.reviewerRole());
        vo.setReviewResult(record.reviewResult());
        vo.setReviewComment(record.reviewComment());
        vo.setBeforeSnapshotRef(record.beforeSnapshotRef());
        vo.setAfterSnapshotRef(record.afterSnapshotRef());
        vo.setBeforeSnapshot(record.beforeSnapshot());
        vo.setAfterSnapshot(record.afterSnapshot());
        vo.setTraceId(record.traceId());
        vo.setCreatedAt(record.createdAt() == null ? null : record.createdAt().toString());
        return vo;
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

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private int defaultVersionNo(Integer versionNo) {
        return versionNo == null || versionNo < 1 ? 1 : versionNo;
    }
}
