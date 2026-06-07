import { readdir, readFile } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const rootDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const srcDir = path.join(rootDir, 'src')

const commandApiNames = [
  'retryTask',
  'cancelTask',
  'reviewTaskReport',
  'createStrategySignal',
  'updateStrategySignalStatus',
  'createMarketEvent',
  'batchImportMarketEvents',
  'mockIngestMarketEvents',
  'syncMarketEventSource',
  'updatePromptTemplate',
  'updateModelStrategy',
  'updateEventAutoTriggerRule',
  'updateEventSourceConfig',
  'updateAgentConfig',
  'updateWorkflowConfig',
  'updateRoleAccessConfig'
]

const workbenchFiles = [
  'src/views/report/ResearchWorkbenchView.vue',
  'src/utils/researchWorkbench.ts',
  'src/utils/taskActionAccess.ts'
]

const commandBuilderFiles = [
  'src/utils/taskActions.ts',
  'src/utils/taskCreate.ts',
  'src/utils/taskNavigation.ts',
  'src/utils/taskActionAccess.ts',
  'src/utils/researchWorkbench.ts',
  'src/views/report/ResearchWorkbenchView.vue'
]

const provenanceTokens = [
  'contextSnapshot',
  'generationMode',
  'fallbackReason',
  'reportFallbackReason',
  'planningFallbackReason',
  'intentFallbackReason',
  'marketDataSource'
]

const allowedReportMetaCommandRefs = new Map([
  ['src/views/task/TaskDetailView.vue', ['report?.reportMeta?.reportId']]
])

async function readText(relativePath) {
  return readFile(path.join(rootDir, relativePath), 'utf8')
}

async function listFiles(dir) {
  const entries = await readdir(dir, { withFileTypes: true })
  const nested = await Promise.all(entries.map(async (entry) => {
    const fullPath = path.join(dir, entry.name)
    if (entry.isDirectory()) return listFiles(fullPath)
    return [fullPath]
  }))
  return nested.flat()
}

function toRelative(filePath) {
  return path.relative(rootDir, filePath).replaceAll(path.sep, '/')
}

