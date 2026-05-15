<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { fetchTaskStats, fetchTasks } from '../../api/task'
import DataEmptyState from '../../components/common/DataEmptyState.vue'
import FilterDock from '../../components/common/FilterDock.vue'
import TaskStatsCards from '../../components/task/TaskStatsCards.vue'
import type { TaskListItem, TaskStats } from '../../types/task'
import { formatDateTime } from '../../utils/format'
import {
  getPriorityTagType,
  getPriorityText,
  getReviewStatusTagType,
  getReviewStatusText,
  getTaskStageText,
  getTaskStatusTagType,
  getTaskStatusText
} from '../../utils/task'
import { canCreateTasks } from '../../utils/roleAccess'
import { resolveTaskListActionAccess } from '../../utils/taskActionAccess'
import { executeTaskCancel, executeTaskRetry } from '../../utils/taskActions'
import {
  REPORT_REVIEW_STATUS,
  TASK_STATUS,
  TASK_STATUS_FILTER_OPTIONS,
  isPendingReviewStatus,
  isTaskActiveStatus,
  isTaskFailedStatus,
  isTaskSuccessStatus,
  type TaskStatus
} from '../../types/taskEnums'
import { buildFromQuery } from '../../utils/taskNavigation'

type QueueTone = 'blue' | 'green' | 'red' | 'amber' | 'ink'

interface QueueItem {
  key: string
  title: string
  count: number
  description: string
  status?: TaskStatus
  onlyFailed?: boolean
  onlyPendingReview?: boolean
  tone: QueueTone
}

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const retryLoading = ref(false)
const autoRefresh = ref(false)

let timer: number | null = null

const text = {
  loadListFailed: '任务列表加载失败',
  loadListError: '任务列表加载异常',
  taskCancelled: '任务已取消',
  cancelButton: '取消',
  taskFlow: 'Agent 任务流',
  taskFlowIntro: '统一查看研究任务的排队、执行、失败、报告审核和补偿动作。',
  queueTitle: '处理队列',
  queueDescription: '按当前最需要处理的状态进入任务列表。',
  focusTitle: '重点任务',
  focusDescription: '优先展示运行中、失败和待审核任务。',
  listTitle: '任务清单',
  listDescription: '保留完整表格能力，用于精确查询和批量排查。',
  currentPagePending: '当前页待审',
  failureRate: '失败率',
  recentTasks: '最近任务',
  taskType: '任务类型',
  taskTypePlaceholder: '如 STOCK_RESEARCH',
  status: '状态',
  statusPlaceholder: '请选择',
  targetCode: '标的代码',
  targetCodePlaceholder: '如 01929',
  targetName: '标的名称',
  targetNamePlaceholder: '如 周大福',
  onlyPendingReview: '仅待审核报告',
  onlyFailed: '仅失败任务',
  autoRefresh: '自动刷新',
  manualRefresh: '手动',
  search: '查询',
  reset: '重置',
  refresh: '刷新',
  createTask: '新建任务',
  tableEmpty: '暂无任务数据',
  taskId: '任务 ID',
  taskTitle: '任务标题',
  priority: '优先级',
  reportReviewStatus: '报告审核',
  currentStage: '当前阶段',
  retryCount: '重试次数',
  errorMessage: '失败原因',
  createdAt: '创建时间',
  action: '操作',
  report: '报告',
  detail: '详情',
  retry: '重试',
  toReview: '去审核',
  retryReason: '前端手工重试',
  cancelReason: '前端手工取消'
} as const

const defaultStats: TaskStats = {
  totalCount: 0,
  runningCount: 0,
  successCount: 0,
  failedCount: 0,
  retriedCount: 0
}

const stats = ref<TaskStats>({ ...defaultStats })

const pageData = reactive({
  total: 0,
  pageNum: 1,
  pageSize: 10,
  records: [] as TaskListItem[]
})

const query = reactive({
  taskType: '',
  status: '' as TaskStatus | '',
  targetCode: '',
  targetName: '',
  onlyFailed: false,
  onlyPendingReview: false
})

