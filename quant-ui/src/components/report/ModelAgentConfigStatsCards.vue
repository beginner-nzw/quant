<script setup lang="ts">
import MetricTile from '../common/MetricTile.vue'
import type { ModelAgentConfigStats } from '../../types/task'

const text = {
  workflowCount: '工作流数',
  activeAgentCount: '启用 Agent',
  modelStrategyCount: '模型策略',
  eventAutoTriggerRuleCount: '事件触发规则',
  eventSourceConfigCount: '事件源配置',
  promptTemplateCount: 'Prompt 模板',
  toolWhitelistCount: '工具白名单',
  placeholderStrategyCount: '占位策略',
  configAuditCount: '变更审计',
  roleAccessConfigCount: '角色配置'
} as const

const props = defineProps<{
  stats?: ModelAgentConfigStats | null
}>()
</script>

<template>
  <section class="metric-grid">
    <MetricTile :label="text.workflowCount" :value="props.stats?.workflowCount ?? 0" hint="workflow" tone="blue" />
    <MetricTile :label="text.activeAgentCount" :value="props.stats?.activeAgentCount ?? 0" hint="enabled agents" tone="green" />
    <MetricTile :label="text.modelStrategyCount" :value="props.stats?.modelStrategyCount ?? 0" hint="model routing" tone="blue" />
    <MetricTile :label="text.eventAutoTriggerRuleCount" :value="props.stats?.eventAutoTriggerRuleCount ?? 0" hint="event rules" tone="amber" />
    <MetricTile :label="text.eventSourceConfigCount" :value="props.stats?.eventSourceConfigCount ?? 0" hint="event sources" tone="green" />
    <MetricTile :label="text.promptTemplateCount" :value="props.stats?.promptTemplateCount ?? 0" hint="prompt templates" tone="ink" />
    <MetricTile :label="text.toolWhitelistCount" :value="props.stats?.toolWhitelistCount ?? 0" hint="tools" tone="ink" />
    <MetricTile :label="text.placeholderStrategyCount" :value="props.stats?.placeholderStrategyCount ?? 0" hint="placeholder" tone="amber" />
    <MetricTile :label="text.configAuditCount" :value="props.stats?.configAuditCount ?? 0" hint="change logs" tone="blue" />
    <MetricTile :label="text.roleAccessConfigCount" :value="props.stats?.roleAccessConfigCount ?? 0" hint="role access" tone="ink" />
  </section>
</template>
