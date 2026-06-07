import { readFile } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const rootDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')

async function readText(relativePath) {
  return readFile(path.join(rootDir, relativePath), 'utf8')
}

function assertIncludes(relativePath, text, expected, message) {
  if (!text.includes(expected)) {
    throw new Error(`${message}: ${relativePath} missing ${expected}`)
  }
}

function assertNotIncludes(relativePath, text, forbidden, message) {
  if (text.includes(forbidden)) {
    throw new Error(`${message}: ${relativePath} contains ${forbidden}`)
  }
}

const contracts = await readText('src/api/marketDataIngestContracts.ts')
for (const expected of [
  "marketEventStats: '/api/tasks/market-event-stats'",
  "marketEvents: '/api/tasks/market-events'",
  "marketEventIngestHistory: '/api/tasks/market-events/ingest-history'",
  "marketEventSourceConfigs: '/api/tasks/market-event-source-configs'",
  "marketEventBatchImportPreview: '/api/tasks/market-events/batch-import/preview'",
  "marketEventBatchImport: '/api/tasks/market-events/batch-import'",
  "marketEventMockIngest: '/api/tasks/market-events/mock-ingest'",
  "marketIntelligence: '/api/tasks/market-intelligence'",
  "marketIntelligenceStats: '/api/tasks/market-intelligence-stats'",
  "marketEvents: '/market-events'",
  "marketIntelligence: '/intelligence'"
]) {
  assertIncludes('src/api/marketDataIngestContracts.ts', contracts, expected, 'Stable market/data-ingest contract changed')
}

const taskApi = await readText('src/api/task.ts')
assertIncludes(
  'src/api/task.ts',
  taskApi,
  "import { MARKET_DATA_INGEST_API_CONTRACTS } from './marketDataIngestContracts'",
  'Market/data-ingest API must consume stable contracts'
)

for (const forbidden of [
  "get<MarketEventStats>('/api/tasks/market-event-stats')",
  "get<MarketEventPageData>('/api/tasks/market-events'",
  "post<MarketEventBatchImportResult>('/api/tasks/market-events/mock-ingest'",
  "get<MarketIntelligenceStats>('/api/tasks/market-intelligence-stats')",
  "get<MarketIntelligencePageData>('/api/tasks/market-intelligence'"
]) {
  assertNotIncludes('src/api/task.ts', taskApi, forbidden, 'Market/data-ingest API bypasses stable contracts')
}

for (const relativePath of [
  'src/router/index.ts',
  'src/layout/BasicLayout.vue',
  'src/views/DashboardView.vue'
]) {
  const source = await readText(relativePath)
  assertIncludes(
    relativePath,
    source,
    'MARKET_DATA_INGEST_ROUTE_CONTRACTS',
    'Market/intelligence route consumers must use stable route contracts'
  )
}

const marketEventView = await readText('src/views/report/MarketEventCenterView.vue')
for (const expected of ['syncMarketEventSource', 'mockIngestMarketEvents']) {
  assertIncludes('src/views/report/MarketEventCenterView.vue', marketEventView, expected, 'Market center lost source operation contract')
}

const modelAgentConfigView = await readText('src/views/report/ModelAgentConfigCenterView.vue')
for (const expected of ['previewMarketEventSource', 'diagnoseMarketEventSource']) {
  assertIncludes('src/views/report/ModelAgentConfigCenterView.vue', modelAgentConfigView, expected, 'Source operations lost preview/diagnose contract')
}

console.log('market-data-ingest-contract-check passed')
