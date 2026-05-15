<script setup lang="ts">
import { computed } from 'vue'
import ReportEvidenceView from '../report/ReportEvidenceView.vue'
import type { TaskReport } from '../../types/task'
import {
  getHumanReviewText,
  getTaskStatusTagType,
  getTaskStatusText,
  getTaskTypeText
} from '../../utils/task'

const props = defineProps<{
  report?: TaskReport | null
}>()

const text = {
  title: '报告结果',
  taskType: '任务类型',
  finalStatus: '最终状态',
  confidenceScore: '置信度',
  needHumanReview: '人工复核',
  reportType: '报告类型',
  resultRef: '结果引用',
  summary: '摘要',
  highlights: '关键亮点',
  emptyHighlights: '暂无关键亮点',
  riskPoints: '风险点',
  emptyRiskPoints: '暂无风险点',
  riskWarnings: '风险预警',
  emptyRiskWarnings: '暂无风险预警',
  contextSnapshot: '上下文快照',
  emptyContextSnapshot: '暂无上下文快照',
  evidenceRefs: '证据链',
  emptyEvidenceRefs: '暂无证据链',
  empty: '当前暂无报告结果',
  taskContextSource: '任务上下文来源',
  marketDataSource: '市场快照来源',
  sourceTaskId: '来源任务',
  sourceReportId: '来源报告',
  sourceEventId: '来源事件',
  sourceEventTitle: '来源事件标题',
  sourceEventType: '来源事件类型',
  sourceEventImpactLevel: '来源事件影响等级',
  sourceEventOccurredAt: '来源事件发生时间',
  latestInsightReportId: '最新洞察报告',
  taskCount: '同标的任务数',
  reportCount: '同标的报告数',
  pendingReviewCount: '待审核报告数',
  liveMarketEventSourceCode: '实时事件源编码',
  liveMarketEventSourceName: '实时事件源',
  liveEventCount: '实时事件数',
  policyLiveEventCount: '政策实时事件数',
  regulatoryRiskLiveEventCount: '监管风险事件数',
  latestLiveEventTitle: '最新实时事件标题',
  latestLiveEventOccurredAt: '最新实时事件时间',
  latestLiveEventImpactLevel: '最新实时事件影响等级',
  latestLiveEventSourceUrl: '最新实时事件链接',
  priorityLiveEventTitle: '优先实时事件标题',
  priorityLiveEventOccurredAt: '优先实时事件时间',
  priorityLiveEventImpactLevel: '优先实时事件影响等级',
  priorityLiveEventSourceUrl: '优先实时事件链接',
  priorityLiveEventEvidenceId: '优先实时事件证据',
  priorityLiveEventReferenceId: '优先实时事件引用',
  priorityLiveEventEvidenceSource: '优先实时事件证据源',
  priorityLiveEventEvidenceStatus: '优先实时事件证据状态',
  priorityLiveEventEvidenceMatchRule: '优先实时事件证据匹配规则',
  priorityLiveEventTitles: '优先实时事件标题组',
  highImpactLiveEventCount: '高影响实时事件数',
  mediumImpactLiveEventCount: '中影响实时事件数',
  lowImpactLiveEventCount: '低影响实时事件数',
  highImpactLiveEventClusterDate: '高影响事件簇日期',
  highImpactLiveEventClusterCount: '高影响事件簇数量',
  highImpactLiveEventClusterTitles: '高影响事件簇标题组',
  liveEventPriorityRule: '实时事件优先规则',
  liveEventHighlights: '实时事件摘要',
  policyLiveEventHighlights: '政策事件摘要',
  regulatoryRiskLiveEventHighlights: '监管风险事件摘要',
  priorityExternalRiskEventSummary: '优先外部风险事件',
  summaryLeadAnchors: '摘要前导锚点',
  summaryLeadAnchorsCovered: '摘要前半段锚点覆盖',
  summaryLeadCoverageStatus: '摘要前半段覆盖状态',
  highlightLeadAnchors: '亮点前导锚点',
  highlightLeadAnchorsCovered: '亮点前两条锚点覆盖',
  highlightLeadCoverageStatus: '亮点前两条覆盖状态',
  liveEventSummaryAnchored: '摘要实时事件增强',
  liveEventSummaryAnchor: '摘要实时事件锚点',
  liveEventSummaryAnchorStatus: '摘要实时事件状态',
  liveEventHighlightAnchored: '亮点实时事件增强',
  liveEventHighlightAnchor: '亮点实时事件锚点',
  liveEventHighlightAnchorStatus: '亮点实时事件状态',
  evidenceSources: '证据来源',
  stepCount: '步骤总数',
  agentCount: 'Agent 数量',
  planningMode: '规划模式',
  planningLlmFramework: '规划节点 LLM 框架',
  planningModelName: '规划节点模型',
  planningGenerationMode: '规划节点生成模式',
  planningFallbackReason: '规划节点回退原因',
  focusDimensions: '关注维度',
  reviewPressure: '审核压力',
  intentLlmFramework: '意图节点 LLM 框架',
  intentModelName: '意图节点模型',
  intentGenerationMode: '意图节点生成模式',
  intentFallbackReason: '意图节点回退原因',
  llmFramework: 'LLM 框架',
  modelName: '模型名称',
  generationMode: '生成模式',
  reportGenerationPath: '报告生成路径',
  reportFallbackReason: '报告回退原因'
} as const

