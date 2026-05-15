<script setup lang="ts">
import { ref } from 'vue'
import DataEmptyState from '../common/DataEmptyState.vue'
import type { AuditRecord } from '../../types/task'
import { formatDateTime } from '../../utils/format'
import {
  getAuditActionText,
  getAuditOperatorTypeText,
  getAuditResultTagType,
  getAuditResultText,
  getAuditStageText,
  getAuditTypeText
} from '../../utils/task'

defineProps<{
  audits: AuditRecord[]
}>()

const text = {
  title: '审计记录',
  subtitle: '沉淀系统、Agent 和人工审核动作，方便回溯决策链路。',
  empty: '暂无审计记录',
  auditType: '审计类型',
  stage: '阶段',
  operatorType: '操作方',
  action: '动作',
  actionDesc: '说明',
  result: '结果',
  detail: '详情',
  view: '查看',
  createdAt: '创建时间',
  dialogTitle: '审计详情'
} as const

const dialogVisible = ref(false)
const dialogContent = ref('')

function showRemark(value?: string) {
  dialogContent.value = value || '-'
  dialogVisible.value = true
}
</script>

<template>
  <section class="detail-panel">
    <div class="detail-panel-heading">
      <span>Audit Trail</span>
      <strong>{{ text.title }}</strong>
      <p>{{ text.subtitle }}</p>
    </div>

    <DataEmptyState
      v-if="audits.length === 0"
      :title="text.empty"
      description="当前任务还没有产生审计事件。"
    />

    <el-table v-else :data="audits" border :empty-text="text.empty">
      <el-table-column :label="text.auditType" width="140">
        <template #default="{ row }">
          {{ getAuditTypeText(row.auditType) }}
        </template>
      </el-table-column>

      <el-table-column :label="text.stage" width="140">
        <template #default="{ row }">
          {{ getAuditStageText(row.auditStage) }}
        </template>
      </el-table-column>

      <el-table-column :label="text.operatorType" width="100">
        <template #default="{ row }">
          {{ getAuditOperatorTypeText(row.operatorType) }}
        </template>
      </el-table-column>

      <el-table-column :label="text.action" width="160">
        <template #default="{ row }">
          {{ getAuditActionText(row.actionCode) }}
        </template>
      </el-table-column>

      <el-table-column prop="actionDesc" :label="text.actionDesc" min-width="220" show-overflow-tooltip />

      <el-table-column :label="text.result" width="100">
        <template #default="{ row }">
          <el-tag :type="getAuditResultTagType(row.resultStatus)">
            {{ getAuditResultText(row.resultStatus) }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column :label="text.detail" width="100">
        <template #default="{ row }">
          <el-button link type="primary" @click="showRemark(row.remark)">{{ text.view }}</el-button>
        </template>
      </el-table-column>

      <el-table-column :label="text.createdAt" width="180">
        <template #default="{ row }">
          {{ formatDateTime(row.createdAt) }}
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="text.dialogTitle" width="700px">
      <pre class="audit-dialog-content">{{ dialogContent }}</pre>
    </el-dialog>
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

.audit-dialog-content {
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
