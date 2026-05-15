<script setup lang="ts">
import MetricTile from '../common/MetricTile.vue'
import type { TaskSummary } from '../../types/task'

defineProps<{
  summary: TaskSummary
}>()

const text = {
  stepCount: '步骤总数',
  successStepCount: '成功步骤',
  failedStepCount: '失败步骤',
  agentCount: 'Agent 数量',
  retryCount: '重试次数',
  hasAudit: '审计记录',
  yes: '有',
  no: '无'
} as const
</script>

<template>
  <div class="summary-grid">
    <MetricTile :label="text.stepCount" :value="summary.stepCount" hint="workflow steps" tone="blue" />
    <MetricTile :label="text.successStepCount" :value="summary.successStepCount" hint="completed" tone="green" />
    <MetricTile :label="text.failedStepCount" :value="summary.failedStepCount" hint="needs attention" tone="red" />
    <MetricTile :label="text.agentCount" :value="summary.agentCount" hint="agents involved" tone="ink" />
    <MetricTile :label="text.retryCount" :value="summary.retryCount" hint="retry attempts" tone="amber" />
    <MetricTile :label="text.hasAudit" :value="summary.hasAudit ? text.yes : text.no" hint="audit trail" tone="ink" />
  </div>
</template>

<style scoped>
.summary-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 14px;
}

@media (max-width: 1280px) {
  .summary-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
