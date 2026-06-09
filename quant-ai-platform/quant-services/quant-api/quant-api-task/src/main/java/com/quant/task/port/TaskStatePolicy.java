package com.quant.task.port;

import com.quant.common.model.enums.TaskStatusEnum;

import java.util.Map;
import java.util.Set;

public interface TaskStatePolicy {

    Set<TaskStatusEnum> FINAL_STATES = Set.of(
            TaskStatusEnum.SUCCESS,
            TaskStatusEnum.FAILED,
            TaskStatusEnum.CANCELLED
    );

    Map<TaskStatusEnum, Set<TaskStatusEnum>> ALLOWED = Map.of(
            TaskStatusEnum.INIT, Set.of(TaskStatusEnum.DISPATCHED, TaskStatusEnum.CANCELLED),
            TaskStatusEnum.DISPATCHED, Set.of(TaskStatusEnum.RUNNING, TaskStatusEnum.FAILED, TaskStatusEnum.CANCELLED, TaskStatusEnum.SUCCESS),
            TaskStatusEnum.RUNNING, Set.of(TaskStatusEnum.RUNNING, TaskStatusEnum.SUCCESS, TaskStatusEnum.FAILED, TaskStatusEnum.CANCELLED),
            TaskStatusEnum.SUCCESS, Set.of(),
            TaskStatusEnum.FAILED, Set.of(),
            TaskStatusEnum.CANCELLED, Set.of()
    );

    default boolean canTransfer(String current, String target) {
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

    default boolean isFinalState(String state) {
        TaskStatusEnum taskStatus = TaskStatusEnum.from(state);
        return taskStatus != null && FINAL_STATES.contains(taskStatus);
    }

    default boolean canAcceptProgressUpdate(String current) {
        TaskStatusEnum currentStatus = TaskStatusEnum.from(current);
        return currentStatus == TaskStatusEnum.DISPATCHED || currentStatus == TaskStatusEnum.RUNNING;
    }

    default boolean canRetry(String current) {
        return TaskStatusEnum.FAILED == TaskStatusEnum.from(current);
    }
}
