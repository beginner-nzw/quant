<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  fetchTaskFullDetail,
  fetchTaskReport,
  fetchTaskReportReviewLogs
} from '../../api/task'
import ReportEvidenceView from '../../components/report/ReportEvidenceView.vue'
import ReportVersionComparison from '../../components/report/ReportVersionComparison.vue'
import type {
  TaskFullDetail,
  TaskReportContextSnapshot,
  TaskReportEvidenceItem,
  TaskReportReviewLog
} from '../../types/task'
import { formatDateTime } from '../../utils/format'
import { buildResearchWorkbenchQuery } from '../../utils/researchWorkbench'
import {
  getHumanReviewText,
  getReviewStatusTagType,
  getReviewStatusText,
  getTaskTypeText
} from '../../utils/task'
import { resolveTaskReportActionAccess } from '../../utils/taskActionAccess'
import { buildFollowUpTaskTitle, buildTaskCreateQuery } from '../../utils/taskCreate'
import { getCurrentUser } from '../../utils/auth'
import { executeTaskReportReview } from '../../utils/taskActions'
import { buildFromQuery, resolveSourcePath } from '../../utils/taskNavigation'
import {
  ANALYSIS_SCOPE,
  REPORT_REVIEW_STATUS,
  isPendingReviewStatus,
  TASK_TYPE,
  type ReportReviewStatus
} from '../../types/taskEnums'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const detail = ref<TaskFullDetail | null>(null)
const editMode = ref(false)
const reviewLoading = ref(false)
const reviewLogs = ref<TaskReportReviewLog[]>([])
const reviewVersionDialogVisible = ref(false)
const selectedReviewLog = ref<TaskReportReviewLog | null>(null)

interface ReportVersionContent {
  summary: string
  highlights: string[]
  riskPoints: string[]
}

const text = {
  loadReportFailed: '报告加载失败',
  loadReportError: '报告加载异常',
  noSummaryToCopy: '当前没有可复制的摘要',
  summaryCopied: '摘要已复制',
  copyFailed: '复制失败',
  noReportToExport: '当前没有可导出的报告',
  markdownExported: 'Markdown 已导出',
  backToDetail: '返回详情',
  backToList: '返回列表',
  backToSource: '返回来源页',
  backToWorkbench: '返回审核工作台',
  refreshReport: '刷新报告',
  workbench: '投研工作台',
  createTask: '发起研究',
  copySummary: '复制摘要',
  exportMarkdown: '导出 Markdown',
  approve: '审核通过',
  reject: '驳回报告',
  cancelEdit: '取消编辑',
  editReport: '编辑报告',
  title: '投研报告',
  reportVersion: '报告版本',
  taskTitle: '任务标题',
  targetCode: '标的代码',
  targetName: '标的名称',
  taskType: '任务类型',
  reportType: '报告类型',
  finishTime: '完成时间',
  reviewedAt: '审核时间',
  confidenceScore: '置信度',
  needHumanReview: '人工复核',
  workflowInstanceId: '工作流实例',
  summaryTitle: '投资结论摘要',
  reportSections: '报告章节',
  emptyReportSections: '暂无报告章节',
  sectionReviewed: '章节审核',
  summary: '摘要',
  versionCompare: '版本对比',
  originalVersion: '原始版本',
  currentVersion: '当前展示版本',
  reviewPanelTitle: '人工审核与修订',
  reviewContentStatus: '修订内容状态',
  originalValue: '原始值',
  currentValue: '当前值',
  revisedStatus: '修订状态',
  revisedYes: '已修订',
  revisedNo: '未修订',
  reviewStatus: '审核状态',
  reviewedBy: '审核人',
  revisedSummary: '修订摘要',
  revisedHighlights: '修订亮点',
  revisedRiskPoints: '修订风险点',
  reviewComment: '审核意见',
  revisedSummaryPlaceholder: '请输入修订后的摘要',
  revisedHighlightsPlaceholder: '每行一个亮点',
  revisedRiskPointsPlaceholder: '每行一个风险点',
  reviewCommentPlaceholder: '请输入审核意见',
  saveReview: '保存审核结果',
  reviewHistory: '审核历史',
  reviewVersionDiff: '本次版本差异',
  openReviewVersionDiff: '查看本次差异',
  reviewVersionDialogTitle: '审核版本差异',
  beforeReviewVersion: '审核前版本',
  afterReviewVersion: '审核后版本',
  emptyReviewHistory: '暂无审核历史',
  noReviewComment: '无审核意见',
  highlights: '关键亮点',
  emptyHighlights: '暂无关键亮点',
  riskPoints: '风险点',
  emptyRiskPoints: '暂无风险点',
  contextSnapshot: '上下文快照',
  emptyContextSnapshot: '暂无上下文快照',
  evidenceRefs: '证据链',
  emptyEvidenceRefs: '暂无证据链',
  humanReviewRecords: '人工复核记录',
  emptyHumanReviewRecords: '暂无人工复核记录',
  reviewerRole: '审核角色',
  reviewTrace: '追踪 ID',
  beforeSnapshot: '审核前快照',
  afterSnapshot: '审核后快照',
  reviewSuggestion: '审核建议',
  taskContextSource: '任务上下文来源',
  marketDataSource: '市场快照来源',
  sourceTaskId: '来源任务',
  sourceReportId: '来源报告',
  sourceEventId: '来源事件',
  sourceEventTitle: '来源事件标题',
  sourceEventType: '来源事件类型',
  sourceEventImpactLevel: '来源事件影响等级',
  sourceEventOccurredAt: '来源事件发生时间',
  latestInsightReportId: '最新洞察报告',
  taskCount: '同标的任务数',
  reportCount: '同标的报告数',
  pendingReviewCount: '待审核报告数',
  liveMarketEventSourceCode: '实时事件源编码',
  liveMarketEventSourceName: '实时事件源',
  liveEventCount: '实时事件数',
  policyLiveEvent: '政策实时事件',
  regulatoryRiskLiveEvent: '监管风险实时事件',
  policyLiveEventCount: '政策实时事件数',
  regulatoryRiskLiveEventCount: '监管风险事件数',
  latestLiveEventTitle: '最新实时事件标题',
  latestLiveEventOccurredAt: '最新实时事件时间',
  latestLiveEventImpactLevel: '最新实时事件影响等级',
  latestLiveEventSourceUrl: '最新实时事件链接',
  priorityLiveEventTitle: '优先实时事件标题',
  priorityLiveEventOccurredAt: '优先实时事件时间',
  priorityLiveEventImpactLevel: '优先实时事件影响等级',
  priorityLiveEventSourceUrl: '优先实时事件链接',
  priorityLiveEventEvidenceId: '优先实时事件证据',
  priorityLiveEventReferenceId: '优先实时事件引用',
  priorityLiveEventEvidenceSource: '优先实时事件证据源',
  priorityLiveEventEvidenceStatus: '优先实时事件证据状态',
  priorityLiveEventEvidenceMatchRule: '优先实时事件证据匹配规则',
  priorityLiveEventTitles: '优先实时事件标题组',
  highImpactLiveEventCount: '高影响实时事件数',
  mediumImpactLiveEventCount: '中影响实时事件数',
  lowImpactLiveEventCount: '低影响实时事件数',
  highImpactLiveEventClusterDate: '高影响事件簇日期',
  highImpactLiveEventClusterCount: '高影响事件簇数量',
  highImpactLiveEventClusterTitles: '高影响事件簇标题组',
  liveEventPriorityRule: '实时事件优先规则',
  liveEventHighlights: '实时事件摘要',
  policyLiveEventHighlights: '政策事件摘要',
  regulatoryRiskLiveEventHighlights: '监管风险事件摘要',
  priorityExternalRiskEventSummary: '优先外部风险事件',
  summaryLeadAnchors: '摘要前导锚点',
  summaryLeadAnchorsCovered: '摘要前半段锚点覆盖',
  summaryLeadCoverageStatus: '摘要前半段覆盖状态',
  highlightLeadAnchors: '亮点前导锚点',
  highlightLeadAnchorsCovered: '亮点前两条锚点覆盖',
  highlightLeadCoverageStatus: '亮点前两条覆盖状态',
  liveEventSummaryAnchored: '摘要实时事件增强',
  liveEventSummaryAnchor: '摘要实时事件锚点',
  liveEventSummaryAnchorStatus: '摘要实时事件状态',
  liveEventHighlightAnchored: '亮点实时事件增强',
  liveEventHighlightAnchor: '亮点实时事件锚点',
  liveEventHighlightAnchorStatus: '亮点实时事件状态',
  evidenceCount: '证据条数',
  evidenceSources: '证据来源',
  stepCount: '步骤总数',
  agentCount: 'Agent 数量',
  planningMode: '规划模式',
  planningLlmFramework: '规划节点 LLM 框架',
  planningModelName: '规划节点模型',
  planningGenerationMode: '规划节点生成模式',
  planningFallbackReason: '规划节点回退原因',
  focusDimensions: '关注维度',
  reviewPressure: '审核压力',
  intentLlmFramework: '意图节点 LLM 框架',
  intentModelName: '意图节点模型',
  intentGenerationMode: '意图节点生成模式',
  intentFallbackReason: '意图节点回退原因',
  llmFramework: 'LLM 框架',
  modelName: '模型名称',
  generationMode: '生成模式',
  reportGenerationPath: '报告生成路径',
  reportFallbackReason: '报告回退原因',
  emptyFullReport: '当前任务暂无可展示的完整报告',
  emptyReportData: '暂无报告数据'
} as const

