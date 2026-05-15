<script setup lang="ts">
import { computed } from 'vue'
import type { TaskReportEvidenceItem } from '../../types/task'

interface EvidenceRefItem {
  label: string
  value: string
  raw: string
}

interface EvidenceSection {
  key: string
  title: string
  type: 'primary' | 'success' | 'warning' | 'info' | 'danger'
  items: EvidenceRefItem[]
}

interface DisplayEvidenceItem {
  evidenceId: string
  evidenceType: string
  source: string
  title: string
  summary: string
  url: string
  occurredAt: string
  referenceId: string
  relevance: string
}

const props = withDefaults(defineProps<{
  evidenceItems?: TaskReportEvidenceItem[]
  evidenceRefs?: string[]
  reviewSuggestion?: string
  emptyText?: string
}>(), {
  evidenceItems: () => [],
  evidenceRefs: () => [],
  reviewSuggestion: '',
  emptyText: '暂无证据链'
})

const text = {
  reviewSuggestion: '审核建议',
  structuredEvidence: '结构化证据',
  source: '来源引用',
  market: '市场快照',
  planning: '规划证据',
  intent: '意图证据',
  financial: '财务分析',
  risk: '风险审查',
  report: '报告生成',
  other: '其他证据',
  sourceLabel: '来源渠道',
  time: '时间',
  referenceId: '引用 ID',
  link: '原文链接',
  openLink: '打开链接',
  taskContext: '任务上下文',
  taskTrace: '任务 Trace',
  sourceTask: '来源任务',
  sourceReport: '来源报告',
  sourceEvent: '来源事件',
  recentMarketEvent: '近期市场事件',
  liveMarketEvent: '实时市场事件',
  policyLiveEvent: '政策实时事件',
  regulatoryRiskLiveEvent: '监管风险实时事件',
  sourceDomain: '来源领域',
  sourceTaskReport: '来源任务报告',
  marketData: '市场数据来源',
  liveEventSource: '实时事件源',
  liveEventCount: '实时事件数',
  policyLiveEventCount: '政策实时事件数',
  regulatoryRiskLiveEventCount: '监管风险事件数',
  reportCount: '同标的报告数',
  taskCount: '同标的任务数',
  pendingReviewCount: '待审核报告数',
  latestInsightReport: '最新洞察报告',
  priorityLiveEventTitle: '优先实时事件标题',
  priorityLiveEventOccurredAt: '优先实时事件时间',
  priorityLiveEventEvidenceStatus: '优先实时事件证据状态',
  priorityLiveEventEvidenceMatchRule: '优先实时事件证据匹配规则',
  priorityLiveEventReferenceId: '优先实时事件引用',
  priorityLiveEventEvidenceId: '优先实时事件证据 ID',
  priorityLiveEventEvidenceSource: '优先实时事件证据源',
  planningMode: '规划模式',
  planningGeneration: '规划生成模式',
  planningFramework: '规划框架',
  planningModel: '规划模型',
  planningFallback: '规划回退原因',
  focus: '关注维度',
  reviewPressure: '审核压力',
  intentGeneration: '意图生成模式',
  intentFramework: '意图框架',
  intentModel: '意图模型',
  intentFallback: '意图回退原因',
  financialGeneration: '财务生成模式',
  financialFramework: '财务框架',
  financialModel: '财务模型',
  riskGeneration: '风险生成模式',
  riskFramework: '风险框架',
  riskModel: '风险模型',
  modelConfigured: '模型配置',
  reportFramework: '报告框架',
  evidenceSource: '证据来源',
  rawRef: '原始证据'
} as const

const sectionMeta: Record<string, Omit<EvidenceSection, 'items'>> = {
  source: { key: 'source', title: text.source, type: 'primary' },
  market: { key: 'market', title: text.market, type: 'info' },
  planning: { key: 'planning', title: text.planning, type: 'warning' },
  intent: { key: 'intent', title: text.intent, type: 'warning' },
  financial: { key: 'financial', title: text.financial, type: 'success' },
  risk: { key: 'risk', title: text.risk, type: 'danger' },
  report: { key: 'report', title: text.report, type: 'success' },
  other: { key: 'other', title: text.other, type: 'info' }
}

