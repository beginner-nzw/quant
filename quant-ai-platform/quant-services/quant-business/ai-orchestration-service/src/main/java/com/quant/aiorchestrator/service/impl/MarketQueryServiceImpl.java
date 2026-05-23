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
import com.quant.aiorchestrator.service.MarketEventService;
import com.quant.aiorchestrator.service.MarketQueryService;
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
public class MarketQueryServiceImpl implements MarketQueryService {

    private final MarketEventService marketEventService;
    private final ResearchTaskMapper researchTaskMapper;
    private final ResearchReportMapper researchReportMapper;
    private final RiskWarningMapper riskWarningMapper;
    private final RiskWarningDetailMapper riskWarningDetailMapper;
    private final StrategySignalMapper strategySignalMapper;
    private final ObjectMapper objectMapper;

private record MarketIntelligenceFollowUpSummary(
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

private static final List<String> POSITIVE_SIGNAL_HINTS = List.of("增长", "改善", "提升", "利好", "看好", "强劲", "稳健", "修复", "机会", "受益", "positive", "upside", "beat");
    private static final List<String> NEGATIVE_SIGNAL_HINTS = List.of("风险", "下滑", "承压", "利空", "谨慎", "波动", "回落", "下行", "不确定", "亏损", "negative", "downside", "miss");

@Override
    public MarketEventPageVO pageMarketEvents(MarketEventPageQueryDTO queryDTO) {
        return marketEventService.pageMarketEvents(queryDTO);
    }

    @Override
    public MarketEventStatsVO getMarketEventStats() {
        return marketEventService.getMarketEventStats();
    }

    @Override
    public MarketEventListItemVO getMarketEvent(String eventId) {
        return marketEventService.getMarketEvent(eventId);
    }

    @Override
    public List<MarketEventIngestHistoryItemVO> listMarketEventIngestHistory() {
        return marketEventService.listMarketEventIngestHistory();
    }

@Override
    public MarketIntelligencePageVO pageMarketIntelligence(MarketIntelligencePageQueryDTO queryDTO) {
        MarketIntelligencePageQueryDTO safeQuery = queryDTO == null ? new MarketIntelligencePageQueryDTO() : queryDTO;
        int pageNum = safeQuery.getPageNum() == null || safeQuery.getPageNum() < 1 ? 1 : safeQuery.getPageNum();
        int pageSize = safeQuery.getPageSize() == null || safeQuery.getPageSize() < 1 ? 10 : safeQuery.getPageSize();

        List<MarketIntelligenceListItemVO> matchedRecords = listMarketIntelligenceRecords(safeQuery);
        int fromIndex = Math.min((pageNum - 1) * pageSize, matchedRecords.size());
        int toIndex = Math.min(fromIndex + pageSize, matchedRecords.size());

        MarketIntelligencePageVO vo = new MarketIntelligencePageVO();
        vo.setTotal((long) matchedRecords.size());
        vo.setPageNum((long) pageNum);
        vo.setPageSize((long) pageSize);
        vo.setRecords(fromIndex >= toIndex ? List.of() : matchedRecords.subList(fromIndex, toIndex));
        return vo;
    }

    @Override
    public MarketIntelligenceStatsVO getMarketIntelligenceStats() {
        List<MarketIntelligenceListItemVO> records = listMarketIntelligenceRecords(new MarketIntelligencePageQueryDTO());
        MarketIntelligenceStatsVO vo = new MarketIntelligenceStatsVO();
        vo.setTotalCount((long) records.size());
        vo.setRiskAlertCount(records.stream().filter(item -> MarketIntelligenceTypeEnum.RISK_ALERT.name().equals(item.getIntelligenceType())).count());
        vo.setStrategySignalCount(records.stream().filter(item -> MarketIntelligenceTypeEnum.STRATEGY_SIGNAL.name().equals(item.getIntelligenceType())).count());
        vo.setReportInsightCount(records.stream().filter(item -> MarketIntelligenceTypeEnum.REPORT_INSIGHT.name().equals(item.getIntelligenceType())).count());
        vo.setHighPriorityCount(records.stream().filter(item -> "HIGH".equalsIgnoreCase(item.getPriority())).count());
        vo.setPendingReviewCount(records.stream().filter(item -> ReportReviewStatusEnum.PENDING.name().equals(item.getReviewStatus())).count());
        return vo;
    }

    private List<MarketIntelligenceListItemVO> listMarketIntelligenceRecords(MarketIntelligencePageQueryDTO queryDTO) {
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
        Map<String, List<RiskWarningDetailDO>> riskWarningDetailMap = loadRiskWarningDetailMapByWarningIds(
                riskWarningMap.values().stream()
                        .map(RiskWarningDO::getWarningId)
                        .filter(warningId -> warningId != null && !warningId.isBlank())
                        .collect(Collectors.toSet())
        );
        Map<String, StrategySignalDO> strategySignalMap = loadLatestStrategySignalMapByTaskIds(taskIds);

        List<ResearchTaskDO> followUpTasks = researchTaskMapper.selectList(
                new LambdaQueryWrapper<ResearchTaskDO>()
                        .eq(ResearchTaskDO::getDeleted, 0)
                        .eq(ResearchTaskDO::getSourceDomain, "MARKET_INTELLIGENCE")
        );

        Map<String, List<ResearchTaskDO>> followUpTaskMapBySourceTaskId = followUpTasks.stream()
                .filter(item -> item.getSourceTaskId() != null && !item.getSourceTaskId().isBlank())
                .collect(Collectors.groupingBy(ResearchTaskDO::getSourceTaskId));

        Map<String, List<ResearchTaskDO>> followUpTaskMapBySourceReportId = followUpTasks.stream()
                .filter(item -> item.getSourceReportId() != null && !item.getSourceReportId().isBlank())
                .collect(Collectors.groupingBy(ResearchTaskDO::getSourceReportId));

        return reports.stream()
                .map(report -> {
                    RiskWarningDO warning = riskWarningMap.get(report.getTaskId());
                    StrategySignalDO signal = strategySignalMap.get(report.getTaskId());
                    return toMarketIntelligenceItem(
                            report,
                            taskMap.get(report.getTaskId()),
                            resolveMarketIntelligenceFollowUpSummary(
                                    taskMap.get(report.getTaskId()),
                                    report,
                                    followUpTaskMapBySourceTaskId,
                                    followUpTaskMapBySourceReportId
                            ),
                            warning,
                            warning == null ? List.of() : riskWarningDetailMap.getOrDefault(warning.getWarningId(), List.of()),
                            signal
                    );
                })
                .filter(Objects::nonNull)
                .filter(item -> matchesMarketIntelligenceQuery(item, queryDTO))
                .sorted(Comparator
                        .comparing(MarketIntelligenceListItemVO::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(MarketIntelligenceListItemVO::getConfidenceScore, Comparator.nullsLast(Comparator.reverseOrder()))
                )
                .toList();
    }

    private MarketIntelligenceListItemVO toMarketIntelligenceItem(ResearchReportDO report,
                                                                  ResearchTaskDO task,
                                                                  MarketIntelligenceFollowUpSummary followUpSummary,
                                                                  RiskWarningDO warning,
                                                                  List<RiskWarningDetailDO> warningDetails,
                                                                  StrategySignalDO strategySignal) {
        if (report == null || task == null) {
            return null;
        }

        String summary = resolveReportCenterSummary(report);
        String reportType = resolveReportType(report, task);
        RiskProjection riskProjection = resolveRiskProjection(report, warning, warningDetails);
        int totalRiskCount = riskProjection.totalRiskCount();
        boolean needHumanReview = riskProjection.needHumanReview();
        Double reportConfidenceScore = report.getConfidenceScore() == null ? null : report.getConfidenceScore().doubleValue();
        Double signalConfidenceScore = strategySignal == null || strategySignal.getConfidenceScore() == null
                ? null
                : strategySignal.getConfidenceScore().doubleValue();
        Double confidenceScore = signalConfidenceScore == null ? reportConfidenceScore : signalConfidenceScore;
        SignalDirectionEnum signalDirection = strategySignal == null
                ? resolveSignalDirection(summary, totalRiskCount, needHumanReview, confidenceScore)
                : resolveDomainSignalDirection(strategySignal);
        RiskLevelEnum riskLevel = riskProjection.riskLevel();
        MarketIntelligenceTypeEnum intelligenceType = resolveMarketIntelligenceType(
                totalRiskCount,
                needHumanReview,
                confidenceScore,
                signalDirection,
                strategySignal != null
        );

        if ((summary == null || summary.isBlank())
                && (report.getResultRef() == null || report.getResultRef().isBlank())
                && (reportType == null || reportType.isBlank())) {
            return null;
        }

        ReportReviewStatusEnum reviewStatus = resolveReviewStatus(report.getReviewStatus());

        MarketIntelligenceListItemVO vo = new MarketIntelligenceListItemVO();
        vo.setTaskId(task.getTaskId());
        vo.setTaskTitle(task.getTaskTitle());
        vo.setTaskType(task.getTaskType());
        vo.setTargetCode(task.getTargetCode());
        vo.setTargetName(task.getTargetName());
        vo.setPriority(task.getPriority());
        vo.setSourceChannel(task.getSourceChannel());
        vo.setIntelligenceType(intelligenceType.name());
        vo.setReportId(report.getReportId());
        vo.setReportType(reportType);
        vo.setFinalStatus(report.getFinalStatus());
        vo.setConfidenceScore(confidenceScore);
        vo.setNeedHumanReview(needHumanReview);
        vo.setReviewStatus(reviewStatus.name());
        vo.setReviewedBy(report.getReviewedBy());
        vo.setReviewedAt(report.getReviewedAt());
        vo.setReviewComment(report.getReviewComment());
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
        vo.setSignalDirection(signalDirection.name());
        vo.setRiskLevel(riskLevel == null ? null : riskLevel.name());
        vo.setIntelligenceSourceTags(buildMarketIntelligenceSourceTags(
                task,
                intelligenceType,
                summary,
                totalRiskCount,
                needHumanReview,
                reviewStatus,
                confidenceScore,
                signalDirection,
                strategySignal != null
        ));
        vo.setSummary(summary);
        vo.setCreatedAt(firstNonNull(report.getCreatedAt(), task.getCreatedAt()));
        return vo;
    }

    private boolean matchesMarketIntelligenceQuery(MarketIntelligenceListItemVO item, MarketIntelligencePageQueryDTO queryDTO) {
        if (item == null) {
            return false;
        }
        if (queryDTO == null) {
            return true;
        }
        if (queryDTO.getTargetCode() != null && !queryDTO.getTargetCode().isBlank()
                && !containsIgnoreCase(item.getTargetCode(), queryDTO.getTargetCode())) {
            return false;
        }
        if (queryDTO.getTargetName() != null && !queryDTO.getTargetName().isBlank()
                && !containsIgnoreCase(item.getTargetName(), queryDTO.getTargetName())) {
            return false;
        }
        MarketIntelligenceTypeEnum intelligenceType = MarketIntelligenceTypeEnum.from(queryDTO.getIntelligenceType());
        if (intelligenceType != null && !intelligenceType.name().equals(item.getIntelligenceType())) {
            return false;
        }
        ReportReviewStatusEnum reviewStatus = ReportReviewStatusEnum.from(queryDTO.getReviewStatus());
        if (reviewStatus != null && !reviewStatus.name().equals(item.getReviewStatus())) {
            return false;
        }
        if (Boolean.TRUE.equals(queryDTO.getOnlyHighPriority()) && !"HIGH".equalsIgnoreCase(item.getPriority())) {
            return false;
        }
        if (queryDTO.getNeedHumanReview() != null && !queryDTO.getNeedHumanReview().equals(item.getNeedHumanReview())) {
            return false;
        }
        return true;
    }

    private MarketIntelligenceTypeEnum resolveMarketIntelligenceType(int totalRiskCount,
                                                                     boolean needHumanReview,
                                                                     Double confidenceScore,
                                                                     SignalDirectionEnum signalDirection,
                                                                     boolean hasDomainStrategySignal) {
        if (needHumanReview || totalRiskCount > 0) {
            return MarketIntelligenceTypeEnum.RISK_ALERT;
        }
        if (hasDomainStrategySignal) {
            return MarketIntelligenceTypeEnum.STRATEGY_SIGNAL;
        }
        if (signalDirection == SignalDirectionEnum.POSITIVE
                || signalDirection == SignalDirectionEnum.NEGATIVE
                || isHighConfidence(confidenceScore)) {
            return MarketIntelligenceTypeEnum.STRATEGY_SIGNAL;
        }
        return MarketIntelligenceTypeEnum.REPORT_INSIGHT;
    }

    private MarketIntelligenceFollowUpSummary resolveMarketIntelligenceFollowUpSummary(ResearchTaskDO sourceTask,
                                                                                       ResearchReportDO sourceReport,
                                                                                       Map<String, List<ResearchTaskDO>> followUpTaskMapBySourceTaskId,
                                                                                       Map<String, List<ResearchTaskDO>> followUpTaskMapBySourceReportId) {
        if (sourceTask == null && sourceReport == null) {
            return defaultMarketIntelligenceFollowUpSummary();
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
            return defaultMarketIntelligenceFollowUpSummary();
        }

        followUpTasks.sort(Comparator
                .comparing(ResearchTaskDO::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ResearchTaskDO::getId, Comparator.nullsLast(Comparator.reverseOrder())));

        ResearchTaskDO latestTask = followUpTasks.get(0);
        String followUpStatus = resolveMarketIntelligenceFollowUpStatus(followUpTasks);
        return new MarketIntelligenceFollowUpSummary(
                followUpStatus,
                followUpTasks.size(),
                latestTask.getTaskId(),
                latestTask.getTaskTitle(),
                latestTask.getStatus(),
                latestTask.getCreatedAt()
        );
    }

    private MarketIntelligenceFollowUpSummary defaultMarketIntelligenceFollowUpSummary() {
        return new MarketIntelligenceFollowUpSummary("NOT_TRACKED", 0, null, null, null, null);
    }

    private String resolveMarketIntelligenceFollowUpStatus(List<ResearchTaskDO> followUpTasks) {
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

    private List<String> buildMarketIntelligenceSourceTags(ResearchTaskDO task,
                                                           MarketIntelligenceTypeEnum intelligenceType,
                                                           String summary,
                                                           int totalRiskCount,
                                                           boolean needHumanReview,
                                                           ReportReviewStatusEnum reviewStatus,
                                                           Double confidenceScore,
                                                           SignalDirectionEnum signalDirection,
                                                           boolean hasDomainStrategySignal) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        if (task != null && task.getSourceChannel() != null && !task.getSourceChannel().isBlank()) {
            tags.add("SOURCE_CHANNEL");
        }
        if (summary != null && !summary.isBlank()) {
            tags.add("REPORT_SUMMARY");
        }
        if (intelligenceType == MarketIntelligenceTypeEnum.RISK_ALERT || totalRiskCount > 0) {
            tags.add("RISK_ALERT");
        }
        if (hasDomainStrategySignal
                || intelligenceType == MarketIntelligenceTypeEnum.STRATEGY_SIGNAL
                || signalDirection == SignalDirectionEnum.POSITIVE
                || signalDirection == SignalDirectionEnum.NEGATIVE
                || isHighConfidence(confidenceScore)) {
            tags.add("STRATEGY_SIGNAL");
        }
        if (intelligenceType == MarketIntelligenceTypeEnum.REPORT_INSIGHT) {
            tags.add("REPORT_INSIGHT");
        }
        if (needHumanReview) {
            tags.add("HUMAN_REVIEW");
        }
        if (reviewStatus == ReportReviewStatusEnum.REJECTED) {
            tags.add("REVIEW_REJECTED");
        }
        return new ArrayList<>(tags);
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

private SignalDirectionEnum resolveDomainSignalDirection(StrategySignalDO signal) {
        SignalDirectionEnum resolved = signal == null ? null : SignalDirectionEnum.from(signal.getSignalDirection());
        if (resolved != null) {
            return resolved;
        }
        Double confidenceScore = signal == null || signal.getConfidenceScore() == null
                ? null
                : signal.getConfidenceScore().doubleValue();
        return resolveSignalDirection(signal == null ? null : signal.getReasonSummary(), 0, false, confidenceScore);
    }

private SignalStrengthEnum resolveDomainSignalStrength(StrategySignalDO signal) {
        SignalStrengthEnum resolved = signal == null ? null : SignalStrengthEnum.from(signal.getSignalLevel());
        if (resolved != null) {
            return resolved;
        }
        if (signal != null && signal.getSignalScore() != null) {
            return resolveSignalStrength(signal.getSignalScore());
        }
        Double confidenceScore = signal == null || signal.getConfidenceScore() == null
                ? null
                : signal.getConfidenceScore().doubleValue();
        int fallbackScore = confidenceScore == null ? 60 : (int) Math.round(Math.max(0D, Math.min(1D, confidenceScore)) * 100D);
        return resolveSignalStrength(fallbackScore);
    }

private SignalDirectionEnum resolveSignalDirection(String strategySummary,
                                                       int totalRiskCount,
                                                       boolean needHumanReview,
                                                       Double confidenceScore) {
        String normalizedSummary = strategySummary == null ? "" : strategySummary.toLowerCase();
        int positiveHit = countKeywords(normalizedSummary, POSITIVE_SIGNAL_HINTS);
        int negativeHit = countKeywords(normalizedSummary, NEGATIVE_SIGNAL_HINTS) + totalRiskCount + (needHumanReview ? 1 : 0);

        if (negativeHit >= positiveHit + 2) {
            return SignalDirectionEnum.NEGATIVE;
        }
        if (positiveHit >= negativeHit + 2 && !needHumanReview && totalRiskCount <= 1 && isHighConfidence(confidenceScore)) {
            return SignalDirectionEnum.POSITIVE;
        }
        if (needHumanReview || totalRiskCount >= 3) {
            return SignalDirectionEnum.NEGATIVE;
        }
        if (isHighConfidence(confidenceScore) && totalRiskCount == 0) {
            return SignalDirectionEnum.POSITIVE;
        }
        return SignalDirectionEnum.NEUTRAL;
    }

private SignalStrengthEnum resolveSignalStrength(int signalScore) {
        if (signalScore >= 80) {
            return SignalStrengthEnum.STRONG;
        }
        if (signalScore >= 60) {
            return SignalStrengthEnum.MEDIUM;
        }
        return SignalStrengthEnum.WEAK;
    }

private int calculateSignalScore(Double confidenceScore,
                                     int totalRiskCount,
                                     boolean needHumanReview,
                                     ReportReviewStatusEnum reviewStatus,
                                     SignalDirectionEnum signalDirection) {
        int score = confidenceScore == null ? 60 : (int) Math.round(Math.max(0D, Math.min(1D, confidenceScore)) * 100D);
        score -= totalRiskCount * 8;
        if (needHumanReview) {
            score -= 12;
        }
        if (reviewStatus == ReportReviewStatusEnum.REJECTED) {
            score -= 10;
        }
        if (signalDirection == SignalDirectionEnum.POSITIVE) {
            score += 5;
        }
        if (signalDirection == SignalDirectionEnum.NEGATIVE) {
            score -= 5;
        }
        return Math.max(0, Math.min(100, score));
    }

private boolean isHighConfidence(Double confidenceScore) {
        return confidenceScore != null && confidenceScore >= 0.8D;
    }

private int countKeywords(String content, List<String> keywords) {
        if (content == null || content.isBlank() || keywords == null || keywords.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String keyword : keywords) {
            if (keyword != null && !keyword.isBlank() && content.contains(keyword.toLowerCase())) {
                count++;
            }
        }
        return count;
    }

private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

private String resolveDisplaySummary(String preferredSummary, String fallbackSummary) {
        String normalizedPreferred = normalizeText(preferredSummary);
        return normalizedPreferred != null ? normalizedPreferred : normalizeText(fallbackSummary);
    }
}
