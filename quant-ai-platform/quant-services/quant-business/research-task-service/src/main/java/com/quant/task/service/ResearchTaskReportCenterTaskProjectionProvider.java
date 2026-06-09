package com.quant.task.service;

import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.aiorchestrator.manager.TaskQueryReadManager;
import com.quant.aiorchestrator.report.ReportCenterTaskProjection;
import com.quant.aiorchestrator.report.ReportCenterTaskProjectionProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ResearchTaskReportCenterTaskProjectionProvider implements ReportCenterTaskProjectionProvider {

    private final TaskQueryReadManager taskQueryReadManager;

    @Override
    public Map<String, ReportCenterTaskProjection> loadTaskMapByTaskIds(Set<String> taskIds) {
        return taskQueryReadManager.loadTaskMapByTaskIds(taskIds)
                .values()
                .stream()
                .collect(Collectors.toMap(
                        ResearchTaskDO::getTaskId,
                        this::toProjection,
                        (left, right) -> left
                ));
    }

    private ReportCenterTaskProjection toProjection(ResearchTaskDO task) {
        return new ReportCenterTaskProjection(
                task.getTaskId(),
                task.getTaskTitle(),
                task.getTaskType(),
                task.getTargetCode(),
                task.getTargetName(),
                task.getPriority(),
                task.getCreatedAt()
        );
    }
}