const reviewStatusOptions = [
  {
    label: getReviewStatusText(REPORT_REVIEW_STATUS.PENDING),
    value: REPORT_REVIEW_STATUS.PENDING
  },
  {
    label: getReviewStatusText(REPORT_REVIEW_STATUS.APPROVED),
    value: REPORT_REVIEW_STATUS.APPROVED
  },
  {
    label: getReviewStatusText(REPORT_REVIEW_STATUS.REJECTED),
    value: REPORT_REVIEW_STATUS.REJECTED
  }
] as const

const reviewForm = reactive({
  reviewStatus: REPORT_REVIEW_STATUS.PENDING as ReportReviewStatus,
  reviewedBy: getCurrentUser().userId,
  revisedSummary: '',
  revisedHighlightsText: '',
  revisedRiskPointsText: '',
  reviewComment: ''
})

const displaySummary = computed(() => {
  if (!detail.value?.report) return ''
  return detail.value.report.displaySummary
    || detail.value.report.revisedSummary
    || detail.value.report.summary
    || detail.value.report.originalSummary
    || detail.value.report.reportMeta?.summary
    || ''
})

const displayHighlights = computed(() => {
  if (!detail.value?.report) return []
  if (detail.value.report.displayHighlights && detail.value.report.displayHighlights.length > 0) {
    return detail.value.report.displayHighlights
  }
  if (detail.value.report.revisedHighlights && detail.value.report.revisedHighlights.length > 0) {
    return detail.value.report.revisedHighlights
  }
  return detail.value.report.originalHighlights || detail.value.report.reportMeta?.highlights || []
})

const displayRiskPoints = computed(() => {
  if (!detail.value?.report) return []
  if (detail.value.report.displayRiskPoints && detail.value.report.displayRiskPoints.length > 0) {
    return detail.value.report.displayRiskPoints
  }
  if (detail.value.report.revisedRiskPoints && detail.value.report.revisedRiskPoints.length > 0) {
    return detail.value.report.revisedRiskPoints
  }
  return detail.value.report.originalRiskPoints || detail.value.report.reportMeta?.riskPoints || []
})

const contextSnapshot = computed<TaskReportContextSnapshot | null>(() => {
  return detail.value?.report?.contextSnapshot || null
})

const actionAccess = computed(() => resolveTaskReportActionAccess(detail.value))

function formatImpactLevel(value?: string | null) {
  const normalized = normalizeTextValue(value).toUpperCase()
  if (normalized === 'HIGH') return '高'
  if (normalized === 'MEDIUM') return '中'
  if (normalized === 'LOW') return '低'
  return normalizeTextValue(value)
}

function formatLiveEventAnchorStatus(value?: string | null) {
  const normalized = normalizeTextValue(value).toUpperCase()
  if (normalized === 'MODEL_NATIVE') return '模型前置命中'
  if (normalized === 'POST_PROCESS_ANCHORED') return '后处理前置补位'
  if (normalized === 'COVERAGE_GAP') return '前置覆盖缺口'
  if (normalized === 'NOT_APPLICABLE') return '不适用'
  return normalizeTextValue(value)
}

function formatCoverageFlag(value?: boolean) {
  if (value === undefined || value === null) return undefined
  return value ? '已覆盖' : '未覆盖'
}

function formatLiveEventEvidenceStatus(value?: string | null) {
  const normalized = normalizeTextValue(value).toUpperCase()
  if (normalized === 'MATCHED') return '已匹配'
  if (normalized === 'MISSING') return '未匹配到结构化证据'
  if (normalized === 'NOT_APPLICABLE') return '不适用'
  return normalizeTextValue(value)
}

function formatLiveEventRule(value?: string | null) {
  const normalized = normalizeTextValue(value).toUpperCase()
  if (normalized === 'HIGH_IMPACT_CLUSTER_FIRST_THEN_IMPACT_DESC_THEN_TITLE_DESC_THEN_TIME_DESC') {
    return '高影响事件簇优先，其次影响等级、标题权重、时间倒序'
  }
  if (normalized === 'URL_THEN_TITLE_TIME_THEN_TITLE') {
    return '链接优先，其次标题+时间，最后标题兜底'
  }
  return normalizeTextValue(value)
}

const markdownEvidenceRefMeta: Record<string, { label: string; formatter?: (value?: string | null) => string }> = {
  policyLiveEvent: { label: text.policyLiveEvent },
  regulatoryRiskLiveEvent: { label: text.regulatoryRiskLiveEvent },
  policyLiveEventCount: { label: text.policyLiveEventCount },
  regulatoryRiskLiveEventCount: { label: text.regulatoryRiskLiveEventCount },
  priorityLiveEventTitle: { label: text.priorityLiveEventTitle },
  priorityLiveEventOccurredAt: { label: text.priorityLiveEventOccurredAt },
  priorityLiveEventEvidenceStatus: {
    label: text.priorityLiveEventEvidenceStatus,
    formatter: formatLiveEventEvidenceStatus
  },
  priorityLiveEventEvidenceMatchRule: {
    label: text.priorityLiveEventEvidenceMatchRule,
    formatter: formatLiveEventRule
  },
  priorityLiveEventReferenceId: { label: text.priorityLiveEventReferenceId },
  priorityLiveEventEvidenceId: { label: text.priorityLiveEventEvidenceId },
  priorityLiveEventEvidenceSource: { label: text.priorityLiveEventEvidenceSource }
}

