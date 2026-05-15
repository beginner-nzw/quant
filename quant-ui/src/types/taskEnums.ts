export const TASK_STATUS = {
  INIT: 'INIT',
  DISPATCHED: 'DISPATCHED',
  RUNNING: 'RUNNING',
  SUCCESS: 'SUCCESS',
  FAILED: 'FAILED',
  CANCELLED: 'CANCELLED'
} as const

export type TaskStatus = typeof TASK_STATUS[keyof typeof TASK_STATUS]

export const TASK_STAGE = {
  INIT: 'INIT',
  DISPATCHED: 'DISPATCHED',
  RETRY_DISPATCHED: 'RETRY_DISPATCHED',
  RECEIVED: 'RECEIVED',
  PLANNING: 'PLANNING',
  INTENT_UNDERSTANDING: 'INTENT_UNDERSTANDING',
  EVIDENCE_COLLECTION: 'EVIDENCE_COLLECTION',
  FINANCIAL_ANALYSIS: 'FINANCIAL_ANALYSIS',
  RISK_REVIEW: 'RISK_REVIEW',
  REPORT_GENERATION: 'REPORT_GENERATION',
  FAILED: 'FAILED',
  TIMEOUT: 'TIMEOUT',
  FINISHED: 'FINISHED',
  CANCELLED: 'CANCELLED'
} as const

export type TaskStage = typeof TASK_STAGE[keyof typeof TASK_STAGE]

export const TASK_TYPE = {
  STOCK_RESEARCH: 'STOCK_RESEARCH',
  FOLLOW_UP_RESEARCH: 'FOLLOW_UP_RESEARCH',
  REPORT_REVIEW: 'REPORT_REVIEW',
  RISK_REVIEW: 'RISK_REVIEW',
  AUDIT_REVIEW: 'AUDIT_REVIEW'
} as const

export type TaskType = typeof TASK_TYPE[keyof typeof TASK_TYPE]

export const ANALYSIS_SCOPE = {
  DEEP_RESEARCH: 'DEEP_RESEARCH',
  INTELLIGENCE_FOLLOW_UP: 'INTELLIGENCE_FOLLOW_UP',
  SIGNAL_FOLLOW_UP: 'SIGNAL_FOLLOW_UP',
  REPORT_FOLLOW_UP: 'REPORT_FOLLOW_UP',
  REPORT_REVIEW_RECHECK: 'REPORT_REVIEW_RECHECK',
  RISK_RECHECK: 'RISK_RECHECK',
  AUDIT_RECHECK: 'AUDIT_RECHECK'
} as const

export type AnalysisScope = typeof ANALYSIS_SCOPE[keyof typeof ANALYSIS_SCOPE]

export const REPORT_REVIEW_STATUS = {
  PENDING: 'PENDING',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED'
} as const

export type ReportReviewStatus = typeof REPORT_REVIEW_STATUS[keyof typeof REPORT_REVIEW_STATUS]

export const RISK_LEVEL = {
  HIGH: 'HIGH',
  MEDIUM: 'MEDIUM',
  LOW: 'LOW'
} as const

export type RiskLevel = typeof RISK_LEVEL[keyof typeof RISK_LEVEL]

export const SIGNAL_DIRECTION = {
  POSITIVE: 'POSITIVE',
  NEUTRAL: 'NEUTRAL',
  NEGATIVE: 'NEGATIVE'
} as const

export type SignalDirection = typeof SIGNAL_DIRECTION[keyof typeof SIGNAL_DIRECTION]

export const SIGNAL_STRENGTH = {
  STRONG: 'STRONG',
  MEDIUM: 'MEDIUM',
  WEAK: 'WEAK'
} as const

export type SignalStrength = typeof SIGNAL_STRENGTH[keyof typeof SIGNAL_STRENGTH]

export const MARKET_INTELLIGENCE_TYPE = {
  RISK_ALERT: 'RISK_ALERT',
  STRATEGY_SIGNAL: 'STRATEGY_SIGNAL',
  REPORT_INSIGHT: 'REPORT_INSIGHT'
} as const

export type MarketIntelligenceType = typeof MARKET_INTELLIGENCE_TYPE[keyof typeof MARKET_INTELLIGENCE_TYPE]

