<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchResearchWorkbench } from '../../api/task'
import ReportRevisionStatusTags from '../../components/report/ReportRevisionStatusTags.vue'
import ResearchWorkbenchStatsCards from '../../components/report/ResearchWorkbenchStatsCards.vue'
import type { ResearchWorkbenchData, ResearchWorkbenchDispositionSummary } from '../../types/task'
import { ANALYSIS_SCOPE, TASK_TYPE } from '../../types/taskEnums'
import { formatDateTime } from '../../utils/format'
import { resolveResearchWorkbenchActionAccess } from '../../utils/taskActionAccess'
import { buildResearchWorkbenchQuery } from '../../utils/researchWorkbench'
import {
  getHumanReviewTagType,
  getHumanReviewText,
  getPriorityTagType,
  getPriorityText,
  getReviewStatusTagType,
  getReviewStatusText,
  getRiskLevelTagType,
  getRiskLevelText,
  getSignalDirectionTagType,
  getSignalDirectionText,
  getSignalStrengthTagType,
  getSignalStrengthText,
  getTaskStageText,
  getTaskStatusTagType,
  getTaskStatusText
} from '../../utils/task'
import { buildFollowUpTaskTitle, buildTaskCreateQuery } from '../../utils/taskCreate'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const loaded = ref(false)
const data = ref<ResearchWorkbenchData | null>(null)

const text = {
  title: '智能投研工作台',
  backToSource: '返回来源页',
  targetCode: '标的代码',
  targetCodePlaceholder: '如 01929 / 600519',
  targetName: '标的名称',
  targetNamePlaceholder: '如 周大福 / 贵州茅台',
  search: '开始研究',
  reset: '重置',
  targetOverview: '标的概览',
  latestInsight: '最新投研洞察',
  dispositionOverview: '处置状态总览',
  highlights: '投资要点',
  riskPoints: '风险要点',
  recentTasks: '近期研究任务',
  dispositionTotal: '条目总数',
  riskDisposition: '风险复核',
  riskDispositionDesc: '风险预警后续复核处置概览',
  signalDisposition: '信号跟踪',
  signalDispositionDesc: '策略信号后续跟踪研究概览',
  intelligenceDisposition: '情报转任务',
  intelligenceDispositionDesc: '市场情报后续转任务处置概览',
  viewRiskCenter: '查看风险中心',
  viewSignalCenter: '查看信号中心',
  viewIntelligenceCenter: '查看情报中心',
  targetType: '标的类型',
  successTaskCount: '成功任务',
  action: '快捷操作',
  createTask: '发起研究',
  viewLatestReport: '查看最新报告',
  taskCenter: '任务中心',
  reportType: '报告类型',
  confidenceScore: '置信度',
  reviewStatus: '审核状态',
  revisionStatus: '修订情况',
  riskLevel: '风险等级',
  signalDirection: '信号方向',
  signalStrength: '信号强度',
  humanReview: '人工复核',
  reportTime: '报告时间',
  noInsight: '暂无最新投研洞察',
  noHighlights: '暂无投资要点',
  noRiskPoints: '暂无风险要点',
  taskId: '任务 ID',
  taskTitle: '任务标题',
  priority: '优先级',
  taskStatus: '任务状态',
  currentStage: '当前阶段',
  createdAt: '创建时间',
  finishTime: '完成时间',
  detail: '详情',
  report: '报告',
  emptySearch: '请输入标的代码或名称开始研究',
  emptyData: '当前标的暂无可用投研数据',
  loadFailed: '投研工作台加载失败',
  loadError: '投研工作台加载异常'
} as const

type DispositionCardKey = 'risk' | 'signal' | 'intelligence'

const defaultDispositionSummary: ResearchWorkbenchDispositionSummary = {
  totalCount: 0,
  notTrackedCount: 0,
  trackingCount: 0,
  completedCount: 0,
  failedCount: 0
}

const query = reactive({
  targetCode: '',
  targetName: ''
})

const sourcePath = computed(() => {
  const from = route.query.from
  if (typeof from === 'string' && from) {
    return from
  }
  return ''
})

function getDisplayTaskStageText(stage?: string) {
  if (stage === 'EVIDENCE_COLLECTION') {
    return '证据采集'
  }
  return getTaskStageText(stage)
}

function formatConfidence(value?: number) {
  if (value == null) {
    return '-'
  }
  return `${Math.round(value * 100)}%`
}