const contextItems = computed(() => {
  const snapshot = contextSnapshot.value
  if (!snapshot) return []

  const taskSummary = snapshot.taskSummary || {}
  const items = [
    { label: text.taskContextSource, value: snapshot.taskContextSource },
    { label: text.marketDataSource, value: snapshot.marketDataSource },
    { label: text.sourceTaskId, value: snapshot.sourceTaskId },
    { label: text.sourceReportId, value: snapshot.sourceReportId },
    { label: text.sourceEventId, value: snapshot.sourceEventId },
    { label: text.sourceEventTitle, value: snapshot.sourceEventTitle },
    { label: text.sourceEventType, value: snapshot.sourceEventType },
    { label: text.sourceEventImpactLevel, value: formatImpactLevel(snapshot.sourceEventImpactLevel) },
    { label: text.sourceEventOccurredAt, value: snapshot.sourceEventOccurredAt },
    { label: text.latestInsightReportId, value: snapshot.latestInsightReportId },
    { label: text.taskCount, value: snapshot.taskCount },
    { label: text.reportCount, value: snapshot.reportCount },
    { label: text.pendingReviewCount, value: snapshot.pendingReviewCount },
    { label: text.liveMarketEventSourceCode, value: snapshot.liveMarketEventSourceCode },
    { label: text.liveMarketEventSourceName, value: snapshot.liveMarketEventSourceName },
    { label: text.liveEventCount, value: snapshot.liveEventCount },
    { label: text.policyLiveEventCount, value: snapshot.policyLiveEventCount },
    { label: text.regulatoryRiskLiveEventCount, value: snapshot.regulatoryRiskLiveEventCount },
    { label: text.latestLiveEventTitle, value: snapshot.latestLiveEventTitle },
    { label: text.latestLiveEventOccurredAt, value: snapshot.latestLiveEventOccurredAt },
    { label: text.latestLiveEventImpactLevel, value: formatImpactLevel(snapshot.latestLiveEventImpactLevel) },
    { label: text.latestLiveEventSourceUrl, value: snapshot.latestLiveEventSourceUrl },
    { label: text.priorityLiveEventTitle, value: snapshot.priorityLiveEventTitle },
    { label: text.priorityLiveEventOccurredAt, value: snapshot.priorityLiveEventOccurredAt },
    { label: text.priorityLiveEventImpactLevel, value: formatImpactLevel(snapshot.priorityLiveEventImpactLevel) },
    { label: text.priorityLiveEventSourceUrl, value: snapshot.priorityLiveEventSourceUrl },
    { label: text.priorityLiveEventEvidenceId, value: snapshot.priorityLiveEventEvidenceId },
    { label: text.priorityLiveEventReferenceId, value: snapshot.priorityLiveEventReferenceId },
    { label: text.priorityLiveEventEvidenceSource, value: snapshot.priorityLiveEventEvidenceSource },
    { label: text.priorityLiveEventEvidenceStatus, value: formatLiveEventEvidenceStatus(snapshot.priorityLiveEventEvidenceStatus) },
    { label: text.priorityLiveEventEvidenceMatchRule, value: formatLiveEventRule(snapshot.priorityLiveEventEvidenceMatchRule) },
    {
      label: text.priorityLiveEventTitles,
      value: Array.isArray(snapshot.priorityLiveEventTitles) ? snapshot.priorityLiveEventTitles.join('；') : undefined
    },
    { label: text.highImpactLiveEventCount, value: snapshot.highImpactLiveEventCount },
    { label: text.mediumImpactLiveEventCount, value: snapshot.mediumImpactLiveEventCount },
    { label: text.lowImpactLiveEventCount, value: snapshot.lowImpactLiveEventCount },
    { label: text.highImpactLiveEventClusterDate, value: snapshot.highImpactLiveEventClusterDate },
    { label: text.highImpactLiveEventClusterCount, value: snapshot.highImpactLiveEventClusterCount },
    {
      label: text.highImpactLiveEventClusterTitles,
      value: Array.isArray(snapshot.highImpactLiveEventClusterTitles) ? snapshot.highImpactLiveEventClusterTitles.join('；') : undefined
    },
    { label: text.liveEventPriorityRule, value: formatLiveEventRule(snapshot.liveEventPriorityRule) },
    {
      label: text.liveEventHighlights,
      value: Array.isArray(snapshot.liveEventHighlights) ? snapshot.liveEventHighlights.join('；') : undefined
    },
    {
      label: text.policyLiveEventHighlights,
      value: Array.isArray(snapshot.policyLiveEventHighlights) ? snapshot.policyLiveEventHighlights.join('；') : undefined
    },
    {
      label: text.regulatoryRiskLiveEventHighlights,
      value: Array.isArray(snapshot.regulatoryRiskLiveEventHighlights) ? snapshot.regulatoryRiskLiveEventHighlights.join('；') : undefined
    },
    { label: text.priorityExternalRiskEventSummary, value: snapshot.priorityExternalRiskEventSummary },
    {
      label: text.summaryLeadAnchors,
      value: Array.isArray(snapshot.summaryLeadAnchors) ? snapshot.summaryLeadAnchors.join('；') : undefined
    },
    {
      label: text.summaryLeadAnchorsCovered,
      value: formatCoverageFlag(snapshot.summaryLeadAnchorsCovered)
    },
    {
      label: text.summaryLeadCoverageStatus,
      value: formatLiveEventAnchorStatus(snapshot.summaryLeadCoverageStatus)
    },
    {
      label: text.highlightLeadAnchors,
      value: Array.isArray(snapshot.highlightLeadAnchors) ? snapshot.highlightLeadAnchors.join('；') : undefined
    },
    {
      label: text.highlightLeadAnchorsCovered,
      value: formatCoverageFlag(snapshot.highlightLeadAnchorsCovered)
    },
    {
      label: text.highlightLeadCoverageStatus,
      value: formatLiveEventAnchorStatus(snapshot.highlightLeadCoverageStatus)
    },
    { label: text.liveEventSummaryAnchored, value: snapshot.liveEventSummaryAnchored ? '已增强' : undefined },
    { label: text.liveEventSummaryAnchor, value: snapshot.liveEventSummaryAnchor },
    { label: text.liveEventSummaryAnchorStatus, value: formatLiveEventAnchorStatus(snapshot.liveEventSummaryAnchorStatus) },
    { label: text.liveEventHighlightAnchored, value: snapshot.liveEventHighlightAnchored ? '已增强' : undefined },
    { label: text.liveEventHighlightAnchor, value: snapshot.liveEventHighlightAnchor },
    { label: text.liveEventHighlightAnchorStatus, value: formatLiveEventAnchorStatus(snapshot.liveEventHighlightAnchorStatus) },
    { label: text.evidenceCount, value: snapshot.evidenceCount },
    {
      label: text.evidenceSources,
      value: Array.isArray(snapshot.evidenceSources) ? snapshot.evidenceSources.join('；') : undefined
    },
    { label: text.stepCount, value: taskSummary.stepCount },
    { label: text.agentCount, value: taskSummary.agentCount },
    { label: text.planningMode, value: snapshot.planningMode },
    { label: text.planningLlmFramework, value: snapshot.planningLlmFramework },
    { label: text.planningModelName, value: snapshot.planningModelName },
    { label: text.planningGenerationMode, value: snapshot.planningGenerationMode },
    { label: text.planningFallbackReason, value: snapshot.planningFallbackReason },
    {
      label: text.focusDimensions,
      value: Array.isArray(snapshot.focusDimensions) ? snapshot.focusDimensions.join(' / ') : undefined
    },
    { label: text.reviewPressure, value: snapshot.reviewPressure },
    { label: text.intentLlmFramework, value: snapshot.intentLlmFramework },
    { label: text.intentModelName, value: snapshot.intentModelName },
    { label: text.intentGenerationMode, value: snapshot.intentGenerationMode },
    { label: text.intentFallbackReason, value: snapshot.intentFallbackReason },
    { label: text.llmFramework, value: snapshot.llmFramework },
    { label: text.modelName, value: snapshot.modelName },
    { label: text.generationMode, value: snapshot.generationMode },
    { label: text.reportGenerationPath, value: snapshot.reportGenerationPath },
    { label: text.reportFallbackReason, value: snapshot.reportFallbackReason }
  ]

  return items.filter((item) => item.value !== undefined && item.value !== null && item.value !== '')
})

