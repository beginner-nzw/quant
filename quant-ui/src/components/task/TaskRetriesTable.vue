<script setup lang="ts">
import DataEmptyState from '../common/DataEmptyState.vue'
import type { TaskRetryLog } from '../../types/task'
import { formatDateTime } from '../../utils/format'
import { getRetrySourceText, getRetryStatusTagType, getRetryStatusText } from '../../utils/task'

defineProps<{
  retries: TaskRetryLog[]
}>()

const text = {
  title: '重试记录',
  subtitle: '记录任务补偿、人工重试和执行恢复过程。',
  empty: '暂无重试记录',
  retryNo: '重试次数',
  source: '来源',
  status: '状态',
  operatorId: '操作人',
  reason: '原因',
  createdAt: '创建时间'
} as const
</script>

<template>
  <section class="detail-panel">
    <div class="detail-panel-heading">
      <span>Retry Ledger</span>
      <strong>{{ text.title }}</strong>
      <p>{{ text.subtitle }}</p>
    </div>

    <DataEmptyState
      v-if="retries.length === 0"
      :title="text.empty"
      description="当前任务还没有重试或补偿记录。"
    />

    <el-table v-else :data="retries" border :empty-text="text.empty">
      <el-table-column prop="retryNo" :label="text.retryNo" width="100" />

      <el-table-column :label="text.source" width="120">
        <template #default="{ row }">
          {{ getRetrySourceText(row.retrySource) }}
        </template>
      </el-table-column>

      <el-table-column :label="text.status" width="120">
        <template #default="{ row }">
          <el-tag :type="getRetryStatusTagType(row.retryStatus)">
            {{ getRetryStatusText(row.retryStatus) }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column :label="text.operatorId" width="140">
        <template #default="{ row }">
          {{ row.operatorId || '-' }}
        </template>
      </el-table-column>

      <el-table-column prop="retryReason" :label="text.reason" min-width="220" show-overflow-tooltip />

      <el-table-column :label="text.createdAt" width="180">
        <template #default="{ row }">
          {{ formatDateTime(row.createdAt) }}
        </template>
      </el-table-column>
    </el-table>
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
</style>
