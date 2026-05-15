import type {
  AnalysisScope,
  AuditActionCode,
  AuditOperatorType,
  AuditResultStatus,
  AuditStage,
  AuditType,
  MarketEventImpactLevel,
  MarketEventStatus,
  MarketEventType,
  MarketIntelligenceType,
  ReportReviewStatus,
  RetrySource,
  RetryStatus,
  RiskLevel,
  SignalDirection,
  SignalStrength,
  TaskStage,
  TaskType,
  TaskStatus
} from './taskEnums'

export type {
  AnalysisScope,
  AuditActionCode,
  AuditOperatorType,
  AuditResultStatus,
  AuditStage,
  AuditType,
  MarketEventImpactLevel,
  MarketEventStatus,
  MarketEventType,
  MarketIntelligenceType,
  ReportReviewStatus,
  RetrySource,
  RetryStatus,
  RiskLevel,
  SignalDirection,
  SignalStrength,
  TaskStage,
  TaskType,
  TaskStatus
} from './taskEnums'

export type TaskPriority = 'HIGH' | 'MEDIUM' | 'LOW'

export interface TaskSourceContext {
  sourceTaskId?: string
  sourceReportId?: string
  sourceEventId?: string
  sourceDomain?: string
  sourceReviewStatus?: string
  analysisScope?: AnalysisScope | string
}

export interface ApiResult<T> {
  success: boolean
  code: string
  message: string
  data: T
}

export interface TaskStats {
  totalCount: number
  runningCount: number
  successCount: number
  failedCount: number
  retriedCount: number
}

export interface TaskListItem extends TaskSourceContext {
  taskId: string
  taskType: TaskType | string
  taskTitle: string
  targetType: string
  targetCode: string
  targetName: string
  reportId?: string
  reportType?: string
  priority: TaskPriority | string
  status: TaskStatus
  currentStage: TaskStage | string
  retryCount: number
  errorMessage?: string
  startTime?: string
  finishTime?: string
  createdAt?: string
  reportReviewStatus?: ReportReviewStatus
  revised?: boolean
  summaryRevised?: boolean
  highlightsRevised?: boolean
  riskPointsRevised?: boolean
  reportReviewedBy?: string
  reportReviewedAt?: string
  reportReviewComment?: string
}

export interface TaskPageData {
  total: number
  pageNum: number
  pageSize: number
  records: TaskListItem[]
}

export interface TaskState {
  taskId: string
  status: TaskStatus
  currentStage: TaskStage | string
  progress?: number
  source?: string
}

export interface TaskDetail extends TaskSourceContext {
  taskId: string
  taskType: TaskType | string
  taskTitle: string
  targetType: string
  targetCode: string
  targetName: string
  priority: TaskPriority | string
  status: TaskStatus
  currentStage: TaskStage | string
  sourceChannel: string
  traceId: string
  resultRef?: string
  retryCount: number
  errorMessage?: string
  startTime?: string
  finishTime?: string
  createdAt?: string
  updatedAt?: string
}

export interface TaskStep {
  taskId: string
  stepCode: string
  stepName: string
  agentCode: string
  executionOrder: number
  status: TaskStatus
  errorMessage?: string
  durationMs?: number
  startTime?: string
  finishTime?: string
}

export interface WorkflowInstance {
  workflowInstanceId: string
  taskId: string
  workflowCode: string
  workflowVersion: string
  entryAgent: string
  currentNode: string
  status: TaskStatus
  graphSnapshot?: string
  startTime?: string
  finishTime?: string
}

export interface AgentExecution {
  executionId: string
  workflowInstanceId: string
  taskId: string
  agentCode: string
  agentName: string
  nodeCode: string
  status: TaskStatus
  confidenceScore?: number
  needHumanReview?: number
  durationMs?: number
  startTime?: string
  finishTime?: string
}

