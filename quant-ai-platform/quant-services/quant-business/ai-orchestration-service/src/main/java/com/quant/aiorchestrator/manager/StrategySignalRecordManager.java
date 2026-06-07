package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.dto.StrategySignalPageQueryDTO;
import com.quant.aiorchestrator.domain.entity.ResearchReportDO;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.aiorchestrator.domain.entity.RiskWarningDO;
import com.quant.aiorchestrator.domain.entity.RiskWarningDetailDO;
import com.quant.aiorchestrator.domain.entity.StrategySignalDO;
import com.quant.aiorchestrator.domain.entity.StrategySignalFactorDO;
import com.quant.aiorchestrator.domain.vo.StrategySignalListItemVO;
import com.quant.common.model.enums.ReportReviewStatusEnum;
import com.quant.common.model.enums.SignalDirectionEnum;
import com.quant.common.model.enums.SignalStrengthEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class StrategySignalRecordManager {

    private final StrategySignalReadManager readManager;
    private final FollowUpTaskSummaryManager followUpManager;
    private final StrategySignalRuleManager ruleManager;
    private final StrategySignalItemAssembler itemAssembler;

    public List<StrategySignalListItemVO> listStrategySignalRecords(StrategySignalPageQueryDTO queryDTO) {
        List<StrategySignalDO> domainSignals = readManager.loadActiveStrategySignals();
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

    private List<StrategySignalListItemVO> listStrategySignalRecordsFromDomain(List<StrategySignalDO> signals,
                                                                               StrategySignalPageQueryDTO queryDTO) {
        if (signals.isEmpty()) {
            return List.of();
        }

        Set<String> taskIds = signals.stream()
                .map(StrategySignalDO::getTaskId)
                .filter(taskId -> taskId != null && !taskId.isBlank())
                .collect(Collectors.toSet());
        Map<String, ResearchTaskDO> taskMap = readManager.loadTaskMap(taskIds);
        Map<String, ResearchReportDO> reportMap = readManager.loadReportMapByTaskIds(taskIds);
        Map<String, RiskWarningDO> riskWarningMap = readManager.loadLatestRiskWarningMapByTaskIds(taskIds);
        Map<String, List<RiskWarningDetailDO>> riskWarningDetailMap = readManager.loadRiskWarningDetailMapByWarningIds(
                riskWarningMap.values().stream()
                        .map(RiskWarningDO::getWarningId)
                        .filter(warningId -> warningId != null && !warningId.isBlank())
                        .collect(Collectors.toSet())
        );

        List<ResearchTaskDO> followUpTasks = readManager.loadStrategySignalFollowUpTasks();
        Map<String, List<ResearchTaskDO>> followUpTaskMapBySourceTaskId = readManager.groupFollowUpTasksBySourceTaskId(followUpTasks);
        Map<String, List<ResearchTaskDO>> followUpTaskMapBySourceReportId = readManager.groupFollowUpTasksBySourceReportId(followUpTasks);

        Set<String> signalIds = signals.stream()
                .map(StrategySignalDO::getSignalId)
                .filter(signalId -> signalId != null && !signalId.isBlank())
                .collect(Collectors.toSet());
        Map<String, List<StrategySignalFactorDO>> factorMap = readManager.loadFactorMapBySignalIds(signalIds);

        return signals.stream()
                .map(signal -> {
                    ResearchTaskDO task = taskMap.get(signal.getTaskId());
                    ResearchReportDO report = reportMap.get(signal.getTaskId());
                    RiskWarningDO warning = riskWarningMap.get(signal.getTaskId());
                    return itemAssembler.fromDomainSignal(
                            signal,
                            task,
                            report,
                            followUpManager.resolveSummary(
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

    private List<StrategySignalListItemVO> listStrategySignalRecordsFromReports(StrategySignalPageQueryDTO queryDTO,
                                                                               Set<String> excludedTaskIds) {
        List<ResearchReportDO> reports = readManager.loadActiveReports();

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

        Map<String, ResearchTaskDO> taskMap = readManager.loadTaskMap(taskIds);
        Map<String, RiskWarningDO> riskWarningMap = readManager.loadLatestRiskWarningMapByTaskIds(taskIds);
        Map<String, List<RiskWarningDetailDO>> riskWarningDetailMap = readManager.loadRiskWarningDetailMapByWarningIds(
                riskWarningMap.values().stream()
                        .map(RiskWarningDO::getWarningId)
                        .filter(warningId -> warningId != null && !warningId.isBlank())
                        .collect(Collectors.toSet())
        );

        List<ResearchTaskDO> followUpTasks = readManager.loadStrategySignalFollowUpTasks();
        Map<String, List<ResearchTaskDO>> followUpTaskMapBySourceTaskId = readManager.groupFollowUpTasksBySourceTaskId(followUpTasks);
        Map<String, List<ResearchTaskDO>> followUpTaskMapBySourceReportId = readManager.groupFollowUpTasksBySourceReportId(followUpTasks);

        return reports.stream()
                .map(report -> {
                    ResearchTaskDO task = taskMap.get(report.getTaskId());
                    RiskWarningDO warning = riskWarningMap.get(report.getTaskId());
                    return itemAssembler.fromReport(
                            report,
                            task,
                            followUpManager.resolveSummary(
                                    task,
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
        if (Boolean.TRUE.equals(queryDTO.getOnlyHighConfidence()) && !ruleManager.isHighConfidence(item.getConfidenceScore())) {
            return false;
        }
        return true;
    }

    private boolean containsIgnoreCase(String source, String target) {
        return source != null && target != null && source.toLowerCase().contains(target.toLowerCase());
    }
}