function resolveDispositionCount(value?: number | null) {
  return value ?? 0
}

function syncQueryFromRoute() {
  query.targetCode = route.query.targetCode ? String(route.query.targetCode) : ''
  query.targetName = route.query.targetName ? String(route.query.targetName) : ''
}

function normalizeQueryValue(value: string) {
  const normalized = value.trim()
  return normalized || undefined
}

function buildRouteQuery() {
  return buildResearchWorkbenchQuery({
    targetCode: query.targetCode,
    targetName: query.targetName,
    from: sourcePath.value
  })
}

function buildDispositionCenterQuery() {
  const routeQuery: Record<string, string> = {}
  const targetCode = normalizeQueryValue(query.targetCode)
  const targetName = normalizeQueryValue(query.targetName)
  if (targetCode) {
    routeQuery.targetCode = targetCode
  }
  if (targetName) {
    routeQuery.targetName = targetName
  }
  if (route.fullPath) {
    routeQuery.from = route.fullPath
  }
  return routeQuery
}

function isSameRouteQuery(nextQuery: { targetCode?: string, targetName?: string, from?: string }) {
  return getRouteQueryValue(route.query.targetCode) === (nextQuery.targetCode || '')
    && getRouteQueryValue(route.query.targetName) === (nextQuery.targetName || '')
    && getRouteQueryValue(route.query.from) === (nextQuery.from || '')
}

function hasSearchValue() {
  return Boolean(query.targetCode.trim() || query.targetName.trim())
}

const dispositionCards = computed(() => {
  const riskSummary = data.value?.riskDispositionSummary ?? defaultDispositionSummary
  const strategySummary = data.value?.strategySignalDispositionSummary ?? defaultDispositionSummary
  const intelligenceSummary = data.value?.marketIntelligenceDispositionSummary ?? defaultDispositionSummary

  return [
    {
      key: 'risk' as const,
      title: text.riskDisposition,
      description: text.riskDispositionDesc,
      actionLabel: text.viewRiskCenter,
      totalCount: resolveDispositionCount(riskSummary.totalCount),
      statuses: [
        { label: '未复核', count: resolveDispositionCount(riskSummary.notTrackedCount), color: '#909399' },
        { label: '复核中', count: resolveDispositionCount(riskSummary.trackingCount), color: '#e6a23c' },
        { label: '已完成', count: resolveDispositionCount(riskSummary.completedCount), color: '#67c23a' },
        { label: '复核失败', count: resolveDispositionCount(riskSummary.failedCount), color: '#f56c6c' }
      ]
    },
    {
      key: 'signal' as const,
      title: text.signalDisposition,
      description: text.signalDispositionDesc,
      actionLabel: text.viewSignalCenter,
      totalCount: resolveDispositionCount(strategySummary.totalCount),
      statuses: [
        { label: '未跟踪', count: resolveDispositionCount(strategySummary.notTrackedCount), color: '#909399' },
        { label: '跟踪中', count: resolveDispositionCount(strategySummary.trackingCount), color: '#e6a23c' },
        { label: '已完成', count: resolveDispositionCount(strategySummary.completedCount), color: '#67c23a' },
        { label: '跟踪失败', count: resolveDispositionCount(strategySummary.failedCount), color: '#f56c6c' }
      ]
    },
    {
      key: 'intelligence' as const,
      title: text.intelligenceDisposition,
      description: text.intelligenceDispositionDesc,
      actionLabel: text.viewIntelligenceCenter,
      totalCount: resolveDispositionCount(intelligenceSummary.totalCount),
      statuses: [
        { label: '未转任务', count: resolveDispositionCount(intelligenceSummary.notTrackedCount), color: '#909399' },
        { label: '处理中', count: resolveDispositionCount(intelligenceSummary.trackingCount), color: '#e6a23c' },
        { label: '已完成', count: resolveDispositionCount(intelligenceSummary.completedCount), color: '#67c23a' },
        { label: '处理失败', count: resolveDispositionCount(intelligenceSummary.failedCount), color: '#f56c6c' }
      ]
    }
  ]
})
const actionAccess = computed(() => resolveResearchWorkbenchActionAccess(data.value))