const keyMeta: Record<string, { label: string; section: keyof typeof sectionMeta }> = {
  taskContext: { label: text.taskContext, section: 'source' },
  taskTrace: { label: text.taskTrace, section: 'source' },
  sourceTask: { label: text.sourceTask, section: 'source' },
  sourceReport: { label: text.sourceReport, section: 'source' },
  sourceEvent: { label: text.sourceEvent, section: 'source' },
  recentMarketEvent: { label: text.recentMarketEvent, section: 'market' },
  liveMarketEvent: { label: text.liveMarketEvent, section: 'market' },
  policyLiveEvent: { label: text.policyLiveEvent, section: 'market' },
  regulatoryRiskLiveEvent: { label: text.regulatoryRiskLiveEvent, section: 'risk' },
  sourceDomain: { label: text.sourceDomain, section: 'source' },
  sourceTaskReport: { label: text.sourceTaskReport, section: 'source' },
  marketData: { label: text.marketData, section: 'market' },
  liveEventSource: { label: text.liveEventSource, section: 'market' },
  liveEventCount: { label: text.liveEventCount, section: 'market' },
  policyLiveEventCount: { label: text.policyLiveEventCount, section: 'market' },
  regulatoryRiskLiveEventCount: { label: text.regulatoryRiskLiveEventCount, section: 'risk' },
  reportCount: { label: text.reportCount, section: 'market' },
  taskCount: { label: text.taskCount, section: 'market' },
  pendingReviewCount: { label: text.pendingReviewCount, section: 'market' },
  latestInsightReport: { label: text.latestInsightReport, section: 'market' },
  priorityLiveEventTitle: { label: text.priorityLiveEventTitle, section: 'market' },
  priorityLiveEventOccurredAt: { label: text.priorityLiveEventOccurredAt, section: 'market' },
  priorityLiveEventEvidenceStatus: { label: text.priorityLiveEventEvidenceStatus, section: 'market' },
  priorityLiveEventEvidenceMatchRule: { label: text.priorityLiveEventEvidenceMatchRule, section: 'market' },
  priorityLiveEventReferenceId: { label: text.priorityLiveEventReferenceId, section: 'market' },
  priorityLiveEventEvidenceId: { label: text.priorityLiveEventEvidenceId, section: 'market' },
  priorityLiveEventEvidenceSource: { label: text.priorityLiveEventEvidenceSource, section: 'market' },
  planningMode: { label: text.planningMode, section: 'planning' },
  planningGeneration: { label: text.planningGeneration, section: 'planning' },
  planningFramework: { label: text.planningFramework, section: 'planning' },
  planningModel: { label: text.planningModel, section: 'planning' },
  planningFallback: { label: text.planningFallback, section: 'planning' },
  focus: { label: text.focus, section: 'intent' },
  reviewPressure: { label: text.reviewPressure, section: 'intent' },
  intentGeneration: { label: text.intentGeneration, section: 'intent' },
  intentFramework: { label: text.intentFramework, section: 'intent' },
  intentModel: { label: text.intentModel, section: 'intent' },
  intentFallback: { label: text.intentFallback, section: 'intent' },
  financialGeneration: { label: text.financialGeneration, section: 'financial' },
  financialFramework: { label: text.financialFramework, section: 'financial' },
  financialModel: { label: text.financialModel, section: 'financial' },
  riskGeneration: { label: text.riskGeneration, section: 'risk' },
  riskFramework: { label: text.riskFramework, section: 'risk' },
  riskModel: { label: text.riskModel, section: 'risk' },
  modelConfigured: { label: text.modelConfigured, section: 'report' },
  reportFramework: { label: text.reportFramework, section: 'report' },
  evidenceSource: { label: text.evidenceSource, section: 'other' }
}

function normalizeText(value?: string | null) {
  return (value || '').trim()
}

const evidenceTypeTextMap: Record<string, string> = {
  SOURCE_EVENT: text.sourceEvent,
  MARKET_EVENT: text.recentMarketEvent,
  LIVE_MARKET_EVENT: text.liveMarketEvent,
  POLICY_LIVE_EVENT: text.policyLiveEvent,
  REGULATORY_RISK_LIVE_EVENT: text.regulatoryRiskLiveEvent,
  SOURCE_REPORT: text.sourceTaskReport,
  RISK_WARNING: text.risk,
  STRATEGY_SIGNAL: '策略信号',
  MARKET_INTELLIGENCE: '市场情报',
  RECENT_TASK: text.sourceTask
}

function formatEvidenceType(value?: string | null) {
  const normalized = normalizeText(value).toUpperCase()
  return evidenceTypeTextMap[normalized] || normalizeText(value)
}

function formatLiveEventEvidenceStatus(value?: string | null) {
  const normalized = normalizeText(value).toUpperCase()
  if (normalized === 'MATCHED') return '已匹配'
  if (normalized === 'MISSING') return '未匹配到结构化证据'
  if (normalized === 'NOT_APPLICABLE') return '不适用'
  return normalizeText(value)
}

function formatLiveEventRule(value?: string | null) {
  const normalized = normalizeText(value).toUpperCase()
  if (normalized === 'URL_THEN_TITLE_TIME_THEN_TITLE') {
    return '链接优先，其次标题+时间，最后标题兜底'
  }
  return normalizeText(value)
}

const evidenceRefValueFormatters: Record<string, (value?: string | null) => string> = {
  priorityLiveEventEvidenceStatus: formatLiveEventEvidenceStatus,
  priorityLiveEventEvidenceMatchRule: formatLiveEventRule
}

function normalizeEvidenceRef(rawRef: string) {
  const normalized = rawRef.trim()
  if (!normalized) return null

  const separatorIndex = normalized.indexOf(':')
  if (separatorIndex < 0) {
    return {
      sectionKey: 'other' as const,
      item: {
        label: text.rawRef,
        value: normalized,
        raw: normalized
      }
    }
  }

  const key = normalized.slice(0, separatorIndex)
  const value = normalized.slice(separatorIndex + 1).trim()
  const meta = keyMeta[key]
  const formatter = evidenceRefValueFormatters[key]

  if (!meta) {
    return {
      sectionKey: 'other' as const,
      item: {
        label: key,
        value: value || normalized,
        raw: normalized
      }
    }
  }

  return {
    sectionKey: meta.section,
    item: {
      label: meta.label,
      value: formatter ? formatter(value || normalized) : (value || '-'),
      raw: normalized
    }
  }
}

