<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { createTask, fetchTaskFullDetail } from '../../api/task'
import type { CreateTaskForm } from '../../types/task'
import { ANALYSIS_SCOPE, TASK_TYPE } from '../../types/taskEnums'
import {
  getAnalysisScopeText,
  getPriorityText,
  getReviewStatusText,
  getSourceDomainText,
  getTaskTypeText
} from '../../utils/task'
import { buildFromQuery, getRouteQueryValue, resolveSourcePath } from '../../utils/taskNavigation'
import { canCreateTasks } from '../../utils/roleAccess'

const DEFAULT_TASK_TYPE = TASK_TYPE.STOCK_RESEARCH
const DEFAULT_TARGET_TYPE = 'STOCK'
const DEFAULT_PRIORITY = 'HIGH'

const router = useRouter()
const route = useRoute()
const formRef = ref<FormInstance>()
const submitLoading = ref(false)

const form = reactive<CreateTaskForm>({
  taskType: DEFAULT_TASK_TYPE,
  taskTitle: '',
  targetType: DEFAULT_TARGET_TYPE,
  targetCode: '',
  targetName: '',
  priority: DEFAULT_PRIORITY,
  sourceTaskId: '',
  sourceReportId: '',
  sourceEventId: '',
  sourceDomain: '',
  sourceReviewStatus: '',
  analysisScope: ANALYSIS_SCOPE.DEEP_RESEARCH
})

const text = {
  title: '新建投研任务',
  sourceAlert: '当前任务会携带来源上下文，可用于后续复核链路和工作流分流。',
  sourceDomain: '来源业务',
  analysisScope: '分析范围',
  sourceTaskId: '来源任务',
  sourceReportId: '来源报告',
  sourceEventId: '来源事件',
  sourceReviewStatus: '来源审核状态',
  sourcePath: '来源页面',
  taskType: '任务类型',
  taskTitle: '任务标题',
  targetType: '标的类型',
  targetCode: '标的代码',
  targetName: '标的名称',
  priority: '优先级',
  stock: '股票',
  taskTitlePlaceholder: '例如：贵州茅台风险复核研究',
  targetCodePlaceholder: '例如：600519.SH / 01929.HK',
  targetNamePlaceholder: '例如：贵州茅台 / 周大福',
  submit: '创建任务',
  reset: '重置',
  backToSource: '返回来源页',
  backToList: '返回列表',
  noCreatePermission: '当前角色无权创建任务',
  createFailed: '任务创建失败',
  createSuccess: '任务创建成功，正在准备详情页',
  createReadyTimeout: '任务已创建，但详情页仍在准备中，先返回列表页',
  createError: '任务创建异常',
  taskTypeRequired: '请选择任务类型',
  taskTitleRequired: '请输入任务标题',
  targetTypeRequired: '请选择标的类型',
  targetCodeRequired: '请输入标的代码',
  targetNameRequired: '请输入标的名称',
  priorityRequired: '请选择优先级'
} as const

const priorityOptions = [
  { value: 'HIGH', label: getPriorityText('HIGH') },
  { value: 'MEDIUM', label: getPriorityText('MEDIUM') },
  { value: 'LOW', label: getPriorityText('LOW') }
]

const canCreateTask = computed(() => canCreateTasks())
const sourcePath = computed(() => resolveSourcePath(route.query.from, []))

const hasSourceContext = computed(() => {
  return Boolean(
    form.sourceTaskId
      || form.sourceReportId
      || form.sourceEventId
      || form.sourceDomain
      || form.sourceReviewStatus
      || sourcePath.value
  )
})

const taskTypeOptions = computed(() => {
  const values = hasSourceContext.value
    ? [
        TASK_TYPE.STOCK_RESEARCH,
        TASK_TYPE.FOLLOW_UP_RESEARCH,
        TASK_TYPE.REPORT_REVIEW,
        TASK_TYPE.RISK_REVIEW,
        TASK_TYPE.AUDIT_REVIEW
      ]
    : [TASK_TYPE.STOCK_RESEARCH]

  return values.map((value) => ({
    value,
    label: getTaskTypeText(value)
  }))
})

const sourceDomainText = computed(() => getSourceDomainText(form.sourceDomain))
const sourceReviewStatusText = computed(() => {
  return form.sourceReviewStatus ? getReviewStatusText(form.sourceReviewStatus) : '-'
})
const analysisScopeText = computed(() => getAnalysisScopeText(form.analysisScope))

