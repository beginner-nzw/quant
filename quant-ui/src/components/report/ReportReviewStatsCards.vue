<script setup lang="ts">
import MetricTile from '../common/MetricTile.vue'
import type { ReportReviewStats } from '../../types/task'

defineProps<{
  stats: ReportReviewStats
}>()
</script>

<template>
  <section class="metric-grid">
    <MetricTile label="报告总数" :value="stats.totalReportCount" hint="review ledger" tone="ink" />
    <MetricTile label="待审核" :value="stats.pendingCount" hint="pending" tone="amber" />
    <MetricTile label="已通过" :value="stats.approvedCount" hint="approved" tone="green" />
    <MetricTile label="已驳回" :value="stats.rejectedCount" hint="rejected" tone="red" />
  </section>
</template>
