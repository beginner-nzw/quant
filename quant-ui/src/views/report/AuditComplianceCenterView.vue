<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchAuditCompliance, fetchAuditComplianceStats } from '../../api/task'
import AuditComplianceStatsCards from '../../components/report/AuditComplianceStatsCards.vue'
import ReportVersionComparison from '../../components/report/ReportVersionComparison.vue'
import type { AuditComplianceListItem, AuditComplianceStats } from '../../types/task'
import {
  ANALYSIS_SCOPE,
  AUDIT_RESULT_STATUS,
  REPORT_REVIEW_STATUS,
  TASK_TYPE,
  type AuditResultStatus,
  type ReportReviewStatus
} from '../../types/taskEnums'
import { formatDateTime } from '../../utils/format'
import { resolveCenterActionAccess } from '../../utils/taskActionAccess'
import {
  getAuditActionText,
  getAuditResultTagType,
  getAuditResultText,
  getAuditStageText,
  getAuditTypeText,
  getBooleanTagType,
  getBooleanText,
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
  title: '审计与合规中心',
  taskId: '任务 ID',
  taskIdPlaceholder: '输入任务 ID',
  targetCode: '标的代码',
  targetCodePlaceholder: '如 01929',
  targetName: '标的名称',
  targetNamePlaceholder: '如 周大福',
  reviewStatus: '审核状态',
  reviewStatusPlaceholder: '全部状态',
  auditResultStatus: '审计结果',
  auditResultStatusPlaceholder: '全部结果',
  needHumanReview: '仅看需人工复核',
  onlyIntercepted: '仅看已拦截',
  search: '查询',
  reset: '重置',
  refresh: '刷新',
  empty: '暂无审计与合规数据',
  taskTitle: '任务标题',
  target: '标的',
  finalStatus: '结果状态',
  latestAuditResult: '最近审计结果',
  intercepted: '拦截状态',
  auditSummary: '审计概况',
  traceSummary: '链路留痕',
  reviewBy: '审核人',
  latestAuditAt: '最近审计时间',
  action: '操作',
  detail: '审计详情',
  versionDiff: '版本对比',
  workbench: '投研工作台',
  createTask: '发起研究',
  report: '查看报告',
  taskDetail: '任务详情',
  loadStatsFailed: '审计与合规统计加载失败',
  loadStatsError: '审计与合规统计加载异常',
  loadListFailed: '审计与合规列表加载失败',
  loadListError: '审计与合规列表加载异常',
  dialogTitle: '审计详情',
  versionDialogTitle: '报告版本对比',
  traceId: '链路追踪 ID',
  reportType: '报告类型',
  reviewComment: '审核意见',
  humanReview: '需人工复核',
  latestAudit: '最近审计动作',
  latestAuditRemark: '审计备注',
  workflow: '工作流',
  workflowStatus: '工作流状态',
  currentNode: '当前节点',
  auditCount: '审计记录数',
  failedAuditCount: '失败审计数',
  humanAuditCount: '人工审计数',
  agentAuditCount: 'Agent 审计数',
  agentExecutionCount: 'Agent 执行数',
  humanReviewAgentCount: '需人工复核节点数',
  inputLog: '输入留痕',
  outputLog: '输出留痕',
  decisionLog: '决策留痕',
  originalVersion: '原始版本',
  revisedVersion: '修订版本',
  summary: '摘要',
  highlights: '亮点',
  riskPoints: '风险点',
  none: '无',
  auditWord: '审计',
  failedWord: '失败',
  humanWord: '人工',
  agentWord: 'Agent',
  inputWord: '输入',
  outputWord: '输出',
  decisionWord: '决策',
  reviewNodeWord: '复核节点',
  interceptedYes: '已拦截',
  interceptedNo: '已放行'
} as const

const defaultStats: AuditComplianceStats = {
  totalCount: 0,
  pendingReviewCount: 0,
  interceptedCount: 0,
  revisedReportCount: 0,
  humanReviewCount: 0,
  decisionTraceCount: 0,
  promptAuditCount: 0
}

const loading = ref(false)
const stats = ref<AuditComplianceStats>({ ...defaultStats })
const detailDialogVisible = ref(false)
const versionDialogVisible = ref(false)
const selectedRecord = ref<AuditComplianceListItem | null>(null)

