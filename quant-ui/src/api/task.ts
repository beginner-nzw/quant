import { get, post } from '../utils/request'
import type {
  AuditCompliancePageData,
  AuditComplianceStats,
  CreateTaskForm,
  MarketEventBatchImportForm,
  MarketEventBatchImportResult,
  MarketEventMockIngestForm,
  MarketEventSourceSyncForm,
  MarketEventBatchPreviewResult,
  MarketEventCreateForm,
  MarketEventCreateResult,
  EventSourceConfigItem,
  EventSourceRequestDiagnosticResult,
  EventSourcePreviewResult,
  MarketEventIngestHistoryItem,
  MarketEventListItem,
  MarketEventPageData,
  MarketEventStats,
  MarketIntelligencePageData,
  MarketIntelligenceStats,
  ModelAgentConfigCenterData,
  ReportCenterPageData,
  ReportCenterStats,
  ReportReviewStats,
  ResearchWorkbenchData,
  RoleAccessConfigItem,
  RiskWarningPageData,
  RiskWarningStats,
  StrategySignalPageData,
  StrategySignalCreateForm,
  StrategySignalFactorItem,
  StrategySignalStats,
  TaskFullDetail,
  TaskPageData,
  TaskReportReviewLog,
  TaskStats
} from '../types/task'
export function fetchTaskStats() {
  return get<TaskStats>('/api/tasks/stats')
}

export function fetchTasks(params: Record<string, any>) {
  return get<TaskPageData>('/api/tasks', params)
}

export function fetchFailedTasks(params: Record<string, any>) {
  return get<TaskPageData>('/api/tasks/failed', params)
}

export function fetchRiskWarningStats() {
  return get<RiskWarningStats>('/api/tasks/risk-warning-stats')
}

export function fetchMarketEventStats() {
  return get<MarketEventStats>('/api/tasks/market-event-stats')
}

export function fetchMarketEvents(params: Record<string, any>) {
  return get<MarketEventPageData>('/api/tasks/market-events', params)
}

export function fetchMarketEvent(eventId: string) {
  return get<MarketEventListItem>(`/api/tasks/market-events/${eventId}`)
}

export function fetchMarketEventIngestHistory() {
  return get<MarketEventIngestHistoryItem[]>('/api/tasks/market-events/ingest-history')
}

export function fetchMarketEventSourceConfigs() {
  return get<EventSourceConfigItem[]>('/api/tasks/market-event-source-configs')
}

export function createMarketEvent(data: MarketEventCreateForm) {
  return post<MarketEventCreateResult>('/api/tasks/market-events', data)
}

export function previewBatchImportMarketEvents(data: MarketEventBatchImportForm) {
  return post<MarketEventBatchPreviewResult>('/api/tasks/market-events/batch-import/preview', data)
}

export function batchImportMarketEvents(data: MarketEventBatchImportForm) {
  return post<MarketEventBatchImportResult>('/api/tasks/market-events/batch-import', data)
}

export function mockIngestMarketEvents(data: MarketEventMockIngestForm) {
  return post<MarketEventBatchImportResult>('/api/tasks/market-events/mock-ingest', data)
}

export function syncMarketEventSource(sourceCode: string, data: MarketEventSourceSyncForm) {
  return post<MarketEventBatchImportResult>(`/api/tasks/market-events/source-sync/${sourceCode}`, data)
}

export function previewMarketEventSource(sourceCode: string, data: MarketEventSourceSyncForm) {
  return post<EventSourcePreviewResult>(`/api/tasks/market-events/source-preview/${sourceCode}`, data)
}

export function diagnoseMarketEventSource(sourceCode: string, data: MarketEventSourceSyncForm) {
  return post<EventSourceRequestDiagnosticResult>(`/api/tasks/market-events/source-diagnose/${sourceCode}`, data)
}

export function fetchRiskWarnings(params: Record<string, any>) {
  return get<RiskWarningPageData>('/api/tasks/risk-warnings', params)
}

export function fetchStrategySignalStats() {
  return get<StrategySignalStats>('/api/tasks/strategy-signal-stats')
}

export function fetchStrategySignals(params: Record<string, any>) {
  return get<StrategySignalPageData>('/api/tasks/strategy-signals', params)
}

