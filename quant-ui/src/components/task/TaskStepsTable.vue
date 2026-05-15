<script setup lang="ts">
import DataEmptyState from '../common/DataEmptyState.vue'
import type { TaskStep } from '../../types/task'
import { formatDateTime } from '../../utils/format'
import { getTaskStatusTagType, getTaskStatusText } from '../../utils/task'

defineProps<{
  steps: TaskStep[]
}>()

const text = {
  title: '步骤列表',
  subtitle: '保留完整步骤明细，用于定位耗时、失败节点和执行顺序。',
  empty: '暂无步骤数据',
  executionOrder: '顺序',
  stepCode: '步骤编码',
  agentCode: 'Agent',
  status: '状态',
  durationMs: '耗时(ms)',
  startTime: '开始时间',
  finishTime: '结束时间',
  errorMessage: '错误信息'
} as const
</script>

<template>
  <section class="detail-panel">
    <div class="detail-panel-heading">
      <span>Step Ledger</span>
      <strong>{{ text.title }}</strong>
      <p>{{ text.subtitle }}</p>
    </div>

    <DataEmptyState
      v-if="steps.length === 0"
      :title="text.empty"
      description="任务执行后会在这里展示步骤明细。"
    />

    <el-table v-else :data="steps" border>
      <el-table-column prop="executionOrder" :label="text.executionOrder" width="80" />
      <el-table-column prop="stepCode" :label="text.stepCode" width="220" />
      <el-table-column prop="agentCode" :label="text.agentCode" width="220" />

      <el-table-column :label="text.status" width="100">
        <template #default="{ row }">
          <el-tag :type="getTaskStatusTagType(row.status)">
            {{ getTaskStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="durationMs" :label="text.durationMs" width="100" />

      <el-table-column :label="text.startTime" width="180">
        <template #default="{ row }">
          {{ formatDateTime(row.startTime) }}
        </template>
      </el-table-column>

      <el-table-column :label="text.finishTime" width="180">
        <template #default="{ row }">
          {{ formatDateTime(row.finishTime) }}
        </template>
      </el-table-column>

      <el-table-column prop="errorMessage" :label="text.errorMessage" min-width="200" show-overflow-tooltip />
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
