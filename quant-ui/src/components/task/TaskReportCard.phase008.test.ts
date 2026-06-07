import type { TaskReport } from '../../types/task'

export const phase008TaskReportFixture: TaskReport = {
  taskType: 'STOCK_RESEARCH',
  finalStatus: 'SUCCESS',
  reportId: 'report-phase-008',
  confidenceScore: 0.86,
  needHumanReview: true,
  evidenceItems: [
    {
      evidenceId: 'evidence-1',
      evidenceType: 'MARKET_EVENT',
      title: 'Policy update',
      summary: 'Policy support evidence',
      referenceId: 'event-1',
      relevance: 'HIGH'
    }
  ],
  evidenceRefs: ['sourceEvent:event-1'],
  reviewSuggestion: 'Human reviewer should inspect audit support.',
  strategyCandidate: {
    direction: 'NEUTRAL',
    summary: 'Watchlist candidate',
    confidence: 0.72,
    trace: {
      source: 'strategy_reasoning_agent',
      authority: 'CANDIDATE_ONLY'
    }
  },
  strategyFactors: [
    {
      factorCode: 'RISK_ADJUSTMENT',
      factorName: 'Risk adjustment',
      factorValue: '2',
      factorWeight: 0.2,
      factorConclusion: 'Risk pressure keeps this as a watchlist signal.',
      evidenceRefs: ['risk-warning:task-1']
    }
  ],
  auditSupport: {
    authority: 'SUPPORT_ONLY_NO_BUSINESS_APPROVAL',
    supportType: 'POLICY_EVIDENCE_REPORT_REVIEW_SUPPORT',
    policyChecks: [{ policyCode: 'NO_DIRECT_APPROVAL', status: 'PASS' }],
    evidenceChecks: [{ checkCode: 'TRACEABLE_REFERENCE_FORMAT', status: 'PASS' }],
    reportReview: { doesNotApproveReport: true },
    reviewSuggestions: ['Use this as review support only.'],
    trace: {
      source: 'audit_compliance_agent',
      traceId: 'trace-phase-008'
    }
  }
}

export function assertPhase008ReportSupportBoundaries(report: TaskReport) {
  if ((report.strategyCandidate as Record<string, any>)?.trace?.authority !== 'CANDIDATE_ONLY') {
    throw new Error('strategy candidate must remain candidate-only display metadata')
  }
  if ((report.auditSupport as Record<string, any>)?.authority !== 'SUPPORT_ONLY_NO_BUSINESS_APPROVAL') {
    throw new Error('audit support must not become business approval authority')
  }
  if (!(report.evidenceItems?.length && report.reviewSuggestion)) {
    throw new Error('traceable evidence and review suggestion must be displayable')
  }
}

assertPhase008ReportSupportBoundaries(phase008TaskReportFixture)
