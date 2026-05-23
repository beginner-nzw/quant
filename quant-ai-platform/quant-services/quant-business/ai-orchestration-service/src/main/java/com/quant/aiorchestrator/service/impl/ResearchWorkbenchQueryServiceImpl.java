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
import com.quant.aiorchestrator.service.ResearchWorkbenchQueryService;
import com.quant.aiorchestrator.util.CacheKeyUtil;
import com.quant.common.core.exception.BizException;
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
public class ResearchWorkbenchQueryServiceImpl implements ResearchWorkbenchQueryService {

    /*
     * Contract boundary: research workbench is a display aggregation only.
     * It may hydrate UI fields from task/report/risk/strategy read models, but
     * it must not define domain truth or feed command/projection decisions.
     */
    private final ResearchTaskMapper researchTaskMapper;
    private final ResearchReportMapper researchReportMapper;
    private final RiskWarningMapper riskWarningMapper;
    private final RiskWarningDetailMapper riskWarningDetailMapper;
    private final StrategySignalMapper strategySignalMapper;
    private final StrategySignalFactorMapper strategySignalFactorMapper;
    private final ObjectMapper objectMapper;

private record MarketIntelligenceFollowUpSummary(
            String followUpStatus,
            Integer followUpTaskCount,
            String latestFollowUpTaskId,
            String latestFollowUpTaskTitle,
            String latestFollowUpTaskStatus,
            LocalDateTime latestFollowUpCreatedAt
    ) {}

private record RiskWarningFollowUpSummary(
            String followUpStatus,
            Integer followUpTaskCount,
            String latestFollowUpTaskId,
            String latestFollowUpTaskTitle,
            String latestFollowUpTaskStatus,
            LocalDateTime latestFollowUpCreatedAt
    ) {}

private record StrategySignalFollowUpSummary(
            String followUpStatus,
            Integer followUpTaskCount,
            String latestFollowUpTaskId,
            String latestFollowUpTaskTitle,
            String latestFollowUpTaskStatus,
            LocalDateTime latestFollowUpCreatedAt
    ) {}

private static final List<String> POSITIVE_SIGNAL_HINTS = List.of("增长", "改善", "提升", "利好", "看好", "强劲", "稳健", "修复", "机会", "受益", "positive", "upside", "beat");
    private static final List<String> NEGATIVE_SIGNAL_HINTS = List.of("风险", "下滑", "承压", "利空", "谨慎", "波动", "回落", "下行", "不确定", "亏损", "negative", "downside", "miss");

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

