<script setup lang="ts">
import MetricTile from '../common/MetricTile.vue'
import type { StrategySignalStats } from '../../types/task'

const text = {
  totalCount: '信号总数',
  positiveCount: '偏多信号',
  neutralCount: '中性信号',
  negativeCount: '偏空信号',
  highConfidenceCount: '高置信度',
  pendingReviewCount: '待审核'
} as const

const props = defineProps<{
  stats?: StrategySignalStats | null
}>()
</script>

<template>
  <section class="metric-grid">
    <MetricTile :label="text.totalCount" :value="props.stats?.totalCount ?? 0" hint="signal ledger" tone="ink" />
    <MetricTile :label="text.positiveCount" :value="props.stats?.positiveCount ?? 0" hint="long bias" tone="green" />
    <MetricTile :label="text.neutralCount" :value="props.stats?.neutralCount ?? 0" hint="neutral" tone="amber" />
    <MetricTile :label="text.negativeCount" :value="props.stats?.negativeCount ?? 0" hint="short bias" tone="red" />
    <MetricTile :label="text.highConfidenceCount" :value="props.stats?.highConfidenceCount ?? 0" hint="high confidence" tone="blue" />
    <MetricTile :label="text.pendingReviewCount" :value="props.stats?.pendingReviewCount ?? 0" hint="review queue" tone="amber" />
  </section>
</template>
