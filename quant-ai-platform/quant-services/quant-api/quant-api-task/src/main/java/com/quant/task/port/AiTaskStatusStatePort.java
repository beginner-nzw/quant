package com.quant.task.port;

import com.quant.task.api.AiTaskStateSnapshot;

public interface AiTaskStatusStatePort {

    AiTaskStateSnapshot selectTask(String taskId);

    int updateTaskStage(String taskId, String status, String currentStage);
}
