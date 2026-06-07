import { get, post } from '../utils/request'
import type {
  ReportCenterPageData,
  ReportCenterStats,
  ReportReviewStats,
  ReportVersion,
  ReportVersionCompare,
  TaskReport,
  TaskReportReviewLog
} from '../types/task'

const REPORT_API_BASE = '/api/reports'

export function fetchReportCenterStats() {
  return get<ReportCenterStats>(`${REPORT_API_BASE}/center/stats`)
}

export function fetchReportCenter(params: Record<string, any>) {
  return get<ReportCenterPageData>(`${REPORT_API_BASE}/center`, params)
}

export function fetchTaskReport(taskId: string) {
  return get<TaskReport>(`${REPORT_API_BASE}/tasks/${taskId}`)
}

export function reviewTaskReport(taskId: string, data: Record<string, any>) {
  return post<string>(`${REPORT_API_BASE}/tasks/${taskId}/review`, data)
}

export function fetchReportReviewStats() {
  return get<ReportReviewStats>(`${REPORT_API_BASE}/review/stats`)
}

export function fetchTaskReportReviewLogs(taskId: string) {
  return get<TaskReportReviewLog[]>(`${REPORT_API_BASE}/tasks/${taskId}/review-logs`)
}

export function fetchTaskReportVersions(taskId: string) {
  return get<ReportVersion[]>(`${REPORT_API_BASE}/tasks/${taskId}/versions`)
}

export function fetchTaskReportVersion(taskId: string, versionNo: number) {
  return get<ReportVersion | null>(`${REPORT_API_BASE}/tasks/${taskId}/versions/${versionNo}`)
}

export function compareTaskReportVersions(taskId: string, fromVersionNo: number, toVersionNo: number) {
  return get<ReportVersionCompare | null>(`${REPORT_API_BASE}/tasks/${taskId}/versions/compare`, {
    fromVersionNo,
    toVersionNo
  })
}
