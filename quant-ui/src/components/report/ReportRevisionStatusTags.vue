<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  revised?: boolean
  summaryRevised?: boolean
  highlightsRevised?: boolean
  riskPointsRevised?: boolean
  emptyText?: string
}>(), {
  revised: false,
  summaryRevised: false,
  highlightsRevised: false,
  riskPointsRevised: false,
  emptyText: '未修订'
})

const text = {
  revised: '已修订',
  summary: '摘要',
  highlights: '亮点',
  riskPoints: '风险点'
} as const

const changedSections = computed(() => {
  const sections: string[] = []
  if (props.summaryRevised) sections.push(text.summary)
  if (props.highlightsRevised) sections.push(text.highlights)
  if (props.riskPointsRevised) sections.push(text.riskPoints)
  return sections
})
</script>

<template>
  <div>
    <template v-if="revised">
      <el-tag type="warning" style="margin-right: 8px; margin-bottom: 8px;">
        {{ text.revised }}
      </el-tag>
      <el-tag
        v-for="section in changedSections"
        :key="section"
        type="info"
        effect="plain"
        style="margin-right: 8px; margin-bottom: 8px;"
      >
        {{ section }}
      </el-tag>
    </template>

    <el-tag v-else type="success" effect="plain">
      {{ emptyText }}
    </el-tag>
  </div>
</template>
