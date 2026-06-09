package com.quant.audit.port;

public interface TaskControlAuditRecordPort {

    void recordCancelAudit(String taskId, String operatorId, String cancelReason);
}
