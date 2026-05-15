import { ElMessage, ElMessageBox } from 'element-plus'
import { cancelTask, retryTask, reviewTaskReport } from '../api/task'
import { getCurrentUser } from './auth'
import { canCancelTasks, canRetryTasks } from './roleAccess'
import { REPORT_REVIEW_STATUS, type ReportReviewStatus } from '../types/taskEnums'

const text = {
  confirmRetryMessage: '确认要重试该失败任务吗？',
  confirmRetryTitle: '重试确认',
  confirmRetryButton: '确认重试',
  confirmCancelMessage: '确认要取消该任务吗？取消后将终止后续执行。',
  confirmCancelTitle: '取消确认',
  confirmCancelButton: '确认取消',
  cancelButton: '取消',
  retrySubmitted: '重试已提交',
  taskCancelled: '任务已取消',
  retryForbidden: '当前角色没有任务重试权限',
  cancelForbidden: '当前角色没有任务取消权限',
  reviewSaved: '报告审核已保存',
  reviewApproved: '报告已审核通过',
  reviewRejected: '报告已驳回',
  saveFailed: '保存失败',
  saveError: '保存异常',
  approveFailed: '审核通过失败',
  approveError: '审核通过异常',
  rejectFailed: '驳回失败',
  rejectError: '驳回异常',
  defaultApproveComment: '审核通过',
  defaultRejectComment: '审核驳回'
} as const

export interface TaskReviewFormValue {
  reviewStatus: ReportReviewStatus
  reviewedBy: string
  revisedSummary: string
  revisedHighlightsText: string
  revisedRiskPointsText: string
  reviewComment: string
}

function getOperatorId() {
  return getCurrentUser().userId
}

export async function executeTaskRetry(taskId: string, retryReason: string) {
  if (!canRetryTasks()) {
    ElMessage.warning(text.retryForbidden)
    return false
  }

  try {
    await ElMessageBox.confirm(text.confirmRetryMessage, text.confirmRetryTitle, {
      type: 'warning',
      confirmButtonText: text.confirmRetryButton,
      cancelButtonText: text.cancelButton
    })
  } catch {
    return false
  }

  const res = await retryTask(taskId, {
    retryReason,
    operatorId: getOperatorId()
  })

  if (!res.success) {
    ElMessage.error(res.message)
    return false
  }

  ElMessage.success(text.retrySubmitted)
  return true
}

export async function executeTaskCancel(taskId: string, cancelReason: string) {
  if (!canCancelTasks()) {
    ElMessage.warning(text.cancelForbidden)
    return false
  }

  try {
    await ElMessageBox.confirm(text.confirmCancelMessage, text.confirmCancelTitle, {
      type: 'warning',
      confirmButtonText: text.confirmCancelButton,
      cancelButtonText: text.cancelButton
    })
  } catch {
    return false
  }

  const res = await cancelTask(taskId, {
    cancelReason,
    operatorId: getOperatorId()
  })

  if (!res.success) {
    ElMessage.error(res.message)
    return false
  }

  ElMessage.success(text.taskCancelled)
  return true
}

export async function executeTaskReportReview(
  taskId: string,
  form: TaskReviewFormValue,
  options?: {
    fallbackSummary?: string
    mode?: 'save' | 'approve' | 'reject'
  }
) {
  const mode = options?.mode || 'save'
  const payload = buildReviewPayload(form, options?.fallbackSummary || '', mode)

  try {
    const res = await reviewTaskReport(taskId, payload)
    if (!res.success) {
      ElMessage.error(getFailureText(mode, res.message))
      return false
    }

    ElMessage.success(getSuccessText(mode))
    return true
  } catch (e: any) {
    ElMessage.error(e?.message || getErrorText(mode))
    return false
  }
}

function buildReviewPayload(
  form: TaskReviewFormValue,
  fallbackSummary: string,
  mode: 'save' | 'approve' | 'reject'
) {
  const reviewStatus = resolveReviewStatus(mode, form.reviewStatus)
  return {
    reviewStatus,
    reviewedBy: getOperatorId(),
    revisedSummary: form.revisedSummary || fallbackSummary,
    revisedHighlights: splitLines(form.revisedHighlightsText),
    revisedRiskPoints: splitLines(form.revisedRiskPointsText),
    reviewComment: resolveReviewComment(mode, form.reviewComment)
  }
}

function resolveReviewStatus(mode: 'save' | 'approve' | 'reject', currentStatus: ReportReviewStatus) {
  if (mode === 'approve') return REPORT_REVIEW_STATUS.APPROVED
  if (mode === 'reject') return REPORT_REVIEW_STATUS.REJECTED
  return currentStatus
}

function resolveReviewComment(mode: 'save' | 'approve' | 'reject', currentComment: string) {
  if (currentComment) return currentComment
  if (mode === 'approve') return text.defaultApproveComment
  if (mode === 'reject') return text.defaultRejectComment
  return currentComment
}

function splitLines(value: string) {
  return value
    .split('\n')
    .map((item) => item.trim())
    .filter(Boolean)
}

function getSuccessText(mode: 'save' | 'approve' | 'reject') {
  switch (mode) {
    case 'approve':
      return text.reviewApproved
    case 'reject':
      return text.reviewRejected
    default:
      return text.reviewSaved
  }
}

function getFailureText(mode: 'save' | 'approve' | 'reject', message?: string) {
  if (message) return message
  switch (mode) {
    case 'approve':
      return text.approveFailed
    case 'reject':
      return text.rejectFailed
    default:
      return text.saveFailed
  }
}

function getErrorText(mode: 'save' | 'approve' | 'reject') {
  switch (mode) {
    case 'approve':
      return text.approveError
    case 'reject':
      return text.rejectError
    default:
      return text.saveError
  }
}
