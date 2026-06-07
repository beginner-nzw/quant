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

const checks = [
  {
    file: 'src/layout/BasicLayout.vue',
    tokens: ['sessionModeLabel', 'canSwitchDemoRole', '权限以后端校验为准']
  },
  {
    file: 'src/views/report/MarketEventCenterView.vue',
    tokens: ['handleSourcePreview', 'handleSourceDiagnose', 'submitSourceSync']
  },
  {
    file: 'src/views/task/TaskDetailView.vue',
    tokens: ['executeTaskResume', 'executeTaskNodeRerun', 'canRetryTasks()']
  },
  {
    file: 'src/components/report/HumanReviewQueuePanel.vue',
    tokens: ['fetchHumanReviewQueue', 'decideHumanReview', 'getExceptionMessage']
  },
  {
    file: 'src/views/task/TaskReportView.vue',
    tokens: ['ReportVersionComparison', 'ReportEvidenceView', 'exportMarkdown']
  }
]

for (const check of checks) {
  const text = await readText(check.file)
  for (const token of check.tokens) {
    assertIncludes(check.file, text, token, 'Production workflow smoke path is incomplete')
  }
}

console.log('e2e-smoke-check passed')
