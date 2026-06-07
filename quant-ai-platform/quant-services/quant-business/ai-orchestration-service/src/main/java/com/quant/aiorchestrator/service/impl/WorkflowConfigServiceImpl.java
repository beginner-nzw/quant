package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.domain.dto.WorkflowConfigUpdateDTO;
import com.quant.aiorchestrator.domain.vo.WorkflowConfigItemVO;
import com.quant.aiorchestrator.manager.WorkflowConfigPolicyManager;
import com.quant.aiorchestrator.manager.WorkflowConfigStoreManager;
import com.quant.aiorchestrator.service.ConfigChangeAuditService;
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
    private final ConfigChangeAuditService configChangeAuditService;

    public WorkflowConfigServiceImpl(
            @Value("${quant.ai.workflow-config:../../../ai-config/workflow-configs.json}") String workflowConfigPath,
            WorkflowConfigStoreManager configStoreManager,
            WorkflowConfigPolicyManager policyManager,
            ConfigChangeAuditService configChangeAuditService
    ) {
        this.workflowConfigPath = workflowConfigPath;
        this.configStoreManager = configStoreManager;
        this.policyManager = policyManager;
        this.configChangeAuditService = configChangeAuditService;
    }

    public List<WorkflowConfigItemVO> loadWorkflows() {
        return policyManager.toWorkflowItems(configStoreManager.readWorkflows(workflowConfigPath));
    }

    public WorkflowConfigItemVO resolveWorkflow(String taskType) {
        return policyManager.resolveWorkflow(taskType, loadWorkflows());
    }

    public void saveWorkflow(String workflowCode, WorkflowConfigUpdateDTO dto) {
        Path configPath = configStoreManager.resolveConfigPath(workflowConfigPath);
        WorkflowConfigPolicyManager.WorkflowConfigUpdatePlan plan = policyManager.buildUpdatePlan(
                workflowCode,
                dto,
                configStoreManager.readWorkflows(workflowConfigPath)
        );
        configChangeAuditService.appendAudit(
                "WORKFLOW_CONFIG",
                plan.workflowCode(),
                plan.workflowCode(),
                "UPDATE",
                configPath.toString(),
                "更新工作流配置",
                plan.changedFields()
        );
        configStoreManager.writeWorkflows(configPath, plan.workflows());
    }

    public String resolveConfigPathForDisplay() {
        return configStoreManager.resolveConfigPath(workflowConfigPath).toString();
    }
}
