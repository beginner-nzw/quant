import { execFile } from 'node:child_process'
import { promisify } from 'node:util'
import { readFile } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { requestJson, normalizeBaseUrl, waitFor } from '../lib/phase017-http.mjs'

const execFileAsync = promisify(execFile)
const baseUrl = normalizeBaseUrl()
const harnessDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const seedPath = process.env.PHASE017_SEED_FILE || path.join(harnessDir, 'data', 'phase017-seed.json')
const seed = JSON.parse(await readFile(seedPath, 'utf8'))
const platformDir = path.resolve(harnessDir, '..')
const composeCwd = process.env.PHASE017_COMPOSE_CWD || platformDir
const serviceContainers = {
  'ai-engine-consumer': 'quant-ai-engine-consumer',
  'ai-engine-worker': 'quant-ai-engine-worker',
  'ai-orchestration-service': 'quant-ai-orchestration-service',
  gateway: 'quant-gateway',
  kafka: 'quant-kafka',
  redis: 'quant-redis'
}

function containerNameForService(serviceName) {
  return serviceContainers[serviceName] || serviceName
}

async function docker(args) {
  return execFileAsync('docker', args, {
    cwd: composeCwd,
    timeout: Number(process.env.PHASE017_DOCKER_TIMEOUT_MS || 120000)
  })
}

async function assertKafkaReachable() {
  await docker([
    'exec',
    containerNameForService('kafka'),
    'kafka-topics.sh',
    '--bootstrap-server',
    '127.0.0.1:9092',
    '--list'
  ])
}

async function assertRedisReachable() {
  const { stdout } = await docker(['exec', containerNameForService('redis'), 'redis-cli', 'ping'])
  if (!stdout.trim().includes('PONG')) {
    throw new Error(`redis ping returned unexpected output: ${stdout.trim()}`)
  }
}

async function waitForContainerReady(serviceName) {
  const timeoutMs = Number(process.env.PHASE017_RESTART_TIMEOUT_MS || 360000)
  const intervalMs = Number(process.env.PHASE017_RESTART_INTERVAL_MS || 5000)
  const deadline = Date.now() + timeoutMs
  let lastState = 'unknown'

  while (Date.now() < deadline) {
    try {
      const inspect = await docker([
        'inspect',
        '--format',
        '{{.State.Status}} {{if .State.Health}}{{.State.Health.Status}}{{else}}no-healthcheck{{end}}',
        containerNameForService(serviceName)
      ])
      lastState = inspect.stdout.trim()
      const [status, health] = lastState.split(/\s+/)
      if (status === 'running' && (health === 'healthy' || health === 'no-healthcheck')) {
        return
      }
    } catch (error) {
      lastState = error.message
    }
    await new Promise((resolve) => setTimeout(resolve, intervalMs))
  }

  throw new Error(`Timed out waiting for ${serviceName} container readiness: ${lastState}`)
}

async function restartService(serviceName, healthPath, predicate = (data) => data?.status === 'ready' || data?.status === 'ok' || data?.status === 'UP') {
  console.log(`restarting ${serviceName}`)
  await docker(['restart', containerNameForService(serviceName)])
  await waitForContainerReady(serviceName)
  if (serviceName !== 'gateway') {
    await docker(['restart', containerNameForService('gateway')])
    await waitForContainerReady('gateway')
  }
  await waitFor(healthPath, predicate, {
    baseUrl,
    timeoutMs: Number(process.env.PHASE017_GATEWAY_RECOVERY_TIMEOUT_MS || 360000),
    intervalMs: 5000
  })
}

async function stopProbeAndRecover(serviceName, outageProbe, recoveryProbe = outageProbe) {
  console.log(`stopping ${serviceName}`)
  let outageObserved = false
  let outcome = 'outage was not observed'
  await docker(['stop', containerNameForService(serviceName)])
  await waitForContainerStopped(serviceName)
  try {
    await outageProbe()
  } catch (error) {
    outageObserved = true
    outcome = `probe failed during outage: ${error.message}`
    console.log(`${serviceName} outage observed: ${error.message}`)
  } finally {
    console.log(`starting ${serviceName}`)
    await docker(['start', containerNameForService(serviceName)])
    await waitForContainerReady(serviceName)
  }

  if (!outageObserved) {
    throw new Error(`${serviceName} outage was not observed; probe unexpectedly succeeded while ${serviceName} was stopped`)
  }

  await recoveryProbe()
  return outcome
}

