package com.quant.aiorchestrator.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.quant.aiorchestrator.domain.dto.AgentConfigUpdateDTO;
import com.quant.aiorchestrator.domain.dto.AuditCompliancePageQueryDTO;
import com.quant.aiorchestrator.domain.dto.EventAutoTriggerRuleUpdateDTO;
import com.quant.aiorchestrator.domain.dto.EventSourceConfigUpdateDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventBatchImportDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventCreateDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventMockIngestDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventPageQueryDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventSourceSyncDTO;
import com.quant.aiorchestrator.domain.dto.MarketIntelligencePageQueryDTO;
import com.quant.aiorchestrator.domain.dto.ModelStrategyUpdateDTO;
import com.quant.aiorchestrator.domain.dto.PromptTemplateUpdateDTO;
import com.quant.aiorchestrator.domain.dto.ReportCenterPageQueryDTO;
import com.quant.aiorchestrator.domain.dto.ResearchWorkbenchQueryDTO;
import com.quant.aiorchestrator.domain.dto.RoleAccessConfigUpdateDTO;
import com.quant.aiorchestrator.domain.dto.RiskWarningPageQueryDTO;
import com.quant.aiorchestrator.domain.dto.StrategySignalCreateDTO;
import com.quant.aiorchestrator.domain.dto.StrategySignalPageQueryDTO;
import com.quant.aiorchestrator.domain.dto.StrategySignalStatusUpdateDTO;
import com.quant.aiorchestrator.domain.dto.TaskCancelDTO;
import com.quant.aiorchestrator.domain.dto.TaskPageQueryDTO;
import com.quant.aiorchestrator.domain.dto.TaskReportReviewDTO;
import com.quant.aiorchestrator.domain.dto.TaskRetryDTO;
import com.quant.aiorchestrator.domain.dto.WorkflowConfigUpdateDTO;
import com.quant.aiorchestrator.domain.vo.AgentExecutionVO;
import com.quant.aiorchestrator.domain.vo.AuditCompliancePageVO;
import com.quant.aiorchestrator.domain.vo.AuditComplianceStatsVO;
import com.quant.aiorchestrator.domain.vo.AuditRecordVO;
import com.quant.aiorchestrator.domain.vo.CninfoProxyAnnouncementResponseVO;
import com.quant.aiorchestrator.domain.vo.MarketEventBatchImportResultVO;
import com.quant.aiorchestrator.domain.vo.MarketEventBatchPreviewResultVO;
import com.quant.aiorchestrator.domain.vo.MarketEventCreateResultVO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.aiorchestrator.domain.vo.EventSourcePreviewResultVO;
import com.quant.aiorchestrator.domain.vo.EventSourceRequestDiagnosticResultVO;
import com.quant.aiorchestrator.domain.vo.MarketEventIngestHistoryItemVO;
import com.quant.aiorchestrator.domain.vo.MarketEventListItemVO;
import com.quant.aiorchestrator.domain.vo.MarketEventPageVO;
import com.quant.aiorchestrator.domain.vo.MarketEventStatsVO;
import com.quant.aiorchestrator.domain.vo.MarketIntelligencePageVO;
import com.quant.aiorchestrator.domain.vo.MarketIntelligenceStatsVO;
import com.quant.aiorchestrator.domain.vo.ModelAgentConfigCenterVO;
import com.quant.aiorchestrator.domain.vo.ReportCenterPageVO;
import com.quant.aiorchestrator.domain.vo.ReportCenterStatsVO;
import com.quant.aiorchestrator.domain.vo.ReportReviewStatsVO;
import com.quant.aiorchestrator.domain.vo.ResearchWorkbenchVO;
import com.quant.aiorchestrator.domain.vo.RoleAccessConfigItemVO;
import com.quant.aiorchestrator.domain.vo.RiskWarningPageVO;
import com.quant.aiorchestrator.domain.vo.RiskWarningStatsVO;
import com.quant.aiorchestrator.domain.vo.StrategySignalFactorItemVO;
import com.quant.aiorchestrator.domain.vo.StrategySignalPageVO;
import com.quant.aiorchestrator.domain.vo.StrategySignalStatsVO;
import com.quant.aiorchestrator.domain.vo.TaskDetailVO;
import com.quant.aiorchestrator.domain.vo.TaskFullDetailVO;
import com.quant.aiorchestrator.domain.vo.TaskPageVO;
import com.quant.aiorchestrator.domain.vo.TaskReportReviewLogVO;
import com.quant.aiorchestrator.domain.vo.TaskReportVO;
import com.quant.aiorchestrator.domain.vo.TaskRetryLogVO;
import com.quant.aiorchestrator.domain.vo.TaskStateVO;
import com.quant.aiorchestrator.domain.vo.TaskStatsVO;
import com.quant.aiorchestrator.domain.vo.TaskStepVO;
import com.quant.aiorchestrator.domain.vo.WorkflowInstanceVO;
import com.quant.aiorchestrator.sentinel.TaskQuerySentinelBlockHandler;
import com.quant.aiorchestrator.service.AgentConfigService;
import com.quant.aiorchestrator.service.EventAutoTriggerConfigService;
import com.quant.aiorchestrator.service.EventSourceConfigService;
import com.quant.aiorchestrator.service.EventSourcePreviewService;
import com.quant.aiorchestrator.service.MarketEventService;
import com.quant.aiorchestrator.service.ModelStrategyConfigService;
import com.quant.aiorchestrator.service.PromptTemplateConfigService;
import com.quant.aiorchestrator.service.RoleAccessConfigService;
import com.quant.aiorchestrator.service.StrategySignalService;
import com.quant.aiorchestrator.service.TaskControlService;
import com.quant.aiorchestrator.service.TaskQueryService;
import com.quant.aiorchestrator.service.TaskReportService;
import com.quant.aiorchestrator.service.TaskRetryService;
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
public class TaskQueryController {