const pageData = reactive({
  total: 0,
  pageNum: 1,
  pageSize: 10,
  records: [] as AuditComplianceListItem[]
})

const query = reactive({
  taskId: '',
  targetCode: '',
  targetName: '',
  reviewStatus: '' as ReportReviewStatus | '',
  auditResultStatus: '' as AuditResultStatus | '',
  needHumanReview: false,
  onlyIntercepted: false
})

const reviewStatusOptions = Object.values(REPORT_REVIEW_STATUS).map((value) => ({
  label: getReviewStatusText(value),
  value
}))

const auditResultOptions = Object.values(AUDIT_RESULT_STATUS).map((value) => ({
  label: getAuditResultText(value),
  value
}))

const reviewStatusValueSet = new Set<string>(Object.values(REPORT_REVIEW_STATUS))
const auditResultValueSet = new Set<string>(Object.values(AUDIT_RESULT_STATUS))
const actionAccess = computed(() => resolveCenterActionAccess())

function syncQueryFromRoute() {
  query.taskId = getRouteQueryValue(route.query.taskId)
  query.targetCode = getRouteQueryValue(route.query.targetCode)
  query.targetName = getRouteQueryValue(route.query.targetName)
  query.reviewStatus = resolveReviewStatus(getRouteQueryValue(route.query.reviewStatus))
  query.auditResultStatus = resolveAuditResultStatus(getRouteQueryValue(route.query.auditResultStatus))
  query.needHumanReview = resolveBooleanQuery(route.query.needHumanReview)
  query.onlyIntercepted = resolveBooleanQuery(route.query.onlyIntercepted)
  pageData.pageNum = normalizePageValue(route.query.pageNum, 1)
  pageData.pageSize = normalizePageValue(route.query.pageSize, 10)
}

function displayText(value?: string | null) {
  return value && value.trim() ? value : text.none
}

function openDetail(record: AuditComplianceListItem) {
  selectedRecord.value = record
  detailDialogVisible.value = true
}

function openVersionDiff(record: AuditComplianceListItem) {
  selectedRecord.value = record
  versionDialogVisible.value = true
}

function canCompareVersion(record: AuditComplianceListItem) {
  return Boolean(
    record.revised
      || record.originalSummary
      || record.revisedSummary
      || record.originalHighlights.length
      || record.revisedHighlights.length
      || record.originalRiskPoints.length
      || record.revisedRiskPoints.length
  )
}

