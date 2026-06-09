package com.quant.aiorchestrator.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.entity.AuditRecordDO;
import com.quant.aiorchestrator.mapper.AuditRecordMapper;
import com.quant.common.model.TaskDomainConstants;
import com.quant.common.model.message.AiTaskActorProvenance;
import com.quant.common.model.message.AiTaskActorProvenanceSupport;
import com.quant.common.model.message.AiTaskAuditMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AiTaskAuditRecordWriteManager {

    private static final int MAX_ACTION_DESC_LENGTH = 500;
    private static final int MAX_REMARK_LENGTH = 20000;

    private final AuditRecordMapper auditRecordMapper;
    private final ObjectMapper objectMapper;

    public void recordAiTaskAudit(AiTaskAuditMessage message) throws Exception {
        AuditRecordDO audit = new AuditRecordDO();
        AiTaskActorProvenance provenance = message.getPayload().getActorProvenance();
        audit.setAuditId(UUID.randomUUID().toString());
        audit.setTaskId(message.getTaskId());
        audit.setAuditType(TaskDomainConstants.AuditType.AI_TASK_AUDIT.name());
        audit.setAuditStage(TaskDomainConstants.AuditStage.WORKFLOW_FINISHED.name());
        audit.setOperatorType(TaskDomainConstants.AuditOperatorType.AGENT.name());
        audit.setOperatorId(defaultText(AiTaskActorProvenanceSupport.delegatedActorId(provenance), "python-ai-engine"));
        audit.setIdentitySource(AiTaskActorProvenanceSupport.identitySource(provenance));
        audit.setRoleSource(AiTaskActorProvenanceSupport.roleSource(provenance));
        audit.setServicePrincipal(AiTaskActorProvenanceSupport.servicePrincipal(provenance));
        audit.setOriginalActorId(AiTaskActorProvenanceSupport.originalActorId(provenance));
        audit.setDelegatedActorId(AiTaskActorProvenanceSupport.delegatedActorId(provenance));
        audit.setActionCode(TaskDomainConstants.AuditActionCode.AUDIT_SUMMARY.name());
        audit.setActionDesc(safeTruncate(message.getPayload().getReviewSuggestion(), MAX_ACTION_DESC_LENGTH));
        audit.setResultStatus(TaskDomainConstants.AuditResultStatus.SUCCESS.name());
        audit.setRemark(safeTruncate(objectMapper.writeValueAsString(message.getPayload()), MAX_REMARK_LENGTH));
        audit.setDeleted(0);
        auditRecordMapper.insert(audit);
    }

    private String safeTruncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
