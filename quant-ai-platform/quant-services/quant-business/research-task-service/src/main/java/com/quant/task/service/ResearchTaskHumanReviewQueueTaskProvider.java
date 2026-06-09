package com.quant.task.service;

import com.quant.aiorchestrator.audit.HumanReviewQueueTaskProjection;
import com.quant.aiorchestrator.audit.HumanReviewQueueTaskProvider;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.aiorchestrator.manager.TaskQueryReadManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ResearchTaskHumanReviewQueueTaskProvider implements HumanReviewQueueTaskProvider {

    private final TaskQueryReadManager taskQueryReadManager;

    @Override
    public Map<String, HumanReviewQueueTaskProjection> loadTaskMapByTaskIds(Set<String> taskIds) {
        return taskQueryReadManager.loadTaskMapByTaskIds(taskIds)
                .values()
                .stream()
                .collect(Collectors.toMap(
                        ResearchTaskDO::getTaskId,
                        this::toProjection,
                        (left, right) -> left
                ));
    }

    private HumanReviewQueueTaskProjection toProjection(ResearchTaskDO task) {
        return new HumanReviewQueueTaskProjection(
                task.getTaskId(),
                task.getTaskTitle(),
                task.getTaskType(),
                task.getTargetCode(),
                task.getTargetName(),
                task.getPriority()
        );
    }
}
