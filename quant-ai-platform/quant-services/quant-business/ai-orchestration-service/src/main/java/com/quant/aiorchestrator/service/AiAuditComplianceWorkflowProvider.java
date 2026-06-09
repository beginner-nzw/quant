package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.audit.AuditComplianceAgentExecutionProjection;
import com.quant.aiorchestrator.audit.AuditComplianceWorkflowProjection;
import com.quant.aiorchestrator.audit.AuditComplianceWorkflowProvider;
import com.quant.aiorchestrator.domain.entity.AiAgentExecutionDO;
import com.quant.aiorchestrator.domain.entity.AiWorkflowInstanceDO;
import com.quant.aiorchestrator.manager.TaskCrossDomainReadManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AiAuditComplianceWorkflowProvider implements AuditComplianceWorkflowProvider {

    private final TaskCrossDomainReadManager taskCrossDomainReadManager;

    @Override
    public Map<String, AuditComplianceWorkflowProjection> loadLatestWorkflowInstanceMapByTaskIds(Set<String> taskIds) {
        return taskCrossDomainReadManager.loadLatestWorkflowInstanceMapByTaskIds(taskIds)
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> toWorkflowProjection(entry.getValue()),
                        (left, right) -> left
                ));
    }

    @Override
    public Map<String, List<AuditComplianceAgentExecutionProjection>> loadAgentExecutionMapByTaskIds(Set<String> taskIds) {
        return taskCrossDomainReadManager.loadAgentExecutionMapByTaskIds(taskIds)
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream().map(this::toAgentProjection).toList(),
                        (left, right) -> left
                ));
    }

    private AuditComplianceWorkflowProjection toWorkflowProjection(AiWorkflowInstanceDO workflow) {
        return new AuditComplianceWorkflowProjection(
                workflow.getWorkflowInstanceId(),
                workflow.getWorkflowCode(),
                workflow.getWorkflowVersion(),
                workflow.getStatus(),
                workflow.getCurrentNode()
        );
    }

    private AuditComplianceAgentExecutionProjection toAgentProjection(AiAgentExecutionDO execution) {
        return new AuditComplianceAgentExecutionProjection(
                execution.getInputRef(),
                execution.getOutputRef(),
                execution.getDecisionRef(),
                execution.getNeedHumanReview()
        );
    }
}
