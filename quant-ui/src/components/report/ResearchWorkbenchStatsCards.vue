<script setup lang="ts">
import MetricTile from '../common/MetricTile.vue'
import type { ResearchWorkbenchData } from '../../types/task'

const text = {
  taskCount: '研究任务',
  reportCount: '可用报告',
  activeTaskCount: '运行中',
  highConfidenceReportCount: '高置信报告',
  pendingReviewCount: '待审核',
  failedTaskCount: '失败任务'
} as const

defineProps<{
  data: ResearchWorkbenchData
}>()
</script>

<template>
  <section class="metric-grid">
    <MetricTile :label="text.taskCount" :value="data.taskCount" hint="task ledger" tone="ink" />
    <MetricTile :label="text.reportCount" :value="data.reportCount" hint="reports" tone="blue" />
    <MetricTile :label="text.activeTaskCount" :value="data.activeTaskCount" hint="active" tone="amber" />
    <MetricTile :label="text.highConfidenceReportCount" :value="data.highConfidenceReportCount" hint="high confidence" tone="green" />
    <MetricTile :label="text.pendingReviewCount" :value="data.pendingReviewCount" hint="review queue" tone="amber" />
    <MetricTile :label="text.failedTaskCount" :value="data.failedTaskCount" hint="needs handling" tone="red" />
  </section>
</template>