    private final TaskQueryService taskQueryService;
    private final TaskRetryService taskRetryService;
    private final TaskControlService taskControlService;
    private final TaskReportService taskReportService;
    private final MarketEventService marketEventService;
    private final EventAutoTriggerConfigService eventAutoTriggerConfigService;
    private final EventSourceConfigService eventSourceConfigService;
    private final EventSourcePreviewService eventSourcePreviewService;
    private final PromptTemplateConfigService promptTemplateConfigService;
    private final ModelStrategyConfigService modelStrategyConfigService;
    private final AgentConfigService agentConfigService;
    private final WorkflowConfigService workflowConfigService;
    private final RoleAccessConfigService roleAccessConfigService;
    private final StrategySignalService strategySignalService;

    @GetMapping("/{taskId}")
    public Result<TaskDetailVO> getTaskDetail(@PathVariable("taskId") String taskId) {
        return Result.success(taskQueryService.getTaskDetail(taskId));
    }

    @GetMapping("/{taskId}/state")
    public Result<TaskStateVO> getTaskState(@PathVariable("taskId") String taskId) {
        return Result.success(taskQueryService.getTaskState(taskId));
    }

    @GetMapping("/{taskId}/steps")
    public Result<List<TaskStepVO>> listTaskSteps(@PathVariable("taskId") String taskId) {
        return Result.success(taskQueryService.listTaskSteps(taskId));
    }

    @GetMapping("/{taskId}/workflow")
    public Result<WorkflowInstanceVO> getWorkflowInstance(@PathVariable("taskId") String taskId) {
        return Result.success(taskQueryService.getWorkflowInstance(taskId));
    }

    @GetMapping("/{taskId}/agents")
    public Result<List<AgentExecutionVO>> listAgentExecutions(@PathVariable("taskId") String taskId) {
        return Result.success(taskQueryService.listAgentExecutions(taskId));
    }

    @GetMapping("/{taskId}/audits")
    public Result<List<AuditRecordVO>> listAuditRecords(@PathVariable("taskId") String taskId) {
        return Result.success(taskQueryService.listAuditRecords(taskId));
    }