export interface AuditRecord {
  auditId: string
  taskId: string
  auditType: AuditType | string
  auditStage: AuditStage | string
  operatorType: AuditOperatorType | string
  operatorId?: string
  actionCode: AuditActionCode | string
  actionDesc?: string
  resultStatus: AuditResultStatus | string
  remark?: string
  createdAt?: string
}

export interface TaskRetryLog {
  taskId: string
  retryNo: number
  retryReason?: string
  retrySource: RetrySource | string
  retryStatus: RetryStatus | string
  operatorId?: string
  createdAt?: string
}

export interface TaskFullDetail {
  taskDetail: TaskDetail
  taskState: TaskState
  summary: TaskSummary
  report?: TaskReport | null
  steps: TaskStep[]
  workflow: WorkflowInstance | null
  agents: AgentExecution[]
  audits: AuditRecord[]
  retries: TaskRetryLog[]
}

export interface TaskSummary {
  stepCount: number
  successStepCount: number
  failedStepCount: number
  agentCount: number
  retryCount: number
  hasAudit: boolean
  hasFailure: boolean
}

export interface CreateTaskForm extends TaskSourceContext {
  taskType: TaskType | string
  taskTitle: string
  targetType: string
  targetCode: string
  targetName: string
  priority: TaskPriority | string
}

export interface MarketEventCreateForm {
  targetType: string
  targetCode: string
  targetName: string
  eventType: MarketEventType | string
  eventTitle: string
  eventSummary: string
  sourceChannel?: string
  sourceUrl?: string
  impactLevel: MarketEventImpactLevel | string
  eventStatus: MarketEventStatus | string
  occurredAt: string
}

export interface MarketEventCreateResult {
  eventId: string
  duplicate?: boolean
  autoTriggerStatus?: string
  autoTriggerTaskId?: string
  autoTriggerMessage?: string
  message?: string
}

export interface MarketEventBatchImportForm {
  events: MarketEventCreateForm[]
}

export interface MarketEventMockIngestForm {
  targetType?: string
  targetCode: string
  targetName: string
  sourcePreset: string
  itemCount?: number
}

export interface MarketEventSourceSyncForm {
  targetType?: string
  targetCode: string
  targetName: string
  sourceCode: string
  itemCount?: number
}

export interface EventSourcePreviewItem {
  targetType?: string
  targetCode?: string
  targetName?: string
  eventType?: string
  eventTitle?: string
  eventSummary?: string
  sourceChannel?: string
  sourceUrl?: string
  impactLevel?: string
  eventStatus?: string
  occurredAt?: string
}

export interface EventSourceRequestDiagnosticItem {
  stageCode?: string
  stageName?: string
  requestMethod?: string
  requestUrl?: string
  requestTimeoutSeconds?: number
  requestHeadersJson?: string
  requestBodyJson?: string
}

export interface EventSourceRequestDiagnosticResult {
  sourceCode: string
  sourceName?: string
  ingestMode?: string
  diagnosedAt?: string
  items: EventSourceRequestDiagnosticItem[]
}

export interface EventSourcePreviewResult {
  sourceCode: string
  sourceName?: string
  sourceCategory?: string
  ingestMode?: string
  endpointUrl?: string
  upstreamUrl?: string
  itemCount: number
  previewedAt?: string
  items: EventSourcePreviewItem[]
}

export interface MarketEventBatchPreviewItem {
  itemNo: number
  valid: boolean
  importable?: boolean
  duplicate?: boolean
  invalidField?: string
  duplicateSource?: string
  existingEventId?: string
  targetCode?: string
  targetName?: string
  eventTitle?: string
  normalizedTargetCode?: string
  normalizedEventType?: string
  normalizedImpactLevel?: string
  normalizedEventStatus?: string
  normalizedSourceChannel?: string
  autoTriggerStatus?: string
  autoTriggerRuleCode?: string
  estimatedTaskType?: string
  message?: string
}

export interface MarketEventBatchPreviewResult {
  totalCount: number
  validCount: number
  invalidCount: number
  duplicateCount: number
  autoTriggerCandidateCount: number
  items: MarketEventBatchPreviewItem[]
}

