package com.quant.aiorchestrator.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quant.aiorchestrator.domain.entity.RiskWarningDO;
import com.quant.aiorchestrator.domain.entity.RiskWarningDetailDO;
import com.quant.aiorchestrator.mapper.RiskWarningDetailMapper;
import com.quant.aiorchestrator.mapper.RiskWarningMapper;
import com.quant.aiorchestrator.report.TaskReportProjection;
import com.quant.aiorchestrator.report.TaskReportReadPort;
import com.quant.task.risk.RiskWarningTaskProjection;
import com.quant.task.risk.RiskWarningTaskReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RiskWarningReadManager {

    private final RiskWarningTaskReadPort taskReadPort;
    private final TaskReportReadPort taskReportReadPort;
    private final RiskWarningMapper riskWarningMapper;
    private final RiskWarningDetailMapper riskWarningDetailMapper;

    public List<RiskWarningDO> loadActiveRiskWarnings() {
        return riskWarningMapper.selectList(
                new LambdaQueryWrapper<RiskWarningDO>()
                        .eq(RiskWarningDO::getDeleted, 0)
                        .orderByDesc(RiskWarningDO::getCreatedAt, RiskWarningDO::getId)
        );
    }

    public List<TaskReportProjection> loadActiveReports() {
        return taskReportReadPort.listActiveReports();
    }

    public Map<String, RiskWarningTaskProjection> loadTaskMap(Set<String> taskIds) {
        return taskReadPort.loadTaskMapByTaskIds(taskIds);
    }

    public Map<String, TaskReportProjection> loadReportMapByTaskIds(Set<String> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return taskReportReadPort.listReportsByTaskIdSet(taskIds).stream().collect(Collectors.toMap(
                TaskReportProjection::getTaskId,
                item -> item,
                (left, right) -> left
        ));
    }

    public Map<String, RiskWarningDO> loadLatestRiskWarningMapByTaskIds(Set<String> taskIds) {
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

    public Map<String, List<RiskWarningDetailDO>> loadRiskWarningDetailMapByWarningIds(Set<String> warningIds) {
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

    public List<RiskWarningTaskProjection> loadRiskWarningFollowUpTasks() {
        return taskReadPort.loadRiskWarningFollowUpTasks();
    }

    public Map<String, List<RiskWarningTaskProjection>> groupFollowUpTasksBySourceTaskId(List<RiskWarningTaskProjection> followUpTasks) {
        if (followUpTasks == null || followUpTasks.isEmpty()) {
            return Collections.emptyMap();
        }
        return followUpTasks.stream()
                .filter(item -> item.sourceTaskId() != null && !item.sourceTaskId().isBlank())
                .collect(Collectors.groupingBy(RiskWarningTaskProjection::sourceTaskId));
    }

    public Map<String, List<RiskWarningTaskProjection>> groupFollowUpTasksBySourceReportId(List<RiskWarningTaskProjection> followUpTasks) {
        if (followUpTasks == null || followUpTasks.isEmpty()) {
            return Collections.emptyMap();
        }
        return followUpTasks.stream()
                .filter(item -> item.sourceReportId() != null && !item.sourceReportId().isBlank())
                .collect(Collectors.groupingBy(RiskWarningTaskProjection::sourceReportId));
    }
}
