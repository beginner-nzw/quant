<script setup lang="ts">
import MetricTile from '../common/MetricTile.vue'
import type { AuditComplianceStats } from '../../types/task'

const text = {
  totalCount: '审计总量',
  pendingReviewCount: '待复核',
  interceptedCount: '拦截记录',
  revisedReportCount: '修订报告',
  humanReviewCount: '人工复核',
  decisionTraceCount: '决策链留痕',
  promptAuditCount: '输入输出留痕'
} as const

const props = defineProps<{
  stats?: AuditComplianceStats | null
}>()
</script>

<template>
  <section class="metric-grid">
    <MetricTile :label="text.totalCount" :value="props.stats?.totalCount ?? 0" hint="audit ledger" tone="ink" />
    <MetricTile :label="text.pendingReviewCount" :value="props.stats?.pendingReviewCount ?? 0" hint="review queue" tone="amber" />
    <MetricTile :label="text.interceptedCount" :value="props.stats?.interceptedCount ?? 0" hint="blocked" tone="red" />
    <MetricTile :label="text.revisedReportCount" :value="props.stats?.revisedReportCount ?? 0" hint="revised reports" tone="blue" />
    <MetricTile :label="text.humanReviewCount" :value="props.stats?.humanReviewCount ?? 0" hint="manual review" tone="red" />
    <MetricTile :label="text.decisionTraceCount" :value="props.stats?.decisionTraceCount ?? 0" hint="decision trace" tone="green" />
    <MetricTile :label="text.promptAuditCount" :value="props.stats?.promptAuditCount ?? 0" hint="prompt logs" tone="ink" />
  </section>
</template>
