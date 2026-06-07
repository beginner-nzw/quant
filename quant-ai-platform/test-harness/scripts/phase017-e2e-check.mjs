import { readFile } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import {
  findInPages,
  pageItems,
  requestJson,
  requireArray,
  requireFound,
  normalizeBaseUrl,
  summarizeTaskFull,
  waitFor
} from '../lib/phase017-http.mjs'

const harnessDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const seedPath = process.env.PHASE017_SEED_FILE || path.join(harnessDir, 'data', 'phase017-seed.json')
const seed = JSON.parse(await readFile(seedPath, 'utf8'))
const baseUrl = normalizeBaseUrl()
const task = {
  ...seed.tasks[0],
  taskTitle: `${seed.tasks[0].taskTitle} ${new Date().toISOString()}`
}

console.log(`Phase 17 e2e target: ${baseUrl}`)

await requestJson('/health', { baseUrl })
await requestJson('/api/tasks/stats', { baseUrl })
const engineReady = await requestJson('/engine/ready', { baseUrl })
if (engineReady.data?.status !== 'ready') {
  throw new Error(`AI engine worker is not ready for e2e dispatch consumption: ${JSON.stringify(engineReady.data)}`)
}

const createResult = await requestJson('/api/research/tasks', {
  baseUrl,
  method: 'POST',
  body: task
})
const taskId = String(createResult.data)
if (!taskId || taskId === 'undefined') {
  throw new Error(`Task creation did not return a task id: ${JSON.stringify(createResult.body)}`)
}
console.log(`created task: ${taskId}`)

let initialDetail
try {
  initialDetail = await waitFor(`/api/tasks/${encodeURIComponent(taskId)}/full`, (detail) => {
    const status = detail?.task?.taskStatus || detail?.taskStatus || detail?.status
      || detail?.taskDetail?.status
    const checkpointStatus = detail?.checkpoint?.status
    return ['SUCCESS', 'COMPLETED', 'WAITING_REVIEW', 'WAITING_HUMAN_REVIEW', 'FAILED', 'CANCELED', 'CANCELLED'].includes(status)
      || checkpointStatus === 'WAITING_HUMAN_REVIEW'
  }, { baseUrl })
} catch (error) {
  const [latestDetail, latestEngine] = await Promise.allSettled([
    requestJson(`/api/tasks/${encodeURIComponent(taskId)}/full`, { baseUrl }),
    requestJson('/engine/ready', { baseUrl })
  ])
  const detailSummary = latestDetail.status === 'fulfilled'
    ? summarizeTaskFull(latestDetail.value.data)
    : latestDetail.reason.message
  const engineSummary = latestEngine.status === 'fulfilled'
    ? latestEngine.value.data
    : latestEngine.reason.message
  throw new Error(`Timed out waiting for AI workflow progression for task ${taskId}: ${error.message}; task=${JSON.stringify(detailSummary)}; engine=${JSON.stringify(engineSummary)}`)
}

if (
  initialDetail.data?.checkpoint?.status === 'WAITING_HUMAN_REVIEW'
  || initialDetail.data?.taskDetail?.status === 'WAITING_HUMAN_REVIEW'
) {
  await requestJson(`/api/tasks/${encodeURIComponent(taskId)}/resume`, {
    baseUrl,
    method: 'POST',
    body: {
      operatorId: 'phase017-reviewer',
      reason: 'Phase 17 e2e approves human-review gate and resumes report generation.'
    }
  })
}

const fullDetail = await requestJson(`/api/tasks/${encodeURIComponent(taskId)}/full`, { baseUrl })
if (!fullDetail.data || typeof fullDetail.data !== 'object') {
  throw new Error('Task full detail response is empty')
}

const report = await waitFor(`/api/reports/tasks/${encodeURIComponent(taskId)}`, (data) => {
  return data && typeof data === 'object'
}, { baseUrl, timeoutMs: 180000, intervalMs: 5000 })
if (!report.data || typeof report.data !== 'object') {
  throw new Error('Report domain did not return a report object')
}
if (report.data.taskId && String(report.data.taskId) !== taskId) {
  throw new Error(`Report domain returned mismatched taskId ${report.data.taskId}, expected ${taskId}`)
}

const reportVersions = await requestJson(`/api/reports/tasks/${encodeURIComponent(taskId)}/versions`, { baseUrl })
requireArray(reportVersions.data, 'report versions')

await requestJson('/api/tasks/risk-warning-stats', { baseUrl })
const riskProjection = requireFound(
  await findInPages('/api/tasks/risk-warnings', (item) => String(item.taskId) === taskId, { baseUrl }),
  'risk warning projection',
  taskId
)
await requestJson('/api/tasks/strategy-signal-stats', { baseUrl })
const strategyProjection = requireFound(
  await findInPages('/api/tasks/strategy-signals', (item) => String(item.taskId) === taskId, { baseUrl }),
  'strategy signal projection',
  taskId
)
await requestJson('/api/reports/center/stats', { baseUrl })
const reportCenterProjection = requireFound(
  await findInPages('/api/reports/center', (item) => String(item.taskId) === taskId, { baseUrl }),
  'report center projection',
  taskId
)
await requestJson('/api/tasks/audit-compliance-stats', { baseUrl })
const auditProjection = requireFound(
  await findInPages('/api/tasks/audit-compliance', (item) => String(item.taskId) === taskId, { baseUrl }),
  'audit compliance projection',
  taskId
)

await requestJson(`/api/reports/tasks/${encodeURIComponent(taskId)}/review`, {
  baseUrl,
  method: 'POST',
  body: {
    reviewStatus: 'APPROVED',
    reviewedBy: 'phase017-reviewer',
    reviewComment: 'Phase 17 e2e review/audit verification.'
  }
})
const reviewLogs = await requestJson(`/api/reports/tasks/${encodeURIComponent(taskId)}/review-logs`, { baseUrl })
if (!requireArray(reviewLogs.data, 'report review logs').some((item) => String(item.taskId || taskId) === taskId)) {
  throw new Error(`report review logs did not contain taskId ${taskId}`)
}

const humanQueue = await requestJson('/api/tasks/human-reviews?pageNo=1&pageSize=10', { baseUrl })
const humanQueueItems = pageItems(humanQueue.data)
const humanReviewMatches = humanQueueItems.filter((item) => String(item.taskId) === taskId)
if (process.env.PHASE017_REQUIRE_HUMAN_REVIEW === 'true' && humanReviewMatches.length === 0) {
  throw new Error(`human review queue did not contain taskId ${taskId}`)
}
await requestJson('/api/tasks/human-reviews/stats', { baseUrl })

console.log(JSON.stringify({
  ok: true,
  taskId,
  evidence: {
    reportTaskId: report.data.taskId || taskId,
    reportVersionCount: reportVersions.data.length,
    riskWarningTaskId: riskProjection.item.taskId,
    strategySignalTaskId: strategyProjection.item.taskId,
    reportCenterTaskId: reportCenterProjection.item.taskId,
    auditComplianceTaskId: auditProjection.item.taskId,
    reviewLogCount: reviewLogs.data.length,
    humanReviewMatches: humanReviewMatches.length
  },
  covered: [
    'task creation',
    'AI workflow detail/progress',
    'report domain',
    'risk warning center',
    'strategy signal center',
    'report review',
    'audit compliance',
    'human review queue'
  ]
}, null, 2))