const rules: FormRules<CreateTaskForm> = {
  taskType: [{ required: true, message: text.taskTypeRequired, trigger: 'change' }],
  taskTitle: [{ required: true, message: text.taskTitleRequired, trigger: 'blur' }],
  targetType: [{ required: true, message: text.targetTypeRequired, trigger: 'change' }],
  targetCode: [{ required: true, message: text.targetCodeRequired, trigger: 'blur' }],
  targetName: [{ required: true, message: text.targetNameRequired, trigger: 'blur' }],
  priority: [{ required: true, message: text.priorityRequired, trigger: 'change' }]
}

function getQueryValue(key: string) {
  return getRouteQueryValue(route.query[key])
}

function resolveDefaultAnalysisScope(taskType: string, sourceDomain?: string) {
  if (taskType === TASK_TYPE.FOLLOW_UP_RESEARCH && sourceDomain === 'MARKET_EVENT') {
    return ANALYSIS_SCOPE.INTELLIGENCE_FOLLOW_UP
  }

  switch (taskType) {
    case TASK_TYPE.RISK_REVIEW:
      return ANALYSIS_SCOPE.RISK_RECHECK
    case TASK_TYPE.AUDIT_REVIEW:
      return ANALYSIS_SCOPE.AUDIT_RECHECK
    case TASK_TYPE.REPORT_REVIEW:
      return ANALYSIS_SCOPE.REPORT_REVIEW_RECHECK
    case TASK_TYPE.FOLLOW_UP_RESEARCH:
      return ANALYSIS_SCOPE.REPORT_FOLLOW_UP
    case TASK_TYPE.STOCK_RESEARCH:
    default:
      return ANALYSIS_SCOPE.DEEP_RESEARCH
  }
}

function syncFormFromRoute() {
  form.taskType = getQueryValue('taskType') || DEFAULT_TASK_TYPE
  form.taskTitle = getQueryValue('taskTitle')
  form.targetType = getQueryValue('targetType') || DEFAULT_TARGET_TYPE
  form.targetCode = getQueryValue('targetCode')
  form.targetName = getQueryValue('targetName')
  form.priority = getQueryValue('priority') || DEFAULT_PRIORITY
  form.sourceTaskId = getQueryValue('sourceTaskId')
  form.sourceReportId = getQueryValue('sourceReportId')
  form.sourceEventId = getQueryValue('sourceEventId')
  form.sourceDomain = getQueryValue('sourceDomain')
  form.sourceReviewStatus = getQueryValue('sourceReviewStatus')

  if (
    !form.sourceTaskId
    && !form.sourceReportId
    && !form.sourceEventId
    && !form.sourceDomain
    && !form.sourceReviewStatus
    && !sourcePath.value
  ) {
    form.taskType = DEFAULT_TASK_TYPE
  }

  form.analysisScope = getQueryValue('analysisScope') || resolveDefaultAnalysisScope(form.taskType, form.sourceDomain)
}

function buildContextQuery() {
  return {
    taskType: form.taskType || undefined,
    from: sourcePath.value || undefined,
    sourceTaskId: form.sourceTaskId || undefined,
    sourceReportId: form.sourceReportId || undefined,
    sourceEventId: form.sourceEventId || undefined,
    sourceDomain: form.sourceDomain || undefined,
    sourceReviewStatus: form.sourceReviewStatus || undefined,
    analysisScope: form.analysisScope || undefined
  }
}

function sleep(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

async function waitUntilTaskReady(taskId: string, maxAttempts = 8, intervalMs = 800) {
  for (let i = 0; i < maxAttempts; i++) {
    try {
      const res = await fetchTaskFullDetail(taskId)
      if (res.success && res.data) {
        return true
      }
    } catch {
      // ignore transient warm-up errors
    }
    await sleep(intervalMs)
  }
  return false
}

async function handleSubmit() {
  if (!canCreateTask.value) {
    ElMessage.warning(text.noCreatePermission)
    return
  }
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const res = await createTask(form)
    if (!res.success) {
      ElMessage.error(res.message || text.createFailed)
      return
    }

    const taskId = res.data
    ElMessage.success(text.createSuccess)

    const ready = await waitUntilTaskReady(taskId)
    if (ready) {
      router.push({
        path: `/tasks/${taskId}`,
        query: buildFromQuery(sourcePath.value)
      })
      return
    }

    ElMessage.warning(text.createReadyTimeout)
    router.push(sourcePath.value || '/tasks')
  } catch (error: any) {
    ElMessage.error(error?.message || text.createError)
  } finally {
    submitLoading.value = false
  }
}

