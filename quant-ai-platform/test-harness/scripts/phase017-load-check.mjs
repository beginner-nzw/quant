import { execFile } from 'node:child_process'
import { promisify } from 'node:util'
import { readFile } from 'node:fs/promises'
import { setTimeout as sleep } from 'node:timers/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import {
  normalizeBaseUrl,
  percentile,
  requestJson
} from '../lib/phase017-http.mjs'

const execFileAsync = promisify(execFile)
const harnessDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const seedPath = process.env.PHASE017_SEED_FILE || path.join(harnessDir, 'data', 'phase017-seed.json')
const seed = JSON.parse(await readFile(seedPath, 'utf8'))
const baseUrl = normalizeBaseUrl()
const concurrency = Number(process.env.PHASE017_LOAD_CONCURRENCY || 8)
const totalTasks = Number(process.env.PHASE017_LOAD_TASKS || 24)
const maxP95Ms = Number(process.env.PHASE017_LOAD_MAX_P95_MS || 5000)
const createRetryCount = Number(process.env.PHASE017_LOAD_CREATE_RETRIES || 8)
const createRetryDelayMs = Number(process.env.PHASE017_LOAD_CREATE_RETRY_DELAY_MS || 500)
const frontendCooldownMs = Number(process.env.PHASE017_LOAD_FRONTEND_COOLDOWN_MS || 10000)
const kafkaProbeTimeoutMs = Number(process.env.PHASE017_KAFKA_PROBE_TIMEOUT_MS || 30000)

function parseKafkaOffsetOutput(stdout) {
  return stdout.trim().split(/\r?\n/).filter(Boolean).map((line) => {
    const parts = line.split(':')
    return Number(parts[2] || 0)
  }).reduce((sum, value) => sum + value, 0)
}

async function kafkaTopicOffset(topic) {
  try {
    const { stdout } = await execFileAsync('docker', [
      'exec',
      'quant-kafka',
      'kafka-get-offsets.sh',
      '--bootstrap-server',
      '127.0.0.1:9092',
      '--topic',
      topic
    ], { timeout: kafkaProbeTimeoutMs })
    return parseKafkaOffsetOutput(stdout)
  } catch (firstError) {
    const { stdout } = await execFileAsync('docker', [
      'exec',
      'quant-kafka',
      'kafka-run-class.sh',
      'kafka.tools.GetOffsetShell',
      '--broker-list',
      '127.0.0.1:9092',
      '--topic',
      topic
    ], { timeout: kafkaProbeTimeoutMs })
    try {
      return parseKafkaOffsetOutput(stdout)
    } catch (parseError) {
      parseError.message = `Kafka offset parse failed for ${topic}: ${parseError.message}`
      throw parseError
    }
  }
}

async function dockerKafkaOffsets() {
  const topics = ['ai.task.dispatch', 'ai.task.status', 'ai.task.result', 'ai.task.audit', 'ai.task.deadletter']
  const result = {}
  for (const topic of topics) {
    try {
      result[topic] = await kafkaTopicOffset(topic)
    } catch (error) {
      throw new Error(`Kafka offset probe failed for ${topic} after ${kafkaProbeTimeoutMs}ms budget: ${error.message}`)
    }
  }
  return result
}

async function runLimited(items, limit, worker) {
  const results = []
  let index = 0
  async function loop() {
    while (index < items.length) {
      const current = items[index++]
      results[current] = await worker(current)
    }
  }
  await Promise.all(Array.from({ length: Math.min(limit, items.length) }, loop))
  return results
}

async function createTaskWithBackoff(body) {
  let lastError
  for (let attempt = 0; attempt <= createRetryCount; attempt += 1) {
    try {
      return await requestJson('/api/research/tasks', {
        baseUrl,
        method: 'POST',
        body
      })
    } catch (error) {
      lastError = error
      const retryable = /RATE_LIMITED|HOT_TARGET_LIMITED|TASK_DUPLICATE|HTTP 503|Service Temporarily Unavailable/.test(error.message)
      if (!retryable || attempt === createRetryCount) {
        throw error
      }
      await sleep(createRetryDelayMs * (attempt + 1))
    }
  }
  throw lastError
}

async function requestWithBackoff(pathName) {
  let lastError
  for (let attempt = 0; attempt <= createRetryCount; attempt += 1) {
    try {
      return await requestJson(pathName, { baseUrl })
    } catch (error) {
      lastError = error
      const retryable = /RATE_LIMITED|HTTP 503|Service Temporarily Unavailable|failed before response/.test(error.message)
      if (!retryable || attempt === createRetryCount) {
        throw error
      }
      await sleep(createRetryDelayMs * (attempt + 1))
    }
  }
  throw lastError
}

const beforeOffsets = await dockerKafkaOffsets()
console.log(`kafka offsets before: ${JSON.stringify(beforeOffsets)}`)

const timings = await runLimited(Array.from({ length: totalTasks }, (_, index) => index), concurrency, async (index) => {
  const taskTemplate = seed.tasks[index % seed.tasks.length]
  const body = {
    ...taskTemplate,
    taskTitle: `${taskTemplate.taskTitle} load-${index}-${Date.now()}`,
    targetCode: `${taskTemplate.targetCode || 'PH17'}-LOAD-${Date.now()}-${index}`,
    sourceChannel: 'PHASE_017_LOAD',
    sourceDomain: 'LOAD_TEST',
    analysisScope: `${taskTemplate.analysisScope || 'load'},load-${index}`
  }
  const result = await createTaskWithBackoff(body)
  return result.durationMs
})

if (frontendCooldownMs > 0) {
  await sleep(frontendCooldownMs)
}

const criticalPaths = [
  '/api/tasks/stats',
  '/api/tasks?pageNo=1&pageSize=10',
  '/api/tasks/research-workbench?pageNo=1&pageSize=10',
  '/api/tasks/risk-warnings?pageNo=1&pageSize=10',
  '/api/tasks/strategy-signals?pageNo=1&pageSize=10',
  '/api/reports/center?pageNo=1&pageSize=10',
  '/api/tasks/audit-compliance?pageNo=1&pageSize=10',
  '/api/tasks/human-reviews?pageNo=1&pageSize=10'
]
const frontendFlowTimings = await Promise.all(criticalPaths.map(async (pathName) => {
  const result = await requestWithBackoff(pathName)
  return { path: pathName, durationMs: result.durationMs }
}))

const afterOffsets = await dockerKafkaOffsets()
console.log(`kafka offsets after: ${JSON.stringify(afterOffsets)}`)

const p95TaskCreateMs = percentile(timings, 95)
const p95FrontendMs = percentile(frontendFlowTimings.map((item) => item.durationMs), 95)
const dispatchDelta = (afterOffsets['ai.task.dispatch'] || 0) - (beforeOffsets['ai.task.dispatch'] || 0)

if (dispatchDelta < totalTasks) {
  throw new Error(`Kafka backlog coverage expected at least ${totalTasks} dispatch messages, observed delta ${dispatchDelta}`)
}
if (p95TaskCreateMs > maxP95Ms) {
  throw new Error(`AI workflow concurrency task creation p95 ${p95TaskCreateMs}ms exceeded ${maxP95Ms}ms`)
}
if (p95FrontendMs > maxP95Ms) {
  throw new Error(`Frontend critical flow p95 ${p95FrontendMs}ms exceeded ${maxP95Ms}ms`)
}

console.log(JSON.stringify({
  ok: true,
  totalTasks,
  concurrency,
  kafkaDispatchDelta: dispatchDelta,
  p95TaskCreateMs,
  p95FrontendMs,
  frontendFlowTimings
}, null, 2))
