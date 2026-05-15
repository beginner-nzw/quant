<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchRiskWarningStats, fetchRiskWarnings } from '../../api/task'
import FilterDock from '../../components/common/FilterDock.vue'
import ReportRevisionStatusTags from '../../components/report/ReportRevisionStatusTags.vue'
import RiskWarningStatsCards from '../../components/report/RiskWarningStatsCards.vue'
import type { RiskWarningListItem, RiskWarningStats } from '../../types/task'
import { ANALYSIS_SCOPE, REPORT_REVIEW_STATUS, RISK_LEVEL, TASK_TYPE, type ReportReviewStatus, type RiskLevel } from '../../types/taskEnums'
import { formatDateTime } from '../../utils/format'
import { resolveCenterActionAccess } from '../../utils/taskActionAccess'
import {
  getHumanReviewTagType,
  getHumanReviewText,
  getPriorityTagType,
  getPriorityText,
  getReviewStatusTagType,
  getReviewStatusText,
  getRiskLevelTagType,
  getRiskLevelText,
  getTaskStageText,
  getTaskStatusTagType,
  getTaskStatusText
} from '../../utils/task'
import { buildResearchWorkbenchQuery } from '../../utils/researchWorkbench'
import { buildFollowUpTaskTitle, buildTaskCreateQuery } from '../../utils/taskCreate'

const route = useRoute()
const router = useRouter()

const text = {
  title: '风险预警中心',
  targetCode: '标的代码',
  targetCodePlaceholder: '如 01929',
  targetName: '标的名称',
  targetNamePlaceholder: '如 周大福',
  riskLevel: '风险等级',
  riskLevelPlaceholder: '全部等级',
  reviewStatus: '审核状态',
  reviewStatusPlaceholder: '全部状态',
  needHumanReview: '仅看需人工复核',
  search: '查询',
  reset: '重置',
  refresh: '刷新',
  empty: '暂无风险预警数据',
  taskId: '任务 ID',
  taskTitle: '任务标题',
  priority: '优先级',
  taskStatus: '任务状态',
  currentStage: '当前阶段',
  totalRiskCount: '风险项数',
  riskSource: '预警来源',
  revisionStatus: '修订情况',
  followUpStatus: '复核状态',
  followUpTaskCount: '复核任务数',
  latestFollowUpTask: '最近复核任务',
  latestFollowUpCreatedAt: '最近复核时间',
  openLatestFollowUpTask: '查看最近复核任务',
  riskReasons: '关键预警原因',
  riskDetail: '风险详情',
  openRiskDetail: '查看风险',
  summary: '风险摘要',
  reviewedBy: '审核人',
  reviewedAt: '审核时间',
  reviewComment: '审核意见',
  noReviewComment: '无审核意见',
  createdAt: '报告时间',
  action: '操作',
  detail: '任务详情',
  createTask: '发起研究',
  report: '查看报告',
  workbench: '投研工作台',
  loadStatsFailed: '风险预警统计加载失败',
  loadStatsError: '风险预警统计加载异常',
  loadListFailed: '风险预警列表加载失败',
  loadListError: '风险预警列表加载异常'
} as const

const defaultStats: RiskWarningStats = {
  totalCount: 0,
  highCount: 0,
  mediumCount: 0,
  lowCount: 0,
  pendingReviewCount: 0,
  humanReviewCount: 0
}

const loading = ref(false)
const stats = ref<RiskWarningStats>({ ...defaultStats })
const detailDialogVisible = ref(false)
const selectedRisk = ref<RiskWarningListItem | null>(null)

const pageData = reactive({
  total: 0,
  pageNum: 1,
  pageSize: 10,
  records: [] as RiskWarningListItem[]
})

const query = reactive({
  targetCode: '',
  targetName: '',
  riskLevel: '' as RiskLevel | '',
  reportReviewStatus: '' as ReportReviewStatus | '',
  needHumanReview: false
})

const riskLevelOptions = Object.values(RISK_LEVEL).map((value) => ({
  label: getRiskLevelText(value),
  value
}))

const reviewStatusOptions = Object.values(REPORT_REVIEW_STATUS).map((value) => ({
  label: getReviewStatusText(value),
  value
}))

const riskLevelValueSet = new Set<string>(Object.values(RISK_LEVEL))
const reviewStatusValueSet = new Set<string>(Object.values(REPORT_REVIEW_STATUS))

function getDisplayTaskStageText(stage?: string) {
  if (stage === 'EVIDENCE_COLLECTION') {
    return '证据采集'
  }
  return getTaskStageText(stage)
}

