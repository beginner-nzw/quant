<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { decideHumanReview, fetchHumanReviewQueue, fetchHumanReviewQueueStats } from '../../api/task'
import type { HumanReviewQueueItem, HumanReviewQueueStats } from '../../types/task'
import { REPORT_REVIEW_STATUS, type ReportReviewStatus } from '../../types/taskEnums'
import { formatDateTime } from '../../utils/format'
import { getCurrentUser } from '../../utils/auth'
import { getReviewStatusTagType, getReviewStatusText } from '../../utils/task'

const props = withDefaults(defineProps<{
  domain?: string
}>(), {
  domain: ''
})

const emit = defineEmits<{
  reviewed: []
}>()

const loading = ref(false)
const decisionLoading = ref(false)
const records = ref<HumanReviewQueueItem[]>([])
const stats = ref<HumanReviewQueueStats | null>(null)
const dialogVisible = ref(false)
const selected = ref<HumanReviewQueueItem | null>(null)

const query = reactive({
  pageNum: 1,
  pageSize: 8,
  domain: props.domain,
  onlyPending: true
})

const form = reactive({
  decision: REPORT_REVIEW_STATUS.APPROVED as ReportReviewStatus,
  reviewComment: '',
  revisedSummary: '',
  revisedHighlightsText: '',
  revisedRiskPointsText: '',
  rerunWorkflow: false,
  rerunNodeName: ''
})

const text = {
  title: '人工复核队列',
  pending: '待处理',
  approved: '已通过',
  rejected: '已驳回',
  total: '全部',
  refresh: '刷新',
  approve: '通过',
  reject: '驳回',
  revise: '修订',
  decision: '处理结果',
  comment: '复核意见',
  revisedSummary: '修订摘要',
  revisedHighlights: '修订亮点',
  revisedRiskPoints: '修订风险点',
  rerunWorkflow: '重新运行节点',
  rerunNodeName: '节点名称',
  submit: '提交',
  cancel: '取消',
  empty: '暂无待复核项',
  saved: '复核处理已提交',
  failed: '复核处理失败'
} as const

const statCards = computed(() => [
  { label: text.pending, value: stats.value?.pendingCount ?? 0 },
  { label: text.approved, value: stats.value?.approvedCount ?? 0 },
  { label: text.rejected, value: stats.value?.rejectedCount ?? 0 },
  { label: text.total, value: stats.value?.totalCount ?? 0 }
])

async function loadQueue() {
  loading.value = true
  try {
    const [queueRes, statsRes] = await Promise.all([
      fetchHumanReviewQueue({
        pageNum: query.pageNum,
        pageSize: query.pageSize,
        domain: query.domain || undefined,
        onlyPending: query.onlyPending
      }),
      fetchHumanReviewQueueStats()
    ])
    if (!queueRes.success) {
      records.value = []
      ElMessage.error(queueRes.message || '人工复核队列加载失败')
      return
    }
    if (!statsRes.success) {
      ElMessage.error(statsRes.message || '人工复核统计加载失败')
    }
    records.value = queueRes.data?.records || []
    stats.value = statsRes.data || null
  } catch (e: any) {
    records.value = []
    stats.value = null
    ElMessage.error(getExceptionMessage(e, '人工复核队列请求异常'))
  } finally {
    loading.value = false
  }
}

function openDecision(row: HumanReviewQueueItem, decision: ReportReviewStatus) {
  selected.value = row
  form.decision = decision
  form.reviewComment = row.reviewComment || ''
  form.revisedSummary = row.summary || ''
  form.revisedHighlightsText = ''
  form.revisedRiskPointsText = (row.riskPoints || []).join('\n')
  form.rerunWorkflow = false
  form.rerunNodeName = row.currentNode || 'report_generation_agent'
  dialogVisible.value = true
}

async function submitDecision() {
  if (!selected.value) return
  decisionLoading.value = true
  try {
    const res = await decideHumanReview(selected.value.queueId, {
      decision: form.decision,
      reviewedBy: getCurrentUser().userId,
      reviewComment: form.reviewComment,
      revisedSummary: form.revisedSummary,
      revisedHighlights: splitLines(form.revisedHighlightsText),
      revisedRiskPoints: splitLines(form.revisedRiskPointsText),
      rerunWorkflow: form.rerunWorkflow,
      rerunNodeName: form.rerunNodeName
    })
    if (!res.success) {
      ElMessage.error(res.message || text.failed)
      return
    }
    ElMessage.success(text.saved)
    dialogVisible.value = false
    emit('reviewed')
    await loadQueue()
  } catch (e: any) {
    ElMessage.error(getExceptionMessage(e, '人工复核提交请求异常'))
  } finally {
    decisionLoading.value = false
  }
}

