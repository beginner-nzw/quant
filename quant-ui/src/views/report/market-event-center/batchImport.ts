import { utils, write, type WorkBook } from 'xlsx'
import type { MarketEventCreateForm } from '../../../types/task'

export const batchImportExample = JSON.stringify([
  {
    targetType: 'STOCK',
    targetCode: '600519.SH',
    targetName: '贵州茅台',
    eventType: 'ANNOUNCEMENT',
    eventTitle: '贵州茅台披露季度经营数据',
    eventSummary: '公司披露季度经营数据，营收与利润保持稳健增长，市场关注后续估值消化情况。',
    sourceChannel: 'MANUAL_IMPORT',
    sourceUrl: '',
    impactLevel: 'HIGH',
    eventStatus: 'ACTIVE',
    occurredAt: '2026-04-04T09:30'
  },
  {
    targetType: 'STOCK',
    targetCode: '000001.SZ',
    targetName: '平安银行',
    eventType: 'EARNINGS',
    eventTitle: '平安银行发布业绩快报',
    eventSummary: '业绩快报显示净利润承压，市场关注零售业务修复节奏和拨备变化。',
    sourceChannel: 'MANUAL_IMPORT',
    sourceUrl: '',
    impactLevel: 'MEDIUM',
    eventStatus: 'ACTIVE',
    occurredAt: '2026-04-04T10:00'
  }
], null, 2)

export const batchImportCsvTemplate = [
  'targetType,targetCode,targetName,eventType,eventTitle,eventSummary,sourceChannel,sourceUrl,impactLevel,eventStatus,occurredAt',
  'STOCK,600519.SH,贵州茅台,ANNOUNCEMENT,贵州茅台披露季度经营数据,公司披露季度经营数据，营收与利润保持稳健增长，市场关注后续估值消化情况。,MANUAL_IMPORT,,HIGH,ACTIVE,2026-04-04T09:30',
  'STOCK,000001.SZ,平安银行,EARNINGS,平安银行发布业绩快报,业绩快报显示净利润承压，市场关注零售业务修复节奏和拨备变化。,MANUAL_IMPORT,,MEDIUM,ACTIVE,2026-04-04T10:00'
].join('\n')

export function parseBatchImportCsv(content: string): MarketEventCreateForm[] {
  const rows = content
    .replace(/^\uFEFF/, '')
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
  if (rows.length < 2) {
    throw new Error('empty csv')
  }

  const headerRow = rows[0]
  if (!headerRow) {
    throw new Error('empty csv header')
  }

  const headers = parseCsvLine(headerRow).map((item) => resolveImportHeader(item))
  return rows.slice(1).map((line) => {
    const values = parseCsvLine(line)
    const record: Record<string, string> = {}
    headers.forEach((header, index) => {
      if (header) {
        record[header] = values[index] ?? ''
      }
    })
    return normalizeBatchImportRecord(record)
  })
}

export function normalizeBatchImportRecord(record: Record<string, unknown>): MarketEventCreateForm {
  const normalized: Record<string, string> = {}
  Object.entries(record).forEach(([key, value]) => {
    const resolvedKey = resolveImportHeader(key)
    if (resolvedKey) {
      normalized[resolvedKey] = String(value ?? '').trim()
    }
  })
  return {
    targetType: normalized.targetType || 'STOCK',
    targetCode: normalized.targetCode || '',
    targetName: normalized.targetName || '',
    eventType: normalized.eventType || '',
    eventTitle: normalized.eventTitle || '',
    eventSummary: normalized.eventSummary || '',
    sourceChannel: normalized.sourceChannel || '',
    sourceUrl: normalized.sourceUrl || '',
    impactLevel: normalized.impactLevel || '',
    eventStatus: normalized.eventStatus || 'ACTIVE',
    occurredAt: normalized.occurredAt || ''
  }
}

export function parseCsvLine(line: string) {
  const values: string[] = []
  let current = ''
  let inQuotes = false

  for (let i = 0; i < line.length; i++) {
    const char = line[i]
    const next = line[i + 1]
    if (char === '"') {
      if (inQuotes && next === '"') {
        current += '"'
        i++
      } else {
        inQuotes = !inQuotes
      }
      continue
    }
    if (char === ',' && !inQuotes) {
      values.push(current.trim())
      current = ''
      continue
    }
    current += char
  }

  values.push(current.trim())
  return values
}

export function resolveImportHeader(header: string) {
  const normalized = header.trim().toLowerCase()
  const mapping: Record<string, string> = {
    targettype: 'targetType',
    type: 'targetType',
    '标的类型': 'targetType',
    targetcode: 'targetCode',
    code: 'targetCode',
    symbol: 'targetCode',
    ticker: 'targetCode',
    '标的代码': 'targetCode',
    targetname: 'targetName',
    name: 'targetName',
    '标的名称': 'targetName',
    eventtype: 'eventType',
    event: 'eventType',
    '事件类型': 'eventType',
    eventtitle: 'eventTitle',
    title: 'eventTitle',
    '事件标题': 'eventTitle',
    eventsummary: 'eventSummary',
    summary: 'eventSummary',
    content: 'eventSummary',
    '事件摘要': 'eventSummary',
    sourcechannel: 'sourceChannel',
    source: 'sourceChannel',
    channel: 'sourceChannel',
    '来源渠道': 'sourceChannel',
    sourceurl: 'sourceUrl',
    url: 'sourceUrl',
    link: 'sourceUrl',
    '来源链接': 'sourceUrl',
    impactlevel: 'impactLevel',
    impact: 'impactLevel',
    level: 'impactLevel',
    '影响等级': 'impactLevel',
    eventstatus: 'eventStatus',
    status: 'eventStatus',
    '事件状态': 'eventStatus',
    occurredat: 'occurredAt',
    occurred_at: 'occurredAt',
    datetime: 'occurredAt',
    time: 'occurredAt',
    '发生时间': 'occurredAt'
  }
  return mapping[normalized] || ''
}

export function escapeCsvValue(value: string | number | boolean | null | undefined) {
  const normalized = String(value ?? '')
  if (/[",\n]/.test(normalized)) {
    return `"${normalized.replace(/"/g, '""')}"`
  }
  return normalized
}

export function downloadTextFile(content: string, fileName: string, mimeType: string) {
  const blob = new Blob([content], { type: mimeType })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

export function exportWorkbook(workbook: WorkBook, fileName: string) {
  const content = write(workbook, {
    bookType: 'xlsx',
    type: 'array'
  })
  const blob = new Blob([content], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
  })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

export function createTemplateWorkbook() {
  const worksheet = utils.json_to_sheet(JSON.parse(batchImportExample) as MarketEventCreateForm[])
  const workbook = utils.book_new()
  utils.book_append_sheet(workbook, worksheet, 'market_events')
  return workbook
}
