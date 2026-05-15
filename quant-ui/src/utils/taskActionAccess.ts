import type { ResearchWorkbenchData, TaskFullDetail, TaskListItem } from '../types/task'
import {
  isPendingReviewStatus,
  isTaskActiveStatus,
  isTaskFailedStatus,
  isTaskSuccessStatus
} from '../types/taskEnums'
import {
  canCancelTasks,
  canCreateTasks,
  canRetryTasks,
  canReviewReports
} from './roleAccess'

export interface TaskListActionAccess {
  showReport: boolean
  showReview: boolean
  showRetry: boolean
  showCancel: boolean
}

export interface TaskDetailActionAccess {
  showViewFullReport: boolean
  showCreateSimilarTask: boolean
  showRetry: boolean
  showCancel: boolean
  showReportCard: boolean
}

export interface TaskReportActionAccess {
  showCreateTask: boolean
  showApprove: boolean
  showReject: boolean
  showEdit: boolean
}

export interface CenterActionAccess {
  showCreateTask: boolean
}

export interface ReportWorkbenchActionAccess {
  showCreateTask: boolean
  showReview: boolean
}

export function resolveTaskListActionAccess(row: TaskListItem): TaskListActionAccess {
  const isSuccess = isTaskSuccessStatus(row.status)
  const isPendingReview = isPendingReviewStatus(row.reportReviewStatus)
  const canReview = canReviewReports()

  return {
    showReport: isSuccess && (!isPendingReview || !canReview),
    showReview: isSuccess && isPendingReview && canReview,
    showRetry: isTaskFailedStatus(row.status) && canRetryTasks(),
    showCancel: isTaskActiveStatus(row.status) && canCancelTasks()
  }
}

export function resolveTaskDetailActionAccess(detail?: TaskFullDetail | null): TaskDetailActionAccess {
  const taskDetail = detail?.taskDetail
  const hasReport = !!detail?.report
  const isSuccess = isTaskSuccessStatus(taskDetail?.status)

  return {
    showViewFullReport: !!taskDetail && isSuccess && hasReport,
    showCreateSimilarTask: !!taskDetail && canCreateTasks(),
    showRetry: !!taskDetail && isTaskFailedStatus(taskDetail.status) && canRetryTasks(),
    showCancel: !!taskDetail && isTaskActiveStatus(taskDetail.status) && canCancelTasks(),
    showReportCard: !!taskDetail && isSuccess && hasReport
  }
}

export function resolveTaskReportActionAccess(detail?: TaskFullDetail | null): TaskReportActionAccess {
  const report = detail?.report
  const canReview = canReviewReports()
  const isPendingReview = isPendingReviewStatus(report?.reviewStatus)

  return {
    showCreateTask: !!report && canCreateTasks(),
    showApprove: !!report && isPendingReview && canReview,
    showReject: !!report && isPendingReview && canReview,
    showEdit: !!report && canReview
  }
}

export function resolveCenterActionAccess(): CenterActionAccess {
  return {
    showCreateTask: canCreateTasks()
  }
}

export function resolveReportWorkbenchActionAccess(row: TaskListItem): ReportWorkbenchActionAccess {
  return {
    showCreateTask: canCreateTasks(),
    showReview: isPendingReviewStatus(row.reportReviewStatus) && canReviewReports()
  }
}

export function resolveResearchWorkbenchActionAccess(data?: ResearchWorkbenchData | null): CenterActionAccess {
  return {
    showCreateTask: !!data && canCreateTasks()
  }
}
