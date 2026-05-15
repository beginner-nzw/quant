import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { fetchReportReviewStats, fetchTasks } from '../api/task'
import type { ReportReviewStats, TaskListItem } from '../types/task'
import { ANALYSIS_SCOPE, REPORT_REVIEW_STATUS, TASK_TYPE } from '../types/taskEnums'
import { buildResearchWorkbenchQuery } from './researchWorkbench'
import { resolveCenterActionAccess, resolveReportWorkbenchActionAccess } from './taskActionAccess'
import { buildFollowUpTaskTitle, buildTaskCreateQuery } from './taskCreate'
import { buildFromQuery } from './taskNavigation'

export type ReportWorkbenchMode = 'pending' | 'approved' | 'rejected'

export const REPORT_WORKBENCH_TABS = [
  {
    label: '待审核',
    route: '/reports/pending',
    type: 'warning'
  },
  {
    label: '已通过',
    route: '/reports/approved',
    type: 'default'
  },
  {
    label: '已驳回',
    route: '/reports/rejected',
    type: 'default'
  }
] as const

export function useReportWorkbench(options: {
  mode: ReportWorkbenchMode
  loadFailedText: string
  loadErrorText: string
  loadStatsFailedText?: string
  loadStatsErrorText?: string
}) {
  const route = useRoute()
  const router = useRouter()
  const loading = ref(false)
  const canCreateTask = computed(() => resolveCenterActionAccess().showCreateTask)
  const supportsReviewerFilter = options.mode !== 'pending'

  const defaultStats: ReportReviewStats = {
    pendingCount: 0,
    approvedCount: 0,
    rejectedCount: 0,
    totalReportCount: 0
  }

  const stats = reactive<ReportReviewStats>({ ...defaultStats })

  const pageData = reactive({
    total: 0,
    pageNum: 1,
    pageSize: 10,
    records: [] as TaskListItem[]
  })

  const query = reactive({
    targetCode: '',
    targetName: '',
    reportReviewedBy: ''
  })

  function syncQueryFromRoute() {
    query.targetCode = route.query.targetCode ? String(route.query.targetCode) : ''
    query.targetName = route.query.targetName ? String(route.query.targetName) : ''
    query.reportReviewedBy = supportsReviewerFilter && route.query.reportReviewedBy
      ? String(route.query.reportReviewedBy)
      : ''
    pageData.pageNum = normalizePageValue(route.query.pageNum, 1)
    pageData.pageSize = normalizePageValue(route.query.pageSize, 10)
  }

  async function loadStats() {
    try {
      const res = await fetchReportReviewStats()
      if (res.success) {
        Object.assign(stats, res.data || { ...defaultStats })
      } else {
        Object.assign(stats, { ...defaultStats })
        ElMessage.error(res.message || options.loadStatsFailedText || options.loadFailedText)
      }
    } catch (e: any) {
      Object.assign(stats, { ...defaultStats })
      ElMessage.error(e?.message || options.loadStatsErrorText || options.loadErrorText)
    }
  }

  async function loadReports() {
    loading.value = true
    try {
      const res = await fetchTasks(buildFetchParams(options.mode, pageData, query))
      if (res.success) {
        pageData.total = res.data?.total || 0
        pageData.records = res.data?.records || []
      } else {
        pageData.total = 0
        pageData.records = []
        ElMessage.error(res.message || options.loadFailedText)
      }
    } catch (e: any) {
      pageData.total = 0
      pageData.records = []
      ElMessage.error(e?.message || options.loadErrorText)
    } finally {
      loading.value = false
    }
  }

  async function navigateWithQuery(path = route.path) {
    const nextQuery = buildRouteQuery(path, pageData, query, supportsReviewerFilter)
    if (isSameRouteState(route.path, path, route.query, nextQuery)) {
      await Promise.all([loadStats(), loadReports()])
      return
    }

    await router.replace({
      path,
      query: nextQuery
    })
  }

  function handleSearch() {
    pageData.pageNum = 1
    return navigateWithQuery()
  }

  function handleReset() {
    query.targetCode = ''
    query.targetName = ''
    query.reportReviewedBy = ''
    pageData.pageNum = 1
    pageData.pageSize = 10
    return navigateWithQuery()
  }

  function handlePageChange(pageNum: number) {
    pageData.pageNum = pageNum
    return navigateWithQuery()
  }

  function handlePageSizeChange(pageSize: number) {
    pageData.pageSize = pageSize
    pageData.pageNum = 1
    return navigateWithQuery()
  }

  function goTab(path: string) {
    pageData.pageNum = 1
    const nextQuery = buildRouteQuery(path, pageData, query, supportsReviewerFilter)
    if (isSameRouteState(route.path, path, route.query, nextQuery)) {
      return Promise.all([loadStats(), loadReports()])
    }

    return router.push({
      path,
      query: nextQuery
    })
  }

  function goReport(taskId: string) {
    router.push({
      path: `/tasks/${taskId}/report`,
      query: buildFromQuery(route.fullPath)
    })
  }

  function goTaskDetail(taskId: string) {
    router.push({
      path: `/tasks/${taskId}`,
      query: buildFromQuery(route.fullPath)
    })
  }

  function goWorkbench(task: TaskListItem) {
    router.push({
      path: '/research-workbench',
      query: buildResearchWorkbenchQuery({
        targetCode: task.targetCode,
        targetName: task.targetName,
        from: route.fullPath
      })
    })
  }

  function goCreateTask(task: TaskListItem) {
    router.push({
      path: '/tasks/create',
      query: buildTaskCreateQuery({
        taskType: options.mode === 'approved' ? TASK_TYPE.FOLLOW_UP_RESEARCH : TASK_TYPE.REPORT_REVIEW,
        taskTitle: resolveFollowUpTaskTitle(options.mode, task),
        targetType: task.targetType,
        targetCode: task.targetCode,
        targetName: task.targetName,
        priority: resolveFollowUpTaskPriority(options.mode, task),
        sourceTaskId: task.taskId,
        sourceReportId: task.reportId || task.sourceReportId,
        sourceDomain: 'REPORT_WORKBENCH',
        sourceReviewStatus: task.reportReviewStatus,
        analysisScope: options.mode === 'approved'
          ? ANALYSIS_SCOPE.REPORT_FOLLOW_UP
          : ANALYSIS_SCOPE.REPORT_REVIEW_RECHECK,
        from: route.fullPath
      })
    })
  }

  function canReviewTask(task: TaskListItem) {
    return resolveReportWorkbenchActionAccess(task).showReview
  }

  watch([
    () => route.query.targetCode,
    () => route.query.targetName,
    () => route.query.reportReviewedBy,
    () => route.query.pageNum,
    () => route.query.pageSize
  ], async () => {
    syncQueryFromRoute()
    await Promise.all([loadStats(), loadReports()])
  }, { immediate: true })

  return {
    loading,
    stats,
    pageData,
    query,
    loadStats,
    loadReports,
    handleSearch,
    handleReset,
    handlePageChange,
    handlePageSizeChange,
    goTab,
    goReport,
    goTaskDetail,
    goWorkbench,
    goCreateTask,
    canCreateTask,
    canReviewTask
  }
}

