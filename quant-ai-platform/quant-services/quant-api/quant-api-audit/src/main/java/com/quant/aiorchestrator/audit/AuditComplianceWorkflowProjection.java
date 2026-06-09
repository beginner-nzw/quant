package com.quant.aiorchestrator.audit;

public record AuditComplianceWorkflowProjection(
        String workflowInstanceId,
        String workflowCode,
        String workflowVersion,
        String status,
        String currentNode
) {
}