function stripComments(text) {
  return text
    .replace(/\/\*[\s\S]*?\*\//g, '')
    .replace(/^\s*\/\/.*$/gm, '')
}

function assertNoRegex(relativePath, text, regex, message) {
  const match = regex.exec(text)
  if (match) {
    throw new Error(`${message}: ${relativePath} matched "${match[0]}"`)
  }
}

function assertIncludes(relativePath, text, expected, message) {
  if (!text.includes(expected)) {
    throw new Error(`${message}: ${relativePath}`)
  }
}

function getFunctionBody(source, functionName) {
  const startPattern = new RegExp(`(?:async\\s+)?function\\s+${functionName}\\s*\\([^)]*\\)\\s*{`, 'm')
  const match = startPattern.exec(source)
  if (!match) {
    throw new Error(`Expected function not found: ${functionName}`)
  }

  let depth = 0
  let bodyStart = -1
  for (let index = match.index; index < source.length; index += 1) {
    const char = source[index]
    if (char === '{') {
      depth += 1
      if (bodyStart === -1) bodyStart = index
    } else if (char === '}') {
      depth -= 1
      if (depth === 0 && bodyStart !== -1) {
        return source.slice(bodyStart, index + 1)
      }
    }
  }

  throw new Error(`Function body not closed: ${functionName}`)
}

function assertFunctionBodyNoTokens(relativePath, source, functionName, tokens, message, allowedRefs = []) {
  const body = allowedRefs.reduce(
    (result, ref) => result.replaceAll(ref, ''),
    stripComments(getFunctionBody(source, functionName))
  )
  assertNoRegex(
    relativePath,
    body,
    new RegExp(`\\b(${tokens.join('|')})\\b`),
    `${message}: ${functionName}`
  )
}

for (const relativePath of workbenchFiles) {
  const source = stripComments(await readText(relativePath))
  assertNoRegex(
    relativePath,
    source,
    new RegExp(`\\b(${commandApiNames.join('|')})\\b`),
    'Workbench display boundary cannot call command APIs'
  )
}

for (const relativePath of commandBuilderFiles) {
  const source = stripComments(await readText(relativePath))
  assertNoRegex(
    relativePath,
    source,
    new RegExp(`\\b(${provenanceTokens.join('|')})\\b`),
    'Fallback/report provenance must not feed command or route helpers'
  )
}

const taskReportView = await readText('src/views/task/TaskReportView.vue')
for (const functionName of ['handleSaveReview', 'handleApprove', 'handleReject']) {
  assertFunctionBodyNoTokens(
    'src/views/task/TaskReportView.vue',
    taskReportView,
    functionName,
    [...provenanceTokens, 'reportMeta'],
    'Report review commands must not consume fallback/report provenance'
  )
}

assertFunctionBodyNoTokens(
  'src/views/task/TaskReportView.vue',
  taskReportView,
  'handleCreateTask',
  [...provenanceTokens, 'reportMeta'],
  'Task-create prefill must not promote fallback/report provenance',
  ['report.reportMeta?.reportId']
)

const taskDetailView = await readText('src/views/task/TaskDetailView.vue')
assertFunctionBodyNoTokens(
  'src/views/task/TaskDetailView.vue',
  taskDetailView,
  'handleCreateSimilarTask',
  [...provenanceTokens, 'reportMeta'],
  'Task-create prefill must not promote fallback/report provenance',
  ['report?.reportMeta?.reportId']
)

for (const filePath of await listFiles(srcDir)) {
  const relativePath = toRelative(filePath)
  const source = stripComments(await readText(relativePath))
  if (!source.includes('reportMeta')) continue

  const allowedRefs = allowedReportMetaCommandRefs.get(relativePath) || []
  const sourceWithoutAllowedRefs = allowedRefs.reduce(
    (result, ref) => result.replaceAll(ref, ''),
    source
  )

  if (sourceWithoutAllowedRefs.includes('reportMeta') && commandBuilderFiles.includes(relativePath)) {
    throw new Error(`reportMeta must not feed command helpers: ${relativePath}`)
  }
}

const taskTypes = await readText('src/types/task.ts')
assertIncludes(
  'src/types/task.ts',
  taskTypes,
  'Display-only aggregation returned by /api/tasks/research-workbench.',
  'ResearchWorkbenchData authority note is missing'
)
assertIncludes(
  'src/types/task.ts',
  taskTypes,
  'Display/audit metadata from report generation.',
  'TaskReportContextSnapshot authority note is missing'
)

const taskApi = await readText('src/api/task.ts')
assertIncludes(
  'src/api/task.ts',
  taskApi,
  "get<ResearchWorkbenchData>('/api/tasks/research-workbench', params)",
  'Research workbench API contract changed'
)
assertIncludes(
  'src/api/task.ts',
  taskApi,
  "from './report'",
  'Legacy task API report wrappers must delegate to the stable report client'
)

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
    throw new Error(`Frontend report consumers must use stable /api/reports client, found ${legacyReportPath} in src/api/task.ts`)
  }
}

const reportApi = await readText('src/api/report.ts')
assertIncludes(
  'src/api/report.ts',
  reportApi,
  "const REPORT_API_BASE = '/api/reports'",
  'Stable report API base changed'
)

const researchWorkbenchView = stripComments(await readText('src/views/report/ResearchWorkbenchView.vue'))
assertNoRegex(
  'src/views/report/ResearchWorkbenchView.vue',
  researchWorkbenchView,
  /sourceReviewStatus\s*:/,
  'Research workbench aggregation must not pass derived domain truth into task-create prefill'
)

const reportWorkbench = stripComments(await readText('src/utils/reportWorkbench.ts'))
assertIncludes(
  'src/utils/reportWorkbench.ts',
  reportWorkbench,
  "import { fetchReportCenter, fetchReportReviewStats } from '../api/report'",
  'Report workbench must use stable report API contract'
)
assertNoRegex(
  'src/utils/reportWorkbench.ts',
  reportWorkbench,
  /\bfetchTasks\b/,
  'Report workbench must not use deprecated task pagination as report truth source'
)
assertNoRegex(
  'src/utils/reportWorkbench.ts',
  reportWorkbench,
  /sourceReviewStatus\s*:/,
  'Report workbench must not pass report review truth into task-create prefill'
)

console.log('authority-boundary-check passed')