function buildFetchParams(
  mode: ReportWorkbenchMode,
  pageData: { pageNum: number; pageSize: number },
  query: { targetCode: string; targetName: string; reportReviewedBy: string }
) {
  const params: Record<string, any> = {
    pageNum: pageData.pageNum,
    pageSize: pageData.pageSize,
    targetCode: normalizeQueryValue(query.targetCode),
    targetName: normalizeQueryValue(query.targetName)
  }

  const reportReviewedBy = normalizeQueryValue(query.reportReviewedBy)
  if (reportReviewedBy) {
    params.reportReviewedBy = reportReviewedBy
  }

  switch (mode) {
    case 'pending':
      params.onlyPendingReview = true
      break
    case 'approved':
      params.reportReviewStatus = REPORT_REVIEW_STATUS.APPROVED
      break
    case 'rejected':
      params.reportReviewStatus = REPORT_REVIEW_STATUS.REJECTED
      break
  }

  return params
}

function buildRouteQuery(
  path: string,
  pageData: { pageNum: number; pageSize: number },
  query: { targetCode: string; targetName: string; reportReviewedBy: string },
  supportsReviewerFilter: boolean
) {
  const params: Record<string, string | undefined> = {
    targetCode: normalizeQueryValue(query.targetCode),
    targetName: normalizeQueryValue(query.targetName),
    pageNum: pageData.pageNum > 1 ? String(pageData.pageNum) : undefined,
    pageSize: pageData.pageSize !== 10 ? String(pageData.pageSize) : undefined
  }

  if (supportsReviewerFilter && path !== '/reports/pending') {
    params.reportReviewedBy = normalizeQueryValue(query.reportReviewedBy)
  }

  return params
}

function isSameRouteState(
  currentPath: string,
  nextPath: string,
  currentQuery: Record<string, unknown>,
  nextQuery: Record<string, string | undefined>
) {
  if (currentPath !== nextPath) {
    return false
  }

  return getRouteQueryValue(currentQuery.targetCode) === (nextQuery.targetCode || '')
    && getRouteQueryValue(currentQuery.targetName) === (nextQuery.targetName || '')
    && getRouteQueryValue(currentQuery.reportReviewedBy) === (nextQuery.reportReviewedBy || '')
    && getRouteQueryValue(currentQuery.pageNum) === (nextQuery.pageNum || '')
    && getRouteQueryValue(currentQuery.pageSize) === (nextQuery.pageSize || '')
}

function getRouteQueryValue(value: unknown) {
  if (Array.isArray(value)) {
    return value[0] ? String(value[0]) : ''
  }
  return value ? String(value) : ''
}

function normalizePageValue(value: unknown, fallback: number) {
  const parsed = Number(getRouteQueryValue(value))
  if (Number.isInteger(parsed) && parsed > 0) {
    return parsed
  }
  return fallback
}

function normalizeQueryValue(value: string) {
  const normalized = value.trim()
  return normalized || undefined
}

function resolveFollowUpTaskTitle(mode: ReportWorkbenchMode, task: TaskListItem) {
  switch (mode) {
    case 'pending':
      return buildFollowUpTaskTitle(task.targetName, task.targetCode, '待审核报告复核研究')
    case 'approved':
      return buildFollowUpTaskTitle(task.targetName, task.targetCode, '已通过报告跟踪研究')
    case 'rejected':
      return buildFollowUpTaskTitle(task.targetName, task.targetCode, '驳回报告复核研究')
  }
}

function resolveFollowUpTaskPriority(mode: ReportWorkbenchMode, task: TaskListItem) {
  if (mode === 'pending' || mode === 'rejected') {
    return 'HIGH'
  }

  return task.priority
}
