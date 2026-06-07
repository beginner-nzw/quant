package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.dto.TaskCancelDTO;
import com.quant.aiorchestrator.domain.entity.AuditRecordDO;
import com.quant.aiorchestrator.mapper.AuditRecordMapper;
import com.quant.common.model.TaskDomainConstants;
import com.quant.common.model.enums.TaskStageEnum;
import com.quant.common.model.message.AiTaskActorProvenance;
import com.quant.common.model.message.AiTaskActorProvenanceSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TaskControlAuditManager {

    private final AuditRecordMapper auditRecordMapper;

    public void recordCancelAudit(String taskId, TaskCancelDTO dto, String cancelReason) {
        AiTaskActorProvenance provenance = AiTaskActorProvenanceSupport.userInitiated(
                "ai-orchestration-service",
                dto == null ? null : dto.getOperatorId(),
                null
        );

        AuditRecordDO audit = new AuditRecordDO();
        audit.setAuditId(UUID.randomUUID().toString());
        audit.setTaskId(taskId);
        audit.setAuditType(TaskDomainConstants.AuditType.TASK_CONTROL.name());
        audit.setAuditStage(TaskStageEnum.CANCELLED.name());
        audit.setOperatorType(TaskDomainConstants.AuditOperatorType.HUMAN.name());
        audit.setOperatorId(dto == null ? null : dto.getOperatorId());
        audit.setIdentitySource(AiTaskActorProvenanceSupport.identitySource(provenance));
        audit.setRoleSource(AiTaskActorProvenanceSupport.roleSource(provenance));
        audit.setServicePrincipal(AiTaskActorProvenanceSupport.servicePrincipal(provenance));
        audit.setOriginalActorId(AiTaskActorProvenanceSupport.originalActorId(provenance));
        audit.setDelegatedActorId(AiTaskActorProvenanceSupport.delegatedActorId(provenance));
        audit.setActionCode(TaskDomainConstants.AuditActionCode.TASK_CANCEL.name());
        audit.setActionDesc(cancelReason);
        audit.setResultStatus(TaskDomainConstants.AuditResultStatus.SUCCESS.name());
        audit.setRemark(cancelReason);
        audit.setDeleted(0);
        auditRecordMapper.insert(audit);
    }
}