async function loadWorkbench() {
  if (!hasSearchValue()) {
    loaded.value = false
    data.value = null
    return
  }

  loading.value = true
  try {
    const res = await fetchResearchWorkbench({
      targetCode: normalizeQueryValue(query.targetCode),
      targetName: normalizeQueryValue(query.targetName)
    })
    if (res.success) {
      data.value = res.data
      loaded.value = true
    } else {
      data.value = null
      loaded.value = true
      ElMessage.error(res.message || text.loadFailed)
    }
  } catch (error: any) {
    data.value = null
    loaded.value = true
    ElMessage.error(error?.message || text.loadError)
  } finally {
    loading.value = false
  }
}

async function handleSearch() {
  const nextQuery = buildRouteQuery()
  if (isSameRouteQuery(nextQuery)) {
    await loadWorkbench()
    return
  }

  await router.replace({
    path: '/research-workbench',
    query: nextQuery
  })
}

async function handleReset() {
  query.targetCode = ''
  query.targetName = ''
  if (!route.query.targetCode && !route.query.targetName) {
    data.value = null
    loaded.value = false
    return
  }

  await router.replace({
    path: '/research-workbench',
    query: buildResearchWorkbenchQuery({
      from: sourcePath.value
    })
  })
}

function goCreateTask() {
  router.push({
    path: '/tasks/create',
      query: buildTaskCreateQuery({
        taskType: TASK_TYPE.STOCK_RESEARCH,
        taskTitle: buildFollowUpTaskTitle(data.value?.targetName, data.value?.targetCode, '深度研究'),
        targetType: data.value?.targetType || 'STOCK',
        targetCode: data.value?.targetCode,
        targetName: data.value?.targetName,
        priority: 'HIGH',
        sourceTaskId: data.value?.latestInsight?.taskId,
        sourceReportId: data.value?.latestInsight?.reportId,
        sourceDomain: 'RESEARCH_WORKBENCH',
        sourceReviewStatus: data.value?.latestInsight?.reviewStatus,
        analysisScope: ANALYSIS_SCOPE.DEEP_RESEARCH,
        from: route.fullPath
      })
    })
  }

function goLatestReport() {
  if (!data.value?.latestInsight?.taskId) {
    return
  }
  router.push({
    path: `/tasks/${data.value.latestInsight.taskId}/report`,
    query: {
      from: route.fullPath
    }
  })
}

function goTaskDetail(taskId: string) {
  router.push({
    path: `/tasks/${taskId}`,
    query: {
      from: route.fullPath
    }
  })
}

function goTaskReport(taskId: string) {
  router.push({
    path: `/tasks/${taskId}/report`,
    query: {
      from: route.fullPath
    }
  })
}

function goDispositionCenter(key: DispositionCardKey) {
  const pathMap: Record<DispositionCardKey, string> = {
    risk: '/risk-warnings',
    signal: '/signals',
    intelligence: '/intelligence'
  }
  router.push({
    path: pathMap[key],
    query: buildDispositionCenterQuery()
  })
}

function hasWorkbenchData() {
  return Boolean(data.value && (data.value.taskCount > 0 || data.value.latestInsight))
}

function getRouteQueryValue(value: unknown) {
  if (Array.isArray(value)) {
    return value[0] ? String(value[0]) : ''
  }
  return value ? String(value) : ''
}

watch([
  () => route.query.targetCode,
  () => route.query.targetName
], async () => {
  syncQueryFromRoute()
  await loadWorkbench()
}, { immediate: true })
</script>

