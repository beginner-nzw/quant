import { readFile } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const rootDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')

async function readText(relativePath) {
  return readFile(path.join(rootDir, relativePath), 'utf8')
}

function assertIncludes(relativePath, text, expected, message) {
  if (!text.includes(expected)) {
    throw new Error(`${message}: ${relativePath} missing "${expected}"`)
  }
}

const auth = await readText('src/utils/auth.ts')
for (const expected of [
  'getAuthSession',
  'setAuthSession',
  'hasActiveAuthSession',
  'getSessionMode',
  'APPROVED_SSO'
]) {
  assertIncludes('src/utils/auth.ts', auth, expected, 'JWT/session-aware auth utility is incomplete')
}

const requestHeaders = await readText('src/utils/requestHeaders.ts')
for (const expected of [
  'Authorization',
  'REQUEST_HEADER_AUTH_MODE',
  'DEMO_HEADER',
  'JWT'
]) {
  assertIncludes('src/utils/requestHeaders.ts', requestHeaders, expected, 'JWT-aware request headers are incomplete')
}

const layout = await readText('src/layout/BasicLayout.vue')
for (const expected of [
  'sessionModeLabel',
  'canSwitchDemoRole',
  '权限以后端校验为准',
  'UI gating is advisory'
]) {
  assertIncludes('src/layout/BasicLayout.vue', layout, expected, 'Role-aware navigation/session UX is incomplete')
}

const taskApi = await readText('src/api/task.ts')
for (const expected of [
  'syncMarketEventSource',
  'previewMarketEventSource',
  'diagnoseMarketEventSource',
  'fetchMarketEventIngestHistory',
  'retryTask',
  'resumeTask',
  'rerunTaskNode',
  'fetchHumanReviewQueue',
  'decideHumanReview'
]) {
  assertIncludes('src/api/task.ts', taskApi, expected, 'Production workflow API client is incomplete')
}

const marketEventCenter = await readText('src/views/report/MarketEventCenterView.vue')
for (const expected of [
  'handleSourcePreview',
  'handleSourceDiagnose',
  'submitSourceSync',
  'ingestHistoryRecords',
  'batchImportResult',
  'sourceDiagnosticResult'
]) {
  assertIncludes('src/views/report/MarketEventCenterView.vue', marketEventCenter, expected, 'Data source operations UX is incomplete')
}

const taskDetail = await readText('src/views/task/TaskDetailView.vue')
for (const expected of [
  'executeTaskResume',
  'executeTaskNodeRerun',
  'executeTaskRetry',
  'TaskAuditsTable',
  'TaskRetriesTable',
  'canRetryTasks()'
]) {
  assertIncludes('src/views/task/TaskDetailView.vue', taskDetail, expected, 'AI workflow operations UX is incomplete')
}

const humanReviewQueue = await readText('src/components/report/HumanReviewQueuePanel.vue')
for (const expected of [
  'fetchHumanReviewQueue',
  'decideHumanReview',
  'rerunWorkflow',
  'REPORT_REVIEW_STATUS.APPROVED',
  'REPORT_REVIEW_STATUS.REJECTED'
]) {
  assertIncludes('src/components/report/HumanReviewQueuePanel.vue', humanReviewQueue, expected, 'Human review queue UX is incomplete')
}

const reportView = await readText('src/views/task/TaskReportView.vue')
for (const expected of [
  'ReportVersionComparison',
  'ReportVersionHistoryPanel',
  'ReportEvidenceView',
  'handleApprove',
  'handleReject',
  'exportMarkdown'
]) {
  assertIncludes('src/views/task/TaskReportView.vue', reportView, expected, 'Report/risk/strategy review UX is incomplete')
}

console.log('frontend-production-workflow-check passed')
