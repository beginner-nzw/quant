<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { read, utils, write, type WorkBook } from 'xlsx'
import { batchImportMarketEvents, createMarketEvent, fetchMarketEvent, fetchMarketEventIngestHistory, fetchMarketEventSourceConfigs, fetchMarketEventStats, fetchMarketEvents, mockIngestMarketEvents, previewBatchImportMarketEvents, syncMarketEventSource } from '../../api/task'
import type {
  MarketEventBatchImportResult,
  MarketEventBatchPreviewResult,
  MarketEventCreateForm,
  MarketEventCreateResult,
  EventSourceConfigItem,
  MarketEventIngestHistoryItem,
  MarketEventListItem,
  MarketEventMockIngestForm,
  MarketEventSourceSyncForm,
  MarketEventStats
} from '../../types/task'
import {
  ANALYSIS_SCOPE,
  MARKET_EVENT_IMPACT_LEVEL,
  MARKET_EVENT_STATUS,
  MARKET_EVENT_TYPE,
  TASK_TYPE,
  type MarketEventImpactLevel,
  type MarketEventStatus,
  type MarketEventType
} from '../../types/taskEnums'
import { formatDateTime } from '../../utils/format'
import { buildResearchWorkbenchQuery } from '../../utils/researchWorkbench'
import { resolveCenterActionAccess } from '../../utils/taskActionAccess'
import {
  getHumanReviewText,
  getMarketEventImpactTagType,
  getMarketEventImpactText,
  getMarketEventStatusTagType,
  getMarketEventStatusText,
  getMarketEventTypeTagType,
  getMarketEventTypeText,
  getMarketIntelligenceTypeTagType,
  getMarketIntelligenceTypeText,
  getReviewStatusTagType,
  getReviewStatusText,
  getRiskLevelTagType,
  getRiskLevelText,
  getSignalDirectionTagType,
  getSignalDirectionText,
  getSignalStrengthTagType,
  getSignalStrengthText,
  getTaskStatusTagType,
  getTaskStatusText
} from '../../utils/task'
import { buildFollowUpTaskTitle, buildTaskCreateQuery } from '../../utils/taskCreate'
import { buildFromQuery, resolveSourcePath } from '../../utils/taskNavigation'

const route = useRoute()
const router = useRouter()

const text = {
  title: '市场事件中心',
  backToSource: '返回来源页',
  createEvent: '录入事件',
  sourceSync: '事件源同步',
  batchImport: '批量导入',
  targetCode: '标的代码',
  targetCodePlaceholder: '如 600519.SH / 01929.HK',
  targetName: '标的名称',
  targetNamePlaceholder: '如 贵州茅台 / 周大福',
  eventType: '事件类型',
  eventTypePlaceholder: '全部类型',
  impactLevel: '影响等级',
  impactLevelPlaceholder: '全部等级',
  eventStatus: '事件状态',
  eventStatusPlaceholder: '全部状态',
  search: '查询',
  reset: '重置',
  refresh: '刷新',
  empty: '暂无市场事件数据',
  totalCount: '事件总数',
  activeCount: '活跃事件',
  highImpactCount: '高影响事件',
  trackedCount: '已跟踪事件',
  todayCount: '今日事件',
  eventId: '事件 ID',
  eventTitle: '事件标题',
  eventSummary: '事件摘要',
  sourceChannel: '来源渠道',
  impactTag: '影响等级',
  autoTriggerStatus: '自动触发',
  autoTriggerRuleCode: '触发规则',
  autoTriggerTask: '自动触发任务',
  autoTriggerAttemptedAt: '触发时间',
  autoTriggerMessage: '触发结果',
  followUpStatus: '跟踪状态',
  followUpTaskCount: '跟踪任务数',
  latestFollowUpTask: '最近跟踪任务',
  latestFollowUpCreatedAt: '最近跟踪时间',
  relatedReportCount: '关联报告数',
  latestReport: '最新报告',
  latestReportType: '报告类型',
  latestReportReviewStatus: '报告审核状态',
  latestReportConfidenceScore: '报告置信度',
  latestNeedHumanReview: '人工复核',
  latestReportCreatedAt: '报告生成时间',
  latestReportSummary: '报告摘要',
  relationCount: '关联实体数',
  relations: '关联实体',
  derivedRisk: '派生风险',
  derivedRiskLevel: '风险等级',
  derivedRiskCount: '风险总数',
  derivedWarningCount: '风险预警数',
  derivedRiskPointCount: '风险点数',
  derivedSignal: '派生信号',
  derivedSignalDirection: '信号方向',
  derivedSignalStrength: '信号强度',
  derivedSignalScore: '信号分数',
  derivedIntelligence: '派生情报',
  derivedIntelligenceType: '情报类型',
  occurredAt: '事件发生时间',
  createdAt: '录入时间',
  action: '操作',
  openDetail: '查看事件',
  openLatestFollowUpTask: '查看跟踪任务',
  openAutoTriggerTask: '查看自动任务',
  openLatestReport: '查看最新报告',
  createTask: '发起研究',
  workbench: '投研工作台',
  detailTitle: '事件详情',
  sourceUrl: '来源链接',
  createdBy: '录入人',
  noSourceUrl: '无来源链接',
  noSummary: '暂无事件摘要',
  noLatestReport: '暂无关联报告',
  createDialogTitle: '录入市场事件',
  createResultDialogTitle: '录入结果',
  batchImportDialogTitle: '批量导入事件',
  batchImportHint: '支持直接粘贴 JSON 数组。每一项结构与单条录入一致，后端会逐条导入并返回成功/失败结果。',
  batchImportContent: '导入内容',
  batchImportPlaceholder: '请粘贴事件 JSON 数组',
  batchImportExample: '填充示例',
  batchImportTemplate: '下载模板',
  batchImportCsvTemplate: '下载 CSV 模板',
  batchImportUpload: '上传文件',
  batchImportUploadSuccess: '文件内容已载入',
  batchImportUploadFailed: '导入文件解析失败',
  batchImportFileTypeError: '请上传 JSON 或 CSV 文件',
  previewBatchImport: '预校验预览',
  submitBatchImport: '开始导入',
  batchPreviewTitle: '预校验结果',
  batchPreviewValidCount: '有效数',
  batchPreviewInvalidCount: '无效数',
  batchPreviewDuplicateCount: '预估去重数',
  batchPreviewAutoTriggerCount: '预估自动入队数',
  batchPreviewResult: '校验结果',
  batchPreviewDuplicateSource: '去重判断',
  batchPreviewNormalized: '标准化结果',
  batchPreviewEstimatedTaskType: '预估任务类型',
  batchPreviewEmpty: '请先执行预校验预览',
  batchPreviewSuccess: '预校验完成',
  batchPreviewFailed: '预校验失败',
  batchPreviewError: '预校验异常',
  batchPreviewApplyImportable: '只保留可导入项',
  batchPreviewImportImportable: '仅导入可导入项',
  exportBatchPreviewJson: '导出预校验 JSON',
  exportBatchPreviewCsv: '导出预校验 CSV',
  exportImportableEventsJson: '导出可导入项 JSON',
  batchPreviewImportableEmpty: '预校验结果中没有可导入项',
  batchPreviewApplySuccess: '已仅保留可导入项',
  batchImportResultDialogTitle: '批量导入结果',
  batchImportTotalCount: '总条数',
  batchImportSuccessCount: '成功数',
  batchImportFailedCount: '失败数',
  batchImportAutoTriggeredCount: '自动入队数',
  exportBatchImportJson: '导出 JSON',
  exportBatchImportCsv: '导出 CSV',
  batchImportItemNo: '序号',
  batchImportMessage: '结果摘要',
  submitEvent: '保存事件',
  eventTitlePlaceholder: '例如：公司发布年报后利润增速不及预期',
  eventSummaryPlaceholder: '请填写事件摘要、影响判断和关注点',
  sourceChannelPlaceholder: '例如：公告 / 新闻 / 手工录入',
  sourceUrlPlaceholder: '可选，填写原始链接',
  occurredAtPlaceholder: '请选择事件发生时间',
  eventTypeRequired: '请选择事件类型',
  eventTitleRequired: '请输入事件标题',
  eventSummaryRequired: '请输入事件摘要',
  impactLevelRequired: '请选择影响等级',
  occurredAtRequired: '请选择事件发生时间',
  targetCodeRequired: '请输入标的代码',
  targetNameRequired: '请输入标的名称',
  createEventSuccess: '事件录入成功',
  createEventFailed: '事件录入失败',
  createEventError: '事件录入异常',
  batchImportParseFailed: '批量导入内容不是合法 JSON 数组',
  batchImportEmpty: '请先填写批量导入内容',
  batchImportSuccess: '批量导入完成',
  batchImportFailed: '批量导入失败',
  batchImportError: '批量导入异常',
  loadCreatedEventFailed: '事件详情获取失败',
  loadCreatedEventError: '事件详情获取异常',
  loadStatsFailed: '市场事件统计加载失败',
  loadStatsError: '市场事件统计加载异常',
  loadListFailed: '市场事件列表加载失败',
  loadListError: '市场事件列表加载异常',
  viewEventDetail: '查看事件详情',
  ingestHistoryTitle: '事件接入历史',
  ingestHistoryEmpty: '暂无事件接入历史',
  ingestSource: '接入来源',
  ingestDetail: '接入明细',
  ingestTotalCount: '接入条数',
  ingestSuccessCount: '成功条数',
  ingestFailedCount: '失败条数',
  ingestDuplicateCount: '去重条数',
  ingestAutoTriggeredCount: '自动入队数',
  ingestResultStatus: '结果状态',
  ingestErrorMessage: '错误信息',
  ingestOperator: '操作人',
  ingestCreatedAt: '接入时间',
  ingestSummary: '接入摘要',
  ingestSourceStatsTitle: '按来源统计',
  ingestSourceStatsEmpty: '暂无来源统计',
  ingestSourceSummary: '来源汇总',
  loadIngestHistoryFailed: '事件接入历史加载失败',
  loadIngestHistoryError: '事件接入历史加载异常',
  sourceSyncDialogTitle: '事件源手动同步',
  sourceSyncSource: '同步来源',
  sourceSyncItemCount: '同步条数',
  sourceSyncSubmit: '开始同步',
  sourceSyncSuccess: '事件源同步完成',
  sourceSyncFailed: '事件源同步失败',
  sourceSyncError: '事件源同步异常'
} as const

const defaultStats: MarketEventStats = {
  totalCount: 0,
  activeCount: 0,
  highImpactCount: 0,
  trackedCount: 0,
  todayCount: 0
}

const eventTypeOptions = Object.values(MARKET_EVENT_TYPE).map((value) => ({
  label: getMarketEventTypeText(value),
  value
}))

const impactLevelOptions = Object.values(MARKET_EVENT_IMPACT_LEVEL).map((value) => ({
  label: getMarketEventImpactText(value),
  value
}))

const eventStatusOptions = Object.values(MARKET_EVENT_STATUS).map((value) => ({
  label: getMarketEventStatusText(value),
  value
}))

const eventTypeValueSet = new Set<string>(Object.values(MARKET_EVENT_TYPE))
const impactLevelValueSet = new Set<string>(Object.values(MARKET_EVENT_IMPACT_LEVEL))
const eventStatusValueSet = new Set<string>(Object.values(MARKET_EVENT_STATUS))

const followUpStatusTextMap: Record<string, string> = {
  NOT_TRACKED: '未跟踪',
  TRACKING: '跟踪中',
  COMPLETED: '已完成',
  FAILED: '跟踪失败'
}

const followUpStatusTagTypeMap: Record<string, 'info' | 'warning' | 'success' | 'danger'> = {
  NOT_TRACKED: 'info',
  TRACKING: 'warning',
  COMPLETED: 'success',
  FAILED: 'danger'
}

const autoTriggerStatusTextMap: Record<string, string> = {
  DISABLED: '已关闭',
  NO_MATCH: '未命中',
  SUCCESS: '已创建任务',
  FAILED: '触发失败',
  WILL_TRIGGER: '已入队',
  SKIPPED_DUPLICATE: '重复跳过',
  INVALID: '校验失败'
}

const autoTriggerStatusTagTypeMap: Record<string, 'info' | 'warning' | 'success' | 'danger'> = {
  DISABLED: 'info',
  NO_MATCH: 'warning',
  SUCCESS: 'success',
  FAILED: 'danger',
  WILL_TRIGGER: 'success',
  SKIPPED_DUPLICATE: 'warning',
  INVALID: 'danger'
}

const duplicateSourceTextMap: Record<string, string> = {
  EXISTING_EVENT: '命中已有事件',
  SAME_BATCH: '批次内重复'
}

