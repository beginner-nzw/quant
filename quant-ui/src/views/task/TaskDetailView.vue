<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import TaskBasicInfoCard from '../../components/task/TaskBasicInfoCard.vue'
import TaskSummaryCard from '../../components/task/TaskSummaryCard.vue'
import TaskWorkflowCard from '../../components/task/TaskWorkflowCard.vue'
import TaskStepsTimeline from '../../components/task/TaskStepsTimeline.vue'
import TaskStepsTable from '../../components/task/TaskStepsTable.vue'
import TaskAgentsTable from '../../components/task/TaskAgentsTable.vue'
import TaskAuditsTable from '../../components/task/TaskAuditsTable.vue'
import TaskRetriesTable from '../../components/task/TaskRetriesTable.vue'
import AgentFlowMap from '../../components/task/AgentFlowMap.vue'
import DataEmptyState from '../../components/common/DataEmptyState.vue'
import TaskReportCard from '@/components/task/TaskReportCard.vue'
import { fetchTaskFullDetail } from '../../api/task'
import type { TaskFullDetail } from '../../types/task'
import { resolveTaskDetailActionAccess } from '../../utils/taskActionAccess'
import { getAnalysisScopeText, getReviewStatusText, getSourceDomainText, getTaskStatusText, getTaskTypeText } from '../../utils/task'
import { buildTaskCreateQuery } from '../../utils/taskCreate'
import { executeTaskCancel, executeTaskRetry } from '../../utils/taskActions'
import { buildFromQuery, resolveSourcePath } from '../../utils/taskNavigation'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const detail = ref<TaskFullDetail | null>(null)

const text = {
  loadDetailFailed: '任务详情加载失败',
  loadDetailError: '任务详情加载异常',
  sourceTitle: '来源上下文',
  sourceDomain: '来源业务',
  sourceTaskId: '来源任务',
  sourceReportId: '来源报告',
  sourceEventId: '来源事件',
  sourceReviewStatus: '来源审核状态',
  analysisScope: '分析范围',
  backToSource: '返回来源页',
  backToList: '返回列表',
  refreshDetail: '刷新详情',
  viewFullReport: '查看完整报告',
  createSimilarTask: '创建类似任务',
  manualRetry: '手工重试',
  cancelTask: '取消任务',
  retryReason: '详情页手工重试',
  cancelReason: '详情页手工取消',
  empty: '暂无任务详情'
} as const

const sourcePath = computed(() => resolveSourcePath(route.query.from))

const hasTaskSourceContext = computed(() => {
  return Boolean(
    detail.value?.taskDetail.sourceTaskId
      || detail.value?.taskDetail.sourceReportId
      || detail.value?.taskDetail.sourceEventId
      || detail.value?.taskDetail.sourceDomain
      || detail.value?.taskDetail.sourceReviewStatus
      || detail.value?.taskDetail.analysisScope
  )
})

const sourceDomainText = computed(() => {
  const value = detail.value?.taskDetail.sourceDomain
  return value ? getSourceDomainText(value) : '-'
})

const sourceReviewStatusText = computed(() => {
  const value = detail.value?.taskDetail.sourceReviewStatus
  return value ? getReviewStatusText(value) : '-'
})

const analysisScopeText = computed(() => {
  const value = detail.value?.taskDetail.analysisScope
  return value ? getAnalysisScopeText(value) : '-'
})

const actionAccess = computed(() => resolveTaskDetailActionAccess(detail.value))

const taskHeadline = computed(() => {
  if (!detail.value?.taskDetail) {
    return '-'
  }
  const task = detail.value.taskDetail
  return `${task.targetName || '-'} · ${task.taskTitle || task.taskId}`
})

const taskMeta = computed(() => {
  if (!detail.value?.taskDetail) {
    return []
  }
  const task = detail.value.taskDetail
  return [
    { label: '任务类型', value: getTaskTypeText(task.taskType) },
    { label: '标的代码', value: task.targetCode || '-' },
    { label: '当前状态', value: getTaskStatusText(detail.value.taskState.status) },
    { label: '执行进度', value: detail.value.taskState.progress == null ? '-' : `${detail.value.taskState.progress}%` }
  ]
})