function splitLines(value: string) {
  return value.split(/\r?\n/).map((item) => item.trim()).filter(Boolean)
}

function domainTagType(domain?: string) {
  if (domain === 'RISK') return 'danger'
  if (domain === 'COMPLIANCE') return 'warning'
  return 'primary'
}

function getExceptionMessage(error: any, fallback: string) {
  return error?.response?.data?.message || error?.message || fallback
}

onMounted(loadQueue)
</script>

<template>
  <el-card shadow="never" class="human-review-panel">
    <template #header>
      <div class="review-header">
        <div>
          <strong>{{ text.title }}</strong>
          <div class="review-stats">
            <span v-for="item in statCards" :key="item.label">{{ item.label }} {{ item.value }}</span>
          </div>
        </div>
        <el-button :loading="loading" @click="loadQueue">{{ text.refresh }}</el-button>
      </div>
    </template>

    <el-empty v-if="!loading && records.length === 0" :description="text.empty" />
    <el-table v-else v-loading="loading" :data="records" border>
      <el-table-column label="领域" width="110">
        <template #default="{ row }">
          <el-tag :type="domainTagType(row.domain)">{{ row.domain }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="targetCode" label="标的" width="150">
        <template #default="{ row }">
          <div>{{ row.targetCode || '-' }}</div>
          <small>{{ row.targetName || '-' }}</small>
        </template>
      </el-table-column>
      <el-table-column prop="taskTitle" label="任务 / 摘要" min-width="280">
        <template #default="{ row }">
          <div class="task-title">{{ row.taskTitle || row.taskId }}</div>
          <div class="summary-line">{{ row.summary || '-' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="getReviewStatusTagType(row.reviewStatus)">
            {{ getReviewStatusText(row.reviewStatus || REPORT_REVIEW_STATUS.PENDING) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="进入时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="210" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="success" @click="openDecision(row, REPORT_REVIEW_STATUS.APPROVED)">
            {{ text.approve }}
          </el-button>
          <el-button size="small" type="warning" @click="openDecision(row, REPORT_REVIEW_STATUS.PENDING)">
            {{ text.revise }}
          </el-button>
          <el-button size="small" type="danger" @click="openDecision(row, REPORT_REVIEW_STATUS.REJECTED)">
            {{ text.reject }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="selected?.taskTitle || text.title" width="720px">
      <el-form label-width="110px">
        <el-form-item :label="text.decision">
          <el-select v-model="form.decision" style="width: 220px;">
            <el-option :value="REPORT_REVIEW_STATUS.APPROVED" :label="getReviewStatusText(REPORT_REVIEW_STATUS.APPROVED)" />
            <el-option :value="REPORT_REVIEW_STATUS.PENDING" :label="getReviewStatusText(REPORT_REVIEW_STATUS.PENDING)" />
            <el-option :value="REPORT_REVIEW_STATUS.REJECTED" :label="getReviewStatusText(REPORT_REVIEW_STATUS.REJECTED)" />
          </el-select>
        </el-form-item>
        <el-form-item :label="text.comment">
          <el-input v-model="form.reviewComment" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item :label="text.revisedSummary">
          <el-input v-model="form.revisedSummary" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item :label="text.revisedHighlights">
          <el-input v-model="form.revisedHighlightsText" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item :label="text.revisedRiskPoints">
          <el-input v-model="form.revisedRiskPointsText" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item :label="text.rerunWorkflow">
          <el-switch v-model="form.rerunWorkflow" />
        </el-form-item>
        <el-form-item v-if="form.rerunWorkflow" :label="text.rerunNodeName">
          <el-input v-model="form.rerunNodeName" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ text.cancel }}</el-button>
        <el-button type="primary" :loading="decisionLoading" @click="submitDecision">{{ text.submit }}</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<style scoped>
.human-review-panel {
  margin-bottom: 16px;
}

.review-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.review-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 6px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.task-title {
  font-weight: 700;
}

.summary-line {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
