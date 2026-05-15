<script setup lang="ts">
import { computed } from 'vue'

interface ReportVersionContent {
  summary?: string | null
  highlights?: string[] | null
  riskPoints?: string[] | null
}

const props = withDefaults(defineProps<{
  original: ReportVersionContent
  current: ReportVersionContent
  originalLabel?: string
  currentLabel?: string
  emptyText?: string
}>(), {
  originalLabel: '原始版本',
  currentLabel: '当前版本',
  emptyText: '无'
})

const text = {
  summary: '摘要',
  highlights: '亮点',
  riskPoints: '风险点',
  addedHighlights: '新增亮点',
  removedHighlights: '移除亮点',
  addedRiskPoints: '新增风险点',
  removedRiskPoints: '移除风险点',
  noDiff: '当前版本与原始版本一致',
  hasDiff: '当前版本包含修订差异',
  summaryChanged: '摘要已修订',
  highlightsChanged: '亮点',
  riskPointsChanged: '风险点'
} as const

function normalizeText(value?: string | null) {
  return (value || '').trim()
}

function normalizeList(value?: string[] | null) {
  return Array.from(
    new Set(
      (value || [])
        .map((item) => item.trim())
        .filter(Boolean)
    )
  )
}

function buildRemovedItems(originalList: string[], currentList: string[]) {
  return originalList.filter((item) => !currentList.includes(item))
}

function buildAddedItems(originalList: string[], currentList: string[]) {
  return currentList.filter((item) => !originalList.includes(item))
}

const originalSummary = computed(() => normalizeText(props.original.summary))
const currentSummary = computed(() => normalizeText(props.current.summary))
const originalHighlights = computed(() => normalizeList(props.original.highlights))
const currentHighlights = computed(() => normalizeList(props.current.highlights))
const originalRiskPoints = computed(() => normalizeList(props.original.riskPoints))
const currentRiskPoints = computed(() => normalizeList(props.current.riskPoints))

const summaryChanged = computed(() => originalSummary.value !== currentSummary.value)
const addedHighlights = computed(() => buildAddedItems(originalHighlights.value, currentHighlights.value))
const removedHighlights = computed(() => buildRemovedItems(originalHighlights.value, currentHighlights.value))
const addedRiskPoints = computed(() => buildAddedItems(originalRiskPoints.value, currentRiskPoints.value))
const removedRiskPoints = computed(() => buildRemovedItems(originalRiskPoints.value, currentRiskPoints.value))

const hasChanges = computed(() => {
  return summaryChanged.value
    || addedHighlights.value.length > 0
    || removedHighlights.value.length > 0
    || addedRiskPoints.value.length > 0
    || removedRiskPoints.value.length > 0
})

const changeSummaryText = computed(() => {
  if (!hasChanges.value) {
    return text.noDiff
  }

  const summaryParts: string[] = []
  if (summaryChanged.value) {
    summaryParts.push(text.summaryChanged)
  }
  if (addedHighlights.value.length > 0 || removedHighlights.value.length > 0) {
    summaryParts.push(`${text.highlightsChanged} +${addedHighlights.value.length} / -${removedHighlights.value.length}`)
  }
  if (addedRiskPoints.value.length > 0 || removedRiskPoints.value.length > 0) {
    summaryParts.push(`${text.riskPointsChanged} +${addedRiskPoints.value.length} / -${removedRiskPoints.value.length}`)
  }
  return summaryParts.join('；')
})

function displayText(value: string) {
  return value || props.emptyText
}
</script>

