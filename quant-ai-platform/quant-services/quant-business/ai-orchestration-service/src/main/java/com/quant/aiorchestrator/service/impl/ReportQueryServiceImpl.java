package com.quant.aiorchestrator.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.dto.*;
import com.quant.aiorchestrator.domain.entity.*;
import com.quant.aiorchestrator.domain.vo.*;
import com.quant.aiorchestrator.mapper.*;
import com.quant.aiorchestrator.service.ReportQueryService;
import com.quant.aiorchestrator.service.ReportVersionService;
import com.quant.aiorchestrator.service.TaskReportService;
import com.quant.aiorchestrator.util.CacheKeyUtil;
import com.quant.common.core.exception.BizException;
import com.quant.common.model.enums.MarketIntelligenceTypeEnum;
import com.quant.common.model.enums.ReportReviewStatusEnum;
import com.quant.common.model.enums.RiskLevelEnum;
import com.quant.common.model.enums.SignalDirectionEnum;
import com.quant.common.model.enums.SignalStrengthEnum;
import com.quant.common.model.enums.TaskStatusEnum;
import com.quant.common.redis.RedisKeyConstants;
import com.quant.common.redis.RedisKeyBuilder;
import com.quant.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportQueryServiceImpl implements ReportQueryService {

    private final ResearchTaskMapper researchTaskMapper;
    private final ResearchReportMapper researchReportMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final RiskWarningMapper riskWarningMapper;
    private final RiskWarningDetailMapper riskWarningDetailMapper;
    private final StrategySignalMapper strategySignalMapper;
    private final ReportEvidenceRefMapper reportEvidenceRefMapper;
    private final HumanReviewRecordMapper humanReviewRecordMapper;
    private final ResearchReportSectionMapper researchReportSectionMapper;
    private final TaskReportService taskReportService;
    private final ReportVersionService reportVersionService;

private record RiskProjection(
            boolean needHumanReview,
            int warningCount,
            int riskPointCount,
            int totalRiskCount,
            RiskLevelEnum riskLevel
    ) {}

@Override
    public ReportCenterPageVO pageReportCenter(ReportCenterPageQueryDTO queryDTO) {
        ReportCenterPageQueryDTO safeQuery = queryDTO == null ? new ReportCenterPageQueryDTO() : queryDTO;
        int pageNum = safeQuery.getPageNum() == null || safeQuery.getPageNum() < 1 ? 1 : safeQuery.getPageNum();
        int pageSize = safeQuery.getPageSize() == null || safeQuery.getPageSize() < 1 ? 10 : safeQuery.getPageSize();

        List<ReportCenterListItemVO> matchedRecords = listReportCenterRecords(safeQuery);
        int fromIndex = Math.min((pageNum - 1) * pageSize, matchedRecords.size());
        int toIndex = Math.min(fromIndex + pageSize, matchedRecords.size());

        ReportCenterPageVO vo = new ReportCenterPageVO();
        vo.setTotal((long) matchedRecords.size());
        vo.setPageNum((long) pageNum);
        vo.setPageSize((long) pageSize);
        vo.setRecords(fromIndex >= toIndex ? List.of() : matchedRecords.subList(fromIndex, toIndex));
        return vo;
    }

    @Override
    public ReportCenterStatsVO getReportCenterStats() {
        List<ReportCenterListItemVO> records = listReportCenterRecords(new ReportCenterPageQueryDTO());
        ReportCenterStatsVO vo = new ReportCenterStatsVO();
        vo.setTotalCount((long) records.size());
        vo.setHighConfidenceCount(records.stream().filter(item -> isHighConfidence(item.getConfidenceScore())).count());
        vo.setPendingReviewCount(records.stream().filter(item -> ReportReviewStatusEnum.PENDING.name().equals(item.getReviewStatus())).count());
        vo.setApprovedCount(records.stream().filter(item -> ReportReviewStatusEnum.APPROVED.name().equals(item.getReviewStatus())).count());
        vo.setHumanReviewCount(records.stream().filter(item -> Boolean.TRUE.equals(item.getNeedHumanReview())).count());
        return vo;
    }

    private List<ReportCenterListItemVO> listReportCenterRecords(ReportCenterPageQueryDTO queryDTO) {
        List<ResearchReportDO> reports = researchReportMapper.selectList(
                new LambdaQueryWrapper<ResearchReportDO>()
                        .eq(ResearchReportDO::getDeleted, 0)
                        .orderByDesc(ResearchReportDO::getCreatedAt, ResearchReportDO::getId)
        );

        if (reports.isEmpty()) {
            return List.of();
        }

        Set<String> taskIds = reports.stream()
                .map(ResearchReportDO::getTaskId)
                .filter(taskId -> taskId != null && !taskId.isBlank())
                .collect(Collectors.toSet());

        if (taskIds.isEmpty()) {
            return List.of();
        }

        Map<String, ResearchTaskDO> taskMap = researchTaskMapper.selectList(
                new LambdaQueryWrapper<ResearchTaskDO>()
                        .eq(ResearchTaskDO::getDeleted, 0)
                        .in(ResearchTaskDO::getTaskId, taskIds)
        ).stream().collect(Collectors.toMap(
                ResearchTaskDO::getTaskId,
                item -> item,
                (left, right) -> left
        ));
        Map<String, RiskWarningDO> riskWarningMap = loadLatestRiskWarningMapByTaskIds(taskIds);

        return reports.stream()
                .map(report -> toReportCenterItem(
                        report,
                        taskMap.get(report.getTaskId()),
                        riskWarningMap.get(report.getTaskId())
                ))
                .filter(Objects::nonNull)
                .filter(item -> matchesReportCenterQuery(item, queryDTO))
                .sorted(Comparator
                        .comparing(ReportCenterListItemVO::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ReportCenterListItemVO::getConfidenceScore, Comparator.nullsLast(Comparator.reverseOrder()))
                )
                .toList();
    }

    private ReportCenterListItemVO toReportCenterItem(ResearchReportDO report,
                                                      ResearchTaskDO task,
                                                      RiskWarningDO warning) {
        if (report == null || task == null) {
            return null;
        }

        String summary = resolveReportCenterSummary(report);
        String reportType = resolveReportType(report, task);

        if ((summary == null || summary.isBlank())
                && (report.getResultRef() == null || report.getResultRef().isBlank())
                && (reportType == null || reportType.isBlank())) {
            return null;
        }

        ReportReviewStatusEnum reviewStatus = resolveReviewStatus(report.getReviewStatus());
        boolean needHumanReview = resolveRiskProjection(report, warning).needHumanReview();

        ReportCenterListItemVO vo = new ReportCenterListItemVO();
        vo.setTaskId(task.getTaskId());
        vo.setTaskTitle(task.getTaskTitle());
        vo.setTaskType(task.getTaskType());
        vo.setTargetCode(task.getTargetCode());
        vo.setTargetName(task.getTargetName());
        vo.setPriority(task.getPriority());
        vo.setReportId(report.getReportId());
        vo.setReportType(reportType);
        vo.setFinalStatus(report.getFinalStatus());
        vo.setConfidenceScore(report.getConfidenceScore() == null ? null : report.getConfidenceScore().doubleValue());
        vo.setNeedHumanReview(needHumanReview);
        vo.setReviewStatus(reviewStatus.name());
        vo.setReviewedBy(report.getReviewedBy());
        vo.setReviewedAt(report.getReviewedAt());
        vo.setRevised(isReportRevised(report));
        vo.setSummaryRevised(isSummaryRevised(report));
        vo.setHighlightsRevised(isHighlightsRevised(report));
        vo.setRiskPointsRevised(isRiskPointsRevised(report));
        vo.setSummary(summary);
        vo.setCreatedAt(firstNonNull(report.getCreatedAt(), task.getCreatedAt()));
        return vo;
    }

    private boolean matchesReportCenterQuery(ReportCenterListItemVO item, ReportCenterPageQueryDTO queryDTO) {
        if (item == null) {
            return false;
        }
        if (queryDTO == null) {
            return true;
        }
        if (queryDTO.getTargetCode() != null && !queryDTO.getTargetCode().isBlank()
                && !queryDTO.getTargetCode().equalsIgnoreCase(item.getTargetCode())) {
            return false;
        }
        if (queryDTO.getTargetName() != null && !queryDTO.getTargetName().isBlank()
                && !containsIgnoreCase(item.getTargetName(), queryDTO.getTargetName())) {
            return false;
        }
        if (queryDTO.getReportType() != null && !queryDTO.getReportType().isBlank()
                && !queryDTO.getReportType().equalsIgnoreCase(item.getReportType())) {
            return false;
        }
        ReportReviewStatusEnum reviewStatus = ReportReviewStatusEnum.from(queryDTO.getReviewStatus());
        if (reviewStatus != null && !reviewStatus.name().equals(item.getReviewStatus())) {
            return false;
        }
        if (Boolean.TRUE.equals(queryDTO.getOnlyHighConfidence()) && !isHighConfidence(item.getConfidenceScore())) {
            return false;
        }
        if (queryDTO.getNeedHumanReview() != null
                && !queryDTO.getNeedHumanReview().equals(item.getNeedHumanReview())) {
            return false;
        }
        return true;
    }

    private String resolveReportCenterSummary(ResearchReportDO report) {
        return resolveDisplaySummary(report.getRevisedSummary(), report.getSummary());
    }

    private boolean isReportRevised(ResearchReportDO report) {
        return isSummaryRevised(report) || isHighlightsRevised(report) || isRiskPointsRevised(report);
    }

    private boolean isSummaryRevised(ResearchReportDO report) {
        if (report == null) {
            return false;
        }
        return !Objects.equals(
                normalizeText(report.getSummary()),
                resolveDisplaySummary(report.getRevisedSummary(), report.getSummary())
        );
    }

    private boolean isHighlightsRevised(ResearchReportDO report) {
        if (report == null) {
            return false;
        }
        return !readTextList(report.getHighlights()).equals(
                readPreferredTextList(report.getRevisedHighlights(), report.getHighlights())
        );
    }

    private boolean isRiskPointsRevised(ResearchReportDO report) {
        if (report == null) {
            return false;
        }
        return !readTextList(report.getRiskPoints()).equals(
                readPreferredTextList(report.getRevisedRiskPoints(), report.getRiskPoints())
        );
    }

    private List<String> buildRiskSourceTags(List<String> warningList,
                                             List<String> riskPointList,
                                             boolean needHumanReview,
                                             ReportReviewStatusEnum reviewStatus) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        if (warningList != null && !warningList.isEmpty()) {
            tags.add("WARNING_SIGNAL");
        }
        if (riskPointList != null && !riskPointList.isEmpty()) {
            tags.add("REPORT_RISK_POINT");
        }
        if (needHumanReview) {
            tags.add("HUMAN_REVIEW");
        }
        if (reviewStatus == ReportReviewStatusEnum.REJECTED) {
            tags.add("REVIEW_REJECTED");
        }
        return new ArrayList<>(tags);
    }

    private String resolveReportType(ResearchReportDO report, ResearchTaskDO task) {
        if (report.getReportType() != null && !report.getReportType().isBlank()) {
            return report.getReportType().trim();
        }
        return task == null ? null : task.getTaskType();
    }

