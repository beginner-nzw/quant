package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.dto.ResearchWorkbenchQueryDTO;
import com.quant.aiorchestrator.report.TaskReportProjection;
import com.quant.aiorchestrator.report.TaskReportReadPort;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.aiorchestrator.domain.vo.ResearchWorkbenchVO;
import com.quant.aiorchestrator.service.ResearchWorkbenchProjectionProvider;
import com.quant.common.model.enums.ReportReviewStatusEnum;
import com.quant.common.model.enums.TaskStatusEnum;
import com.quant.task.workbench.ResearchWorkbenchRiskDetailProjection;
import com.quant.task.workbench.ResearchWorkbenchRiskProjection;
import com.quant.task.workbench.ResearchWorkbenchStrategyProjection;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ResearchWorkbenchProjectionManager implements ResearchWorkbenchProjectionProvider {

    private final TaskQueryReadManager taskQueryReadManager;
    private final TaskReportReadPort taskReportReadPort;
    private final ResearchWorkbenchReadManager readManager;
    private final ResearchWorkbenchDispositionManager dispositionManager;
    private final ResearchWorkbenchRuleManager ruleManager;
    private final ResearchWorkbenchItemAssembler itemAssembler;

    public ResearchWorkbenchProjectionManager(TaskQueryReadManager taskQueryReadManager,
                                              TaskReportReadPort taskReportReadPort,
                                              ResearchWorkbenchReadManager readManager,
                                              ResearchWorkbenchDispositionManager dispositionManager,
                                              ResearchWorkbenchRuleManager ruleManager,
                                              ResearchWorkbenchItemAssembler itemAssembler) {
        this.taskQueryReadManager = taskQueryReadManager;
        this.taskReportReadPort = taskReportReadPort;
        this.readManager = readManager;
        this.dispositionManager = dispositionManager;
        this.ruleManager = ruleManager;
        this.itemAssembler = itemAssembler;
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

        List<ResearchTaskDO> tasks = taskQueryReadManager.listWorkbenchTasks(safeQuery.getTargetCode(), safeQuery.getTargetName());
        if (tasks.isEmpty()) {
            return vo;
        }

        ResearchTaskDO latestTask = tasks.get(0);
        Map<String, ResearchTaskDO> taskMap = tasks.stream()
                .filter(item -> item.getTaskId() != null && !item.getTaskId().isBlank())
                .collect(Collectors.toMap(ResearchTaskDO::getTaskId, item -> item, (left, right) -> left));
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

        List<TaskReportProjection> reports = taskReportReadPort.listReportsByTaskIdSet(taskIds);
        Map<String, ResearchWorkbenchRiskProjection> riskWarningMap = readManager.loadLatestRiskWarningMapByTaskIds(taskIds);
        Map<String, List<ResearchWorkbenchRiskDetailProjection>> riskWarningDetailMap = readManager.loadRiskWarningDetailMapByWarningIds(
                riskWarningMap.values().stream()
                        .map(ResearchWorkbenchRiskProjection::warningId)
                        .filter(warningId -> warningId != null && !warningId.isBlank())
                        .collect(Collectors.toSet())
        );
        Map<String, ResearchWorkbenchStrategyProjection> strategySignalMap = readManager.loadLatestStrategySignalMapByTaskIds(taskIds);

        vo.setReportCount((long) reports.size());
        vo.setHighConfidenceReportCount(reports.stream().filter(item -> ruleManager.isHighConfidence(item.getConfidenceScore() == null ? null : item.getConfidenceScore().doubleValue())).count());
        vo.setPendingReviewCount(reports.stream().filter(item -> ReportReviewStatusEnum.PENDING == ruleManager.resolveReviewStatus(item.getReviewStatus())).count());
        dispositionManager.populateDispositionSummaries(vo, tasks, reports, taskMap, riskWarningMap, strategySignalMap);

        Map<String, TaskReportProjection> latestReportMap = reports.stream().collect(Collectors.toMap(
                TaskReportProjection::getTaskId,
                item -> item,
                (left, right) -> left
        ));

        if (!reports.isEmpty()) {
            TaskReportProjection latestReport = reports.get(0);
            ResearchWorkbenchRiskProjection latestWarning = riskWarningMap.get(latestReport.getTaskId());
            vo.setLatestInsight(itemAssembler.toResearchWorkbenchInsight(
                    latestReport,
                    taskMap.get(latestReport.getTaskId()),
                    latestWarning,
                    latestWarning == null ? List.of() : riskWarningDetailMap.getOrDefault(latestWarning.warningId(), List.of()),
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
}