const displaySummary = computed(() => {
  if (!props.report) return ''
  return props.report.displaySummary
    || props.report.revisedSummary
    || props.report.summary
    || props.report.originalSummary
    || props.report.reportMeta?.summary
    || ''
})

const displayHighlights = computed(() => {
  if (!props.report) return []
  if (props.report.displayHighlights && props.report.displayHighlights.length > 0) {
    return props.report.displayHighlights
  }
  if (props.report.revisedHighlights && props.report.revisedHighlights.length > 0) {
    return props.report.revisedHighlights
  }
  return props.report.originalHighlights || props.report.reportMeta?.highlights || []
})

const displayRiskPoints = computed(() => {
  if (!props.report) return []
  if (props.report.displayRiskPoints && props.report.displayRiskPoints.length > 0) {
    return props.report.displayRiskPoints
  }
  if (props.report.revisedRiskPoints && props.report.revisedRiskPoints.length > 0) {
    return props.report.revisedRiskPoints
  }
  return props.report.originalRiskPoints || props.report.reportMeta?.riskPoints || []
})

const riskWarnings = computed(() => props.report?.riskWarnings || [])
const evidenceItems = computed(() => props.report?.evidenceItems || [])
const evidenceRefs = computed(() => props.report?.evidenceRefs || [])
const reviewSuggestion = computed(() => props.report?.reviewSuggestion || '')

function normalizeTextValue(value?: string | null) {
  return (value || '').trim()
}

function formatImpactLevel(value?: string | null) {
  const normalized = normalizeTextValue(value).toUpperCase()
  if (normalized === 'HIGH') return '高'
  if (normalized === 'MEDIUM') return '中'
  if (normalized === 'LOW') return '低'
  return normalizeTextValue(value)
}

function formatLiveEventAnchorStatus(value?: string | null) {
  const normalized = normalizeTextValue(value).toUpperCase()
  if (normalized === 'MODEL_NATIVE') return '模型前置命中'
  if (normalized === 'POST_PROCESS_ANCHORED') return '后处理前置补位'
  if (normalized === 'COVERAGE_GAP') return '前置覆盖缺口'
  if (normalized === 'NOT_APPLICABLE') return '不适用'
  return normalizeTextValue(value)
}

function formatCoverageFlag(value?: boolean) {
  if (value === undefined || value === null) return undefined
  return value ? '已覆盖' : '未覆盖'
}

function formatLiveEventEvidenceStatus(value?: string | null) {
  const normalized = normalizeTextValue(value).toUpperCase()
  if (normalized === 'MATCHED') return '已匹配'
  if (normalized === 'MISSING') return '未匹配到结构化证据'
  if (normalized === 'NOT_APPLICABLE') return '不适用'
  return normalizeTextValue(value)
}