export interface MarketEventBatchImportItem {
  itemNo: number
  success: boolean
  duplicate?: boolean
  eventId?: string
  targetCode?: string
  targetName?: string
  eventTitle?: string
  autoTriggerStatus?: string
  autoTriggerTaskId?: string
  message?: string
}

export interface MarketEventBatchImportResult {
  totalCount: number
  successCount: number
  failedCount: number
  duplicateCount: number
  autoTriggeredCount: number
  items: MarketEventBatchImportItem[]
}

export interface MarketEventIngestHistoryItem {
  historyId: string
  sourceType?: string
  sourceLabel?: string
  sourceCode?: string
  sourceName?: string
  sourceCategory?: string
  sourceChannel?: string
  sourceDetail?: string
  totalCount: number
  successCount: number
  failedCount: number
  duplicateCount: number
  autoTriggeredCount: number
  resultStatus?: string
  errorMessage?: string
  operatorId?: string
  operatorRole?: string
  summary?: string
  createdAt?: string
}

export interface TaskReportMeta {
  reportId?: string
  reportType?: string
  highlights?: string[]
  riskPoints?: string[]
  summary?: string
}

export interface TaskReportContextSnapshot {
  taskContextSource?: string
  marketDataSource?: string
  taskSummary?: Partial<TaskSummary>
  sourceTaskId?: string
  sourceReportId?: string
  sourceEventId?: string
  sourceEventTitle?: string
  sourceEventType?: string
  sourceEventImpactLevel?: string
  sourceEventOccurredAt?: string
  latestInsightReportId?: string
  reportCount?: number
  taskCount?: number
  pendingReviewCount?: number
  liveMarketEventSourceCode?: string
  liveMarketEventSourceName?: string
  liveEventCount?: number
  policyLiveEventCount?: number
  regulatoryRiskLiveEventCount?: number
  latestLiveEventTitle?: string
  latestLiveEventOccurredAt?: string
  latestLiveEventImpactLevel?: string
  latestLiveEventSourceUrl?: string
  priorityLiveEventTitle?: string
  priorityLiveEventOccurredAt?: string
  priorityLiveEventImpactLevel?: string
  priorityLiveEventSourceUrl?: string
  priorityLiveEventEvidenceId?: string
  priorityLiveEventReferenceId?: string
  priorityLiveEventEvidenceSource?: string
  priorityLiveEventEvidenceStatus?: string
  priorityLiveEventEvidenceMatchRule?: string
  priorityLiveEventTitles?: string[]
  highImpactLiveEventCount?: number
  mediumImpactLiveEventCount?: number
  lowImpactLiveEventCount?: number
  highImpactLiveEventClusterDate?: string
  highImpactLiveEventClusterCount?: number
  highImpactLiveEventClusterTitles?: string[]
  liveEventPriorityRule?: string
  liveEventHighlights?: string[]
  policyLiveEventHighlights?: string[]
  regulatoryRiskLiveEventHighlights?: string[]
  priorityExternalRiskEventSummary?: string
  summaryLeadAnchors?: string[]
  summaryLeadAnchorsCovered?: boolean
  summaryLeadCoverageStatus?: string
  highlightLeadAnchors?: string[]
  highlightLeadAnchorsCovered?: boolean
  highlightLeadCoverageStatus?: string
  liveEventSummaryAnchored?: boolean
  liveEventSummaryAnchor?: string
  liveEventSummaryAnchorStatus?: string
  liveEventHighlightAnchored?: boolean
  liveEventHighlightAnchor?: string
  liveEventHighlightAnchorStatus?: string
  evidenceCount?: number
  evidenceSources?: string[]
  planningMode?: string
  contextReady?: boolean
  planningLlmFramework?: string
  planningModelName?: string
  planningGenerationMode?: string
  planningFallbackReason?: string
  focusDimensions?: string[]
  reviewPressure?: string
  intentLlmFramework?: string
  intentModelName?: string
  intentGenerationMode?: string
  intentFallbackReason?: string
  llmFramework?: string
  generationMode?: string
  modelName?: string
  reportGenerationPath?: string
  reportFallbackReason?: string
}

