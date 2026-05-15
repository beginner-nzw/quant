<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  createStrategySignal,
  fetchStrategySignalFactors,
  fetchStrategySignalStats,
  fetchStrategySignals,
  updateStrategySignalStatus
} from '../../api/task'
import FilterDock from '../../components/common/FilterDock.vue'
import ReportRevisionStatusTags from '../../components/report/ReportRevisionStatusTags.vue'
import StrategySignalStatsCards from '../../components/report/StrategySignalStatsCards.vue'
import type {
  StrategySignalCreateForm,
  StrategySignalFactorItem,
  StrategySignalListItem,
  StrategySignalStats
} from '../../types/task'
import {
  ANALYSIS_SCOPE,
  REPORT_REVIEW_STATUS,
  SIGNAL_DIRECTION,
  SIGNAL_STRENGTH,
  TASK_TYPE,
  type ReportReviewStatus,
  type SignalDirection,
  type SignalStrength
} from '../../types/taskEnums'
import { formatDateTime } from '../../utils/format'
import { resolveCenterActionAccess } from '../../utils/taskActionAccess'
import {
  getBacktestStatusTagType,
  getBacktestStatusText,
  getHumanReviewTagType,
  getHumanReviewText,
  getPriorityTagType,
  getPriorityText,
  getReviewStatusTagType,
  getReviewStatusText,
  getSignalDirectionTagType,
  getSignalDirectionText,
  getSignalStrengthTagType,
  getSignalStrengthText,
  getTaskStatusTagType,
  getTaskStatusText
} from '../../utils/task'
import { buildResearchWorkbenchQuery } from '../../utils/researchWorkbench'
import { buildFollowUpTaskTitle, buildTaskCreateQuery } from '../../utils/taskCreate'
import { canReviewReports } from '../../utils/roleAccess'

const route = useRoute()
const router = useRouter()

const text = {
  title: '策略洞察与信号中心',
  targetCode: '标的代码',
  targetCodePlaceholder: '如 01929',
  targetName: '标的名称',
  targetNamePlaceholder: '如 周大福',
  signalDirection: '信号方向',
  signalDirectionPlaceholder: '全部方向',
  signalStrength: '信号强度',
  signalStrengthPlaceholder: '全部强度',
  reviewStatus: '审核状态',
  reviewStatusPlaceholder: '全部状态',
  onlyHighConfidence: '仅看高置信度',
  search: '查询',
  reset: '重置',
  refresh: '刷新',
  empty: '暂无策略信号数据',
  rank: '排名',
  taskId: '任务 ID',
  taskTitle: '任务标题',
  priority: '优先级',
  signalScore: '信号分数',
  confidenceScore: '置信度',
  strategySummary: '策略观点摘要',
  signalSources: '信号来源',
  signalSourceCategory: '来源分类',
  revisionStatus: '修订情况',
  followUpStatus: '跟踪状态',
  followUpTaskCount: '跟踪任务数',
  latestFollowUpTask: '最近跟踪任务',
  latestFollowUpCreatedAt: '最近跟踪时间',
  signalDetail: '信号详情',
  openSignalDetail: '查看信号',
  openLatestFollowUpTask: '查看最近跟踪任务',
  needHumanReview: '人工复核',
  reviewedBy: '审核人',
  reviewedAt: '审核时间',
  reviewComment: '审核意见',
  noReviewComment: '无审核意见',
  backtestStatus: '历史回测',
  backtestSummary: '回测摘要',
  createdAt: '报告时间',
  action: '操作',
  detail: '任务详情',
  createTask: '发起研究',
  report: '查看报告',
  workbench: '投研工作台',
  loadStatsFailed: '策略信号统计加载失败',
  loadStatsError: '策略信号统计加载异常',
  loadListFailed: '策略信号列表加载失败',
  loadListError: '策略信号列表加载异常',
  createSignal: '录入信号',
  createSignalTitle: '人工录入策略信号',
  editSignal: '编辑信号',
  factorDetail: '因子明细',
  factorCode: '因子编码',
  factorName: '因子名称',
  factorValue: '因子值',
  factorWeight: '权重',
  factorConclusion: '因子结论',
  addFactor: '添加因子',
  removeFactor: '删除',
  save: '保存',
  cancel: '取消',
  archive: '归档',
  disable: '禁用',
  activate: '恢复',
  statusUpdated: '信号状态已更新',
  statusUpdateFailed: '信号状态更新失败',
  statusUpdateError: '信号状态更新异常',
  factorLoadFailed: '因子明细加载失败',
  factorLoadError: '因子明细加载异常',
  saveSignalSuccess: '策略信号已保存',
  saveSignalFailed: '策略信号保存失败',
  saveSignalError: '策略信号保存异常',
  entityCodeRequired: '请填写标的代码',
  entityNameRequired: '请填写标的名称'
} as const