async function waitForContainerStopped(serviceName) {
  const timeoutMs = Number(process.env.PHASE017_RESTART_TIMEOUT_MS || 360000)
  const intervalMs = Number(process.env.PHASE017_RESTART_INTERVAL_MS || 5000)
  const deadline = Date.now() + timeoutMs
  let lastState = 'unknown'

  while (Date.now() < deadline) {
    try {
      const inspect = await docker(['inspect', '--format', '{{.State.Status}}', containerNameForService(serviceName)])
      lastState = inspect.stdout.trim()
      if (lastState !== 'running') {
        return
      }
    } catch (error) {
      lastState = error.message
    }
    await new Promise((resolve) => setTimeout(resolve, intervalMs))
  }

  throw new Error(`Timed out waiting for ${serviceName} container stop: ${lastState}`)
}

await requestJson('/health', { baseUrl })
await requestJson('/api/tasks/stats', { baseUrl })

await restartService('ai-engine-consumer', '/engine/ready')
await restartService('ai-engine-worker', '/engine/ready')
await restartService('ai-orchestration-service', '/api/tasks/stats', (data) => Boolean(data))

const kafkaOutageOutcome = await stopProbeAndRecover('kafka', assertKafkaReachable)
await waitFor('/api/tasks/stats', (data) => Boolean(data), { baseUrl, timeoutMs: 180000, intervalMs: 5000 })

const redisOutageOutcome = await stopProbeAndRecover('redis', assertRedisReachable)
await waitFor('/api/tasks/stats', (data) => Boolean(data), { baseUrl, timeoutMs: 180000, intervalMs: 5000 })

const recoveryTaskTemplate = seed.tasks.find((item) => item.sourceDomain === 'RECOVERY_TEST') || seed.tasks[0]
const uniqueSuffix = Date.now()
const timeoutTask = await requestJson('/api/research/tasks', {
  baseUrl,
  method: 'POST',
  body: {
    ...recoveryTaskTemplate,
    taskTitle: `${recoveryTaskTemplate.taskTitle} ${new Date(uniqueSuffix).toISOString()}`,
    targetCode: `${recoveryTaskTemplate.targetCode || 'PH17-RECOVERY'}-${uniqueSuffix}`,
    sourceChannel: 'PHASE_017_FAILURE',
    analysisScope: `${recoveryTaskTemplate.analysisScope || 'recovery'},ai-timeout,checkpoint-recovery`
  }
})
const taskId = String(timeoutTask.data)
await requestJson(`/api/tasks/${encodeURIComponent(taskId)}/full`, { baseUrl })
await waitFor(`/api/tasks/${encodeURIComponent(taskId)}/workflow`, (workflow) => Boolean(workflow), {
  baseUrl,
  timeoutMs: Number(process.env.PHASE017_CHECKPOINT_TIMEOUT_MS || 360000),
  intervalMs: 5000
})
await docker(['restart', containerNameForService('ai-engine-consumer')])
await waitForContainerReady('ai-engine-consumer')
await requestJson(`/api/tasks/${encodeURIComponent(taskId)}/resume`, {
  baseUrl,
  method: 'POST',
  body: {
    operatorId: 'phase017-runner',
    reason: 'Phase 17 checkpoint recovery verification after service restart.'
  }
})
await requestJson(`/api/tasks/${encodeURIComponent(taskId)}/workflow`, { baseUrl })
await requestJson(`/api/tasks/${encodeURIComponent(taskId)}/audits`, { baseUrl })

console.log(JSON.stringify({
  ok: true,
  covered: [
    'service restart',
    'Kafka down',
    'Redis down',
    'AI timeout probe',
    'checkpoint recovery resume'
  ],
  checkpointTaskId: taskId,
  outageOutcomes: {
    kafka: kafkaOutageOutcome,
    redis: redisOutageOutcome
  }
}, null, 2))