export interface TaskReportEvidenceItem {
  evidenceId?: string
  evidenceType?: string
  source?: string
  title?: string
  summary?: string
  url?: string
  occurredAt?: string
  referenceId?: string
  relevance?: string
}

export interface TaskReportSection {
  sectionId?: string
  versionNo?: number
  sectionCode?: string
  sectionTitle?: string
  sectionOrder?: number
  sectionContent?: string
  sectionItems?: string[]
  revisedContent?: string
  revisedItems?: string[]
  displayContent?: string
  displayItems?: string[]
  reviewStatus?: ReportReviewStatus | string
  reviewedBy?: string
  reviewedAt?: string
  reviewComment?: string
  confidenceScore?: number
}

export interface TaskReportHumanReviewRecord {
  reviewId?: string
  reviewerId?: string
  reviewerRole?: string
  reviewResult?: ReportReviewStatus | string
  reviewComment?: string
  beforeSnapshotRef?: string
  afterSnapshotRef?: string
  beforeSnapshot?: string
  afterSnapshot?: string
  traceId?: string
  createdAt?: string
}

export interface TaskReport {
  taskType: TaskType | string
  finalStatus: TaskStatus
  reportId?: string
  versionNo?: number
  reportType?: string
  summary?: string
  originalSummary?: string
  displaySummary?: string
  confidenceScore?: number
  needHumanReview?: boolean
  riskWarnings?: string[]
  originalHighlights?: string[]
  displayHighlights?: string[]
  originalRiskPoints?: string[]
  displayRiskPoints?: string[]
  reportMeta?: TaskReportMeta
  resultRef?: string
  rawPayload?: string
  contextSnapshot?: TaskReportContextSnapshot
  sections?: TaskReportSection[]
  evidenceItems?: TaskReportEvidenceItem[]
  evidenceRefs?: string[]
  humanReviewRecords?: TaskReportHumanReviewRecord[]
  reviewSuggestion?: string
  reviewStatus?: ReportReviewStatus
  reviewedBy?: string
  reviewedAt?: string
  revisedSummary?: string
  revisedHighlights?: string[]
  revisedRiskPoints?: string[]
  reviewComment?: string
}

export interface ReportReviewStats {
  pendingCount: number
  approvedCount: number
  rejectedCount: number
  totalReportCount: number
}

export interface TaskReportReviewLog {
  reviewLogId: string
  reportId: string
  taskId: string
  versionNo?: number
  reviewStatus: ReportReviewStatus
  reviewedBy?: string
  reviewComment?: string
  revisedSummary?: string
  revisedHighlights?: string[]
  revisedRiskPoints?: string[]
  createdAt?: string
}

export interface RiskWarningStats {
  totalCount: number
  highCount: number
  mediumCount: number
  lowCount: number
  pendingReviewCount: number
  humanReviewCount: number
}

export interface RiskWarningListItem {
  taskId: string
  taskTitle: string
  taskType: string
  targetCode: string
  targetName: string
  priority: string
  taskStatus: TaskStatus | string
  currentStage: TaskStage | string
  reportId?: string
  reportType?: string
  finalStatus?: TaskStatus | string
  riskLevel: RiskLevel | string
  warningCount: number
  riskPointCount: number
  totalRiskCount: number
  needHumanReview: boolean
  reportReviewStatus: ReportReviewStatus | string
  reportReviewedBy?: string
  reportReviewedAt?: string
  revised?: boolean
  summaryRevised?: boolean
  highlightsRevised?: boolean
  riskPointsRevised?: boolean
  followUpStatus?: string
  followUpTaskCount?: number
  latestFollowUpTaskId?: string
  latestFollowUpTaskTitle?: string
  latestFollowUpTaskStatus?: TaskStatus | string
  latestFollowUpCreatedAt?: string
  reviewComment?: string
  summary?: string
  riskReasons: string[]
  riskSourceTags?: string[]
  createdAt?: string
}