const taskStatusOptions = TASK_STATUS_FILTER_OPTIONS.map((status) => ({
  label: getTaskStatusText(status),
  value: status
}))

const taskStatusValueSet = new Set<string>(TASK_STATUS_FILTER_OPTIONS)
const recentTasks = computed(() => pageData.records.slice(0, 4))

const pendingReviewCount = computed(() => {
  return pageData.records.filter((item) => {
    return isTaskSuccessStatus(item.status) && isPendingReviewStatus(item.reportReviewStatus)
  }).length
})

const failedRate = computed(() => {
  if (!stats.value.totalCount) {
    return '0%'
  }
  return `${Math.round((stats.value.failedCount / stats.value.totalCount) * 100)}%`
})

const focusTasks = computed(() => {
  const prioritized = pageData.records.filter((item) => {
    return isTaskActiveStatus(item.status)
      || isTaskFailedStatus(item.status)
      || (isTaskSuccessStatus(item.status) && isPendingReviewStatus(item.reportReviewStatus))
  })
  return (prioritized.length ? prioritized : pageData.records).slice(0, 4)
})

const activeQueueKey = computed(() => {
  if (query.onlyPendingReview) {
    return 'pending-review'
  }
  if (query.onlyFailed) {
    return 'failed'
  }
  if (query.status === TASK_STATUS.RUNNING) {
    return 'running'
  }
  if (query.status === TASK_STATUS.SUCCESS) {
    return 'success'
  }
  return 'all'
})

const queueItems = computed<QueueItem[]>(() => [
  {
    key: 'all',
    title: '全部任务',
    count: stats.value.totalCount,
    description: '查看所有研究任务和报告产出状态。',
    tone: 'ink'
  },
  {
    key: 'running',
    title: '执行中',
    count: stats.value.runningCount,
    description: 'Agent 正在执行或等待调度的任务。',
    status: TASK_STATUS.RUNNING,
    tone: 'blue'
  },
  {
    key: 'failed',
    title: '失败待处理',
    count: stats.value.failedCount,
    description: '需要重试、取消或人工定位的问题任务。',
    status: TASK_STATUS.FAILED,
    onlyFailed: true,
    tone: 'red'
  },
  {
    key: 'pending-review',
    title: '报告待审',
    count: pendingReviewCount.value,
    description: '当前页成功完成但报告仍待审核的任务。',
    onlyPendingReview: true,
    tone: 'amber'
  },
  {
    key: 'success',
    title: '已交付',
    count: stats.value.successCount,
    description: '已产出报告或完成执行闭环的任务。',
    status: TASK_STATUS.SUCCESS,
    tone: 'green'
  }
])

function getDisplayTaskStageText(stage?: string) {
  if (stage === 'EVIDENCE_COLLECTION') {
    return '证据采集'
  }
  return getTaskStageText(stage)
}

function syncQueryFromRoute() {
  query.taskType = getRouteQueryValue(route.query.taskType)
  query.status = resolveTaskStatus(getRouteQueryValue(route.query.status))
  query.targetCode = getRouteQueryValue(route.query.targetCode)
  query.targetName = getRouteQueryValue(route.query.targetName)
  query.onlyFailed = resolveBooleanQuery(route.query.onlyFailed)
  query.onlyPendingReview = resolveBooleanQuery(route.query.onlyPendingReview)
  pageData.pageNum = normalizePageValue(route.query.pageNum, 1)
  pageData.pageSize = normalizePageValue(route.query.pageSize, 10)
}

async function loadStats() {
  try {
    const res = await fetchTaskStats()
    if (res.success) {
      stats.value = res.data
    } else {
      stats.value = { ...defaultStats }
      ElMessage.error(res.message || text.loadListFailed)
    }
  } catch (e: any) {
    stats.value = { ...defaultStats }
    ElMessage.error(e?.message || text.loadListError)
  }
}

