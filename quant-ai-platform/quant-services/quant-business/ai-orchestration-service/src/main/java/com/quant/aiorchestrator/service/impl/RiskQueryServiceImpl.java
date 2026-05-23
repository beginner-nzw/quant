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
import com.quant.aiorchestrator.service.RiskQueryService;
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
public class RiskQueryServiceImpl implements RiskQueryService {

    private final ResearchTaskMapper researchTaskMapper;
    private final ResearchReportMapper researchReportMapper;
    private final RiskWarningMapper riskWarningMapper;
    private final RiskWarningDetailMapper riskWarningDetailMapper;
    private final StrategySignalMapper strategySignalMapper;
    private final ObjectMapper objectMapper;

private record RiskWarningFollowUpSummary(
            String followUpStatus,
            Integer followUpTaskCount,
            String latestFollowUpTaskId,
            String latestFollowUpTaskTitle,
            String latestFollowUpTaskStatus,
            LocalDateTime latestFollowUpCreatedAt
    ) {}

private record RiskProjection(
            boolean needHumanReview,
            int warningCount,
            int riskPointCount,
            int totalRiskCount,
            RiskLevelEnum riskLevel
    ) {}

@Override
    public RiskWarningPageVO pageRiskWarnings(RiskWarningPageQueryDTO queryDTO) {
        RiskWarningPageQueryDTO safeQuery = queryDTO == null ? new RiskWarningPageQueryDTO() : queryDTO;
        int pageNum = safeQuery.getPageNum() == null || safeQuery.getPageNum() < 1 ? 1 : safeQuery.getPageNum();
        int pageSize = safeQuery.getPageSize() == null || safeQuery.getPageSize() < 1 ? 10 : safeQuery.getPageSize();

        List<RiskWarningListItemVO> matchedRecords = listRiskWarningRecords(safeQuery);
        int fromIndex = Math.min((pageNum - 1) * pageSize, matchedRecords.size());
        int toIndex = Math.min(fromIndex + pageSize, matchedRecords.size());

        RiskWarningPageVO vo = new RiskWarningPageVO();
        vo.setTotal((long) matchedRecords.size());
        vo.setPageNum((long) pageNum);
        vo.setPageSize((long) pageSize);
        vo.setRecords(fromIndex >= toIndex ? List.of() : matchedRecords.subList(fromIndex, toIndex));
        return vo;
    }

    @Override
    public RiskWarningStatsVO getRiskWarningStats() {
        List<RiskWarningListItemVO> records = listRiskWarningRecords(new RiskWarningPageQueryDTO());
        RiskWarningStatsVO vo = new RiskWarningStatsVO();
        vo.setTotalCount((long) records.size());
        vo.setHighCount(records.stream().filter(item -> RiskLevelEnum.HIGH.name().equals(item.getRiskLevel())).count());
        vo.setMediumCount(records.stream().filter(item -> RiskLevelEnum.MEDIUM.name().equals(item.getRiskLevel())).count());
        vo.setLowCount(records.stream().filter(item -> RiskLevelEnum.LOW.name().equals(item.getRiskLevel())).count());
        vo.setPendingReviewCount(records.stream().filter(item -> ReportReviewStatusEnum.PENDING.name().equals(item.getReportReviewStatus())).count());
        vo.setHumanReviewCount(records.stream().filter(item -> Boolean.TRUE.equals(item.getNeedHumanReview())).count());
        return vo;
    }

    private List<RiskWarningListItemVO> listRiskWarningRecords(RiskWarningPageQueryDTO queryDTO) {
        List<RiskWarningDO> domainWarnings = loadActiveRiskWarnings();
        if (domainWarnings.isEmpty()) {
            return listRiskWarningRecordsFromReports(queryDTO, Collections.emptySet());
        }

        Set<String> coveredTaskIds = domainWarnings.stream()
                .map(RiskWarningDO::getTaskId)
                .filter(taskId -> taskId != null && !taskId.isBlank())
                .collect(Collectors.toSet());

        List<RiskWarningListItemVO> records = new ArrayList<>(listRiskWarningRecordsFromDomain(domainWarnings, queryDTO));
        records.addAll(listRiskWarningRecordsFromReports(queryDTO, coveredTaskIds));
        return sortRiskWarningRecords(records);
    }