export interface RiskWarningPageData {
  total: number
  pageNum: number
  pageSize: number
  records: RiskWarningListItem[]
}

export interface StrategySignalStats {
  totalCount: number
  positiveCount: number
  neutralCount: number
  negativeCount: number
  highConfidenceCount: number
  pendingReviewCount: number
}

export interface StrategySignalListItem {
  signalId?: string
  taskId: string
  taskTitle: string
  taskType: string
  targetCode: string
  targetName: string
  priority: string
  reportId?: string
  reportType?: string
  finalStatus?: TaskStatus | string
  signalDirection: SignalDirection | string
  signalStrength: SignalStrength | string
  signalScore: number
  confidenceScore?: number
  reportReviewStatus: ReportReviewStatus | string
  reportReviewedBy?: string
  reportReviewedAt?: string
  needHumanReview?: boolean
  revised?: boolean
  summaryRevised?: boolean
  highlightsRevised?: boolean
  riskPointsRevised?: boolean
  followUpStatus?: string
  followUpTaskCount?: number
  latestFollowUpTaskId?: string
  latestFollowUpTaskTitle?: string
  latestFollowUpTaskStatus?: TaskStatus | string
  latestFollowUpCreatedAt?: string
  strategySummary?: string
  signalSources: string[]
  signalSourceTags?: string[]
  backtestStatus?: string
  backtestSummary?: string
  reviewComment?: string
  createdAt?: string
}

export interface StrategySignalPageData {
  total: number
  pageNum: number
  pageSize: number
  records: StrategySignalListItem[]
}

export interface StrategySignalFactorItem {
  factorId?: string
  signalId?: string
  factorCode?: string
  factorName?: string
  factorValue?: string
  factorWeight?: number
  factorConclusion?: string
  createdAt?: string
}

export interface StrategySignalCreateFactorForm {
  factorCode?: string
  factorName?: string
  factorValue?: string
  factorWeight?: number
  factorConclusion?: string
}

export interface StrategySignalCreateForm {
  signalId?: string
  taskId?: string
  signalType?: string
  entityCode: string
  entityName: string
  signalDate?: string
  signalScore?: number
  signalLevel?: string
  signalDirection?: SignalDirection | string
  reasonSummary?: string
  confidenceScore?: number
  sourceEventId?: string
  status?: string
  traceId?: string
  tenantId?: string
  factors?: StrategySignalCreateFactorForm[]
}

export interface ReportCenterStats {
  totalCount: number
  highConfidenceCount: number
  pendingReviewCount: number
  approvedCount: number
  humanReviewCount: number
}

export interface ReportCenterListItem {
  taskId: string
  taskTitle: string
  taskType: string
  targetCode: string
  targetName: string
  priority: string
  reportId?: string
  reportType?: string
  finalStatus?: TaskStatus | string
  confidenceScore?: number
  needHumanReview?: boolean
  reviewStatus: ReportReviewStatus | string
  reviewedBy?: string
  reviewedAt?: string
  revised?: boolean
  summaryRevised?: boolean
  highlightsRevised?: boolean
  riskPointsRevised?: boolean
  summary?: string
  createdAt?: string
}

export interface ReportCenterPageData {
  total: number
  pageNum: number
  pageSize: number
  records: ReportCenterListItem[]
}


export interface MarketIntelligenceStats {
  totalCount: number
  riskAlertCount: number
  strategySignalCount: number
  reportInsightCount: number
  highPriorityCount: number
  pendingReviewCount: number
}

export interface MarketEventStats {
  totalCount: number
  activeCount: number
  highImpactCount: number
  trackedCount: number
  todayCount: number
}