const evidenceItems = computed(() => detail.value?.report?.evidenceItems || [])
const evidenceRefs = computed(() => detail.value?.report?.evidenceRefs || [])
const humanReviewRecords = computed(() => detail.value?.report?.humanReviewRecords || [])
const reportSections = computed(() => detail.value?.report?.sections || [])
const reviewSuggestion = computed(() => detail.value?.report?.reviewSuggestion || '')

function normalizeTextValue(value?: string | null) {
  return (value || '').trim()
}

function normalizeListValue(value?: string[] | null) {
  return Array.from(
    new Set(
      (value || [])
        .map((item) => item.trim())
        .filter(Boolean)
    )
  )
}

function formatMarkdownEvidenceItem(item: TaskReportEvidenceItem, index: number) {
  const parts = [
    normalizeTextValue(item.source) ? `来源：${normalizeTextValue(item.source)}` : '',
    normalizeTextValue(item.evidenceType) ? `类型：${normalizeTextValue(item.evidenceType)}` : '',
    normalizeTextValue(item.relevance) ? `相关性：${normalizeTextValue(item.relevance)}` : '',
    normalizeTextValue(item.occurredAt) ? `时间：${normalizeTextValue(item.occurredAt)}` : '',
    normalizeTextValue(item.referenceId) ? `引用：${normalizeTextValue(item.referenceId)}` : ''
  ].filter(Boolean)
  const title = normalizeTextValue(item.title) || `证据 ${index + 1}`
  const summary = normalizeTextValue(item.summary)
  const url = normalizeTextValue(item.url)
  const lines = [`- ${title}${parts.length > 0 ? `（${parts.join('；')}）` : ''}`]
  if (summary) {
    lines.push(`  - 摘要：${summary}`)
  }
  if (url) {
    lines.push(`  - 链接：${url}`)
  }
  return lines.join('\n')
}

function formatMarkdownEvidenceRef(rawRef: string) {
  const normalized = normalizeTextValue(rawRef)
  if (!normalized) return ''

  const separatorIndex = normalized.indexOf(':')
  if (separatorIndex < 0) {
    return `- ${normalized}`
  }

  const key = normalized.slice(0, separatorIndex)
  const value = normalized.slice(separatorIndex + 1).trim()
  const meta = markdownEvidenceRefMeta[key]

  if (!meta) {
    return `- ${normalized}`
  }

  const formattedValue = meta.formatter ? meta.formatter(value) : value
  return `- ${meta.label}：${formattedValue || '-'}`
}

function isSameTextValue(left?: string | null, right?: string | null) {
  return normalizeTextValue(left) === normalizeTextValue(right)
}

function isSameListValue(left?: string[] | null, right?: string[] | null) {
  const normalizedLeft = normalizeListValue(left)
  const normalizedRight = normalizeListValue(right)
  if (normalizedLeft.length !== normalizedRight.length) {
    return false
  }
  return normalizedLeft.every((item, index) => item === normalizedRight[index])
}

function buildOriginalReportVersion(): ReportVersionContent {
  const report = detail.value?.report
  return {
    summary: normalizeTextValue(
      report?.originalSummary
      || report?.summary
      || report?.reportMeta?.summary
      || ''
    ),
    highlights: normalizeListValue(
      report?.originalHighlights
      || report?.reportMeta?.highlights
      || []
    ),
    riskPoints: normalizeListValue(
      report?.originalRiskPoints
      || report?.reportMeta?.riskPoints
      || []
    )
  }
}

function buildReviewLogVersion(
  previousVersion: ReportVersionContent,
  reviewLog: TaskReportReviewLog
): ReportVersionContent {
  return {
    summary: reviewLog.revisedSummary !== undefined && reviewLog.revisedSummary !== null
      ? normalizeTextValue(reviewLog.revisedSummary)
      : previousVersion.summary,
    highlights: Array.isArray(reviewLog.revisedHighlights)
      ? normalizeListValue(reviewLog.revisedHighlights)
      : previousVersion.highlights,
    riskPoints: Array.isArray(reviewLog.revisedRiskPoints)
      ? normalizeListValue(reviewLog.revisedRiskPoints)
      : previousVersion.riskPoints
  }
}

const currentReviewVersion = computed<ReportVersionContent>(() => ({
  summary: displaySummary.value,
  highlights: displayHighlights.value,
  riskPoints: displayRiskPoints.value
}))

const reviewContentSections = computed(() => {
  const originalVersion = buildOriginalReportVersion()
  const currentVersion = currentReviewVersion.value

  return [
    {
      key: 'summary',
      label: text.summary,
      revised: !isSameTextValue(originalVersion.summary, currentVersion.summary),
      originalText: originalVersion.summary,
      currentText: currentVersion.summary,
      originalList: [] as string[],
      currentList: [] as string[],
      emptyText: '-'
    },
    {
      key: 'highlights',
      label: text.highlights,
      revised: !isSameListValue(originalVersion.highlights, currentVersion.highlights),
      originalText: '',
      currentText: '',
      originalList: originalVersion.highlights,
      currentList: currentVersion.highlights,
      emptyText: text.emptyHighlights
    },
    {
      key: 'riskPoints',
      label: text.riskPoints,
      revised: !isSameListValue(originalVersion.riskPoints, currentVersion.riskPoints),
      originalText: '',
      currentText: '',
      originalList: originalVersion.riskPoints,
      currentList: currentVersion.riskPoints,
      emptyText: text.emptyRiskPoints
    }
  ]
})

function compareReviewLogCreatedAt(left: TaskReportReviewLog, right: TaskReportReviewLog) {
  const leftTime = left.createdAt ? new Date(left.createdAt).getTime() : 0
  const rightTime = right.createdAt ? new Date(right.createdAt).getTime() : 0
  if (leftTime !== rightTime) {
    return leftTime - rightTime
  }
  return left.reviewLogId.localeCompare(right.reviewLogId)
}

const reviewLogVersionDiffMap = computed(() => {
  const versionMap = new Map<string, { before: ReportVersionContent; after: ReportVersionContent }>()
  const logs = [...reviewLogs.value].sort(compareReviewLogCreatedAt)
  let previousVersion = buildOriginalReportVersion()

  for (const reviewLog of logs) {
    const afterVersion = buildReviewLogVersion(previousVersion, reviewLog)
    versionMap.set(reviewLog.reviewLogId, {
      before: previousVersion,
      after: afterVersion
    })
    previousVersion = afterVersion
  }

  return versionMap
})

const selectedReviewVersionDiff = computed(() => {
  if (!selectedReviewLog.value) return null
  return reviewLogVersionDiffMap.value.get(selectedReviewLog.value.reviewLogId) || null
})

function openReviewVersionDiff(reviewLog: TaskReportReviewLog) {
  selectedReviewLog.value = reviewLog
  reviewVersionDialogVisible.value = true
}

function getReviewLogVersionDiff(reviewLogId: string) {
  return reviewLogVersionDiffMap.value.get(reviewLogId)
}

function canCompareReviewLogVersion(reviewLogId: string) {
  return reviewLogVersionDiffMap.value.has(reviewLogId)
}

const backToSourcePath = computed(() => {
  return resolveSourcePath(route.query.from)
})

const backToSourceText = computed(() => {
  return backToSourcePath.value.startsWith('/reports/')
    ? text.backToWorkbench
    : text.backToSource
})