const riskSourceTextMap: Record<string, string> = {
  WARNING_SIGNAL: '风险预警',
  REPORT_RISK_POINT: '报告风险点',
  HUMAN_REVIEW: '人工复核',
  REVIEW_REJECTED: '审核驳回'
}

const riskSourceTagTypeMap: Record<string, 'danger' | 'warning' | 'info'> = {
  WARNING_SIGNAL: 'danger',
  REPORT_RISK_POINT: 'warning',
  HUMAN_REVIEW: 'info',
  REVIEW_REJECTED: 'danger'
}

const followUpStatusTextMap: Record<string, string> = {
  NOT_TRACKED: '未复核',
  TRACKING: '复核中',
  COMPLETED: '已完成',
  FAILED: '复核失败'
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
  query.riskLevel = resolveRiskLevel(getRouteQueryValue(route.query.riskLevel))
  query.reportReviewStatus = resolveReviewStatus(getRouteQueryValue(route.query.reportReviewStatus))
  query.needHumanReview = resolveBooleanQuery(route.query.needHumanReview)
  pageData.pageNum = normalizePageValue(route.query.pageNum, 1)
  pageData.pageSize = normalizePageValue(route.query.pageSize, 10)
}

async function loadStats() {
  try {
    const res = await fetchRiskWarningStats()
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

async function loadWarnings() {
  loading.value = true
  try {
    const res = await fetchRiskWarnings({
      pageNum: pageData.pageNum,
      pageSize: pageData.pageSize,
      targetCode: normalizeQueryValue(query.targetCode),
      targetName: normalizeQueryValue(query.targetName),
      riskLevel: query.riskLevel || undefined,
      reportReviewStatus: query.reportReviewStatus || undefined,
      needHumanReview: query.needHumanReview || undefined
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

async function reloadAll() {
  await Promise.all([loadStats(), loadWarnings()])
}

function buildRouteQuery() {
  return {
    targetCode: normalizeQueryValue(query.targetCode),
    targetName: normalizeQueryValue(query.targetName),
    riskLevel: query.riskLevel || undefined,
    reportReviewStatus: query.reportReviewStatus || undefined,
    needHumanReview: query.needHumanReview ? 'true' : undefined,
    pageNum: pageData.pageNum > 1 ? String(pageData.pageNum) : undefined,
    pageSize: pageData.pageSize !== 10 ? String(pageData.pageSize) : undefined
  }
}

function isSameRouteState(nextQuery: ReturnType<typeof buildRouteQuery>) {
  return getRouteQueryValue(route.query.targetCode) === (nextQuery.targetCode || '')
    && getRouteQueryValue(route.query.targetName) === (nextQuery.targetName || '')
    && getRouteQueryValue(route.query.riskLevel) === (nextQuery.riskLevel || '')
    && getRouteQueryValue(route.query.reportReviewStatus) === (nextQuery.reportReviewStatus || '')
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
    path: '/risk-warnings',
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
  query.riskLevel = ''
  query.reportReviewStatus = ''
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

function goCreateTask(row: RiskWarningListItem) {
  router.push({
    path: '/tasks/create',
      query: buildTaskCreateQuery({
        taskType: TASK_TYPE.RISK_REVIEW,
        taskTitle: buildFollowUpTaskTitle(row.targetName, row.targetCode, '风险复核研究'),
        targetCode: row.targetCode,
        targetName: row.targetName,
        priority: row.riskLevel === RISK_LEVEL.HIGH ? 'HIGH' : row.priority,
        sourceTaskId: row.taskId,
        sourceReportId: row.reportId,
        sourceDomain: 'RISK_WARNING',
        sourceReviewStatus: row.reportReviewStatus,
        analysisScope: ANALYSIS_SCOPE.RISK_RECHECK,
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

function openRiskDetail(row: RiskWarningListItem) {
  selectedRisk.value = row
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
  () => route.query.riskLevel,
  () => route.query.reportReviewStatus,
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

function resolveRiskLevel(value: string): RiskLevel | '' {
  return riskLevelValueSet.has(value) ? value as RiskLevel : ''
}

function resolveReviewStatus(value: string): ReportReviewStatus | '' {
  return reviewStatusValueSet.has(value) ? value as ReportReviewStatus : ''
}

function getRiskSourceText(source: string) {
  return riskSourceTextMap[source] || source
}

function getRiskSourceTagType(source: string) {
  return riskSourceTagTypeMap[source] || 'info'
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
    <RiskWarningStatsCards :stats="stats" />

    <FilterDock :title="text.title" description="按标的、风险级别和审核状态定位风险预警。">
      <el-form inline>
        <el-form-item :label="text.targetCode">
          <el-input v-model="query.targetCode" :placeholder="text.targetCodePlaceholder" clearable />
        </el-form-item>

        <el-form-item :label="text.targetName">
          <el-input v-model="query.targetName" :placeholder="text.targetNamePlaceholder" clearable />
        </el-form-item>

        <el-form-item :label="text.riskLevel">
          <el-select v-model="query.riskLevel" :placeholder="text.riskLevelPlaceholder" clearable class="center-select">
            <el-option
              v-for="option in riskLevelOptions"
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
          <el-checkbox v-model="query.needHumanReview">{{ text.needHumanReview }}</el-checkbox>
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
        <el-table-column prop="taskId" :label="text.taskId" min-width="240" />
        <el-table-column prop="taskTitle" :label="text.taskTitle" min-width="180" />
        <el-table-column prop="targetCode" :label="text.targetCode" width="100" />
        <el-table-column prop="targetName" :label="text.targetName" width="120" />

        <el-table-column :label="text.riskLevel" width="110">
          <template #default="{ row }">
            <el-tag :type="getRiskLevelTagType(row.riskLevel)">
              {{ getRiskLevelText(row.riskLevel) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column :label="text.priority" width="100">
          <template #default="{ row }">
            <el-tag :type="getPriorityTagType(row.priority)">
              {{ getPriorityText(row.priority) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column :label="text.taskStatus" width="110">
          <template #default="{ row }">
            <el-tag :type="getTaskStatusTagType(row.taskStatus)">
              {{ getTaskStatusText(row.taskStatus) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column :label="text.currentStage" width="160">
          <template #default="{ row }">
            {{ getDisplayTaskStageText(row.currentStage) }}
          </template>
        </el-table-column>

        <el-table-column :label="text.totalRiskCount" width="100">
          <template #default="{ row }">
            {{ row.totalRiskCount }}
          </template>
        </el-table-column>

        <el-table-column :label="text.riskSource" min-width="180">
          <template #default="{ row }">
            <div v-if="row.riskSourceTags?.length" style="display:flex; gap:4px; flex-wrap:wrap;">
              <el-tag
                v-for="source in row.riskSourceTags"
                :key="source"
                :type="getRiskSourceTagType(source)"
                effect="plain"
              >
                {{ getRiskSourceText(source) }}
              </el-tag>
            </div>
            <span v-else>-</span>
          </template>
        </el-table-column>

        <el-table-column :label="text.needHumanReview" width="130">
          <template #default="{ row }">
            <el-tag :type="getHumanReviewTagType(row.needHumanReview)">
              {{ getHumanReviewText(row.needHumanReview) }}
            </el-tag>
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

        <el-table-column :label="text.riskReasons" min-width="260">
          <template #default="{ row }">
            <div v-if="row.riskReasons?.length" style="display:flex; gap:4px; flex-wrap:wrap;">
              <el-tag
                v-for="reason in row.riskReasons.slice(0, 3)"
                :key="reason"
                type="danger"
                effect="plain"
              >
                {{ reason }}
              </el-tag>
              <el-tag v-if="row.riskReasons.length > 3" type="info" effect="plain">
                +{{ row.riskReasons.length - 3 }}
              </el-tag>
            </div>
            <span v-else>-</span>
          </template>
        </el-table-column>

        <el-table-column :label="text.createdAt" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>

        <el-table-column :label="text.action" width="320" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" @click="openRiskDetail(row)">{{ text.openRiskDetail }}</el-button>
            <el-button link type="primary" @click="goTaskDetail(row.taskId)">{{ text.detail }}</el-button>
            <el-button link type="warning" @click="goWorkbench(row.targetCode, row.targetName)">{{ text.workbench }}</el-button>
            <el-button v-if="actionAccess.showCreateTask" link type="info" @click="goCreateTask(row)">{{ text.createTask }}</el-button>
            <el-button link type="success" @click="goReport(row.taskId)">{{ text.report }}</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-row">
        <el-pagination
          background
          layout="total, prev, pager, next, sizes"
          :total="pageData.total"
          v-model:current-page="pageData.pageNum"
          v-model:page-size="pageData.pageSize"
          @current-change="handlePageChange"
          @size-change="handlePageSizeChange"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="detailDialogVisible"
      :title="text.riskDetail"
      width="960px"
    >
      <template v-if="selectedRisk">
        <el-descriptions :column="2" border>
          <el-descriptions-item :label="text.taskId">{{ selectedRisk.taskId }}</el-descriptions-item>
          <el-descriptions-item :label="text.taskTitle">{{ selectedRisk.taskTitle }}</el-descriptions-item>
          <el-descriptions-item :label="text.targetCode">{{ selectedRisk.targetCode }}</el-descriptions-item>
          <el-descriptions-item :label="text.targetName">{{ selectedRisk.targetName }}</el-descriptions-item>
          <el-descriptions-item :label="text.riskLevel">
            <el-tag :type="getRiskLevelTagType(selectedRisk.riskLevel)">
              {{ getRiskLevelText(selectedRisk.riskLevel) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="text.totalRiskCount">{{ selectedRisk.totalRiskCount }}</el-descriptions-item>
          <el-descriptions-item :label="text.reviewStatus">
            <el-tag :type="getReviewStatusTagType(selectedRisk.reportReviewStatus)">
              {{ getReviewStatusText(selectedRisk.reportReviewStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="text.needHumanReview">
            <el-tag :type="getHumanReviewTagType(selectedRisk.needHumanReview)">
              {{ getHumanReviewText(selectedRisk.needHumanReview) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="text.reviewedBy">
            {{ selectedRisk.reportReviewedBy || '-' }}
          </el-descriptions-item>
          <el-descriptions-item :label="text.reviewedAt">
            {{ formatDateTime(selectedRisk.reportReviewedAt) }}
          </el-descriptions-item>
          <el-descriptions-item :label="text.reviewComment" :span="2">
            {{ selectedRisk.reviewComment || text.noReviewComment }}
          </el-descriptions-item>
          <el-descriptions-item :label="text.summary" :span="2">
            <div style="white-space: pre-wrap; word-break: break-word;">
              {{ selectedRisk.summary || '-' }}
            </div>
          </el-descriptions-item>
        </el-descriptions>

        <el-row :gutter="16" style="margin-top: 16px;">
          <el-col :span="12">
            <el-card shadow="never">
              <template #header>
                <div style="font-weight: 700;">{{ text.riskSource }}</div>
              </template>
              <div v-if="selectedRisk.riskSourceTags?.length" style="display:flex; gap:8px; flex-wrap:wrap;">
                <el-tag
                  v-for="source in selectedRisk.riskSourceTags"
                  :key="source"
                  :type="getRiskSourceTagType(source)"
                  effect="plain"
                >
                  {{ getRiskSourceText(source) }}
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
                :revised="selectedRisk.revised"
                :summary-revised="selectedRisk.summaryRevised"
                :highlights-revised="selectedRisk.highlightsRevised"
                :risk-points-revised="selectedRisk.riskPointsRevised"
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
              <el-tag :type="getFollowUpStatusTagType(selectedRisk.followUpStatus)">
                {{ getFollowUpStatusText(selectedRisk.followUpStatus) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item :label="text.followUpTaskCount">
              {{ selectedRisk.followUpTaskCount ?? 0 }}
            </el-descriptions-item>
            <el-descriptions-item :label="text.latestFollowUpTask">
              <div v-if="selectedRisk.latestFollowUpTaskId">
                <div>{{ selectedRisk.latestFollowUpTaskTitle || selectedRisk.latestFollowUpTaskId }}</div>
                <div style="margin-top: 8px;">
                  <el-tag v-if="selectedRisk.latestFollowUpTaskStatus" :type="getTaskStatusTagType(selectedRisk.latestFollowUpTaskStatus)">
                    {{ getTaskStatusText(selectedRisk.latestFollowUpTaskStatus) }}
                  </el-tag>
                  <el-button
                    link
                    type="primary"
                    style="margin-left: 8px;"
                    @click="openLatestFollowUpTask(selectedRisk.latestFollowUpTaskId)"
                  >
                    {{ text.openLatestFollowUpTask }}
                  </el-button>
                </div>
              </div>
              <span v-else>-</span>
            </el-descriptions-item>
            <el-descriptions-item :label="text.latestFollowUpCreatedAt">
              {{ formatDateTime(selectedRisk.latestFollowUpCreatedAt) }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card shadow="never" style="margin-top: 16px;">
          <template #header>
            <div style="font-weight: 700;">{{ text.riskReasons }}</div>
          </template>
          <div v-if="selectedRisk.riskReasons?.length" style="display:flex; gap:8px; flex-wrap:wrap;">
            <el-tag
              v-for="reason in selectedRisk.riskReasons"
              :key="reason"
              type="danger"
              effect="plain"
            >
              {{ reason }}
            </el-tag>
          </div>
          <span v-else>-</span>
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
