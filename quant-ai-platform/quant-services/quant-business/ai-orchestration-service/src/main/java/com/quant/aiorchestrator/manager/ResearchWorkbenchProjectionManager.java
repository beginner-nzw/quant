package com.quant.aiorchestrator.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quant.aiorchestrator.domain.dto.*;
import com.quant.aiorchestrator.domain.entity.*;
import com.quant.aiorchestrator.domain.vo.*;
import com.quant.aiorchestrator.mapper.*;
import com.quant.common.model.enums.ReportReviewStatusEnum;
import com.quant.common.model.enums.TaskStatusEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ResearchWorkbenchProjectionManager {

    /*
     * Contract boundary: research workbench is a display aggregation only.
     * It may hydrate UI fields from task/report/risk/strategy read models, but
     * it must not define domain truth or feed command/projection decisions.
    */
    private final ResearchTaskMapper researchTaskMapper;
    private final ResearchReportMapper researchReportMapper;
    private final ResearchWorkbenchReadManager readManager;
    private final ResearchWorkbenchDispositionManager dispositionManager;
    private final StrategySignalRuleManager ruleManager;
    private final ResearchWorkbenchItemAssembler itemAssembler;

    @Autowired
    public ResearchWorkbenchProjectionManager(ResearchTaskMapper researchTaskMapper,
                                              ResearchReportMapper researchReportMapper,
                                              ResearchWorkbenchReadManager readManager,
                                              ResearchWorkbenchDispositionManager dispositionManager,
                                              StrategySignalRuleManager ruleManager) {
        this(
                researchTaskMapper,
                researchReportMapper,
                readManager,
                dispositionManager,
                ruleManager,
                new ResearchWorkbenchItemAssembler(ruleManager)
        );
    }

    public ResearchWorkbenchProjectionManager(ResearchTaskMapper researchTaskMapper,
                                              ResearchReportMapper researchReportMapper,
                                              ResearchWorkbenchReadManager readManager,
                                              ResearchWorkbenchDispositionManager dispositionManager,
                                              StrategySignalRuleManager ruleManager,
                                              ResearchWorkbenchItemAssembler itemAssembler) {
        this.researchTaskMapper = researchTaskMapper;
        this.researchReportMapper = researchReportMapper;
        this.readManager = readManager;
        this.dispositionManager = dispositionManager;
        this.ruleManager = ruleManager;
        this.itemAssembler = itemAssembler;
    }

    public ResearchWorkbenchProjectionManager(ResearchTaskMapper researchTaskMapper,
                                              ResearchReportMapper researchReportMapper,
                                              RiskWarningMapper riskWarningMapper,
                                              RiskWarningDetailMapper riskWarningDetailMapper,
                                              StrategySignalMapper strategySignalMapper,
                                              StrategySignalFactorMapper strategySignalFactorMapper,
                                              FollowUpTaskSummaryManager followUpManager,
                                              StrategySignalRuleManager ruleManager) {
        this(
                researchTaskMapper,
                researchReportMapper,
                new ResearchWorkbenchReadManager(
                        researchTaskMapper,
                        riskWarningMapper,
                        riskWarningDetailMapper,
                        strategySignalMapper
                ),
                new ResearchWorkbenchDispositionManager(
                        new ResearchWorkbenchReadManager(
                                researchTaskMapper,
                                riskWarningMapper,
                                riskWarningDetailMapper,
                                strategySignalMapper
                        ),
                        followUpManager,
                        ruleManager
                ),
                ruleManager
        );
    }

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
        vo.setRiskDispositionSummary(dispositionManager.emptyDispositionSummary("RISK_WARNING"));
        vo.setStrategySignalDispositionSummary(dispositionManager.emptyDispositionSummary("STRATEGY_SIGNAL"));
        vo.setMarketIntelligenceDispositionSummary(dispositionManager.emptyDispositionSummary("MARKET_INTELLIGENCE"));
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
                    .map(item -> itemAssembler.toResearchWorkbenchRecentTask(item, null))
                    .toList());
            return vo;
        }

        List<ResearchReportDO> reports = researchReportMapper.selectList(
                new LambdaQueryWrapper<ResearchReportDO>()
                        .eq(ResearchReportDO::getDeleted, 0)
                        .in(ResearchReportDO::getTaskId, taskIds)
                        .orderByDesc(ResearchReportDO::getCreatedAt, ResearchReportDO::getId)
        );
        Map<String, RiskWarningDO> riskWarningMap = readManager.loadLatestRiskWarningMapByTaskIds(taskIds);
        Map<String, List<RiskWarningDetailDO>> riskWarningDetailMap = readManager.loadRiskWarningDetailMapByWarningIds(
                riskWarningMap.values().stream()
                        .map(RiskWarningDO::getWarningId)
                        .filter(warningId -> warningId != null && !warningId.isBlank())
                        .collect(Collectors.toSet())
        );
        Map<String, StrategySignalDO> strategySignalMap = readManager.loadLatestStrategySignalMapByTaskIds(taskIds);

        vo.setReportCount((long) reports.size());
        vo.setHighConfidenceReportCount(reports.stream().filter(item -> ruleManager.isHighConfidence(item.getConfidenceScore() == null ? null : item.getConfidenceScore().doubleValue())).count());
        vo.setPendingReviewCount(reports.stream().filter(item -> ReportReviewStatusEnum.PENDING == ruleManager.resolveReviewStatus(item.getReviewStatus())).count());
        dispositionManager.populateDispositionSummaries(vo, tasks, reports, taskMap, riskWarningMap, strategySignalMap);

        Map<String, ResearchReportDO> latestReportMap = reports.stream().collect(Collectors.toMap(
                ResearchReportDO::getTaskId,
                item -> item,
                (left, right) -> left
        ));

        if (!reports.isEmpty()) {
            ResearchReportDO latestReport = reports.get(0);
            RiskWarningDO latestWarning = riskWarningMap.get(latestReport.getTaskId());
            vo.setLatestInsight(itemAssembler.toResearchWorkbenchInsight(
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
                .map(item -> itemAssembler.toResearchWorkbenchRecentTask(item, latestReportMap.get(item.getTaskId())))
                .toList());
        return vo;
    }

    private int resolveRecentTaskLimit(Integer recentTaskLimit) {
        if (recentTaskLimit == null || recentTaskLimit < 1) {
            return 6;
        }
        return Math.min(recentTaskLimit, 10);
    }

    /*
     * Preferred/fallback selection is display hydration only. It preserves the
     * existing UI precedence without creating a new authoritative source.
     */

}
