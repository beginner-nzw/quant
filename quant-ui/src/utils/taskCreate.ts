import { ANALYSIS_SCOPE, TASK_TYPE } from '../types/taskEnums'

const DEFAULT_TASK_TYPE = TASK_TYPE.STOCK_RESEARCH
const DEFAULT_TARGET_TYPE = 'STOCK'
const DEFAULT_PRIORITY = 'HIGH'

interface TaskCreateQueryOptions {
  taskType?: string | null
  taskTitle?: string | null
  targetType?: string | null
  targetCode?: string | null
  targetName?: string | null
  priority?: string | null
  sourceTaskId?: string | null
  sourceReportId?: string | null
  sourceEventId?: string | null
  sourceDomain?: string | null
  sourceReviewStatus?: string | null
  analysisScope?: string | null
  from?: string | null
}

export function buildTaskCreateQuery(options: TaskCreateQueryOptions) {
  const targetCode = normalizeValue(options.targetCode)
  const targetName = normalizeValue(options.targetName)
  const analysisScope = resolveAnalysisScope(options.analysisScope, options.taskType, options.sourceDomain)
  const taskType = resolveTaskType(options.taskType, analysisScope)

  return {
    taskType,
    taskTitle: normalizeValue(options.taskTitle) || buildFollowUpTaskTitle(targetName, targetCode),
    targetType: normalizeValue(options.targetType) || DEFAULT_TARGET_TYPE,
    targetCode,
    targetName,
    priority: resolvePriority(options.priority),
    sourceTaskId: normalizeValue(options.sourceTaskId),
    sourceReportId: normalizeValue(options.sourceReportId),
    sourceEventId: normalizeValue(options.sourceEventId),
    sourceDomain: normalizeValue(options.sourceDomain),
    sourceReviewStatus: normalizeValue(options.sourceReviewStatus),
    analysisScope,
    from: normalizeValue(options.from)
  }
}

export function buildFollowUpTaskTitle(targetName?: string, targetCode?: string, suffix = '跟踪研究') {
  const targetLabel = targetName || targetCode || '标的'
  return `${targetLabel}${suffix}`
}

function resolvePriority(priority?: string | null) {
  switch (priority) {
    case 'LOW':
      return 'LOW'
    case 'MEDIUM':
      return 'MEDIUM'
    case 'HIGH':
    default:
      return DEFAULT_PRIORITY
  }
}

function resolveTaskType(taskType?: string | null, analysisScope?: string) {
  const normalizedTaskType = normalizeValue(taskType)
  if (normalizedTaskType) {
    return normalizedTaskType
  }

  switch (analysisScope) {
    case ANALYSIS_SCOPE.RISK_RECHECK:
      return TASK_TYPE.RISK_REVIEW
    case ANALYSIS_SCOPE.AUDIT_RECHECK:
      return TASK_TYPE.AUDIT_REVIEW
    case ANALYSIS_SCOPE.REPORT_REVIEW_RECHECK:
      return TASK_TYPE.REPORT_REVIEW
    case ANALYSIS_SCOPE.INTELLIGENCE_FOLLOW_UP:
    case ANALYSIS_SCOPE.SIGNAL_FOLLOW_UP:
    case ANALYSIS_SCOPE.REPORT_FOLLOW_UP:
      return TASK_TYPE.FOLLOW_UP_RESEARCH
    case ANALYSIS_SCOPE.DEEP_RESEARCH:
    default:
      return DEFAULT_TASK_TYPE
  }
}

function resolveAnalysisScope(analysisScope?: string | null, taskType?: string | null, sourceDomain?: string | null) {
  const normalizedAnalysisScope = normalizeValue(analysisScope)
  if (normalizedAnalysisScope) {
    return normalizedAnalysisScope
  }

  if (normalizeValue(taskType) === TASK_TYPE.FOLLOW_UP_RESEARCH && normalizeValue(sourceDomain) === 'MARKET_EVENT') {
    return ANALYSIS_SCOPE.INTELLIGENCE_FOLLOW_UP
  }

  switch (normalizeValue(taskType)) {
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

function normalizeValue(value?: string | null) {
  const normalized = value?.trim()
  return normalized || undefined
}
