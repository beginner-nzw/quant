<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchMarketIntelligence, fetchMarketIntelligenceStats } from '../../api/task'
import FilterDock from '../../components/common/FilterDock.vue'
import MarketIntelligenceStatsCards from '../../components/report/MarketIntelligenceStatsCards.vue'
import ReportRevisionStatusTags from '../../components/report/ReportRevisionStatusTags.vue'
import type { MarketIntelligenceListItem, MarketIntelligenceStats } from '../../types/task'
import {
  ANALYSIS_SCOPE,
  MARKET_INTELLIGENCE_TYPE,
  REPORT_REVIEW_STATUS,
  TASK_TYPE,
  type MarketIntelligenceType,
  type ReportReviewStatus
} from '../../types/taskEnums'
import { formatDateTime } from '../../utils/format'
import { resolveCenterActionAccess } from '../../utils/taskActionAccess'
import {
  getHumanReviewTagType,
  getHumanReviewText,
  getMarketIntelligenceTypeTagType,
  getMarketIntelligenceTypeText,
  getPriorityTagType,
  getPriorityText,
  getReviewStatusTagType,
  getReviewStatusText,
  getRiskLevelTagType,
  getRiskLevelText,
  getSignalDirectionTagType,
  getSignalDirectionText,
  getTaskStatusTagType,
  getTaskStatusText
} from '../../utils/task'
import { buildResearchWorkbenchQuery } from '../../utils/researchWorkbench'
import { buildFollowUpTaskTitle, buildTaskCreateQuery } from '../../utils/taskCreate'

const route = useRoute()
const router = useRouter()

const text = {
  title: '市场情报中心',
  targetCode: '标的代码',
  targetCodePlaceholder: '如 01929',
  targetName: '标的名称',
  targetNamePlaceholder: '如 周大福',
  intelligenceType: '情报类型',
  intelligenceTypePlaceholder: '全部情报',
  reviewStatus: '审核状态',
  reviewStatusPlaceholder: '全部状态',
  onlyHighPriority: '仅看高优先级',
  needHumanReviewFilter: '仅看需人工复核',
  search: '查询',
  reset: '重置',
  refresh: '刷新',
  empty: '暂无市场情报数据',
  taskId: '任务 ID',
  taskTitle: '任务标题',
  priority: '优先级',
  reportType: '报告类型',
  intelligenceSource: '情报来源',
  revisionStatus: '修订情况',
  followUpStatus: '转任务状态',
  followUpTaskCount: '转任务数',
  latestFollowUpTask: '最近转任务',
  latestFollowUpCreatedAt: '最近转任务时间',
  openLatestFollowUpTask: '查看最近转任务',
  riskLevel: '风险等级',
  signalDirection: '信号方向',
  humanReview: '人工复核',
  summary: '情报摘要',
  intelligenceDetail: '情报详情',
  openIntelligenceDetail: '查看情报',
  sourceChannel: '来源渠道',
  reviewedBy: '审核人',
  reviewedAt: '审核时间',
  reviewComment: '审核意见',
  noReviewComment: '无审核意见',
  createdAt: '生成时间',
  action: '操作',
  detail: '任务详情',
  createTask: '发起研究',
  report: '查看报告',
  workbench: '投研工作台',
  loadStatsFailed: '市场情报统计加载失败',
  loadStatsError: '市场情报统计加载异常',
  loadListFailed: '市场情报列表加载失败',
  loadListError: '市场情报列表加载异常'
} as const

const defaultStats: MarketIntelligenceStats = {
  totalCount: 0,
  riskAlertCount: 0,
  strategySignalCount: 0,
  reportInsightCount: 0,
  highPriorityCount: 0,
  pendingReviewCount: 0
}

const loading = ref(false)
const stats = ref<MarketIntelligenceStats>({ ...defaultStats })
const detailDialogVisible = ref(false)
const selectedIntelligence = ref<MarketIntelligenceListItem | null>(null)

const pageData = reactive({
  total: 0,
  pageNum: 1,
  pageSize: 10,
  records: [] as MarketIntelligenceListItem[]
})

const query = reactive({
  targetCode: '',
  targetName: '',
  intelligenceType: '' as MarketIntelligenceType | '',
  reviewStatus: '' as ReportReviewStatus | '',
  onlyHighPriority: false,
  needHumanReview: false
})

const intelligenceTypeOptions = Object.values(MARKET_INTELLIGENCE_TYPE).map((value) => ({
  label: getMarketIntelligenceTypeText(value),
  value
}))

