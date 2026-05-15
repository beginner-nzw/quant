<script setup lang="ts">
import DataEmptyState from '../common/DataEmptyState.vue'
import type { WorkflowInstance } from '../../types/task'
import { formatDateTime } from '../../utils/format'
import { getTaskStatusTagType, getTaskStatusText } from '../../utils/task'

defineProps<{
  workflow: WorkflowInstance | null
}>()

const text = {
  title: '工作流信息',
  subtitle: '执行实例、当前节点和图快照。',
  workflowInstanceId: '实例 ID',
  workflowCode: '工作流编码',
  workflowVersion: '版本',
  entryAgent: '入口 Agent',
  currentNode: '当前节点',
  status: '状态',
  startTime: '开始时间',
  finishTime: '结束时间',
  graphSnapshot: '图快照',
  empty: '暂无工作流信息'
} as const
</script>

<template>
  <section class="detail-panel">
    <div class="detail-panel-heading">
      <span>Workflow</span>
      <strong>{{ text.title }}</strong>
      <p>{{ text.subtitle }}</p>
    </div>

    <div v-if="workflow" class="workflow-grid">
      <div class="workflow-main">
        <span>{{ text.workflowCode }}</span>
        <strong>{{ workflow.workflowCode }}</strong>
        <small>{{ text.workflowInstanceId }} · {{ workflow.workflowInstanceId }}</small>
      </div>

      <div class="workflow-meta">
        <div>
          <span>{{ text.workflowVersion }}</span>
          <strong>{{ workflow.workflowVersion }}</strong>
        </div>
        <div>
          <span>{{ text.entryAgent }}</span>
          <strong>{{ workflow.entryAgent || '-' }}</strong>
        </div>
        <div>
          <span>{{ text.currentNode }}</span>
          <strong>{{ workflow.currentNode || '-' }}</strong>
        </div>
        <div>
          <span>{{ text.status }}</span>
          <el-tag :type="getTaskStatusTagType(workflow.status)">
            {{ getTaskStatusText(workflow.status) }}
          </el-tag>
        </div>
        <div>
          <span>{{ text.startTime }}</span>
          <strong>{{ formatDateTime(workflow.startTime) }}</strong>
        </div>
        <div>
          <span>{{ text.finishTime }}</span>
          <strong>{{ formatDateTime(workflow.finishTime) }}</strong>
        </div>
      </div>

      <div class="graph-snapshot">
        <span>{{ text.graphSnapshot }}</span>
        <pre>{{ workflow.graphSnapshot || '-' }}</pre>
      </div>
    </div>

    <DataEmptyState v-else :title="text.empty" description="任务尚未绑定或返回工作流实例。" />
  </section>
</template>

<style scoped>
.detail-panel {
  padding: 22px;
  border: 1px solid rgba(20, 32, 51, 0.08);
  border-radius: var(--qa-radius-lg);
  background: rgba(255, 255, 255, 0.7);
  box-shadow: 0 20px 54px rgba(32, 52, 80, 0.09);
}

.detail-panel-heading span,
.workflow-main span,
.workflow-meta span,
.graph-snapshot span {
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

.workflow-grid {
  display: grid;
  grid-template-columns: minmax(260px, 0.38fr) minmax(0, 1fr);
  gap: 14px;
  margin-top: 18px;
}

.workflow-main,
.workflow-meta,
.graph-snapshot {
  border-radius: var(--qa-radius-lg);
  background: rgba(20, 32, 51, 0.045);
  box-shadow: inset 0 0 0 1px rgba(20, 32, 51, 0.06);
}

.workflow-main {
  display: grid;
  align-content: center;
  min-height: 190px;
  padding: 20px;
}

.workflow-main strong {
  margin-top: 10px;
  color: #142033;
  font-size: 30px;
  font-weight: 950;
  letter-spacing: 0;
}

.workflow-main small {
  margin-top: 12px;
  color: #748396;
  word-break: break-all;
}

.workflow-meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  padding: 14px;
}

.workflow-meta div {
  min-width: 0;
  padding: 14px;
  border-radius: var(--qa-radius-lg);
  background: rgba(255, 255, 255, 0.72);
}

.workflow-meta strong {
  display: block;
  margin-top: 8px;
  overflow: hidden;
  color: #142033;
  font-weight: 950;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.graph-snapshot {
  grid-column: 1 / -1;
  padding: 16px;
}

.graph-snapshot pre {
  overflow: auto;
  margin: 10px 0 0;
  padding: 14px;
  border-radius: var(--qa-radius-lg);
  background: #142033;
  color: #d8fff8;
  font-family: 'JetBrains Mono', 'Cascadia Code', monospace;
  font-size: 12px;
  line-height: 1.7;
}

@media (max-width: 960px) {
  .workflow-grid,
  .workflow-meta {
    grid-template-columns: 1fr;
  }
}
</style>
