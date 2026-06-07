import {
  getMarketEventImpactText,
  getMarketEventStatusText,
  getMarketEventTypeText
} from '../../../utils/task'
import {
  MARKET_EVENT_IMPACT_LEVEL,
  MARKET_EVENT_STATUS,
  MARKET_EVENT_TYPE
} from '../../../types/taskEnums'

export const defaultStats = {
  totalCount: 0,
  activeCount: 0,
  highImpactCount: 0,
  trackedCount: 0,
  todayCount: 0
}

export const eventTypeOptions = Object.values(MARKET_EVENT_TYPE).map((value) => ({
  label: getMarketEventTypeText(value),
  value
}))

export const impactLevelOptions = Object.values(MARKET_EVENT_IMPACT_LEVEL).map((value) => ({
  label: getMarketEventImpactText(value),
  value
}))

export const eventStatusOptions = Object.values(MARKET_EVENT_STATUS).map((value) => ({
  label: getMarketEventStatusText(value),
  value
}))

export const eventTypeValueSet = new Set<string>(Object.values(MARKET_EVENT_TYPE))
export const impactLevelValueSet = new Set<string>(Object.values(MARKET_EVENT_IMPACT_LEVEL))
export const eventStatusValueSet = new Set<string>(Object.values(MARKET_EVENT_STATUS))

export const followUpStatusTextMap: Record<string, string> = {
  NOT_TRACKED: '未跟踪',
  TRACKING: '跟踪中',
  COMPLETED: '已完成',
  FAILED: '跟踪失败'
}

export const followUpStatusTagTypeMap: Record<string, 'info' | 'warning' | 'success' | 'danger'> = {
  NOT_TRACKED: 'info',
  TRACKING: 'warning',
  COMPLETED: 'success',
  FAILED: 'danger'
}

export const autoTriggerStatusTextMap: Record<string, string> = {
  DISABLED: '已关闭',
  NO_MATCH: '未命中',
  SUCCESS: '已创建任务',
  FAILED: '触发失败',
  WILL_TRIGGER: '已入队',
  DISPATCHING: '触发中',
  SKIPPED_DUPLICATE: '重复跳过',
  INVALID: '校验失败',
  RATE_LIMITED: '限流中'
}

export const autoTriggerStatusTagTypeMap: Record<string, 'info' | 'warning' | 'success' | 'danger'> = {
  DISABLED: 'info',
  NO_MATCH: 'warning',
  SUCCESS: 'success',
  FAILED: 'danger',
  WILL_TRIGGER: 'success',
  DISPATCHING: 'warning',
  SKIPPED_DUPLICATE: 'warning',
  INVALID: 'danger',
  RATE_LIMITED: 'warning'
}

export const duplicateSourceTextMap: Record<string, string> = {
  EXISTING_EVENT: '命中已有事件',
  SAME_BATCH: '批次内重复'
}

export const ingestSourceTagTypeMap: Record<string, 'info' | 'warning' | 'success' | 'primary' | 'danger'> = {
  MANUAL_CREATE: 'primary',
  BATCH_IMPORT: 'info',
  MOCK_INGEST: 'success',
  MANUAL: 'primary',
  IMPORT: 'info',
  NEWS: 'success',
  ANNOUNCEMENT: 'warning',
  POLICY: 'primary',
  RISK: 'danger',
  MOCK: 'success'
}

export const defaultMockSourceOptions = [
  { label: '新闻快讯源', value: 'NEWS_WIRE' },
  { label: '交易所公告源', value: 'EXCHANGE_ANNOUNCEMENT' },
  { label: '政策跟踪源', value: 'POLICY_TRACKER' },
  { label: '风险监测源', value: 'RISK_MONITOR' }
]

export const previewInvalidFieldTextMap: Record<string, string> = {
  events: '导入内容',
  targetCode: '标的代码',
  targetName: '标的名称',
  eventType: '事件类型',
  eventTitle: '事件标题',
  eventSummary: '事件摘要',
  impactLevel: '影响等级',
  occurredAt: '发生时间'
}