function formatLiveEventRule(value?: string | null) {
  const normalized = normalizeTextValue(value).toUpperCase()
  if (normalized === 'HIGH_IMPACT_CLUSTER_FIRST_THEN_IMPACT_DESC_THEN_TITLE_DESC_THEN_TIME_DESC') {
    return '高影响事件簇优先，其次影响等级、标题权重、时间倒序'
  }
  if (normalized === 'URL_THEN_TITLE_TIME_THEN_TITLE') {
    return '链接优先，其次标题+时间，最后标题兜底'
  }
  return normalizeTextValue(value)
}

const contextItems = computed(() => {
  const snapshot = props.report?.contextSnapshot
  if (!snapshot) return []

  const taskSummary = snapshot.taskSummary || {}
  const items = [
    { label: text.taskContextSource, value: snapshot.taskContextSource },
    { label: text.marketDataSource, value: snapshot.marketDataSource },
    { label: text.sourceTaskId, value: snapshot.sourceTaskId },
    { label: text.sourceReportId, value: snapshot.sourceReportId },
    { label: text.sourceEventId, value: snapshot.sourceEventId },
    { label: text.sourceEventTitle, value: snapshot.sourceEventTitle },
    { label: text.sourceEventType, value: snapshot.sourceEventType },
    { label: text.sourceEventImpactLevel, value: formatImpactLevel(snapshot.sourceEventImpactLevel) },
    { label: text.sourceEventOccurredAt, value: snapshot.sourceEventOccurredAt },
    { label: text.latestInsightReportId, value: snapshot.latestInsightReportId },
    { label: text.taskCount, value: snapshot.taskCount },
    { label: text.reportCount, value: snapshot.reportCount },
    { label: text.pendingReviewCount, value: snapshot.pendingReviewCount },
    { label: text.liveMarketEventSourceCode, value: snapshot.liveMarketEventSourceCode },
    { label: text.liveMarketEventSourceName, value: snapshot.liveMarketEventSourceName },
    { label: text.liveEventCount, value: snapshot.liveEventCount },
    { label: text.policyLiveEventCount, value: snapshot.policyLiveEventCount },
    { label: text.regulatoryRiskLiveEventCount, value: snapshot.regulatoryRiskLiveEventCount },
    { label: text.latestLiveEventTitle, value: snapshot.latestLiveEventTitle },
    { label: text.latestLiveEventOccurredAt, value: snapshot.latestLiveEventOccurredAt },
    { label: text.latestLiveEventImpactLevel, value: formatImpactLevel(snapshot.latestLiveEventImpactLevel) },
    { label: text.latestLiveEventSourceUrl, value: snapshot.latestLiveEventSourceUrl },
    { label: text.priorityLiveEventTitle, value: snapshot.priorityLiveEventTitle },
    { label: text.priorityLiveEventOccurredAt, value: snapshot.priorityLiveEventOccurredAt },
    { label: text.priorityLiveEventImpactLevel, value: formatImpactLevel(snapshot.priorityLiveEventImpactLevel) },
    { label: text.priorityLiveEventSourceUrl, value: snapshot.priorityLiveEventSourceUrl },
    { label: text.priorityLiveEventEvidenceId, value: snapshot.priorityLiveEventEvidenceId },
    { label: text.priorityLiveEventReferenceId, value: snapshot.priorityLiveEventReferenceId },
    { label: text.priorityLiveEventEvidenceSource, value: snapshot.priorityLiveEventEvidenceSource },
    { label: text.priorityLiveEventEvidenceStatus, value: formatLiveEventEvidenceStatus(snapshot.priorityLiveEventEvidenceStatus) },
    { label: text.priorityLiveEventEvidenceMatchRule, value: formatLiveEventRule(snapshot.priorityLiveEventEvidenceMatchRule) },
    {
      label: text.priorityLiveEventTitles,
      value: Array.isArray(snapshot.priorityLiveEventTitles) ? snapshot.priorityLiveEventTitles.join('；') : undefined
    },
    { label: text.highImpactLiveEventCount, value: snapshot.highImpactLiveEventCount },
    { label: text.mediumImpactLiveEventCount, value: snapshot.mediumImpactLiveEventCount },
    { label: text.lowImpactLiveEventCount, value: snapshot.lowImpactLiveEventCount },
    { label: text.highImpactLiveEventClusterDate, value: snapshot.highImpactLiveEventClusterDate },
    { label: text.highImpactLiveEventClusterCount, value: snapshot.highImpactLiveEventClusterCount },
    {
      label: text.highImpactLiveEventClusterTitles,
      value: Array.isArray(snapshot.highImpactLiveEventClusterTitles) ? snapshot.highImpactLiveEventClusterTitles.join('；') : undefined
    },
    { label: text.liveEventPriorityRule, value: formatLiveEventRule(snapshot.liveEventPriorityRule) },
    {
      label: text.liveEventHighlights,
      value: Array.isArray(snapshot.liveEventHighlights) ? snapshot.liveEventHighlights.join('；') : undefined
    },
    {
      label: text.policyLiveEventHighlights,
      value: Array.isArray(snapshot.policyLiveEventHighlights) ? snapshot.policyLiveEventHighlights.join('；') : undefined
    },
    {
      label: text.regulatoryRiskLiveEventHighlights,
      value: Array.isArray(snapshot.regulatoryRiskLiveEventHighlights) ? snapshot.regulatoryRiskLiveEventHighlights.join('；') : undefined
    },
    { label: text.priorityExternalRiskEventSummary, value: snapshot.priorityExternalRiskEventSummary },
    {
      label: text.summaryLeadAnchors,
      value: Array.isArray(snapshot.summaryLeadAnchors) ? snapshot.summaryLeadAnchors.join('；') : undefined
    },
    {
      label: text.summaryLeadAnchorsCovered,
      value: formatCoverageFlag(snapshot.summaryLeadAnchorsCovered)
    },
    {
      label: text.summaryLeadCoverageStatus,
      value: formatLiveEventAnchorStatus(snapshot.summaryLeadCoverageStatus)
    },
    {
      label: text.highlightLeadAnchors,
      value: Array.isArray(snapshot.highlightLeadAnchors) ? snapshot.highlightLeadAnchors.join('；') : undefined
    },
    {
      label: text.highlightLeadAnchorsCovered,
      value: formatCoverageFlag(snapshot.highlightLeadAnchorsCovered)
    },
    {
      label: text.highlightLeadCoverageStatus,
      value: formatLiveEventAnchorStatus(snapshot.highlightLeadCoverageStatus)
    },
    { label: text.liveEventSummaryAnchored, value: snapshot.liveEventSummaryAnchored ? '已增强' : undefined },
    { label: text.liveEventSummaryAnchor, value: snapshot.liveEventSummaryAnchor },
    { label: text.liveEventSummaryAnchorStatus, value: formatLiveEventAnchorStatus(snapshot.liveEventSummaryAnchorStatus) },
    { label: text.liveEventHighlightAnchored, value: snapshot.liveEventHighlightAnchored ? '已增强' : undefined },
    { label: text.liveEventHighlightAnchor, value: snapshot.liveEventHighlightAnchor },
    { label: text.liveEventHighlightAnchorStatus, value: formatLiveEventAnchorStatus(snapshot.liveEventHighlightAnchorStatus) },
    {
      label: text.evidenceSources,
      value: Array.isArray(snapshot.evidenceSources) ? snapshot.evidenceSources.join('；') : undefined
    },
    { label: text.stepCount, value: taskSummary.stepCount },
    { label: text.agentCount, value: taskSummary.agentCount },
    { label: text.planningMode, value: snapshot.planningMode },
    { label: text.planningLlmFramework, value: snapshot.planningLlmFramework },
    { label: text.planningModelName, value: snapshot.planningModelName },
    { label: text.planningGenerationMode, value: snapshot.planningGenerationMode },
    { label: text.planningFallbackReason, value: snapshot.planningFallbackReason },
    {
      label: text.focusDimensions,
      value: Array.isArray(snapshot.focusDimensions) ? snapshot.focusDimensions.join(' / ') : undefined
    },
    { label: text.reviewPressure, value: snapshot.reviewPressure },
    { label: text.intentLlmFramework, value: snapshot.intentLlmFramework },
    { label: text.intentModelName, value: snapshot.intentModelName },
    { label: text.intentGenerationMode, value: snapshot.intentGenerationMode },
    { label: text.intentFallbackReason, value: snapshot.intentFallbackReason },
    { label: text.llmFramework, value: snapshot.llmFramework },
    { label: text.modelName, value: snapshot.modelName },
    { label: text.generationMode, value: snapshot.generationMode },
    { label: text.reportGenerationPath, value: snapshot.reportGenerationPath },
    { label: text.reportFallbackReason, value: snapshot.reportFallbackReason }
  ]

  return items.filter((item) => item.value !== undefined && item.value !== null && item.value !== '')
})
</script>