    private List<RiskWarningDO> loadActiveRiskWarnings() {
        return riskWarningMapper.selectList(
                new LambdaQueryWrapper<RiskWarningDO>()
                        .eq(RiskWarningDO::getDeleted, 0)
                        .orderByDesc(RiskWarningDO::getCreatedAt, RiskWarningDO::getId)
        );
    }

    private List<RiskWarningListItemVO> listRiskWarningRecordsFromDomain(List<RiskWarningDO> warnings,
                                                                         RiskWarningPageQueryDTO queryDTO) {
        if (warnings.isEmpty()) {
            return List.of();
        }

        Set<String> taskIds = warnings.stream()
                .map(RiskWarningDO::getTaskId)
                .filter(taskId -> taskId != null && !taskId.isBlank())
                .collect(Collectors.toSet());
        Map<String, ResearchTaskDO> taskMap = loadTaskMap(taskIds);
        Map<String, ResearchReportDO> reportMap = loadReportMapByTaskIds(taskIds);

        List<ResearchTaskDO> followUpTasks = researchTaskMapper.selectList(
                new LambdaQueryWrapper<ResearchTaskDO>()
                        .eq(ResearchTaskDO::getDeleted, 0)
                        .eq(ResearchTaskDO::getSourceDomain, "RISK_WARNING")
        );
        Map<String, List<ResearchTaskDO>> followUpTaskMapBySourceTaskId = followUpTasks.stream()
                .filter(item -> item.getSourceTaskId() != null && !item.getSourceTaskId().isBlank())
                .collect(Collectors.groupingBy(ResearchTaskDO::getSourceTaskId));
        Map<String, List<ResearchTaskDO>> followUpTaskMapBySourceReportId = followUpTasks.stream()
                .filter(item -> item.getSourceReportId() != null && !item.getSourceReportId().isBlank())
                .collect(Collectors.groupingBy(ResearchTaskDO::getSourceReportId));

        Set<String> warningIds = warnings.stream()
                .map(RiskWarningDO::getWarningId)
                .filter(warningId -> warningId != null && !warningId.isBlank())
                .collect(Collectors.toSet());
        Map<String, List<RiskWarningDetailDO>> detailMap = warningIds.isEmpty()
                ? Collections.emptyMap()
                : riskWarningDetailMapper.selectList(
                        new LambdaQueryWrapper<RiskWarningDetailDO>()
                                .eq(RiskWarningDetailDO::getDeleted, 0)
                                .in(RiskWarningDetailDO::getWarningId, warningIds)
                                .orderByAsc(RiskWarningDetailDO::getId)
                ).stream().collect(Collectors.groupingBy(RiskWarningDetailDO::getWarningId));

        return warnings.stream()
                .map(warning -> {
                    ResearchTaskDO task = taskMap.get(warning.getTaskId());
                    ResearchReportDO report = reportMap.get(warning.getTaskId());
                    return toRiskWarningItem(
                            warning,
                            task,
                            report,
                            resolveRiskWarningFollowUpSummary(
                                    task,
                                    report,
                                    followUpTaskMapBySourceTaskId,
                                    followUpTaskMapBySourceReportId
                            ),
                            detailMap.getOrDefault(warning.getWarningId(), List.of())
                    );
                })
                .filter(Objects::nonNull)
                .filter(item -> matchesRiskWarningQuery(item, queryDTO))
                .toList();
    }

