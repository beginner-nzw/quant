<script setup lang="ts">
import type { TaskDetail, TaskState, TaskReport } from '../../types/task'
import { REPORT_REVIEW_STATUS, TASK_STAGE } from '../../types/taskEnums'
import { formatDateTime } from '../../utils/format'
import {
  getReviewStatusTagType,
  getReviewStatusText,
  getTaskStageText,
  getTaskStatusTagType,
  getTaskStatusText,
  getTaskTypeText
} from '../../utils/task'

defineProps<{
  detail: TaskDetail
  state: TaskState
  report?: TaskReport | null
}>()

function getDisplayTaskStageText(stage?: string) {
  if (stage === 'EVIDENCE_COLLECTION') {
    return '证据采集'
  }
  return getTaskStageText(stage)
}

const text = {
  title: '任务基础信息',
  taskId: '任务 ID',
  taskTitle: '任务标题',
  taskType: '任务类型',
  targetCode: '标的代码',
  targetName: '标的名称',
  priority: '优先级',
  status: '当前状态',
  currentStage: '当前阶段',
  progress: '进度',
  reportReviewStatus: '报告审核',
  retryCount: '重试次数',
  startTime: '开始时间',
  finishTime: '结束时间',
  createdAt: '创建时间',
  updatedAt: '更新时间',
  sourceChannel: '来源渠道',
  errorMessage: '失败原因'
} as const
</script>

<template>
  <section class="detail-panel">
    <div class="detail-panel-heading">
      <span>Task Profile</span>
      <strong>{{ text.title }}</strong>
    </div>

    <el-descriptions :column="3" border>
      <el-descriptions-item :label="text.taskId">{{ detail.taskId }}</el-descriptions-item>
      <el-descriptions-item :label="text.taskTitle">{{ detail.taskTitle }}</el-descriptions-item>
      <el-descriptions-item :label="text.taskType">{{ getTaskTypeText(detail.taskType) }}</el-descriptions-item>

      <el-descriptions-item :label="text.targetCode">{{ detail.targetCode }}</el-descriptions-item>
      <el-descriptions-item :label="text.targetName">{{ detail.targetName }}</el-descriptions-item>
      <el-descriptions-item :label="text.priority">{{ detail.priority }}</el-descriptions-item>

      <el-descriptions-item :label="text.status">
        <el-tag :type="getTaskStatusTagType(state.status)">
          {{ getTaskStatusText(state.status) }}
        </el-tag>
      </el-descriptions-item>

      <el-descriptions-item :label="text.currentStage">{{ getDisplayTaskStageText(state.currentStage) }}</el-descriptions-item>
      <el-descriptions-item :label="text.progress">{{ state.progress ?? '-' }}</el-descriptions-item>

      <el-descriptions-item :label="text.reportReviewStatus">
        <template v-if="report">
          <el-tag :type="getReviewStatusTagType(report.reviewStatus || REPORT_REVIEW_STATUS.PENDING)">
            {{ getReviewStatusText(report.reviewStatus || REPORT_REVIEW_STATUS.PENDING) }}
          </el-tag>
        </template>
        <span v-else>-</span>
      </el-descriptions-item>

      <el-descriptions-item :label="text.retryCount">{{ detail.retryCount }}</el-descriptions-item>
      <el-descriptions-item :label="text.startTime">{{ formatDateTime(detail.startTime) }}</el-descriptions-item>
      <el-descriptions-item :label="text.finishTime">{{ formatDateTime(detail.finishTime) }}</el-descriptions-item>

      <el-descriptions-item :label="text.createdAt">{{ formatDateTime(detail.createdAt) }}</el-descriptions-item>
      <el-descriptions-item :label="text.updatedAt">{{ formatDateTime(detail.updatedAt) }}</el-descriptions-item>
      <el-descriptions-item :label="text.sourceChannel">{{ detail.sourceChannel || '-' }}</el-descriptions-item>

      <el-descriptions-item :label="text.errorMessage" :span="3">
        <el-alert
          v-if="detail.errorMessage"
          :title="detail.errorMessage"
          :type="detail.currentStage === TASK_STAGE.TIMEOUT ? 'warning' : 'error'"
          :closable="false"
          show-icon
        />
        <span v-else>-</span>
      </el-descriptions-item>
    </el-descriptions>
  </section>
</template>

<style scoped>
.detail-panel {
  display: grid;
  gap: 16px;
  padding: 22px;
  border: 1px solid rgba(20, 32, 51, 0.08);
  border-radius: var(--qa-radius-lg);
  background: rgba(255, 255, 255, 0.7);
  box-shadow: 0 20px 54px rgba(32, 52, 80, 0.09);
}

.detail-panel-heading span {
  display: block;
  color: #527086;
  font-size: 11px;
  font-weight: 950;
  letter-spacing: 0;
  text-transform: uppercase;
}

.detail-panel-heading strong {
  display: block;
  margin-top: 6px;
  color: #142033;
  font-size: 28px;
  font-weight: 950;
  letter-spacing: 0;
}
</style>