const defaultStats: StrategySignalStats = {
  totalCount: 0,
  positiveCount: 0,
  neutralCount: 0,
  negativeCount: 0,
  highConfidenceCount: 0,
  pendingReviewCount: 0
}

const loading = ref(false)
const stats = ref<StrategySignalStats>({ ...defaultStats })
const detailDialogVisible = ref(false)
const selectedSignal = ref<StrategySignalListItem | null>(null)
const signalFactors = ref<StrategySignalFactorItem[]>([])
const factorLoading = ref(false)
const createDialogVisible = ref(false)
const createSaving = ref(false)
const statusUpdatingSignalId = ref('')
const canEditSignals = computed(() => canReviewReports())

const createForm = reactive<StrategySignalCreateForm>({
  signalType: 'MANUAL',
  entityCode: '',
  entityName: '',
  signalDate: '',
  signalScore: 70,
  signalLevel: SIGNAL_STRENGTH.MEDIUM,
  signalDirection: SIGNAL_DIRECTION.NEUTRAL,
  reasonSummary: '',
  confidenceScore: 0.7,
  status: 'ACTIVE',
  factors: []
})

const pageData = reactive({
  total: 0,
  pageNum: 1,
  pageSize: 10,
  records: [] as StrategySignalListItem[]
})

const query = reactive({
  targetCode: '',
  targetName: '',
  signalDirection: '' as SignalDirection | '',
  signalStrength: '' as SignalStrength | '',
  reportReviewStatus: '' as ReportReviewStatus | '',
  onlyHighConfidence: false
})

const signalDirectionOptions = Object.values(SIGNAL_DIRECTION).map((value) => ({
  label: getSignalDirectionText(value),
  value
}))

const signalStrengthOptions = Object.values(SIGNAL_STRENGTH).map((value) => ({
  label: getSignalStrengthText(value),
  value
}))

const reviewStatusOptions = Object.values(REPORT_REVIEW_STATUS).map((value) => ({
  label: getReviewStatusText(value),
  value
}))

const signalDirectionValueSet = new Set<string>(Object.values(SIGNAL_DIRECTION))
const signalStrengthValueSet = new Set<string>(Object.values(SIGNAL_STRENGTH))
const reviewStatusValueSet = new Set<string>(Object.values(REPORT_REVIEW_STATUS))

const signalSourceTextMap: Record<string, string> = {
  REPORT_HIGHLIGHT: '报告亮点',
  SUMMARY_INFERENCE: '摘要推断',
  HIGH_CONFIDENCE: '高置信度',
  RISK_ADJUSTED: '风险校正',
  HUMAN_REVIEW: '人工复核',
  REVIEW_REJECTED: '审核驳回'
}

const signalSourceTagTypeMap: Record<string, 'success' | 'warning' | 'info' | 'danger'> = {
  REPORT_HIGHLIGHT: 'success',
  SUMMARY_INFERENCE: 'info',
  HIGH_CONFIDENCE: 'success',
  RISK_ADJUSTED: 'warning',
  HUMAN_REVIEW: 'info',
  REVIEW_REJECTED: 'danger'
}

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
const actionAccess = computed(() => resolveCenterActionAccess())

function formatConfidence(value?: number) {
  if (value == null) {
    return '-'
  }
  return `${Math.round(value * 100)}%`
}

function getRowIndex(index: number) {
  return (pageData.pageNum - 1) * pageData.pageSize + index + 1
}

function syncQueryFromRoute() {
  query.targetCode = getRouteQueryValue(route.query.targetCode)
  query.targetName = getRouteQueryValue(route.query.targetName)
  query.signalDirection = resolveSignalDirection(getRouteQueryValue(route.query.signalDirection))
  query.signalStrength = resolveSignalStrength(getRouteQueryValue(route.query.signalStrength))
  query.reportReviewStatus = resolveReviewStatus(getRouteQueryValue(route.query.reportReviewStatus))
  query.onlyHighConfidence = resolveBooleanQuery(route.query.onlyHighConfidence)
  pageData.pageNum = normalizePageValue(route.query.pageNum, 1)
  pageData.pageSize = normalizePageValue(route.query.pageSize, 10)
}

