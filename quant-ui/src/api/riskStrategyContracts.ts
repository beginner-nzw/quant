export const RISK_STRATEGY_API_CONTRACTS = {
  riskWarnings: '/api/tasks/risk-warnings',
  riskWarningStats: '/api/tasks/risk-warning-stats',
  strategySignals: '/api/tasks/strategy-signals',
  strategySignalStats: '/api/tasks/strategy-signal-stats',
  strategySignalFactors: (signalId: string) => `/api/tasks/strategy-signals/${signalId}/factors`,
  strategySignalStatus: (signalId: string) => `/api/tasks/strategy-signals/${signalId}/status`
} as const

export const RISK_STRATEGY_ROUTE_CONTRACTS = {
  riskWarnings: '/risk-warnings',
  strategySignals: '/signals'
} as const