async function loadTasks() {
  loading.value = true
  try {
    const res = await fetchTasks({
      pageNum: pageData.pageNum,
      pageSize: pageData.pageSize,
      taskType: normalizeQueryValue(query.taskType),
      status: query.status || undefined,
      targetCode: normalizeQueryValue(query.targetCode),
      targetName: normalizeQueryValue(query.targetName),
      onlyFailed: query.onlyFailed || undefined,
      onlyPendingReview: query.onlyPendingReview || undefined
    })
    if (res.success) {
      pageData.total = res.data.total
      pageData.records = res.data.records
    } else {
      pageData.total = 0
      pageData.records = []
      ElMessage.error(res.message || text.loadListFailed)
    }
  } catch (e: any) {
    pageData.total = 0
    pageData.records = []
    ElMessage.error(e?.message || text.loadListError)
  } finally {
    loading.value = false
  }
}

async function reloadAll() {
  await Promise.all([loadStats(), loadTasks()])
}

function getRowClassName({ row }: { row: TaskListItem }) {
  return isTaskFailedStatus(row.status) ? 'task-row-failed' : ''
}

function getTaskCardClass(row: TaskListItem) {
  if (isTaskFailedStatus(row.status)) {
    return 'failed'
  }
  if (isTaskActiveStatus(row.status)) {
    return 'active'
  }
  if (isTaskSuccessStatus(row.status)) {
    return 'success'
  }
  return 'idle'
}

function buildRouteQuery() {
  return {
    taskType: normalizeQueryValue(query.taskType),
    status: query.status || undefined,
    targetCode: normalizeQueryValue(query.targetCode),
    targetName: normalizeQueryValue(query.targetName),
    onlyFailed: query.onlyFailed ? 'true' : undefined,
    onlyPendingReview: query.onlyPendingReview ? 'true' : undefined,
    pageNum: pageData.pageNum > 1 ? String(pageData.pageNum) : undefined,
    pageSize: pageData.pageSize !== 10 ? String(pageData.pageSize) : undefined
  }
}

function isSameRouteState(nextQuery: ReturnType<typeof buildRouteQuery>) {
  return getRouteQueryValue(route.query.taskType) === (nextQuery.taskType || '')
    && getRouteQueryValue(route.query.status) === (nextQuery.status || '')
    && getRouteQueryValue(route.query.targetCode) === (nextQuery.targetCode || '')
    && getRouteQueryValue(route.query.targetName) === (nextQuery.targetName || '')
    && resolveBooleanQuery(route.query.onlyFailed) === Boolean(nextQuery.onlyFailed)
    && resolveBooleanQuery(route.query.onlyPendingReview) === Boolean(nextQuery.onlyPendingReview)
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
    path: '/tasks',
    query: nextQuery
  })
}

function applyQuickQueue(item: QueueItem) {
  query.status = item.status || ''
  query.onlyFailed = Boolean(item.onlyFailed)
  query.onlyPendingReview = Boolean(item.onlyPendingReview)
  pageData.pageNum = 1
  return navigateWithQuery()
}

function handleSearch() {
  pageData.pageNum = 1
  return navigateWithQuery()
}