async function loadDetail() {
  loading.value = true
  try {
    const taskId = route.params.taskId as string
    const res = await fetchTaskFullDetail(taskId)
    if (res.success) {
      detail.value = res.data
    } else {
      detail.value = null
      ElMessage.error(res.message || text.loadDetailFailed)
    }
  } catch (e: any) {
    detail.value = null
    ElMessage.error(e?.message || text.loadDetailError)
  } finally {
    loading.value = false
  }
}

async function handleRetry() {
  const taskId = route.params.taskId as string
  const success = await executeTaskRetry(taskId, text.retryReason)
  if (success) {
    await loadDetail()
  }
}

async function handleCancel() {
  const taskId = route.params.taskId as string
  const success = await executeTaskCancel(taskId, text.cancelReason)
  if (success) {
    await loadDetail()
  }
}

function goFullReport() {
  const taskId = route.params.taskId as string
  router.push({
    path: `/tasks/${taskId}/report`,
    query: buildFromQuery(sourcePath.value)
  })
}

function handleCreateSimilarTask() {
  if (!detail.value?.taskDetail) {
    return
  }

  const taskDetail = detail.value.taskDetail
  const report = detail.value.report

  router.push({
    path: '/tasks/create',
    query: buildTaskCreateQuery({
      taskType: taskDetail.taskType,
      taskTitle: taskDetail.taskTitle,
      targetType: taskDetail.targetType,
      targetCode: taskDetail.targetCode,
      targetName: taskDetail.targetName,
      priority: taskDetail.priority,
      sourceTaskId: taskDetail.sourceTaskId || taskDetail.taskId,
      sourceReportId: taskDetail.sourceReportId || report?.reportId || report?.reportMeta?.reportId,
      sourceEventId: taskDetail.sourceEventId,
      sourceDomain: taskDetail.sourceDomain || 'TASK_DETAIL',
      sourceReviewStatus: taskDetail.sourceReviewStatus || report?.reviewStatus,
      analysisScope: taskDetail.analysisScope || 'DEEP_RESEARCH',
      from: sourcePath.value || route.fullPath
    })
  })
}

watch(
  () => route.params.taskId,
  () => {
    loadDetail()
  },
  { immediate: true }
)
</script>

<template>
  <div v-loading="loading" class="task-detail-page">
    <template v-if="detail">
      <section class="detail-hero">
        <div class="hero-copy">
          <span>Task Intelligence</span>
          <h2>{{ taskHeadline }}</h2>
          <p>围绕一个任务查看状态、Agent 链路、证据报告、审计记录和补偿动作。</p>
        </div>

        <div class="hero-actions">
          <el-button v-if="sourcePath" @click="router.push(sourcePath)">{{ text.backToSource }}</el-button>
          <el-button @click="router.push('/tasks')">{{ text.backToList }}</el-button>
          <el-button @click="loadDetail">{{ text.refreshDetail }}</el-button>
          <el-button v-if="actionAccess.showViewFullReport" type="primary" @click="goFullReport">
            {{ text.viewFullReport }}
          </el-button>
          <el-button v-if="actionAccess.showCreateSimilarTask" type="success" @click="handleCreateSimilarTask">
            {{ text.createSimilarTask }}
          </el-button>
          <el-button v-if="actionAccess.showRetry" type="danger" @click="handleRetry">
            {{ text.manualRetry }}
          </el-button>
          <el-button v-if="actionAccess.showCancel" type="warning" @click="handleCancel">
            {{ text.cancelTask }}
          </el-button>
        </div>

        <div class="hero-meta">
          <div v-for="item in taskMeta" :key="item.label">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </div>
        </div>
      </section>

      <TaskSummaryCard :summary="detail.summary" />

      <AgentFlowMap
        :workflow="detail.workflow"
        :agents="detail.agents"
        :steps="detail.steps"
      />

      <section class="detail-grid">
        <TaskBasicInfoCard
          :detail="detail.taskDetail"
          :state="detail.taskState"
          :report="detail.report"
        />

        <TaskReportCard
          v-if="actionAccess.showReportCard"
          :report="detail.report"
        />
      </section>

      <section v-if="hasTaskSourceContext" class="source-panel">
        <div class="source-panel-heading">
          <span>Source Context</span>
          <strong>{{ text.sourceTitle }}</strong>
        </div>

        <el-descriptions :column="2" border>
          <el-descriptions-item :label="text.sourceDomain">
            {{ sourceDomainText }}
          </el-descriptions-item>
          <el-descriptions-item :label="text.analysisScope">
            {{ analysisScopeText }}
          </el-descriptions-item>
          <el-descriptions-item :label="text.sourceTaskId">
            {{ detail.taskDetail.sourceTaskId || '-' }}
          </el-descriptions-item>
          <el-descriptions-item :label="text.sourceReportId">
            {{ detail.taskDetail.sourceReportId || '-' }}
          </el-descriptions-item>
          <el-descriptions-item :label="text.sourceEventId">
            {{ detail.taskDetail.sourceEventId || '-' }}
          </el-descriptions-item>
          <el-descriptions-item :label="text.sourceReviewStatus">
            {{ sourceReviewStatusText }}
          </el-descriptions-item>
        </el-descriptions>
      </section>

      <section class="detail-section">
        <TaskWorkflowCard :workflow="detail.workflow" />
      </section>

      <section class="detail-section">
        <TaskStepsTimeline :steps="detail.steps" />
      </section>

      <section class="detail-section">
        <TaskStepsTable :steps="detail.steps" />
      </section>

      <section class="detail-section">
        <TaskAgentsTable :agents="detail.agents" />
      </section>

      <section class="detail-section">
        <TaskAuditsTable :audits="detail.audits" />
      </section>

      <section class="detail-section">
        <TaskRetriesTable :retries="detail.retries" />
      </section>
    </template>

    <DataEmptyState
      v-else
      :title="text.empty"
      description="任务不存在或接口暂不可用。请返回任务中心重新选择任务。"
      :action-label="text.backToList"
      @action="router.push('/tasks')"
    />
  </div>
