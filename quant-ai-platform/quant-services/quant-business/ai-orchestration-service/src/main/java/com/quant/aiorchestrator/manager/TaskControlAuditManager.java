package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.dto.TaskCancelDTO;
import com.quant.audit.port.TaskControlAuditRecordPort;
import com.quant.task.port.TaskControlAuditAppender;
import org.springframework.stereotype.Component;

@Component
public class TaskControlAuditManager implements TaskControlAuditAppender {

    private final TaskControlAuditRecordPort taskControlAuditRecordPort;

    public TaskControlAuditManager(TaskControlAuditRecordPort taskControlAuditRecordPort) {
        this.taskControlAuditRecordPort = taskControlAuditRecordPort;
    }

    public void recordCancelAudit(String taskId, TaskCancelDTO dto, String cancelReason) {
        taskControlAuditRecordPort.recordCancelAudit(
                taskId,
                dto == null ? null : dto.getOperatorId(),
                cancelReason
        );
    }
}