function handleReset() {
  query.taskType = ''
  query.status = ''
  query.targetCode = ''
  query.targetName = ''
  query.onlyFailed = false
  query.onlyPendingReview = false
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

function goDetail(taskId: string) {
  router.push({
    path: `/tasks/${taskId}`,
    query: buildFromQuery(route.fullPath)
  })
}

function goReport(taskId: string) {
  router.push({
    path: `/tasks/${taskId}/report`,
    query: buildFromQuery(route.fullPath)
  })
}

async function handleRetry(taskId: string) {
  retryLoading.value = true
  try {
    const success = await executeTaskRetry(taskId, text.retryReason)
    if (success) {
      await reloadAll()
    }
  } finally {
    retryLoading.value = false
  }
}

async function handleCancel(taskId: string) {
  const success = await executeTaskCancel(taskId, text.cancelReason)
  if (success) {
    await reloadAll()
  }
}

function handleAutoRefreshChange(value: boolean) {
  if (timer) {
    window.clearInterval(timer)
    timer = null
  }
  if (value) {
    timer = window.setInterval(() => {
      reloadAll()
    }, 10000)
  }
}

function getRowActionAccess(row: TaskListItem) {
  return resolveTaskListActionAccess(row)
}

watch([
  () => route.query.taskType,
  () => route.query.status,
  () => route.query.targetCode,
  () => route.query.targetName,
  () => route.query.onlyFailed,
  () => route.query.onlyPendingReview,
  () => route.query.pageNum,
  () => route.query.pageSize
], async () => {
  syncQueryFromRoute()
  await reloadAll()
}, { immediate: true })

onBeforeUnmount(() => {
  if (timer) {
    window.clearInterval(timer)
    timer = null
  }
})

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

function resolveTaskStatus(value: string): TaskStatus | '' {
  return taskStatusValueSet.has(value) ? value as TaskStatus : ''
}
</script>

<template>
  <div class="task-board-page">
    <section class="task-hero">
      <div class="hero-copy">
        <span>Task Flow</span>
        <h2>{{ text.taskFlow }}</h2>
        <p>{{ text.taskFlowIntro }}</p>
      </div>

      <div class="hero-status">
        <div>
          <span>{{ text.currentPagePending }}</span>
          <strong>{{ pendingReviewCount }}</strong>
        </div>
        <div>
          <span>{{ text.failureRate }}</span>
          <strong>{{ failedRate }}</strong>
        </div>
      </div>

      <div class="hero-actions">
        <el-button type="primary" @click="reloadAll">{{ text.refresh }}</el-button>
        <el-button
          v-if="canCreateTasks()"
          type="success"
          @click="router.push('/tasks/create')"
        >
          {{ text.createTask }}
        </el-button>
        <el-switch
          v-model="autoRefresh"
          inline-prompt
          :active-text="text.autoRefresh"
          :inactive-text="text.manualRefresh"
          @change="handleAutoRefreshChange"
        />
      </div>
    </section>

    <TaskStatsCards :stats="stats" />

    <section class="task-workspace">
      <aside class="queue-panel">
        <div class="panel-heading">
          <span>Queues</span>
          <strong>{{ text.queueTitle }}</strong>
          <p>{{ text.queueDescription }}</p>
        </div>

        <div class="queue-list">
          <button
            v-for="item in queueItems"
            :key="item.key"
            type="button"
            class="queue-card"
            :class="[`tone-${item.tone}`, { active: activeQueueKey === item.key }]"
            @click="applyQuickQueue(item)"
          >
            <span>{{ item.title }}</span>
            <strong>{{ item.count }}</strong>
            <small>{{ item.description }}</small>
          </button>
        </div>

        <div class="focus-panel">
          <div class="panel-heading compact">
            <span>Focus</span>
            <strong>{{ text.focusTitle }}</strong>
            <p>{{ text.focusDescription }}</p>
          </div>

          <div v-if="focusTasks.length" class="focus-list">
            <button
              v-for="item in focusTasks"
              :key="item.taskId"
              type="button"
              class="focus-task"
              :class="getTaskCardClass(item)"
              @click="goDetail(item.taskId)"
            >
              <span>{{ item.targetName || item.targetCode }}</span>
              <strong>{{ item.taskTitle || item.taskId }}</strong>
              <small>{{ getTaskStatusText(item.status) }} · {{ getDisplayTaskStageText(item.currentStage) }}</small>
            </button>
          </div>

          <DataEmptyState
            v-else
            title="暂无重点任务"
            description="当前筛选条件下没有运行、失败或待审核任务。"
          />
        </div>
      </aside>

      <section class="list-panel">
        <FilterDock
          :title="text.listTitle"
          :description="text.listDescription"
        >
          <el-form inline @keyup.enter="handleSearch">
            <el-form-item :label="text.taskType">
              <el-input v-model="query.taskType" :placeholder="text.taskTypePlaceholder" clearable />
            </el-form-item>

            <el-form-item :label="text.status">
              <el-select v-model="query.status" :placeholder="text.statusPlaceholder" clearable class="status-select">
                <el-option
                  v-for="status in taskStatusOptions"
                  :key="status.value"
                  :label="status.label"
                  :value="status.value"
                />
              </el-select>
            </el-form-item>

            <el-form-item :label="text.targetCode">
              <el-input v-model="query.targetCode" :placeholder="text.targetCodePlaceholder" clearable />
            </el-form-item>

            <el-form-item :label="text.targetName">
              <el-input v-model="query.targetName" :placeholder="text.targetNamePlaceholder" clearable />
            </el-form-item>

            <el-form-item>
              <el-checkbox v-model="query.onlyPendingReview">{{ text.onlyPendingReview }}</el-checkbox>
            </el-form-item>

            <el-form-item>
              <el-checkbox v-model="query.onlyFailed">{{ text.onlyFailed }}</el-checkbox>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="handleSearch">{{ text.search }}</el-button>
              <el-button @click="handleReset">{{ text.reset }}</el-button>
            </el-form-item>
          </el-form>
        </FilterDock>

        <section class="recent-strip" v-if="recentTasks.length">
          <div class="recent-title">{{ text.recentTasks }}</div>
          <button
            v-for="item in recentTasks"
            :key="item.taskId"
            type="button"
            @click="goDetail(item.taskId)"
          >
            <span>{{ item.targetName || item.targetCode }}</span>
            <strong>{{ item.taskTitle || item.taskId }}</strong>
          </button>
        </section>

        <section class="table-shell" v-loading="loading">
          <div class="table-heading">
            <div>
              <span>Task Ledger</span>
              <strong>{{ pageData.total }} 条任务记录</strong>
            </div>
            <el-button @click="reloadAll">{{ text.refresh }}</el-button>
          </div>

          <DataEmptyState
            v-if="!pageData.records.length && !loading"
            :title="text.tableEmpty"
            description="可以调整筛选条件，或从事件、情报、标的工作台发起新的研究任务。"
            :action-label="canCreateTasks() ? text.createTask : ''"
            @action="router.push('/tasks/create')"
          />

          <template v-else>
            <el-table
              :data="pageData.records"
              border
              :empty-text="text.tableEmpty"
              :row-class-name="getRowClassName"
            >
              <el-table-column prop="taskId" :label="text.taskId" min-width="240" />
              <el-table-column prop="taskTitle" :label="text.taskTitle" min-width="180" />
              <el-table-column prop="targetCode" :label="text.targetCode" width="100" />
              <el-table-column prop="targetName" :label="text.targetName" width="120" />

              <el-table-column :label="text.priority" width="100">
                <template #default="{ row }">
                  <el-tag :type="getPriorityTagType(row.priority)">
                    {{ getPriorityText(row.priority) }}
                  </el-tag>
                </template>
              </el-table-column>

              <el-table-column :label="text.status" width="110">
                <template #default="{ row }">
                  <el-tag :type="getTaskStatusTagType(row.status)">
                    {{ getTaskStatusText(row.status) }}
                  </el-tag>
                </template>
              </el-table-column>

              <el-table-column :label="text.reportReviewStatus" width="120">
                <template #default="{ row }">
                  <template v-if="isTaskSuccessStatus(row.status)">
                    <el-tag :type="getReviewStatusTagType(row.reportReviewStatus)">
                      {{ getReviewStatusText(row.reportReviewStatus || REPORT_REVIEW_STATUS.PENDING) }}
                    </el-tag>
                  </template>
                  <span v-else>-</span>
                </template>
              </el-table-column>

              <el-table-column :label="text.currentStage" width="160">
                <template #default="{ row }">
                  {{ getDisplayTaskStageText(row.currentStage) }}
                </template>
              </el-table-column>

              <el-table-column prop="retryCount" :label="text.retryCount" width="100" />

              <el-table-column :label="text.errorMessage" min-width="220">
                <template #default="{ row }">
                  <el-tooltip
                    v-if="row.errorMessage"
                    :content="row.errorMessage"
                    placement="top"
                  >
                    <span class="error-text">
                      {{ row.errorMessage.length > 20 ? row.errorMessage.slice(0, 20) + '...' : row.errorMessage }}
                    </span>
                  </el-tooltip>
                  <span v-else>-</span>
                </template>
              </el-table-column>

              <el-table-column :label="text.createdAt" width="180">
                <template #default="{ row }">
                  {{ formatDateTime(row.createdAt) }}
                </template>
              </el-table-column>

              <el-table-column :label="text.action" width="180" fixed="right">
                <template #default="{ row }">
                  <el-button
                    v-if="getRowActionAccess(row).showReport"
                    link
                    type="success"
                    @click="goReport(row.taskId)"
                  >
                    {{ text.report }}
                  </el-button>

                  <el-button link type="primary" @click="goDetail(row.taskId)">{{ text.detail }}</el-button>

                  <el-button
                    v-if="getRowActionAccess(row).showRetry"
                    link
                    type="danger"
                    :loading="retryLoading"
                    @click="handleRetry(row.taskId)"
                  >
                    {{ text.retry }}
                  </el-button>

                  <el-button
                    v-if="getRowActionAccess(row).showCancel"
                    link
                    type="warning"
                    @click="handleCancel(row.taskId)"
                  >
                    {{ text.cancelButton }}
                  </el-button>

                  <el-button
                    v-if="getRowActionAccess(row).showReview"
                    link
                    type="warning"
                    @click="goReport(row.taskId)"
                  >
                    {{ text.toReview }}
                  </el-button>
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
          </template>
        </section>
      </section>
    </section>
  </div>
</template>

<style scoped>
.task-board-page {
  display: grid;
  gap: 18px;
}

.task-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 18px;
  padding: 24px;
  border-radius: var(--qa-radius-lg);
  background: linear-gradient(135deg, #142033, #1d3c5c);
  color: #fff;
  box-shadow: 0 22px 70px rgba(20, 32, 51, 0.18);
}

.hero-copy span,
.panel-heading span,
.table-heading span {
  display: block;
  color: #69d2c4;
  font-size: 11px;
  font-weight: 950;
  letter-spacing: 0;
  text-transform: uppercase;
}

.hero-copy h2 {
  margin: 10px 0 0;
  font-size: 46px;
  font-weight: 950;
  letter-spacing: 0;
  line-height: 0.96;
}

.hero-copy p {
  max-width: 640px;
  margin: 14px 0 0;
  color: rgba(255, 255, 255, 0.68);
  line-height: 1.8;
}

.hero-status {
  display: grid;
  grid-template-columns: repeat(2, minmax(110px, 1fr));
  gap: 10px;
}

.hero-status div {
  padding: 16px;
  border-radius: var(--qa-radius-lg);
  background: rgba(255, 255, 255, 0.12);
}

.hero-status span,
.hero-status strong {
  display: block;
}

.hero-status span {
  color: rgba(255, 255, 255, 0.62);
  font-size: 12px;
  font-weight: 900;
}

.hero-status strong {
  margin-top: 6px;
  font-size: 30px;
  font-weight: 950;
  letter-spacing: 0;
}

.hero-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
}

