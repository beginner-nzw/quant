package com.quant.task.port;

import com.quant.aiorchestrator.domain.dto.TaskCancelDTO;

public interface TaskControlAuditAppender {

    void recordCancelAudit(String taskId, TaskCancelDTO dto, String cancelReason);
}
