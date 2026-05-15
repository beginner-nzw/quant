<script setup lang="ts">
import DataEmptyState from '../common/DataEmptyState.vue'
import type { TaskStep } from '../../types/task'
import { formatDateTime } from '../../utils/format'
import { getTaskStatusTagType, getTaskStatusText } from '../../utils/task'

defineProps<{
  steps: TaskStep[]
}>()

const text = {
  title: '步骤时间线',
  subtitle: '按执行顺序查看任务从规划到报告生成的关键节点。',
  empty: '暂无步骤时间线'
} as const
</script>

<template>
  <section class="detail-panel">
    <div class="detail-panel-heading">
      <span>Step Timeline</span>
      <strong>{{ text.title }}</strong>
      <p>{{ text.subtitle }}</p>
    </div>

    <DataEmptyState
      v-if="steps.length === 0"
      :title="text.empty"
      description="任务启动后会在这里展示每个步骤的执行顺序。"
    />

    <el-timeline v-else>
      <el-timeline-item
        v-for="step in steps"
        :key="step.executionOrder + '-' + step.stepCode"
        :timestamp="formatDateTime(step.finishTime || step.startTime)"
        placement="top"
      >
        <div class="timeline-node">
          <div>
            <strong>{{ step.stepName || step.stepCode }}</strong>
            <span>Agent: {{ step.agentCode || '-' }}</span>
            <p v-if="step.errorMessage">错误：{{ step.errorMessage }}</p>
          </div>
          <el-tag :type="getTaskStatusTagType(step.status)">
            {{ getTaskStatusText(step.status) }}
          </el-tag>
        </div>
      </el-timeline-item>
    </el-timeline>
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

.detail-panel-heading p {
  margin: 8px 0 0;
  color: #6b7a8d;
}

.timeline-node {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  padding: 14px;
  border: 1px solid rgba(20, 32, 51, 0.08);
  border-radius: var(--qa-radius-lg);
  background: rgba(255, 255, 255, 0.78);
}

.timeline-node strong,
.timeline-node span {
  display: block;
}

.timeline-node strong {
  color: #142033;
  font-weight: 950;
}

.timeline-node span {
  margin-top: 6px;
  color: #6b7a8d;
}

.timeline-node p {
  margin: 8px 0 0;
  color: #cf3b38;
}

@media (max-width: 760px) {
  .timeline-node {
    flex-direction: column;
  }
}
</style>