const reviewStatusOptions = Object.values(REPORT_REVIEW_STATUS).map((value) => ({
  label: getReviewStatusText(value),
  value
}))

const intelligenceTypeValueSet = new Set<string>(Object.values(MARKET_INTELLIGENCE_TYPE))
const reviewStatusValueSet = new Set<string>(Object.values(REPORT_REVIEW_STATUS))

const intelligenceSourceTextMap: Record<string, string> = {
  SOURCE_CHANNEL: '来源渠道',
  REPORT_SUMMARY: '报告摘要',
  RISK_ALERT: '风险情报',
  STRATEGY_SIGNAL: '策略信号',
  REPORT_INSIGHT: '报告洞察',
  HUMAN_REVIEW: '人工复核',
  REVIEW_REJECTED: '审核驳回'
}

const intelligenceSourceTagTypeMap: Record<string, 'success' | 'warning' | 'info' | 'danger'> = {
  SOURCE_CHANNEL: 'info',
  REPORT_SUMMARY: 'info',
  RISK_ALERT: 'danger',
  STRATEGY_SIGNAL: 'success',
  REPORT_INSIGHT: 'warning',
  HUMAN_REVIEW: 'info',
  REVIEW_REJECTED: 'danger'
}

const followUpStatusTextMap: Record<string, string> = {
  NOT_TRACKED: '未转任务',
  TRACKING: '处理中',
  COMPLETED: '已完成',
  FAILED: '处理失败'
}

const followUpStatusTagTypeMap: Record<string, 'info' | 'warning' | 'success' | 'danger'> = {
  NOT_TRACKED: 'info',
  TRACKING: 'warning',
  COMPLETED: 'success',
  FAILED: 'danger'
}
const actionAccess = computed(() => resolveCenterActionAccess())

function syncQueryFromRoute() {
  query.targetCode = getRouteQueryValue(route.query.targetCode)
  query.targetName = getRouteQueryValue(route.query.targetName)
  query.intelligenceType = resolveIntelligenceType(getRouteQueryValue(route.query.intelligenceType))
  query.reviewStatus = resolveReviewStatus(getRouteQueryValue(route.query.reviewStatus))
  query.onlyHighPriority = resolveBooleanQuery(route.query.onlyHighPriority)
  query.needHumanReview = resolveBooleanQuery(route.query.needHumanReview)
  pageData.pageNum = normalizePageValue(route.query.pageNum, 1)
  pageData.pageSize = normalizePageValue(route.query.pageSize, 10)
}