export function createStrategySignal(data: StrategySignalCreateForm) {
  return post<string>('/api/tasks/strategy-signals', data)
}

export function fetchStrategySignalFactors(signalId: string) {
  return get<StrategySignalFactorItem[]>(`/api/tasks/strategy-signals/${signalId}/factors`)
}

export function updateStrategySignalStatus(signalId: string, status: string) {
  return post<string>(`/api/tasks/strategy-signals/${signalId}/status`, { status })
}

export function fetchReportCenterStats() {
  return get<ReportCenterStats>('/api/tasks/report-center-stats')
}

export function fetchReportCenter(params: Record<string, any>) {
  return get<ReportCenterPageData>('/api/tasks/report-center', params)
}

export function fetchMarketIntelligenceStats() {
  return get<MarketIntelligenceStats>('/api/tasks/market-intelligence-stats')
}

export function fetchMarketIntelligence(params: Record<string, any>) {
  return get<MarketIntelligencePageData>('/api/tasks/market-intelligence', params)
}

export function fetchAuditComplianceStats() {
  return get<AuditComplianceStats>('/api/tasks/audit-compliance-stats')
}

export function fetchAuditCompliance(params: Record<string, any>) {
  return get<AuditCompliancePageData>('/api/tasks/audit-compliance', params)
}

export function fetchModelAgentConfigCenter() {
  return get<ModelAgentConfigCenterData>('/api/tasks/model-agent-config')
}

export function fetchRoleAccessConfigs() {
  return get<RoleAccessConfigItem[]>('/api/tasks/role-access-configs')
}

export function updatePromptTemplate(templateCode: string, templateContent: string) {
  return post<string>(`/api/tasks/model-agent-config/prompt-templates/${templateCode}`, {
    templateContent
  })
}

export function updateModelStrategy(strategyCode: string, data: Record<string, any>) {
  return post<string>(`/api/tasks/model-agent-config/model-strategies/${strategyCode}`, data)
}

export function updateEventAutoTriggerRule(ruleCode: string, data: Record<string, any>) {
  return post<string>(`/api/tasks/model-agent-config/event-auto-trigger-rules/${ruleCode}`, data)
}

export function updateEventSourceConfig(sourceCode: string, data: Record<string, any>) {
  return post<string>(`/api/tasks/model-agent-config/event-sources/${sourceCode}`, data)
}

export function updateAgentConfig(agentCode: string, data: Record<string, any>) {
  return post<string>(`/api/tasks/model-agent-config/agents/${agentCode}`, data)
}

export function updateWorkflowConfig(workflowCode: string, data: Record<string, any>) {
  return post<string>(`/api/tasks/model-agent-config/workflows/${workflowCode}`, data)
}

export function updateRoleAccessConfig(roleCode: string, data: Record<string, any>) {
  return post<string>(`/api/tasks/model-agent-config/role-access/${roleCode}`, data)
}

export function fetchResearchWorkbench(params: Record<string, any>) {
  return get<ResearchWorkbenchData>('/api/tasks/research-workbench', params)
}

export function fetchTaskFullDetail(taskId: string) {
  return get<TaskFullDetail>(`/api/tasks/${taskId}/full`)
}

export function retryTask(taskId: string, data?: Record<string, any>) {
  return post<string>(`/api/tasks/${taskId}/retry`, data)
}

export function cancelTask(taskId: string, data?: Record<string, any>) {
  return post<string>(`/api/tasks/${taskId}/cancel`, data)
}

export function createTask(data: CreateTaskForm) {
  return post<string>('/api/research/tasks', data)
}

export function fetchTaskReport(taskId: string) {
  return get<any>(`/api/tasks/${taskId}/report`)
}

export function reviewTaskReport(taskId: string, data: Record<string, any>) {
  return post<string>(`/api/tasks/${taskId}/report/review`, data)
}

export function fetchReportReviewStats() {
  return get<ReportReviewStats>('/api/tasks/report-review-stats')
}

export function fetchTaskReportReviewLogs(taskId: string) {
  return get<TaskReportReviewLog[]>(`/api/tasks/${taskId}/report/review-logs`)
}
