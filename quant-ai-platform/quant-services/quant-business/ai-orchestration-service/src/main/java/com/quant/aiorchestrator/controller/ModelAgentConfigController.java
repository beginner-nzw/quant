package com.quant.aiorchestrator.controller;

import com.quant.aiorchestrator.domain.dto.AgentConfigUpdateDTO;
import com.quant.aiorchestrator.domain.dto.EventAutoTriggerRuleUpdateDTO;
import com.quant.aiorchestrator.domain.dto.EventSourceConfigUpdateDTO;
import com.quant.aiorchestrator.domain.dto.ModelStrategyUpdateDTO;
import com.quant.aiorchestrator.domain.dto.PromptTemplateUpdateDTO;
import com.quant.aiorchestrator.domain.dto.RoleAccessConfigUpdateDTO;
import com.quant.aiorchestrator.domain.dto.WorkflowConfigUpdateDTO;
import com.quant.aiorchestrator.domain.vo.ModelAgentConfigCenterVO;
import com.quant.aiorchestrator.domain.vo.RoleAccessConfigItemVO;
import com.quant.aiorchestrator.service.AgentConfigService;
import com.quant.aiorchestrator.service.AuditConfigDashboardQueryService;
import com.quant.aiorchestrator.service.EventAutoTriggerConfigService;
import com.quant.aiorchestrator.service.EventSourceConfigService;
import com.quant.aiorchestrator.service.ModelAgentConfigDashboardQueryService;
import com.quant.aiorchestrator.service.ModelStrategyConfigService;
import com.quant.aiorchestrator.service.PromptTemplateConfigService;
import com.quant.aiorchestrator.service.RoleAccessConfigService;
import com.quant.aiorchestrator.service.WorkflowConfigService;
import com.quant.common.core.model.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class ModelAgentConfigController {

    private final AuditConfigDashboardQueryService auditConfigDashboardQueryService;
    private final ModelAgentConfigDashboardQueryService modelAgentConfigDashboardQueryService;
    private final EventAutoTriggerConfigService eventAutoTriggerConfigService;
    private final EventSourceConfigService eventSourceConfigService;
    private final PromptTemplateConfigService promptTemplateConfigService;
    private final ModelStrategyConfigService modelStrategyConfigService;
    private final AgentConfigService agentConfigService;
    private final WorkflowConfigService workflowConfigService;
    private final RoleAccessConfigService roleAccessConfigService;

    @GetMapping("/model-agent-config")
    public Result<ModelAgentConfigCenterVO> getModelAgentConfigCenter() {
        roleAccessConfigService.requirePermission(RoleAccessConfigService.PERMISSION_MODEL_AGENT_CONFIG_VIEW);
        return Result.success(modelAgentConfigDashboardQueryService.getModelAgentConfigCenter());
    }

    @GetMapping("/role-access-configs")
    public Result<List<RoleAccessConfigItemVO>> getRoleAccessConfigs() {
        return Result.success(auditConfigDashboardQueryService.listRoleAccessConfigs());
    }

    @PostMapping("/model-agent-config/prompt-templates/{templateCode}")
    public Result<String> updatePromptTemplate(@PathVariable("templateCode") String templateCode,
                                               @RequestBody PromptTemplateUpdateDTO dto) {
        roleAccessConfigService.requirePermission(RoleAccessConfigService.PERMISSION_MODEL_AGENT_CONFIG_EDIT);
        promptTemplateConfigService.saveTemplateContent(templateCode, dto == null ? null : dto.getTemplateContent());
        return Result.success("保存成功");
    }

    @PostMapping("/model-agent-config/model-strategies/{strategyCode}")
    public Result<String> updateModelStrategy(@PathVariable("strategyCode") String strategyCode,
                                              @RequestBody ModelStrategyUpdateDTO dto) {
        roleAccessConfigService.requirePermission(RoleAccessConfigService.PERMISSION_MODEL_AGENT_CONFIG_EDIT);
        modelStrategyConfigService.saveStrategy(strategyCode, dto);
        return Result.success("保存成功");
    }

    @PostMapping("/model-agent-config/event-auto-trigger-rules/{ruleCode}")
    public Result<String> updateEventAutoTriggerRule(@PathVariable("ruleCode") String ruleCode,
                                                     @RequestBody EventAutoTriggerRuleUpdateDTO dto) {
        roleAccessConfigService.requirePermission(RoleAccessConfigService.PERMISSION_MODEL_AGENT_CONFIG_EDIT);
        eventAutoTriggerConfigService.saveRule(ruleCode, dto);
        return Result.success("保存成功");
    }

    @PostMapping("/model-agent-config/event-sources/{sourceCode}")
    public Result<String> updateEventSourceConfig(@PathVariable("sourceCode") String sourceCode,
                                                  @RequestBody EventSourceConfigUpdateDTO dto) {
        roleAccessConfigService.requirePermission(RoleAccessConfigService.PERMISSION_MODEL_AGENT_CONFIG_EDIT);
        eventSourceConfigService.saveSource(sourceCode, dto);
        return Result.success("保存成功");
    }

    @PostMapping("/model-agent-config/agents/{agentCode}")
    public Result<String> updateAgentConfig(@PathVariable("agentCode") String agentCode,
                                            @RequestBody AgentConfigUpdateDTO dto) {
        roleAccessConfigService.requirePermission(RoleAccessConfigService.PERMISSION_MODEL_AGENT_CONFIG_EDIT);
        agentConfigService.saveAgent(agentCode, dto);
        return Result.success("保存成功");
    }

    @PostMapping("/model-agent-config/workflows/{workflowCode}")
    public Result<String> updateWorkflowConfig(@PathVariable("workflowCode") String workflowCode,
                                               @RequestBody WorkflowConfigUpdateDTO dto) {
        roleAccessConfigService.requirePermission(RoleAccessConfigService.PERMISSION_MODEL_AGENT_CONFIG_EDIT);
        workflowConfigService.saveWorkflow(workflowCode, dto);
        return Result.success("保存成功");
    }

    @PostMapping("/model-agent-config/role-access/{roleCode}")
    public Result<String> updateRoleAccessConfig(@PathVariable("roleCode") String roleCode,
                                                 @RequestBody RoleAccessConfigUpdateDTO dto) {
        roleAccessConfigService.requirePermission(RoleAccessConfigService.PERMISSION_MODEL_AGENT_CONFIG_EDIT);
        roleAccessConfigService.saveRole(roleCode, dto);
        return Result.success("保存成功");
    }
}
