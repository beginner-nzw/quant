<script setup lang="ts">
import { computed } from 'vue'
import MetricTile from '../common/MetricTile.vue'
import type { TaskStats } from '../../types/task'

const props = defineProps<{
  stats: TaskStats
}>()

const text = {
  totalCount: '任务总数',
  runningCount: '运行中',
  successCount: '成功',
  failedCount: '失败',
  retriedCount: '已重试',
  successRate: '成功率'
} as const

const successRate = computed(() => {
  if (!props.stats.totalCount) {
    return '0%'
  }
  return `${Math.round((props.stats.successCount / props.stats.totalCount) * 100)}%`
})
</script>

<template>
  <section class="task-stats-grid">
    <MetricTile :label="text.totalCount" :value="stats.totalCount" hint="task ledger" tone="ink" />
    <MetricTile :label="text.runningCount" :value="stats.runningCount" hint="active agents" tone="blue" />
    <MetricTile :label="text.successCount" :value="stats.successCount" hint="delivered" tone="green" />
    <MetricTile :label="text.failedCount" :value="stats.failedCount" hint="needs handling" tone="red" />
    <MetricTile :label="text.retriedCount" :value="stats.retriedCount" hint="retry history" tone="amber" />
    <MetricTile :label="text.successRate" :value="successRate" hint="success / total" tone="green" />
  </section>
</template>

<style scoped>
.task-stats-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 14px;
}

@media (max-width: 1280px) {
  .task-stats-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .task-stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