async function loadStats() {
  try {
    const res = await fetchStrategySignalStats()
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

async function loadSignals() {
  loading.value = true
  try {
    const res = await fetchStrategySignals({
      pageNum: pageData.pageNum,
      pageSize: pageData.pageSize,
      targetCode: normalizeQueryValue(query.targetCode),
      targetName: normalizeQueryValue(query.targetName),
      signalDirection: query.signalDirection || undefined,
      signalStrength: query.signalStrength || undefined,
      reportReviewStatus: query.reportReviewStatus || undefined,
      onlyHighConfidence: query.onlyHighConfidence || undefined
    })
    if (res.success) {
      pageData.total = res.data?.total || 0
      pageData.records = res.data?.records || []
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

async function reloadAll() {
  await Promise.all([loadStats(), loadSignals()])
}

function buildRouteQuery() {
  return {
    targetCode: normalizeQueryValue(query.targetCode),
    targetName: normalizeQueryValue(query.targetName),
    signalDirection: query.signalDirection || undefined,
    signalStrength: query.signalStrength || undefined,
    reportReviewStatus: query.reportReviewStatus || undefined,
    onlyHighConfidence: query.onlyHighConfidence ? 'true' : undefined,
    pageNum: pageData.pageNum > 1 ? String(pageData.pageNum) : undefined,
    pageSize: pageData.pageSize !== 10 ? String(pageData.pageSize) : undefined
  }
}

function isSameRouteState(nextQuery: ReturnType<typeof buildRouteQuery>) {
  return getRouteQueryValue(route.query.targetCode) === (nextQuery.targetCode || '')
    && getRouteQueryValue(route.query.targetName) === (nextQuery.targetName || '')
    && getRouteQueryValue(route.query.signalDirection) === (nextQuery.signalDirection || '')
    && getRouteQueryValue(route.query.signalStrength) === (nextQuery.signalStrength || '')
    && getRouteQueryValue(route.query.reportReviewStatus) === (nextQuery.reportReviewStatus || '')
    && resolveBooleanQuery(route.query.onlyHighConfidence) === Boolean(nextQuery.onlyHighConfidence)
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
    path: '/signals',
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
  query.signalDirection = ''
  query.signalStrength = ''
  query.reportReviewStatus = ''
  query.onlyHighConfidence = false
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

function goTaskDetail(taskId: string) {
  router.push({
    path: `/tasks/${taskId}`,
    query: {
      from: route.fullPath
    }
  })
}

function goReport(taskId: string) {
  router.push({
    path: `/tasks/${taskId}/report`,
    query: {
      from: route.fullPath
    }
  })
}

function goCreateTask(row: StrategySignalListItem) {
  router.push({
    path: '/tasks/create',
    query: buildTaskCreateQuery({
      taskType: TASK_TYPE.FOLLOW_UP_RESEARCH,
      taskTitle: buildFollowUpTaskTitle(row.targetName, row.targetCode, '策略信号跟踪研究'),
      targetCode: row.targetCode,
      targetName: row.targetName,
      priority: row.priority,
      sourceTaskId: row.taskId,
      sourceReportId: row.reportId,
      sourceDomain: 'STRATEGY_SIGNAL',
      sourceReviewStatus: row.reportReviewStatus,
      analysisScope: ANALYSIS_SCOPE.SIGNAL_FOLLOW_UP,
      from: route.fullPath
    })
  })
}

function goWorkbench(targetCode?: string, targetName?: string) {
  router.push({
    path: '/research-workbench',
    query: buildResearchWorkbenchQuery({
      targetCode,
      targetName,
      from: route.fullPath
    })
  })
}

async function openSignalDetail(row: StrategySignalListItem) {
  selectedSignal.value = row
  signalFactors.value = []
  detailDialogVisible.value = true
  if (!row.signalId) {
    return
  }
  factorLoading.value = true
  try {
    const res = await fetchStrategySignalFactors(row.signalId)
    if (res.success) {
      signalFactors.value = res.data || []
    } else {
      ElMessage.error(res.message || text.factorLoadFailed)
    }
  } catch (error: any) {
    ElMessage.error(error?.message || text.factorLoadError)
  } finally {
    factorLoading.value = false
  }
}

function openLatestFollowUpTask(taskId?: string) {
  if (!taskId) {
    return
  }
  goTaskDetail(taskId)
}

function resetCreateForm(row?: StrategySignalListItem) {
  createForm.signalId = row?.signalId
  createForm.taskId = row?.taskId || ''
  createForm.signalType = 'MANUAL'
  createForm.entityCode = row?.targetCode || ''
  createForm.entityName = row?.targetName || ''
  createForm.signalDate = ''
  createForm.signalScore = row?.signalScore ?? 70
  createForm.signalLevel = row?.signalStrength || SIGNAL_STRENGTH.MEDIUM
  createForm.signalDirection = row?.signalDirection || SIGNAL_DIRECTION.NEUTRAL
  createForm.reasonSummary = row?.strategySummary || ''
  createForm.confidenceScore = row?.confidenceScore ?? 0.7
  createForm.sourceEventId = ''
  createForm.status = 'ACTIVE'
  createForm.traceId = ''
  createForm.tenantId = ''
  createForm.factors = []
}

async function openCreateSignal(row?: StrategySignalListItem) {
  resetCreateForm(row)
  createDialogVisible.value = true
  if (!row?.signalId) {
    addFactor()
    return
  }

  try {
    const res = await fetchStrategySignalFactors(row.signalId)
    if (res.success) {
      createForm.factors = (res.data || []).map((item) => ({
        factorCode: item.factorCode,
        factorName: item.factorName,
        factorValue: item.factorValue,
        factorWeight: item.factorWeight,
        factorConclusion: item.factorConclusion
      }))
    }
  } catch {
    createForm.factors = []
  }
  if (!createForm.factors?.length) {
    addFactor()
  }
}

function addFactor() {
  const factors = createForm.factors || []
  factors.push({
    factorCode: '',
    factorName: '',
    factorValue: '',
    factorWeight: undefined,
    factorConclusion: ''
  })
  createForm.factors = factors
}

function removeFactor(index: number) {
  const factors = createForm.factors || []
  factors.splice(index, 1)
  createForm.factors = factors
}

async function submitCreateSignal() {
  if (!createForm.entityCode?.trim()) {
    ElMessage.error(text.entityCodeRequired)
    return
  }
  if (!createForm.entityName?.trim()) {
    ElMessage.error(text.entityNameRequired)
    return
  }

  createSaving.value = true
  try {
    const payload: StrategySignalCreateForm = {
      ...createForm,
      entityCode: createForm.entityCode.trim(),
      entityName: createForm.entityName.trim(),
      factors: (createForm.factors || []).filter((item) => {
        return item.factorCode || item.factorName || item.factorValue || item.factorWeight != null || item.factorConclusion
      })
    }
    const res = await createStrategySignal(payload)
    if (res.success) {
      ElMessage.success(res.message || text.saveSignalSuccess)
      createDialogVisible.value = false
      await reloadAll()
    } else {
      ElMessage.error(res.message || text.saveSignalFailed)
    }
  } catch (error: any) {
    ElMessage.error(error?.message || text.saveSignalError)
  } finally {
    createSaving.value = false
  }
}

async function changeSignalStatus(row: StrategySignalListItem, status: string) {
  if (!row.signalId) {
    return
  }
  statusUpdatingSignalId.value = row.signalId
  try {
    const res = await updateStrategySignalStatus(row.signalId, status)
    if (res.success) {
      ElMessage.success(res.message || text.statusUpdated)
      await reloadAll()
      if (selectedSignal.value?.signalId === row.signalId) {
        detailDialogVisible.value = false
      }
    } else {
      ElMessage.error(res.message || text.statusUpdateFailed)
    }
  } catch (error: any) {
    ElMessage.error(error?.message || text.statusUpdateError)
  } finally {
    statusUpdatingSignalId.value = ''
  }
}

watch([
  () => route.query.targetCode,
  () => route.query.targetName,
  () => route.query.signalDirection,
  () => route.query.signalStrength,
  () => route.query.reportReviewStatus,
  () => route.query.onlyHighConfidence,
  () => route.query.pageNum,
  () => route.query.pageSize
], async () => {
  syncQueryFromRoute()
  await reloadAll()
}, { immediate: true })

function normalizeQueryValue(value: string) {
  const normalized = value.trim()
  return normalized || undefined
}

function getRouteQueryValue(value: unknown) {
  if (Array.isArray(value)) {
    return value[0] ? String(value[0]) : ''
  }
  return value ? String(value) : ''
}

function resolveBooleanQuery(value: unknown) {
  const normalized = getRouteQueryValue(value)
  return normalized === 'true' || normalized === '1'
}

function normalizePageValue(value: unknown, fallback: number) {
  const parsed = Number(getRouteQueryValue(value))
  if (Number.isInteger(parsed) && parsed > 0) {
    return parsed
  }
  return fallback
}

function resolveSignalDirection(value: string): SignalDirection | '' {
  return signalDirectionValueSet.has(value) ? value as SignalDirection : ''
}

function resolveSignalStrength(value: string): SignalStrength | '' {
  return signalStrengthValueSet.has(value) ? value as SignalStrength : ''
}

function resolveReviewStatus(value: string): ReportReviewStatus | '' {
  return reviewStatusValueSet.has(value) ? value as ReportReviewStatus : ''
}

function getSignalSourceText(source: string) {
  return signalSourceTextMap[source] || source
}

function getSignalSourceTagType(source: string) {
  return signalSourceTagTypeMap[source] || 'info'
}

function getFollowUpStatusText(status?: string) {
  if (!status) {
    return '-'
  }
  return followUpStatusTextMap[status] || status
}

function getFollowUpStatusTagType(status?: string) {
  if (!status) {
    return 'info'
  }
  return followUpStatusTagTypeMap[status] || 'info'
}
</script>

<template>
  <div class="center-workspace">
    <StrategySignalStatsCards :stats="stats" />

    <FilterDock :title="text.title" description="按信号方向、强度、标的和审核状态筛选策略信号。">
      <el-form inline>
        <el-form-item :label="text.targetCode">
          <el-input v-model="query.targetCode" :placeholder="text.targetCodePlaceholder" clearable />
        </el-form-item>

        <el-form-item :label="text.targetName">
          <el-input v-model="query.targetName" :placeholder="text.targetNamePlaceholder" clearable />
        </el-form-item>

        <el-form-item :label="text.signalDirection">
          <el-select v-model="query.signalDirection" :placeholder="text.signalDirectionPlaceholder" clearable class="center-select">
            <el-option
              v-for="option in signalDirectionOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item :label="text.signalStrength">
          <el-select v-model="query.signalStrength" :placeholder="text.signalStrengthPlaceholder" clearable class="center-select">
            <el-option
              v-for="option in signalStrengthOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item :label="text.reviewStatus">
          <el-select v-model="query.reportReviewStatus" :placeholder="text.reviewStatusPlaceholder" clearable class="center-select">
            <el-option
              v-for="option in reviewStatusOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-checkbox v-model="query.onlyHighConfidence">{{ text.onlyHighConfidence }}</el-checkbox>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSearch">{{ text.search }}</el-button>
          <el-button @click="handleReset">{{ text.reset }}</el-button>
          <el-button @click="reloadAll">{{ text.refresh }}</el-button>
          <el-button v-if="canEditSignals" type="success" @click="openCreateSignal()">{{ text.createSignal }}</el-button>
        </el-form-item>
      </el-form>
    </FilterDock>

    <el-card class="center-table-card">
      <el-table :data="pageData.records" v-loading="loading" border :empty-text="text.empty">
        <el-table-column :label="text.rank" width="80">
          <template #default="{ $index }">
            {{ getRowIndex($index) }}
          </template>
        </el-table-column>

        <el-table-column prop="taskId" :label="text.taskId" min-width="220" />
        <el-table-column prop="taskTitle" :label="text.taskTitle" min-width="180" />
        <el-table-column prop="targetCode" :label="text.targetCode" width="100" />
        <el-table-column prop="targetName" :label="text.targetName" width="120" />

        <el-table-column :label="text.signalDirection" width="100">
          <template #default="{ row }">
            <el-tag :type="getSignalDirectionTagType(row.signalDirection)">
              {{ getSignalDirectionText(row.signalDirection) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column :label="text.signalStrength" width="100">
          <template #default="{ row }">
            <el-tag :type="getSignalStrengthTagType(row.signalStrength)">
              {{ getSignalStrengthText(row.signalStrength) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column :label="text.priority" width="90">
          <template #default="{ row }">
            <el-tag :type="getPriorityTagType(row.priority)">
              {{ getPriorityText(row.priority) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="signalScore" :label="text.signalScore" width="100" />

        <el-table-column :label="text.confidenceScore" width="100">
          <template #default="{ row }">
            {{ formatConfidence(row.confidenceScore) }}
          </template>
        </el-table-column>

        <el-table-column :label="text.reviewStatus" width="120">
          <template #default="{ row }">
            <el-tag :type="getReviewStatusTagType(row.reportReviewStatus)">
              {{ getReviewStatusText(row.reportReviewStatus) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column :label="text.followUpStatus" width="120">
          <template #default="{ row }">
            <el-tag :type="getFollowUpStatusTagType(row.followUpStatus)">
              {{ getFollowUpStatusText(row.followUpStatus) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column :label="text.signalSourceCategory" min-width="180">
          <template #default="{ row }">
            <div v-if="row.signalSourceTags?.length" style="display:flex; gap:4px; flex-wrap:wrap;">
              <el-tag
                v-for="source in row.signalSourceTags"
                :key="source"
                :type="getSignalSourceTagType(source)"
                effect="plain"
              >
                {{ getSignalSourceText(source) }}
              </el-tag>
            </div>
            <span v-else>-</span>
          </template>
        </el-table-column>

        <el-table-column :label="text.revisionStatus" min-width="180">
          <template #default="{ row }">
            <ReportRevisionStatusTags
              :revised="row.revised"
              :summary-revised="row.summaryRevised"
              :highlights-revised="row.highlightsRevised"
              :risk-points-revised="row.riskPointsRevised"
            />
          </template>
        </el-table-column>

        <el-table-column :label="text.strategySummary" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.strategySummary || '-' }}
          </template>
        </el-table-column>

        <el-table-column :label="text.signalSources" min-width="240">
          <template #default="{ row }">
            <div v-if="row.signalSources?.length" style="display:flex; gap:4px; flex-wrap:wrap;">
              <el-tag
                v-for="source in row.signalSources.slice(0, 3)"
                :key="source"
                type="info"
                effect="plain"
              >
                {{ source }}
              </el-tag>
              <el-tag v-if="row.signalSources.length > 3" type="info" effect="plain">
                +{{ row.signalSources.length - 3 }}
              </el-tag>
            </div>
            <span v-else>-</span>
          </template>
        </el-table-column>

        <el-table-column :label="text.backtestStatus" width="110">
          <template #default="{ row }">
            <el-tag :type="getBacktestStatusTagType(row.backtestStatus)">
              {{ getBacktestStatusText(row.backtestStatus) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column :label="text.createdAt" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>

        <el-table-column :label="text.action" width="520" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openSignalDetail(row)">{{ text.openSignalDetail }}</el-button>
            <el-button v-if="canEditSignals && row.signalId" link type="primary" @click="openCreateSignal(row)">{{ text.editSignal }}</el-button>
            <el-button link type="primary" @click="goTaskDetail(row.taskId)">{{ text.detail }}</el-button>
            <el-button link type="warning" @click="goWorkbench(row.targetCode, row.targetName)">{{ text.workbench }}</el-button>
            <el-button v-if="actionAccess.showCreateTask" link type="info" @click="goCreateTask(row)">{{ text.createTask }}</el-button>
            <el-button link type="success" @click="goReport(row.taskId)">{{ text.report }}</el-button>
            <el-button
              v-if="canEditSignals && row.signalId"
              link
              type="warning"
              :loading="statusUpdatingSignalId === row.signalId"
              @click="changeSignalStatus(row, 'ARCHIVED')"
            >
              {{ text.archive }}
            </el-button>
            <el-button
              v-if="canEditSignals && row.signalId"
              link
              type="danger"
              :loading="statusUpdatingSignalId === row.signalId"
              @click="changeSignalStatus(row, 'DISABLED')"
            >
              {{ text.disable }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-row">
        <el-pagination
          v-model:current-page="pageData.pageNum"
          v-model:page-size="pageData.pageSize"
          background
          layout="total, prev, pager, next, sizes"
          :total="pageData.total"
          @current-change="handlePageChange"
          @size-change="handlePageSizeChange"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="detailDialogVisible"
      :title="text.signalDetail"
      width="960px"
    >
      <template v-if="selectedSignal">
        <el-descriptions :column="2" border>
          <el-descriptions-item :label="text.taskId">{{ selectedSignal.taskId }}</el-descriptions-item>
          <el-descriptions-item :label="text.taskTitle">{{ selectedSignal.taskTitle }}</el-descriptions-item>
          <el-descriptions-item :label="text.targetCode">{{ selectedSignal.targetCode }}</el-descriptions-item>
          <el-descriptions-item :label="text.targetName">{{ selectedSignal.targetName }}</el-descriptions-item>
          <el-descriptions-item :label="text.signalDirection">
            <el-tag :type="getSignalDirectionTagType(selectedSignal.signalDirection)">
              {{ getSignalDirectionText(selectedSignal.signalDirection) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="text.signalStrength">
            <el-tag :type="getSignalStrengthTagType(selectedSignal.signalStrength)">
              {{ getSignalStrengthText(selectedSignal.signalStrength) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="text.signalScore">{{ selectedSignal.signalScore }}</el-descriptions-item>
          <el-descriptions-item :label="text.confidenceScore">{{ formatConfidence(selectedSignal.confidenceScore) }}</el-descriptions-item>
          <el-descriptions-item :label="text.reviewStatus">
            <el-tag :type="getReviewStatusTagType(selectedSignal.reportReviewStatus)">
              {{ getReviewStatusText(selectedSignal.reportReviewStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="text.needHumanReview">
            <el-tag :type="getHumanReviewTagType(selectedSignal.needHumanReview)">
              {{ getHumanReviewText(selectedSignal.needHumanReview) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="text.reviewedBy">
            {{ selectedSignal.reportReviewedBy || '-' }}
          </el-descriptions-item>
          <el-descriptions-item :label="text.reviewedAt">
            {{ formatDateTime(selectedSignal.reportReviewedAt) }}
          </el-descriptions-item>
          <el-descriptions-item :label="text.reviewComment" :span="2">
            {{ selectedSignal.reviewComment || text.noReviewComment }}
          </el-descriptions-item>
          <el-descriptions-item :label="text.strategySummary" :span="2">
            <div style="white-space: pre-wrap; word-break: break-word;">
              {{ selectedSignal.strategySummary || '-' }}
            </div>
          </el-descriptions-item>
        </el-descriptions>

        <el-row :gutter="16" style="margin-top: 16px;">
          <el-col :span="12">
            <el-card shadow="never">
              <template #header>
                <div style="font-weight: 700;">{{ text.signalSourceCategory }}</div>
              </template>
              <div v-if="selectedSignal.signalSourceTags?.length" style="display:flex; gap:8px; flex-wrap:wrap;">
                <el-tag
                  v-for="source in selectedSignal.signalSourceTags"
                  :key="source"
                  :type="getSignalSourceTagType(source)"
                  effect="plain"
                >
                  {{ getSignalSourceText(source) }}
                </el-tag>
              </div>
              <span v-else>-</span>
            </el-card>
          </el-col>

          <el-col :span="12">
            <el-card shadow="never">
              <template #header>
                <div style="font-weight: 700;">{{ text.revisionStatus }}</div>
              </template>
              <ReportRevisionStatusTags
                :revised="selectedSignal.revised"
                :summary-revised="selectedSignal.summaryRevised"
                :highlights-revised="selectedSignal.highlightsRevised"
                :risk-points-revised="selectedSignal.riskPointsRevised"
              />
            </el-card>
          </el-col>
        </el-row>

        <el-row :gutter="16" style="margin-top: 16px;">
          <el-col :span="12">
            <el-card shadow="never">
              <template #header>
                <div style="font-weight: 700;">{{ text.signalSources }}</div>
              </template>
              <div v-if="selectedSignal.signalSources?.length" style="display:flex; gap:8px; flex-wrap:wrap;">
                <el-tag
                  v-for="source in selectedSignal.signalSources"
                  :key="source"
                  type="info"
                  effect="plain"
                >
                  {{ source }}
                </el-tag>
              </div>
              <span v-else>-</span>
            </el-card>
          </el-col>

          <el-col :span="12">
            <el-card shadow="never">
              <template #header>
                <div style="font-weight: 700;">{{ text.backtestSummary }}</div>
              </template>
              <div style="margin-bottom: 8px;">
                <el-tag :type="getBacktestStatusTagType(selectedSignal.backtestStatus)">
                  {{ getBacktestStatusText(selectedSignal.backtestStatus) }}
                </el-tag>
              </div>
              <div style="white-space: pre-wrap; word-break: break-word;">
                {{ selectedSignal.backtestSummary || '-' }}
              </div>
            </el-card>
          </el-col>
        </el-row>

        <el-card shadow="never" style="margin-top: 16px;">
          <template #header>
            <div style="font-weight: 700;">{{ text.factorDetail }}</div>
          </template>
          <el-table :data="signalFactors" v-loading="factorLoading" border>
            <el-table-column prop="factorCode" :label="text.factorCode" width="140" />
            <el-table-column prop="factorName" :label="text.factorName" width="160" />
            <el-table-column prop="factorValue" :label="text.factorValue" width="140" />
            <el-table-column prop="factorWeight" :label="text.factorWeight" width="100" />
            <el-table-column prop="factorConclusion" :label="text.factorConclusion" min-width="220" show-overflow-tooltip />
          </el-table>
        </el-card>

        <el-card shadow="never" style="margin-top: 16px;">
          <template #header>
            <div style="font-weight: 700;">{{ text.followUpStatus }}</div>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item :label="text.followUpStatus">
              <el-tag :type="getFollowUpStatusTagType(selectedSignal.followUpStatus)">
                {{ getFollowUpStatusText(selectedSignal.followUpStatus) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item :label="text.followUpTaskCount">
              {{ selectedSignal.followUpTaskCount ?? 0 }}
            </el-descriptions-item>
            <el-descriptions-item :label="text.latestFollowUpTask">
              <div v-if="selectedSignal.latestFollowUpTaskId">
                <div>{{ selectedSignal.latestFollowUpTaskTitle || selectedSignal.latestFollowUpTaskId }}</div>
                <div style="margin-top: 8px;">
                  <el-tag v-if="selectedSignal.latestFollowUpTaskStatus" :type="getTaskStatusTagType(selectedSignal.latestFollowUpTaskStatus)">
                    {{ getTaskStatusText(selectedSignal.latestFollowUpTaskStatus) }}
                  </el-tag>
                  <el-button
                    link
                    type="primary"
                    style="margin-left: 8px;"
                    @click="openLatestFollowUpTask(selectedSignal.latestFollowUpTaskId)"
                  >
                    {{ text.openLatestFollowUpTask }}
                  </el-button>
                </div>
              </div>
              <span v-else>-</span>
            </el-descriptions-item>
            <el-descriptions-item :label="text.latestFollowUpCreatedAt">
              {{ formatDateTime(selectedSignal.latestFollowUpCreatedAt) }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </template>
    </el-dialog>

    <el-dialog
      v-model="createDialogVisible"
      :title="createForm.signalId ? text.editSignal : text.createSignalTitle"
      width="920px"
    >
      <el-form label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="text.targetCode" required>
              <el-input v-model="createForm.entityCode" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="text.targetName" required>
              <el-input v-model="createForm.entityName" clearable />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item :label="text.signalDirection">
              <el-select v-model="createForm.signalDirection" style="width: 100%;">
                <el-option
                  v-for="option in signalDirectionOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="text.signalStrength">
              <el-select v-model="createForm.signalLevel" style="width: 100%;">
                <el-option
                  v-for="option in signalStrengthOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="text.signalScore">
              <el-input-number v-model="createForm.signalScore" :min="0" :max="100" style="width: 100%;" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item :label="text.confidenceScore">
              <el-input-number v-model="createForm.confidenceScore" :min="0" :max="1" :step="0.05" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="信号日期">
              <el-date-picker v-model="createForm.signalDate" type="date" value-format="YYYY-MM-DD" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="来源事件">
              <el-input v-model="createForm.sourceEventId" clearable />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item :label="text.strategySummary">
          <el-input v-model="createForm.reasonSummary" type="textarea" :rows="4" resize="vertical" />
        </el-form-item>

        <el-form-item :label="text.factorDetail">
          <div class="factor-editor">
            <div
              v-for="(factor, index) in createForm.factors"
              :key="index"
              class="factor-editor-row"
            >
              <el-input v-model="factor.factorCode" :placeholder="text.factorCode" />
              <el-input v-model="factor.factorName" :placeholder="text.factorName" />
              <el-input v-model="factor.factorValue" :placeholder="text.factorValue" />
              <el-input-number v-model="factor.factorWeight" :min="0" :max="1" :step="0.05" />
              <el-input v-model="factor.factorConclusion" :placeholder="text.factorConclusion" />
              <el-button link type="danger" @click="removeFactor(index)">{{ text.removeFactor }}</el-button>
            </div>
            <el-button type="primary" plain @click="addFactor">{{ text.addFactor }}</el-button>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="createDialogVisible = false">{{ text.cancel }}</el-button>
        <el-button type="primary" :loading="createSaving" @click="submitCreateSignal">{{ text.save }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.center-workspace {
  display: grid;
  gap: 16px;
}

.center-select {
  width: 140px;
}

.center-table-card {
  min-width: 0;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.factor-editor {
  display: grid;
  gap: 10px;
  width: 100%;
}

.factor-editor-row {
  display: grid;
  grid-template-columns: 120px 140px 120px 120px minmax(180px, 1fr) 56px;
  gap: 8px;
  align-items: center;
}
</style>
