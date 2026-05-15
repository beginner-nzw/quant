<script setup lang="ts">
import MetricTile from '../common/MetricTile.vue'
import type { MarketIntelligenceStats } from '../../types/task'

const text = {
  totalCount: '情报总数',
  riskAlertCount: '风险情报',
  strategySignalCount: '策略情报',
  reportInsightCount: '报告洞察',
  highPriorityCount: '高优先级',
  pendingReviewCount: '待审核'
} as const

const props = defineProps<{
  stats?: MarketIntelligenceStats | null
}>()
</script>

<template>
  <section class="metric-grid">
    <MetricTile :label="text.totalCount" :value="props.stats?.totalCount ?? 0" hint="intelligence ledger" tone="ink" />
    <MetricTile :label="text.riskAlertCount" :value="props.stats?.riskAlertCount ?? 0" hint="risk alerts" tone="red" />
    <MetricTile :label="text.strategySignalCount" :value="props.stats?.strategySignalCount ?? 0" hint="signals" tone="green" />
    <MetricTile :label="text.reportInsightCount" :value="props.stats?.reportInsightCount ?? 0" hint="report insights" tone="blue" />
    <MetricTile :label="text.highPriorityCount" :value="props.stats?.highPriorityCount ?? 0" hint="priority" tone="amber" />
    <MetricTile :label="text.pendingReviewCount" :value="props.stats?.pendingReviewCount ?? 0" hint="review queue" tone="amber" />
  </section>
</template>