async function handleReset() {
  form.taskType = getQueryValue('taskType') || DEFAULT_TASK_TYPE
  form.taskTitle = ''
  form.targetType = DEFAULT_TARGET_TYPE
  form.targetCode = ''
  form.targetName = ''
  form.priority = DEFAULT_PRIORITY
  form.analysisScope = getQueryValue('analysisScope') || resolveDefaultAnalysisScope(form.taskType, form.sourceDomain)
  formRef.value?.clearValidate()

  if (
    route.query.taskTitle
    || route.query.targetCode
    || route.query.targetName
    || route.query.priority
    || route.query.from
    || route.query.sourceTaskId
    || route.query.sourceReportId
    || route.query.sourceEventId
    || route.query.sourceDomain
    || route.query.sourceReviewStatus
    || route.query.analysisScope
  ) {
    await router.replace({
      path: '/tasks/create',
      query: buildContextQuery()
    })
  }
}

watch(
  () => form.taskType,
  (next, prev) => {
    if (!next || next === prev) {
      return
    }
    form.analysisScope = resolveDefaultAnalysisScope(next, form.sourceDomain)
  }
)

watch(
  [
    () => route.query.taskType,
    () => route.query.taskTitle,
    () => route.query.targetType,
    () => route.query.targetCode,
    () => route.query.targetName,
    () => route.query.priority,
    () => route.query.sourceTaskId,
    () => route.query.sourceReportId,
    () => route.query.sourceEventId,
    () => route.query.sourceDomain,
    () => route.query.sourceReviewStatus,
    () => route.query.analysisScope
  ],
  () => {
    syncFormFromRoute()
  },
  { immediate: true }
)
</script>

<template>
  <div>
    <el-card>
      <template #header>
        <div style="font-weight: 700;">{{ text.title }}</div>
      </template>

      <el-alert
        v-if="!canCreateTask"
        :title="text.noCreatePermission"
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 16px;"
      />

      <el-alert
        v-if="hasSourceContext"
        :title="text.sourceAlert"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 16px;"
      />

      <el-descriptions
        v-if="hasSourceContext"
        :column="2"
        border
        style="margin-bottom: 16px;"
      >
        <el-descriptions-item :label="text.sourceDomain">
          {{ sourceDomainText }}
        </el-descriptions-item>
        <el-descriptions-item :label="text.analysisScope">
          {{ analysisScopeText }}
        </el-descriptions-item>
        <el-descriptions-item :label="text.sourceTaskId">
          {{ form.sourceTaskId || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="text.sourceReportId">
          {{ form.sourceReportId || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="text.sourceEventId">
          {{ form.sourceEventId || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="text.sourceReviewStatus">
          {{ sourceReviewStatusText }}
        </el-descriptions-item>
        <el-descriptions-item :label="text.sourcePath" :span="2">
          {{ sourcePath || '-' }}
        </el-descriptions-item>
      </el-descriptions>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        style="max-width: 720px;"
      >
        <el-form-item :label="text.taskType" prop="taskType">
          <el-select v-model="form.taskType" style="width: 100%;">
            <el-option
              v-for="option in taskTypeOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item :label="text.taskTitle" prop="taskTitle">
          <el-input v-model="form.taskTitle" :placeholder="text.taskTitlePlaceholder" />
        </el-form-item>

        <el-form-item :label="text.targetType" prop="targetType">
          <el-select v-model="form.targetType" style="width: 100%;">
            <el-option :label="text.stock" value="STOCK" />
          </el-select>
        </el-form-item>

        <el-form-item :label="text.targetCode" prop="targetCode">
          <el-input v-model="form.targetCode" :placeholder="text.targetCodePlaceholder" />
        </el-form-item>

        <el-form-item :label="text.targetName" prop="targetName">
          <el-input v-model="form.targetName" :placeholder="text.targetNamePlaceholder" />
        </el-form-item>

        <el-form-item :label="text.priority" prop="priority">
          <el-select v-model="form.priority" style="width: 100%;">
            <el-option
              v-for="option in priorityOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button
            v-if="canCreateTask"
            type="primary"
            :loading="submitLoading"
            @click="handleSubmit"
          >
            {{ text.submit }}
          </el-button>
          <el-button @click="handleReset">{{ text.reset }}</el-button>
          <el-button v-if="sourcePath" @click="router.push(sourcePath)">{{ text.backToSource }}</el-button>
          <el-button @click="router.push('/tasks')">{{ text.backToList }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>
