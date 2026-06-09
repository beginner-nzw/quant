package com.quant.aiorchestrator.audit;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface AuditComplianceWorkflowProvider {

    Map<String, AuditComplianceWorkflowProjection> loadLatestWorkflowInstanceMapByTaskIds(Set<String> taskIds);

    Map<String, List<AuditComplianceAgentExecutionProjection>> loadAgentExecutionMapByTaskIds(Set<String> taskIds);
}