<template>
  <section class="detail-panel report-panel">
    <div class="detail-panel-heading">
      <span>Report Output</span>
      <strong>{{ text.title }}</strong>
    </div>

    <template v-if="report">
      <el-descriptions :column="2" border>
        <el-descriptions-item :label="text.taskType">
          {{ getTaskTypeText(report.taskType) }}
        </el-descriptions-item>
        <el-descriptions-item :label="text.finalStatus">
          <el-tag :type="getTaskStatusTagType(report.finalStatus)">
            {{ getTaskStatusText(report.finalStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="text.confidenceScore">
          {{ report.confidenceScore ?? '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="text.needHumanReview">
          {{ getHumanReviewText(report.needHumanReview) }}
        </el-descriptions-item>
        <el-descriptions-item :label="text.reportType">
          {{ report.reportType || report.reportMeta?.reportType || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="text.resultRef">
          {{ report.resultRef || '-' }}
        </el-descriptions-item>
      </el-descriptions>

      <div class="report-block">
        <div class="report-block-title">{{ text.summary }}</div>
        <div class="report-summary">
          {{ displaySummary || '-' }}
        </div>
      </div>

      <div class="report-block">
        <div class="report-block-title">{{ text.highlights }}</div>
        <el-empty
          v-if="displayHighlights.length === 0"
          :description="text.emptyHighlights"
        />
        <el-timeline v-else>
          <el-timeline-item
            v-for="(item, index) in displayHighlights"
            :key="`highlight-${index}`"
          >
            {{ item }}
          </el-timeline-item>
        </el-timeline>
      </div>

      <div class="report-block">
        <div class="report-block-title">{{ text.riskPoints }}</div>
        <el-empty
          v-if="displayRiskPoints.length === 0"
          :description="text.emptyRiskPoints"
        />
        <div v-else>
          <el-alert
            v-for="(item, index) in displayRiskPoints"
            :key="`risk-${index}`"
            :title="item"
            type="warning"
            :closable="false"
            show-icon
            class="report-alert"
          />
        </div>
      </div>

      <div class="report-block">
        <div class="report-block-title">{{ text.riskWarnings }}</div>
        <el-empty
          v-if="riskWarnings.length === 0"
          :description="text.emptyRiskWarnings"
        />
        <div v-else>
          <el-alert
            v-for="(item, index) in riskWarnings"
            :key="`warning-${index}`"
            :title="item"
            type="error"
            :closable="false"
            show-icon
            class="report-alert"
          />
        </div>
      </div>

      <div class="report-block">
        <div class="report-block-title">{{ text.contextSnapshot }}</div>
        <el-empty
          v-if="contextItems.length === 0"
          :description="text.emptyContextSnapshot"
        />
        <el-descriptions
          v-else
          :column="1"
          border
        >
          <el-descriptions-item
            v-for="item in contextItems"
            :key="item.label"
            :label="item.label"
          >
            {{ item.value }}
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <div class="report-block">
        <div class="report-block-title">{{ text.evidenceRefs }}</div>
        <ReportEvidenceView
          :evidence-items="evidenceItems"
          :evidence-refs="evidenceRefs"
          :review-suggestion="reviewSuggestion"
          :empty-text="text.emptyEvidenceRefs"
        />
      </div>
    </template>

    <el-empty
      v-else
      :description="text.empty"
    />
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

.report-block {
  margin-top: 16px;
}

.report-block-title {
  margin-bottom: 8px;
  color: #142033;
  font-weight: 900;
}

.report-summary {
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}

.report-alert {
  margin-bottom: 8px;
}
</style>
