package com.quant.configservice;

import com.quant.aiorchestrator.configstore.ConfigStoreAuditAppender;
import com.quant.aiorchestrator.configstore.ConfigStoreKey;
import com.quant.aiorchestrator.configstore.GovernedConfigStore;
import com.quant.aiorchestrator.controller.GovernedConfigController;
import com.quant.aiorchestrator.controller.ModelAgentConfigController;
import com.quant.aiorchestrator.domain.dto.AgentConfigUpdateDTO;
import com.quant.aiorchestrator.domain.dto.ConfigRollbackDTO;
import com.quant.aiorchestrator.domain.dto.EventAutoTriggerRuleUpdateDTO;
import com.quant.aiorchestrator.domain.dto.EventSourceConfigUpdateDTO;
import com.quant.aiorchestrator.domain.dto.ModelStrategyUpdateDTO;
import com.quant.aiorchestrator.domain.dto.PromptTemplateUpdateDTO;
import com.quant.aiorchestrator.domain.dto.RoleAccessConfigUpdateDTO;
import com.quant.aiorchestrator.domain.dto.WorkflowConfigUpdateDTO;
import com.quant.aiorchestrator.domain.vo.AgentConfigItemVO;
import com.quant.aiorchestrator.domain.vo.ConfigChangeAuditItemVO;
import com.quant.aiorchestrator.domain.vo.ConfigRollbackResultVO;
import com.quant.aiorchestrator.domain.vo.EngineRuntimeConfigVO;
import com.quant.aiorchestrator.domain.vo.EventAutoTriggerConfigVO;
import com.quant.aiorchestrator.domain.vo.EventAutoTriggerRuleItemVO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigVO;
import com.quant.aiorchestrator.domain.vo.GovernedConfigVO;
import com.quant.aiorchestrator.domain.vo.ModelAgentConfigCenterVO;
import com.quant.aiorchestrator.domain.vo.ModelAgentConfigStatsVO;
import com.quant.aiorchestrator.domain.vo.ModelStrategyItemVO;
import com.quant.aiorchestrator.domain.vo.PromptTemplateItemVO;
import com.quant.aiorchestrator.domain.vo.RoleAccessConfigItemVO;
import com.quant.aiorchestrator.domain.vo.ToolWhitelistItemVO;
import com.quant.aiorchestrator.domain.vo.WorkflowConfigItemVO;
import com.quant.aiorchestrator.manager.AgentConfigPolicyManager;
import com.quant.aiorchestrator.manager.AgentConfigStoreManager;
import com.quant.aiorchestrator.manager.ConfigChangeAuditItemManager;
import com.quant.aiorchestrator.manager.ConfigChangeAuditStoreManager;
import com.quant.aiorchestrator.manager.EventAutoTriggerConfigManager;
import com.quant.aiorchestrator.manager.EventAutoTriggerConfigPolicyManager;
import com.quant.aiorchestrator.manager.EventAutoTriggerConfigStoreManager;
import com.quant.aiorchestrator.manager.EventSourceConfigCommandManager;
import com.quant.aiorchestrator.manager.EventSourceConfigPolicyManager;
import com.quant.aiorchestrator.manager.EventSourceConfigStoreManager;
import com.quant.aiorchestrator.manager.ModelAgentConfigDashboardManager;
import com.quant.aiorchestrator.manager.ModelAgentDefaultCatalogManager;
import com.quant.aiorchestrator.manager.ModelStrategyConfigCommandManager;
import com.quant.aiorchestrator.manager.ModelStrategyConfigItemManager;
import com.quant.aiorchestrator.manager.ModelStrategyConfigPolicyManager;
import com.quant.aiorchestrator.manager.ModelStrategyConfigStoreManager;
import com.quant.aiorchestrator.manager.PromptTemplateAuditManager;
import com.quant.aiorchestrator.manager.PromptTemplateCatalogManager;
import com.quant.aiorchestrator.manager.PromptTemplateFileManager;
import com.quant.aiorchestrator.manager.RoleAccessConfigPolicyManager;
import com.quant.aiorchestrator.manager.RoleAccessConfigStoreManager;
import com.quant.aiorchestrator.manager.WorkflowConfigAssemblyManager;
import com.quant.aiorchestrator.manager.WorkflowConfigPolicyManager;
import com.quant.aiorchestrator.manager.WorkflowConfigStoreManager;
import com.quant.aiorchestrator.service.AgentConfigService;
import com.quant.aiorchestrator.service.AuditConfigDashboardQueryService;
import com.quant.aiorchestrator.service.ConfigChangeAuditService;
import com.quant.aiorchestrator.service.EventAutoTriggerConfigService;
import com.quant.aiorchestrator.service.EventSourceConfigService;
import com.quant.aiorchestrator.service.EventSourceIngestStatsProvider;
import com.quant.aiorchestrator.service.ModelAgentConfigDashboardQueryService;
import com.quant.aiorchestrator.service.ModelStrategyConfigService;
import com.quant.aiorchestrator.service.PromptTemplateConfigService;
import com.quant.aiorchestrator.service.RoleAccessConfigService;
import com.quant.aiorchestrator.service.WorkflowConfigService;
import com.quant.aiorchestrator.service.impl.AgentConfigServiceImpl;
import com.quant.aiorchestrator.service.impl.AuditConfigDashboardQueryServiceImpl;
import com.quant.aiorchestrator.service.impl.ConfigChangeAuditServiceImpl;
import com.quant.aiorchestrator.service.impl.EventAutoTriggerConfigServiceImpl;
import com.quant.aiorchestrator.service.impl.EventSourceConfigServiceImpl;
import com.quant.aiorchestrator.service.impl.ModelAgentConfigDashboardQueryServiceImpl;
import com.quant.aiorchestrator.service.impl.ModelStrategyConfigServiceImpl;
import com.quant.aiorchestrator.service.impl.NoopEventSourceIngestStatsProvider;
import com.quant.aiorchestrator.service.impl.PromptTemplateConfigServiceImpl;
import com.quant.aiorchestrator.service.impl.RoleAccessConfigServiceImpl;
import com.quant.aiorchestrator.service.impl.WorkflowConfigServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigServiceBoundaryTests {

    @Test
    void configServiceOwnsGovernedStoreAndAgentConfigRuntime() {
        assertEquals("com.quant.aiorchestrator.configstore", ConfigStoreKey.class.getPackageName());
        assertEquals(ConfigStoreKey.class.getPackageName(), GovernedConfigStore.class.getPackageName());
        assertEquals(ConfigStoreKey.class.getPackageName(), ConfigStoreAuditAppender.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.controller", GovernedConfigController.class.getPackageName());
        assertEquals(GovernedConfigController.class.getPackageName(), ModelAgentConfigController.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.domain.dto", ConfigRollbackDTO.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.domain.vo", GovernedConfigVO.class.getPackageName());
        assertEquals(GovernedConfigVO.class.getPackageName(), ConfigRollbackResultVO.class.getPackageName());

        assertEquals("com.quant.aiorchestrator.service", AgentConfigService.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.service.impl", AgentConfigServiceImpl.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.manager", AgentConfigStoreManager.class.getPackageName());
        assertEquals(AgentConfigStoreManager.class.getPackageName(), AgentConfigPolicyManager.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.domain.dto", AgentConfigUpdateDTO.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.domain.vo", AgentConfigItemVO.class.getPackageName());

        assertEquals("com.quant.aiorchestrator.service", WorkflowConfigService.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.service.impl", WorkflowConfigServiceImpl.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.manager", WorkflowConfigStoreManager.class.getPackageName());
        assertEquals(WorkflowConfigStoreManager.class.getPackageName(), WorkflowConfigPolicyManager.class.getPackageName());
        assertEquals(WorkflowConfigStoreManager.class.getPackageName(), WorkflowConfigAssemblyManager.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.domain.dto", WorkflowConfigUpdateDTO.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.domain.vo", WorkflowConfigItemVO.class.getPackageName());

        assertEquals("com.quant.aiorchestrator.service", RoleAccessConfigService.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.service.impl", RoleAccessConfigServiceImpl.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.manager", RoleAccessConfigStoreManager.class.getPackageName());
        assertEquals(RoleAccessConfigStoreManager.class.getPackageName(), RoleAccessConfigPolicyManager.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.domain.dto", RoleAccessConfigUpdateDTO.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.domain.vo", RoleAccessConfigItemVO.class.getPackageName());

        assertEquals("com.quant.aiorchestrator.service", ModelStrategyConfigService.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.service.impl", ModelStrategyConfigServiceImpl.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.manager", ModelStrategyConfigStoreManager.class.getPackageName());
        assertEquals(ModelStrategyConfigStoreManager.class.getPackageName(), ModelStrategyConfigPolicyManager.class.getPackageName());
        assertEquals(ModelStrategyConfigStoreManager.class.getPackageName(), ModelStrategyConfigItemManager.class.getPackageName());
        assertEquals(ModelStrategyConfigStoreManager.class.getPackageName(), ModelStrategyConfigCommandManager.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.domain.dto", ModelStrategyUpdateDTO.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.domain.vo", ModelStrategyItemVO.class.getPackageName());

        assertEquals("com.quant.aiorchestrator.service", PromptTemplateConfigService.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.service.impl", PromptTemplateConfigServiceImpl.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.manager", PromptTemplateFileManager.class.getPackageName());
        assertEquals(PromptTemplateFileManager.class.getPackageName(), PromptTemplateCatalogManager.class.getPackageName());
        assertEquals(PromptTemplateFileManager.class.getPackageName(), PromptTemplateAuditManager.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.domain.dto", PromptTemplateUpdateDTO.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.domain.vo", PromptTemplateItemVO.class.getPackageName());

        assertEquals("com.quant.aiorchestrator.service", EventSourceConfigService.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.service.impl", EventSourceConfigServiceImpl.class.getPackageName());
        assertEquals(EventSourceConfigService.class.getPackageName(), AuditConfigDashboardQueryService.class.getPackageName());
        assertEquals(EventSourceConfigServiceImpl.class.getPackageName(), AuditConfigDashboardQueryServiceImpl.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.manager", EventSourceConfigStoreManager.class.getPackageName());
        assertEquals(EventSourceConfigStoreManager.class.getPackageName(), EventSourceConfigPolicyManager.class.getPackageName());
        assertEquals(EventSourceConfigStoreManager.class.getPackageName(), EventSourceConfigCommandManager.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.domain.dto", EventSourceConfigUpdateDTO.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.domain.vo", EventSourceConfigVO.class.getPackageName());
        assertEquals(EventSourceConfigVO.class.getPackageName(), EventSourceConfigItemVO.class.getPackageName());

        assertEquals("com.quant.aiorchestrator.service", EventAutoTriggerConfigService.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.service.impl", EventAutoTriggerConfigServiceImpl.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.manager", EventAutoTriggerConfigStoreManager.class.getPackageName());
        assertEquals(EventAutoTriggerConfigStoreManager.class.getPackageName(), EventAutoTriggerConfigPolicyManager.class.getPackageName());
        assertEquals(EventAutoTriggerConfigStoreManager.class.getPackageName(), EventAutoTriggerConfigManager.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.domain.dto", EventAutoTriggerRuleUpdateDTO.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.domain.vo", EventAutoTriggerConfigVO.class.getPackageName());
        assertEquals(EventAutoTriggerConfigVO.class.getPackageName(), EventAutoTriggerRuleItemVO.class.getPackageName());

        assertEquals("com.quant.aiorchestrator.service", ModelAgentConfigDashboardQueryService.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.service.impl", ModelAgentConfigDashboardQueryServiceImpl.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.service", ConfigChangeAuditService.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.service.impl", ConfigChangeAuditServiceImpl.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.service", EventSourceIngestStatsProvider.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.service.impl", NoopEventSourceIngestStatsProvider.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.manager", ModelAgentConfigDashboardManager.class.getPackageName());
        assertEquals(ModelAgentConfigDashboardManager.class.getPackageName(), ModelAgentDefaultCatalogManager.class.getPackageName());
        assertEquals(ModelAgentConfigDashboardManager.class.getPackageName(), ConfigChangeAuditItemManager.class.getPackageName());
        assertEquals(ModelAgentConfigDashboardManager.class.getPackageName(), ConfigChangeAuditStoreManager.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.domain.vo", ModelAgentConfigCenterVO.class.getPackageName());
        assertEquals(ModelAgentConfigCenterVO.class.getPackageName(), ModelAgentConfigStatsVO.class.getPackageName());
        assertEquals(ModelAgentConfigCenterVO.class.getPackageName(), EngineRuntimeConfigVO.class.getPackageName());
        assertEquals(ModelAgentConfigCenterVO.class.getPackageName(), ToolWhitelistItemVO.class.getPackageName());
        assertEquals(ModelAgentConfigCenterVO.class.getPackageName(), ConfigChangeAuditItemVO.class.getPackageName());
    }
}