const normalizedEvidenceItems = computed<DisplayEvidenceItem[]>(() => {
  const result: DisplayEvidenceItem[] = []
  const seen = new Set<string>()

  for (const item of props.evidenceItems) {
    const evidenceId = normalizeText(item.evidenceId)
    const title = normalizeText(item.title)
    const summary = normalizeText(item.summary)
    if (!evidenceId && !title && !summary) continue

    const dedupeKey = evidenceId || `${title}::${summary}`
    if (seen.has(dedupeKey)) continue
    seen.add(dedupeKey)

    result.push({
      evidenceId,
      evidenceType: formatEvidenceType(item.evidenceType),
      source: normalizeText(item.source),
      title: title || '未命名证据',
      summary,
      url: normalizeText(item.url),
      occurredAt: normalizeText(item.occurredAt),
      referenceId: normalizeText(item.referenceId),
      relevance: normalizeText(item.relevance)
    })
  }

  return result
})

const evidenceSections = computed<EvidenceSection[]>(() => {
  const grouped = new Map<string, EvidenceRefItem[]>()

  for (const rawRef of props.evidenceRefs) {
    const parsed = normalizeEvidenceRef(rawRef)
    if (!parsed) continue
    const current = grouped.get(parsed.sectionKey) || []
    current.push(parsed.item)
    grouped.set(parsed.sectionKey, current)
  }

  return Object.values(sectionMeta)
    .map((meta) => ({
      ...meta,
      items: grouped.get(meta.key) || []
    }))
    .filter((section) => section.items.length > 0)
})

const hasContent = computed(() => {
  return normalizedEvidenceItems.value.length > 0 || evidenceSections.value.length > 0
})
</script>

<template>
  <div>
    <el-alert
      v-if="reviewSuggestion"
      :title="`${text.reviewSuggestion}：${reviewSuggestion}`"
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom: 12px;"
    />

    <el-empty
      v-if="!hasContent"
      :description="emptyText"
    />

    <div v-else>
      <el-card
        v-if="normalizedEvidenceItems.length > 0"
        shadow="never"
        style="margin-bottom: 12px;"
      >
        <template #header>
          <div style="display: flex; justify-content: space-between; align-items: center;">
            <span style="font-weight: 700;">{{ text.structuredEvidence }}</span>
            <el-tag type="success">{{ normalizedEvidenceItems.length }}</el-tag>
          </div>
        </template>

        <div
          v-for="item in normalizedEvidenceItems"
          :key="item.evidenceId || `${item.title}-${item.referenceId}`"
          style="padding: 12px 0; border-bottom: 1px solid var(--el-border-color-lighter);"
        >
          <div style="display: flex; justify-content: space-between; gap: 12px; align-items: flex-start;">
            <div style="font-weight: 600; line-height: 1.6;">{{ item.title }}</div>
            <div style="display: flex; gap: 8px; flex-wrap: wrap; justify-content: flex-end;">
              <el-tag v-if="item.evidenceType" type="primary">{{ item.evidenceType }}</el-tag>
              <el-tag v-if="item.relevance" type="warning">{{ item.relevance }}</el-tag>
            </div>
          </div>

          <div
            v-if="item.summary"
            style="margin-top: 8px; color: var(--el-text-color-regular); line-height: 1.7;"
          >
            {{ item.summary }}
          </div>

          <el-descriptions
            :column="1"
            border
            size="small"
            style="margin-top: 10px;"
          >
            <el-descriptions-item
              v-if="item.source"
              :label="text.sourceLabel"
            >
              {{ item.source }}
            </el-descriptions-item>
            <el-descriptions-item
              v-if="item.occurredAt"
              :label="text.time"
            >
              {{ item.occurredAt }}
            </el-descriptions-item>
            <el-descriptions-item
              v-if="item.referenceId"
              :label="text.referenceId"
            >
              {{ item.referenceId }}
            </el-descriptions-item>
            <el-descriptions-item
              v-if="item.url"
              :label="text.link"
            >
              <el-link :href="item.url" target="_blank" type="primary">
                {{ text.openLink }}
              </el-link>
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </el-card>

      <el-card
        v-for="section in evidenceSections"
        :key="section.key"
        shadow="never"
        style="margin-bottom: 12px;"
      >
        <template #header>
          <div style="display: flex; justify-content: space-between; align-items: center;">
            <span style="font-weight: 700;">{{ section.title }}</span>
            <el-tag :type="section.type">{{ section.items.length }}</el-tag>
          </div>
        </template>

        <el-descriptions :column="1" border>
          <el-descriptions-item
            v-for="item in section.items"
            :key="item.raw"
            :label="item.label"
          >
            {{ item.value }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>
    </div>
  </div>
</template>