<template>
  <div>
    <el-alert
      :title="changeSummaryText"
      :type="hasChanges ? 'warning' : 'success'"
      :closable="false"
      show-icon
      style="margin-bottom: 16px;"
    />

    <el-row :gutter="16">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>{{ originalLabel }}</template>

          <div style="font-weight: 700; margin-bottom: 8px;">{{ text.summary }}</div>
          <div style="white-space: pre-wrap; word-break: break-word;">{{ displayText(originalSummary) }}</div>

          <div style="font-weight: 700; margin: 16px 0 8px;">{{ text.highlights }}</div>
          <div v-if="originalHighlights.length">
            <div
              v-for="(item, index) in originalHighlights"
              :key="`original-highlight-${index}`"
              style="margin-bottom: 6px;"
            >
              {{ index + 1 }}. {{ item }}
            </div>
          </div>
          <div v-else>{{ emptyText }}</div>

          <div
            v-if="removedHighlights.length"
            style="margin-top: 12px;"
          >
            <div style="font-weight: 700; margin-bottom: 8px;">{{ text.removedHighlights }}</div>
            <el-tag
              v-for="(item, index) in removedHighlights"
              :key="`removed-highlight-${index}`"
              type="danger"
              effect="plain"
              style="margin-right: 8px; margin-bottom: 8px;"
            >
              {{ item }}
            </el-tag>
          </div>

          <div style="font-weight: 700; margin: 16px 0 8px;">{{ text.riskPoints }}</div>
          <div v-if="originalRiskPoints.length">
            <div
              v-for="(item, index) in originalRiskPoints"
              :key="`original-risk-${index}`"
              style="margin-bottom: 6px;"
            >
              {{ index + 1 }}. {{ item }}
            </div>
          </div>
          <div v-else>{{ emptyText }}</div>

          <div
            v-if="removedRiskPoints.length"
            style="margin-top: 12px;"
          >
            <div style="font-weight: 700; margin-bottom: 8px;">{{ text.removedRiskPoints }}</div>
            <el-tag
              v-for="(item, index) in removedRiskPoints"
              :key="`removed-risk-${index}`"
              type="danger"
              effect="plain"
              style="margin-right: 8px; margin-bottom: 8px;"
            >
              {{ item }}
            </el-tag>
          </div>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card shadow="never">
          <template #header>{{ currentLabel }}</template>

          <div style="font-weight: 700; margin-bottom: 8px;">{{ text.summary }}</div>
          <div style="white-space: pre-wrap; word-break: break-word;">{{ displayText(currentSummary) }}</div>

          <div style="font-weight: 700; margin: 16px 0 8px;">{{ text.highlights }}</div>
          <div v-if="currentHighlights.length">
            <div
              v-for="(item, index) in currentHighlights"
              :key="`current-highlight-${index}`"
              style="margin-bottom: 6px;"
            >
              {{ index + 1 }}. {{ item }}
            </div>
          </div>
          <div v-else>{{ emptyText }}</div>

          <div
            v-if="addedHighlights.length"
            style="margin-top: 12px;"
          >
            <div style="font-weight: 700; margin-bottom: 8px;">{{ text.addedHighlights }}</div>
            <el-tag
              v-for="(item, index) in addedHighlights"
              :key="`added-highlight-${index}`"
              type="success"
              effect="plain"
              style="margin-right: 8px; margin-bottom: 8px;"
            >
              {{ item }}
            </el-tag>
          </div>

          <div style="font-weight: 700; margin: 16px 0 8px;">{{ text.riskPoints }}</div>
          <div v-if="currentRiskPoints.length">
            <div
              v-for="(item, index) in currentRiskPoints"
              :key="`current-risk-${index}`"
              style="margin-bottom: 6px;"
            >
              {{ index + 1 }}. {{ item }}
            </div>
          </div>
          <div v-else>{{ emptyText }}</div>

          <div
            v-if="addedRiskPoints.length"
            style="margin-top: 12px;"
          >
            <div style="font-weight: 700; margin-bottom: 8px;">{{ text.addedRiskPoints }}</div>
            <el-tag
              v-for="(item, index) in addedRiskPoints"
              :key="`added-risk-${index}`"
              type="warning"
              effect="plain"
              style="margin-right: 8px; margin-bottom: 8px;"
            >
              {{ item }}
            </el-tag>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>