    @GetMapping
    @SentinelResource(
            value = "pageTasks",
            blockHandlerClass = TaskQuerySentinelBlockHandler.class,
            blockHandler = "handlePageTasksBlock"
    )
    public Result<TaskPageVO> pageTasks(TaskPageQueryDTO queryDTO) {
        return Result.success(taskQueryService.pageTasks(queryDTO));
    }

    @PostMapping("/{taskId}/retry")
    public Result<String> retryTask(@PathVariable("taskId") String taskId,
                                    @RequestBody(required = false) TaskRetryDTO dto) {
        roleAccessConfigService.requirePermission(RoleAccessConfigService.PERMISSION_TASK_RETRY);
        return Result.success(taskRetryService.retryTask(taskId, dto));
    }

    @GetMapping("/{taskId}/retries")
    public Result<List<TaskRetryLogVO>> listRetryLogs(@PathVariable("taskId") String taskId) {
        return Result.success(taskQueryService.listRetryLogs(taskId));
    }

    @GetMapping("/{taskId}/full")
    @SentinelResource(
            value = "getTaskFullDetail",
            blockHandlerClass = TaskQuerySentinelBlockHandler.class,
            blockHandler = "handleTaskFullDetailBlock"
    )
    public Result<TaskFullDetailVO> getTaskFullDetail(@PathVariable("taskId") String taskId) {
        return Result.success(taskQueryService.getTaskFullDetail(taskId));
    }

    @GetMapping("/stats")
    public Result<TaskStatsVO> getTaskStats() {
        return Result.success(taskQueryService.getTaskStats());
    }

    @GetMapping("/market-events")
    public Result<MarketEventPageVO> pageMarketEvents(MarketEventPageQueryDTO queryDTO) {
        return Result.success(marketEventService.pageMarketEvents(queryDTO));
    }

    @GetMapping("/market-event-stats")
    public Result<MarketEventStatsVO> getMarketEventStats() {
        return Result.success(marketEventService.getMarketEventStats());
    }

    @GetMapping("/market-events/{eventId}")
    public Result<MarketEventListItemVO> getMarketEvent(@PathVariable("eventId") String eventId) {
        return Result.success(marketEventService.getMarketEvent(eventId));
    }

    @GetMapping("/market-events/ingest-history")
    public Result<List<MarketEventIngestHistoryItemVO>> listMarketEventIngestHistory() {
        return Result.success(marketEventService.listMarketEventIngestHistory());
    }

    @GetMapping("/market-event-source-configs")
    public Result<List<EventSourceConfigItemVO>> listMarketEventSourceConfigs() {
        return Result.success(eventSourceConfigService.loadSources());
    }

    @PostMapping("/market-events")
    public Result<MarketEventCreateResultVO> createMarketEvent(@RequestBody MarketEventCreateDTO dto) {
        roleAccessConfigService.requirePermission(RoleAccessConfigService.PERMISSION_TASK_CREATE);
        return Result.success(marketEventService.createMarketEvent(dto));
    }

    @PostMapping("/market-events/batch-import/preview")
    public Result<MarketEventBatchPreviewResultVO> previewImportMarketEvents(@RequestBody MarketEventBatchImportDTO dto) {
        roleAccessConfigService.requirePermission(RoleAccessConfigService.PERMISSION_TASK_CREATE);
        return Result.success(marketEventService.previewImportMarketEvents(dto));
    }

    @PostMapping("/market-events/batch-import")
    public Result<MarketEventBatchImportResultVO> importMarketEvents(@RequestBody MarketEventBatchImportDTO dto) {
        roleAccessConfigService.requirePermission(RoleAccessConfigService.PERMISSION_TASK_CREATE);
        return Result.success(marketEventService.importMarketEvents(dto));
    }

    @PostMapping("/market-events/mock-ingest")
    public Result<MarketEventBatchImportResultVO> mockIngestMarketEvents(@RequestBody MarketEventMockIngestDTO dto) {
        roleAccessConfigService.requirePermission(RoleAccessConfigService.PERMISSION_TASK_CREATE);
        return Result.success(marketEventService.mockIngestMarketEvents(dto));
    }