watch(
  () => route.params.taskId,
  () => {
    loadReport()
  },
  { immediate: true }
)

async function loadReport() {
  loading.value = true
  editMode.value = false
  detail.value = null
  reviewLogs.value = []
  try {
    const taskId = route.params.taskId as string

    const [fullRes, reportRes, reviewLogsRes] = await Promise.all([
      fetchTaskFullDetail(taskId),
      fetchTaskReport(taskId),
      fetchTaskReportReviewLogs(taskId)
    ])

    reviewLogs.value = reviewLogsRes.success ? (reviewLogsRes.data || []) : []

    if (!fullRes.success) {
      ElMessage.error(fullRes.message || text.loadReportFailed)
      return
    }

    detail.value = fullRes.data
    if (reportRes.success && reportRes.data && detail.value) {
      detail.value.report = reportRes.data
    }

    if (detail.value?.report) {
      reviewForm.reviewStatus = detail.value.report.reviewStatus || REPORT_REVIEW_STATUS.PENDING
      reviewForm.reviewedBy = getCurrentUser().userId
      reviewForm.revisedSummary = detail.value.report.revisedSummary || detail.value.report.originalSummary || detail.value.report.summary || ''
      reviewForm.revisedHighlightsText = (
        detail.value.report.revisedHighlights ||
        detail.value.report.originalHighlights ||
        detail.value.report.reportMeta?.highlights ||
        []
      ).join('\n')
      reviewForm.revisedRiskPointsText = (
        detail.value.report.revisedRiskPoints ||
        detail.value.report.originalRiskPoints ||
        detail.value.report.reportMeta?.riskPoints ||
        []
      ).join('\n')
      reviewForm.reviewComment = detail.value.report.reviewComment || ''
    } else {
      reviewForm.reviewStatus = REPORT_REVIEW_STATUS.PENDING
      reviewForm.reviewedBy = getCurrentUser().userId
      reviewForm.revisedSummary = ''
      reviewForm.revisedHighlightsText = ''
      reviewForm.revisedRiskPointsText = ''
      reviewForm.reviewComment = ''
    }
  } catch (e: any) {
    detail.value = null
    reviewLogs.value = []
    ElMessage.error(e?.message || text.loadReportError)
  } finally {
    loading.value = false
  }
}

function goTaskDetail() {
  router.push({
    path: `/tasks/${route.params.taskId}`,
    query: buildFromQuery(backToSourcePath.value)
  })
}

async function handleCopySummary() {
  if (!detail.value?.report) return

  const summaryText = displaySummary.value
  if (!summaryText) {
    ElMessage.warning(text.noSummaryToCopy)
    return
  }

  try {
    await navigator.clipboard.writeText(summaryText)
    ElMessage.success(text.summaryCopied)
  } catch (e: any) {
    ElMessage.error(e?.message || text.copyFailed)
  }
}

function buildMarkdownReport() {
  if (!detail.value?.report) return ''

  const task = detail.value.taskDetail
  const report = detail.value.report
  const summary = detail.value.summary
  const highlights = displayHighlights.value
  const riskPoints = displayRiskPoints.value
  const riskWarnings = report.riskWarnings || []
  const snapshot = contextSnapshot.value
  const markdownContext = contextItems.value
  const markdownEvidenceItems = evidenceItems.value
  const markdownEvidenceRefs = evidenceRefs.value
  const markdownEvidenceText = markdownEvidenceItems.length > 0
    ? markdownEvidenceItems.map(formatMarkdownEvidenceItem).join('\n')
    : '- 暂无结构化证据'
  const markdownEvidenceRefsText = markdownEvidenceRefs.length > 0
    ? markdownEvidenceRefs.map(formatMarkdownEvidenceRef).filter(Boolean).join('\n')
    : '- 暂无证据引用'

  return `# 投研报告

## 基本信息
- 任务标题：${task.taskTitle}
- 标的代码：${task.targetCode}
- 标的名称：${task.targetName}
- 任务类型：${getTaskTypeText(task.taskType)}
- 报告类型：${report.reportType || report.reportMeta?.reportType || '-'}
- 完成时间：${formatDateTime(task.finishTime)}
- 置信度：${report.confidenceScore ?? '-'}
- 是否需要人工复核：${getHumanReviewText(report.needHumanReview)}
- 审核状态：${getReviewStatusText(report.reviewStatus || REPORT_REVIEW_STATUS.PENDING)}
- 审核人：${report.reviewedBy || '-'}

## 摘要
${displaySummary.value || '-'}

## 关键亮点
${highlights.length > 0 ? highlights.map((item) => `- ${item}`).join('\n') : '- 暂无关键亮点'}

## 风险点
${riskPoints.length > 0 ? riskPoints.map((item) => `- ${item}`).join('\n') : '- 暂无风险点'}

## 风险预警
${riskWarnings.length > 0 ? riskWarnings.map((item) => `- ${item}`).join('\n') : '- 暂无风险预警'}

## 上下文快照
${snapshot && markdownContext.length > 0 ? markdownContext.map((item) => `- ${item.label}：${item.value}`).join('\n') : '- 暂无上下文快照'}

## 证据链
${reviewSuggestion.value ? `- 审核建议：${reviewSuggestion.value}\n` : ''}${markdownEvidenceText}

### 证据引用
${markdownEvidenceRefsText}

## 执行摘要
- 步骤总数：${summary.stepCount}
- 成功步骤：${summary.successStepCount}
- 失败步骤：${summary.failedStepCount}
- Agent 数量：${summary.agentCount}
- 重试次数：${summary.retryCount}
`
}

