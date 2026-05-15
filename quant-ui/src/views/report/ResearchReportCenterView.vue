<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchReportCenter, fetchReportCenterStats } from '../../api/task'
import FilterDock from '../../components/common/FilterDock.vue'
import ReportRevisionStatusTags from '../../components/report/ReportRevisionStatusTags.vue'
import ResearchReportStatsCards from '../../components/report/ResearchReportStatsCards.vue'
import type { ReportCenterListItem, ReportCenterStats } from '../../types/task'
import { ANALYSIS_SCOPE, REPORT_REVIEW_STATUS, TASK_TYPE, type ReportReviewStatus } from '../../types/taskEnums'
import { formatDateTime } from '../../utils/format'
import { resolveCenterActionAccess } from '../../utils/taskActionAccess'
import {
  getHumanReviewTagType,
  getHumanReviewText,
  getPriorityTagType,
  getPriorityText,
  getReviewStatusTagType,
  getReviewStatusText,
  getTaskStatusTagType,
  getTaskStatusText
} from '../../utils/task'
import { buildResearchWorkbenchQuery } from '../../utils/researchWorkbench'
import { buildFollowUpTaskTitle, buildTaskCreateQuery } from '../../utils/taskCreate'

const route = useRoute()
const router = useRouter()

const text = {
  title: '研究报告中心',
  targetCode: '标的代码',
  targetCodePlaceholder: '如 01929',
  targetName: '标的名称',
  targetNamePlaceholder: '如 周大福',
  reportType: '报告类型',
  reportTypePlaceholder: '如 EQUITY_RESEARCH',
  reviewStatus: '审核状态',
  reviewStatusPlaceholder: '全部状态',
  onlyHighConfidence: '仅看高置信度',
  needHumanReview: '仅看需人工复核',
  search: '查询',
  reset: '重置',
  refresh: '刷新',
  empty: '暂无研究报告数据',
  taskId: '任务 ID',
  taskTitle: '任务标题',
  priority: '优先级',
  finalStatus: '报告结果',
  confidenceScore: '置信度',
  humanReview: '人工复核',
  revisionStatus: '修订情况',
  summary: '报告摘要',
  reviewedBy: '审核人',
  createdAt: '生成时间',
  action: '操作',
  detail: '任务详情',
  createTask: '发起研究',
  report: '查看报告',
  workbench: '投研工作台',
  loadStatsFailed: '研究报告统计加载失败',
  loadStatsError: '研究报告统计加载异常',
  loadListFailed: '研究报告列表加载失败',
  loadListError: '研究报告列表加载异常'
} as const

const defaultStats: ReportCenterStats = {
  totalCount: 0,
  highConfidenceCount: 0,
  pendingReviewCount: 0,
  approvedCount: 0,
  humanReviewCount: 0
}

const loading = ref(false)
const stats = ref<ReportCenterStats>({ ...defaultStats })

const pageData = reactive({
  total: 0,
  pageNum: 1,
  pageSize: 10,
  records: [] as ReportCenterListItem[]
})

const query = reactive({
  targetCode: '',
  targetName: '',
  reportType: '',
  reviewStatus: '' as ReportReviewStatus | '',
  onlyHighConfidence: false,
  needHumanReview: false
})

const reviewStatusOptions = Object.values(REPORT_REVIEW_STATUS).map((value) => ({
  label: getReviewStatusText(value),
  value
}))

const reviewStatusValueSet = new Set<string>(Object.values(REPORT_REVIEW_STATUS))
const actionAccess = computed(() => resolveCenterActionAccess())

function formatConfidence(value?: number) {
  if (value == null) {
    return '-'
  }
  return `${Math.round(value * 100)}%`
}

function syncQueryFromRoute() {
  query.targetCode = getRouteQueryValue(route.query.targetCode)
  query.targetName = getRouteQueryValue(route.query.targetName)
  query.reportType = getRouteQueryValue(route.query.reportType)
  query.reviewStatus = resolveReviewStatus(getRouteQueryValue(route.query.reviewStatus))
  query.onlyHighConfidence = resolveBooleanQuery(route.query.onlyHighConfidence)
  query.needHumanReview = resolveBooleanQuery(route.query.needHumanReview)
  pageData.pageNum = normalizePageValue(route.query.pageNum, 1)
  pageData.pageSize = normalizePageValue(route.query.pageSize, 10)
}

