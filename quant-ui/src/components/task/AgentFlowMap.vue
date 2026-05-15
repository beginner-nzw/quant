<script setup lang="ts">
import { computed } from 'vue'
import type { AgentExecution, TaskStep, WorkflowInstance } from '../../types/task'
import { getTaskStatusText } from '../../utils/task'

const props = defineProps<{
  workflow: WorkflowInstance | null
  agents: AgentExecution[]
  steps: TaskStep[]
}>()

const nodes = computed(() => {
  if (props.agents.length) {
    return props.agents.map((agent, index) => ({
      key: agent.executionId || `${agent.agentCode}-${index}`,
      title: agent.agentName || agent.agentCode || `Agent ${index + 1}`,
      subtitle: agent.nodeCode || agent.agentCode || '-',
      status: agent.status,
      duration: agent.durationMs,
      confidence: agent.confidenceScore,
      humanReview: Boolean(agent.needHumanReview)
    }))
  }

  return props.steps.map((step, index) => ({
    key: `${step.stepCode}-${index}`,
    title: step.stepName || step.stepCode || `Step ${index + 1}`,
    subtitle: step.agentCode || step.stepCode || '-',
    status: step.status,
    duration: step.durationMs,
    confidence: undefined,
    humanReview: false
  }))
})

const successCount = computed(() => nodes.value.filter((node) => node.status === 'SUCCESS').length)
const failedCount = computed(() => nodes.value.filter((node) => node.status === 'FAILED').length)

function getNodeClass(status?: string) {
  switch (status) {
    case 'SUCCESS':
      return 'success'
    case 'FAILED':
      return 'failed'
    case 'RUNNING':
    case 'PROCESSING':
      return 'running'
    default:
      return 'pending'
  }
}

function formatConfidence(value?: number) {
  if (value == null) {
    return '-'
  }
  return value > 1 ? `${Math.round(value)}%` : `${Math.round(value * 100)}%`
}
</script>

<template>
  <section class="agent-flow">
    <div class="flow-header">
      <div>
        <span>Agent Flow</span>
        <strong>多智能体执行链路</strong>
        <p>
          {{ workflow?.workflowCode || '未绑定工作流' }}
          <template v-if="workflow?.currentNode"> · 当前节点 {{ workflow.currentNode }}</template>
        </p>
      </div>
      <div class="flow-stats">
        <div>
          <b>{{ nodes.length }}</b>
          <small>节点</small>
        </div>
        <div>
          <b>{{ successCount }}</b>
          <small>成功</small>
        </div>
        <div>
          <b>{{ failedCount }}</b>
          <small>失败</small>
        </div>
      </div>
    </div>

    <div v-if="nodes.length" class="node-track">
      <article
        v-for="node in nodes"
        :key="node.key"
        class="node-card"
        :class="getNodeClass(node.status)"
      >
        <div class="node-dot" />
        <span>{{ node.subtitle }}</span>
        <strong>{{ node.title }}</strong>
        <div class="node-meta">
          <small>{{ getTaskStatusText(node.status) }}</small>
          <small>{{ node.duration ?? '-' }}ms</small>
          <small>置信度 {{ formatConfidence(node.confidence) }}</small>
        </div>
        <em v-if="node.humanReview">人工复核</em>
      </article>
    </div>

    <div v-else class="flow-empty">
      暂无 Agent 执行链路。任务启动后会在这里展示节点状态、耗时和复核要求。
    </div>
  </section>
</template>

<style scoped>
.agent-flow {
  padding: 22px;
  border: 1px solid rgba(20, 32, 51, 0.08);
  border-radius: var(--qa-radius-lg);
  background: rgba(255, 255, 255, 0.72);
  box-shadow: 0 20px 54px rgba(32, 52, 80, 0.09);
}

.flow-header {
  display: flex;
  justify-content: space-between;
  gap: 18px;
}

.flow-header span {
  display: block;
  color: #527086;
  font-size: 11px;
  font-weight: 950;
  letter-spacing: 0;
  text-transform: uppercase;
}

.flow-header strong {
  display: block;
  margin-top: 6px;
  color: #142033;
  font-size: 28px;
  font-weight: 950;
  letter-spacing: 0;
}

.flow-header p {
  margin: 8px 0 0;
  color: #6b7a8d;
}

.flow-stats {
  display: flex;
  gap: 10px;
}

.flow-stats div {
  min-width: 78px;
  padding: 12px;
  border-radius: var(--qa-radius-lg);
  background: rgba(20, 32, 51, 0.06);
  text-align: center;
}

.flow-stats b,
.flow-stats small {
  display: block;
}

.flow-stats b {
  color: #142033;
  font-size: 24px;
  font-weight: 950;
}

.flow-stats small {
  color: #6b7a8d;
}

.node-track {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
  margin-top: 22px;
}

.node-card {
  position: relative;
  min-height: 168px;
  padding: 18px;
  border-radius: var(--qa-radius-lg);
  background: rgba(255, 255, 255, 0.76);
  box-shadow: inset 0 0 0 1px rgba(20, 32, 51, 0.08);
}

.node-card::after {
  content: '';
  position: absolute;
  top: 35px;
  right: -12px;
  width: 24px;
  height: 2px;
  background: rgba(20, 32, 51, 0.14);
}

.node-card:last-child::after {
  display: none;
}

.node-dot {
  width: 14px;
  height: 14px;
  border-radius: 999px;
  background: #8a97a8;
  box-shadow: 0 0 0 7px rgba(138, 151, 168, 0.12);
}

.node-card.success .node-dot {
  background: #158c73;
  box-shadow: 0 0 0 7px rgba(21, 140, 115, 0.14);
}

.node-card.failed .node-dot {
  background: #cf3b38;
  box-shadow: 0 0 0 7px rgba(207, 59, 56, 0.14);
}

.node-card.running .node-dot {
  background: #b26b11;
  box-shadow: 0 0 0 7px rgba(178, 107, 17, 0.14);
}

.node-card span,
.node-card strong,
.node-card em {
  display: block;
}

.node-card span {
  margin-top: 18px;
  color: #6b7a8d;
  font-size: 12px;
  font-weight: 900;
}

.node-card strong {
  margin-top: 6px;
  color: #142033;
  font-size: 17px;
  font-weight: 950;
}

.node-meta {
  display: grid;
  gap: 4px;
  margin-top: 14px;
  color: #758498;
  font-size: 12px;
}

.node-card em {
  position: absolute;
  top: 14px;
  right: 14px;
  padding: 5px 9px;
  border-radius: var(--qa-radius-md);
  background: #fff1d5;
  color: #b26b11;
  font-size: 12px;
  font-style: normal;
  font-weight: 900;
}

.flow-empty {
  margin-top: 18px;
  padding: 28px;
  border: 1px dashed rgba(20, 32, 51, 0.14);
  border-radius: var(--qa-radius-lg);
  color: #6b7a8d;
  text-align: center;
}

@media (max-width: 760px) {
  .flow-header,
  .flow-stats {
    flex-direction: column;
  }
}
</style>