.task-workspace {
  display: grid;
  grid-template-columns: minmax(280px, 0.34fr) minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.queue-panel,
.list-panel {
  min-width: 0;
}

.queue-panel {
  display: grid;
  gap: 14px;
}

.panel-heading {
  padding: 18px;
  border-radius: var(--qa-radius-lg);
  background: rgba(255, 255, 255, 0.68);
  box-shadow: inset 0 0 0 1px rgba(20, 32, 51, 0.08);
}

.panel-heading strong,
.table-heading strong {
  display: block;
  margin-top: 8px;
  color: #142033;
  font-size: 22px;
  font-weight: 950;
  letter-spacing: 0;
}

.panel-heading p {
  margin: 8px 0 0;
  color: #6b7a8d;
  line-height: 1.7;
}

.panel-heading.compact {
  padding: 0;
  background: transparent;
  box-shadow: none;
}

.queue-list,
.focus-list {
  display: grid;
  gap: 10px;
}

.queue-card,
.focus-task,
.recent-strip button {
  border: 0;
  font: inherit;
  cursor: pointer;
}

.queue-card {
  position: relative;
  overflow: hidden;
  min-height: 138px;
  padding: 18px;
  border-radius: var(--qa-radius-lg);
  background: rgba(255, 255, 255, 0.72);
  color: #142033;
  text-align: left;
  border-left: 4px solid currentColor;
  box-shadow: inset 0 0 0 1px rgba(20, 32, 51, 0.08);
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.queue-card:hover,
.queue-card.active {
  transform: translateY(-2px);
  box-shadow: inset 0 0 0 1px currentColor, 0 18px 46px rgba(32, 52, 80, 0.12);
}

.queue-card span,
.queue-card strong,
.queue-card small {
  position: relative;
  z-index: 1;
  display: block;
}

.queue-card span {
  color: #687789;
  font-size: 13px;
  font-weight: 900;
}

.queue-card strong {
  margin-top: 14px;
  font-size: 42px;
  font-weight: 950;
  letter-spacing: 0;
}

.queue-card small {
  margin-top: 8px;
  color: #748396;
  line-height: 1.6;
}

.tone-blue {
  color: #2653b1;
}

.tone-green {
  color: #158c73;
}

.tone-red {
  color: #cf3b38;
}

.tone-amber {
  color: #b26b11;
}

.tone-ink {
  color: #142033;
}

.focus-panel,
.table-shell {
  padding: 18px;
  border: 1px solid rgba(20, 32, 51, 0.08);
  border-radius: var(--qa-radius-lg);
  background: rgba(255, 255, 255, 0.68);
  box-shadow: 0 18px 46px rgba(32, 52, 80, 0.08);
}

.focus-task {
  position: relative;
  overflow: hidden;
  padding: 16px;
  border-radius: var(--qa-radius-lg);
  background: #fff;
  color: #142033;
  text-align: left;
  box-shadow: inset 0 0 0 1px rgba(20, 32, 51, 0.08);
}

.focus-task::before {
  content: '';
  position: absolute;
  inset: 0 auto 0 0;
  width: 5px;
  background: #8a97a8;
}

.focus-task.active::before {
  background: #2653b1;
}

.focus-task.failed::before {
  background: #cf3b38;
}

.focus-task.success::before {
  background: #158c73;
}

.focus-task span,
.focus-task strong,
.focus-task small {
  display: block;
}

.focus-task span {
  color: #687789;
  font-size: 12px;
  font-weight: 900;
}

.focus-task strong {
  margin-top: 6px;
  overflow: hidden;
  font-weight: 950;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.focus-task small {
  margin-top: 8px;
  color: #748396;
}

.list-panel {
  display: grid;
  gap: 14px;
}

.status-select {
  width: 140px;
}

.recent-strip {
  display: grid;
  grid-template-columns: auto repeat(4, minmax(0, 1fr));
  align-items: stretch;
  gap: 10px;
}

.recent-title,
.recent-strip button {
  padding: 14px;
  border-radius: var(--qa-radius-lg);
  background: rgba(255, 255, 255, 0.7);
  box-shadow: inset 0 0 0 1px rgba(20, 32, 51, 0.08);
}

.recent-title {
  display: grid;
  place-items: center;
  color: #526276;
  font-weight: 950;
}

.recent-strip button {
  min-width: 0;
  color: #142033;
  text-align: left;
}

.recent-strip span,
.recent-strip strong {
  display: block;
}

.recent-strip span {
  color: #748396;
  font-size: 12px;
  font-weight: 900;
}

.recent-strip strong {
  margin-top: 6px;
  overflow: hidden;
  font-size: 13px;
  font-weight: 950;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.table-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.table-heading span {
  color: #527086;
}

.error-text {
  color: #cf3b38;
  cursor: pointer;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

:deep(.task-row-failed) {
  --el-table-tr-bg-color: #fff2f0;
}

@media (max-width: 1280px) {
  .task-hero,
  .task-workspace {
    grid-template-columns: 1fr;
  }

  .hero-actions {
    justify-content: flex-start;
  }
}

@media (max-width: 900px) {
  .recent-strip {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .task-hero {
    padding: 18px;
    border-radius: var(--qa-radius-lg);
  }

  .hero-copy h2 {
    font-size: 32px;
  }

  .hero-status {
    grid-template-columns: 1fr;
  }
}
</style>
