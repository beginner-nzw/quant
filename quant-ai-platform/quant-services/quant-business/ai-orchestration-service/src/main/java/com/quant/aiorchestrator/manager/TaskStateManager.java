package com.quant.aiorchestrator.manager;

import com.quant.common.model.enums.TaskStatusEnum;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class TaskStateManager {

    private static final Set<TaskStatusEnum> FINAL_STATES = Set.of(
            TaskStatusEnum.SUCCESS,
            TaskStatusEnum.FAILED,
            TaskStatusEnum.CANCELLED
    );

    private static final Map<TaskStatusEnum, Set<TaskStatusEnum>> ALLOWED = Map.of(
            TaskStatusEnum.INIT, Set.of(TaskStatusEnum.DISPATCHED, TaskStatusEnum.CANCELLED),
            TaskStatusEnum.DISPATCHED, Set.of(TaskStatusEnum.RUNNING, TaskStatusEnum.FAILED, TaskStatusEnum.CANCELLED, TaskStatusEnum.SUCCESS),
            TaskStatusEnum.RUNNING, Set.of(TaskStatusEnum.RUNNING, TaskStatusEnum.SUCCESS, TaskStatusEnum.FAILED, TaskStatusEnum.CANCELLED),
            TaskStatusEnum.SUCCESS, Set.of(),
            TaskStatusEnum.FAILED, Set.of(),
            TaskStatusEnum.CANCELLED, Set.of()
    );

    public boolean canTransfer(String current, String target) {
        TaskStatusEnum targetStatus = TaskStatusEnum.from(target);
        if (targetStatus == null) {
            return false;
        }
        if (current == null || current.isBlank()) {
            return true;
        }
        TaskStatusEnum currentStatus = TaskStatusEnum.from(current);
        if (currentStatus == null) {
            return false;
        }
        if (FINAL_STATES.contains(currentStatus)) {
            return false;
        }
        return ALLOWED.getOrDefault(currentStatus, Set.of()).contains(targetStatus);
    }

    public boolean isFinalState(String state) {
        TaskStatusEnum taskStatus = TaskStatusEnum.from(state);
        return taskStatus != null && FINAL_STATES.contains(taskStatus);
    }

    public boolean canAcceptProgressUpdate(String current) {
        TaskStatusEnum currentStatus = TaskStatusEnum.from(current);
        return currentStatus == TaskStatusEnum.DISPATCHED || currentStatus == TaskStatusEnum.RUNNING;
    }

    public boolean canRetry(String current) {
        return TaskStatusEnum.FAILED == TaskStatusEnum.from(current);
    }
}
