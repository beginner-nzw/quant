package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.vo.*;
import com.quant.aiorchestrator.service.AgentConfigService;
import com.quant.aiorchestrator.service.ConfigChangeAuditService;
import com.quant.aiorchestrator.service.EventAutoTriggerConfigService;
import com.quant.aiorchestrator.service.EventSourceIngestStatsProvider;
import com.quant.aiorchestrator.service.EventSourceConfigService;
import com.quant.aiorchestrator.service.ModelStrategyConfigService;
import com.quant.aiorchestrator.service.PromptTemplateConfigService;
import com.quant.aiorchestrator.service.RoleAccessConfigService;
import com.quant.aiorchestrator.service.WorkflowConfigService;
import com.quant.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ModelAgentConfigDashboardManager {

    private final AgentConfigService agentConfigService;
    private final ConfigChangeAuditService configChangeAuditService;
    private final EventAutoTriggerConfigService eventAutoTriggerConfigService;
    private final EventSourceConfigService eventSourceConfigService;
    private final ModelStrategyConfigService modelStrategyConfigService;
    private final PromptTemplateConfigService promptTemplateConfigService;
    private final WorkflowConfigService workflowConfigService;
    private final RoleAccessConfigService roleAccessConfigService;
    private final WorkflowConfigAssemblyManager workflowConfigAssemblyManager;
    private final EventSourceIngestStatsProvider eventSourceIngestStatsProvider;
    private final ModelAgentDefaultCatalogManager defaultCatalogManager;

    public ModelAgentConfigCenterVO getModelAgentConfigCenter() {
        ModelAgentConfigCenterVO vo = new ModelAgentConfigCenterVO();
        vo.setCurrentAccessRole(SecurityUtils.currentUserRole());
        vo.setEditable(roleAccessConfigService.hasPermissionForCurrentRole(RoleAccessConfigService.PERMISSION_MODEL_AGENT_CONFIG_EDIT));

        List<ToolWhitelistItemVO> toolWhitelists = defaultCatalogManager.defaultToolWhitelists();
        List<PromptTemplateItemVO> promptTemplates = defaultCatalogManager.defaultPromptTemplates();
        List<AgentConfigItemVO> agents = defaultCatalogManager.defaultAgents();
        List<WorkflowConfigItemVO> defaultWorkflows = defaultCatalogManager.defaultWorkflows();

        List<WorkflowConfigItemVO> workflows = workflowConfigService.loadWorkflows();
        if (workflows.isEmpty()) {
            workflows = defaultWorkflows;
        }

        List<AgentConfigItemVO> configuredAgents = agentConfigService.loadAgents();
        if (!configuredAgents.isEmpty()) {
            agents = configuredAgents;
        }
        workflows = workflowConfigAssemblyManager.applyAgentConfigsToWorkflows(workflows, agents);

        List<ModelStrategyItemVO> modelStrategies = modelStrategyConfigService.loadStrategies();
        EventAutoTriggerConfigVO eventAutoTriggerConfig = eventAutoTriggerConfigService.loadConfigView();
        EventSourceConfigVO eventSourceConfig = eventSourceConfigService.loadConfigView();
        eventSourceIngestStatsProvider.enrichEventSourceConfigStats(eventSourceConfig);
        List<ConfigChangeAuditItemVO> configChangeAudits = configChangeAuditService.loadRecentAudits();
        List<RoleAccessConfigItemVO> roleAccessConfigs = roleAccessConfigService.loadRoles();

        EngineRuntimeConfigVO engineRuntime = new EngineRuntimeConfigVO();
        engineRuntime.setEngineCode("python-ai-engine");
        engineRuntime.setEnv("local");
        engineRuntime.setHost("0.0.0.0");
        engineRuntime.setPort(8090);
        engineRuntime.setWorkflowTimeoutSeconds(workflowConfigAssemblyManager.resolveWorkflowTimeoutSeconds(workflows, agents, 60));
        engineRuntime.setConsumerGroup("python-ai-engine-group");
        engineRuntime.setKafkaBootstrapServers("127.0.0.1:19092");
        engineRuntime.setDispatchTopic("ai.task.dispatch");
        engineRuntime.setStatusTopic("ai.task.status");
        engineRuntime.setResultTopic("ai.task.result");
        engineRuntime.setAuditTopic("ai.task.audit");
        engineRuntime.setRedisEndpoint("127.0.0.1:6379/0");
        engineRuntime.setRuntimeMode(resolveRuntimeMode(modelStrategies));

        ModelAgentConfigStatsVO stats = new ModelAgentConfigStatsVO();
        stats.setWorkflowCount(workflows.size());
        stats.setActiveAgentCount((int) agents.stream().filter(item -> Boolean.TRUE.equals(item.getEnabled())).count());
        stats.setModelStrategyCount(modelStrategies.size());
        stats.setPromptTemplateCount(promptTemplates.size());
        stats.setToolWhitelistCount(toolWhitelists.size());
        stats.setPlaceholderStrategyCount((int) modelStrategies.stream().filter(item -> Boolean.TRUE.equals(item.getPlaceholder())).count());
        stats.setEventAutoTriggerRuleCount(eventAutoTriggerConfig.getRules() == null ? 0 : eventAutoTriggerConfig.getRules().size());
        stats.setEventSourceConfigCount(eventSourceConfig.getSources() == null ? 0 : eventSourceConfig.getSources().size());
        stats.setConfigAuditCount(configChangeAudits.size());
        stats.setRoleAccessConfigCount(roleAccessConfigs.size());

        vo.setStats(stats);
        vo.setEngineRuntime(engineRuntime);
        vo.setWorkflows(workflows);
        vo.setAgents(agents);
        vo.setModelStrategies(modelStrategies);
        vo.setEventAutoTriggerConfig(eventAutoTriggerConfig);
        vo.setEventSourceConfig(eventSourceConfig);
        vo.setPromptTemplates(promptTemplates);
        vo.setToolWhitelists(toolWhitelists);
        vo.setRoleAccessConfigs(roleAccessConfigs);
        vo.setConfigChangeAudits(configChangeAudits);
        return vo;
    }

    private String resolveRuntimeMode(List<ModelStrategyItemVO> modelStrategies) {
        if (modelStrategies == null || modelStrategies.isEmpty()) {
            return "RULE_PLACEHOLDER";
        }
        boolean hasModelStrategy = modelStrategies.stream()
                .anyMatch(item -> Boolean.TRUE.equals(item.getEnabled()) && !Boolean.TRUE.equals(item.getPlaceholder()));
        if (hasModelStrategy) {
            return "LANGCHAIN_WITH_FALLBACK";
        }
        return "RULE_PLACEHOLDER";
    }

}