const ingestSourceTagTypeMap: Record<string, 'info' | 'warning' | 'success' | 'primary' | 'danger'> = {
  MANUAL_CREATE: 'primary',
  BATCH_IMPORT: 'info',
  MOCK_INGEST: 'success',
  MANUAL: 'primary',
  IMPORT: 'info',
  NEWS: 'success',
  ANNOUNCEMENT: 'warning',
  POLICY: 'primary',
  RISK: 'danger',
  MOCK: 'success'
}

const defaultMockSourceOptions = [
  { label: '新闻快讯源', value: 'NEWS_WIRE' },
  { label: '交易所公告源', value: 'EXCHANGE_ANNOUNCEMENT' },
  { label: '政策跟踪源', value: 'POLICY_TRACKER' },
  { label: '风险监测源', value: 'RISK_MONITOR' }
]

const statsCards = computed(() => [
  { label: text.totalCount, value: stats.value.totalCount, color: '#303133' },
  { label: text.activeCount, value: stats.value.activeCount, color: '#f56c6c' },
  { label: text.highImpactCount, value: stats.value.highImpactCount, color: '#e6a23c' },
  { label: text.trackedCount, value: stats.value.trackedCount, color: '#67c23a' },
  { label: text.todayCount, value: stats.value.todayCount, color: '#409eff' }
])

const ingestSourceStats = computed(() => {
  const grouped = new Map<string, {
    sourceType: string
    sourceLabel: string
    sourceCode?: string
    sourceName?: string
    sourceCategory?: string
    sourceChannel?: string
    totalCount: number
    successCount: number
    failedCount: number
    duplicateCount: number
    autoTriggeredCount: number
  }>()

  ingestHistoryRecords.value.forEach((item) => {
    const sourceKey = item.sourceCode || item.sourceType || 'UNKNOWN'
    const current = grouped.get(sourceKey) || {
      sourceType: item.sourceType || 'UNKNOWN',
      sourceLabel: item.sourceName || item.sourceLabel || item.sourceCode || item.sourceType || 'UNKNOWN',
      sourceCode: item.sourceCode,
      sourceName: item.sourceName,
      sourceCategory: item.sourceCategory,
      sourceChannel: item.sourceChannel,
      totalCount: 0,
      successCount: 0,
      failedCount: 0,
      duplicateCount: 0,
      autoTriggeredCount: 0
    }
    current.sourceCode = current.sourceCode || item.sourceCode
    current.sourceName = current.sourceName || item.sourceName
    current.sourceCategory = current.sourceCategory || item.sourceCategory
    current.sourceChannel = current.sourceChannel || item.sourceChannel
    current.totalCount += item.totalCount || 0
    current.successCount += item.successCount || 0
    current.failedCount += item.failedCount || 0
    current.duplicateCount += item.duplicateCount || 0
    current.autoTriggeredCount += item.autoTriggeredCount || 0
    grouped.set(sourceKey, current)
  })

  return Array.from(grouped.values()).sort((left, right) => right.totalCount - left.totalCount)
})

const loading = ref(false)
const ingestHistoryLoading = ref(false)
const creating = ref(false)
const batchPreviewing = ref(false)
const batchImporting = ref(false)
const stats = ref<MarketEventStats>({ ...defaultStats })
const detailDialogVisible = ref(false)
const createDialogVisible = ref(false)
const mockIngestDialogVisible = ref(false)
const sourceSyncDialogVisible = ref(false)
const createResultDialogVisible = ref(false)
const batchImportDialogVisible = ref(false)
const batchImportResultDialogVisible = ref(false)
const previewEditDialogVisible = ref(false)
const previewOnlyInvalid = ref(false)
const selectedEvent = ref<MarketEventListItem | null>(null)
const ingestHistoryRecords = ref<MarketEventIngestHistoryItem[]>([])
const eventSourceConfigs = ref<EventSourceConfigItem[]>([])
const createdEventResult = ref<MarketEventCreateResult | null>(null)
const createdEventFeedback = ref<MarketEventListItem | null>(null)
const batchImportContent = ref('')
const batchImportPreviewResult = ref<MarketEventBatchPreviewResult | null>(null)
const batchImportResult = ref<MarketEventBatchImportResult | null>(null)
const batchImportFileInput = ref<HTMLInputElement | null>(null)
const mockIngesting = ref(false)
const sourceSyncing = ref(false)
const batchImportWorkbook = ref<WorkBook | null>(null)
const batchImportSheetNames = ref<string[]>([])
const batchImportSelectedSheet = ref('')
const batchImportFileName = ref('')
const batchImportSourceType = ref<'JSON' | 'CSV' | 'EXCEL' | ''>('')
const editingPreviewItemNo = ref<number | null>(null)

const pageData = reactive({
  total: 0,
  pageNum: 1,
  pageSize: 10,
  records: [] as MarketEventListItem[]
})

const query = reactive({
  targetCode: '',
  targetName: '',
  eventType: '' as MarketEventType | '',
  impactLevel: '' as MarketEventImpactLevel | '',
  eventStatus: '' as MarketEventStatus | ''
})

const createForm = reactive<MarketEventCreateForm>({
  targetType: 'STOCK',
  targetCode: '',
  targetName: '',
  eventType: MARKET_EVENT_TYPE.NEWS,
  eventTitle: '',
  eventSummary: '',
  sourceChannel: 'MANUAL_ENTRY',
  sourceUrl: '',
  impactLevel: MARKET_EVENT_IMPACT_LEVEL.MEDIUM,
  eventStatus: MARKET_EVENT_STATUS.ACTIVE,
  occurredAt: buildCurrentDateTimeValue()
})

const previewEditForm = reactive<MarketEventCreateForm>({
  targetType: 'STOCK',
  targetCode: '',
  targetName: '',
  eventType: MARKET_EVENT_TYPE.NEWS,
  eventTitle: '',
  eventSummary: '',
  sourceChannel: 'MANUAL_ENTRY',
  sourceUrl: '',
  impactLevel: MARKET_EVENT_IMPACT_LEVEL.MEDIUM,
  eventStatus: MARKET_EVENT_STATUS.ACTIVE,
  occurredAt: buildCurrentDateTimeValue()
})

const mockIngestForm = reactive<MarketEventMockIngestForm>({
  targetType: 'STOCK',
  targetCode: '',
  targetName: '',
  sourcePreset: 'NEWS_WIRE',
  itemCount: 3
})

const sourceSyncForm = reactive<MarketEventSourceSyncForm>({
  targetType: 'STOCK',
  targetCode: '',
  targetName: '',
  sourceCode: '',
  itemCount: 3
})

const batchImportExample = `[
  {
    "targetType": "STOCK",
    "targetCode": "600519.SH",
    "targetName": "贵州茅台",
    "eventType": "ANNOUNCEMENT",
    "eventTitle": "贵州茅台披露季度经营数据",
    "eventSummary": "公司披露季度经营数据，营收与利润保持稳健增长，市场关注后续估值消化情况。",
    "sourceChannel": "MANUAL_IMPORT",
    "sourceUrl": "",
    "impactLevel": "HIGH",
    "eventStatus": "ACTIVE",
    "occurredAt": "2026-04-04T09:30"
  },
  {
    "targetType": "STOCK",
    "targetCode": "000001.SZ",
    "targetName": "平安银行",
    "eventType": "EARNINGS",
    "eventTitle": "平安银行发布业绩快报",
    "eventSummary": "业绩快报显示净利润承压，市场关注零售业务修复节奏和拨备变化。",
    "sourceChannel": "MANUAL_IMPORT",
    "sourceUrl": "",
    "impactLevel": "MEDIUM",
    "eventStatus": "ACTIVE",
    "occurredAt": "2026-04-04T10:00"
  }
]`

const batchImportCsvTemplate = `targetType,targetCode,targetName,eventType,eventTitle,eventSummary,sourceChannel,sourceUrl,impactLevel,eventStatus,occurredAt
STOCK,600519.SH,贵州茅台,ANNOUNCEMENT,贵州茅台披露季度经营数据,公司披露季度经营数据，营收与利润保持稳健增长，市场关注后续估值消化情况。,MANUAL_IMPORT,,HIGH,ACTIVE,2026-04-04T09:30
STOCK,000001.SZ,平安银行,EARNINGS,平安银行发布业绩快报,业绩快报显示净利润承压，市场关注零售业务修复节奏和拨备变化。,MANUAL_IMPORT,,MEDIUM,ACTIVE,2026-04-04T10:00`

const actionAccess = computed(() => resolveCenterActionAccess())
const sourcePath = computed(() => resolveSourcePath(route.query.from))
const visibleBatchPreviewItems = computed(() => {
  const items = batchImportPreviewResult.value?.items || []
  if (!previewOnlyInvalid.value) {
    return items
  }
  return items.filter((item) => !(item.importable ?? (item.valid && !item.duplicate)))
})
const previewIssueSummary = computed(() => {
  const items = batchImportPreviewResult.value?.items || []
  const counter = new Map<string, { label: string; count: number; type: 'danger' | 'warning' }>()

  items.forEach((item) => {
    const importable = item.importable ?? (item.valid && !item.duplicate)
    if (importable) {
      return
    }

    let key = ''
    let label = ''
    let type: 'danger' | 'warning' = 'danger'
    if (item.duplicate) {
      const duplicateText = getDuplicateSourceText(item.duplicateSource)
      key = `duplicate:${item.duplicateSource || 'UNKNOWN'}`
      label = `重复：${duplicateText}`
      type = 'warning'
    } else {
      const invalidFieldText = getPreviewInvalidFieldText(item.invalidField)
      key = `invalid:${item.invalidField || 'UNKNOWN'}`
      label = `字段问题：${invalidFieldText}`
    }

    const current = counter.get(key)
    if (current) {
      current.count += 1
      return
    }
    counter.set(key, { label, count: 1, type })
  })

  return Array.from(counter.values()).sort((left, right) => right.count - left.count)
})
const currentEditingPreviewItem = computed(() => {
  if (!batchImportPreviewResult.value || !editingPreviewItemNo.value) {
    return null
  }
  return batchImportPreviewResult.value.items.find((item) => item.itemNo === editingPreviewItemNo.value) || null
})
const batchImportSourceSummary = computed(() => {
  if (!batchImportFileName.value || !batchImportSourceType.value) {
    return ''
  }
  if (batchImportSourceType.value === 'EXCEL' && batchImportSelectedSheet.value) {
    return `${batchImportFileName.value} / Excel / ${batchImportSelectedSheet.value}`
  }
  return `${batchImportFileName.value} / ${batchImportSourceType.value}`
})
const mockSourceOptions = computed(() => {
  const dynamicOptions = eventSourceConfigs.value
    .filter((item) => item.enabled && item.supportsMockIngest)
    .map((item) => ({
      label: item.sourceName || item.sourceCode,
      value: item.sourceCode
    }))
  return dynamicOptions.length ? dynamicOptions : defaultMockSourceOptions
})
const syncSourceOptions = computed(() => {
  return eventSourceConfigs.value
    .filter((item) => item.enabled)
    .map((item) => ({
      label: `${item.sourceName || item.sourceCode} / ${item.ingestMode || '-'}`,
      value: item.sourceCode
    }))
})

function syncQueryFromRoute() {
  query.targetCode = getRouteQueryValue(route.query.targetCode)
  query.targetName = getRouteQueryValue(route.query.targetName)
  query.eventType = resolveEventType(getRouteQueryValue(route.query.eventType))
  query.impactLevel = resolveImpactLevel(getRouteQueryValue(route.query.impactLevel))
  query.eventStatus = resolveEventStatus(getRouteQueryValue(route.query.eventStatus))
  pageData.pageNum = normalizePageValue(route.query.pageNum, 1)
  pageData.pageSize = normalizePageValue(route.query.pageSize, 10)
}

async function loadStats() {
  try {
    const res = await fetchMarketEventStats()
    if (res.success) {
      stats.value = res.data || { ...defaultStats }
    } else {
      stats.value = { ...defaultStats }
      ElMessage.error(res.message || text.loadStatsFailed)
    }
  } catch (error: any) {
    stats.value = { ...defaultStats }
    ElMessage.error(error?.message || text.loadStatsError)
  }
}

async function loadEvents() {
  loading.value = true
  try {
    const res = await fetchMarketEvents({
      pageNum: pageData.pageNum,
      pageSize: pageData.pageSize,
      targetCode: normalizeQueryValue(query.targetCode),
      targetName: normalizeQueryValue(query.targetName),
      eventType: query.eventType || undefined,
      impactLevel: query.impactLevel || undefined,
      eventStatus: query.eventStatus || undefined
    })
    if (res.success) {
      pageData.total = res.data.total
      pageData.records = res.data.records
    } else {
      pageData.total = 0
      pageData.records = []
      ElMessage.error(res.message || text.loadListFailed)
    }
  } catch (error: any) {
    pageData.total = 0
    pageData.records = []
    ElMessage.error(error?.message || text.loadListError)
  } finally {
    loading.value = false
  }
}