export interface MarketEventRelation {
  relationType: string
  relationCode: string
  relationName?: string
  relationWeight?: number
}

export interface MarketEventListItem {
  eventId: string
  targetType: string
  targetCode: string
  targetName: string
  eventType: MarketEventType | string
  eventTitle: string
  eventSummary: string
  sourceChannel?: string
  sourceUrl?: string
  impactLevel: MarketEventImpactLevel | string
  eventStatus: MarketEventStatus | string
  autoTriggerRuleCode?: string
  autoTriggerStatus?: string
  autoTriggerTaskId?: string
  autoTriggerMessage?: string
  autoTriggerAttemptedAt?: string
  occurredAt?: string
  createdBy?: string
  createdAt?: string
  followUpStatus?: string
  followUpTaskCount?: number
  latestFollowUpTaskId?: string
  latestFollowUpTaskTitle?: string
  latestFollowUpTaskStatus?: string
  latestFollowUpCreatedAt?: string
  relatedReportCount?: number
  latestReportTaskId?: string
  latestReportId?: string
  latestReportType?: string
  latestReportReviewStatus?: ReportReviewStatus | string
  latestReportSummary?: string
  latestReportConfidenceScore?: number
  latestNeedHumanReview?: boolean
  latestReportCreatedAt?: string
  derivedRiskLevel?: RiskLevel | string
  derivedWarningCount?: number
  derivedRiskPointCount?: number
  derivedRiskCount?: number
  derivedSignalDirection?: SignalDirection | string
  derivedSignalStrength?: SignalStrength | string
  derivedSignalScore?: number
  derivedIntelligenceType?: MarketIntelligenceType | string
  relationCount?: number
  relations?: MarketEventRelation[]
}

export interface MarketEventPageData {
  total: number
  pageNum: number
  pageSize: number
  records: MarketEventListItem[]
}

export interface MarketIntelligenceListItem {
  taskId: string
  taskTitle: string
  taskType: string
  targetCode: string
  targetName: string
  priority: string
  sourceChannel?: string
  intelligenceType: MarketIntelligenceType | string
  reportId?: string
  reportType?: string
  finalStatus?: TaskStatus | string
  confidenceScore?: number
  needHumanReview?: boolean
  reviewStatus: ReportReviewStatus | string
  reviewedBy?: string
  reviewedAt?: string
  reviewComment?: string
  revised?: boolean
  summaryRevised?: boolean
  highlightsRevised?: boolean
  riskPointsRevised?: boolean
  followUpStatus?: string
  followUpTaskCount?: number
  latestFollowUpTaskId?: string
  latestFollowUpTaskTitle?: string
  latestFollowUpTaskStatus?: TaskStatus | string
  latestFollowUpCreatedAt?: string
  signalDirection?: SignalDirection | string
  riskLevel?: RiskLevel | string
  intelligenceSourceTags?: string[]
  summary?: string
  createdAt?: string
}

export interface MarketIntelligencePageData {
  total: number
  pageNum: number
  pageSize: number
  records: MarketIntelligenceListItem[]
}

export interface ResearchWorkbenchInsight {
  taskId: string
  taskTitle?: string
  reportId?: string
  reportType?: string
  finalStatus?: TaskStatus | string
  confidenceScore?: number
  needHumanReview?: boolean
  reviewStatus: ReportReviewStatus | string
  reviewedBy?: string
  reviewedAt?: string
  revised?: boolean
  summaryRevised?: boolean
  highlightsRevised?: boolean
  riskPointsRevised?: boolean
  signalDirection?: SignalDirection | string
  signalStrength?: SignalStrength | string
  riskLevel?: RiskLevel | string
  summary?: string
  highlights: string[]
  riskPoints: string[]
  createdAt?: string
}