async function loadStats() {
  try {
    const res = await fetchMarketIntelligenceStats()
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

async function loadMarketIntelligence() {
  loading.value = true
  try {
    const res = await fetchMarketIntelligence({
      pageNum: pageData.pageNum,
      pageSize: pageData.pageSize,
      targetCode: normalizeQueryValue(query.targetCode),
      targetName: normalizeQueryValue(query.targetName),
      intelligenceType: query.intelligenceType || undefined,
      reviewStatus: query.reviewStatus || undefined,
      onlyHighPriority: query.onlyHighPriority || undefined,
      needHumanReview: query.needHumanReview || undefined
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
  await Promise.all([loadStats(), loadMarketIntelligence()])
}

function buildRouteQuery() {
  return {
    targetCode: normalizeQueryValue(query.targetCode),
    targetName: normalizeQueryValue(query.targetName),
    intelligenceType: query.intelligenceType || undefined,
    reviewStatus: query.reviewStatus || undefined,
    onlyHighPriority: query.onlyHighPriority ? 'true' : undefined,
    needHumanReview: query.needHumanReview ? 'true' : undefined,
    pageNum: pageData.pageNum > 1 ? String(pageData.pageNum) : undefined,
    pageSize: pageData.pageSize !== 10 ? String(pageData.pageSize) : undefined
  }
}

function isSameRouteState(nextQuery: ReturnType<typeof buildRouteQuery>) {
  return getRouteQueryValue(route.query.targetCode) === (nextQuery.targetCode || '')
    && getRouteQueryValue(route.query.targetName) === (nextQuery.targetName || '')
    && getRouteQueryValue(route.query.intelligenceType) === (nextQuery.intelligenceType || '')
    && getRouteQueryValue(route.query.reviewStatus) === (nextQuery.reviewStatus || '')
    && resolveBooleanQuery(route.query.onlyHighPriority) === Boolean(nextQuery.onlyHighPriority)
    && resolveBooleanQuery(route.query.needHumanReview) === Boolean(nextQuery.needHumanReview)
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
    path: '/intelligence',
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
  query.intelligenceType = ''
  query.reviewStatus = ''
  query.onlyHighPriority = false
  query.needHumanReview = false
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

function goCreateTask(row: MarketIntelligenceListItem) {
  const taskTitle = buildFollowUpTaskTitle(
    row.targetName,
    row.targetCode,
    resolveTaskTitleSuffix(row.intelligenceType)
  )

  router.push({
    path: '/tasks/create',
    query: buildTaskCreateQuery({
      taskType: resolveTaskType(row.intelligenceType),
      taskTitle,
      targetCode: row.targetCode,
      targetName: row.targetName,
      priority: row.priority,
      sourceTaskId: row.taskId,
      sourceReportId: row.reportId,
      sourceDomain: 'MARKET_INTELLIGENCE',
      sourceReviewStatus: row.reviewStatus,
      analysisScope: resolveAnalysisScope(row.intelligenceType),
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

function openIntelligenceDetail(row: MarketIntelligenceListItem) {
  selectedIntelligence.value = row
  detailDialogVisible.value = true
}

function openLatestFollowUpTask(taskId?: string) {
  if (!taskId) {
    return
  }
  goTaskDetail(taskId)
}

watch([
  () => route.query.targetCode,
  () => route.query.targetName,
  () => route.query.intelligenceType,
  () => route.query.reviewStatus,
  () => route.query.onlyHighPriority,
  () => route.query.needHumanReview,
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

function resolveIntelligenceType(value: string): MarketIntelligenceType | '' {
  return intelligenceTypeValueSet.has(value) ? value as MarketIntelligenceType : ''
}

function resolveReviewStatus(value: string): ReportReviewStatus | '' {
  return reviewStatusValueSet.has(value) ? value as ReportReviewStatus : ''
}

function resolveTaskTitleSuffix(intelligenceType?: string) {
  switch (intelligenceType) {
    case MARKET_INTELLIGENCE_TYPE.RISK_ALERT:
      return '风险情报跟踪研究'
    case MARKET_INTELLIGENCE_TYPE.STRATEGY_SIGNAL:
      return '策略情报跟踪研究'
    default:
      return '报告洞察跟踪研究'
  }
}

function resolveAnalysisScope(intelligenceType?: string) {
  switch (intelligenceType) {
    case MARKET_INTELLIGENCE_TYPE.RISK_ALERT:
      return ANALYSIS_SCOPE.RISK_RECHECK
    case MARKET_INTELLIGENCE_TYPE.STRATEGY_SIGNAL:
      return ANALYSIS_SCOPE.SIGNAL_FOLLOW_UP
    default:
      return ANALYSIS_SCOPE.INTELLIGENCE_FOLLOW_UP
  }
}

function resolveTaskType(intelligenceType?: string) {
  if (intelligenceType === MARKET_INTELLIGENCE_TYPE.RISK_ALERT) {
    return TASK_TYPE.RISK_REVIEW
  }
  return TASK_TYPE.FOLLOW_UP_RESEARCH
}

function getIntelligenceSourceText(source: string) {
  return intelligenceSourceTextMap[source] || source
}

function getIntelligenceSourceTagType(source: string) {
  return intelligenceSourceTagTypeMap[source] || 'info'
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
    <MarketIntelligenceStatsCards :stats="stats" />

    <FilterDock :title="text.title" description="按情报类型、标的和审核状态筛选市场情报。">
      <el-form inline>
        <el-form-item :label="text.targetCode">
          <el-input v-model="query.targetCode" :placeholder="text.targetCodePlaceholder" clearable />
        </el-form-item>

        <el-form-item :label="text.targetName">
          <el-input v-model="query.targetName" :placeholder="text.targetNamePlaceholder" clearable />
        </el-form-item>

        <el-form-item :label="text.intelligenceType">
          <el-select v-model="query.intelligenceType" :placeholder="text.intelligenceTypePlaceholder" clearable class="center-select">
            <el-option
              v-for="option in intelligenceTypeOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item :label="text.reviewStatus">
          <el-select v-model="query.reviewStatus" :placeholder="text.reviewStatusPlaceholder" clearable class="center-select">
            <el-option
              v-for="option in reviewStatusOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-checkbox v-model="query.onlyHighPriority">{{ text.onlyHighPriority }}</el-checkbox>
        </el-form-item>

        <el-form-item>
          <el-checkbox v-model="query.needHumanReview">{{ text.needHumanReviewFilter }}</el-checkbox>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSearch">{{ text.search }}</el-button>
          <el-button @click="handleReset">{{ text.reset }}</el-button>
          <el-button @click="reloadAll">{{ text.refresh }}</el-button>
        </el-form-item>
      </el-form>
    </FilterDock>

    <el-card class="center-table-card">
      <el-table :data="pageData.records" v-loading="loading" border :empty-text="text.empty">
        <el-table-column prop="taskId" :label="text.taskId" min-width="220" />
        <el-table-column prop="taskTitle" :label="text.taskTitle" min-width="180" />
        <el-table-column prop="targetCode" :label="text.targetCode" width="100" />
        <el-table-column prop="targetName" :label="text.targetName" width="120" />

        <el-table-column :label="text.intelligenceType" width="110">
          <template #default="{ row }">
            <el-tag :type="getMarketIntelligenceTypeTagType(row.intelligenceType)">
              {{ getMarketIntelligenceTypeText(row.intelligenceType) }}
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

        <el-table-column prop="reportType" :label="text.reportType" width="140">
          <template #default="{ row }">
            {{ row.reportType || '-' }}
          </template>
        </el-table-column>

        <el-table-column :label="text.intelligenceSource" min-width="220">
          <template #default="{ row }">
            <div v-if="row.intelligenceSourceTags?.length" style="display:flex; gap:4px; flex-wrap:wrap;">
              <el-tag
                v-for="source in row.intelligenceSourceTags"
                :key="source"
                :type="getIntelligenceSourceTagType(source)"
                effect="plain"
              >
                {{ getIntelligenceSourceText(source) }}
              </el-tag>
            </div>
            <span v-else>-</span>
          </template>
        </el-table-column>

        <el-table-column :label="text.followUpStatus" width="120">
          <template #default="{ row }">
            <el-tag :type="getFollowUpStatusTagType(row.followUpStatus)">
              {{ getFollowUpStatusText(row.followUpStatus) }}
            </el-tag>
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

        <el-table-column :label="text.riskLevel" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.riskLevel" :type="getRiskLevelTagType(row.riskLevel)">
              {{ getRiskLevelText(row.riskLevel) }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>

        <el-table-column :label="text.signalDirection" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.signalDirection" :type="getSignalDirectionTagType(row.signalDirection)">
              {{ getSignalDirectionText(row.signalDirection) }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>

        <el-table-column :label="text.humanReview" width="120">
          <template #default="{ row }">
            <el-tag :type="getHumanReviewTagType(row.needHumanReview)">
              {{ getHumanReviewText(row.needHumanReview) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column :label="text.reviewStatus" width="120">
          <template #default="{ row }">
            <el-tag :type="getReviewStatusTagType(row.reviewStatus)">
              {{ getReviewStatusText(row.reviewStatus) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column :label="text.summary" min-width="280" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.summary || '-' }}
          </template>
        </el-table-column>

        <el-table-column :label="text.createdAt" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>

        <el-table-column :label="text.action" width="380" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openIntelligenceDetail(row)">{{ text.openIntelligenceDetail }}</el-button>
            <el-button link type="primary" @click="goTaskDetail(row.taskId)">{{ text.detail }}</el-button>
            <el-button link type="warning" @click="goWorkbench(row.targetCode, row.targetName)">{{ text.workbench }}</el-button>
            <el-button v-if="actionAccess.showCreateTask" link type="info" @click="goCreateTask(row)">{{ text.createTask }}</el-button>
            <el-button link type="success" @click="goReport(row.taskId)">{{ text.report }}</el-button>
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
      :title="text.intelligenceDetail"
      width="960px"
    >
      <template v-if="selectedIntelligence">
        <el-descriptions :column="2" border>
          <el-descriptions-item :label="text.taskId">{{ selectedIntelligence.taskId }}</el-descriptions-item>
          <el-descriptions-item :label="text.taskTitle">{{ selectedIntelligence.taskTitle }}</el-descriptions-item>
          <el-descriptions-item :label="text.targetCode">{{ selectedIntelligence.targetCode }}</el-descriptions-item>
          <el-descriptions-item :label="text.targetName">{{ selectedIntelligence.targetName }}</el-descriptions-item>
          <el-descriptions-item :label="text.intelligenceType">
            <el-tag :type="getMarketIntelligenceTypeTagType(selectedIntelligence.intelligenceType)">
              {{ getMarketIntelligenceTypeText(selectedIntelligence.intelligenceType) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="text.priority">
            <el-tag :type="getPriorityTagType(selectedIntelligence.priority)">
              {{ getPriorityText(selectedIntelligence.priority) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="text.signalDirection">
            <el-tag v-if="selectedIntelligence.signalDirection" :type="getSignalDirectionTagType(selectedIntelligence.signalDirection)">
              {{ getSignalDirectionText(selectedIntelligence.signalDirection) }}
            </el-tag>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item :label="text.riskLevel">
            <el-tag v-if="selectedIntelligence.riskLevel" :type="getRiskLevelTagType(selectedIntelligence.riskLevel)">
              {{ getRiskLevelText(selectedIntelligence.riskLevel) }}
            </el-tag>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item :label="text.reviewStatus">
            <el-tag :type="getReviewStatusTagType(selectedIntelligence.reviewStatus)">
              {{ getReviewStatusText(selectedIntelligence.reviewStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="text.humanReview">
            <el-tag :type="getHumanReviewTagType(selectedIntelligence.needHumanReview)">
              {{ getHumanReviewText(selectedIntelligence.needHumanReview) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="text.sourceChannel">
            {{ selectedIntelligence.sourceChannel || '-' }}
          </el-descriptions-item>
          <el-descriptions-item :label="text.reportType">
            {{ selectedIntelligence.reportType || '-' }}
          </el-descriptions-item>
          <el-descriptions-item :label="text.reviewedBy">
            {{ selectedIntelligence.reviewedBy || '-' }}
          </el-descriptions-item>
          <el-descriptions-item :label="text.reviewedAt">
            {{ formatDateTime(selectedIntelligence.reviewedAt) }}
          </el-descriptions-item>
          <el-descriptions-item :label="text.reviewComment" :span="2">
            {{ selectedIntelligence.reviewComment || text.noReviewComment }}
          </el-descriptions-item>
          <el-descriptions-item :label="text.summary" :span="2">
            <div style="white-space: pre-wrap; word-break: break-word;">
              {{ selectedIntelligence.summary || '-' }}
            </div>
          </el-descriptions-item>
        </el-descriptions>

        <el-row :gutter="16" style="margin-top: 16px;">
          <el-col :span="12">
            <el-card shadow="never">
              <template #header>
                <div style="font-weight: 700;">{{ text.intelligenceSource }}</div>
              </template>
              <div v-if="selectedIntelligence.intelligenceSourceTags?.length" style="display:flex; gap:8px; flex-wrap:wrap;">
                <el-tag
                  v-for="source in selectedIntelligence.intelligenceSourceTags"
                  :key="source"
                  :type="getIntelligenceSourceTagType(source)"
                  effect="plain"
                >
                  {{ getIntelligenceSourceText(source) }}
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
                :revised="selectedIntelligence.revised"
                :summary-revised="selectedIntelligence.summaryRevised"
                :highlights-revised="selectedIntelligence.highlightsRevised"
                :risk-points-revised="selectedIntelligence.riskPointsRevised"
              />
            </el-card>
          </el-col>
        </el-row>

        <el-card shadow="never" style="margin-top: 16px;">
          <template #header>
            <div style="font-weight: 700;">{{ text.followUpStatus }}</div>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item :label="text.followUpStatus">
              <el-tag :type="getFollowUpStatusTagType(selectedIntelligence.followUpStatus)">
                {{ getFollowUpStatusText(selectedIntelligence.followUpStatus) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item :label="text.followUpTaskCount">
              {{ selectedIntelligence.followUpTaskCount ?? 0 }}
            </el-descriptions-item>
            <el-descriptions-item :label="text.latestFollowUpTask">
              <div v-if="selectedIntelligence.latestFollowUpTaskId">
                <div>{{ selectedIntelligence.latestFollowUpTaskTitle || selectedIntelligence.latestFollowUpTaskId }}</div>
                <div style="margin-top: 8px;">
                  <el-tag v-if="selectedIntelligence.latestFollowUpTaskStatus" :type="getTaskStatusTagType(selectedIntelligence.latestFollowUpTaskStatus)">
                    {{ getTaskStatusText(selectedIntelligence.latestFollowUpTaskStatus) }}
                  </el-tag>
                  <el-button
                    link
                    type="primary"
                    style="margin-left: 8px;"
                    @click="openLatestFollowUpTask(selectedIntelligence.latestFollowUpTaskId)"
                  >
                    {{ text.openLatestFollowUpTask }}
                  </el-button>
                </div>
              </div>
              <span v-else>-</span>
            </el-descriptions-item>
            <el-descriptions-item :label="text.latestFollowUpCreatedAt">
              {{ formatDateTime(selectedIntelligence.latestFollowUpCreatedAt) }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
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
</style>