<template>
  <div v-loading="loading">
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center; gap: 12px;">
          <div style="font-weight: 700;">{{ text.title }}</div>
          <el-button v-if="sourcePath" @click="router.push(sourcePath)">{{ text.backToSource }}</el-button>
        </div>
      </template>

      <el-form inline>
        <el-form-item :label="text.targetCode">
          <el-input v-model="query.targetCode" :placeholder="text.targetCodePlaceholder" clearable />
        </el-form-item>

        <el-form-item :label="text.targetName">
          <el-input v-model="query.targetName" :placeholder="text.targetNamePlaceholder" clearable />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSearch">{{ text.search }}</el-button>
          <el-button @click="handleReset">{{ text.reset }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <template v-if="hasWorkbenchData() && data">
      <div style="margin-top: 16px;">
        <ResearchWorkbenchStatsCards :data="data" />
      </div>

      <el-card style="margin-top: 16px;">
        <template #header>
          <div style="font-weight: 700;">{{ text.dispositionOverview }}</div>
        </template>

        <el-row :gutter="16">
          <el-col v-for="item in dispositionCards" :key="item.key" :span="8">
            <el-card shadow="never">
              <template #header>
                <div style="display: flex; justify-content: space-between; align-items: flex-start; gap: 12px;">
                  <div>
                    <div style="font-weight: 700;">{{ item.title }}</div>
                    <div style="font-size: 12px; color: #909399; margin-top: 4px;">{{ item.description }}</div>
                  </div>
                  <el-button link type="primary" @click="goDispositionCenter(item.key)">{{ item.actionLabel }}</el-button>
                </div>
              </template>

              <div style="display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 16px;">
                <div style="font-size: 13px; color: #909399;">{{ text.dispositionTotal }}</div>
                <div style="font-size: 28px; font-weight: 700;">{{ item.totalCount }}</div>
              </div>

              <div style="display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px;">
                <div
                  v-for="status in item.statuses"
                  :key="status.label"
                  style="padding: 12px; border-radius: 8px; background: #f5f7fa;"
                >
                  <div style="font-size: 12px; color: #909399; margin-bottom: 8px;">{{ status.label }}</div>
                  <div :style="{ fontSize: '22px', fontWeight: 700, color: status.color }">{{ status.count }}</div>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-card>

      <el-row :gutter="16" style="margin-top: 16px;">
        <el-col :span="8">
          <el-card>
            <template #header>
              <div style="font-weight: 700;">{{ text.targetOverview }}</div>
            </template>

            <div style="display: grid; row-gap: 12px;">
              <div><strong>{{ text.targetCode }}：</strong>{{ data.targetCode || '-' }}</div>
              <div><strong>{{ text.targetName }}：</strong>{{ data.targetName || '-' }}</div>
              <div><strong>{{ text.targetType }}：</strong>{{ data.targetType || '-' }}</div>
              <div><strong>{{ text.successTaskCount }}：</strong>{{ data.successTaskCount }}</div>
            </div>

            <div style="display: flex; gap: 12px; flex-wrap: wrap; margin-top: 20px;">
              <el-button v-if="actionAccess.showCreateTask" type="primary" @click="goCreateTask">{{ text.createTask }}</el-button>
              <el-button type="success" :disabled="!data.latestInsight?.taskId" @click="goLatestReport">{{ text.viewLatestReport }}</el-button>
              <el-button @click="router.push('/tasks')">{{ text.taskCenter }}</el-button>
            </div>
          </el-card>
        </el-col>

        <el-col :span="16">
          <el-card>
            <template #header>
              <div style="font-weight: 700;">{{ text.latestInsight }}</div>
            </template>

            <template v-if="data.latestInsight">
              <div style="display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 12px;">
                <el-tag v-if="data.latestInsight.reportType" type="info">{{ data.latestInsight.reportType }}</el-tag>
                <el-tag v-if="data.latestInsight.signalDirection" :type="getSignalDirectionTagType(data.latestInsight.signalDirection)">
                  {{ getSignalDirectionText(data.latestInsight.signalDirection) }}
                </el-tag>
                <el-tag v-if="data.latestInsight.signalStrength" :type="getSignalStrengthTagType(data.latestInsight.signalStrength)">
                  {{ getSignalStrengthText(data.latestInsight.signalStrength) }}
                </el-tag>
                <el-tag v-if="data.latestInsight.riskLevel" :type="getRiskLevelTagType(data.latestInsight.riskLevel)">
                  {{ getRiskLevelText(data.latestInsight.riskLevel) }}
                </el-tag>
                <el-tag :type="getReviewStatusTagType(data.latestInsight.reviewStatus)">
                  {{ getReviewStatusText(data.latestInsight.reviewStatus) }}
                </el-tag>
                <el-tag :type="getHumanReviewTagType(data.latestInsight.needHumanReview)">
                  {{ getHumanReviewText(data.latestInsight.needHumanReview) }}
                </el-tag>
              </div>

              <div style="display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; margin-bottom: 16px;">
                <div><strong>{{ text.confidenceScore }}：</strong>{{ formatConfidence(data.latestInsight.confidenceScore) }}</div>
                <div><strong>{{ text.reviewStatus }}：</strong>{{ getReviewStatusText(data.latestInsight.reviewStatus) }}</div>
                <div><strong>{{ text.reportTime }}：</strong>{{ formatDateTime(data.latestInsight.createdAt) }}</div>
              </div>

              <div style="margin-bottom: 16px;">
                <strong>{{ text.revisionStatus }}：</strong>
                <div style="margin-top: 8px;">
                  <ReportRevisionStatusTags
                    :revised="data.latestInsight.revised"
                    :summary-revised="data.latestInsight.summaryRevised"
                    :highlights-revised="data.latestInsight.highlightsRevised"
                    :risk-points-revised="data.latestInsight.riskPointsRevised"
                  />
                </div>
              </div>

              <div style="line-height: 1.8; color: #303133; white-space: pre-wrap;">{{ data.latestInsight.summary || text.noInsight }}</div>
            </template>

            <el-empty v-else :description="text.noInsight" />
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="16" style="margin-top: 16px;">
        <el-col :span="12">
          <el-card>
            <template #header>
              <div style="font-weight: 700;">{{ text.highlights }}</div>
            </template>
            <div v-if="data.latestInsight?.highlights?.length" style="display:flex; gap:8px; flex-wrap:wrap;">
              <el-tag v-for="item in data.latestInsight.highlights" :key="item" type="success" effect="plain">{{ item }}</el-tag>
            </div>
            <el-empty v-else :description="text.noHighlights" />
          </el-card>
        </el-col>

        <el-col :span="12">
          <el-card>
            <template #header>
              <div style="font-weight: 700;">{{ text.riskPoints }}</div>
            </template>
            <div v-if="data.latestInsight?.riskPoints?.length" style="display:flex; gap:8px; flex-wrap:wrap;">
              <el-tag v-for="item in data.latestInsight.riskPoints" :key="item" type="danger" effect="plain">{{ item }}</el-tag>
            </div>
            <el-empty v-else :description="text.noRiskPoints" />
          </el-card>
        </el-col>
      </el-row>

      <el-card style="margin-top: 16px;">
        <template #header>
          <div style="font-weight: 700;">{{ text.recentTasks }}</div>
        </template>

        <el-table :data="data.recentTasks" border>
          <el-table-column prop="taskId" :label="text.taskId" min-width="220" />
          <el-table-column prop="taskTitle" :label="text.taskTitle" min-width="180" />

          <el-table-column :label="text.priority" width="90">
            <template #default="{ row }">
              <el-tag :type="getPriorityTagType(row.priority)">{{ getPriorityText(row.priority) }}</el-tag>
            </template>
          </el-table-column>

          <el-table-column :label="text.taskStatus" width="110">
            <template #default="{ row }">
              <el-tag :type="getTaskStatusTagType(row.status)">{{ getTaskStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>

          <el-table-column :label="text.currentStage" width="150">
            <template #default="{ row }">
              {{ getDisplayTaskStageText(row.currentStage) }}
            </template>
          </el-table-column>

          <el-table-column :label="text.reviewStatus" width="120">
            <template #default="{ row }">
              <el-tag v-if="row.reportReviewStatus" :type="getReviewStatusTagType(row.reportReviewStatus)">
                {{ getReviewStatusText(row.reportReviewStatus) }}
              </el-tag>
              <span v-else>-</span>
            </template>
          </el-table-column>

          <el-table-column :label="text.revisionStatus" min-width="180">
            <template #default="{ row }">
              <ReportRevisionStatusTags
                v-if="row.reportId"
                :revised="row.revised"
                :summary-revised="row.summaryRevised"
                :highlights-revised="row.highlightsRevised"
                :risk-points-revised="row.riskPointsRevised"
              />
              <span v-else>-</span>
            </template>
          </el-table-column>

          <el-table-column :label="text.confidenceScore" width="100">
            <template #default="{ row }">
              {{ formatConfidence(row.confidenceScore) }}
            </template>
          </el-table-column>

          <el-table-column :label="text.createdAt" width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.createdAt) }}
            </template>
          </el-table-column>

          <el-table-column :label="text.finishTime" width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.finishTime) }}
            </template>
          </el-table-column>

          <el-table-column :label="text.action" width="160" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="goTaskDetail(row.taskId)">{{ text.detail }}</el-button>
              <el-button link type="success" :disabled="!row.reportReviewStatus && row.confidenceScore == null" @click="goTaskReport(row.taskId)">{{ text.report }}</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>

    <el-empty v-else-if="loaded" style="margin-top: 24px;" :description="text.emptyData" />
    <el-empty v-else style="margin-top: 24px;" :description="text.emptySearch" />
  </div>
</template>