async function loadIngestHistory() {
  ingestHistoryLoading.value = true
  try {
    const res = await fetchMarketEventIngestHistory()
    if (res.success) {
      ingestHistoryRecords.value = res.data || []
    } else {
      ingestHistoryRecords.value = []
      ElMessage.error(res.message || text.loadIngestHistoryFailed)
    }
  } catch (error: any) {
    ingestHistoryRecords.value = []
    ElMessage.error(error?.message || text.loadIngestHistoryError)
  } finally {
    ingestHistoryLoading.value = false
  }
}

async function loadEventSourceConfigs() {
  try {
    const res = await fetchMarketEventSourceConfigs()
    if (res.success) {
      eventSourceConfigs.value = res.data || []
    } else {
      eventSourceConfigs.value = []
    }
  } catch {
    eventSourceConfigs.value = []
  }
}

async function reloadAll() {
  await Promise.all([loadStats(), loadEvents(), loadIngestHistory(), loadEventSourceConfigs()])
}

function buildRouteQuery() {
  return {
    targetCode: normalizeQueryValue(query.targetCode),
    targetName: normalizeQueryValue(query.targetName),
    eventType: query.eventType || undefined,
    impactLevel: query.impactLevel || undefined,
    eventStatus: query.eventStatus || undefined,
    pageNum: pageData.pageNum > 1 ? String(pageData.pageNum) : undefined,
    pageSize: pageData.pageSize !== 10 ? String(pageData.pageSize) : undefined,
    from: sourcePath.value || undefined
  }
}

function isSameRouteState(nextQuery: ReturnType<typeof buildRouteQuery>) {
  return getRouteQueryValue(route.query.targetCode) === (nextQuery.targetCode || '')
    && getRouteQueryValue(route.query.targetName) === (nextQuery.targetName || '')
    && getRouteQueryValue(route.query.eventType) === (nextQuery.eventType || '')
    && getRouteQueryValue(route.query.impactLevel) === (nextQuery.impactLevel || '')
    && getRouteQueryValue(route.query.eventStatus) === (nextQuery.eventStatus || '')
    && getRouteQueryValue(route.query.pageNum) === (nextQuery.pageNum || '')
    && getRouteQueryValue(route.query.pageSize) === (nextQuery.pageSize || '')
}

async function navigateWithQuery() {
  const nextQuery = buildRouteQuery()
  if (isSameRouteState(nextQuery)) {
    await reloadAll()
    return
  }

  await router.replace({
    path: route.path,
    query: nextQuery
  })
}

function handleSearch() {
  pageData.pageNum = 1
  return navigateWithQuery()
}

function handleReset() {
  query.targetCode = ''
  query.targetName = ''
  query.eventType = ''
  query.impactLevel = ''
  query.eventStatus = ''
  pageData.pageNum = 1
  pageData.pageSize = 10
  return navigateWithQuery()
}

function handlePageChange(pageNum: number) {
  pageData.pageNum = pageNum
  return navigateWithQuery()
}

function handlePageSizeChange(pageSize: number) {
  pageData.pageSize = pageSize
  pageData.pageNum = 1
  return navigateWithQuery()
}

function openEventDetail(row: MarketEventListItem) {
  selectedEvent.value = row
  detailDialogVisible.value = true
}

function openCreatedEventDetail() {
  if (!createdEventFeedback.value) {
    return
  }
  selectedEvent.value = createdEventFeedback.value
  createResultDialogVisible.value = false
  detailDialogVisible.value = true
}

function openCreateDialog() {
  resetCreateForm()
  createDialogVisible.value = true
}

function openMockIngestDialog() {
  mockIngestForm.targetType = 'STOCK'
  mockIngestForm.targetCode = query.targetCode || ''
  mockIngestForm.targetName = query.targetName || ''
  mockIngestForm.sourcePreset = mockSourceOptions.value[0]?.value || 'NEWS_WIRE'
  mockIngestForm.itemCount = 3
  mockIngestDialogVisible.value = true
}

function openSourceSyncDialog() {
  if (!syncSourceOptions.value.length) {
    ElMessage.warning('当前没有可同步的事件源')
    return
  }
  sourceSyncForm.targetType = 'STOCK'
  sourceSyncForm.targetCode = query.targetCode || ''
  sourceSyncForm.targetName = query.targetName || ''
  sourceSyncForm.sourceCode = syncSourceOptions.value[0]?.value || ''
  sourceSyncForm.itemCount = 3
  sourceSyncDialogVisible.value = true
}

function openBatchImportDialog() {
  batchImportPreviewResult.value = null
  batchImportContent.value = batchImportContent.value || batchImportExample
  batchImportDialogVisible.value = true
}

function resetBatchImportFileContext() {
  batchImportWorkbook.value = null
  batchImportSheetNames.value = []
  batchImportSelectedSheet.value = ''
  batchImportFileName.value = ''
  batchImportSourceType.value = ''
}

function fillBatchImportExample() {
  resetBatchImportFileContext()
  batchImportPreviewResult.value = null
  batchImportContent.value = batchImportExample
}

