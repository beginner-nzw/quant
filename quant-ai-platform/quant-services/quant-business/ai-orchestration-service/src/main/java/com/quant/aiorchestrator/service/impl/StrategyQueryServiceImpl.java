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
import com.quant.aiorchestrator.service.StrategyQueryService;
import com.quant.aiorchestrator.service.StrategySignalService;
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
public class StrategyQueryServiceImpl implements StrategyQueryService {

    private final ResearchTaskMapper researchTaskMapper;
    private final ResearchReportMapper researchReportMapper;
    private final RiskWarningMapper riskWarningMapper;
    private final RiskWarningDetailMapper riskWarningDetailMapper;
    private final StrategySignalMapper strategySignalMapper;
    private final StrategySignalFactorMapper strategySignalFactorMapper;
    private final StrategySignalService strategySignalService;
    private final ObjectMapper objectMapper;

private record StrategySignalFollowUpSummary(
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
    private static final String BACKTEST_STATUS_NOT_READY = "NOT_READY";
    private static final String BACKTEST_SUMMARY_NOT_READY = "历史回测待接入";

@Override
    public StrategySignalPageVO pageStrategySignals(StrategySignalPageQueryDTO queryDTO) {
        StrategySignalPageQueryDTO safeQuery = queryDTO == null ? new StrategySignalPageQueryDTO() : queryDTO;
        int pageNum = safeQuery.getPageNum() == null || safeQuery.getPageNum() < 1 ? 1 : safeQuery.getPageNum();
        int pageSize = safeQuery.getPageSize() == null || safeQuery.getPageSize() < 1 ? 10 : safeQuery.getPageSize();

        List<StrategySignalListItemVO> matchedRecords = listStrategySignalRecords(safeQuery);
        int fromIndex = Math.min((pageNum - 1) * pageSize, matchedRecords.size());
        int toIndex = Math.min(fromIndex + pageSize, matchedRecords.size());

        StrategySignalPageVO vo = new StrategySignalPageVO();
        vo.setTotal((long) matchedRecords.size());
        vo.setPageNum((long) pageNum);
        vo.setPageSize((long) pageSize);
        vo.setRecords(fromIndex >= toIndex ? List.of() : matchedRecords.subList(fromIndex, toIndex));
        return vo;
    }

    @Override
    public StrategySignalStatsVO getStrategySignalStats() {
        List<StrategySignalListItemVO> records = listStrategySignalRecords(new StrategySignalPageQueryDTO());
        StrategySignalStatsVO vo = new StrategySignalStatsVO();
        vo.setTotalCount((long) records.size());
        vo.setPositiveCount(records.stream().filter(item -> SignalDirectionEnum.POSITIVE.name().equals(item.getSignalDirection())).count());
        vo.setNeutralCount(records.stream().filter(item -> SignalDirectionEnum.NEUTRAL.name().equals(item.getSignalDirection())).count());
        vo.setNegativeCount(records.stream().filter(item -> SignalDirectionEnum.NEGATIVE.name().equals(item.getSignalDirection())).count());
        vo.setHighConfidenceCount(records.stream().filter(item -> isHighConfidence(item.getConfidenceScore())).count());
        vo.setPendingReviewCount(records.stream().filter(item -> ReportReviewStatusEnum.PENDING.name().equals(item.getReportReviewStatus())).count());
        return vo;
    }

    private List<StrategySignalListItemVO> listStrategySignalRecords(StrategySignalPageQueryDTO queryDTO) {
        List<StrategySignalDO> domainSignals = loadActiveStrategySignals();
        if (domainSignals.isEmpty()) {
            return listStrategySignalRecordsFromReports(queryDTO, Collections.emptySet());
        }

        Set<String> coveredTaskIds = domainSignals.stream()
                .map(StrategySignalDO::getTaskId)
                .filter(taskId -> taskId != null && !taskId.isBlank())
                .collect(Collectors.toSet());

        List<StrategySignalListItemVO> records = new ArrayList<>(listStrategySignalRecordsFromDomain(domainSignals, queryDTO));
        records.addAll(listStrategySignalRecordsFromReports(queryDTO, coveredTaskIds));
        return sortStrategySignalRecords(records);
    }

    private List<StrategySignalDO> loadActiveStrategySignals() {
        return strategySignalMapper.selectList(
                new LambdaQueryWrapper<StrategySignalDO>()
                        .eq(StrategySignalDO::getDeleted, 0)
                        .orderByDesc(StrategySignalDO::getSignalDate, StrategySignalDO::getCreatedAt, StrategySignalDO::getId)
        );
    }

    private List<StrategySignalListItemVO> listStrategySignalRecordsFromDomain(List<StrategySignalDO> signals,
                                                                               StrategySignalPageQueryDTO queryDTO) {
        if (signals.isEmpty()) {
            return List.of();
        }

        Set<String> taskIds = signals.stream()
                .map(StrategySignalDO::getTaskId)
                .filter(taskId -> taskId != null && !taskId.isBlank())
                .collect(Collectors.toSet());
        Map<String, ResearchTaskDO> taskMap = loadTaskMap(taskIds);
        Map<String, ResearchReportDO> reportMap = loadReportMapByTaskIds(taskIds);
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
                        .eq(ResearchTaskDO::getSourceDomain, "STRATEGY_SIGNAL")
        );
        Map<String, List<ResearchTaskDO>> followUpTaskMapBySourceTaskId = followUpTasks.stream()
                .filter(item -> item.getSourceTaskId() != null && !item.getSourceTaskId().isBlank())
                .collect(Collectors.groupingBy(ResearchTaskDO::getSourceTaskId));
        Map<String, List<ResearchTaskDO>> followUpTaskMapBySourceReportId = followUpTasks.stream()
                .filter(item -> item.getSourceReportId() != null && !item.getSourceReportId().isBlank())
                .collect(Collectors.groupingBy(ResearchTaskDO::getSourceReportId));

        Set<String> signalIds = signals.stream()
                .map(StrategySignalDO::getSignalId)
                .filter(signalId -> signalId != null && !signalId.isBlank())
                .collect(Collectors.toSet());
        Map<String, List<StrategySignalFactorDO>> factorMap = signalIds.isEmpty()
                ? Collections.emptyMap()
                : strategySignalFactorMapper.selectList(
                        new LambdaQueryWrapper<StrategySignalFactorDO>()
                                .eq(StrategySignalFactorDO::getDeleted, 0)
                                .in(StrategySignalFactorDO::getSignalId, signalIds)
                                .orderByAsc(StrategySignalFactorDO::getId)
                ).stream().collect(Collectors.groupingBy(StrategySignalFactorDO::getSignalId));

        return signals.stream()
                .map(signal -> {
                    ResearchTaskDO task = taskMap.get(signal.getTaskId());
                    ResearchReportDO report = reportMap.get(signal.getTaskId());
                    RiskWarningDO warning = riskWarningMap.get(signal.getTaskId());
                    return toStrategySignalItem(
                            signal,
                            task,
                            report,
                            resolveStrategySignalFollowUpSummary(
                                    task,
                                    report,
                                    followUpTaskMapBySourceTaskId,
                                    followUpTaskMapBySourceReportId
                            ),
                            factorMap.getOrDefault(signal.getSignalId(), List.of()),
                            warning,
                            warning == null ? List.of() : riskWarningDetailMap.getOrDefault(warning.getWarningId(), List.of())
                    );
                })
                .filter(Objects::nonNull)
                .filter(item -> matchesStrategySignalQuery(item, queryDTO))
                .sorted(Comparator
                        .comparing(StrategySignalListItemVO::getSignalScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(StrategySignalListItemVO::getConfidenceScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(StrategySignalListItemVO::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                )
                .toList();
    }

    private StrategySignalListItemVO toStrategySignalItem(StrategySignalDO signal,
                                                          ResearchTaskDO task,
                                                          ResearchReportDO report,
                                                          StrategySignalFollowUpSummary followUpSummary,
                                                          List<StrategySignalFactorDO> factors,
                                                          RiskWarningDO warning,
                                                          List<RiskWarningDetailDO> warningDetails) {
        if (signal == null) {
            return null;
        }

        Double confidenceScore = signal.getConfidenceScore() == null ? null : signal.getConfidenceScore().doubleValue();
        ReportReviewStatusEnum reviewStatus = resolveReviewStatus(report == null ? null : report.getReviewStatus());
        boolean needHumanReview = resolveRiskProjection(report, warning, warningDetails).needHumanReview();

        StrategySignalListItemVO vo = new StrategySignalListItemVO();
        vo.setSignalId(signal.getSignalId());
        vo.setTaskId(signal.getTaskId());
        vo.setTaskTitle(task == null ? signal.getReasonSummary() : task.getTaskTitle());
        vo.setTaskType(task == null ? null : task.getTaskType());
        vo.setTargetCode(signal.getEntityCode());
        vo.setTargetName(signal.getEntityName());
        vo.setPriority(task == null ? null : task.getPriority());
        vo.setReportId(report == null ? null : report.getReportId());
        vo.setReportType(report == null ? signal.getSignalType() : report.getReportType());
        vo.setFinalStatus(report == null ? null : report.getFinalStatus());
        vo.setSignalDirection(resolveDomainSignalDirection(signal).name());
        vo.setSignalStrength(resolveDomainSignalStrength(signal).name());
        vo.setSignalScore(signal.getSignalScore());
        vo.setConfidenceScore(confidenceScore);
        vo.setReportReviewStatus(reviewStatus.name());
        vo.setReportReviewedBy(report == null ? null : report.getReviewedBy());
        vo.setReportReviewedAt(report == null ? null : report.getReviewedAt());
        vo.setNeedHumanReview(needHumanReview);
        vo.setReviewComment(report == null ? null : report.getReviewComment());
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
        vo.setStrategySummary(signal.getReasonSummary());
        vo.setSignalSources(buildDomainSignalSources(signal, factors));
        vo.setSignalSourceTags(buildDomainSignalSourceTags(signal, factors, needHumanReview, reviewStatus, confidenceScore));
        vo.setBacktestStatus(BACKTEST_STATUS_NOT_READY);
        vo.setBacktestSummary(BACKTEST_SUMMARY_NOT_READY);
        vo.setCreatedAt(signal.getCreatedAt());
        return vo;
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

    private List<String> buildDomainSignalSources(StrategySignalDO signal, List<StrategySignalFactorDO> factors) {
        LinkedHashSet<String> sources = new LinkedHashSet<>();
        if (signal != null && signal.getReasonSummary() != null && !signal.getReasonSummary().isBlank()) {
            sources.add(signal.getReasonSummary().trim());
        }
        if (factors != null) {
            for (StrategySignalFactorDO factor : factors) {
                String conclusion = normalizeText(factor.getFactorConclusion());
                if (conclusion != null) {
                    sources.add(conclusion);
                    continue;
                }
                String factorName = normalizeText(factor.getFactorName());
                String factorValue = normalizeText(factor.getFactorValue());
                if (factorName != null && factorValue != null) {
                    sources.add(factorName + ": " + factorValue);
                } else if (factorName != null) {
                    sources.add(factorName);
                } else if (factorValue != null) {
                    sources.add(factorValue);
                }
            }
        }
        return new ArrayList<>(sources);
    }

    private List<String> buildDomainSignalSourceTags(StrategySignalDO signal,
                                                     List<StrategySignalFactorDO> factors,
                                                     boolean needHumanReview,
                                                     ReportReviewStatusEnum reviewStatus,
                                                     Double confidenceScore) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        tags.add("STRATEGY_SIGNAL");
        if (signal != null && signal.getSignalType() != null && !signal.getSignalType().isBlank()) {
            tags.add(signal.getSignalType().trim());
        }
        if (signal != null && signal.getSourceEventId() != null && !signal.getSourceEventId().isBlank()) {
            tags.add("EVENT_TRIGGERED");
        }
        if (factors != null && !factors.isEmpty()) {
            tags.add("FACTOR_EXPLAINED");
            boolean hasRiskFactor = factors.stream()
                    .map(StrategySignalFactorDO::getFactorCode)
                    .filter(Objects::nonNull)
                    .anyMatch(code -> "RISK_COUNT".equalsIgnoreCase(code) || "HUMAN_REVIEW".equalsIgnoreCase(code));
            if (hasRiskFactor) {
                tags.add("RISK_ADJUSTED");
            }
        }
        if (isHighConfidence(confidenceScore)) {
            tags.add("HIGH_CONFIDENCE");
        }
        if (needHumanReview) {
            tags.add("HUMAN_REVIEW");
        }
        if (reviewStatus == ReportReviewStatusEnum.REJECTED) {
            tags.add("REVIEW_REJECTED");
        }
        return new ArrayList<>(tags);
    }

    private List<StrategySignalListItemVO> listStrategySignalRecordsFromReports(StrategySignalPageQueryDTO queryDTO,
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
                        .eq(ResearchTaskDO::getSourceDomain, "STRATEGY_SIGNAL")
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
                    return toStrategySignalItem(
                            report,
                            taskMap.get(report.getTaskId()),
                            resolveStrategySignalFollowUpSummary(
                                    taskMap.get(report.getTaskId()),
                                    report,
                                    followUpTaskMapBySourceTaskId,
                                    followUpTaskMapBySourceReportId
                            ),
                            warning,
                            warning == null ? List.of() : riskWarningDetailMap.getOrDefault(warning.getWarningId(), List.of())
                    );
                })
                .filter(Objects::nonNull)
                .filter(item -> matchesStrategySignalQuery(item, queryDTO))
                .sorted(Comparator
                        .comparing(StrategySignalListItemVO::getSignalScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(StrategySignalListItemVO::getConfidenceScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(StrategySignalListItemVO::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                )
                .toList();
    }

    private List<StrategySignalListItemVO> sortStrategySignalRecords(List<StrategySignalListItemVO> records) {
        if (records == null || records.isEmpty()) {
            return List.of();
        }
        return records.stream()
                .sorted(Comparator
                        .comparing(StrategySignalListItemVO::getSignalScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(StrategySignalListItemVO::getConfidenceScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(StrategySignalListItemVO::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(StrategySignalListItemVO::getTaskId, Comparator.nullsLast(Comparator.reverseOrder()))
                )
                .toList();
    }

    private StrategySignalListItemVO toStrategySignalItem(ResearchReportDO report,
                                                          ResearchTaskDO task,
                                                          StrategySignalFollowUpSummary followUpSummary,
                                                          RiskWarningDO warning,
                                                          List<RiskWarningDetailDO> warningDetails) {
        if (report == null || task == null) {
            return null;
        }

        String strategySummary = resolveStrategySummary(report);
        List<String> signalSources = resolveSignalSources(report, strategySummary);
        Double confidenceScore = report.getConfidenceScore() == null ? null : report.getConfidenceScore().doubleValue();

        if ((strategySummary == null || strategySummary.isBlank()) && signalSources.isEmpty() && confidenceScore == null) {
            return null;
        }

        RiskProjection riskProjection = resolveRiskProjection(report, warning, warningDetails);
        int totalRiskCount = riskProjection.totalRiskCount();
        boolean needHumanReview = riskProjection.needHumanReview();
        ReportReviewStatusEnum reviewStatus = resolveReviewStatus(report.getReviewStatus());
        SignalDirectionEnum signalDirection = resolveSignalDirection(strategySummary, totalRiskCount, needHumanReview, confidenceScore);
        int signalScore = calculateSignalScore(confidenceScore, totalRiskCount, needHumanReview, reviewStatus, signalDirection);
        SignalStrengthEnum signalStrength = resolveSignalStrength(signalScore);

        StrategySignalListItemVO vo = new StrategySignalListItemVO();
        vo.setSignalId(null);
        vo.setTaskId(task.getTaskId());
        vo.setTaskTitle(task.getTaskTitle());
        vo.setTaskType(task.getTaskType());
        vo.setTargetCode(task.getTargetCode());
        vo.setTargetName(task.getTargetName());
        vo.setPriority(task.getPriority());
        vo.setReportId(report.getReportId());
        vo.setReportType(report.getReportType());
        vo.setFinalStatus(report.getFinalStatus());
        vo.setSignalDirection(signalDirection.name());
        vo.setSignalStrength(signalStrength.name());
        vo.setSignalScore(signalScore);
        vo.setConfidenceScore(confidenceScore);
        vo.setReportReviewStatus(reviewStatus.name());
        vo.setReportReviewedBy(report.getReviewedBy());
        vo.setReportReviewedAt(report.getReviewedAt());
        vo.setNeedHumanReview(needHumanReview);
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
        vo.setStrategySummary(strategySummary);
        vo.setSignalSources(signalSources);
        vo.setSignalSourceTags(buildSignalSourceTags(report, signalSources, totalRiskCount, needHumanReview, reviewStatus, confidenceScore));
        vo.setBacktestStatus(BACKTEST_STATUS_NOT_READY);
        vo.setBacktestSummary(BACKTEST_SUMMARY_NOT_READY);
        vo.setCreatedAt(firstNonNull(report.getCreatedAt(), task.getCreatedAt()));
        return vo;
    }

    private boolean matchesStrategySignalQuery(StrategySignalListItemVO item, StrategySignalPageQueryDTO queryDTO) {
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
        SignalDirectionEnum signalDirection = SignalDirectionEnum.from(queryDTO.getSignalDirection());
        if (signalDirection != null && !signalDirection.name().equals(item.getSignalDirection())) {
            return false;
        }
        SignalStrengthEnum signalStrength = SignalStrengthEnum.from(queryDTO.getSignalStrength());
        if (signalStrength != null && !signalStrength.name().equals(item.getSignalStrength())) {
            return false;
        }
        ReportReviewStatusEnum reviewStatus = ReportReviewStatusEnum.from(queryDTO.getReportReviewStatus());
        if (reviewStatus != null && !reviewStatus.name().equals(item.getReportReviewStatus())) {
            return false;
        }
        if (Boolean.TRUE.equals(queryDTO.getOnlyHighConfidence()) && !isHighConfidence(item.getConfidenceScore())) {
            return false;
        }
        return true;
    }

    private String resolveStrategySummary(ResearchReportDO report) {
        if (report.getRevisedSummary() != null && !report.getRevisedSummary().isBlank()) {
            return report.getRevisedSummary().trim();
        }
        if (report.getSummary() != null && !report.getSummary().isBlank()) {
            return report.getSummary().trim();
        }
        return null;
    }

    private List<String> resolveSignalSources(ResearchReportDO report, String strategySummary) {
        LinkedHashSet<String> sources = new LinkedHashSet<>();
        sources.addAll(readTextList(report.getRevisedHighlights()));
        sources.addAll(readTextList(report.getHighlights()));
        if (sources.isEmpty() && strategySummary != null && !strategySummary.isBlank()) {
            sources.add(strategySummary);
        }
        return new ArrayList<>(sources);
    }

    private List<String> buildSignalSourceTags(ResearchReportDO report,
                                               List<String> signalSources,
                                               int totalRiskCount,
                                               boolean needHumanReview,
                                               ReportReviewStatusEnum reviewStatus,
                                               Double confidenceScore) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        if (!readTextList(report.getRevisedHighlights()).isEmpty() || !readTextList(report.getHighlights()).isEmpty()) {
            tags.add("REPORT_HIGHLIGHT");
        } else if (signalSources != null && !signalSources.isEmpty()) {
            tags.add("SUMMARY_INFERENCE");
        }
        if (isHighConfidence(confidenceScore)) {
            tags.add("HIGH_CONFIDENCE");
        }
        if (totalRiskCount > 0) {
            tags.add("RISK_ADJUSTED");
        }
        if (needHumanReview) {
            tags.add("HUMAN_REVIEW");
        }
        if (reviewStatus == ReportReviewStatusEnum.REJECTED) {
            tags.add("REVIEW_REJECTED");
        }
        return new ArrayList<>(tags);
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

private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

@Override
    public List<StrategySignalFactorItemVO> listStrategySignalFactors(String signalId) {
        return strategySignalService.listFactors(signalId);
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
}
