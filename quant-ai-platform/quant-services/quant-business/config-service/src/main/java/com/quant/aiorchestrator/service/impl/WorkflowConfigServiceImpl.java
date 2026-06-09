package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.configstore.ConfigStoreAuditAppender;
import com.quant.aiorchestrator.domain.dto.WorkflowConfigUpdateDTO;
import com.quant.aiorchestrator.domain.vo.WorkflowConfigItemVO;
import com.quant.aiorchestrator.manager.WorkflowConfigPolicyManager;
import com.quant.aiorchestrator.manager.WorkflowConfigStoreManager;
import com.quant.aiorchestrator.service.WorkflowConfigService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

@Service
public class WorkflowConfigServiceImpl implements WorkflowConfigService {

    private final String workflowConfigPath;
    private final WorkflowConfigStoreManager configStoreManager;
    private final WorkflowConfigPolicyManager policyManager;
    private final ConfigStoreAuditAppender configStoreAuditAppender;

    public WorkflowConfigServiceImpl(
            @Value("${quant.ai.workflow-config:../../../ai-config/workflow-configs.json}") String workflowConfigPath,
            WorkflowConfigStoreManager configStoreManager,
            WorkflowConfigPolicyManager policyManager,
            ConfigStoreAuditAppender configStoreAuditAppender
    ) {
        this.workflowConfigPath = workflowConfigPath;
        this.configStoreManager = configStoreManager;
        this.policyManager = policyManager;
        this.configStoreAuditAppender = configStoreAuditAppender;
    }

    @Override
    public List<WorkflowConfigItemVO> loadWorkflows() {
        return policyManager.toWorkflowItems(configStoreManager.readWorkflows(workflowConfigPath));
    }

    @Override
    public WorkflowConfigItemVO resolveWorkflow(String taskType) {
        return policyManager.resolveWorkflow(taskType, loadWorkflows());
    }

    @Override
    public void saveWorkflow(String workflowCode, WorkflowConfigUpdateDTO dto) {
        Path configPath = configStoreManager.resolveConfigPath(workflowConfigPath);
        WorkflowConfigPolicyManager.WorkflowConfigUpdatePlan plan = policyManager.buildUpdatePlan(
                workflowCode,
                dto,
                configStoreManager.readWorkflows(workflowConfigPath)
        );
        configStoreAuditAppender.appendAudit(
                "WORKFLOW_CONFIG",
                plan.workflowCode(),
                plan.workflowCode(),
                "UPDATE",
                configPath.toString(),
                "Update workflow config",
                plan.changedFields()
        );
        configStoreManager.writeWorkflows(configPath, plan.workflows());
    }

    @Override
    public String resolveConfigPathForDisplay() {
        return configStoreManager.resolveConfigPath(workflowConfigPath).toString();
    }
}
