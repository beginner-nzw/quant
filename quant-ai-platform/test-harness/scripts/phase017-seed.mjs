import { readFile } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { requestJson, normalizeBaseUrl } from '../lib/phase017-http.mjs'

const harnessDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const seedPath = process.env.PHASE017_SEED_FILE || path.join(harnessDir, 'data', 'phase017-seed.json')
const seed = JSON.parse(await readFile(seedPath, 'utf8'))
const baseUrl = normalizeBaseUrl()
const created = {
  tasks: [],
  marketEvents: []
}

for (const marketEvent of seed.marketEvents || []) {
  const result = await requestJson('/api/tasks/market-events', {
    baseUrl,
    method: 'POST',
    body: marketEvent
  })
  created.marketEvents.push(result.data)
  console.log(`seeded market event: ${marketEvent.eventTitle}`)
}

for (const task of seed.tasks || []) {
  const result = await requestJson('/api/research/tasks', {
    baseUrl,
    method: 'POST',
    body: task
  })
  created.tasks.push(result.data)
  console.log(`seeded task: ${task.taskTitle} -> ${result.data}`)
}

console.log(JSON.stringify({ baseUrl, created }, null, 2))
