package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class ModelAgentConfigCenterVO {

    private String currentAccessRole;
    private Boolean editable;
    private ModelAgentConfigStatsVO stats;
    private EngineRuntimeConfigVO engineRuntime;
    private List<WorkflowConfigItemVO> workflows;
    private List<AgentConfigItemVO> agents;
    private List<ModelStrategyItemVO> modelStrategies;
    private EventAutoTriggerConfigVO eventAutoTriggerConfig;
    private EventSourceConfigVO eventSourceConfig;
    private List<PromptTemplateItemVO> promptTemplates;
    private List<ToolWhitelistItemVO> toolWhitelists;
    private List<RoleAccessConfigItemVO> roleAccessConfigs;
    private List<ConfigChangeAuditItemVO> configChangeAudits;
}