async function loadStats() {
  try {
    const res = await fetchAuditComplianceStats()
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

async function loadRecords() {
  loading.value = true
  try {
    const res = await fetchAuditCompliance({
      pageNum: pageData.pageNum,
      pageSize: pageData.pageSize,
      taskId: normalizeQueryValue(query.taskId),
      targetCode: normalizeQueryValue(query.targetCode),
      targetName: normalizeQueryValue(query.targetName),
      reviewStatus: query.reviewStatus || undefined,
      auditResultStatus: query.auditResultStatus || undefined,
      needHumanReview: query.needHumanReview || undefined,
      onlyIntercepted: query.onlyIntercepted || undefined
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
  await Promise.all([loadStats(), loadRecords()])
}

function buildRouteQuery() {
  return {
    taskId: normalizeQueryValue(query.taskId),
    targetCode: normalizeQueryValue(query.targetCode),
    targetName: normalizeQueryValue(query.targetName),
    reviewStatus: query.reviewStatus || undefined,
    auditResultStatus: query.auditResultStatus || undefined,
    needHumanReview: query.needHumanReview ? 'true' : undefined,
    onlyIntercepted: query.onlyIntercepted ? 'true' : undefined,
    pageNum: pageData.pageNum > 1 ? String(pageData.pageNum) : undefined,
    pageSize: pageData.pageSize !== 10 ? String(pageData.pageSize) : undefined
  }
}

function isSameRouteState(nextQuery: ReturnType<typeof buildRouteQuery>) {
  return getRouteQueryValue(route.query.taskId) === (nextQuery.taskId || '')
    && getRouteQueryValue(route.query.targetCode) === (nextQuery.targetCode || '')
    && getRouteQueryValue(route.query.targetName) === (nextQuery.targetName || '')
    && getRouteQueryValue(route.query.reviewStatus) === (nextQuery.reviewStatus || '')
    && getRouteQueryValue(route.query.auditResultStatus) === (nextQuery.auditResultStatus || '')
    && resolveBooleanQuery(route.query.needHumanReview) === Boolean(nextQuery.needHumanReview)
    && resolveBooleanQuery(route.query.onlyIntercepted) === Boolean(nextQuery.onlyIntercepted)
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
    path: '/audit-compliance',
    query: nextQuery
  })
}

function handleSearch() {
  pageData.pageNum = 1
  return navigateWithQuery()
}

function handleReset() {
  query.taskId = ''
  query.targetCode = ''
  query.targetName = ''
  query.reviewStatus = ''
  query.auditResultStatus = ''
  query.needHumanReview = false
  query.onlyIntercepted = false
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

function goReport(taskId: string) {
  router.push({
    path: `/tasks/${taskId}/report`,
    query: {
      from: route.fullPath
    }
  })
}

function goCreateTask(row: AuditComplianceListItem) {
  router.push({
    path: '/tasks/create',
      query: buildTaskCreateQuery({
        taskType: TASK_TYPE.AUDIT_REVIEW,
        taskTitle: buildFollowUpTaskTitle(row.targetName, row.targetCode, '审计复核研究'),
        targetCode: row.targetCode,
        targetName: row.targetName,
        priority: row.intercepted || row.needHumanReview || row.reviewStatus === REPORT_REVIEW_STATUS.PENDING ? 'HIGH' : row.priority,
        sourceTaskId: row.taskId,
        sourceReportId: row.reportId,
        sourceDomain: 'AUDIT_COMPLIANCE',
        sourceReviewStatus: row.reviewStatus,
        analysisScope: ANALYSIS_SCOPE.AUDIT_RECHECK,
        from: route.fullPath
      })
    })
  }

watch([
  () => route.query.taskId,
  () => route.query.targetCode,
  () => route.query.targetName,
  () => route.query.reviewStatus,
  () => route.query.auditResultStatus,
  () => route.query.needHumanReview,
  () => route.query.onlyIntercepted,
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

function resolveAuditResultStatus(value: string): AuditResultStatus | '' {
  return auditResultValueSet.has(value) ? value as AuditResultStatus : ''
}
</script>

<template>
  <div>
    <AuditComplianceStatsCards :stats="stats" />

    <el-card style="margin-top: 16px;">
      <template #header>
        <div style="font-weight: 700;">{{ text.title }}</div>
      </template>

      <el-form inline>
        <el-form-item :label="text.taskId">
          <el-input v-model="query.taskId" :placeholder="text.taskIdPlaceholder" clearable />
        </el-form-item>

        <el-form-item :label="text.targetCode">
          <el-input v-model="query.targetCode" :placeholder="text.targetCodePlaceholder" clearable />
        </el-form-item>

        <el-form-item :label="text.targetName">
          <el-input v-model="query.targetName" :placeholder="text.targetNamePlaceholder" clearable />
        </el-form-item>

        <el-form-item :label="text.reviewStatus">
          <el-select v-model="query.reviewStatus" :placeholder="text.reviewStatusPlaceholder" clearable style="width: 140px;">
            <el-option
              v-for="option in reviewStatusOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item :label="text.auditResultStatus">
          <el-select v-model="query.auditResultStatus" :placeholder="text.auditResultStatusPlaceholder" clearable style="width: 140px;">
            <el-option
              v-for="option in auditResultOptions"
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
          <el-checkbox v-model="query.onlyIntercepted">{{ text.onlyIntercepted }}</el-checkbox>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSearch">{{ text.search }}</el-button>
          <el-button @click="handleReset">{{ text.reset }}</el-button>
          <el-button @click="reloadAll">{{ text.refresh }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top: 16px;">
      <el-table :data="pageData.records" v-loading="loading" border :empty-text="text.empty">
        <el-table-column prop="taskId" :label="text.taskId" min-width="220" />
        <el-table-column prop="taskTitle" :label="text.taskTitle" min-width="180" />

        <el-table-column :label="text.target" min-width="150">
          <template #default="{ row }">
            <div>{{ row.targetName || '-' }}</div>
            <div style="color: #909399; font-size: 12px;">{{ row.targetCode || '-' }}</div>
          </template>
        </el-table-column>

        <el-table-column :label="text.finalStatus" width="110">
          <template #default="{ row }">
            <el-tag :type="getTaskStatusTagType(row.finalStatus)">
              {{ getTaskStatusText(row.finalStatus) }}
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

        <el-table-column :label="text.latestAuditResult" width="130">
          <template #default="{ row }">
            <el-tag :type="getAuditResultTagType(row.latestAuditResultStatus)">
              {{ getAuditResultText(row.latestAuditResultStatus) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column :label="text.intercepted" width="110">
          <template #default="{ row }">
            <el-tag :type="getBooleanTagType(row.intercepted, 'danger', 'success')">
              {{ getBooleanText(row.intercepted, text.interceptedYes, text.interceptedNo) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column :label="text.auditSummary" min-width="180">
          <template #default="{ row }">
            <div>{{ text.auditWord }} {{ row.auditCount }} / {{ text.failedWord }} {{ row.failedAuditCount }}</div>
            <div style="color: #909399; font-size: 12px;">
              {{ text.humanWord }} {{ row.humanAuditCount }} / {{ text.agentWord }} {{ row.agentAuditCount }}
            </div>
          </template>
        </el-table-column>

        <el-table-column :label="text.traceSummary" min-width="220">
          <template #default="{ row }">
            <div>{{ text.workflow }} {{ row.workflowCode || '-' }}</div>
            <div style="display: flex; gap: 4px; flex-wrap: wrap; margin-top: 4px;">
              <el-tag size="small" :type="getBooleanTagType(row.hasInputLog, 'success', 'info')">{{ text.inputWord }}</el-tag>
              <el-tag size="small" :type="getBooleanTagType(row.hasOutputLog, 'success', 'info')">{{ text.outputWord }}</el-tag>
              <el-tag size="small" :type="getBooleanTagType(row.hasDecisionLog, 'success', 'info')">{{ text.decisionWord }}</el-tag>
            </div>
            <div style="color: #909399; font-size: 12px; margin-top: 4px;">
              Agent {{ row.agentExecutionCount }} / {{ text.reviewNodeWord }} {{ row.humanReviewAgentCount }}
            </div>
          </template>
        </el-table-column>

        <el-table-column :label="text.reviewBy" width="120">
          <template #default="{ row }">
            {{ row.reviewedBy || '-' }}
          </template>
        </el-table-column>

        <el-table-column :label="text.latestAuditAt" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.latestAuditAt || row.reviewedAt || row.createdAt) }}
          </template>
        </el-table-column>

        <el-table-column :label="text.action" width="450" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">{{ text.detail }}</el-button>
            <el-button link type="warning" :disabled="!canCompareVersion(row)" @click="openVersionDiff(row)">{{ text.versionDiff }}</el-button>
            <el-button link type="warning" @click="goWorkbench(row.targetCode, row.targetName)">{{ text.workbench }}</el-button>
            <el-button v-if="actionAccess.showCreateTask" link type="info" @click="goCreateTask(row)">{{ text.createTask }}</el-button>
            <el-button link type="info" @click="goTaskDetail(row.taskId)">{{ text.taskDetail }}</el-button>
            <el-button link type="success" @click="goReport(row.taskId)">{{ text.report }}</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="display: flex; justify-content: flex-end; margin-top: 16px;">
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

    <el-dialog v-model="detailDialogVisible" :title="text.dialogTitle" width="920px">
      <el-descriptions class="audit-detail-descriptions" :column="2" border v-if="selectedRecord">
        <el-descriptions-item :label="text.taskId" :span="2">{{ displayText(selectedRecord.taskId) }}</el-descriptions-item>
        <el-descriptions-item :label="text.target" :span="2">{{ displayText(selectedRecord.targetName) }} / {{ displayText(selectedRecord.targetCode) }}</el-descriptions-item>
        <el-descriptions-item :label="text.traceId" :span="2">{{ displayText(selectedRecord.traceId) }}</el-descriptions-item>
        <el-descriptions-item :label="text.reportType">{{ displayText(selectedRecord.reportType) }}</el-descriptions-item>
        <el-descriptions-item :label="text.reviewBy">{{ displayText(selectedRecord.reviewedBy) }}</el-descriptions-item>
        <el-descriptions-item :label="text.reviewStatus">
          <el-tag :type="getReviewStatusTagType(selectedRecord.reviewStatus)">
            {{ getReviewStatusText(selectedRecord.reviewStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="text.latestAuditResult">
          <el-tag :type="getAuditResultTagType(selectedRecord.latestAuditResultStatus)">
            {{ getAuditResultText(selectedRecord.latestAuditResultStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="text.intercepted">
          <el-tag :type="getBooleanTagType(selectedRecord.intercepted, 'danger', 'success')">
            {{ getBooleanText(selectedRecord.intercepted, text.interceptedYes, text.interceptedNo) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="text.humanReview">
          <el-tag :type="getBooleanTagType(selectedRecord.needHumanReview, 'warning', 'info')">
            {{ getBooleanText(selectedRecord.needHumanReview) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="text.workflow" :span="2">{{ displayText(selectedRecord.workflowCode) }}@{{ displayText(selectedRecord.workflowVersion) }}</el-descriptions-item>
        <el-descriptions-item :label="text.workflowStatus">
          <el-tag :type="getTaskStatusTagType(selectedRecord.workflowStatus)">
            {{ getTaskStatusText(selectedRecord.workflowStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="text.currentNode">{{ displayText(selectedRecord.currentNode) }}</el-descriptions-item>
        <el-descriptions-item :label="text.latestAuditAt">{{ formatDateTime(selectedRecord.latestAuditAt) }}</el-descriptions-item>
        <el-descriptions-item :label="text.auditCount">{{ selectedRecord.auditCount }}</el-descriptions-item>
        <el-descriptions-item :label="text.failedAuditCount">{{ selectedRecord.failedAuditCount }}</el-descriptions-item>
        <el-descriptions-item :label="text.humanAuditCount">{{ selectedRecord.humanAuditCount }}</el-descriptions-item>
        <el-descriptions-item :label="text.agentAuditCount">{{ selectedRecord.agentAuditCount }}</el-descriptions-item>
        <el-descriptions-item :label="text.agentExecutionCount">{{ selectedRecord.agentExecutionCount }}</el-descriptions-item>
        <el-descriptions-item :label="text.humanReviewAgentCount">{{ selectedRecord.humanReviewAgentCount }}</el-descriptions-item>
        <el-descriptions-item :label="text.inputLog">
          <el-tag :type="getBooleanTagType(selectedRecord.hasInputLog, 'success', 'info')">
            {{ getBooleanText(selectedRecord.hasInputLog) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="text.outputLog">
          <el-tag :type="getBooleanTagType(selectedRecord.hasOutputLog, 'success', 'info')">
            {{ getBooleanText(selectedRecord.hasOutputLog) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="text.decisionLog">
          <el-tag :type="getBooleanTagType(selectedRecord.hasDecisionLog, 'success', 'info')">
            {{ getBooleanText(selectedRecord.hasDecisionLog) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="text.latestAudit">
          {{ getAuditTypeText(selectedRecord.latestAuditType) }} / {{ getAuditStageText(selectedRecord.latestAuditStage) }} / {{ getAuditActionText(selectedRecord.latestAuditActionCode) }}
        </el-descriptions-item>
        <el-descriptions-item :label="text.reviewComment" :span="2">{{ displayText(selectedRecord.reviewComment) }}</el-descriptions-item>
        <el-descriptions-item :label="text.latestAuditRemark" :span="2">{{ displayText(selectedRecord.latestAuditRemark) }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog v-model="versionDialogVisible" :title="text.versionDialogTitle" width="980px">
      <ReportVersionComparison
        v-if="selectedRecord"
        :original="{
          summary: selectedRecord.originalSummary,
          highlights: selectedRecord.originalHighlights,
          riskPoints: selectedRecord.originalRiskPoints
        }"
        :current="{
          summary: selectedRecord.revisedSummary,
          highlights: selectedRecord.revisedHighlights,
          riskPoints: selectedRecord.revisedRiskPoints
        }"
        :original-label="text.originalVersion"
        :current-label="text.revisedVersion"
        :empty-text="text.none"
      />
    </el-dialog>
  </div>
</template>

<style scoped>
:deep(.audit-detail-descriptions .el-descriptions__table) {
  width: 100%;
  table-layout: fixed;
}

:deep(.audit-detail-descriptions .el-descriptions__label) {
  width: 120px;
  white-space: nowrap;
}

:deep(.audit-detail-descriptions .el-descriptions__content) {
  word-break: break-all;
  overflow-wrap: anywhere;
}
</style>