async function loadStats() {
  try {
    const res = await fetchReportCenterStats()
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

async function loadReports() {
  loading.value = true
  try {
    const res = await fetchReportCenter({
      pageNum: pageData.pageNum,
      pageSize: pageData.pageSize,
      targetCode: normalizeQueryValue(query.targetCode),
      targetName: normalizeQueryValue(query.targetName),
      reportType: normalizeQueryValue(query.reportType),
      reviewStatus: query.reviewStatus || undefined,
      onlyHighConfidence: query.onlyHighConfidence || undefined,
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
  await Promise.all([loadStats(), loadReports()])
}

function buildRouteQuery() {
  return {
    targetCode: normalizeQueryValue(query.targetCode),
    targetName: normalizeQueryValue(query.targetName),
    reportType: normalizeQueryValue(query.reportType),
    reviewStatus: query.reviewStatus || undefined,
    onlyHighConfidence: query.onlyHighConfidence ? 'true' : undefined,
    needHumanReview: query.needHumanReview ? 'true' : undefined,
    pageNum: pageData.pageNum > 1 ? String(pageData.pageNum) : undefined,
    pageSize: pageData.pageSize !== 10 ? String(pageData.pageSize) : undefined
  }
}

function isSameRouteState(nextQuery: ReturnType<typeof buildRouteQuery>) {
  return getRouteQueryValue(route.query.targetCode) === (nextQuery.targetCode || '')
    && getRouteQueryValue(route.query.targetName) === (nextQuery.targetName || '')
    && getRouteQueryValue(route.query.reportType) === (nextQuery.reportType || '')
    && getRouteQueryValue(route.query.reviewStatus) === (nextQuery.reviewStatus || '')
    && resolveBooleanQuery(route.query.onlyHighConfidence) === Boolean(nextQuery.onlyHighConfidence)
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
    path: '/reports/center',
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
  query.reportType = ''
  query.reviewStatus = ''
  query.onlyHighConfidence = false
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

function goCreateTask(row: ReportCenterListItem) {
  router.push({
    path: '/tasks/create',
      query: buildTaskCreateQuery({
        taskType: row.needHumanReview || row.reviewStatus === REPORT_REVIEW_STATUS.PENDING
          ? TASK_TYPE.REPORT_REVIEW
          : TASK_TYPE.FOLLOW_UP_RESEARCH,
        taskTitle: buildFollowUpTaskTitle(
          row.targetName,
          row.targetCode,
          row.needHumanReview || row.reviewStatus === REPORT_REVIEW_STATUS.PENDING
            ? '报告复核研究'
            : '报告跟踪研究'
        ),
        targetCode: row.targetCode,
        targetName: row.targetName,
        priority: row.needHumanReview || row.reviewStatus === REPORT_REVIEW_STATUS.PENDING ? 'HIGH' : row.priority,
        sourceTaskId: row.taskId,
        sourceReportId: row.reportId,
        sourceDomain: 'REPORT_CENTER',
        sourceReviewStatus: row.reviewStatus,
        analysisScope: row.needHumanReview || row.reviewStatus === REPORT_REVIEW_STATUS.PENDING
          ? ANALYSIS_SCOPE.REPORT_REVIEW_RECHECK
          : ANALYSIS_SCOPE.REPORT_FOLLOW_UP,
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

watch([
  () => route.query.targetCode,
  () => route.query.targetName,
  () => route.query.reportType,
  () => route.query.reviewStatus,
  () => route.query.onlyHighConfidence,
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

function resolveReviewStatus(value: string): ReportReviewStatus | '' {
  return reviewStatusValueSet.has(value) ? value as ReportReviewStatus : ''
}
</script>

<template>
  <div class="center-workspace">
    <ResearchReportStatsCards :stats="stats" />

    <FilterDock :title="text.title" description="按标的、报告类型和审核状态筛选报告。">
      <el-form inline>
        <el-form-item :label="text.targetCode">
          <el-input v-model="query.targetCode" :placeholder="text.targetCodePlaceholder" clearable />
        </el-form-item>

        <el-form-item :label="text.targetName">
          <el-input v-model="query.targetName" :placeholder="text.targetNamePlaceholder" clearable />
        </el-form-item>

        <el-form-item :label="text.reportType">
          <el-input v-model="query.reportType" :placeholder="text.reportTypePlaceholder" clearable />
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
          <el-checkbox v-model="query.onlyHighConfidence">{{ text.onlyHighConfidence }}</el-checkbox>
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
        <el-table-column prop="taskId" :label="text.taskId" min-width="220" />
        <el-table-column prop="taskTitle" :label="text.taskTitle" min-width="180" />
        <el-table-column prop="targetCode" :label="text.targetCode" width="100" />
        <el-table-column prop="targetName" :label="text.targetName" width="120" />

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

        <el-table-column :label="text.finalStatus" width="110">
          <template #default="{ row }">
            <el-tag :type="getTaskStatusTagType(row.finalStatus)">
              {{ getTaskStatusText(row.finalStatus) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column :label="text.confidenceScore" width="100">
          <template #default="{ row }">
            {{ formatConfidence(row.confidenceScore) }}
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

        <el-table-column :label="text.reviewedBy" width="120">
          <template #default="{ row }">
            {{ row.reviewedBy || '-' }}
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

        <el-table-column :label="text.action" width="320" fixed="right">
          <template #default="{ row }">
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
