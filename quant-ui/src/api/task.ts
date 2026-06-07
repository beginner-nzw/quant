import { get, post } from '../utils/request'
import { MARKET_DATA_INGEST_API_CONTRACTS } from './marketDataIngestContracts'
import {
  compareTaskReportVersions as compareStableTaskReportVersions,
  fetchReportCenter as fetchStableReportCenter,
  fetchReportCenterStats as fetchStableReportCenterStats,
  fetchReportReviewStats as fetchStableReportReviewStats,
  fetchTaskReport as fetchStableTaskReport,
  fetchTaskReportReviewLogs as fetchStableTaskReportReviewLogs,
  fetchTaskReportVersion as fetchStableTaskReportVersion,
  fetchTaskReportVersions as fetchStableTaskReportVersions,
  reviewTaskReport as reviewStableTaskReport
} from './report'
import { RISK_STRATEGY_API_CONTRACTS } from './riskStrategyContracts'
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
  HumanReviewDecisionForm,
  HumanReviewQueuePageData,
  HumanReviewQueueStats,
  MarketEventIngestHistoryItem,
  MarketEventListItem,
  MarketEventPageData,
  MarketEventStats,
  MarketIntelligencePageData,
  MarketIntelligenceStats,
  ModelAgentConfigCenterData,
  ReportVersion,
  ReportVersionCompare,
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
  return get<RiskWarningStats>(RISK_STRATEGY_API_CONTRACTS.riskWarningStats)
}

export function fetchMarketEventStats() {
  return get<MarketEventStats>(MARKET_DATA_INGEST_API_CONTRACTS.marketEventStats)
}

export function fetchMarketEvents(params: Record<string, any>) {
  return get<MarketEventPageData>(MARKET_DATA_INGEST_API_CONTRACTS.marketEvents, params)
}

export function fetchMarketEvent(eventId: string) {
  return get<MarketEventListItem>(MARKET_DATA_INGEST_API_CONTRACTS.marketEvent(eventId))
}

export function fetchMarketEventIngestHistory() {
  return get<MarketEventIngestHistoryItem[]>(MARKET_DATA_INGEST_API_CONTRACTS.marketEventIngestHistory)
}

export function fetchMarketEventSourceConfigs() {
  return get<EventSourceConfigItem[]>(MARKET_DATA_INGEST_API_CONTRACTS.marketEventSourceConfigs)
}

export function createMarketEvent(data: MarketEventCreateForm) {
  return post<MarketEventCreateResult>(MARKET_DATA_INGEST_API_CONTRACTS.marketEvents, data)
}

export function previewBatchImportMarketEvents(data: MarketEventBatchImportForm) {
  return post<MarketEventBatchPreviewResult>(MARKET_DATA_INGEST_API_CONTRACTS.marketEventBatchImportPreview, data)
}

export function batchImportMarketEvents(data: MarketEventBatchImportForm) {
  return post<MarketEventBatchImportResult>(MARKET_DATA_INGEST_API_CONTRACTS.marketEventBatchImport, data)
}

export function mockIngestMarketEvents(data: MarketEventMockIngestForm) {
  return post<MarketEventBatchImportResult>(MARKET_DATA_INGEST_API_CONTRACTS.marketEventMockIngest, data)
}

export function syncMarketEventSource(sourceCode: string, data: MarketEventSourceSyncForm) {
  return post<MarketEventBatchImportResult>(MARKET_DATA_INGEST_API_CONTRACTS.marketEventSourceSync(sourceCode), data)
}

export function previewMarketEventSource(sourceCode: string, data: MarketEventSourceSyncForm) {
  return post<EventSourcePreviewResult>(MARKET_DATA_INGEST_API_CONTRACTS.marketEventSourcePreview(sourceCode), data)
}

export function diagnoseMarketEventSource(sourceCode: string, data: MarketEventSourceSyncForm) {
  return post<EventSourceRequestDiagnosticResult>(MARKET_DATA_INGEST_API_CONTRACTS.marketEventSourceDiagnose(sourceCode), data)
}

export function fetchRiskWarnings(params: Record<string, any>) {
  return get<RiskWarningPageData>(RISK_STRATEGY_API_CONTRACTS.riskWarnings, params)
}

export function fetchStrategySignalStats() {
  return get<StrategySignalStats>(RISK_STRATEGY_API_CONTRACTS.strategySignalStats)
}

export function fetchStrategySignals(params: Record<string, any>) {
  return get<StrategySignalPageData>(RISK_STRATEGY_API_CONTRACTS.strategySignals, params)
}

export function createStrategySignal(data: StrategySignalCreateForm) {
  return post<string>(RISK_STRATEGY_API_CONTRACTS.strategySignals, data)
}

export function fetchStrategySignalFactors(signalId: string) {
  return get<StrategySignalFactorItem[]>(RISK_STRATEGY_API_CONTRACTS.strategySignalFactors(signalId))
}

export function updateStrategySignalStatus(signalId: string, status: string) {
  return post<string>(RISK_STRATEGY_API_CONTRACTS.strategySignalStatus(signalId), { status })
}

export function fetchReportCenterStats() {
  return fetchStableReportCenterStats()
}

export function fetchReportCenter(params: Record<string, any>) {
  return fetchStableReportCenter(params)
}

export function fetchMarketIntelligenceStats() {
  return get<MarketIntelligenceStats>(MARKET_DATA_INGEST_API_CONTRACTS.marketIntelligenceStats)
}

export function fetchMarketIntelligence(params: Record<string, any>) {
  return get<MarketIntelligencePageData>(MARKET_DATA_INGEST_API_CONTRACTS.marketIntelligence, params)
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

// Display-only aggregation; callers must not use it as domain authority.
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

export function resumeTask(taskId: string, data?: Record<string, any>) {
  return post<string>(`/api/tasks/${taskId}/resume`, data)
}

export function rerunTaskNode(taskId: string, data?: Record<string, any>) {
  return post<string>(`/api/tasks/${taskId}/rerun`, data)
}

export function createTask(data: CreateTaskForm) {
  return post<string>('/api/research/tasks', data)
}

export function fetchTaskReport(taskId: string) {
  return fetchStableTaskReport(taskId)
}

export function reviewTaskReport(taskId: string, data: Record<string, any>) {
  return reviewStableTaskReport(taskId, data)
}

export function fetchReportReviewStats() {
  return fetchStableReportReviewStats()
}

export function fetchTaskReportReviewLogs(taskId: string) {
  return fetchStableTaskReportReviewLogs(taskId)
}

export function fetchHumanReviewQueue(params?: Record<string, any>) {
  return get<HumanReviewQueuePageData>('/api/tasks/human-reviews', params)
}

export function fetchHumanReviewQueueStats() {
  return get<HumanReviewQueueStats>('/api/tasks/human-reviews/stats')
}

export function decideHumanReview(queueId: string, data: HumanReviewDecisionForm) {
  return post<string>(`/api/tasks/human-reviews/${encodeURIComponent(queueId)}/decision`, data)
}

export function fetchTaskReportVersions(taskId: string) {
  return fetchStableTaskReportVersions(taskId)
}

export function fetchTaskReportVersion(taskId: string, versionNo: number) {
  return fetchStableTaskReportVersion(taskId, versionNo)
}

export function compareTaskReportVersions(taskId: string, fromVersionNo: number, toVersionNo: number) {
  return compareStableTaskReportVersions(taskId, fromVersionNo, toVersionNo)
}