@Override
    public TaskReportVO getTaskReportOnly(String taskId) {
        String cacheKey = RedisKeyBuilder.taskResult(taskId);
        String cache = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cache != null && !cache.isBlank()) {
            try {
                TaskReportVO cached = objectMapper.readValue(cache, TaskReportVO.class);
                boolean upgraded = hydrateTaskReportContextFields(cached);
                upgraded = hydrateTaskReportDomainFields(cached) || upgraded;
                if (isCurrentTaskReportCache(cached)) {
                    if (upgraded) {
                        try {
                            stringRedisTemplate.opsForValue().set(
                                    cacheKey,
                                    objectMapper.writeValueAsString(cached),
                                    java.time.Duration.ofHours(12)
                            );
                        } catch (Exception ignored) {
                        }
                    }
                    return cached;
                }
            } catch (Exception ignored) {
            }
        }

        ResearchReportDO report = researchReportMapper.selectOne(
                new LambdaQueryWrapper<ResearchReportDO>()
                        .eq(ResearchReportDO::getTaskId, taskId)
                        .eq(ResearchReportDO::getDeleted, 0)
                        .last("limit 1")
        );
        if (report == null) {
            return null;
        }

        RiskWarningDO warning = loadLatestRiskWarningMapByTaskIds(Set.of(taskId)).get(taskId);
        List<RiskWarningDetailDO> warningDetails = warning == null
                ? List.of()
                : loadRiskWarningDetailMapByWarningIds(Set.of(warning.getWarningId()))
                .getOrDefault(warning.getWarningId(), List.of());

        TaskReportVO vo = toTaskReportVO(report, warning, warningDetails);
        try {
            stringRedisTemplate.opsForValue().set(
                    cacheKey,
                    objectMapper.writeValueAsString(vo),
                    java.time.Duration.ofHours(12)
            );
        } catch (Exception ignored) {
        }
        return vo;
    }

    private TaskReportVO toTaskReportVO(ResearchReportDO report,
                                        RiskWarningDO warning,
                                        List<RiskWarningDetailDO> warningDetails) {
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
                : isDomainRiskHumanReview(warning));
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
        hydrateTaskReportContextFields(vo);
        hydrateTaskReportDomainFields(vo);

        return vo;
    }

    private boolean hydrateTaskReportDomainFields(TaskReportVO report) {
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

        List<HumanReviewRecordDO> reviewRecords = humanReviewRecordMapper.selectList(
                new LambdaQueryWrapper<HumanReviewRecordDO>()
                        .eq(HumanReviewRecordDO::getRelatedObjectType, "REPORT")
                        .eq(HumanReviewRecordDO::getRelatedObjectId, report.getReportId())
                        .eq(HumanReviewRecordDO::getDeleted, 0)
                        .orderByDesc(HumanReviewRecordDO::getId)
        );
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

    private TaskReportVO.HumanReviewRecordVO toHumanReviewRecord(HumanReviewRecordDO record) {
        TaskReportVO.HumanReviewRecordVO vo = new TaskReportVO.HumanReviewRecordVO();
        vo.setReviewId(record.getReviewId());
        vo.setReviewerId(record.getReviewerId());
        vo.setReviewerRole(record.getReviewerRole());
        vo.setReviewResult(record.getReviewResult());
        vo.setReviewComment(record.getReviewComment());
        vo.setBeforeSnapshotRef(record.getBeforeSnapshotRef());
        vo.setAfterSnapshotRef(record.getAfterSnapshotRef());
        vo.setBeforeSnapshot(record.getBeforeSnapshot());
        vo.setAfterSnapshot(record.getAfterSnapshot());
        vo.setTraceId(record.getTraceId());
        vo.setCreatedAt(record.getCreatedAt() == null ? null : record.getCreatedAt().toString());
        return vo;
    }

    private List<String> buildDomainRiskWarningMessages(RiskWarningDO warning) {
        LinkedHashSet<String> messages = new LinkedHashSet<>();
        if (warning == null) {
            return List.of();
        }
        String summary = normalizeText(warning.getWarningSummary());
        if (summary != null) {
            messages.add(summary);
        }
        String reason = normalizeText(warning.getWarningReason());
        if (reason != null) {
            for (String item : reason.split("\\R")) {
                if (item != null && !item.isBlank()) {
                    messages.add(item.trim());
                }
            }
        }
        return new ArrayList<>(messages);
    }

    private boolean isCurrentTaskReportCache(TaskReportVO cached) {
        return cached != null && cached.getReportId() != null && !cached.getReportId().isBlank();
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

    private boolean hydrateTaskReportContextFields(TaskReportVO report) {
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

private ReportReviewStatusEnum resolveReviewStatus(String reviewStatus) {
        ReportReviewStatusEnum resolved = ReportReviewStatusEnum.from(reviewStatus);
        return resolved == null ? ReportReviewStatusEnum.PENDING : resolved;
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

private int riskLevelRank(String riskLevel) {
        RiskLevelEnum resolved = RiskLevelEnum.from(riskLevel);
        if (resolved == null) {
            return 0;
        }
        return switch (resolved) {
            case HIGH -> 3;
            case MEDIUM -> 2;
            case LOW -> 1;
        };
    }

private boolean containsIgnoreCase(String source, String target) {
        return source != null && target != null && source.toLowerCase().contains(target.toLowerCase());
    }

private LocalDateTime firstNonNull(LocalDateTime left, LocalDateTime right) {
        return left != null ? left : right;
    }

private Map<String, ResearchTaskDO> loadTaskMap(Set<String> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return researchTaskMapper.selectList(
                new LambdaQueryWrapper<ResearchTaskDO>()
                        .eq(ResearchTaskDO::getDeleted, 0)
                        .in(ResearchTaskDO::getTaskId, taskIds)
        ).stream().collect(Collectors.toMap(
                ResearchTaskDO::getTaskId,
                item -> item,
                (left, right) -> left
        ));
    }

private Map<String, ResearchReportDO> loadReportMapByTaskIds(Set<String> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return researchReportMapper.selectList(
                new LambdaQueryWrapper<ResearchReportDO>()
                        .eq(ResearchReportDO::getDeleted, 0)
                        .in(ResearchReportDO::getTaskId, taskIds)
                        .orderByDesc(ResearchReportDO::getCreatedAt, ResearchReportDO::getId)
        ).stream().collect(Collectors.toMap(
                ResearchReportDO::getTaskId,
                item -> item,
                (left, right) -> left
        ));
    }

private Map<String, RiskWarningDO> loadLatestRiskWarningMapByTaskIds(Set<String> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return riskWarningMapper.selectList(
                new LambdaQueryWrapper<RiskWarningDO>()
                        .eq(RiskWarningDO::getDeleted, 0)
                        .in(RiskWarningDO::getTaskId, taskIds)
                        .orderByDesc(RiskWarningDO::getCreatedAt, RiskWarningDO::getId)
        ).stream().collect(Collectors.toMap(
                RiskWarningDO::getTaskId,
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new
        ));
    }

private Map<String, StrategySignalDO> loadLatestStrategySignalMapByTaskIds(Set<String> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<StrategySignalDO> signals = strategySignalMapper.selectList(
                new LambdaQueryWrapper<StrategySignalDO>()
                        .eq(StrategySignalDO::getDeleted, 0)
                        .in(StrategySignalDO::getTaskId, taskIds)
                        .orderByDesc(StrategySignalDO::getSignalDate, StrategySignalDO::getCreatedAt, StrategySignalDO::getId)
        );
        if (signals == null || signals.isEmpty()) {
            return Collections.emptyMap();
        }
        return signals.stream()
                .filter(item -> item.getTaskId() != null && !item.getTaskId().isBlank())
                .collect(Collectors.toMap(
                        StrategySignalDO::getTaskId,
                        item -> item,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

private Map<String, List<RiskWarningDetailDO>> loadRiskWarningDetailMapByWarningIds(Set<String> warningIds) {
        if (warningIds == null || warningIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return riskWarningDetailMapper.selectList(
                new LambdaQueryWrapper<RiskWarningDetailDO>()
                        .eq(RiskWarningDetailDO::getDeleted, 0)
                        .in(RiskWarningDetailDO::getWarningId, warningIds)
                        .orderByAsc(RiskWarningDetailDO::getId)
        ).stream().collect(Collectors.groupingBy(RiskWarningDetailDO::getWarningId, LinkedHashMap::new, Collectors.toList()));
    }

private RiskProjection resolveRiskProjection(ResearchReportDO report,
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

private RiskProjection resolveRiskProjection(ResearchReportDO report, RiskWarningDO warning) {
        return resolveRiskProjection(report, warning, List.of());
    }

private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

private int defaultVersionNo(Integer versionNo) {
        return versionNo == null || versionNo < 1 ? 1 : versionNo;
    }

private boolean isHighConfidence(Double confidenceScore) {
        return confidenceScore != null && confidenceScore >= 0.8D;
    }

@Override
    public List<TaskReportReviewLogVO> listReviewLogs(String taskId) {
        return taskReportService.listReviewLogs(taskId);
    }

    @Override
    public List<ReportVersionVO> listReportVersions(String taskId) {
        return reportVersionService.listVersions(taskId);
    }

    @Override
    public ReportVersionVO getReportVersion(String taskId, Integer versionNo) {
        return reportVersionService.getVersion(taskId, versionNo);
    }

    @Override
    public ReportVersionCompareVO compareReportVersions(String taskId, Integer fromVersionNo, Integer toVersionNo) {
        return reportVersionService.compareVersions(taskId, fromVersionNo, toVersionNo);
    }

@Override
    public ReportReviewStatsVO getReportReviewStats() {
        ReportReviewStatsVO vo = new ReportReviewStatsVO();

        vo.setPendingCount(researchReportMapper.selectCount(
                new LambdaQueryWrapper<ResearchReportDO>()
                        .eq(ResearchReportDO::getDeleted, 0)
                        .and(wrapper -> wrapper
                                .isNull(ResearchReportDO::getReviewStatus)
                                .or()
                                .eq(ResearchReportDO::getReviewStatus, ReportReviewStatusEnum.PENDING.name()))
        ));

        vo.setApprovedCount(researchReportMapper.selectCount(
                new LambdaQueryWrapper<ResearchReportDO>()
                        .eq(ResearchReportDO::getDeleted, 0)
                        .eq(ResearchReportDO::getReviewStatus, ReportReviewStatusEnum.APPROVED.name())
        ));

        vo.setRejectedCount(researchReportMapper.selectCount(
                new LambdaQueryWrapper<ResearchReportDO>()
                        .eq(ResearchReportDO::getDeleted, 0)
                        .eq(ResearchReportDO::getReviewStatus, ReportReviewStatusEnum.REJECTED.name())
        ));

        vo.setTotalReportCount(researchReportMapper.selectCount(
                new LambdaQueryWrapper<ResearchReportDO>()
                        .eq(ResearchReportDO::getDeleted, 0)
        ));

        return vo;
    }
}