function downloadBatchImportTemplate() {
  const blob = new Blob([batchImportExample], { type: 'application/json;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = 'market-event-import-template.json'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

function downloadBatchImportCsvTemplate() {
  const blob = new Blob([batchImportCsvTemplate], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = 'market-event-import-template.csv'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

function downloadBatchImportExcelTemplate() {
  const worksheet = utils.json_to_sheet(JSON.parse(batchImportExample) as MarketEventCreateForm[])
  const workbook = utils.book_new()
  utils.book_append_sheet(workbook, worksheet, 'market_events')
  const content = write(workbook, {
    bookType: 'xlsx',
    type: 'array'
  })
  const blob = new Blob([content], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
  })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = 'market-event-import-template.xlsx'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

function openBatchImportFilePicker() {
  batchImportFileInput.value?.click()
}

async function handleBatchImportFileChange(event: Event) {
  const input = event.target as HTMLInputElement | null
  const file = input?.files?.[0]
  if (!file) {
    return
  }
  const fileName = file.name.toLowerCase()
  const isJson = fileName.endsWith('.json')
  const isCsv = fileName.endsWith('.csv')
  const isExcel = fileName.endsWith('.xlsx') || fileName.endsWith('.xls')
  if (!isJson && !isCsv && !isExcel) {
    ElMessage.error('请上传 JSON、CSV 或 Excel 文件')
    input.value = ''
    return
  }

  try {
    batchImportFileName.value = file.name
    if (isExcel) {
      const buffer = await file.arrayBuffer()
      const workbook = read(buffer, { type: 'array' })
      batchImportWorkbook.value = workbook
      batchImportSheetNames.value = workbook.SheetNames || []
      batchImportSelectedSheet.value = batchImportSheetNames.value[0] || ''
      batchImportSourceType.value = 'EXCEL'
      applyBatchImportWorkbookSheet(batchImportSelectedSheet.value)
    } else {
      resetBatchImportFileContext()
      batchImportFileName.value = file.name
      const content = await file.text()
      if (isCsv) {
        const parsed = parseBatchImportCsv(content)
        batchImportContent.value = JSON.stringify(parsed, null, 2)
        batchImportSourceType.value = 'CSV'
      } else {
        JSON.parse(content)
        batchImportContent.value = content
        batchImportSourceType.value = 'JSON'
      }
    }
    batchImportPreviewResult.value = null
    ElMessage.success('文件内容已加载')
  } catch {
    ElMessage.error('导入文件解析失败')
  } finally {
    if (input) {
      input.value = ''
    }
  }
}

function applyBatchImportWorkbookSheet(sheetName: string) {
  if (!batchImportWorkbook.value || !sheetName) {
    return
  }
  const worksheet = batchImportWorkbook.value.Sheets[sheetName]
  if (!worksheet) {
    return
  }
  const rows = utils.sheet_to_json<Record<string, unknown>>(worksheet, { defval: '' })
  if (!rows.length) {
    throw new Error('empty worksheet')
  }
  const parsed = rows.map((row) => normalizeBatchImportRecord(row))
  batchImportContent.value = JSON.stringify(parsed, null, 2)
  batchImportPreviewResult.value = null
}

function parseBatchImportPayload(): MarketEventCreateForm[] | null {
  const content = batchImportContent.value.trim()
  if (!content) {
    ElMessage.warning(text.batchImportEmpty)
    return null
  }

  let parsed: unknown
  try {
    parsed = JSON.parse(content)
  } catch {
    ElMessage.error(text.batchImportParseFailed)
    return null
  }

  if (!Array.isArray(parsed)) {
    ElMessage.error(text.batchImportParseFailed)
    return null
  }

  return parsed as MarketEventCreateForm[]
}

function parseBatchImportExcel(content: ArrayBuffer): MarketEventCreateForm[] {
  const workbook = read(content, { type: 'array' })
  const sheetName = workbook.SheetNames[0]
  if (!sheetName) {
    throw new Error('empty workbook')
  }
  const worksheet = workbook.Sheets[sheetName]
  if (!worksheet) {
    throw new Error('empty worksheet')
  }
  const rows = utils.sheet_to_json<Record<string, unknown>>(worksheet, { defval: '' })
  if (!rows.length) {
    throw new Error('empty worksheet')
  }
  return rows.map((row) => normalizeBatchImportRecord(row))
}

function parseBatchImportCsv(content: string): MarketEventCreateForm[] {
  const rows = content
    .replace(/^\uFEFF/, '')
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
  if (rows.length < 2) {
    throw new Error('empty csv')
  }

  const headerRow = rows[0]
  if (!headerRow) {
    throw new Error('empty csv header')
  }

  const headers = parseCsvLine(headerRow).map((item) => resolveCsvHeader(item))
  return rows.slice(1).map((line) => {
    const values = parseCsvLine(line)
    const record: Record<string, string> = {}
    headers.forEach((header, index) => {
      if (header) {
        record[header] = values[index] ?? ''
      }
    })
    return normalizeBatchImportRecord(record)
  })
}

function normalizeBatchImportRecord(record: Record<string, unknown>): MarketEventCreateForm {
  const normalized: Record<string, string> = {}
  Object.entries(record).forEach(([key, value]) => {
    const resolvedKey = resolveCsvHeader(key)
    if (resolvedKey) {
      normalized[resolvedKey] = String(value ?? '').trim()
    }
  })
  return {
    targetType: normalized.targetType || 'STOCK',
    targetCode: normalized.targetCode || '',
    targetName: normalized.targetName || '',
    eventType: normalized.eventType || '',
    eventTitle: normalized.eventTitle || '',
    eventSummary: normalized.eventSummary || '',
    sourceChannel: normalized.sourceChannel || '',
    sourceUrl: normalized.sourceUrl || '',
    impactLevel: normalized.impactLevel || '',
    eventStatus: normalized.eventStatus || 'ACTIVE',
    occurredAt: normalized.occurredAt || ''
  }
}

function parseCsvLine(line: string) {
  const values: string[] = []
  let current = ''
  let inQuotes = false

  for (let i = 0; i < line.length; i++) {
    const char = line[i]
    const next = line[i + 1]
    if (char === '"') {
      if (inQuotes && next === '"') {
        current += '"'
        i++
      } else {
        inQuotes = !inQuotes
      }
      continue
    }
    if (char === ',' && !inQuotes) {
      values.push(current.trim())
      current = ''
      continue
    }
    current += char
  }

  values.push(current.trim())
  return values
}

function resolveCsvHeader(header: string) {
  const normalized = header.trim().toLowerCase()
  const mapping: Record<string, string> = {
    targettype: 'targetType',
    type: 'targetType',
    '标的类型': 'targetType',
    targetcode: 'targetCode',
    code: 'targetCode',
    symbol: 'targetCode',
    ticker: 'targetCode',
    '标的代码': 'targetCode',
    targetname: 'targetName',
    name: 'targetName',
    '标的名称': 'targetName',
    eventtype: 'eventType',
    event: 'eventType',
    '事件类型': 'eventType',
    eventtitle: 'eventTitle',
    title: 'eventTitle',
    '事件标题': 'eventTitle',
    eventsummary: 'eventSummary',
    summary: 'eventSummary',
    content: 'eventSummary',
    '事件摘要': 'eventSummary',
    sourcechannel: 'sourceChannel',
    source: 'sourceChannel',
    channel: 'sourceChannel',
    '来源渠道': 'sourceChannel',
    sourceurl: 'sourceUrl',
    url: 'sourceUrl',
    link: 'sourceUrl',
    '来源链接': 'sourceUrl',
    impactlevel: 'impactLevel',
    impact: 'impactLevel',
    level: 'impactLevel',
    '影响等级': 'impactLevel',
    eventstatus: 'eventStatus',
    status: 'eventStatus',
    '事件状态': 'eventStatus',
    occurredat: 'occurredAt',
    occurred_at: 'occurredAt',
    datetime: 'occurredAt',
    time: 'occurredAt',
    '发生时间': 'occurredAt'
  }
  return mapping[normalized] || ''
}

function exportBatchImportResultJson() {
  if (!batchImportResult.value) {
    return
  }
  const content = JSON.stringify(batchImportResult.value, null, 2)
  downloadTextFile(content, 'market-event-import-result.json', 'application/json;charset=utf-8')
}

function exportBatchImportResultCsv() {
  if (!batchImportResult.value) {
    return
  }
  const header = [
    'itemNo',
    'success',
    'duplicate',
    'eventId',
    'targetCode',
    'targetName',
    'eventTitle',
    'autoTriggerStatus',
    'autoTriggerTaskId',
    'message'
  ]
  const lines = [
    header.join(','),
    ...batchImportResult.value.items.map((item) => ([
      item.itemNo,
      item.success ? 'true' : 'false',
      item.duplicate ? 'true' : 'false',
      item.eventId || '',
      item.targetCode || '',
      item.targetName || '',
      item.eventTitle || '',
      item.autoTriggerStatus || '',
      item.autoTriggerTaskId || '',
      item.message || ''
    ].map(escapeCsvValue).join(',')))
  ]
  downloadTextFile(lines.join('\n'), 'market-event-import-result.csv', 'text/csv;charset=utf-8')
}

function exportWorkbook(workbook: ReturnType<typeof utils.book_new>, fileName: string) {
  const content = write(workbook, {
    bookType: 'xlsx',
    type: 'array'
  })
  const blob = new Blob([content], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
  })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

function exportBatchImportResultExcel() {
  if (!batchImportResult.value) {
    return
  }

  const workbook = utils.book_new()
  const summarySheet = utils.json_to_sheet([
    {
      总条数: batchImportResult.value.totalCount,
      成功数: batchImportResult.value.successCount,
      失败数: batchImportResult.value.failedCount,
      去重数: batchImportResult.value.duplicateCount,
      自动入队数: batchImportResult.value.autoTriggeredCount
    }
  ])
  const detailSheet = utils.json_to_sheet(
    batchImportResult.value.items.map((item) => ({
      序号: item.itemNo,
      录入成功: item.success ? '是' : '否',
      命中已有事件: item.duplicate ? '是' : '否',
      事件ID: item.eventId || '',
      标的代码: item.targetCode || '',
      标的名称: item.targetName || '',
      事件标题: item.eventTitle || '',
      自动触发状态: getAutoTriggerStatusText(item.autoTriggerStatus),
      自动触发任务: item.autoTriggerTaskId || '',
      结果摘要: item.message || ''
    }))
  )
  utils.book_append_sheet(workbook, summarySheet, 'summary')
  utils.book_append_sheet(workbook, detailSheet, 'items')
  exportWorkbook(workbook, 'market-event-import-result.xlsx')
}

async function previewBatchImport() {
  const events = parseBatchImportPayload()
  if (!events) {
    return
  }

  batchPreviewing.value = true
  try {
    const res = await previewBatchImportMarketEvents({ events })
    if (!res.success) {
      ElMessage.error(res.message || text.batchPreviewFailed)
      return
    }

    batchImportPreviewResult.value = res.data || null
    ElMessage.success(text.batchPreviewSuccess)
  } catch (error: any) {
    ElMessage.error(error?.message || text.batchPreviewError)
  } finally {
    batchPreviewing.value = false
  }
}

function exportBatchPreviewResultJson() {
  if (!batchImportPreviewResult.value) {
    ElMessage.warning(text.batchPreviewEmpty)
    return
  }

  const content = JSON.stringify(batchImportPreviewResult.value, null, 2)
  downloadTextFile(content, 'market-event-import-preview.json', 'application/json;charset=utf-8')
}

function exportBatchPreviewResultCsv() {
  if (!batchImportPreviewResult.value) {
    ElMessage.warning(text.batchPreviewEmpty)
    return
  }

  const header = [
    'itemNo',
    'valid',
    'importable',
    'duplicate',
    'invalidField',
    'duplicateSource',
    'existingEventId',
    'targetCode',
    'targetName',
    'eventTitle',
    'normalizedTargetCode',
    'normalizedEventType',
    'normalizedImpactLevel',
    'normalizedEventStatus',
    'normalizedSourceChannel',
    'autoTriggerStatus',
    'autoTriggerRuleCode',
    'estimatedTaskType',
    'message'
  ]
  const lines = [
    header.join(','),
    ...batchImportPreviewResult.value.items.map((item) => ([
      item.itemNo,
      item.valid ? 'true' : 'false',
      item.importable ? 'true' : 'false',
      item.duplicate ? 'true' : 'false',
      item.invalidField || '',
      item.duplicateSource || '',
      item.existingEventId || '',
      item.targetCode || '',
      item.targetName || '',
      item.eventTitle || '',
      item.normalizedTargetCode || '',
      item.normalizedEventType || '',
      item.normalizedImpactLevel || '',
      item.normalizedEventStatus || '',
      item.normalizedSourceChannel || '',
      item.autoTriggerStatus || '',
      item.autoTriggerRuleCode || '',
      item.estimatedTaskType || '',
      item.message || ''
    ].map(escapeCsvValue).join(',')))
  ]
  downloadTextFile(lines.join('\n'), 'market-event-import-preview.csv', 'text/csv;charset=utf-8')
}

function exportBatchPreviewResultExcel() {
  if (!batchImportPreviewResult.value) {
    ElMessage.warning(text.batchPreviewEmpty)
    return
  }

  const workbook = utils.book_new()
  const summarySheet = utils.json_to_sheet([
    {
      总条数: batchImportPreviewResult.value.totalCount,
      有效数: batchImportPreviewResult.value.validCount,
      无效数: batchImportPreviewResult.value.invalidCount,
      预估去重数: batchImportPreviewResult.value.duplicateCount,
      预估自动入队数: batchImportPreviewResult.value.autoTriggerCandidateCount
    }
  ])
  const issueSummarySheet = utils.json_to_sheet(
    previewIssueSummary.value.map((item) => ({
      问题类型: item.label,
      数量: item.count,
      级别: item.type === 'warning' ? '警告' : '错误'
    }))
  )
  const detailSheet = utils.json_to_sheet(
    batchImportPreviewResult.value.items.map((item) => ({
      序号: item.itemNo,
      校验结果: item.valid ? '通过' : '失败',
      可导入: getPreviewImportableText(item.importable),
      问题字段: getPreviewInvalidFieldText(item.invalidField),
      去重判断: getDuplicateSourceText(item.duplicateSource),
      已有事件ID: item.existingEventId || '',
      标的代码: item.targetCode || '',
      标的名称: item.targetName || '',
      事件标题: item.eventTitle || '',
      标准化标的代码: item.normalizedTargetCode || '',
      标准化事件类型: item.normalizedEventType || '',
      标准化影响等级: item.normalizedImpactLevel || '',
      标准化事件状态: item.normalizedEventStatus || '',
      标准化来源渠道: item.normalizedSourceChannel || '',
      自动触发状态: getAutoTriggerStatusText(item.autoTriggerStatus),
      触发规则: item.autoTriggerRuleCode || '',
      预估任务类型: item.estimatedTaskType || '',
      结果摘要: item.message || ''
    }))
  )
  utils.book_append_sheet(workbook, summarySheet, 'summary')
  utils.book_append_sheet(workbook, issueSummarySheet, 'issues')
  utils.book_append_sheet(workbook, detailSheet, 'items')
  exportWorkbook(workbook, 'market-event-import-preview.xlsx')
}

function getPreviewImportableEvents() {
  if (!batchImportPreviewResult.value) {
    ElMessage.warning(text.batchPreviewEmpty)
    return null
  }

  const events = parseBatchImportPayload()
  if (!events) {
    return null
  }

  const importableItemNos = new Set(
    batchImportPreviewResult.value.items
      .filter((item) => item.importable ?? (item.valid && !item.duplicate))
      .map((item) => item.itemNo)
  )

  const importableEvents = events.filter((_, index) => importableItemNos.has(index + 1))
  if (!importableEvents.length) {
    ElMessage.warning(text.batchPreviewImportableEmpty)
    return null
  }
  return importableEvents
}

function exportImportableEventsJson() {
  const importableEvents = getPreviewImportableEvents()
  if (!importableEvents) {
    return
  }

  const content = JSON.stringify(importableEvents, null, 2)
  downloadTextFile(content, 'market-event-importable-events.json', 'application/json;charset=utf-8')
}

function openPreviewItemEditor(row: { itemNo: number }) {
  const events = parseBatchImportPayload()
  if (!events) {
    return
  }

  const target = events[row.itemNo - 1]
  if (!target) {
    ElMessage.warning('未找到对应导入项')
    return
  }

  editingPreviewItemNo.value = row.itemNo
  previewEditForm.targetType = target.targetType || 'STOCK'
  previewEditForm.targetCode = target.targetCode || ''
  previewEditForm.targetName = target.targetName || ''
  previewEditForm.eventType = (target.eventType || MARKET_EVENT_TYPE.NEWS) as MarketEventType | string
  previewEditForm.eventTitle = target.eventTitle || ''
  previewEditForm.eventSummary = target.eventSummary || ''
  previewEditForm.sourceChannel = target.sourceChannel || 'MANUAL_ENTRY'
  previewEditForm.sourceUrl = target.sourceUrl || ''
  previewEditForm.impactLevel = (target.impactLevel || MARKET_EVENT_IMPACT_LEVEL.MEDIUM) as MarketEventImpactLevel | string
  previewEditForm.eventStatus = (target.eventStatus || MARKET_EVENT_STATUS.ACTIVE) as MarketEventStatus | string
  previewEditForm.occurredAt = target.occurredAt || buildCurrentDateTimeValue()
  previewEditDialogVisible.value = true
}

function savePreviewItemEdit() {
  if (!editingPreviewItemNo.value) {
    ElMessage.warning('未找到需要修正的导入项')
    return
  }

  const events = parseBatchImportPayload()
  if (!events) {
    return
  }

  const index = editingPreviewItemNo.value - 1
  if (!events[index]) {
    ElMessage.warning('未找到对应导入项')
    return
  }

  events[index] = {
    targetType: previewEditForm.targetType || 'STOCK',
    targetCode: previewEditForm.targetCode.trim(),
    targetName: previewEditForm.targetName.trim(),
    eventType: previewEditForm.eventType,
    eventTitle: previewEditForm.eventTitle.trim(),
    eventSummary: previewEditForm.eventSummary.trim(),
    sourceChannel: previewEditForm.sourceChannel?.trim() || '',
    sourceUrl: previewEditForm.sourceUrl?.trim() || '',
    impactLevel: previewEditForm.impactLevel,
    eventStatus: previewEditForm.eventStatus,
    occurredAt: previewEditForm.occurredAt
  }

  batchImportContent.value = JSON.stringify(events, null, 2)
  previewEditDialogVisible.value = false
  editingPreviewItemNo.value = null
  ElMessage.success('已更新导入项，请重新预校验')
}

function applyPreviewImportableItems() {
  const importableEvents = getPreviewImportableEvents()
  if (!importableEvents) {
    return
  }

  batchImportContent.value = JSON.stringify(importableEvents, null, 2)
  ElMessage.success(text.batchPreviewApplySuccess)
}

async function submitBatchImportRequest(events: MarketEventCreateForm[]) {
  batchImporting.value = true
  try {
    const res = await batchImportMarketEvents({ events })
    if (!res.success) {
      ElMessage.error(res.message || text.batchImportFailed)
      return
    }

    batchImportResult.value = res.data || null
    batchImportDialogVisible.value = false
    batchImportResultDialogVisible.value = true
    ElMessage.success(text.batchImportSuccess)
    await reloadAll()
  } catch (error: any) {
    ElMessage.error(error?.message || text.batchImportError)
  } finally {
    batchImporting.value = false
  }
}

async function submitMockIngest() {
  if (!mockIngestForm.targetCode.trim()) {
    ElMessage.warning('请输入标的代码')
    return
  }
  if (!mockIngestForm.targetName.trim()) {
    ElMessage.warning('请输入标的名称')
    return
  }

  mockIngesting.value = true
  try {
    const res = await mockIngestMarketEvents({
      targetType: mockIngestForm.targetType || 'STOCK',
      targetCode: mockIngestForm.targetCode.trim(),
      targetName: mockIngestForm.targetName.trim(),
      sourcePreset: mockIngestForm.sourcePreset,
      itemCount: mockIngestForm.itemCount || 3
    })
    if (!res.success) {
      ElMessage.error(res.message || '模拟接入失败')
      return
    }

    batchImportResult.value = res.data || null
    mockIngestDialogVisible.value = false
    batchImportResultDialogVisible.value = true
    ElMessage.success('模拟接入完成')
    await reloadAll()
  } catch (error: any) {
    ElMessage.error(error?.message || '模拟接入异常')
  } finally {
    mockIngesting.value = false
  }
}

async function submitSourceSync() {
  if (!sourceSyncForm.sourceCode) {
    ElMessage.warning('请选择同步来源')
    return
  }
  if (!sourceSyncForm.targetCode.trim()) {
    ElMessage.warning('请输入标的代码')
    return
  }
  if (!sourceSyncForm.targetName.trim()) {
    ElMessage.warning('请输入标的名称')
    return
  }

  sourceSyncing.value = true
  try {
    const res = await syncMarketEventSource(sourceSyncForm.sourceCode, {
      targetType: sourceSyncForm.targetType || 'STOCK',
      targetCode: sourceSyncForm.targetCode.trim(),
      targetName: sourceSyncForm.targetName.trim(),
      sourceCode: sourceSyncForm.sourceCode,
      itemCount: sourceSyncForm.itemCount || 3
    })
    if (!res.success) {
      ElMessage.error(res.message || text.sourceSyncFailed)
      return
    }

    batchImportResult.value = res.data || null
    sourceSyncDialogVisible.value = false
    batchImportResultDialogVisible.value = true
    ElMessage.success(text.sourceSyncSuccess)
    await reloadAll()
  } catch (error: any) {
    ElMessage.error(error?.message || text.sourceSyncError)
  } finally {
    sourceSyncing.value = false
  }
}

async function submitPreviewImportableBatchImport() {
  const importableEvents = getPreviewImportableEvents()
  if (!importableEvents) {
    return
  }

  await submitBatchImportRequest(importableEvents)
}

function escapeCsvValue(value: string | number) {
  const normalized = String(value ?? '')
  if (/[",\n]/.test(normalized)) {
    return `"${normalized.replace(/"/g, '""')}"`
  }
  return normalized
}

function downloadTextFile(content: string, fileName: string, mimeType: string) {
  const blob = new Blob([content], { type: mimeType })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

async function submitEvent() {
  if (!normalizeQueryValue(createForm.targetCode)) {
    ElMessage.warning(text.targetCodeRequired)
    return
  }
  if (!normalizeQueryValue(createForm.targetName)) {
    ElMessage.warning(text.targetNameRequired)
    return
  }
  if (!createForm.eventType) {
    ElMessage.warning(text.eventTypeRequired)
    return
  }
  if (!normalizeQueryValue(createForm.eventTitle)) {
    ElMessage.warning(text.eventTitleRequired)
    return
  }
  if (!normalizeQueryValue(createForm.eventSummary)) {
    ElMessage.warning(text.eventSummaryRequired)
    return
  }
  if (!createForm.impactLevel) {
    ElMessage.warning(text.impactLevelRequired)
    return
  }
  if (!normalizeQueryValue(createForm.occurredAt)) {
    ElMessage.warning(text.occurredAtRequired)
    return
  }

  creating.value = true
  try {
    const res = await createMarketEvent({
      ...createForm,
      targetCode: normalizeQueryValue(createForm.targetCode) || '',
      targetName: normalizeQueryValue(createForm.targetName) || '',
      eventTitle: normalizeQueryValue(createForm.eventTitle) || '',
      eventSummary: normalizeQueryValue(createForm.eventSummary) || '',
      sourceChannel: normalizeQueryValue(createForm.sourceChannel),
      sourceUrl: normalizeQueryValue(createForm.sourceUrl)
    })
    if (!res.success) {
      ElMessage.error(res.message || text.createEventFailed)
      return
    }

    createdEventResult.value = res.data || null
    ElMessage.success(res.data?.message || text.createEventSuccess)
    createDialogVisible.value = false
    await loadCreatedEventFeedback(res.data?.eventId)
    query.targetCode = createForm.targetCode
    query.targetName = createForm.targetName
    pageData.pageNum = 1
    await navigateWithQuery()
  } catch (error: any) {
    ElMessage.error(error?.message || text.createEventError)
  } finally {
    creating.value = false
  }
}

async function loadCreatedEventFeedback(eventId?: string) {
  if (!eventId) {
    createdEventFeedback.value = null
    createResultDialogVisible.value = !!createdEventResult.value
    return
  }
  try {
    const res = await fetchMarketEvent(eventId)
    if (res.success) {
      createdEventFeedback.value = res.data || null
    } else {
      createdEventFeedback.value = null
      ElMessage.warning(res.message || text.loadCreatedEventFailed)
    }
  } catch (error: any) {
    createdEventFeedback.value = null
    ElMessage.warning(error?.message || text.loadCreatedEventError)
  } finally {
    createResultDialogVisible.value = true
  }
}

async function submitBatchImport() {
  const events = parseBatchImportPayload()
  if (!events) {
    return
  }

  await submitBatchImportRequest(events)
}

function goTaskDetail(taskId?: string) {
  if (!taskId) return
  router.push({
    path: `/tasks/${taskId}`,
    query: buildFromQuery(route.fullPath)
  })
}

function goTaskReport(taskId?: string) {
  if (!taskId) return
  router.push({
    path: `/tasks/${taskId}/report`,
    query: buildFromQuery(route.fullPath)
  })
}

function goWorkbench(row: MarketEventListItem) {
  router.push({
    path: '/research-workbench',
    query: buildResearchWorkbenchQuery({
      targetCode: row.targetCode,
      targetName: row.targetName,
      from: route.fullPath
    })
  })
}

function createFollowUpTask(row: MarketEventListItem) {
  router.push({
    path: '/tasks/create',
    query: buildTaskCreateQuery({
      taskType: TASK_TYPE.FOLLOW_UP_RESEARCH,
      taskTitle: buildFollowUpTaskTitle(row.targetName, row.targetCode, '事件跟踪研究'),
      targetType: row.targetType,
      targetCode: row.targetCode,
      targetName: row.targetName,
      priority: resolvePriorityByImpactLevel(row.impactLevel),
      sourceEventId: row.eventId,
      sourceDomain: 'MARKET_EVENT',
      analysisScope: ANALYSIS_SCOPE.INTELLIGENCE_FOLLOW_UP,
      from: route.fullPath
    })
  })
}

function getFollowUpStatusText(status?: string) {
  return status ? (followUpStatusTextMap[status] || status) : '-'
}

function getFollowUpStatusTagType(status?: string) {
  return status ? (followUpStatusTagTypeMap[status] || 'info') : 'info'
}

function getAutoTriggerStatusText(status?: string) {
  return status ? (autoTriggerStatusTextMap[status] || status) : '-'
}

function getAutoTriggerStatusTagType(status?: string) {
  return status ? (autoTriggerStatusTagTypeMap[status] || 'info') : 'info'
}

function getDuplicateSourceText(source?: string) {
  return source ? (duplicateSourceTextMap[source] || source) : '-'
}

function getIngestSourceTagType(sourceType?: string) {
  return sourceType ? (ingestSourceTagTypeMap[sourceType] || 'info') : 'info'
}

function getIngestResultStatusTagType(status?: string) {
  switch (status) {
    case 'SUCCESS':
      return 'success'
    case 'PARTIAL_SUCCESS':
      return 'warning'
    case 'FAILED':
      return 'danger'
    default:
      return 'info'
  }
}

function getIngestResultStatusText(status?: string) {
  switch (status) {
    case 'SUCCESS':
      return '成功'
    case 'PARTIAL_SUCCESS':
      return '部分成功'
    case 'FAILED':
      return '失败'
    default:
      return status || '-'
  }
}

function getIngestSourceSummary(item: {
  successCount?: number
  failedCount?: number
  duplicateCount?: number
  autoTriggeredCount?: number
}) {
  return `成功 ${item.successCount || 0} / 失败 ${item.failedCount || 0} / 去重 ${item.duplicateCount || 0} / 自动入队 ${item.autoTriggeredCount || 0}`
}

function getPreviewImportableText(importable?: boolean) {
  if (importable == null) {
    return '-'
  }
  return importable ? '可导入' : '不可导入'
}

function getPreviewImportableTagType(importable?: boolean) {
  if (importable == null) {
    return 'info'
  }
  return importable ? 'success' : 'danger'
}

function getPreviewInvalidFieldText(field?: string) {
  const fieldTextMap: Record<string, string> = {
    events: '导入内容',
    targetCode: '标的代码',
    targetName: '标的名称',
    eventType: '事件类型',
    eventTitle: '事件标题',
    eventSummary: '事件摘要',
    impactLevel: '影响等级',
    occurredAt: '发生时间'
  }
  return field ? (fieldTextMap[field] || field) : '-'
}

function isPreviewFieldInvalid(field: string) {
  return currentEditingPreviewItem.value?.invalidField === field
}

function togglePreviewOnlyInvalid() {
  previewOnlyInvalid.value = !previewOnlyInvalid.value
}

function resolvePriorityByImpactLevel(level?: string) {
  switch (level) {
    case MARKET_EVENT_IMPACT_LEVEL.HIGH:
      return 'HIGH'
    case MARKET_EVENT_IMPACT_LEVEL.LOW:
      return 'LOW'
    case MARKET_EVENT_IMPACT_LEVEL.MEDIUM:
    default:
      return 'MEDIUM'
  }
}

function resetCreateForm() {
  createForm.targetType = 'STOCK'
  createForm.targetCode = ''
  createForm.targetName = ''
  createForm.eventType = MARKET_EVENT_TYPE.NEWS
  createForm.eventTitle = ''
  createForm.eventSummary = ''
  createForm.sourceChannel = 'MANUAL_ENTRY'
  createForm.sourceUrl = ''
  createForm.impactLevel = MARKET_EVENT_IMPACT_LEVEL.MEDIUM
  createForm.eventStatus = MARKET_EVENT_STATUS.ACTIVE
  createForm.occurredAt = buildCurrentDateTimeValue()
}

function buildCurrentDateTimeValue() {
  const now = new Date()
  const local = new Date(now.getTime() - now.getTimezoneOffset() * 60000)
  return local.toISOString().slice(0, 16)
}

function resolveEventType(value: string): MarketEventType | '' {
  return eventTypeValueSet.has(value) ? value as MarketEventType : ''
}

function resolveImpactLevel(value: string): MarketEventImpactLevel | '' {
  return impactLevelValueSet.has(value) ? value as MarketEventImpactLevel : ''
}

function resolveEventStatus(value: string): MarketEventStatus | '' {
  return eventStatusValueSet.has(value) ? value as MarketEventStatus : ''
}

function normalizePageValue(value: unknown, fallback: number) {
  const raw = Number(getRouteQueryValue(value))
  return Number.isFinite(raw) && raw > 0 ? raw : fallback
}

function normalizeQueryValue(value?: string | null) {
  const normalized = value?.trim()
  return normalized || undefined
}

function getRouteQueryValue(value: unknown) {
  if (Array.isArray(value)) {
    return value[0] ? String(value[0]) : ''
  }
  return value ? String(value) : ''
}

watch(
  [
    () => route.query.targetCode,
    () => route.query.targetName,
    () => route.query.eventType,
    () => route.query.impactLevel,
    () => route.query.eventStatus,
    () => route.query.pageNum,
    () => route.query.pageSize
  ],
  () => {
    syncQueryFromRoute()
    reloadAll()
  },
  { immediate: true }
)

watch(batchImportContent, () => {
  batchImportPreviewResult.value = null
  previewOnlyInvalid.value = false
})

watch(batchImportSelectedSheet, (sheetName, previousSheetName) => {
  if (!batchImportWorkbook.value || !sheetName || !previousSheetName || sheetName === previousSheetName) {
    return
  }
  try {
    applyBatchImportWorkbookSheet(sheetName)
    ElMessage.success(`已切换到工作表：${sheetName}`)
  } catch {
    ElMessage.error('Excel 工作表解析失败')
  }
})
</script>

<template>
  <div>
    <div style="margin-bottom: 16px; display: flex; gap: 12px; flex-wrap: wrap;">
      <el-button v-if="sourcePath" @click="router.push(sourcePath)">{{ text.backToSource }}</el-button>
      <el-button @click="reloadAll">{{ text.refresh }}</el-button>
      <el-button
        v-if="actionAccess.showCreateTask"
        type="primary"
        @click="openCreateDialog"
      >
        {{ text.createEvent }}
      </el-button>
      <el-button
        v-if="actionAccess.showCreateTask"
        type="success"
        plain
        @click="openBatchImportDialog"
      >
        {{ text.batchImport }}
      </el-button>
      <el-button
        v-if="actionAccess.showCreateTask"
        type="warning"
        plain
        @click="openMockIngestDialog"
      >
        模拟接入
      </el-button>
      <el-button
        v-if="actionAccess.showCreateTask"
        type="info"
        plain
        @click="openSourceSyncDialog"
      >
        {{ text.sourceSync }}
      </el-button>
    </div>

    <el-card style="margin-bottom: 16px;">
      <template #header>
        <div style="font-weight: 700;">{{ text.title }}</div>
      </template>

      <el-form inline>
        <el-form-item :label="text.targetCode">
          <el-input v-model="query.targetCode" :placeholder="text.targetCodePlaceholder" clearable />
        </el-form-item>
        <el-form-item :label="text.targetName">
          <el-input v-model="query.targetName" :placeholder="text.targetNamePlaceholder" clearable />
        </el-form-item>
        <el-form-item :label="text.eventType">
          <el-select v-model="query.eventType" :placeholder="text.eventTypePlaceholder" clearable style="width: 180px;">
            <el-option
              v-for="option in eventTypeOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="text.impactLevel">
          <el-select v-model="query.impactLevel" :placeholder="text.impactLevelPlaceholder" clearable style="width: 180px;">
            <el-option
              v-for="option in impactLevelOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="text.eventStatus">
          <el-select v-model="query.eventStatus" :placeholder="text.eventStatusPlaceholder" clearable style="width: 180px;">
            <el-option
              v-for="option in eventStatusOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">{{ text.search }}</el-button>
          <el-button @click="handleReset">{{ text.reset }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="16" style="margin-bottom: 16px;">
      <el-col
        v-for="item in statsCards"
        :key="item.label"
        :xs="24"
        :sm="12"
        :md="8"
        :lg="4"
        :xl="4"
      >
        <el-card shadow="hover">
          <div style="font-size: 13px; color: #909399; margin-bottom: 8px;">{{ item.label }}</div>
          <div style="font-size: 28px; font-weight: 700;" :style="{ color: item.color }">{{ item.value }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card>
      <el-table
        v-loading="loading"
        :data="pageData.records"
        border
        stripe
        style="width: 100%;"
      >
        <template #empty>
          <el-empty :description="text.empty" />
        </template>

        <el-table-column prop="eventTitle" :label="text.eventTitle" min-width="260" />
        <el-table-column prop="targetCode" :label="text.targetCode" min-width="120" />
        <el-table-column prop="targetName" :label="text.targetName" min-width="140" />

        <el-table-column :label="text.eventType" min-width="120">
          <template #default="{ row }">
            <el-tag :type="getMarketEventTypeTagType(row.eventType)">
              {{ getMarketEventTypeText(row.eventType) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column :label="text.impactTag" min-width="120">
          <template #default="{ row }">
            <el-tag :type="getMarketEventImpactTagType(row.impactLevel)">
              {{ getMarketEventImpactText(row.impactLevel) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column :label="text.eventStatus" min-width="120">
          <template #default="{ row }">
            <el-tag :type="getMarketEventStatusTagType(row.eventStatus)">
              {{ getMarketEventStatusText(row.eventStatus) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column :label="text.followUpStatus" min-width="120">
          <template #default="{ row }">
            <el-tag :type="getFollowUpStatusTagType(row.followUpStatus)">
              {{ getFollowUpStatusText(row.followUpStatus) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column :label="text.autoTriggerStatus" min-width="120">
          <template #default="{ row }">
            <el-tag :type="getAutoTriggerStatusTagType(row.autoTriggerStatus)">
              {{ getAutoTriggerStatusText(row.autoTriggerStatus) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="followUpTaskCount" :label="text.followUpTaskCount" width="110" align="center" />
        <el-table-column :label="text.occurredAt" min-width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.occurredAt) }}
          </template>
        </el-table-column>

        <el-table-column :label="text.action" fixed="right" min-width="280">
          <template #default="{ row }">
            <el-space wrap>
              <el-button link type="primary" @click="openEventDetail(row)">{{ text.openDetail }}</el-button>
              <el-button
                v-if="actionAccess.showCreateTask"
                link
                type="success"
                @click="createFollowUpTask(row)"
              >
                {{ text.createTask }}
              </el-button>
              <el-button link @click="goWorkbench(row)">{{ text.workbench }}</el-button>
              <el-button
                v-if="row.latestFollowUpTaskId"
                link
                type="warning"
                @click="goTaskDetail(row.latestFollowUpTaskId)"
              >
                {{ text.openLatestFollowUpTask }}
              </el-button>
            </el-space>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top: 16px; display: flex; justify-content: flex-end;">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next"
          :total="pageData.total"
          :current-page="pageData.pageNum"
          :page-size="pageData.pageSize"
          :page-sizes="[10, 20, 50]"
          @current-change="handlePageChange"
          @size-change="handlePageSizeChange"
        />
      </div>
    </el-card>

    <el-card style="margin-top: 16px;">
      <template #header>
        <div style="display: flex; align-items: center; justify-content: space-between;">
          <span>{{ text.ingestHistoryTitle }}</span>
          <el-button link type="primary" @click="loadIngestHistory">刷新历史</el-button>
        </div>
      </template>

      <div style="margin-bottom: 16px;">
        <div style="font-size: 14px; font-weight: 600; margin-bottom: 12px;">{{ text.ingestSourceStatsTitle }}</div>
        <el-empty v-if="!ingestSourceStats.length" :description="text.ingestSourceStatsEmpty" :image-size="60" />
        <el-row v-else :gutter="16">
          <el-col
            v-for="item in ingestSourceStats"
            :key="item.sourceCode || item.sourceType"
            :xs="24"
            :sm="12"
            :md="8"
            :lg="8"
            :xl="6"
            style="margin-bottom: 12px;"
          >
            <el-card shadow="hover">
              <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px;">
                <span style="font-size: 14px; font-weight: 600;">{{ item.sourceName || item.sourceLabel }}</span>
                <el-tag :type="getIngestSourceTagType(item.sourceCategory || item.sourceType)">
                  {{ item.sourceCode || item.sourceType }}
                </el-tag>
              </div>
              <div
                v-if="item.sourceChannel || item.sourceCategory"
                style="font-size: 12px; color: #909399; margin-bottom: 8px;"
              >
                <span v-if="item.sourceCategory">{{ item.sourceCategory }}</span>
                <span v-if="item.sourceCategory && item.sourceChannel"> / </span>
                <span v-if="item.sourceChannel">{{ item.sourceChannel }}</span>
              </div>
              <div style="font-size: 28px; font-weight: 700; color: #303133; margin-bottom: 8px;">
                {{ item.totalCount }}
              </div>
              <div style="font-size: 12px; color: #909399; margin-bottom: 6px;">{{ text.ingestTotalCount }}</div>
              <div style="font-size: 13px; color: #606266; line-height: 1.7;">
                {{ getIngestSourceSummary(item) }}
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <el-table
        v-loading="ingestHistoryLoading"
        :data="ingestHistoryRecords"
        border
        stripe
        style="width: 100%;"
      >
        <template #empty>
          <el-empty :description="text.ingestHistoryEmpty" />
        </template>

        <el-table-column :label="text.ingestSource" min-width="140">
          <template #default="{ row }">
            <div style="display: flex; flex-direction: column; gap: 4px;">
              <div>
                <el-tag :type="getIngestSourceTagType(row.sourceCategory || row.sourceType)">
                  {{ row.sourceName || row.sourceLabel || row.sourceCode || row.sourceType || '-' }}
                </el-tag>
              </div>
              <div
                v-if="row.sourceCode || row.sourceChannel"
                style="font-size: 12px; color: #909399;"
              >
                <span v-if="row.sourceCode">{{ row.sourceCode }}</span>
                <span v-if="row.sourceCode && row.sourceChannel"> / </span>
                <span v-if="row.sourceChannel">{{ row.sourceChannel }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="sourceDetail" :label="text.ingestDetail" min-width="220" show-overflow-tooltip />
        <el-table-column prop="totalCount" :label="text.ingestTotalCount" width="100" align="center" />
        <el-table-column prop="successCount" :label="text.ingestSuccessCount" width="100" align="center" />
        <el-table-column prop="failedCount" :label="text.ingestFailedCount" width="100" align="center" />
        <el-table-column prop="duplicateCount" :label="text.ingestDuplicateCount" width="100" align="center" />
        <el-table-column prop="autoTriggeredCount" :label="text.ingestAutoTriggeredCount" width="110" align="center" />
        <el-table-column :label="text.ingestResultStatus" width="110">
          <template #default="{ row }">
            <el-tag :type="getIngestResultStatusTagType(row.resultStatus)">{{ getIngestResultStatusText(row.resultStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="text.ingestOperator" min-width="140">
          <template #default="{ row }">
            <span>{{ row.operatorId || '-' }}</span>
            <span v-if="row.operatorRole" style="color: #909399;"> / {{ row.operatorRole }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="text.ingestCreatedAt" min-width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="errorMessage" :label="text.ingestErrorMessage" min-width="220" show-overflow-tooltip />
        <el-table-column prop="summary" :label="text.ingestSummary" min-width="220" show-overflow-tooltip />
      </el-table>
    </el-card>

    <el-dialog
      v-model="createResultDialogVisible"
      :title="text.createResultDialogTitle"
      width="640px"
      destroy-on-close
    >
      <template v-if="createdEventResult || createdEventFeedback">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="录入结果">
            <el-tag :type="createdEventResult?.duplicate ? 'warning' : 'success'">
              {{ createdEventResult?.duplicate ? '命中已有事件' : '新录入' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="录入摘要">
            {{ createdEventResult?.message || '-' }}
          </el-descriptions-item>
          <el-descriptions-item :label="text.eventId">
            {{ createdEventFeedback?.eventId || createdEventResult?.eventId || '-' }}
          </el-descriptions-item>
          <el-descriptions-item :label="text.eventTitle">
            {{ createdEventFeedback?.eventTitle || '-' }}
          </el-descriptions-item>
          <el-descriptions-item :label="text.autoTriggerStatus">
            <el-tag
              v-if="createdEventFeedback?.autoTriggerStatus || createdEventResult?.autoTriggerStatus"
              :type="getAutoTriggerStatusTagType(createdEventFeedback?.autoTriggerStatus || createdEventResult?.autoTriggerStatus)"
            >
              {{ getAutoTriggerStatusText(createdEventFeedback?.autoTriggerStatus || createdEventResult?.autoTriggerStatus) }}
            </el-tag>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item :label="text.autoTriggerRuleCode">
            {{ createdEventFeedback?.autoTriggerRuleCode || '-' }}
          </el-descriptions-item>
          <el-descriptions-item :label="text.autoTriggerAttemptedAt">
            {{ formatDateTime(createdEventFeedback?.autoTriggerAttemptedAt) }}
          </el-descriptions-item>
          <el-descriptions-item :label="text.autoTriggerTask">
            <template v-if="createdEventFeedback?.autoTriggerTaskId || createdEventResult?.autoTriggerTaskId">
              <span>{{ createdEventFeedback?.autoTriggerTaskId || createdEventResult?.autoTriggerTaskId }}</span>
              <el-button
                link
                type="primary"
                style="margin-left: 8px;"
                @click="goTaskDetail(createdEventFeedback?.autoTriggerTaskId || createdEventResult?.autoTriggerTaskId)"
              >
                {{ text.openAutoTriggerTask }}
              </el-button>
            </template>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item :label="text.autoTriggerMessage" :span="2">
            {{ createdEventFeedback?.autoTriggerMessage || createdEventResult?.autoTriggerMessage || '-' }}
          </el-descriptions-item>
        </el-descriptions>
      </template>

      <template #footer>
        <el-button @click="createResultDialogVisible = false">关闭</el-button>
        <el-button
          v-if="createdEventFeedback"
          type="primary"
          @click="openCreatedEventDetail"
        >
          {{ text.viewEventDetail }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="batchImportDialogVisible"
      :title="text.batchImportDialogTitle"
      width="760px"
      destroy-on-close
    >
      <div style="display: grid; row-gap: 16px;">
        <el-alert :title="text.batchImportHint" type="info" :closable="false" />

        <input
          ref="batchImportFileInput"
          type="file"
          accept=".json,.csv,.xlsx,.xls,application/json,text/csv,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,application/vnd.ms-excel"
          style="display: none;"
          @change="handleBatchImportFileChange"
        />

        <el-form label-position="top">
          <el-form-item :label="text.batchImportContent">
            <el-input
              v-model="batchImportContent"
              type="textarea"
              :rows="16"
              resize="vertical"
              :placeholder="text.batchImportPlaceholder"
            />
          </el-form-item>
        </el-form>

        <div v-if="batchImportSourceSummary || batchImportSheetNames.length" style="display: flex; gap: 12px; align-items: center; flex-wrap: wrap;">
          <el-tag v-if="batchImportSourceSummary" type="info">
            {{ batchImportSourceSummary }}
          </el-tag>
          <el-select
            v-if="batchImportSheetNames.length > 1"
            v-model="batchImportSelectedSheet"
            placeholder="选择工作表"
            style="width: 220px;"
          >
            <el-option
              v-for="sheetName in batchImportSheetNames"
              :key="sheetName"
              :label="sheetName"
              :value="sheetName"
            />
          </el-select>
        </div>

        <template v-if="batchImportPreviewResult">
          <el-divider content-position="left">{{ text.batchPreviewTitle }}</el-divider>

          <el-row :gutter="16">
            <el-col :span="4">
              <el-card shadow="never">
                <div style="font-size: 13px; color: #909399;">{{ text.batchImportTotalCount }}</div>
                <div style="font-size: 24px; font-weight: 700;">{{ batchImportPreviewResult.totalCount }}</div>
              </el-card>
            </el-col>
            <el-col :span="4">
              <el-card shadow="never">
                <div style="font-size: 13px; color: #909399;">{{ text.batchPreviewValidCount }}</div>
                <div style="font-size: 24px; font-weight: 700; color: #67c23a;">{{ batchImportPreviewResult.validCount }}</div>
              </el-card>
            </el-col>
            <el-col :span="4">
              <el-card shadow="never">
                <div style="font-size: 13px; color: #909399;">{{ text.batchPreviewInvalidCount }}</div>
                <div style="font-size: 24px; font-weight: 700; color: #f56c6c;">{{ batchImportPreviewResult.invalidCount }}</div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card shadow="never">
                <div style="font-size: 13px; color: #909399;">{{ text.batchPreviewDuplicateCount }}</div>
                <div style="font-size: 24px; font-weight: 700; color: #e6a23c;">{{ batchImportPreviewResult.duplicateCount }}</div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card shadow="never">
                <div style="font-size: 13px; color: #909399;">{{ text.batchPreviewAutoTriggerCount }}</div>
                <div style="font-size: 24px; font-weight: 700; color: #409eff;">{{ batchImportPreviewResult.autoTriggerCandidateCount }}</div>
              </el-card>
            </el-col>
          </el-row>

          <el-alert
            v-if="previewIssueSummary.length"
            type="warning"
            :closable="false"
            style="margin-top: 12px;"
          >
            <template #title>
              <div style="display: flex; flex-wrap: wrap; gap: 8px;">
                <el-tag
                  v-for="item in previewIssueSummary"
                  :key="item.label"
                  :type="item.type"
                >
                  {{ item.label }} {{ item.count }} 条
                </el-tag>
              </div>
            </template>
          </el-alert>

          <div style="display: flex; gap: 12px; flex-wrap: wrap;">
            <el-button @click="exportBatchPreviewResultJson">
              {{ text.exportBatchPreviewJson }}
            </el-button>
            <el-button @click="exportBatchPreviewResultCsv">
              {{ text.exportBatchPreviewCsv }}
            </el-button>
            <el-button @click="exportBatchPreviewResultExcel">
              导出预校验 Excel
            </el-button>
            <el-button @click="exportImportableEventsJson">
              {{ text.exportImportableEventsJson }}
            </el-button>
            <el-button @click="togglePreviewOnlyInvalid">
              {{ previewOnlyInvalid ? '查看全部' : '只看不可导入项' }}
            </el-button>
          </div>

          <el-table :data="visibleBatchPreviewItems" border max-height="320">
            <el-table-column prop="itemNo" :label="text.batchImportItemNo" width="80" />
            <el-table-column prop="targetCode" :label="text.targetCode" width="120" />
            <el-table-column prop="targetName" :label="text.targetName" width="140" />
            <el-table-column prop="eventTitle" :label="text.eventTitle" min-width="220" show-overflow-tooltip />
            <el-table-column :label="text.batchPreviewResult" width="120">
              <template #default="{ row }">
                <el-tag :type="row.valid ? 'success' : 'danger'">
                  {{ row.valid ? '通过' : '失败' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="可导入" width="120">
              <template #default="{ row }">
                <el-tag :type="getPreviewImportableTagType(row.importable)">
                  {{ getPreviewImportableText(row.importable) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="问题字段" width="120">
              <template #default="{ row }">
                <el-tag v-if="row.invalidField" type="danger">
                  {{ getPreviewInvalidFieldText(row.invalidField) }}
                </el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column :label="text.batchPreviewDuplicateSource" width="140">
              <template #default="{ row }">
                <el-tag v-if="row.duplicate" type="warning">
                  {{ getDuplicateSourceText(row.duplicateSource) }}
                </el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column :label="text.batchPreviewNormalized" min-width="220">
              <template #default="{ row }">
                <div>{{ row.normalizedTargetCode || '-' }}</div>
                <div style="color: #909399; font-size: 12px;">
                  {{ row.normalizedEventType || '-' }} / {{ row.normalizedImpactLevel || '-' }} / {{ row.normalizedSourceChannel || '-' }}
                </div>
              </template>
            </el-table-column>
            <el-table-column :label="text.autoTriggerStatus" width="120">
              <template #default="{ row }">
                <el-tag :type="getAutoTriggerStatusTagType(row.autoTriggerStatus)">
                  {{ getAutoTriggerStatusText(row.autoTriggerStatus) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="text.autoTriggerRuleCode" width="180" show-overflow-tooltip>
              <template #default="{ row }">
                {{ row.autoTriggerRuleCode || '-' }}
              </template>
            </el-table-column>
            <el-table-column :label="text.batchPreviewEstimatedTaskType" width="160">
              <template #default="{ row }">
                {{ row.estimatedTaskType || '-' }}
              </template>
            </el-table-column>
            <el-table-column :label="text.batchImportMessage" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">
                {{ row.message || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="修正" width="100" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openPreviewItemEditor(row)">
                  修正
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </template>
      </div>

      <template #footer>
        <div style="display: flex; justify-content: space-between; gap: 12px; width: 100%;">
          <div style="display: flex; gap: 12px; flex-wrap: wrap;">
            <el-button @click="fillBatchImportExample">{{ text.batchImportExample }}</el-button>
            <el-button @click="downloadBatchImportTemplate">{{ text.batchImportTemplate }}</el-button>
            <el-button @click="downloadBatchImportCsvTemplate">{{ text.batchImportCsvTemplate }}</el-button>
            <el-button @click="downloadBatchImportExcelTemplate">下载 Excel 模板</el-button>
            <el-button @click="openBatchImportFilePicker">{{ text.batchImportUpload }}</el-button>
          </div>
          <div style="display: flex; gap: 12px;">
            <el-button @click="batchImportDialogVisible = false">关闭</el-button>
            <el-button
              v-if="batchImportPreviewResult"
              @click="applyPreviewImportableItems"
            >
              {{ text.batchPreviewApplyImportable }}
            </el-button>
            <el-button :loading="batchPreviewing" @click="previewBatchImport">
              {{ text.previewBatchImport }}
            </el-button>
            <el-button
              v-if="batchImportPreviewResult"
              type="warning"
              :loading="batchImporting"
              @click="submitPreviewImportableBatchImport"
            >
              {{ text.batchPreviewImportImportable }}
            </el-button>
            <el-button type="primary" :loading="batchImporting" @click="submitBatchImport">
              {{ text.submitBatchImport }}
            </el-button>
          </div>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="batchImportResultDialogVisible"
      :title="text.batchImportResultDialogTitle"
      width="860px"
      destroy-on-close
    >
      <template v-if="batchImportResult">
        <el-row :gutter="16" style="margin-bottom: 16px;">
          <el-col :span="4">
            <el-card shadow="never">
              <div style="font-size: 13px; color: #909399;">{{ text.batchImportTotalCount }}</div>
              <div style="font-size: 24px; font-weight: 700;">{{ batchImportResult.totalCount }}</div>
            </el-card>
          </el-col>
          <el-col :span="4">
            <el-card shadow="never">
              <div style="font-size: 13px; color: #909399;">{{ text.batchImportSuccessCount }}</div>
              <div style="font-size: 24px; font-weight: 700; color: #67c23a;">{{ batchImportResult.successCount }}</div>
            </el-card>
          </el-col>
          <el-col :span="4">
            <el-card shadow="never">
              <div style="font-size: 13px; color: #909399;">{{ text.batchImportFailedCount }}</div>
              <div style="font-size: 24px; font-weight: 700; color: #f56c6c;">{{ batchImportResult.failedCount }}</div>
            </el-card>
          </el-col>
          <el-col :span="4">
            <el-card shadow="never">
              <div style="font-size: 13px; color: #909399;">去重数</div>
              <div style="font-size: 24px; font-weight: 700; color: #e6a23c;">{{ batchImportResult.duplicateCount }}</div>
            </el-card>
          </el-col>
          <el-col :span="4">
            <el-card shadow="never">
              <div style="font-size: 13px; color: #909399;">{{ text.batchImportAutoTriggeredCount }}</div>
              <div style="font-size: 24px; font-weight: 700; color: #409eff;">{{ batchImportResult.autoTriggeredCount }}</div>
            </el-card>
          </el-col>
        </el-row>

        <el-table :data="batchImportResult.items" border max-height="420">
          <el-table-column prop="itemNo" :label="text.batchImportItemNo" width="80" />
          <el-table-column prop="targetCode" :label="text.targetCode" width="120" />
          <el-table-column prop="targetName" :label="text.targetName" width="140" />
          <el-table-column prop="eventTitle" :label="text.eventTitle" min-width="220" show-overflow-tooltip />
          <el-table-column label="录入结果" width="120">
            <template #default="{ row }">
              <el-tag :type="row.success ? (row.duplicate ? 'warning' : 'success') : 'danger'">
                {{ row.success ? (row.duplicate ? '命中已有事件' : '新录入') : '失败' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="text.autoTriggerStatus" width="120">
            <template #default="{ row }">
              <el-tag :type="getAutoTriggerStatusTagType(row.autoTriggerStatus)">
                {{ getAutoTriggerStatusText(row.autoTriggerStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="text.autoTriggerTask" min-width="180">
            <template #default="{ row }">
              <template v-if="row.autoTriggerTaskId">
                <span>{{ row.autoTriggerTaskId }}</span>
                <el-button
                  link
                  type="primary"
                  style="margin-left: 8px;"
                  @click="goTaskDetail(row.autoTriggerTaskId)"
                >
                  {{ text.openAutoTriggerTask }}
                </el-button>
              </template>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column :label="text.batchImportMessage" min-width="220" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.message || '-' }}
            </template>
          </el-table-column>
        </el-table>
      </template>

      <template #footer>
        <div style="display: flex; justify-content: space-between; gap: 12px; width: 100%;">
          <div style="display: flex; gap: 12px; flex-wrap: wrap;">
            <el-button @click="exportBatchImportResultJson">{{ text.exportBatchImportJson }}</el-button>
            <el-button @click="exportBatchImportResultCsv">{{ text.exportBatchImportCsv }}</el-button>
            <el-button @click="exportBatchImportResultExcel">导出 Excel</el-button>
          </div>
          <el-button @click="batchImportResultDialogVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="mockIngestDialogVisible"
      title="模拟外部事件接入"
      width="640px"
      destroy-on-close
    >
      <el-form label-width="120px">
        <el-form-item label="模拟来源" required>
          <el-select v-model="mockIngestForm.sourcePreset" style="width: 100%;">
            <el-option
              v-for="option in mockSourceOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="text.targetCode" required>
          <el-input v-model="mockIngestForm.targetCode" :placeholder="text.targetCodePlaceholder" />
        </el-form-item>
        <el-form-item :label="text.targetName" required>
          <el-input v-model="mockIngestForm.targetName" :placeholder="text.targetNamePlaceholder" />
        </el-form-item>
        <el-form-item label="模拟条数">
          <el-input-number v-model="mockIngestForm.itemCount" :min="1" :max="10" style="width: 180px;" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="mockIngestDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="mockIngesting" @click="submitMockIngest">开始接入</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="sourceSyncDialogVisible"
      :title="text.sourceSyncDialogTitle"
      width="640px"
      destroy-on-close
    >
      <el-form label-width="120px">
        <el-form-item :label="text.sourceSyncSource" required>
          <el-select v-model="sourceSyncForm.sourceCode" style="width: 100%;">
            <el-option
              v-for="option in syncSourceOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="text.targetCode" required>
          <el-input v-model="sourceSyncForm.targetCode" :placeholder="text.targetCodePlaceholder" />
        </el-form-item>
        <el-form-item :label="text.targetName" required>
          <el-input v-model="sourceSyncForm.targetName" :placeholder="text.targetNamePlaceholder" />
        </el-form-item>
        <el-form-item :label="text.sourceSyncItemCount">
          <el-input-number v-model="sourceSyncForm.itemCount" :min="1" :max="10" style="width: 180px;" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="sourceSyncDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="sourceSyncing" @click="submitSourceSync">{{ text.sourceSyncSubmit }}</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="previewEditDialogVisible"
      title="修正导入项"
      width="720px"
      destroy-on-close
    >
      <el-form label-width="120px">
        <el-form-item :label="text.targetCode" required :error="isPreviewFieldInvalid('targetCode') ? '当前预校验卡在这个字段' : ''">
          <el-input v-model="previewEditForm.targetCode" :placeholder="text.targetCodePlaceholder" />
        </el-form-item>
        <el-form-item :label="text.targetName" required :error="isPreviewFieldInvalid('targetName') ? '当前预校验卡在这个字段' : ''">
          <el-input v-model="previewEditForm.targetName" :placeholder="text.targetNamePlaceholder" />
        </el-form-item>
        <el-form-item :label="text.eventType" required :error="isPreviewFieldInvalid('eventType') ? '当前预校验卡在这个字段' : ''">
          <el-select v-model="previewEditForm.eventType" style="width: 100%;">
            <el-option
              v-for="option in eventTypeOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="text.eventTitle" required :error="isPreviewFieldInvalid('eventTitle') ? '当前预校验卡在这个字段' : ''">
          <el-input v-model="previewEditForm.eventTitle" :placeholder="text.eventTitlePlaceholder" />
        </el-form-item>
        <el-form-item :label="text.eventSummary" required :error="isPreviewFieldInvalid('eventSummary') ? '当前预校验卡在这个字段' : ''">
          <el-input
            v-model="previewEditForm.eventSummary"
            type="textarea"
            :rows="4"
            :placeholder="text.eventSummaryPlaceholder"
          />
        </el-form-item>
        <el-form-item :label="text.sourceChannel">
          <el-input v-model="previewEditForm.sourceChannel" :placeholder="text.sourceChannelPlaceholder" />
        </el-form-item>
        <el-form-item :label="text.sourceUrl">
          <el-input v-model="previewEditForm.sourceUrl" :placeholder="text.sourceUrlPlaceholder" />
        </el-form-item>
        <el-form-item :label="text.impactLevel" required :error="isPreviewFieldInvalid('impactLevel') ? '当前预校验卡在这个字段' : ''">
          <el-select v-model="previewEditForm.impactLevel" style="width: 100%;">
            <el-option
              v-for="option in impactLevelOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="text.eventStatus">
          <el-select v-model="previewEditForm.eventStatus" style="width: 100%;">
            <el-option
              v-for="option in eventStatusOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="text.occurredAt" required :error="isPreviewFieldInvalid('occurredAt') ? '当前预校验卡在这个字段' : ''">
          <el-input
            v-model="previewEditForm.occurredAt"
            type="datetime-local"
            :placeholder="text.occurredAtPlaceholder"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="previewEditDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="savePreviewItemEdit">保存并更新</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="detailDialogVisible"
      :title="text.detailTitle"
      width="760px"
      destroy-on-close
    >
      <template v-if="selectedEvent">
        <el-descriptions :column="2" border>
          <el-descriptions-item :label="text.eventId">{{ selectedEvent.eventId }}</el-descriptions-item>
          <el-descriptions-item :label="text.eventTitle">{{ selectedEvent.eventTitle }}</el-descriptions-item>
          <el-descriptions-item :label="text.targetCode">{{ selectedEvent.targetCode }}</el-descriptions-item>
          <el-descriptions-item :label="text.targetName">{{ selectedEvent.targetName }}</el-descriptions-item>
          <el-descriptions-item :label="text.eventType">
            {{ getMarketEventTypeText(selectedEvent.eventType) }}
          </el-descriptions-item>
          <el-descriptions-item :label="text.impactTag">
            {{ getMarketEventImpactText(selectedEvent.impactLevel) }}
          </el-descriptions-item>
          <el-descriptions-item :label="text.eventStatus">
            {{ getMarketEventStatusText(selectedEvent.eventStatus) }}
          </el-descriptions-item>
          <el-descriptions-item :label="text.autoTriggerStatus">
            <el-tag
              v-if="selectedEvent.autoTriggerStatus"
              :type="getAutoTriggerStatusTagType(selectedEvent.autoTriggerStatus)"
            >
              {{ getAutoTriggerStatusText(selectedEvent.autoTriggerStatus) }}
            </el-tag>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item :label="text.autoTriggerRuleCode">
            {{ selectedEvent.autoTriggerRuleCode || '-' }}
          </el-descriptions-item>
          <el-descriptions-item :label="text.autoTriggerAttemptedAt">
            {{ formatDateTime(selectedEvent.autoTriggerAttemptedAt) }}
          </el-descriptions-item>
          <el-descriptions-item :label="text.followUpStatus">
            {{ getFollowUpStatusText(selectedEvent.followUpStatus) }}
          </el-descriptions-item>
          <el-descriptions-item :label="text.followUpTaskCount">
            {{ selectedEvent.followUpTaskCount ?? 0 }}
          </el-descriptions-item>
          <el-descriptions-item :label="text.latestFollowUpCreatedAt">
            {{ formatDateTime(selectedEvent.latestFollowUpCreatedAt) }}
          </el-descriptions-item>
          <el-descriptions-item :label="text.relatedReportCount">
            {{ selectedEvent.relatedReportCount ?? 0 }}
          </el-descriptions-item>
          <el-descriptions-item :label="text.relationCount">
            {{ selectedEvent.relationCount ?? 0 }}
          </el-descriptions-item>
          <el-descriptions-item :label="text.latestReportCreatedAt">
            {{ formatDateTime(selectedEvent.latestReportCreatedAt) }}
          </el-descriptions-item>
          <el-descriptions-item :label="text.sourceChannel">
            {{ selectedEvent.sourceChannel || '-' }}
          </el-descriptions-item>
          <el-descriptions-item :label="text.sourceUrl">
            <a
              v-if="selectedEvent.sourceUrl"
              :href="selectedEvent.sourceUrl"
              target="_blank"
              rel="noreferrer"
            >
              {{ selectedEvent.sourceUrl }}
            </a>
            <span v-else>{{ text.noSourceUrl }}</span>
          </el-descriptions-item>
          <el-descriptions-item :label="text.occurredAt">
            {{ formatDateTime(selectedEvent.occurredAt) }}
          </el-descriptions-item>
          <el-descriptions-item :label="text.createdAt">
            {{ formatDateTime(selectedEvent.createdAt) }}
          </el-descriptions-item>
          <el-descriptions-item :label="text.createdBy">
            {{ selectedEvent.createdBy || '-' }}
          </el-descriptions-item>
          <el-descriptions-item :label="text.latestFollowUpTask">
            <template v-if="selectedEvent.latestFollowUpTaskId">
              <el-tag :type="getTaskStatusTagType(selectedEvent.latestFollowUpTaskStatus)">
                {{ getTaskStatusText(selectedEvent.latestFollowUpTaskStatus) }}
              </el-tag>
              <span style="margin-left: 8px;">{{ selectedEvent.latestFollowUpTaskTitle || selectedEvent.latestFollowUpTaskId }}</span>
            </template>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item :label="text.autoTriggerTask">
            <template v-if="selectedEvent.autoTriggerTaskId">
              <span>{{ selectedEvent.autoTriggerTaskId }}</span>
              <el-button
                link
                type="primary"
                style="margin-left: 8px;"
                @click="goTaskDetail(selectedEvent.autoTriggerTaskId)"
              >
                {{ text.openAutoTriggerTask }}
              </el-button>
            </template>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item :label="text.latestReport">
            <template v-if="selectedEvent.latestReportId">
              <span>{{ selectedEvent.latestReportId }}</span>
              <el-button
                link
                type="primary"
                style="margin-left: 8px;"
                @click="goTaskReport(selectedEvent.latestReportTaskId)"
              >
                {{ text.openLatestReport }}
              </el-button>
            </template>
            <span v-else>{{ text.noLatestReport }}</span>
          </el-descriptions-item>
          <el-descriptions-item :label="text.latestReportType">
            {{ selectedEvent.latestReportType || '-' }}
          </el-descriptions-item>
          <el-descriptions-item :label="text.latestReportReviewStatus">
            <el-tag
              v-if="selectedEvent.latestReportReviewStatus"
              :type="getReviewStatusTagType(selectedEvent.latestReportReviewStatus)"
            >
              {{ getReviewStatusText(selectedEvent.latestReportReviewStatus) }}
            </el-tag>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item :label="text.latestReportConfidenceScore">
            {{ selectedEvent.latestReportConfidenceScore ?? '-' }}
          </el-descriptions-item>
          <el-descriptions-item :label="text.latestNeedHumanReview">
            {{ selectedEvent.latestReportId ? getHumanReviewText(selectedEvent.latestNeedHumanReview) : '-' }}
          </el-descriptions-item>
          <el-descriptions-item :label="text.derivedRiskLevel">
            <el-tag
              v-if="selectedEvent.derivedRiskLevel"
              :type="getRiskLevelTagType(selectedEvent.derivedRiskLevel)"
            >
              {{ getRiskLevelText(selectedEvent.derivedRiskLevel) }}
            </el-tag>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item :label="text.derivedRiskCount">
            {{ selectedEvent.derivedRiskCount ?? 0 }}
          </el-descriptions-item>
          <el-descriptions-item :label="text.derivedWarningCount">
            {{ selectedEvent.derivedWarningCount ?? 0 }}
          </el-descriptions-item>
          <el-descriptions-item :label="text.derivedRiskPointCount">
            {{ selectedEvent.derivedRiskPointCount ?? 0 }}
          </el-descriptions-item>
          <el-descriptions-item :label="text.derivedSignalDirection">
            <el-tag
              v-if="selectedEvent.derivedSignalDirection"
              :type="getSignalDirectionTagType(selectedEvent.derivedSignalDirection)"
            >
              {{ getSignalDirectionText(selectedEvent.derivedSignalDirection) }}
            </el-tag>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item :label="text.derivedSignalStrength">
            <el-tag
              v-if="selectedEvent.derivedSignalStrength"
              :type="getSignalStrengthTagType(selectedEvent.derivedSignalStrength)"
            >
              {{ getSignalStrengthText(selectedEvent.derivedSignalStrength) }}
            </el-tag>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item :label="text.derivedSignalScore">
            {{ selectedEvent.derivedSignalScore ?? '-' }}
          </el-descriptions-item>
          <el-descriptions-item :label="text.derivedIntelligenceType">
            <el-tag
              v-if="selectedEvent.derivedIntelligenceType"
              :type="getMarketIntelligenceTypeTagType(selectedEvent.derivedIntelligenceType)"
            >
              {{ getMarketIntelligenceTypeText(selectedEvent.derivedIntelligenceType) }}
            </el-tag>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item :label="text.eventSummary" :span="2">
            {{ selectedEvent.eventSummary || text.noSummary }}
          </el-descriptions-item>
          <el-descriptions-item :label="text.relations" :span="2">
            <template v-if="selectedEvent.relations?.length">
              <el-space wrap>
                <el-tag
                  v-for="relation in selectedEvent.relations"
                  :key="`${relation.relationType}-${relation.relationCode}`"
                  type="info"
                >
                  {{ relation.relationName || relation.relationCode }} / {{ relation.relationType }}
                </el-tag>
              </el-space>
            </template>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item :label="text.latestReportSummary" :span="2">
            {{ selectedEvent.latestReportSummary || text.noLatestReport }}
          </el-descriptions-item>
          <el-descriptions-item :label="text.autoTriggerMessage" :span="2">
            {{ selectedEvent.autoTriggerMessage || '-' }}
          </el-descriptions-item>
        </el-descriptions>
      </template>

      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="createDialogVisible"
      :title="text.createDialogTitle"
      width="720px"
      destroy-on-close
    >
      <el-form label-width="120px">
        <el-form-item :label="text.targetCode" required>
          <el-input v-model="createForm.targetCode" :placeholder="text.targetCodePlaceholder" />
        </el-form-item>
        <el-form-item :label="text.targetName" required>
          <el-input v-model="createForm.targetName" :placeholder="text.targetNamePlaceholder" />
        </el-form-item>
        <el-form-item :label="text.eventType" required>
          <el-select v-model="createForm.eventType" style="width: 100%;">
            <el-option
              v-for="option in eventTypeOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="text.eventTitle" required>
          <el-input v-model="createForm.eventTitle" :placeholder="text.eventTitlePlaceholder" />
        </el-form-item>
        <el-form-item :label="text.eventSummary" required>
          <el-input
            v-model="createForm.eventSummary"
            type="textarea"
            :rows="4"
            :placeholder="text.eventSummaryPlaceholder"
          />
        </el-form-item>
        <el-form-item :label="text.sourceChannel">
          <el-input v-model="createForm.sourceChannel" :placeholder="text.sourceChannelPlaceholder" />
        </el-form-item>
        <el-form-item :label="text.sourceUrl">
          <el-input v-model="createForm.sourceUrl" :placeholder="text.sourceUrlPlaceholder" />
        </el-form-item>
        <el-form-item :label="text.impactLevel" required>
          <el-select v-model="createForm.impactLevel" style="width: 100%;">
            <el-option
              v-for="option in impactLevelOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="text.eventStatus">
          <el-select v-model="createForm.eventStatus" style="width: 100%;">
            <el-option
              v-for="option in eventStatusOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="text.occurredAt" required>
          <el-input
            v-model="createForm.occurredAt"
            type="datetime-local"
            :placeholder="text.occurredAtPlaceholder"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="submitEvent">{{ text.submitEvent }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>
