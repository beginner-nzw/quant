package com.quant.aiorchestrator.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.dto.*;
import com.quant.aiorchestrator.domain.entity.*;
import com.quant.aiorchestrator.domain.vo.*;
import com.quant.aiorchestrator.mapper.*;
import com.quant.aiorchestrator.service.AgentConfigService;
import com.quant.aiorchestrator.service.ConfigChangeAuditService;
import com.quant.aiorchestrator.service.EventAutoTriggerConfigService;
import com.quant.aiorchestrator.service.EventSourceConfigService;
import com.quant.aiorchestrator.service.MarketEventIngestHistoryService;
import com.quant.aiorchestrator.service.ModelAgentConfigDashboardQueryService;
import com.quant.aiorchestrator.service.ModelStrategyConfigService;
import com.quant.aiorchestrator.service.PromptTemplateConfigService;
import com.quant.aiorchestrator.service.RoleAccessConfigService;
import com.quant.aiorchestrator.service.WorkflowConfigService;
import com.quant.aiorchestrator.util.CacheKeyUtil;
import com.quant.common.core.exception.BizException;
import com.quant.common.model.enums.MarketIntelligenceTypeEnum;
import com.quant.common.model.enums.ReportReviewStatusEnum;
import com.quant.common.model.enums.RiskLevelEnum;
import com.quant.common.model.enums.SignalDirectionEnum;
import com.quant.common.model.enums.SignalStrengthEnum;
import com.quant.common.model.enums.TaskStatusEnum;
import com.quant.common.redis.RedisKeyConstants;
import com.quant.common.redis.RedisKeyBuilder;
import com.quant.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ModelAgentConfigDashboardQueryServiceImpl implements ModelAgentConfigDashboardQueryService {

    private final AgentConfigService agentConfigService;
    private final ConfigChangeAuditService configChangeAuditService;
    private final EventAutoTriggerConfigService eventAutoTriggerConfigService;
    private final MarketEventIngestHistoryService marketEventIngestHistoryService;
    private final EventSourceConfigService eventSourceConfigService;
    private final ModelStrategyConfigService modelStrategyConfigService;
    private final PromptTemplateConfigService promptTemplateConfigService;
    private final WorkflowConfigService workflowConfigService;
    private final RoleAccessConfigService roleAccessConfigService;

@Override
    public ModelAgentConfigCenterVO getModelAgentConfigCenter() {
        ModelAgentConfigCenterVO vo = new ModelAgentConfigCenterVO();
        vo.setCurrentAccessRole(SecurityUtils.currentUserRole());
        vo.setEditable(roleAccessConfigService.hasPermissionForCurrentRole(RoleAccessConfigService.PERMISSION_MODEL_AGENT_CONFIG_EDIT));

        List<ToolWhitelistItemVO> toolWhitelists = List.of(
                buildToolWhitelist("task_control_service.check_cancelled", "任务取消检查", "RUNTIME_GUARD", "ALL_AGENTS", "执行前统一检查任务是否已取消"),
                buildToolWhitelist("market_data_service.load_financial_data", "财务数据加载", "DATA_SERVICE", "financial_analysis_agent", "当前接最小财务数据占位实现"),
                buildToolWhitelist("timeout_executor.run_with_timeout", "节点超时控制", "RUNTIME_GUARD", "WORKFLOW_NODE", "对每个 LangGraph 节点做超时保护")
        );

        List<PromptTemplateItemVO> promptTemplates = List.of(
                buildPromptTemplate("planner_agent_template", "任务规划模板", "planner_agent", List.of("task_type", "target_code"), "当前为内联规则模板，未拆独立 Prompt 文件"),
                buildPromptTemplate("intent_agent_template", "意图识别模板", "intent_agent", List.of("target_name", "task_type"), "当前为内联规则模板，输出分析模式和关注维度"),
                buildPromptTemplate("financial_analysis_agent_template", "财务分析模板", "financial_analysis_agent", List.of("target_code", "financial_data"), "当前为规则占位模板，结合最小财务数据结构"),
                buildPromptTemplate("risk_review_agent_template", "风险审查模板", "risk_review_agent", List.of("financial_result", "target_name"), "当前为规则占位模板，输出固定风险审查结构"),
                buildPromptTemplate("report_generation_agent_template", "报告生成模板", "report_generation_agent", List.of("financial_result", "risk_result", "target_name"), "当前为规则占位模板，生成结构化报告结果")
        );

        List<AgentConfigItemVO> agents = List.of(
                buildAgentConfig("planner_agent", "Planner Agent", "PLANNING", 1, 5, false,
                        List.of("task_control_service.check_cancelled"),
                        List.of("task_id", "task_type", "target_code"),
                        List.of("current_stage", "current_node", "agent_audits"),
                        "负责接单和初始任务规划"),
                buildAgentConfig("intent_agent", "Intent Agent", "INTENT_UNDERSTANDING", 2, 5, false,
                        List.of("task_control_service.check_cancelled"),
                        List.of("task_id", "task_type", "target_name"),
                        List.of("intent_result", "current_stage", "agent_audits"),
                        "负责识别分析模式和关注维度"),
                buildAgentConfig("evidence_collection_agent", "Evidence Collection Agent", "EVIDENCE_COLLECTION", 3, 5, false,
                        List.of("task_control_service.check_cancelled", "market_data_service.load_financial_data"),
                        List.of("task_id", "target_code", "source_context"),
                        List.of("evidence_items", "evidence_refs", "market_context", "current_stage", "agent_audits"),
                        "负责汇总来源事件、来源报告和同标的市场快照，生成结构化证据条目"),
                buildAgentConfig("financial_analysis_agent", "Financial Analysis Agent", "FINANCIAL_ANALYSIS", 4, 10, false,
                        List.of("task_control_service.check_cancelled", "market_data_service.load_financial_data"),
                        List.of("task_id", "target_code", "target_name"),
                        List.of("financial_result", "current_stage", "agent_audits"),
                        "当前接最小财务数据占位实现，支持 FAIL001 / TIMEOUT001 测试分支"),
                buildAgentConfig("risk_review_agent", "Risk Review Agent", "RISK_REVIEW", 5, 10, false,
                        List.of("task_control_service.check_cancelled"),
                        List.of("financial_result", "target_name"),
                        List.of("risk_result", "current_stage", "agent_audits"),
                        "负责风险等级和风险点审查"),
                buildAgentConfig("report_generation_agent", "Report Generation Agent", "REPORT_GENERATION", 6, 10, false,
                        List.of("task_control_service.check_cancelled"),
                        List.of("financial_result", "risk_result", "target_name"),
                        List.of("report_result", "evidence_refs", "status", "agent_audits"),
                        "负责汇总生成结构化研究报告")
        );

        List<WorkflowConfigItemVO> ignoredWorkflows = List.of(
                buildWorkflowConfig(
                        "stock_research_workflow",
                        "1.0.0",
                        "planner_agent",
                        List.of("planner_agent", "intent_agent", "evidence_collection_agent", "financial_analysis_agent", "risk_review_agent", "report_generation_agent"),
                        "planner=5s, intent=5s, evidence=5s, financial=10s, risk=10s, report=10s",
                        "当前唯一启用的 LangGraph 串行研究工作流"
                )
        );

        List<WorkflowConfigItemVO> workflows = workflowConfigService.loadWorkflows();
        if (workflows.isEmpty()) {
            workflows = ignoredWorkflows;
        }

        List<AgentConfigItemVO> configuredAgents = agentConfigService.loadAgents();
        if (!configuredAgents.isEmpty()) {
            agents = configuredAgents;
        }
        workflows = applyAgentConfigsToWorkflows(workflows, agents);

        List<ModelStrategyItemVO> modelStrategies = List.of(
                buildModelStrategy(
                        "analysis_rule_engine",
                        "STOCK_RESEARCH_ANALYSIS",
                        "BUILTIN",
                        "RULE_PLACEHOLDER",
                        "LOCAL_INLINE",
                        true,
                        "financial_analysis_agent_template",
                        List.of("planner_agent", "intent_agent", "financial_analysis_agent"),
                        "当前未接外部大模型 SDK，使用内置规则/占位逻辑"
                ),
                buildModelStrategy(
                        "review_rule_engine",
                        "RISK_REVIEW_AND_REPORT",
                        "BUILTIN",
                        "RULE_PLACEHOLDER",
                        "LOCAL_INLINE",
                        true,
                        "risk_review_agent_template",
                        List.of("risk_review_agent", "report_generation_agent"),
                        "当前未接外部大模型 SDK，使用内置规则/占位逻辑"
                )
        );

        modelStrategies = modelStrategyConfigService.loadStrategies();
        EventAutoTriggerConfigVO eventAutoTriggerConfig = eventAutoTriggerConfigService.loadConfigView();
        EventSourceConfigVO eventSourceConfig = eventSourceConfigService.loadConfigView();
        enrichEventSourceConfigStats(eventSourceConfig);
        List<ConfigChangeAuditItemVO> configChangeAudits = configChangeAuditService.loadRecentAudits();
        List<RoleAccessConfigItemVO> roleAccessConfigs = roleAccessConfigService.loadRoles();

        EngineRuntimeConfigVO engineRuntime = new EngineRuntimeConfigVO();
        engineRuntime.setEngineCode("python-ai-engine");
        engineRuntime.setEnv("local");
        engineRuntime.setHost("0.0.0.0");
        engineRuntime.setPort(8090);
        engineRuntime.setWorkflowTimeoutSeconds(resolveWorkflowTimeoutSeconds(workflows, agents, 60));
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

    private WorkflowConfigItemVO buildWorkflowConfig(String workflowCode,
                                                     String workflowVersion,
                                                     String entryAgent,
                                                     List<String> nodeSequence,
                                                     String nodeTimeoutSummary,
                                                     String remark) {
        WorkflowConfigItemVO vo = new WorkflowConfigItemVO();
        vo.setWorkflowCode(workflowCode);
        vo.setWorkflowVersion(workflowVersion);
        vo.setWorkflowType("LANGGRAPH_STATE_GRAPH");
        vo.setEntryAgent(entryAgent);
        vo.setNodeCount(nodeSequence.size());
        vo.setEnabled(true);
        vo.setDefaultSelected(true);
        vo.setNodeSequence(nodeSequence);
        vo.setNodeTimeoutSummary(nodeTimeoutSummary);
        vo.setRemark(remark);
        return vo;
    }

    private AgentConfigItemVO buildAgentConfig(String agentCode,
                                               String agentName,
                                               String stageCode,
                                               Integer executionOrder,
                                               Integer timeoutSeconds,
                                               boolean needHumanReview,
                                               List<String> toolWhitelist,
                                               List<String> inputKeys,
                                               List<String> outputKeys,
                                               String remark) {
        AgentConfigItemVO vo = new AgentConfigItemVO();
        vo.setAgentCode(agentCode);
        vo.setAgentName(agentName);
        vo.setStageCode(stageCode);
        vo.setExecutionOrder(executionOrder);
        vo.setEnabled(true);
        vo.setTimeoutSeconds(timeoutSeconds);
        vo.setNeedHumanReview(needHumanReview);
        vo.setImplementationMode("PYTHON_RULE_PLACEHOLDER");
        vo.setVersion("1.0.0");
        vo.setToolWhitelist(toolWhitelist);
        vo.setInputKeys(inputKeys);
        vo.setOutputKeys(outputKeys);
        vo.setRemark(remark);
        return vo;
    }

    private List<WorkflowConfigItemVO> applyAgentConfigsToWorkflows(List<WorkflowConfigItemVO> workflows,
                                                                    List<AgentConfigItemVO> agents) {
        if (workflows == null || workflows.isEmpty()) {
            return List.of();
        }
        Map<String, AgentConfigItemVO> agentMap = agents == null
                ? Collections.emptyMap()
                : agents.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getAgentCode() != null && !item.getAgentCode().isBlank())
                .collect(Collectors.toMap(AgentConfigItemVO::getAgentCode, item -> item, (left, right) -> right, LinkedHashMap::new));

        List<WorkflowConfigItemVO> result = new ArrayList<>();
        for (WorkflowConfigItemVO item : workflows) {
            WorkflowConfigItemVO workflow = new WorkflowConfigItemVO();
            BeanUtils.copyProperties(item, workflow);

            List<String> currentSequence = item.getNodeSequence() == null ? List.of() : item.getNodeSequence();
            List<String> effectiveSequence = currentSequence.stream()
                    .filter(agentCode -> {
                        AgentConfigItemVO config = agentMap.get(agentCode);
                        if (config == null) {
                            return true;
                        }
                        if ("report_generation_agent".equals(agentCode)) {
                            return true;
                        }
                        return !Boolean.FALSE.equals(config.getEnabled());
                    })
                    .toList();

            if (effectiveSequence.isEmpty()) {
                effectiveSequence = currentSequence;
            }

            workflow.setNodeSequence(currentSequence);
            workflow.setNodeCount(currentSequence.size());
            workflow.setEntryAgent(effectiveSequence.isEmpty() ? item.getEntryAgent() : effectiveSequence.get(0));
            workflow.setEnabled(!Boolean.FALSE.equals(item.getEnabled()));
            workflow.setNodeTimeoutSummary(buildWorkflowTimeoutSummary(effectiveSequence, agentMap));
            result.add(workflow);
        }
        return result;
    }

    private String buildWorkflowTimeoutSummary(List<String> nodeSequence, Map<String, AgentConfigItemVO> agentMap) {
        if (nodeSequence == null || nodeSequence.isEmpty()) {
            return "";
        }
        return nodeSequence.stream()
                .map(agentCode -> {
                    AgentConfigItemVO config = agentMap.get(agentCode);
                    Integer timeoutSeconds = config == null ? null : config.getTimeoutSeconds();
                    if (timeoutSeconds == null) {
                        return agentCode;
                    }
                    return agentCode + "=" + timeoutSeconds + "s";
                })
                .collect(Collectors.joining(", "));
    }

    private Integer resolveWorkflowTimeoutSeconds(List<WorkflowConfigItemVO> workflows,
                                                  List<AgentConfigItemVO> agents,
                                                  int fallbackSeconds) {
        if (workflows == null || workflows.isEmpty()) {
            return fallbackSeconds;
        }
        Map<String, AgentConfigItemVO> agentMap = agents == null
                ? Collections.emptyMap()
                : agents.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getAgentCode() != null && !item.getAgentCode().isBlank())
                .collect(Collectors.toMap(AgentConfigItemVO::getAgentCode, item -> item, (left, right) -> right, LinkedHashMap::new));

        WorkflowConfigItemVO workflow = workflows.stream()
                .filter(item -> !Boolean.FALSE.equals(item.getEnabled()))
                .filter(item -> Boolean.TRUE.equals(item.getDefaultSelected()))
                .findFirst()
                .orElseGet(() -> workflows.stream()
                        .filter(item -> !Boolean.FALSE.equals(item.getEnabled()))
                        .findFirst()
                        .orElse(workflows.get(0)));

        List<String> nodeSequence = (workflow.getNodeSequence() == null ? List.<String>of() : workflow.getNodeSequence()).stream()
                .filter(agentCode -> {
                    if ("report_generation_agent".equals(agentCode)) {
                        return true;
                    }
                    AgentConfigItemVO config = agentMap.get(agentCode);
                    return config == null || !Boolean.FALSE.equals(config.getEnabled());
                })
                .toList();
        int total = 0;
        for (String agentCode : nodeSequence) {
            AgentConfigItemVO config = agentMap.get(agentCode);
            if (config != null && config.getTimeoutSeconds() != null) {
                total += config.getTimeoutSeconds();
            }
        }
        return total > 0 ? total : fallbackSeconds;
    }

    private ModelStrategyItemVO buildModelStrategy(String strategyCode,
                                                   String scenarioCode,
                                                   String provider,
                                                   String modelName,
                                                   String accessMode,
                                                   boolean placeholder,
                                                   String promptTemplateCode,
                                                   List<String> boundAgents,
                                                   String remark) {
        ModelStrategyItemVO vo = new ModelStrategyItemVO();
        vo.setStrategyCode(strategyCode);
        vo.setScenarioCode(scenarioCode);
        vo.setProvider(provider);
        vo.setModelName(modelName);
        vo.setAccessMode(accessMode);
        vo.setEnabled(true);
        vo.setPlaceholder(placeholder);
        vo.setPromptTemplateCode(promptTemplateCode);
        vo.setBoundAgents(boundAgents);
        vo.setRemark(remark);
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

    private PromptTemplateItemVO buildPromptTemplate(String templateCode,
                                                     String templateName,
                                                     String boundAgentCode,
                                                     List<String> variables,
                                                     String remark) {
        PromptTemplateItemVO vo = new PromptTemplateItemVO();
        vo.setTemplateCode(templateCode);
        vo.setTemplateName(templateName);
        vo.setVersion("1.0.0");
        vo.setSourceType("FILE_SYSTEM_PROMPT");
        vo.setEditable(true);
        vo.setEnabled(true);
        vo.setBoundAgentCode(boundAgentCode);
        vo.setVariables(variables);
        vo.setTemplatePath(promptTemplateConfigService.resolveTemplatePathForDisplay(templateCode));
        vo.setTemplateContent(promptTemplateConfigService.loadTemplateContent(templateCode));
        vo.setRemark(remark);
        return vo;
    }

    private ToolWhitelistItemVO buildToolWhitelist(String toolCode,
                                                   String toolName,
                                                   String toolType,
                                                   String scope,
                                                   String remark) {
        ToolWhitelistItemVO vo = new ToolWhitelistItemVO();
        vo.setToolCode(toolCode);
        vo.setToolName(toolName);
        vo.setToolType(toolType);
        vo.setEnabled(true);
        vo.setScope(scope);
        vo.setRemark(remark);
        return vo;
    }

private void enrichEventSourceConfigStats(EventSourceConfigVO eventSourceConfig) {
        if (eventSourceConfig == null || eventSourceConfig.getSources() == null || eventSourceConfig.getSources().isEmpty()) {
            return;
        }
        List<MarketEventIngestHistoryItemVO> histories = marketEventIngestHistoryService.loadRecentHistory();
        if (histories.isEmpty()) {
            return;
        }

        Map<String, List<MarketEventIngestHistoryItemVO>> grouped = histories.stream()
                .filter(item -> item.getSourceCode() != null && !item.getSourceCode().isBlank())
                .collect(Collectors.groupingBy(MarketEventIngestHistoryItemVO::getSourceCode, LinkedHashMap::new, Collectors.toList()));

        for (EventSourceConfigItemVO source : eventSourceConfig.getSources()) {
            if (source == null || source.getSourceCode() == null || source.getSourceCode().isBlank()) {
                continue;
            }
            List<MarketEventIngestHistoryItemVO> sourceHistories = grouped.get(source.getSourceCode());
            if (sourceHistories == null || sourceHistories.isEmpty()) {
                source.setIngestRecordCount(0);
                source.setTotalCount(0);
                source.setSuccessCount(0);
                source.setFailedCount(0);
                source.setDuplicateCount(0);
                source.setAutoTriggeredCount(0);
                source.setLastIngestAt(null);
                source.setLastResultStatus(null);
                source.setLastErrorMessage(null);
                continue;
            }
            MarketEventIngestHistoryItemVO latestHistory = sourceHistories.get(0);
            source.setIngestRecordCount(sourceHistories.size());
            source.setTotalCount(sourceHistories.stream().mapToInt(item -> defaultInt(item.getTotalCount())).sum());
            source.setSuccessCount(sourceHistories.stream().mapToInt(item -> defaultInt(item.getSuccessCount())).sum());
            source.setFailedCount(sourceHistories.stream().mapToInt(item -> defaultInt(item.getFailedCount())).sum());
            source.setDuplicateCount(sourceHistories.stream().mapToInt(item -> defaultInt(item.getDuplicateCount())).sum());
            source.setAutoTriggeredCount(sourceHistories.stream().mapToInt(item -> defaultInt(item.getAutoTriggeredCount())).sum());
            source.setLastIngestAt(sourceHistories.stream()
                    .map(MarketEventIngestHistoryItemVO::getCreatedAt)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null));
            source.setLastResultStatus(latestHistory == null ? null : latestHistory.getResultStatus());
            source.setLastErrorMessage(latestHistory == null ? null : latestHistory.getErrorMessage());
        }
    }

private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }
}
