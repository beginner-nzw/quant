package com.quant.common.model;

public final class TaskDomainConstants {

    private TaskDomainConstants() {
    }

    public enum RetrySource {
        MANUAL
    }

    public enum RetryStatus {
        SUBMITTED,
        DISPATCHED,
        SUCCESS,
        FAILED,
        CANCELLED
    }

    public enum AuditType {
        TASK_CONTROL,
        AI_TASK_AUDIT
    }

    public enum AuditOperatorType {
        HUMAN,
        AGENT
    }

    public enum AuditStage {
        CANCELLED,
        WORKFLOW_FINISHED
    }

    public enum AuditActionCode {
        TASK_CANCEL,
        AUDIT_SUMMARY
    }

    public enum AuditResultStatus {
        SUCCESS,
        FAILED
    }
}