    private LocalDateTime firstNonNull(LocalDateTime left, LocalDateTime right) {
        return left != null ? left : right;
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

    private StrategySignalFollowUpSummary resolveStrategySignalFollowUpSummary(ResearchTaskDO sourceTask,
                                                                              ResearchReportDO sourceReport,
                                                                              Map<String, List<ResearchTaskDO>> followUpTaskMapBySourceTaskId,
                                                                              Map<String, List<ResearchTaskDO>> followUpTaskMapBySourceReportId) {
        if (sourceTask == null && sourceReport == null) {
            return defaultStrategySignalFollowUpSummary();
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
            return defaultStrategySignalFollowUpSummary();
        }

        followUpTasks.sort(Comparator
                .comparing(ResearchTaskDO::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ResearchTaskDO::getId, Comparator.nullsLast(Comparator.reverseOrder())));

        ResearchTaskDO latestTask = followUpTasks.get(0);
        String followUpStatus = resolveStrategySignalFollowUpStatus(followUpTasks);
        return new StrategySignalFollowUpSummary(
                followUpStatus,
                followUpTasks.size(),
                latestTask.getTaskId(),
                latestTask.getTaskTitle(),
                latestTask.getStatus(),
                latestTask.getCreatedAt()
        );
    }

    private StrategySignalFollowUpSummary defaultStrategySignalFollowUpSummary() {
        return new StrategySignalFollowUpSummary("NOT_TRACKED", 0, null, null, null, null);
    }

    private String resolveStrategySignalFollowUpStatus(List<ResearchTaskDO> followUpTasks) {
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

    private String resolveReportType(ResearchReportDO report, ResearchTaskDO task) {
        if (report.getReportType() != null && !report.getReportType().isBlank()) {
            return report.getReportType().trim();
        }
        return task == null ? null : task.getTaskType();
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

@Override
    public ResearchWorkbenchVO getResearchWorkbench(ResearchWorkbenchQueryDTO queryDTO) {
        ResearchWorkbenchQueryDTO safeQuery = queryDTO == null ? new ResearchWorkbenchQueryDTO() : queryDTO;
        ResearchWorkbenchVO vo = new ResearchWorkbenchVO();
        vo.setTargetCode(safeQuery.getTargetCode());
        vo.setTargetName(safeQuery.getTargetName());
        vo.setTaskCount(0L);
        vo.setReportCount(0L);
        vo.setActiveTaskCount(0L);
        vo.setSuccessTaskCount(0L);
        vo.setFailedTaskCount(0L);
        vo.setHighConfidenceReportCount(0L);
        vo.setPendingReviewCount(0L);
        vo.setRiskDispositionSummary(emptyResearchWorkbenchDispositionSummary("RISK_WARNING"));
        vo.setStrategySignalDispositionSummary(emptyResearchWorkbenchDispositionSummary("STRATEGY_SIGNAL"));
        vo.setMarketIntelligenceDispositionSummary(emptyResearchWorkbenchDispositionSummary("MARKET_INTELLIGENCE"));
        vo.setRecentTasks(List.of());

        if ((safeQuery.getTargetCode() == null || safeQuery.getTargetCode().isBlank())
                && (safeQuery.getTargetName() == null || safeQuery.getTargetName().isBlank())) {
            return vo;
        }

        LambdaQueryWrapper<ResearchTaskDO> wrapper = new LambdaQueryWrapper<ResearchTaskDO>()
                .eq(ResearchTaskDO::getDeleted, 0)
                .orderByDesc(ResearchTaskDO::getCreatedAt, ResearchTaskDO::getId);
        if (safeQuery.getTargetCode() != null && !safeQuery.getTargetCode().isBlank()) {
            wrapper.eq(ResearchTaskDO::getTargetCode, safeQuery.getTargetCode().trim());
        }
        if (safeQuery.getTargetName() != null && !safeQuery.getTargetName().isBlank()) {
            wrapper.like(ResearchTaskDO::getTargetName, safeQuery.getTargetName().trim());
        }

        List<ResearchTaskDO> tasks = researchTaskMapper.selectList(wrapper);
        if (tasks.isEmpty()) {
            return vo;
        }

        ResearchTaskDO latestTask = tasks.get(0);
        Map<String, ResearchTaskDO> taskMap = tasks.stream()
                .filter(item -> item.getTaskId() != null && !item.getTaskId().isBlank())
                .collect(Collectors.toMap(
                        ResearchTaskDO::getTaskId,
                        item -> item,
                        (left, right) -> left
                ));
        vo.setTargetCode(latestTask.getTargetCode());
        vo.setTargetName(latestTask.getTargetName());
        vo.setTargetType(latestTask.getTargetType());
        vo.setTaskCount((long) tasks.size());
        vo.setActiveTaskCount(tasks.stream().filter(item -> item.getStatus() != null && (TaskStatusEnum.DISPATCHED.name().equals(item.getStatus()) || TaskStatusEnum.RUNNING.name().equals(item.getStatus()))).count());
        vo.setSuccessTaskCount(tasks.stream().filter(item -> TaskStatusEnum.SUCCESS.name().equals(item.getStatus())).count());
        vo.setFailedTaskCount(tasks.stream().filter(item -> TaskStatusEnum.FAILED.name().equals(item.getStatus())).count());

        Set<String> taskIds = tasks.stream()
                .map(ResearchTaskDO::getTaskId)
                .filter(taskId -> taskId != null && !taskId.isBlank())
                .collect(Collectors.toSet());
        if (taskIds.isEmpty()) {
            vo.setRecentTasks(tasks.stream()
                    .limit(resolveRecentTaskLimit(safeQuery.getRecentTaskLimit()))
                    .map(item -> toResearchWorkbenchRecentTask(item, null))
                    .toList());
            return vo;
        }

        List<ResearchReportDO> reports = researchReportMapper.selectList(
                new LambdaQueryWrapper<ResearchReportDO>()
                        .eq(ResearchReportDO::getDeleted, 0)
                        .in(ResearchReportDO::getTaskId, taskIds)
                        .orderByDesc(ResearchReportDO::getCreatedAt, ResearchReportDO::getId)
        );
        Map<String, RiskWarningDO> riskWarningMap = loadLatestRiskWarningMapByTaskIds(taskIds);
        Map<String, List<RiskWarningDetailDO>> riskWarningDetailMap = loadRiskWarningDetailMapByWarningIds(
                riskWarningMap.values().stream()
                        .map(RiskWarningDO::getWarningId)
                        .filter(warningId -> warningId != null && !warningId.isBlank())
                        .collect(Collectors.toSet())
        );
        Map<String, StrategySignalDO> strategySignalMap = loadLatestStrategySignalMapByTaskIds(taskIds);

        vo.setReportCount((long) reports.size());
        vo.setHighConfidenceReportCount(reports.stream().filter(item -> isHighConfidence(item.getConfidenceScore() == null ? null : item.getConfidenceScore().doubleValue())).count());
        vo.setPendingReviewCount(reports.stream().filter(item -> ReportReviewStatusEnum.PENDING == resolveReviewStatus(item.getReviewStatus())).count());
        populateResearchWorkbenchDispositionSummaries(vo, tasks, reports, taskMap, riskWarningMap, riskWarningDetailMap, strategySignalMap);

        Map<String, ResearchReportDO> latestReportMap = reports.stream().collect(Collectors.toMap(
                ResearchReportDO::getTaskId,
                item -> item,
                (left, right) -> left
        ));

        if (!reports.isEmpty()) {
            ResearchReportDO latestReport = reports.get(0);
            RiskWarningDO latestWarning = riskWarningMap.get(latestReport.getTaskId());
            vo.setLatestInsight(toResearchWorkbenchInsight(
                    latestReport,
                    taskMap.get(latestReport.getTaskId()),
                    latestWarning,
                    latestWarning == null
                            ? List.of()
                            : riskWarningDetailMap.getOrDefault(latestWarning.getWarningId(), List.of()),
                    strategySignalMap.get(latestReport.getTaskId())
            ));
        }

        vo.setRecentTasks(tasks.stream()
                .limit(resolveRecentTaskLimit(safeQuery.getRecentTaskLimit()))
                .map(item -> toResearchWorkbenchRecentTask(item, latestReportMap.get(item.getTaskId())))
                .toList());
        return vo;
    }

    private int resolveRecentTaskLimit(Integer recentTaskLimit) {
        if (recentTaskLimit == null || recentTaskLimit < 1) {
            return 6;
        }
        return Math.min(recentTaskLimit, 10);
    }

    private ResearchWorkbenchInsightVO toResearchWorkbenchInsight(ResearchReportDO report,
                                                                  ResearchTaskDO task,
                                                                  RiskWarningDO warning,
                                                                  List<RiskWarningDetailDO> details,
                                                                  StrategySignalDO strategySignal) {
        if (report == null && warning == null) {
            return null;
        }
        String summary = report == null ? normalizeText(warning == null ? null : warning.getWarningSummary()) : resolveReportCenterSummary(report);
        if ((summary == null || summary.isBlank()) && strategySignal != null) {
            summary = normalizeText(strategySignal.getReasonSummary());
        }
        List<String> highlights = report == null ? List.of() : readPreferredTextList(report.getRevisedHighlights(), report.getHighlights());
        List<String> fallbackRiskPoints = report == null ? List.of() : readPreferredTextList(report.getRevisedRiskPoints(), report.getRiskPoints());
        List<String> fallbackRiskWarnings = report == null ? List.of() : readTextList(report.getRiskWarnings());
        List<String> domainRiskPoints = warning == null ? List.of() : buildDomainRiskInsightPoints(warning, details);
        int totalRiskCount = warning != null
                ? 1 + (details == null ? 0 : details.size())
                : fallbackRiskPoints.size() + fallbackRiskWarnings.size();
        Double reportConfidenceScore = report == null || report.getConfidenceScore() == null ? null : report.getConfidenceScore().doubleValue();
        Double signalConfidenceScore = strategySignal == null || strategySignal.getConfidenceScore() == null
                ? null
                : strategySignal.getConfidenceScore().doubleValue();
        Double confidenceScore = signalConfidenceScore == null ? reportConfidenceScore : signalConfidenceScore;
        boolean needHumanReview = warning != null
                ? isDomainRiskHumanReview(warning)
                : report.getNeedHumanReview() != null && report.getNeedHumanReview() == 1;
        ReportReviewStatusEnum reviewStatus = resolveReviewStatus(
                warning != null && warning.getReviewStatus() != null ? warning.getReviewStatus() : (report == null ? null : report.getReviewStatus())
        );
        SignalDirectionEnum signalDirection = strategySignal == null
                ? resolveSignalDirection(summary, totalRiskCount, needHumanReview, confidenceScore)
                : resolveDomainSignalDirection(strategySignal);
        SignalStrengthEnum signalStrength = strategySignal == null
                ? resolveSignalStrength(calculateSignalScore(confidenceScore, totalRiskCount, needHumanReview, reviewStatus, signalDirection))
                : resolveDomainSignalStrength(strategySignal);
        RiskLevelEnum riskLevel = warning != null
                ? resolveDomainRiskLevel(warning)
                : (totalRiskCount > 0 || needHumanReview ? resolveRiskLevel(totalRiskCount, needHumanReview) : null);

        ResearchWorkbenchInsightVO vo = new ResearchWorkbenchInsightVO();
        vo.setTaskId(report == null ? (warning == null ? null : warning.getTaskId()) : report.getTaskId());
        vo.setTaskTitle(task == null ? null : task.getTaskTitle());
        vo.setReportId(report == null ? null : report.getReportId());
        vo.setReportType(report == null ? (task == null ? null : task.getTaskType()) : resolveReportType(report, task));
        vo.setFinalStatus(report == null ? (task == null ? null : task.getStatus()) : report.getFinalStatus());
        vo.setConfidenceScore(confidenceScore);
        vo.setNeedHumanReview(needHumanReview);
        vo.setReviewStatus(reviewStatus.name());
        vo.setReviewedBy(warning != null && warning.getReviewerId() != null ? warning.getReviewerId() : (report == null ? null : report.getReviewedBy()));
        vo.setReviewedAt(warning != null && warning.getReviewTime() != null ? warning.getReviewTime() : (report == null ? null : report.getReviewedAt()));
        vo.setRevised(report != null && isReportRevised(report));
        vo.setSummaryRevised(report != null && isSummaryRevised(report));
        vo.setHighlightsRevised(report != null && isHighlightsRevised(report));
        vo.setRiskPointsRevised(report != null && isRiskPointsRevised(report));
        vo.setSignalDirection(signalDirection.name());
        vo.setSignalStrength(signalStrength.name());
        vo.setRiskLevel(riskLevel == null ? null : riskLevel.name());
        vo.setSummary(summary);
        vo.setHighlights(highlights);
        vo.setRiskPoints(domainRiskPoints.isEmpty() ? (fallbackRiskPoints.isEmpty() ? fallbackRiskWarnings : fallbackRiskPoints) : domainRiskPoints);
        vo.setCreatedAt(firstNonNull(
                report == null ? (warning == null ? null : warning.getCreatedAt()) : report.getCreatedAt(),
                task == null ? null : task.getCreatedAt()
        ));
        return vo;
    }

    private ResearchWorkbenchRecentTaskVO toResearchWorkbenchRecentTask(ResearchTaskDO task, ResearchReportDO report) {
        ResearchWorkbenchRecentTaskVO vo = new ResearchWorkbenchRecentTaskVO();
        vo.setTaskId(task.getTaskId());
        vo.setTaskTitle(task.getTaskTitle());
        vo.setPriority(task.getPriority());
        vo.setStatus(task.getStatus());
        vo.setCurrentStage(task.getCurrentStage());
        vo.setRetryCount(task.getRetryCount());
        vo.setReportId(report == null ? null : report.getReportId());
        vo.setReportReviewStatus(report == null ? null : resolveReviewStatus(report.getReviewStatus()).name());
        vo.setRevised(report != null && isReportRevised(report));
        vo.setSummaryRevised(report != null && isSummaryRevised(report));
        vo.setHighlightsRevised(report != null && isHighlightsRevised(report));
        vo.setRiskPointsRevised(report != null && isRiskPointsRevised(report));
        vo.setConfidenceScore(report == null || report.getConfidenceScore() == null ? null : report.getConfidenceScore().doubleValue());
        vo.setFinishTime(task.getFinishTime());
        vo.setCreatedAt(task.getCreatedAt());
        return vo;
    }

    private void populateResearchWorkbenchDispositionSummaries(ResearchWorkbenchVO vo,
                                                               List<ResearchTaskDO> tasks,
                                                               List<ResearchReportDO> reports,
                                                               Map<String, ResearchTaskDO> taskMap,
                                                               Map<String, RiskWarningDO> riskWarningMap,
                                                               Map<String, List<RiskWarningDetailDO>> riskWarningDetailMap,
                                                               Map<String, StrategySignalDO> strategySignalMap) {
        if (vo == null || tasks == null || tasks.isEmpty()) {
            return;
        }

        Set<String> taskIds = tasks.stream()
                .map(ResearchTaskDO::getTaskId)
                .filter(taskId -> taskId != null && !taskId.isBlank())
                .collect(Collectors.toSet());
        List<ResearchReportDO> safeReports = reports == null ? List.of() : reports;
        Set<String> reportIds = safeReports.stream()
                .map(ResearchReportDO::getReportId)
                .filter(reportId -> reportId != null && !reportId.isBlank())
                .collect(Collectors.toSet());
        Map<String, ResearchReportDO> latestReportMap = safeReports.stream().collect(Collectors.toMap(
                ResearchReportDO::getTaskId,
                item -> item,
                (left, right) -> left
        ));

        List<ResearchTaskDO> riskFollowUpTasks = loadResearchWorkbenchFollowUpTasks("RISK_WARNING", taskIds, reportIds);
        Map<String, List<ResearchTaskDO>> riskFollowUpTaskMapBySourceTaskId = groupFollowUpTasksBySourceTaskId(riskFollowUpTasks);
        Map<String, List<ResearchTaskDO>> riskFollowUpTaskMapBySourceReportId = groupFollowUpTasksBySourceReportId(riskFollowUpTasks);
        Set<String> coveredRiskTaskIds = riskWarningMap == null
                ? Collections.emptySet()
                : riskWarningMap.keySet().stream()
                        .filter(taskId -> taskId != null && !taskId.isBlank())
                        .collect(Collectors.toSet());
        List<String> riskStatuses = new ArrayList<>();
        if (riskWarningMap != null && !riskWarningMap.isEmpty()) {
            riskStatuses.addAll(riskWarningMap.values().stream()
                    .map(warning -> resolveRiskWarningFollowUpSummary(
                                    taskMap.get(warning.getTaskId()),
                                    latestReportMap.get(warning.getTaskId()),
                                    riskFollowUpTaskMapBySourceTaskId,
                                    riskFollowUpTaskMapBySourceReportId
                            ).followUpStatus()
                    )
                    .filter(status -> status != null && !status.isBlank())
                    .toList());
        }
        riskStatuses.addAll(safeReports.stream()
                .filter(report -> report.getTaskId() == null || !coveredRiskTaskIds.contains(report.getTaskId()))
                .filter(this::hasReportRiskDisposition)
                .map(report -> resolveRiskWarningFollowUpSummary(
                                taskMap.get(report.getTaskId()),
                                report,
                                riskFollowUpTaskMapBySourceTaskId,
                                riskFollowUpTaskMapBySourceReportId
                        ).followUpStatus()
                )
                .filter(status -> status != null && !status.isBlank())
                .toList());
        vo.setRiskDispositionSummary(buildResearchWorkbenchDispositionSummary("RISK_WARNING", riskStatuses));

        List<ResearchTaskDO> strategyFollowUpTasks = loadResearchWorkbenchFollowUpTasks("STRATEGY_SIGNAL", taskIds, reportIds);
        Map<String, List<ResearchTaskDO>> strategyFollowUpTaskMapBySourceTaskId = groupFollowUpTasksBySourceTaskId(strategyFollowUpTasks);
        Map<String, List<ResearchTaskDO>> strategyFollowUpTaskMapBySourceReportId = groupFollowUpTasksBySourceReportId(strategyFollowUpTasks);
        Set<String> coveredStrategyTaskIds = strategySignalMap == null
                ? Collections.emptySet()
                : strategySignalMap.keySet().stream()
                        .filter(taskId -> taskId != null && !taskId.isBlank())
                        .collect(Collectors.toSet());
        List<String> strategyStatuses = new ArrayList<>();
        if (strategySignalMap != null && !strategySignalMap.isEmpty()) {
            strategyStatuses.addAll(strategySignalMap.values().stream()
                    .map(signal -> resolveStrategySignalFollowUpSummary(
                                        taskMap.get(signal.getTaskId()),
                                        latestReportMap.get(signal.getTaskId()),
                                        strategyFollowUpTaskMapBySourceTaskId,
                                        strategyFollowUpTaskMapBySourceReportId
                                ).followUpStatus()
                    )
                    .filter(status -> status != null && !status.isBlank())
                    .toList());
        }
        strategyStatuses.addAll(safeReports.stream()
                .filter(report -> report.getTaskId() == null || !coveredStrategyTaskIds.contains(report.getTaskId()))
                .filter(this::hasReportStrategySignalDisposition)
                .map(report -> resolveStrategySignalFollowUpSummary(
                                    taskMap.get(report.getTaskId()),
                                    report,
                                    strategyFollowUpTaskMapBySourceTaskId,
                                    strategyFollowUpTaskMapBySourceReportId
                            ).followUpStatus()
                )
                .filter(status -> status != null && !status.isBlank())
                .toList());
        vo.setStrategySignalDispositionSummary(buildResearchWorkbenchDispositionSummary("STRATEGY_SIGNAL", strategyStatuses));

        List<ResearchTaskDO> intelligenceFollowUpTasks = loadResearchWorkbenchFollowUpTasks("MARKET_INTELLIGENCE", taskIds, reportIds);
        Map<String, List<ResearchTaskDO>> intelligenceFollowUpTaskMapBySourceTaskId = groupFollowUpTasksBySourceTaskId(intelligenceFollowUpTasks);
        Map<String, List<ResearchTaskDO>> intelligenceFollowUpTaskMapBySourceReportId = groupFollowUpTasksBySourceReportId(intelligenceFollowUpTasks);
        List<String> intelligenceStatuses = safeReports.stream()
                .filter(report -> hasMarketIntelligenceDisposition(report, taskMap.get(report.getTaskId())))
                .map(report -> resolveMarketIntelligenceFollowUpSummary(
                                    taskMap.get(report.getTaskId()),
                                    report,
                                    intelligenceFollowUpTaskMapBySourceTaskId,
                                    intelligenceFollowUpTaskMapBySourceReportId
                            ).followUpStatus()
                )
                .filter(status -> status != null && !status.isBlank())
                .toList();
        vo.setMarketIntelligenceDispositionSummary(buildResearchWorkbenchDispositionSummary("MARKET_INTELLIGENCE", intelligenceStatuses));
    }

    private boolean hasReportRiskDisposition(ResearchReportDO report) {
        if (report == null) {
            return false;
        }
        boolean needHumanReview = report.getNeedHumanReview() != null && report.getNeedHumanReview() == 1;
        return needHumanReview
                || !readTextList(report.getRiskWarnings()).isEmpty()
                || !readPreferredTextList(report.getRevisedRiskPoints(), report.getRiskPoints()).isEmpty();
    }

    private boolean hasReportStrategySignalDisposition(ResearchReportDO report) {
        if (report == null) {
            return false;
        }
        String strategySummary = resolveReportCenterSummary(report);
        return (strategySummary != null && !strategySummary.isBlank())
                || !readTextList(report.getRevisedHighlights()).isEmpty()
                || !readTextList(report.getHighlights()).isEmpty()
                || report.getConfidenceScore() != null;
    }

    private boolean hasMarketIntelligenceDisposition(ResearchReportDO report, ResearchTaskDO task) {
        if (report == null) {
            return false;
        }
        String summary = resolveReportCenterSummary(report);
        String reportType = resolveReportType(report, task);
        return (summary != null && !summary.isBlank())
                || (report.getResultRef() != null && !report.getResultRef().isBlank())
                || (reportType != null && !reportType.isBlank());
    }

    private List<String> buildDomainRiskInsightPoints(RiskWarningDO warning, List<RiskWarningDetailDO> details) {
        LinkedHashSet<String> points = new LinkedHashSet<>();
        if (details != null) {
            for (RiskWarningDetailDO detail : details) {
                String description = normalizeText(detail.getDetailDesc());
                if (description != null) {
                    points.add(description);
                    continue;
                }
                String indicatorName = normalizeText(detail.getIndicatorName());
                String indicatorValue = normalizeText(detail.getIndicatorValue());
                if (indicatorName != null && indicatorValue != null) {
                    points.add(indicatorName + ": " + indicatorValue);
                } else if (indicatorValue != null) {
                    points.add(indicatorValue);
                }
            }
        }
        if (warning != null && warning.getWarningReason() != null && !warning.getWarningReason().isBlank()) {
            for (String item : warning.getWarningReason().split("\\R")) {
                if (item != null && !item.isBlank()) {
                    points.add(item.trim());
                }
            }
        }
        if (points.isEmpty() && warning != null && warning.getWarningSummary() != null && !warning.getWarningSummary().isBlank()) {
            points.add(warning.getWarningSummary().trim());
        }
        return new ArrayList<>(points);
    }

    private ResearchWorkbenchDispositionSummaryVO emptyResearchWorkbenchDispositionSummary(String domainCode) {
        return buildResearchWorkbenchDispositionSummary(domainCode, List.of());
    }

    private ResearchWorkbenchDispositionSummaryVO buildResearchWorkbenchDispositionSummary(String domainCode,
                                                                                           List<String> followUpStatuses) {
        List<String> safeStatuses = followUpStatuses == null ? List.of() : followUpStatuses;
        ResearchWorkbenchDispositionSummaryVO vo = new ResearchWorkbenchDispositionSummaryVO();
        vo.setDomainCode(domainCode);
        vo.setTotalCount((long) safeStatuses.size());
        vo.setNotTrackedCount(safeStatuses.stream().filter("NOT_TRACKED"::equals).count());
        vo.setTrackingCount(safeStatuses.stream().filter("TRACKING"::equals).count());
        vo.setCompletedCount(safeStatuses.stream().filter("COMPLETED"::equals).count());
        vo.setFailedCount(safeStatuses.stream().filter("FAILED"::equals).count());
        return vo;
    }

    private List<ResearchTaskDO> loadResearchWorkbenchFollowUpTasks(String sourceDomain,
                                                                    Set<String> sourceTaskIds,
                                                                    Set<String> sourceReportIds) {
        boolean hasSourceTaskIds = sourceTaskIds != null && !sourceTaskIds.isEmpty();
        boolean hasSourceReportIds = sourceReportIds != null && !sourceReportIds.isEmpty();
        if (!hasSourceTaskIds && !hasSourceReportIds) {
            return List.of();
        }

        LambdaQueryWrapper<ResearchTaskDO> wrapper = new LambdaQueryWrapper<ResearchTaskDO>()
                .eq(ResearchTaskDO::getDeleted, 0)
                .eq(ResearchTaskDO::getSourceDomain, sourceDomain);
        if (hasSourceTaskIds && hasSourceReportIds) {
            wrapper.and(nested -> nested
                    .in(ResearchTaskDO::getSourceTaskId, sourceTaskIds)
                    .or()
                    .in(ResearchTaskDO::getSourceReportId, sourceReportIds));
        } else if (hasSourceTaskIds) {
            wrapper.in(ResearchTaskDO::getSourceTaskId, sourceTaskIds);
        } else {
            wrapper.in(ResearchTaskDO::getSourceReportId, sourceReportIds);
        }
        return researchTaskMapper.selectList(wrapper);
    }

    private Map<String, List<ResearchTaskDO>> groupFollowUpTasksBySourceTaskId(List<ResearchTaskDO> followUpTasks) {
        if (followUpTasks == null || followUpTasks.isEmpty()) {
            return Collections.emptyMap();
        }
        return followUpTasks.stream()
                .filter(item -> item.getSourceTaskId() != null && !item.getSourceTaskId().isBlank())
                .collect(Collectors.groupingBy(ResearchTaskDO::getSourceTaskId));
    }

    private Map<String, List<ResearchTaskDO>> groupFollowUpTasksBySourceReportId(List<ResearchTaskDO> followUpTasks) {
        if (followUpTasks == null || followUpTasks.isEmpty()) {
            return Collections.emptyMap();
        }
        return followUpTasks.stream()
                .filter(item -> item.getSourceReportId() != null && !item.getSourceReportId().isBlank())
                .collect(Collectors.groupingBy(ResearchTaskDO::getSourceReportId));
    }

    /*
     * Preferred/fallback selection is display hydration only. It preserves the
     * existing UI precedence without creating a new authoritative source.
     */
    private List<String> readPreferredTextList(String preferredRawJson, String fallbackRawJson) {
        List<String> preferred = readTextList(preferredRawJson);
        return preferred.isEmpty() ? readTextList(fallbackRawJson) : preferred;
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