    private RiskWarningListItemVO toRiskWarningItem(RiskWarningDO warning,
                                                    ResearchTaskDO task,
                                                    ResearchReportDO report,
                                                    RiskWarningFollowUpSummary followUpSummary,
                                                    List<RiskWarningDetailDO> details) {
        if (warning == null) {
            return null;
        }

        List<String> riskReasons = buildDomainRiskReasons(warning, details);
        boolean needHumanReview = isDomainRiskHumanReview(warning);
        ReportReviewStatusEnum reviewStatus = resolveReviewStatus(
                warning.getReviewStatus() == null && report != null ? report.getReviewStatus() : warning.getReviewStatus()
        );

        RiskWarningListItemVO vo = new RiskWarningListItemVO();
        vo.setTaskId(warning.getTaskId());
        vo.setTaskTitle(task == null ? warning.getWarningSummary() : task.getTaskTitle());
        vo.setTaskType(task == null ? null : task.getTaskType());
        vo.setTargetCode(warning.getEntityCode());
        vo.setTargetName(warning.getEntityName());
        vo.setPriority(task == null ? null : task.getPriority());
        vo.setTaskStatus(task == null ? null : task.getStatus());
        vo.setCurrentStage(task == null ? null : task.getCurrentStage());
        vo.setReportId(report == null ? null : report.getReportId());
        vo.setReportType(report == null ? null : report.getReportType());
        vo.setFinalStatus(report == null ? null : report.getFinalStatus());
        vo.setRiskLevel(resolveDomainRiskLevel(warning).name());
        vo.setWarningCount(1);
        vo.setRiskPointCount(details == null ? 0 : details.size());
        vo.setTotalRiskCount(1 + (details == null ? 0 : details.size()));
        vo.setNeedHumanReview(needHumanReview);
        vo.setReportReviewStatus(reviewStatus.name());
        vo.setReportReviewedBy(warning.getReviewerId() == null && report != null ? report.getReviewedBy() : warning.getReviewerId());
        vo.setReportReviewedAt(warning.getReviewTime() == null && report != null ? report.getReviewedAt() : warning.getReviewTime());
        vo.setRevised(report != null && isReportRevised(report));
        vo.setSummaryRevised(report != null && isSummaryRevised(report));
        vo.setHighlightsRevised(report != null && isHighlightsRevised(report));
        vo.setRiskPointsRevised(report != null && isRiskPointsRevised(report));
        if (followUpSummary != null) {
            vo.setFollowUpStatus(followUpSummary.followUpStatus());
            vo.setFollowUpTaskCount(followUpSummary.followUpTaskCount());
            vo.setLatestFollowUpTaskId(followUpSummary.latestFollowUpTaskId());
            vo.setLatestFollowUpTaskTitle(followUpSummary.latestFollowUpTaskTitle());
            vo.setLatestFollowUpTaskStatus(followUpSummary.latestFollowUpTaskStatus());
            vo.setLatestFollowUpCreatedAt(followUpSummary.latestFollowUpCreatedAt());
        }
        vo.setReviewComment(report == null ? null : report.getReviewComment());
        vo.setSummary(warning.getWarningSummary());
        vo.setRiskReasons(riskReasons);
        vo.setRiskSourceTags(buildDomainRiskSourceTags(warning, needHumanReview, reviewStatus));
        vo.setCreatedAt(warning.getCreatedAt());
        return vo;
    }

    private List<String> buildDomainRiskReasons(RiskWarningDO warning, List<RiskWarningDetailDO> details) {
        LinkedHashSet<String> reasons = new LinkedHashSet<>();
        if (warning.getWarningReason() != null && !warning.getWarningReason().isBlank()) {
            for (String item : warning.getWarningReason().split("\\R")) {
                if (item != null && !item.isBlank()) {
                    reasons.add(item.trim());
                }
            }
        }
        if (warning.getWarningSummary() != null && !warning.getWarningSummary().isBlank()) {
            reasons.add(warning.getWarningSummary().trim());
        }
        if (details != null) {
            for (RiskWarningDetailDO detail : details) {
                if (detail.getDetailDesc() != null && !detail.getDetailDesc().isBlank()) {
                    reasons.add(detail.getDetailDesc().trim());
                } else if (detail.getIndicatorValue() != null && !detail.getIndicatorValue().isBlank()) {
                    reasons.add(detail.getIndicatorValue().trim());
                }
            }
        }
        return new ArrayList<>(reasons);
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

    private List<String> buildDomainRiskSourceTags(RiskWarningDO warning,
                                                   boolean needHumanReview,
                                                   ReportReviewStatusEnum reviewStatus) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        tags.add("RISK_WARNING");
        if (warning.getWarningType() != null && !warning.getWarningType().isBlank()) {
            tags.add(warning.getWarningType().trim());
        }
        if (warning.getTriggerSource() != null && !warning.getTriggerSource().isBlank()) {
            tags.add(warning.getTriggerSource().trim());
        }
        if (warning.getTriggerEventId() != null && !warning.getTriggerEventId().isBlank()) {
            tags.add("EVENT_TRIGGERED");
        }
        if (needHumanReview) {
            tags.add("HUMAN_REVIEW");
        }
        if (reviewStatus == ReportReviewStatusEnum.REJECTED) {
            tags.add("REVIEW_REJECTED");
        }
        return new ArrayList<>(tags);
    }