export interface ResearchWorkbenchRecentTask {
  taskId: string
  taskTitle: string
  priority: string
  status: TaskStatus | string
  currentStage: TaskStage | string
  retryCount?: number
  reportId?: string
  reportReviewStatus?: ReportReviewStatus | string
  revised?: boolean
  summaryRevised?: boolean
  highlightsRevised?: boolean
  riskPointsRevised?: boolean
  confidenceScore?: number
  finishTime?: string
  createdAt?: string
}

export interface ResearchWorkbenchDispositionSummary {
  domainCode?: string
  totalCount: number
  notTrackedCount: number
  trackingCount: number
  completedCount: number
  failedCount: number
}

export interface ResearchWorkbenchData {
  targetCode?: string
  targetName?: string
  targetType?: string
  taskCount: number
  reportCount: number
  activeTaskCount: number
  successTaskCount: number
  failedTaskCount: number
  highConfidenceReportCount: number
  pendingReviewCount: number
  riskDispositionSummary?: ResearchWorkbenchDispositionSummary | null
  strategySignalDispositionSummary?: ResearchWorkbenchDispositionSummary | null
  marketIntelligenceDispositionSummary?: ResearchWorkbenchDispositionSummary | null
  latestInsight?: ResearchWorkbenchInsight | null
  recentTasks: ResearchWorkbenchRecentTask[]
}

export interface AuditComplianceStats {
  totalCount: number
  pendingReviewCount: number
  interceptedCount: number
  revisedReportCount: number
  humanReviewCount: number
  decisionTraceCount: number
  promptAuditCount: number
}

export interface AuditComplianceListItem {
  taskId: string
  taskTitle: string
  taskType: string
  targetCode: string
  targetName: string
  priority: string
  traceId?: string
  reportId?: string
  reportType?: string
  finalStatus?: TaskStatus | string
  reviewStatus: ReportReviewStatus | string
  reviewedBy?: string
  reviewedAt?: string
  reviewComment?: string
  needHumanReview?: boolean
  revised?: boolean
  intercepted?: boolean
  auditCount: number
  failedAuditCount: number
  agentAuditCount: number
  humanAuditCount: number
  agentExecutionCount: number
  humanReviewAgentCount: number
  workflowInstanceId?: string
  workflowCode?: string
  workflowVersion?: string
  workflowStatus?: TaskStatus | string
  currentNode?: string
  hasInputLog?: boolean
  hasOutputLog?: boolean
  hasDecisionLog?: boolean
  latestAuditType?: AuditType | string
  latestAuditStage?: AuditStage | string
  latestAuditActionCode?: AuditActionCode | string
  latestAuditResultStatus?: AuditResultStatus | string
  latestAuditRemark?: string
  latestAuditAt?: string
  originalSummary?: string
  revisedSummary?: string
  originalHighlights: string[]
  revisedHighlights: string[]
  originalRiskPoints: string[]
  revisedRiskPoints: string[]
  createdAt?: string
}

export interface AuditCompliancePageData {
  total: number
  pageNum: number
  pageSize: number
  records: AuditComplianceListItem[]
}

export interface ModelAgentConfigStats {
  workflowCount: number
  activeAgentCount: number
  modelStrategyCount: number
  promptTemplateCount: number
  toolWhitelistCount: number
  placeholderStrategyCount: number
  eventAutoTriggerRuleCount: number
  eventSourceConfigCount: number
  configAuditCount: number
  roleAccessConfigCount: number
}

export interface EngineRuntimeConfig {
  engineCode: string
  env: string
  host: string
  port: number
  workflowTimeoutSeconds: number
  consumerGroup: string
  kafkaBootstrapServers: string
  dispatchTopic: string
  statusTopic: string
  resultTopic: string
  auditTopic: string
  redisEndpoint: string
  runtimeMode: string
}

export interface WorkflowConfigItem {
  workflowCode: string
  workflowVersion: string
  workflowType: string
  taskTypes: string[]
  entryAgent: string
  nodeCount: number
  enabled: boolean
  defaultSelected: boolean
  nodeSequence: string[]
  nodeTimeoutSummary: string
  remark?: string
}