function handleExportMarkdown() {
  if (!detail.value?.report) {
    ElMessage.warning(text.noReportToExport)
    return
  }

  const content = buildMarkdownReport()
  const blob = new Blob([content], { type: 'text/markdown;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')

  a.href = url
  a.download = `${detail.value.taskDetail.targetCode || 'report'}-report.md`
  a.click()

  URL.revokeObjectURL(url)
  ElMessage.success(text.markdownExported)
}

function handleCreateTask() {
  if (!detail.value?.report) return

  const report = detail.value.report
  const task = detail.value.taskDetail

  router.push({
    path: '/tasks/create',
      query: buildTaskCreateQuery({
        taskType: report.reviewStatus === REPORT_REVIEW_STATUS.APPROVED
          ? TASK_TYPE.FOLLOW_UP_RESEARCH
          : TASK_TYPE.REPORT_REVIEW,
        taskTitle: buildFollowUpTaskTitle(
          task.targetName,
          task.targetCode,
          resolveFollowUpTaskSuffix(report.reviewStatus)
        ),
        targetType: task.targetType,
        targetCode: task.targetCode,
        targetName: task.targetName,
        priority: resolveFollowUpTaskPriority(report.reviewStatus, report.needHumanReview, task.priority),
        sourceTaskId: task.taskId,
        sourceReportId: report.reportId || report.reportMeta?.reportId,
        sourceEventId: task.sourceEventId,
        sourceDomain: 'TASK_REPORT',
        sourceReviewStatus: report.reviewStatus,
        analysisScope: report.reviewStatus === REPORT_REVIEW_STATUS.APPROVED
          ? ANALYSIS_SCOPE.REPORT_FOLLOW_UP
          : ANALYSIS_SCOPE.REPORT_REVIEW_RECHECK,
        from: backToSourcePath.value || route.fullPath
      })
    })
  }

function goWorkbench() {
  if (!detail.value) return

  router.push({
    path: '/research-workbench',
    query: buildResearchWorkbenchQuery({
      targetCode: detail.value.taskDetail.targetCode,
      targetName: detail.value.taskDetail.targetName,
      from: route.fullPath
    })
  })
}

async function handleSaveReview() {
  if (!detail.value) return

  reviewLoading.value = true
  try {
    const taskId = route.params.taskId as string
    const success = await executeTaskReportReview(taskId, reviewForm, {
      fallbackSummary: detail.value.report?.displaySummary || detail.value.report?.summary || '',
      mode: 'save'
    })
    if (success) {
      editMode.value = false
      await loadReport()
    }
  } finally {
    reviewLoading.value = false
  }
}

async function handleApprove() {
  if (!detail.value) return

  reviewLoading.value = true
  try {
    const taskId = route.params.taskId as string
    const success = await executeTaskReportReview(taskId, reviewForm, {
      fallbackSummary: detail.value.report?.displaySummary || detail.value.report?.summary || '',
      mode: 'approve'
    })
    if (success) {
      editMode.value = false
      await loadReport()
    }
  } finally {
    reviewLoading.value = false
  }
}

async function handleReject() {
  if (!detail.value) return

  reviewLoading.value = true
  try {
    const taskId = route.params.taskId as string
    const success = await executeTaskReportReview(taskId, reviewForm, {
      fallbackSummary: detail.value.report?.displaySummary || detail.value.report?.summary || '',
      mode: 'reject'
    })
    if (success) {
      editMode.value = false
      await loadReport()
    }
  } finally {
    reviewLoading.value = false
  }
}

function resolveFollowUpTaskSuffix(reviewStatus?: ReportReviewStatus) {
  switch (reviewStatus) {
    case REPORT_REVIEW_STATUS.APPROVED:
      return '报告跟踪研究'
    case REPORT_REVIEW_STATUS.REJECTED:
      return '驳回报告复核研究'
    case REPORT_REVIEW_STATUS.PENDING:
    default:
      return '待审核报告复核研究'
  }
}

function resolveFollowUpTaskPriority(
  reviewStatus: ReportReviewStatus | undefined,
  needHumanReview: boolean | undefined,
  fallbackPriority: string
) {
  if (needHumanReview || reviewStatus === REPORT_REVIEW_STATUS.PENDING || reviewStatus === REPORT_REVIEW_STATUS.REJECTED) {
    return 'HIGH'
  }

  return fallbackPriority
}

</script>

<template>
  <div v-loading="loading">
    <div style="margin-bottom: 16px; display: flex; gap: 12px;">
      <el-button @click="goTaskDetail">{{ text.backToDetail }}</el-button>
      <el-button @click="router.push('/tasks')">{{ text.backToList }}</el-button>
      <el-button v-if="backToSourcePath" @click="router.push(backToSourcePath)">{{ backToSourceText }}</el-button>
      <el-button @click="loadReport">{{ text.refreshReport }}</el-button>

      <el-button
        v-if="detail"
        @click="goWorkbench"
      >
        {{ text.workbench }}
      </el-button>

      <el-button
        v-if="actionAccess.showCreateTask"
        type="info"
        @click="handleCreateTask"
      >
        {{ text.createTask }}
      </el-button>

      <el-button
        v-if="detail?.report"
        type="primary"
        @click="handleCopySummary"
      >
        {{ text.copySummary }}
      </el-button>

      <el-button
        v-if="detail?.report"
        type="success"
        @click="handleExportMarkdown"
      >
        {{ text.exportMarkdown }}
      </el-button>

      <el-button
        v-if="actionAccess.showApprove"
        type="success"
        :loading="reviewLoading"
        @click="handleApprove"
      >
        {{ text.approve }}
      </el-button>

      <el-button
        v-if="actionAccess.showReject"
        type="danger"
        :loading="reviewLoading"
        @click="handleReject"
      >
        {{ text.reject }}
      </el-button>

      <el-button
        v-if="actionAccess.showEdit"
        type="warning"
        @click="editMode = !editMode"
      >
        {{ editMode ? text.cancelEdit : text.editReport }}
      </el-button>
    </div>

    <template v-if="detail">
      <template v-if="detail.report">
        <el-card>
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center;">
              <div style="font-size: 18px; font-weight: 700;">{{ text.title }}</div>
              <el-tag type="success">{{ detail.report.reportType || detail.report.reportMeta?.reportType || 'REPORT' }}</el-tag>
            </div>
          </template>

          <el-descriptions :column="3" border>
            <el-descriptions-item :label="text.taskTitle">
              {{ detail.taskDetail.taskTitle }}
            </el-descriptions-item>
            <el-descriptions-item :label="text.targetCode">
              {{ detail.taskDetail.targetCode }}
            </el-descriptions-item>
            <el-descriptions-item :label="text.targetName">
              {{ detail.taskDetail.targetName }}
            </el-descriptions-item>

            <el-descriptions-item :label="text.taskType">
              {{ getTaskTypeText(detail.taskDetail.taskType) }}
            </el-descriptions-item>
            <el-descriptions-item :label="text.reportType">
              {{ detail.report.reportType || detail.report.reportMeta?.reportType || '-' }}
            </el-descriptions-item>
            <el-descriptions-item :label="text.reportVersion">
              v{{ detail.report.versionNo || 1 }}
            </el-descriptions-item>
            <el-descriptions-item :label="text.finishTime">
              {{ formatDateTime(detail.taskDetail.finishTime) }}
            </el-descriptions-item>

            <el-descriptions-item :label="text.confidenceScore">
              {{ detail.report.confidenceScore ?? '-' }}
            </el-descriptions-item>
            <el-descriptions-item :label="text.needHumanReview">
              {{ getHumanReviewText(detail.report.needHumanReview) }}
            </el-descriptions-item>
            <el-descriptions-item :label="text.workflowInstanceId">
              {{ detail.workflow?.workflowInstanceId || '-' }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card style="margin-top: 16px;">
          <template #header>
            <div style="font-weight: 700;">{{ text.summaryTitle }}</div>
          </template>
          <div style="line-height: 1.9; font-size: 15px;">
            {{ displaySummary || '-' }}
          </div>
        </el-card>

        <el-card style="margin-top: 16px;">
          <template #header>
            <div style="font-weight: 700;">{{ text.versionCompare }}</div>
          </template>

          <ReportVersionComparison
            :original="{
              summary: detail.report.originalSummary || detail.report.summary || detail.report.reportMeta?.summary,
              highlights: detail.report.originalHighlights || detail.report.reportMeta?.highlights || [],
              riskPoints: detail.report.originalRiskPoints || detail.report.reportMeta?.riskPoints || []
            }"
            :current="{
              summary: displaySummary,
              highlights: displayHighlights,
              riskPoints: displayRiskPoints
            }"
            :original-label="text.originalVersion"
            :current-label="text.currentVersion"
          />
        </el-card>

        <el-card style="margin-top: 16px;">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center;">
              <div style="font-weight: 700;">{{ text.reportSections }}</div>
              <el-tag type="info">{{ reportSections.length }}</el-tag>
            </div>
          </template>

          <el-empty
            v-if="reportSections.length === 0"
            :description="text.emptyReportSections"
          />

          <el-collapse v-else>
            <el-collapse-item
              v-for="section in reportSections"
              :key="section.sectionId || section.sectionCode"
              :name="section.sectionId || section.sectionCode"
            >
              <template #title>
                <div style="display: flex; align-items: center; justify-content: space-between; gap: 12px; width: 100%;">
                  <div style="display: flex; align-items: center; gap: 8px;">
                    <span style="font-weight: 600;">{{ section.sectionTitle || section.sectionCode }}</span>
                    <el-tag v-if="section.sectionCode" size="small" type="info" effect="plain">
                      {{ section.sectionCode }}
                    </el-tag>
                    <el-tag size="small" type="primary" effect="plain">
                      v{{ section.versionNo || detail.report.versionNo || 1 }}
                    </el-tag>
                  </div>
                  <div style="display: flex; align-items: center; gap: 8px;">
                    <el-tag v-if="section.reviewStatus" size="small" :type="getReviewStatusTagType(section.reviewStatus)">
                      {{ getReviewStatusText(section.reviewStatus) }}
                    </el-tag>
                    <el-tag v-if="section.revisedContent || section.revisedItems?.length" size="small" type="warning" effect="plain">
                      {{ text.sectionReviewed }}
                    </el-tag>
                  </div>
                </div>
              </template>

              <div
                v-if="section.displayContent || section.sectionContent"
                style="white-space: pre-wrap; word-break: break-word; line-height: 1.8; margin-bottom: 10px;"
              >
                {{ section.displayContent || section.sectionContent }}
              </div>

              <div v-if="(section.displayItems || section.sectionItems)?.length" style="display: flex; gap: 8px; flex-wrap: wrap;">
                <el-tag
                  v-for="(item, index) in (section.displayItems || section.sectionItems)"
                  :key="`${section.sectionId || section.sectionCode}-${index}`"
                  :type="section.revisedItems?.length ? 'warning' : 'info'"
                  effect="plain"
                >
                  {{ item }}
                </el-tag>
              </div>

              <el-descriptions
                v-if="section.reviewedBy || section.reviewedAt || section.reviewComment"
                :column="1"
                border
                size="small"
                style="margin-top: 12px;"
              >
                <el-descriptions-item v-if="section.reviewedBy" :label="text.reviewedBy">
                  {{ section.reviewedBy }}
                </el-descriptions-item>
                <el-descriptions-item v-if="section.reviewedAt" :label="text.reviewedAt">
                  {{ formatDateTime(section.reviewedAt) }}
                </el-descriptions-item>
                <el-descriptions-item v-if="section.reviewComment" :label="text.reviewComment">
                  {{ section.reviewComment }}
                </el-descriptions-item>
              </el-descriptions>
            </el-collapse-item>
          </el-collapse>
        </el-card>

        <el-row :gutter="16" style="margin-top: 16px;">
          <el-col :span="12">
            <el-card style="height: 100%;">
              <template #header>
                <div style="font-weight: 700;">{{ text.contextSnapshot }}</div>
              </template>

              <el-empty
                v-if="contextItems.length === 0"
                :description="text.emptyContextSnapshot"
              />

              <el-descriptions
                v-else
                :column="1"
                border
              >
                <el-descriptions-item
                  v-for="item in contextItems"
                  :key="item.label"
                  :label="item.label"
                >
                  {{ item.value }}
                </el-descriptions-item>
              </el-descriptions>
            </el-card>
          </el-col>

          <el-col :span="12">
            <el-card style="height: 100%;">
              <template #header>
                <div style="font-weight: 700;">{{ text.evidenceRefs }}</div>
              </template>
              <ReportEvidenceView
                :evidence-items="evidenceItems"
                :evidence-refs="evidenceRefs"
                :review-suggestion="reviewSuggestion"
                :empty-text="text.emptyEvidenceRefs"
              />
            </el-card>
          </el-col>
        </el-row>

        <el-card style="margin-top: 16px;">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center;">
              <div style="font-weight: 700;">{{ text.humanReviewRecords }}</div>
              <el-tag type="info">{{ humanReviewRecords.length }}</el-tag>
            </div>
          </template>

          <el-empty
            v-if="humanReviewRecords.length === 0"
            :description="text.emptyHumanReviewRecords"
          />

          <el-timeline v-else>
            <el-timeline-item
              v-for="item in humanReviewRecords"
              :key="item.reviewId"
              :timestamp="formatDateTime(item.createdAt)"
              placement="top"
            >
              <el-card shadow="never">
                <div style="display: flex; justify-content: space-between; gap: 12px; align-items: flex-start;">
                  <div>
                    <div style="font-weight: 600;">{{ item.reviewerId || '-' }}</div>
                    <div style="margin-top: 6px; color: var(--el-text-color-secondary);">
                      {{ text.reviewerRole }}：{{ item.reviewerRole || '-' }}
                    </div>
                  </div>
                  <el-tag :type="getReviewStatusTagType(item.reviewResult)">
                    {{ getReviewStatusText(item.reviewResult) }}
                  </el-tag>
                </div>

                <div style="margin-top: 10px; line-height: 1.7;">
                  {{ item.reviewComment || text.noReviewComment }}
                </div>

                <el-descriptions :column="1" border size="small" style="margin-top: 12px;">
                  <el-descriptions-item v-if="item.traceId" :label="text.reviewTrace">
                    {{ item.traceId }}
                  </el-descriptions-item>
                  <el-descriptions-item v-if="item.beforeSnapshotRef" :label="text.beforeSnapshot">
                    {{ item.beforeSnapshotRef }}
                  </el-descriptions-item>
                  <el-descriptions-item v-if="item.afterSnapshotRef" :label="text.afterSnapshot">
                    {{ item.afterSnapshotRef }}
                  </el-descriptions-item>
                </el-descriptions>
              </el-card>
            </el-timeline-item>
          </el-timeline>
        </el-card>

        <el-card style="margin-top: 16px;">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center;">
              <div style="font-weight: 700;">{{ text.reviewPanelTitle }}</div>
              <el-tag :type="getReviewStatusTagType(detail.report.reviewStatus || REPORT_REVIEW_STATUS.PENDING)">
                {{ getReviewStatusText(detail.report.reviewStatus || REPORT_REVIEW_STATUS.PENDING) }}
              </el-tag>
            </div>
          </template>

          <template v-if="editMode">
            <el-form label-width="120px">
              <el-form-item :label="text.reviewStatus">
                <el-select v-model="reviewForm.reviewStatus" style="width: 220px;">
                  <el-option
                    v-for="option in reviewStatusOptions"
                    :key="option.value"
                    :label="option.label"
                    :value="option.value"
                  />
                </el-select>
              </el-form-item>

              <el-form-item :label="text.reviewedBy">
                <el-input v-model="reviewForm.reviewedBy" style="width: 300px;" disabled />
              </el-form-item>

              <el-form-item :label="text.revisedSummary">
                <el-input
                  v-model="reviewForm.revisedSummary"
                  type="textarea"
                  :rows="4"
                  :placeholder="text.revisedSummaryPlaceholder"
                />
              </el-form-item>

              <el-form-item :label="text.revisedHighlights">
                <el-input
                  v-model="reviewForm.revisedHighlightsText"
                  type="textarea"
                  :rows="5"
                  :placeholder="text.revisedHighlightsPlaceholder"
                />
              </el-form-item>

              <el-form-item :label="text.revisedRiskPoints">
                <el-input
                  v-model="reviewForm.revisedRiskPointsText"
                  type="textarea"
                  :rows="5"
                  :placeholder="text.revisedRiskPointsPlaceholder"
                />
              </el-form-item>

              <el-form-item :label="text.reviewComment">
                <el-input
                  v-model="reviewForm.reviewComment"
                  type="textarea"
                  :rows="3"
                  :placeholder="text.reviewCommentPlaceholder"
                />
              </el-form-item>

              <el-form-item>
                <el-button type="primary" :loading="reviewLoading" @click="handleSaveReview">
                  {{ text.saveReview }}
                </el-button>
                <el-button @click="editMode = false">{{ text.cancelEdit }}</el-button>
              </el-form-item>
            </el-form>
          </template>

          <template v-else>
            <el-descriptions :column="2" border>
              <el-descriptions-item :label="text.reviewStatus">
                <el-tag :type="getReviewStatusTagType(detail.report.reviewStatus || REPORT_REVIEW_STATUS.PENDING)">
                  {{ getReviewStatusText(detail.report.reviewStatus || REPORT_REVIEW_STATUS.PENDING) }}
                </el-tag>
              </el-descriptions-item>

              <el-descriptions-item :label="text.reviewedBy">
                {{ detail.report.reviewedBy || '-' }}
              </el-descriptions-item>

              <el-descriptions-item :label="text.reviewedAt">
                {{ formatDateTime(detail.report.reviewedAt) }}
              </el-descriptions-item>

              <el-descriptions-item :label="text.reviewComment">
                {{ detail.report.reviewComment || '-' }}
              </el-descriptions-item>
            </el-descriptions>

            <div style="margin-top: 16px;">
              <div style="font-weight: 700; margin-bottom: 12px;">{{ text.reviewContentStatus }}</div>

              <el-row :gutter="16">
                <el-col
                  v-for="section in reviewContentSections"
                  :key="section.key"
                  :span="8"
                >
                  <el-card shadow="never" style="height: 100%;">
                    <template #header>
                      <div style="display: flex; justify-content: space-between; align-items: center;">
                        <span style="font-weight: 600;">{{ section.label }}</span>
                        <el-tag :type="section.revised ? 'warning' : 'success'">
                          {{ section.revised ? text.revisedYes : text.revisedNo }}
                        </el-tag>
                      </div>
                    </template>

                    <div style="font-weight: 600; margin-bottom: 8px;">{{ text.originalValue }}</div>
                    <template v-if="section.key === 'summary'">
                      <div style="white-space: pre-wrap; word-break: break-word; line-height: 1.8;">
                        {{ section.originalText || section.emptyText }}
                      </div>
                    </template>
                    <template v-else>
                      <div v-if="section.originalList.length > 0">
                        <el-tag
                          v-for="(item, index) in section.originalList"
                          :key="`original-${section.key}-${index}`"
                          style="margin-right: 8px; margin-bottom: 8px;"
                          effect="plain"
                        >
                          {{ item }}
                        </el-tag>
                      </div>
                      <div v-else>{{ section.emptyText }}</div>
                    </template>

                    <div style="font-weight: 600; margin: 16px 0 8px;">{{ text.currentValue }}</div>
                    <template v-if="section.key === 'summary'">
                      <div style="white-space: pre-wrap; word-break: break-word; line-height: 1.8;">
                        {{ section.currentText || section.emptyText }}
                      </div>
                    </template>
                    <template v-else>
                      <div v-if="section.currentList.length > 0">
                        <el-tag
                          v-for="(item, index) in section.currentList"
                          :key="`current-${section.key}-${index}`"
                          style="margin-right: 8px; margin-bottom: 8px;"
                          :type="section.revised ? (section.key === 'riskPoints' ? 'warning' : 'success') : 'info'"
                        >
                          {{ item }}
                        </el-tag>
                      </div>
                      <div v-else>{{ section.emptyText }}</div>
                    </template>
                  </el-card>
                </el-col>
              </el-row>
            </div>
          </template>
        </el-card>

        <el-card style="margin-top: 16px;">
          <template #header>
            <div style="font-weight: 700;">{{ text.reviewHistory }}</div>
          </template>

          <el-empty
            v-if="reviewLogs.length === 0"
            :description="text.emptyReviewHistory"
          />

          <el-timeline v-else>
            <el-timeline-item
              v-for="item in reviewLogs"
              :key="item.reviewLogId"
              :timestamp="formatDateTime(item.createdAt)"
              placement="top"
            >
              <el-card shadow="never">
                <div style="display: flex; justify-content: space-between; align-items: center;">
                  <div>
                    <div style="font-weight: 600;">
                      {{ item.reviewedBy || '-' }}
                      <el-tag size="small" type="primary" effect="plain" style="margin-left: 8px;">
                        v{{ item.versionNo || 1 }}
                      </el-tag>
                    </div>
                    <div style="margin-top: 6px; color: #666;">
                      {{ item.reviewComment || text.noReviewComment }}
                    </div>
                  </div>
                  <div style="display: flex; align-items: center; gap: 8px;">
                    <el-button
                      v-if="canCompareReviewLogVersion(item.reviewLogId)"
                      link
                      type="primary"
                      @click="openReviewVersionDiff(item)"
                    >
                      {{ text.openReviewVersionDiff }}
                    </el-button>
                    <el-tag :type="getReviewStatusTagType(item.reviewStatus)">
                      {{ getReviewStatusText(item.reviewStatus) }}
                    </el-tag>
                  </div>
                </div>

                <div v-if="item.revisedSummary" style="margin-top: 12px;">
                  <div style="font-weight: 600;">{{ text.revisedSummary }}</div>
                  <div style="margin-top: 6px; line-height: 1.8;">{{ item.revisedSummary }}</div>
                </div>

                <div v-if="item.revisedHighlights && item.revisedHighlights.length > 0" style="margin-top: 12px;">
                  <div style="font-weight: 600;">{{ text.revisedHighlights }}</div>
                  <div style="margin-top: 6px;">
                    <el-tag
                      v-for="(highlight, idx) in item.revisedHighlights"
                      :key="idx"
                      style="margin-right: 8px; margin-bottom: 8px;"
                      type="success"
                    >
                      {{ highlight }}
                    </el-tag>
                  </div>
                </div>

                <div v-if="item.revisedRiskPoints && item.revisedRiskPoints.length > 0" style="margin-top: 12px;">
                  <div style="font-weight: 600;">{{ text.revisedRiskPoints }}</div>
                  <div style="margin-top: 6px;">
                    <el-alert
                      v-for="(risk, idx) in item.revisedRiskPoints"
                      :key="idx"
                      :title="risk"
                      type="warning"
                      :closable="false"
                      show-icon
                      style="margin-bottom: 8px;"
                    />
                  </div>
                </div>
              </el-card>
            </el-timeline-item>
          </el-timeline>
        </el-card>

        <el-row :gutter="16" style="margin-top: 16px;">
          <el-col :span="12">
            <el-card style="height: 100%;">
              <template #header>
                <div style="font-weight: 700;">{{ text.highlights }}</div>
              </template>

              <el-empty
                v-if="displayHighlights.length === 0"
                :description="text.emptyHighlights"
              />

              <div v-else>
                <el-timeline>
                  <el-timeline-item
                    v-for="(item, index) in displayHighlights"
                    :key="index"
                  >
                    {{ item }}
                  </el-timeline-item>
                </el-timeline>
              </div>
            </el-card>
          </el-col>

          <el-col :span="12">
            <el-card style="height: 100%;">
              <template #header>
                <div style="font-weight: 700;">{{ text.riskPoints }}</div>
              </template>

              <el-empty
                v-if="displayRiskPoints.length === 0"
                :description="text.emptyRiskPoints"
              />

              <div v-else>
                <el-alert
                  v-for="(item, index) in displayRiskPoints"
                  :key="index"
                  :title="item"
                  type="warning"
                  :closable="false"
                  show-icon
                  style="margin-bottom: 10px;"
                />
              </div>
            </el-card>
          </el-col>
        </el-row>
      </template>

      <el-alert
        v-else
        :title="text.emptyFullReport"
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 16px;"
      />
    </template>

    <el-empty v-else :description="text.emptyReportData" />

    <el-dialog
      v-model="reviewVersionDialogVisible"
      :title="text.reviewVersionDialogTitle"
      width="980px"
    >
      <ReportVersionComparison
        v-if="selectedReviewVersionDiff"
        :original="selectedReviewVersionDiff.before"
        :current="selectedReviewVersionDiff.after"
        :original-label="text.beforeReviewVersion"
        :current-label="text.afterReviewVersion"
      />
    </el-dialog>
  </div>
</template>
