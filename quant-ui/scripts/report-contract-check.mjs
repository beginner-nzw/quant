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

const reportApi = await readText('src/api/report.ts')
assertIncludes('src/api/report.ts', reportApi, "const REPORT_API_BASE = '/api/reports'", 'Stable report API base changed')
assertIncludes('src/api/report.ts', reportApi, '`${REPORT_API_BASE}/center/stats`', 'Report center stats path changed')
assertIncludes('src/api/report.ts', reportApi, '`${REPORT_API_BASE}/center`', 'Report center path changed')
assertIncludes('src/api/report.ts', reportApi, '`${REPORT_API_BASE}/review/stats`', 'Report review stats path changed')
assertIncludes('src/api/report.ts', reportApi, '`${REPORT_API_BASE}/tasks/${taskId}`', 'Task report path changed')
assertIncludes('src/api/report.ts', reportApi, '`${REPORT_API_BASE}/tasks/${taskId}/review`', 'Report review path changed')
assertIncludes('src/api/report.ts', reportApi, '`${REPORT_API_BASE}/tasks/${taskId}/versions`', 'Report versions path changed')

const taskApi = await readText('src/api/task.ts')
assertIncludes('src/api/task.ts', taskApi, "from './report'", 'Legacy report wrappers must delegate to stable report client')
assertIncludes('src/api/task.ts', taskApi, 'fetchStableReportCenterStats()', 'Legacy report stats wrapper must delegate')
assertIncludes('src/api/task.ts', taskApi, 'fetchStableReportCenter(params)', 'Legacy report center wrapper must delegate')
assertIncludes('src/api/task.ts', taskApi, 'fetchStableTaskReport(taskId)', 'Legacy task report wrapper must delegate')
assertIncludes('src/api/task.ts', taskApi, 'reviewStableTaskReport(taskId, data)', 'Legacy report review wrapper must delegate')

for (const legacyReportPath of [
  "'/api/tasks/report-center-stats'",
  "'/api/tasks/report-center'",
  '`/api/tasks/${taskId}/report`',
  '`/api/tasks/${taskId}/report/review`',
  "'/api/tasks/report-review-stats'",
  '`/api/tasks/${taskId}/report/review-logs`',
  '`/api/tasks/${taskId}/report/versions`'
]) {
  if (taskApi.includes(legacyReportPath)) {
    throw new Error(`Frontend report route cutover must not call legacy report path directly: ${legacyReportPath}`)
  }
}

const stableConsumers = new Map([
  ['src/views/report/ResearchReportCenterView.vue', "from '../../api/report'"],
  ['src/utils/reportWorkbench.ts', "from '../api/report'"],
  ['src/views/task/TaskReportView.vue', "from '../../api/report'"],
  ['src/utils/taskActions.ts', "from '../api/report'"],
  ['src/components/report/ReportVersionHistoryPanel.vue', "from '../../api/report'"]
])

for (const [relativePath, expected] of stableConsumers) {
  assertIncludes(relativePath, await readText(relativePath), expected, 'Report consumer must use stable report API contract')
}

const reportWorkbench = await readText('src/utils/reportWorkbench.ts')
assertIncludes(
  'src/utils/reportWorkbench.ts',
  reportWorkbench,
  'fetchReportCenter(buildFetchParams',
  'Report workbench list must use stable report center contract'
)

if (reportWorkbench.includes('fetchTasks(')) {
  throw new Error('Report workbench list must not use deprecated task pagination as report truth source')
}

console.log('report-contract-check passed')
