<script setup lang="ts">
import MetricTile from '../common/MetricTile.vue'
import type { RiskWarningStats } from '../../types/task'

const text = {
  totalCount: '预警总数',
  highCount: '高风险',
  mediumCount: '中风险',
  lowCount: '低风险',
  pendingReviewCount: '待审核',
  humanReviewCount: '需人工复核'
} as const

const props = defineProps<{
  stats?: RiskWarningStats | null
}>()
</script>

<template>
  <section class="metric-grid">
    <MetricTile :label="text.totalCount" :value="props.stats?.totalCount ?? 0" hint="risk ledger" tone="ink" />
    <MetricTile :label="text.highCount" :value="props.stats?.highCount ?? 0" hint="critical" tone="red" />
    <MetricTile :label="text.mediumCount" :value="props.stats?.mediumCount ?? 0" hint="watch list" tone="amber" />
    <MetricTile :label="text.lowCount" :value="props.stats?.lowCount ?? 0" hint="tracked" tone="blue" />
    <MetricTile :label="text.pendingReviewCount" :value="props.stats?.pendingReviewCount ?? 0" hint="review queue" tone="amber" />
    <MetricTile :label="text.humanReviewCount" :value="props.stats?.humanReviewCount ?? 0" hint="manual review" tone="red" />
  </section>
</template>
