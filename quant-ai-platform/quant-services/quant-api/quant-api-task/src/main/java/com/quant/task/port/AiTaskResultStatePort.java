package com.quant.task.port;

import com.quant.common.model.message.AiTaskResultMessage;
import com.quant.task.api.AiTaskStateSnapshot;

public interface AiTaskResultStatePort {

    AiTaskStateSnapshot selectTask(String taskId);

    int updateFinalState(AiTaskResultMessage message, AiTaskStateSnapshot currentTask, String finalStage);

    void updateRetryLogStatus(AiTaskResultMessage message, String retryStatus);

    String retryStatusForFinalStatus(String finalStatus);
}
