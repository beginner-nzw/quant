package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.vo.AgentConfigItemVO;
import com.quant.aiorchestrator.domain.vo.ModelStrategyItemVO;
import com.quant.aiorchestrator.domain.vo.PromptTemplateItemVO;
import com.quant.aiorchestrator.domain.vo.ToolWhitelistItemVO;
import com.quant.aiorchestrator.domain.vo.WorkflowConfigItemVO;
import com.quant.aiorchestrator.service.PromptTemplateConfigService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ModelAgentDefaultCatalogManager {

    private final PromptTemplateConfigService promptTemplateConfigService;

    public ModelAgentDefaultCatalogManager(PromptTemplateConfigService promptTemplateConfigService) {
        this.promptTemplateConfigService = promptTemplateConfigService;
    }

    public List<ToolWhitelistItemVO> defaultToolWhitelists() {
        return List.of(
                buildToolWhitelist("task_control_service.check_cancelled", "任务取消检查", "RUNTIME_GUARD", "ALL_AGENTS", "执行前统一检查任务是否已取消"),
                buildToolWhitelist("market_data_service.load_financial_data", "财务数据加载", "DATA_SERVICE", "financial_analysis_agent", "当前接最小财务数据占位实现"),
                buildToolWhitelist("timeout_executor.run_with_timeout", "节点超时控制", "RUNTIME_GUARD", "WORKFLOW_NODE", "对每个 LangGraph 节点做超时保护")
        );
    }

    public List<PromptTemplateItemVO> defaultPromptTemplates() {
        return List.of(
                buildPromptTemplate("planner_agent_template", "任务规划模板", "planner_agent", List.of("task_type", "target_code"), "当前为内联规则模板，未拆独立 Prompt 文件"),
                buildPromptTemplate("intent_agent_template", "意图识别模板", "intent_agent", List.of("target_name", "task_type"), "当前为内联规则模板，输出分析模式和关注维度"),
                buildPromptTemplate("financial_analysis_agent_template", "财务分析模板", "financial_analysis_agent", List.of("target_code", "financial_data"), "当前为规则占位模板，结合最小财务数据结构"),
                buildPromptTemplate("risk_review_agent_template", "风险审查模板", "risk_review_agent", List.of("financial_result", "target_name"), "当前为规则占位模板，输出固定风险审查结构"),
                buildPromptTemplate("report_generation_agent_template", "报告生成模板", "report_generation_agent", List.of("financial_result", "risk_result", "target_name"), "当前为规则占位模板，生成结构化报告结果")
        );
    }

    public List<AgentConfigItemVO> defaultAgents() {
        return List.of(
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
    }

    public List<WorkflowConfigItemVO> defaultWorkflows() {
        return List.of(
                buildWorkflowConfig(
                        "stock_research_workflow",
                        "1.0.0",
                        "planner_agent",
                        List.of("planner_agent", "intent_agent", "evidence_collection_agent", "financial_analysis_agent", "risk_review_agent", "report_generation_agent"),
                        "planner=5s, intent=5s, evidence=5s, financial=10s, risk=10s, report=10s",
                        "当前唯一启用的 LangGraph 串行研究工作流"
                )
        );
    }

    public List<ModelStrategyItemVO> defaultModelStrategies() {
        return List.of(
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
}