    private List<RiskWarningListItemVO> listRiskWarningRecordsFromReports(RiskWarningPageQueryDTO queryDTO,
                                                                          Set<String> excludedTaskIds) {
        List<ResearchReportDO> reports = researchReportMapper.selectList(
                new LambdaQueryWrapper<ResearchReportDO>()
                        .eq(ResearchReportDO::getDeleted, 0)
                        .orderByDesc(ResearchReportDO::getCreatedAt, ResearchReportDO::getId)
        );

        if (reports.isEmpty()) {
            return List.of();
        }

        if (excludedTaskIds != null && !excludedTaskIds.isEmpty()) {
            reports = reports.stream()
                    .filter(report -> report.getTaskId() == null || !excludedTaskIds.contains(report.getTaskId()))
                    .toList();
            if (reports.isEmpty()) {
                return List.of();
            }
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
        Map<String, List<RiskWarningDetailDO>> riskWarningDetailMap = loadRiskWarningDetailMapByWarningIds(
                riskWarningMap.values().stream()
                        .map(RiskWarningDO::getWarningId)
                        .filter(warningId -> warningId != null && !warningId.isBlank())
                        .collect(Collectors.toSet())
        );

        List<ResearchTaskDO> followUpTasks = researchTaskMapper.selectList(
                new LambdaQueryWrapper<ResearchTaskDO>()
                        .eq(ResearchTaskDO::getDeleted, 0)
                        .eq(ResearchTaskDO::getSourceDomain, "RISK_WARNING")
        );

        Map<String, List<ResearchTaskDO>> followUpTaskMapBySourceTaskId = followUpTasks.stream()
                .filter(item -> item.getSourceTaskId() != null && !item.getSourceTaskId().isBlank())
                .collect(Collectors.groupingBy(ResearchTaskDO::getSourceTaskId));

        Map<String, List<ResearchTaskDO>> followUpTaskMapBySourceReportId = followUpTasks.stream()
                .filter(item -> item.getSourceReportId() != null && !item.getSourceReportId().isBlank())
                .collect(Collectors.groupingBy(ResearchTaskDO::getSourceReportId));

        return reports.stream()
                .map(report -> toRiskWarningItem(
                        report,
                        taskMap.get(report.getTaskId()),
                        resolveRiskWarningFollowUpSummary(
                                taskMap.get(report.getTaskId()),
                                report,
                                followUpTaskMapBySourceTaskId,
                                followUpTaskMapBySourceReportId
                        )
                ))
                .filter(Objects::nonNull)
                .filter(item -> matchesRiskWarningQuery(item, queryDTO))
                .toList();
    }

    private List<RiskWarningListItemVO> sortRiskWarningRecords(List<RiskWarningListItemVO> records) {
        if (records == null || records.isEmpty()) {
            return List.of();
        }
        return records.stream()
                .sorted(Comparator
                        .comparingInt((RiskWarningListItemVO item) -> riskLevelRank(item.getRiskLevel()))
                        .reversed()
                        .thenComparing(
                                RiskWarningListItemVO::getCreatedAt,
                                Comparator.nullsLast(Comparator.reverseOrder())
                        )
                        .thenComparing(
                                RiskWarningListItemVO::getTaskId,
                                Comparator.nullsLast(Comparator.reverseOrder())
                        )
                )
                .toList();
    }

    private RiskWarningListItemVO toRiskWarningItem(ResearchReportDO report,
                                                    ResearchTaskDO task,
                                                    RiskWarningFollowUpSummary followUpSummary) {
        if (report == null || task == null) {
            return null;
        }

        List<String> warningList = readTextList(report.getRiskWarnings());
        List<String> riskPointList = readPreferredTextList(report.getRevisedRiskPoints(), report.getRiskPoints());
        boolean needHumanReview = report.getNeedHumanReview() != null && report.getNeedHumanReview() == 1;

        if (!needHumanReview && warningList.isEmpty() && riskPointList.isEmpty()) {
            return null;
        }

        int totalRiskCount = warningList.size() + riskPointList.size();
        ReportReviewStatusEnum reviewStatus = resolveReviewStatus(report.getReviewStatus());
        RiskLevelEnum riskLevel = resolveRiskLevel(totalRiskCount, needHumanReview);

        RiskWarningListItemVO vo = new RiskWarningListItemVO();
        vo.setTaskId(task.getTaskId());
        vo.setTaskTitle(task.getTaskTitle());
        vo.setTaskType(task.getTaskType());
        vo.setTargetCode(task.getTargetCode());
        vo.setTargetName(task.getTargetName());
        vo.setPriority(task.getPriority());
        vo.setTaskStatus(task.getStatus());
        vo.setCurrentStage(task.getCurrentStage());
        vo.setReportId(report.getReportId());
        vo.setReportType(report.getReportType());
        vo.setFinalStatus(report.getFinalStatus());
        vo.setRiskLevel(riskLevel.name());
        vo.setWarningCount(warningList.size());
        vo.setRiskPointCount(riskPointList.size());
        vo.setTotalRiskCount(totalRiskCount);
        vo.setNeedHumanReview(needHumanReview);
        vo.setReportReviewStatus(reviewStatus.name());
        vo.setReportReviewedBy(report.getReviewedBy());
        vo.setReportReviewedAt(report.getReviewedAt());
        vo.setRevised(isReportRevised(report));
        vo.setSummaryRevised(isSummaryRevised(report));
        vo.setHighlightsRevised(isHighlightsRevised(report));
        vo.setRiskPointsRevised(isRiskPointsRevised(report));
        if (followUpSummary != null) {
            vo.setFollowUpStatus(followUpSummary.followUpStatus());
            vo.setFollowUpTaskCount(followUpSummary.followUpTaskCount());
            vo.setLatestFollowUpTaskId(followUpSummary.latestFollowUpTaskId());
            vo.setLatestFollowUpTaskTitle(followUpSummary.latestFollowUpTaskTitle());
            vo.setLatestFollowUpTaskStatus(followUpSummary.latestFollowUpTaskStatus());
            vo.setLatestFollowUpCreatedAt(followUpSummary.latestFollowUpCreatedAt());
        }
        vo.setReviewComment(report.getReviewComment());
        vo.setSummary(resolveReportCenterSummary(report));
        vo.setRiskReasons(mergeRiskReasons(warningList, riskPointList));
        vo.setRiskSourceTags(buildRiskSourceTags(warningList, riskPointList, needHumanReview, reviewStatus));
        vo.setCreatedAt(firstNonNull(report.getCreatedAt(), task.getCreatedAt()));
        return vo;
    }

    private boolean matchesRiskWarningQuery(RiskWarningListItemVO item, RiskWarningPageQueryDTO queryDTO) {
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
        RiskLevelEnum riskLevel = RiskLevelEnum.from(queryDTO.getRiskLevel());
        if (riskLevel != null && !riskLevel.name().equals(item.getRiskLevel())) {
            return false;
        }
        ReportReviewStatusEnum reviewStatus = ReportReviewStatusEnum.from(queryDTO.getReportReviewStatus());
        if (reviewStatus != null && !reviewStatus.name().equals(item.getReportReviewStatus())) {
            return false;
        }
        if (queryDTO.getNeedHumanReview() != null && !queryDTO.getNeedHumanReview().equals(item.getNeedHumanReview())) {
            return false;
        }
        return true;
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

    private List<String> mergeRiskReasons(List<String> warningList, List<String> riskPointList) {
        LinkedHashSet<String> reasons = new LinkedHashSet<>();
        reasons.addAll(warningList);
        reasons.addAll(riskPointList);
        return new ArrayList<>(reasons);
    }

    private RiskWarningFollowUpSummary resolveRiskWarningFollowUpSummary(ResearchTaskDO sourceTask,
                                                                        ResearchReportDO sourceReport,
                                                                        Map<String, List<ResearchTaskDO>> followUpTaskMapBySourceTaskId,
                                                                        Map<String, List<ResearchTaskDO>> followUpTaskMapBySourceReportId) {
        if (sourceTask == null && sourceReport == null) {
            return defaultRiskWarningFollowUpSummary();
        }

        LinkedHashMap<String, ResearchTaskDO> followUpTaskMap = new LinkedHashMap<>();
        if (sourceTask != null && sourceTask.getTaskId() != null) {
            followUpTaskMapBySourceTaskId.getOrDefault(sourceTask.getTaskId(), List.of())
                    .forEach(item -> followUpTaskMap.put(item.getTaskId(), item));
        }
        if (sourceReport != null && sourceReport.getReportId() != null) {
            followUpTaskMapBySourceReportId.getOrDefault(sourceReport.getReportId(), List.of())
                    .forEach(item -> followUpTaskMap.put(item.getTaskId(), item));
        }

        List<ResearchTaskDO> followUpTasks = new ArrayList<>(followUpTaskMap.values());
        if (followUpTasks.isEmpty()) {
            return defaultRiskWarningFollowUpSummary();
        }

        followUpTasks.sort(Comparator
                .comparing(ResearchTaskDO::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ResearchTaskDO::getId, Comparator.nullsLast(Comparator.reverseOrder())));

        ResearchTaskDO latestTask = followUpTasks.get(0);
        String followUpStatus = resolveRiskWarningFollowUpStatus(followUpTasks);
        return new RiskWarningFollowUpSummary(
                followUpStatus,
                followUpTasks.size(),
                latestTask.getTaskId(),
                latestTask.getTaskTitle(),
                latestTask.getStatus(),
                latestTask.getCreatedAt()
        );
    }

    private RiskWarningFollowUpSummary defaultRiskWarningFollowUpSummary() {
        return new RiskWarningFollowUpSummary("NOT_TRACKED", 0, null, null, null, null);
    }

    private String resolveRiskWarningFollowUpStatus(List<ResearchTaskDO> followUpTasks) {
        if (followUpTasks == null || followUpTasks.isEmpty()) {
            return "NOT_TRACKED";
        }

        boolean hasActiveTask = followUpTasks.stream()
                .map(ResearchTaskDO::getStatus)
                .map(TaskStatusEnum::from)
                .anyMatch(status -> status == TaskStatusEnum.INIT
                        || status == TaskStatusEnum.DISPATCHED
                        || status == TaskStatusEnum.RUNNING);
        if (hasActiveTask) {
            return "TRACKING";
        }

        boolean hasSuccessTask = followUpTasks.stream()
                .map(ResearchTaskDO::getStatus)
                .map(TaskStatusEnum::from)
                .anyMatch(status -> status == TaskStatusEnum.SUCCESS);
        if (hasSuccessTask) {
            return "COMPLETED";
        }

        boolean hasFailedTask = followUpTasks.stream()
                .map(ResearchTaskDO::getStatus)
                .map(TaskStatusEnum::from)
                .anyMatch(status -> status == TaskStatusEnum.FAILED || status == TaskStatusEnum.CANCELLED);
        if (hasFailedTask) {
            return "FAILED";
        }

        return "TRACKING";
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

private List<String> readPreferredTextList(String preferredRawJson, String fallbackRawJson) {
        List<String> preferred = readTextList(preferredRawJson);
        return preferred.isEmpty() ? readTextList(fallbackRawJson) : preferred;
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

private String resolveDisplaySummary(String preferredSummary, String fallbackSummary) {
        String normalizedPreferred = normalizeText(preferredSummary);
        return normalizedPreferred != null ? normalizedPreferred : normalizeText(fallbackSummary);
    }

private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