export interface AgentConfigItem {
  agentCode: string
  agentName: string
  stageCode: string
  executionOrder: number
  enabled: boolean
  timeoutSeconds: number
  needHumanReview: boolean
  implementationMode: string
  version: string
  toolWhitelist: string[]
  inputKeys: string[]
  outputKeys: string[]
  remark?: string
}

export interface ModelStrategyItem {
  strategyCode: string
  scenarioCode: string
  provider: string
  modelName: string
  baseUrl?: string
  accessMode: string
  enabled: boolean
  placeholder: boolean
  fallbackEnabled?: boolean
  requestTimeoutSeconds?: number
  temperature?: number
  maxTokens?: number
  promptTemplateCode?: string
  boundAgents: string[]
  remark?: string
}

export interface EventAutoTriggerRuleItem {
  ruleCode: string
  ruleName: string
  enabled: boolean
  eventTypes: string[]
  impactLevels: string[]
  taskType: string
  analysisScope: string
  priority: string
  sourceChannel: string
  titleTemplate: string
  remark?: string
}

export interface EventAutoTriggerConfig {
  enabled: boolean
  configPath?: string
  rules: EventAutoTriggerRuleItem[]
}

export interface EventSourceConfigItem {
  sourceCode: string
  sourceName: string
  sourceCategory: string
  sourceChannel: string
  ingestMode: string
  enabled: boolean
  supportsMockIngest: boolean
  sslVerify?: boolean
  endpointUrl?: string
  requestMethod?: string
  requestTimeoutSeconds?: number
  requestHeadersJson?: string
  requestQueryJson?: string
  requestBodyJson?: string
  responseItemsField?: string
  fieldMappingJson?: string
  upstreamUrl?: string
  upstreamMethod?: string
  upstreamHeadersJson?: string
  upstreamQueryJson?: string
  upstreamBodyJson?: string
  upstreamItemsField?: string
  upstreamFieldMappingJson?: string
  defaultEventType: string
  defaultImpactLevel: string
  ingestRecordCount?: number
  totalCount?: number
  successCount?: number
  failedCount?: number
  duplicateCount?: number
  autoTriggeredCount?: number
  lastIngestAt?: string
  lastResultStatus?: string
  lastErrorMessage?: string
  remark?: string
}

export interface EventSourceConfig {
  configPath?: string
  sources: EventSourceConfigItem[]
}

export interface PromptTemplateItem {
  templateCode: string
  templateName: string
  version: string
  sourceType: string
  editable: boolean
  enabled: boolean
  boundAgentCode?: string
  variables: string[]
  templatePath?: string
  templateContent?: string
  remark?: string
}

export interface ToolWhitelistItem {
  toolCode: string
  toolName: string
  toolType: string
  enabled: boolean
  scope: string
  remark?: string
}

export interface ConfigChangeAuditItem {
  auditId: string
  configType: string
  targetCode?: string
  targetName?: string
  operation?: string
  operatorId?: string
  operatorRole?: string
  configPath?: string
  changeSummary?: string
  changedFields: string[]
  createdAt?: string
}

export interface RoleAccessConfigItem {
  roleCode: string
  roleName: string
  roleDescription: string
  menuKeys: string[]
  permissionKeys: string[]
  remark?: string
}

export interface ModelAgentConfigCenterData {
  currentAccessRole?: string
  editable?: boolean
  stats: ModelAgentConfigStats
  engineRuntime: EngineRuntimeConfig
  workflows: WorkflowConfigItem[]
  agents: AgentConfigItem[]
  modelStrategies: ModelStrategyItem[]
  eventAutoTriggerConfig?: EventAutoTriggerConfig
  eventSourceConfig?: EventSourceConfig
  promptTemplates: PromptTemplateItem[]
  toolWhitelists: ToolWhitelistItem[]
  roleAccessConfigs: RoleAccessConfigItem[]
  configChangeAudits: ConfigChangeAuditItem[]
}