export const MARKET_EVENT_TYPE = {
  NEWS: 'NEWS',
  ANNOUNCEMENT: 'ANNOUNCEMENT',
  EARNINGS: 'EARNINGS',
  POLICY: 'POLICY',
  RISK_ALERT: 'RISK_ALERT',
  OTHER: 'OTHER'
} as const

export type MarketEventType = typeof MARKET_EVENT_TYPE[keyof typeof MARKET_EVENT_TYPE]

export const MARKET_EVENT_IMPACT_LEVEL = {
  HIGH: 'HIGH',
  MEDIUM: 'MEDIUM',
  LOW: 'LOW'
} as const

export type MarketEventImpactLevel = typeof MARKET_EVENT_IMPACT_LEVEL[keyof typeof MARKET_EVENT_IMPACT_LEVEL]

export const MARKET_EVENT_STATUS = {
  ACTIVE: 'ACTIVE',
  RESOLVED: 'RESOLVED',
  IGNORED: 'IGNORED'
} as const

export type MarketEventStatus = typeof MARKET_EVENT_STATUS[keyof typeof MARKET_EVENT_STATUS]

export const RETRY_SOURCE = {
  MANUAL: 'MANUAL'
} as const

export type RetrySource = typeof RETRY_SOURCE[keyof typeof RETRY_SOURCE]

export const RETRY_STATUS = {
  SUBMITTED: 'SUBMITTED',
  DISPATCHED: 'DISPATCHED'
} as const

export type RetryStatus = typeof RETRY_STATUS[keyof typeof RETRY_STATUS]

export const AUDIT_TYPE = {
  TASK_CONTROL: 'TASK_CONTROL',
  AI_TASK_AUDIT: 'AI_TASK_AUDIT'
} as const

export type AuditType = typeof AUDIT_TYPE[keyof typeof AUDIT_TYPE]

export const AUDIT_OPERATOR_TYPE = {
  HUMAN: 'HUMAN',
  AGENT: 'AGENT'
} as const

export type AuditOperatorType = typeof AUDIT_OPERATOR_TYPE[keyof typeof AUDIT_OPERATOR_TYPE]

export const AUDIT_STAGE = {
  CANCELLED: 'CANCELLED',
  WORKFLOW_FINISHED: 'WORKFLOW_FINISHED'
} as const

export type AuditStage = typeof AUDIT_STAGE[keyof typeof AUDIT_STAGE]

export const AUDIT_ACTION_CODE = {
  TASK_CANCEL: 'TASK_CANCEL',
  AUDIT_SUMMARY: 'AUDIT_SUMMARY'
} as const

export type AuditActionCode = typeof AUDIT_ACTION_CODE[keyof typeof AUDIT_ACTION_CODE]

export const AUDIT_RESULT_STATUS = {
  SUCCESS: 'SUCCESS',
  FAILED: 'FAILED'
} as const

export type AuditResultStatus = typeof AUDIT_RESULT_STATUS[keyof typeof AUDIT_RESULT_STATUS]

export const TASK_STATUS_FILTER_OPTIONS = [
  TASK_STATUS.DISPATCHED,
  TASK_STATUS.RUNNING,
  TASK_STATUS.SUCCESS,
  TASK_STATUS.FAILED
] as const

export function isTaskSuccessStatus(status?: string) {
  return status === TASK_STATUS.SUCCESS
}

export function isTaskFailedStatus(status?: string) {
  return status === TASK_STATUS.FAILED
}

export function isTaskActiveStatus(status?: string) {
  return status === TASK_STATUS.RUNNING || status === TASK_STATUS.DISPATCHED
}

export function resolveReportReviewStatus(status?: string): ReportReviewStatus {
  switch (status) {
    case REPORT_REVIEW_STATUS.APPROVED:
      return REPORT_REVIEW_STATUS.APPROVED
    case REPORT_REVIEW_STATUS.REJECTED:
      return REPORT_REVIEW_STATUS.REJECTED
    default:
      return REPORT_REVIEW_STATUS.PENDING
  }
}

export function isPendingReviewStatus(status?: string) {
  return resolveReportReviewStatus(status) === REPORT_REVIEW_STATUS.PENDING
}
