<script setup lang="ts">
import MetricTile from '../common/MetricTile.vue'
import type { ReportCenterStats } from '../../types/task'

const text = {
  totalCount: '报告总数',
  highConfidenceCount: '高置信度',
  pendingReviewCount: '待审核',
  approvedCount: '已通过',
  humanReviewCount: '需人工复核'
} as const

const props = defineProps<{
  stats?: ReportCenterStats | null
}>()
</script>

<template>
  <section class="metric-grid">
    <MetricTile :label="text.totalCount" :value="props.stats?.totalCount ?? 0" hint="report ledger" tone="ink" />
    <MetricTile :label="text.highConfidenceCount" :value="props.stats?.highConfidenceCount ?? 0" hint="high confidence" tone="green" />
    <MetricTile :label="text.pendingReviewCount" :value="props.stats?.pendingReviewCount ?? 0" hint="review queue" tone="amber" />
    <MetricTile :label="text.approvedCount" :value="props.stats?.approvedCount ?? 0" hint="approved" tone="blue" />
    <MetricTile :label="text.humanReviewCount" :value="props.stats?.humanReviewCount ?? 0" hint="manual review" tone="red" />
  </section>
</template>
