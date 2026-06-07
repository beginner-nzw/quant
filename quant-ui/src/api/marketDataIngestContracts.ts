export const MARKET_DATA_INGEST_API_CONTRACTS = {
  marketEventStats: '/api/tasks/market-event-stats',
  marketEvents: '/api/tasks/market-events',
  marketEvent: (eventId: string) => `/api/tasks/market-events/${eventId}`,
  marketEventIngestHistory: '/api/tasks/market-events/ingest-history',
  marketEventSourceConfigs: '/api/tasks/market-event-source-configs',
  marketEventBatchImportPreview: '/api/tasks/market-events/batch-import/preview',
  marketEventBatchImport: '/api/tasks/market-events/batch-import',
  marketEventMockIngest: '/api/tasks/market-events/mock-ingest',
  marketEventSourceSync: (sourceCode: string) => `/api/tasks/market-events/source-sync/${sourceCode}`,
  marketEventSourcePreview: (sourceCode: string) => `/api/tasks/market-events/source-preview/${sourceCode}`,
  marketEventSourceDiagnose: (sourceCode: string) => `/api/tasks/market-events/source-diagnose/${sourceCode}`,
  marketIntelligence: '/api/tasks/market-intelligence',
  marketIntelligenceStats: '/api/tasks/market-intelligence-stats'
} as const

export const MARKET_DATA_INGEST_ROUTE_CONTRACTS = {
  marketEvents: '/market-events',
  marketIntelligence: '/intelligence'
} as const
