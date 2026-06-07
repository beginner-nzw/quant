import { execFile } from 'node:child_process'
import path from 'node:path'
import { setTimeout as sleep } from 'node:timers/promises'
import { fileURLToPath } from 'node:url'

import { normalizeBaseUrl, requestJson } from '../lib/phase017-http.mjs'

const scriptDir = path.dirname(fileURLToPath(import.meta.url))
const platformDir = path.resolve(scriptDir, '..', '..')
const composeDir = path.join(platformDir, 'docker', 'compose')
const requiredContainers = [
  'quant-kafka',
  'quant-redis',
  'quant-mysql',
  'quant-ai-orchestration-service',
  'quant-research-task-service',
  'quant-ai-engine-worker',
  'quant-ai-engine-consumer',
  'quant-gateway'
]
const singleNetworkContainers = [
  'quant-kafka',
  'quant-ai-engine-worker',
  'quant-ai-engine-consumer',
  'quant-gateway'
]
const runtimeNetwork = 'compose_quant_net'
const containerHealthTimeoutMs = Number(process.env.PHASE017_CONTAINER_HEALTH_TIMEOUT_MS || 90000)
const containerHealthPollMs = Number(process.env.PHASE017_CONTAINER_HEALTH_POLL_MS || 3000)

function run(command, args, options = {}) {
  return new Promise((resolve, reject) => {
    const child = execFile(command, args, {
      cwd: options.cwd || platformDir,
      timeout: options.timeoutMs || 20000,
      windowsHide: true
    }, (error, stdout, stderr) => {
      if (error) {
        error.stdout = stdout
        error.stderr = stderr
        reject(error)
        return
      }
      resolve({ stdout, stderr })
    })
    child.stdin?.end()
  })
}

async function docker(args, options = {}) {
  return run('docker', args, options)
}

function section(title) {
  console.log(`\n[phase017-preflight] ${title}`)
}

function line(message) {
  console.log(`  ${message}`)
}

function fail(message, hints = []) {
  const suffix = hints.length ? `\n${hints.map((hint) => `  - ${hint}`).join('\n')}` : ''
  throw new Error(`${message}${suffix}`)
}

function parseTableRows(text) {
  return text
    .split(/\r?\n/)
    .map((row) => row.trim())
    .filter(Boolean)
}

async function assertDockerEngine() {
  section('Docker engine')
  try {
    const result = await docker(['version', '--format', '{{.Server.Version}}'], { timeoutMs: 15000 })
    line(`server=${result.stdout.trim()}`)
  } catch (error) {
    fail('Docker engine is not reachable; Phase 17 runtime checks cannot start.', [
      `docker error: ${(error.stderr || error.stdout || error.message).trim()}`,
      'Start Docker Desktop and wait until `docker ps` succeeds.',
      'If Docker Desktop is already open, restart Docker Desktop or start the Windows Docker Desktop Service from an elevated shell.'
    ])
  }
}

async function assertComposeNetworkConfig() {
  section('Compose network config')
  const result = await docker(['compose', '--profile', 'runtime', 'config'], {
    cwd: composeDir,
    timeoutMs: 30000
  })
  if (!result.stdout.includes(`name: ${runtimeNetwork}`)) {
    fail(`docker compose config does not pin quant_net to ${runtimeNetwork}.`, [
      `Run from ${composeDir}; partial compose starts from another project can create the wrong network.`
    ])
  }
  line(`quant_net=${runtimeNetwork}`)
}

async function inspectContainer(name, format) {
  const result = await docker(['inspect', '-f', format, name], { timeoutMs: 15000 })
  return result.stdout.trim()
}

async function loadContainerTable() {
  const result = await docker([
    'ps',
    '-a',
    '--format',
    '{{.Names}}\t{{.Status}}\t{{.Image}}\t{{.Command}}'
  ], { timeoutMs: 20000 })
  const rows = parseTableRows(result.stdout)
  return new Map(rows.map((row) => {
    const [name, status, image, command] = row.split('\t')
    return [name, { status, image, command }]
  }))
}

function containerIsHealthy(item) {
  return item?.status?.startsWith('Up')
    && !item.status.includes('(unhealthy)')
    && !item.status.includes('(health: starting)')
}

async function waitForContainersHealthy(byName) {
  const starting = requiredContainers.filter((name) => byName.get(name)?.status.includes('(health: starting)'))
  if (!starting.length) {
    return byName
  }

  line(`waiting for container health: ${starting.join(', ')}`)
  const deadline = Date.now() + containerHealthTimeoutMs
  let current = byName

  while (Date.now() < deadline) {
    await sleep(containerHealthPollMs)
    current = await loadContainerTable()
    const pending = requiredContainers.filter((name) => current.get(name)?.status.includes('(health: starting)'))
    const unhealthy = requiredContainers.filter((name) => current.get(name)?.status.includes('(unhealthy)'))
    if (unhealthy.length) {
      return current
    }
    if (!pending.length && requiredContainers.every((name) => containerIsHealthy(current.get(name)))) {
      return current
    }
  }

  return current
}