    @PostMapping("/market-events/source-sync/{sourceCode}")
    public Result<MarketEventBatchImportResultVO> syncMarketEventSource(@PathVariable("sourceCode") String sourceCode,
                                                                        @RequestBody MarketEventSourceSyncDTO dto) {
        roleAccessConfigService.requirePermission(RoleAccessConfigService.PERMISSION_TASK_CREATE);
        return Result.success(marketEventService.syncMarketEventSource(sourceCode, dto));
    }

    @PostMapping("/market-events/source-preview/{sourceCode}")
    public Result<EventSourcePreviewResultVO> previewMarketEventSource(@PathVariable("sourceCode") String sourceCode,
                                                                       @RequestBody MarketEventSourceSyncDTO dto) {
        roleAccessConfigService.requirePermission(RoleAccessConfigService.PERMISSION_MODEL_AGENT_CONFIG_VIEW);
        return Result.success(eventSourcePreviewService.previewSource(sourceCode, dto));
    }

    @PostMapping("/market-events/source-diagnose/{sourceCode}")
    public Result<EventSourceRequestDiagnosticResultVO> diagnoseMarketEventSource(@PathVariable("sourceCode") String sourceCode,
                                                                                  @RequestBody MarketEventSourceSyncDTO dto) {
        roleAccessConfigService.requirePermission(RoleAccessConfigService.PERMISSION_MODEL_AGENT_CONFIG_VIEW);
        return Result.success(eventSourcePreviewService.diagnoseSource(sourceCode, dto));
    }

    @GetMapping("/market-events/cninfo-proxy")
    public Result<CninfoProxyAnnouncementResponseVO> previewCninfoProxyAnnouncements(MarketEventSourceSyncDTO dto) {
        return Result.success(marketEventService.previewCninfoProxyAnnouncements(dto));
    }

    @GetMapping("/risk-warnings")
    public Result<RiskWarningPageVO> pageRiskWarnings(RiskWarningPageQueryDTO queryDTO) {
        return Result.success(taskQueryService.pageRiskWarnings(queryDTO));
    }

    @GetMapping("/risk-warning-stats")
    public Result<RiskWarningStatsVO> getRiskWarningStats() {
        return Result.success(taskQueryService.getRiskWarningStats());
    }

    @GetMapping("/strategy-signals")
    public Result<StrategySignalPageVO> pageStrategySignals(StrategySignalPageQueryDTO queryDTO) {
        return Result.success(taskQueryService.pageStrategySignals(queryDTO));
    }

    @GetMapping("/strategy-signal-stats")
    public Result<StrategySignalStatsVO> getStrategySignalStats() {
        return Result.success(taskQueryService.getStrategySignalStats());
    }

    @PostMapping("/strategy-signals")
    public Result<String> createStrategySignal(@RequestBody StrategySignalCreateDTO dto) {
        roleAccessConfigService.requirePermission(RoleAccessConfigService.PERMISSION_REPORT_REVIEW);
        return Result.success(strategySignalService.createOrUpdate(dto));
    }

    @GetMapping("/strategy-signals/{signalId}/factors")
    public Result<List<StrategySignalFactorItemVO>> listStrategySignalFactors(@PathVariable("signalId") String signalId) {
        return Result.success(strategySignalService.listFactors(signalId));
    }

    @PostMapping("/strategy-signals/{signalId}/status")
    public Result<String> updateStrategySignalStatus(@PathVariable("signalId") String signalId,
                                                     @RequestBody StrategySignalStatusUpdateDTO dto) {
        roleAccessConfigService.requirePermission(RoleAccessConfigService.PERMISSION_REPORT_REVIEW);
        return Result.success(strategySignalService.updateStatus(signalId, dto));
    }

    @GetMapping("/report-center")
    public Result<ReportCenterPageVO> pageReportCenter(ReportCenterPageQueryDTO queryDTO) {
        return Result.success(taskQueryService.pageReportCenter(queryDTO));
    }