</template>

<style scoped>
.task-detail-page {
  display: grid;
  gap: 18px;
}

.detail-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(260px, 0.42fr);
  gap: 18px;
  padding: 24px;
  border-radius: var(--qa-radius-lg);
  background: linear-gradient(135deg, #142033, #1d3c5c);
  color: #fff;
  box-shadow: 0 22px 70px rgba(20, 32, 51, 0.18);
}

.hero-copy span {
  color: #69d2c4;
  font-size: 12px;
  font-weight: 950;
  letter-spacing: 0;
  text-transform: uppercase;
}

.hero-copy h2 {
  margin: 10px 0 0;
  font-size: 42px;
  font-weight: 950;
  letter-spacing: 0;
  line-height: 1;
}

.hero-copy p {
  max-width: 660px;
  margin: 14px 0 0;
  color: rgba(255, 255, 255, 0.68);
  line-height: 1.8;
}

.hero-actions {
  display: flex;
  align-content: flex-start;
  align-items: flex-start;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
}

.hero-meta {
  grid-column: 1 / -1;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.hero-meta div {
  padding: 14px;
  border-radius: var(--qa-radius-lg);
  background: rgba(255, 255, 255, 0.12);
}

.hero-meta span,
.hero-meta strong {
  display: block;
}

.hero-meta span {
  color: rgba(255, 255, 255, 0.58);
  font-size: 12px;
}

.hero-meta strong {
  margin-top: 4px;
  font-weight: 950;
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(360px, 0.9fr);
  gap: 18px;
}

.detail-section {
  min-width: 0;
}

.source-panel {
  display: grid;
  gap: 16px;
  padding: 22px;
  border: 1px solid rgba(20, 32, 51, 0.08);
  border-radius: var(--qa-radius-lg);
  background: rgba(255, 255, 255, 0.7);
  box-shadow: 0 20px 54px rgba(32, 52, 80, 0.09);
}

.source-panel-heading span {
  display: block;
  color: #527086;
  font-size: 11px;
  font-weight: 950;
  letter-spacing: 0;
  text-transform: uppercase;
}

.source-panel-heading strong {
  display: block;
  margin-top: 6px;
  color: #142033;
  font-size: 24px;
  font-weight: 950;
  letter-spacing: 0;
}

@media (max-width: 1180px) {
  .detail-hero,
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .hero-actions {
    justify-content: flex-start;
  }
}

@media (max-width: 760px) {
  .hero-copy h2 {
    font-size: 30px;
  }

  .hero-meta {
    grid-template-columns: 1fr;
  }
}
</style>