async function assertContainers() {
  section('Required containers')
  let byName = await loadContainerTable()

  const missing = requiredContainers.filter((name) => !byName.has(name))
  if (missing.length) {
    fail(`Missing runtime containers: ${missing.join(', ')}`, [
      `cd ${composeDir}`,
      'docker compose --profile runtime up -d --build'
    ])
  }

  for (const name of requiredContainers) {
    const item = byName.get(name)
    if (!item.status.startsWith('Up')) {
      fail(`${name} is not running: ${item.status}`, [
        `cd ${composeDir}`,
        'docker compose --profile runtime up -d --build --no-deps ai-engine-worker ai-engine-consumer gateway'
      ])
    }
  }

  byName = await waitForContainersHealthy(byName)

  for (const name of requiredContainers) {
    const item = byName.get(name)
    line(`${name}: ${item.status} image=${item.image}`)
    if (item.status.includes('(unhealthy)') || item.status.includes('(health: starting)')) {
      fail(`${name} is not healthy yet: ${item.status}`, [
        `docker logs ${name} --tail 120`,
        'Do not run e2e until all runtime containers are healthy.'
      ])
    }
  }

  const workerImage = byName.get('quant-ai-engine-worker')?.image || ''
  const consumerImage = byName.get('quant-ai-engine-consumer')?.image || ''
  if (!workerImage.includes('phase-017') || !consumerImage.includes('phase-017')) {
    fail('AI engine containers are not running the Phase 17 image tag.', [
      `worker image=${workerImage}`,
      `consumer image=${consumerImage}`,
      `cd ${composeDir}`,
      'docker compose --profile runtime up -d --build --no-deps ai-engine-worker ai-engine-consumer gateway'
    ])
  }
}

async function assertNetworkAttachment() {
  section('Docker network attachment')
  await docker(['network', 'inspect', runtimeNetwork], { timeoutMs: 15000 }).catch((error) => {
    fail(`${runtimeNetwork} does not exist.`, [
      (error.stderr || error.stdout || error.message).trim(),
      `cd ${composeDir}`,
      'docker compose --profile runtime up -d --build'
    ])
  })

  for (const name of singleNetworkContainers) {
    const networks = await inspectContainer(
      name,
      '{{range $name, $network := .NetworkSettings.Networks}}{{$name}} {{end}}'
    )
    line(`${name}: ${networks || '(none)'}`)
    if (!networks.split(/\s+/).includes(runtimeNetwork)) {
      fail(`${name} is not attached to ${runtimeNetwork}.`, [
        'This is the known stale-runtime blocker that leaves python-ai-engine-group without active members.',
        `Recreate stale services from ${composeDir}.`,
        'docker rm -f quant-ai-engine-worker quant-ai-engine-consumer quant-gateway',
        'docker compose --profile runtime up -d --build --no-deps ai-engine-worker ai-engine-consumer gateway'
      ])
    }
  }
}

async function assertGatewayAndEngine() {
  section('Gateway and engine readiness')
  const baseUrl = normalizeBaseUrl()
  line(`baseUrl=${baseUrl}`)

  await requestJson('/health', { baseUrl }).then((result) => {
    line(`/health=${JSON.stringify(result.data)}`)
  }).catch((error) => {
    fail('Gateway /health is not reachable.', [
      error.message,
      `docker logs quant-gateway --tail 120`
    ])
  })

  const ready = await requestJson('/engine/ready', { baseUrl }).catch((error) => {
    fail('Gateway /engine/ready is not reachable.', [
      error.message,
      'Check quant-gateway and quant-ai-engine-worker logs.'
    ])
  })
  line(`/engine/ready=${JSON.stringify(ready.data).slice(0, 800)}`)

  const status = String(ready.data?.status || '').toLowerCase()
  const groupActive = ready.data?.consumerGroup?.active
  if (status !== 'ready' || groupActive === false) {
    fail('AI engine is not business-ready; Kafka consumer group is not active.', [
      `ready=${JSON.stringify(ready.data).slice(0, 800)}`,
      'Do not run e2e yet; it would create another DISPATCHED task.',
      'Check `docker logs quant-ai-engine-consumer --tail 120`.'
    ])
  }
}

async function assertConsumerGroup() {
  section('Kafka consumer group')
  const result = await docker([
    'exec',
    'quant-kafka',
    'kafka-consumer-groups.sh',
    '--bootstrap-server',
    '127.0.0.1:9092',
    '--describe',
    '--group',
    'python-ai-engine-group'
  ], { timeoutMs: Number(process.env.PHASE017_KAFKA_PROBE_TIMEOUT_MS || 30000) })

  const output = result.stdout.trim()
  line(output || '(empty)')
  if (/has no active members/i.test(output)) {
    fail('python-ai-engine-group has no active members.', [
      'The AI engine consumer is not consuming ai.task.dispatch.',
      'Check consumer logs and network attachment before running e2e.'
    ])
  }

  const lagRows = output
    .split(/\r?\n/)
    .filter((row) => row.includes('ai.task.dispatch'))
  const lags = lagRows
    .map((row) => row.trim().split(/\s+/))
    .map((parts) => Number(parts[5]))
    .filter((value) => Number.isFinite(value))
  const totalLag = lags.reduce((sum, value) => sum + value, 0)
  line(`ai.task.dispatch totalLag=${totalLag}`)
}

async function main() {
  await assertDockerEngine()
  await assertComposeNetworkConfig()
  await assertContainers()
  await assertNetworkAttachment()
  await assertGatewayAndEngine()
  await assertConsumerGroup()
  console.log('\n[phase017-preflight] OK: runtime is ready for Phase 17 checks.')
}

main().catch((error) => {
  console.error(`\n[phase017-preflight] FAILED: ${error.message}`)
  process.exit(1)
})