    @GetMapping("/report-center-stats")
    public Result<ReportCenterStatsVO> getReportCenterStats() {
        return Result.success(taskQueryService.getReportCenterStats());
    }

    @GetMapping("/market-intelligence")
    public Result<MarketIntelligencePageVO> pageMarketIntelligence(MarketIntelligencePageQueryDTO queryDTO) {
        return Result.success(taskQueryService.pageMarketIntelligence(queryDTO));
    }

    @GetMapping("/market-intelligence-stats")
    public Result<MarketIntelligenceStatsVO> getMarketIntelligenceStats() {
        return Result.success(taskQueryService.getMarketIntelligenceStats());
    }

    @GetMapping("/audit-compliance")
    public Result<AuditCompliancePageVO> pageAuditCompliance(AuditCompliancePageQueryDTO queryDTO) {
        roleAccessConfigService.requirePermission(RoleAccessConfigService.PERMISSION_AUDIT_COMPLIANCE_VIEW);
        return Result.success(taskQueryService.pageAuditCompliance(queryDTO));
    }

    @GetMapping("/audit-compliance-stats")
    public Result<AuditComplianceStatsVO> getAuditComplianceStats() {
        roleAccessConfigService.requirePermission(RoleAccessConfigService.PERMISSION_AUDIT_COMPLIANCE_VIEW);
        return Result.success(taskQueryService.getAuditComplianceStats());
    }

    @GetMapping("/model-agent-config")
    public Result<ModelAgentConfigCenterVO> getModelAgentConfigCenter() {
        roleAccessConfigService.requirePermission(RoleAccessConfigService.PERMISSION_MODEL_AGENT_CONFIG_VIEW);
        return Result.success(taskQueryService.getModelAgentConfigCenter());
    }

    @GetMapping("/role-access-configs")
    public Result<List<RoleAccessConfigItemVO>> getRoleAccessConfigs() {
        return Result.success(roleAccessConfigService.loadRoles());
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

    @GetMapping("/research-workbench")
    public Result<ResearchWorkbenchVO> getResearchWorkbench(ResearchWorkbenchQueryDTO queryDTO) {
        return Result.success(taskQueryService.getResearchWorkbench(queryDTO));
    }

    @GetMapping("/failed")
    public Result<TaskPageVO> pageFailedTasks(TaskPageQueryDTO queryDTO) {
        queryDTO.setOnlyFailed(true);
        return Result.success(taskQueryService.pageTasks(queryDTO));
    }

    @PostMapping("/{taskId}/cancel")
    public Result<String> cancelTask(@PathVariable("taskId") String taskId,
                                     @RequestBody(required = false) TaskCancelDTO dto) {
        roleAccessConfigService.requirePermission(RoleAccessConfigService.PERMISSION_TASK_CANCEL);
        return Result.success(taskControlService.cancelTask(taskId, dto));
    }

    @GetMapping("/{taskId}/report")
    public Result<TaskReportVO> getTaskReport(@PathVariable("taskId") String taskId) {
        return Result.success(taskQueryService.getTaskReportOnly(taskId));
    }

    @PostMapping("/{taskId}/report/review")
    public Result<String> reviewReport(@PathVariable("taskId") String taskId,
                                       @RequestBody TaskReportReviewDTO dto) {
        roleAccessConfigService.requirePermission(RoleAccessConfigService.PERMISSION_REPORT_REVIEW);
        return Result.success(taskReportService.reviewReport(taskId, dto));
    }

    @GetMapping("/report-review-stats")
    public Result<ReportReviewStatsVO> getReportReviewStats() {
        return Result.success(taskQueryService.getReportReviewStats());
    }

    @GetMapping("/{taskId}/report/review-logs")
    public Result<List<TaskReportReviewLogVO>> listReportReviewLogs(@PathVariable("taskId") String taskId) {
        return Result.success(taskReportService.listReviewLogs(taskId));
    }
}
