package com.quant.aiorchestrator.audit;

public record AuditComplianceAgentExecutionProjection(
        String inputRef,
        String outputRef,
        String decisionRef,
        Integer needHumanReview
) {
}
