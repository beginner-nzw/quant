<script setup lang="ts">
import DataEmptyState from '../common/DataEmptyState.vue'
import type { AgentExecution } from '../../types/task'
import {
  getHumanReviewTagType,
  getHumanReviewText,
  getTaskStatusTagType,
  getTaskStatusText
} from '../../utils/task'

defineProps<{
  agents: AgentExecution[]
}>()

const text = {
  title: 'Agent 执行记录',
  subtitle: '记录每个 Agent 节点的状态、置信度、复核要求和耗时。',
  empty: '暂无 Agent 执行数据',
  agentCode: 'Agent 编码',
  agentName: 'Agent 名称',
  nodeCode: '节点',
  status: '状态',
  confidenceScore: '置信度',
  needHumanReview: '人工复核',
  durationMs: '耗时(ms)'
} as const
</script>

<template>
  <section class="detail-panel">
    <div class="detail-panel-heading">
      <span>Agent Ledger</span>
      <strong>{{ text.title }}</strong>
      <p>{{ text.subtitle }}</p>
    </div>

    <DataEmptyState
      v-if="agents.length === 0"
      :title="text.empty"
      description="任务启动后会在这里沉淀 Agent 执行记录。"
    />

    <el-table v-else :data="agents" border>
      <el-table-column prop="agentCode" :label="text.agentCode" width="220" />
      <el-table-column prop="agentName" :label="text.agentName" width="220" />
      <el-table-column prop="nodeCode" :label="text.nodeCode" width="200" />

      <el-table-column :label="text.status" width="100">
        <template #default="{ row }">
          <el-tag :type="getTaskStatusTagType(row.status)">
            {{ getTaskStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column :label="text.confidenceScore" width="100">
        <template #default="{ row }">
          {{ row.confidenceScore ?? '-' }}
        </template>
      </el-table-column>

      <el-table-column :label="text.needHumanReview" width="100">
        <template #default="{ row }">
          <el-tag :type="getHumanReviewTagType(row.needHumanReview)">
            {{ getHumanReviewText(row.needHumanReview) }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column :label="text.durationMs" width="100">
        <template #default="{ row }">
          {{ row.durationMs ?? '-' }}
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
