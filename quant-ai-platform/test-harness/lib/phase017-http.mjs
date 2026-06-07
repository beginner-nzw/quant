import { setTimeout as sleep } from 'node:timers/promises'

export const defaultHeaders = {
  'Content-Type': 'application/json',
  'X-User-Id': process.env.PHASE017_USER_ID || 'admin',
  'X-User-Role': process.env.PHASE017_USER_ROLE || 'ADMIN',
  'X-Trace-Id': `phase017-${Date.now()}`
}

export function normalizeBaseUrl(value) {
  return (value || process.env.PHASE017_BASE_URL || 'http://127.0.0.1:18080').replace(/\/+$/, '')
}

export async function requestJson(path, options = {}) {
  const baseUrl = normalizeBaseUrl(options.baseUrl)
  const method = options.method || 'GET'
  const url = path.startsWith('http') ? path : `${baseUrl}${path}`
  const started = Date.now()
  let response
  let bodyText = ''
  try {
    response = await fetch(url, {
      method,
      headers: {
        ...defaultHeaders,
        ...(options.headers || {})
      },
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
      signal: options.signal
    })
    bodyText = await response.text()
  } catch (error) {
    throw new Error(`${method} ${url} failed before response: ${error.message}`)
  }

  let body = null
  if (bodyText) {
    try {
      body = JSON.parse(bodyText)
    } catch {
      body = bodyText
    }
  }

  const durationMs = Date.now() - started
  if (!response.ok) {
    throw new Error(`${method} ${url} returned HTTP ${response.status} in ${durationMs}ms: ${bodyText.slice(0, 300)}`)
  }
  if (body && typeof body === 'object' && body.success === false) {
    throw new Error(`${method} ${url} returned unsuccessful envelope in ${durationMs}ms: ${JSON.stringify(body).slice(0, 400)}`)
  }
  return {
    status: response.status,
    durationMs,
    body,
    data: body && typeof body === 'object' && 'data' in body ? body.data : body
  }
}

export async function waitFor(path, predicate, options = {}) {
  const timeoutMs = Number(options.timeoutMs || process.env.PHASE017_POLL_TIMEOUT_MS || 180000)
  const intervalMs = Number(options.intervalMs || process.env.PHASE017_POLL_INTERVAL_MS || 3000)
  const deadline = Date.now() + timeoutMs
  let last
  let lastError

  while (Date.now() < deadline) {
    try {
      last = await requestJson(path, options)
      if (predicate(last.data, last.body)) {
        return last
      }
    } catch (error) {
      lastError = error
    }
    await sleep(intervalMs)
  }

  const reason = lastError ? lastError.message : JSON.stringify(last?.data ?? last?.body ?? null).slice(0, 500)
  throw new Error(`Timed out waiting for ${path}: ${reason}`)
}

export function requireArray(value, label) {
  if (!Array.isArray(value)) {
    throw new Error(`${label} expected an array but received ${typeof value}`)
  }
  return value
}

export function pageItems(pageData) {
  if (!pageData || typeof pageData !== 'object') {
    return []
  }
  return pageData.records || pageData.items || pageData.list || pageData.rows || []
}

export async function findInPages(pathBase, predicate, options = {}) {
  const pageSize = Number(options.pageSize || 50)
  const maxPages = Number(options.maxPages || 5)
  const separator = pathBase.includes('?') ? '&' : '?'

  for (let pageNo = 1; pageNo <= maxPages; pageNo += 1) {
    const result = await requestJson(`${pathBase}${separator}pageNo=${pageNo}&pageSize=${pageSize}`, options)
    const items = pageItems(result.data)
    const found = items.find(predicate)
    if (found) {
      return {
        item: found,
        pageNo,
        pageSize,
        total: result.data?.total ?? result.data?.totalCount ?? null
      }
    }
    if (items.length < pageSize) {
      break
    }
  }

  return null
}

export function requireFound(found, label, taskId) {
  if (!found) {
    throw new Error(`${label} did not contain taskId ${taskId} in scanned pages`)
  }
  return found
}

export function percentile(values, pct) {
  if (!values.length) {
    return 0
  }
  const sorted = [...values].sort((a, b) => a - b)
  const index = Math.min(sorted.length - 1, Math.ceil((pct / 100) * sorted.length) - 1)
  return sorted[index]
}

export function summarizeTaskFull(detail) {
  if (!detail || typeof detail !== 'object') {
    return detail
  }
  const task = detail.task || detail.taskDetail || {}
  return {
    taskId: task.taskId || detail.taskId || detail.id,
    status: task.taskStatus || task.status || detail.taskStatus || detail.status,
    currentStage: task.currentStage || task.stage || detail.currentStage,
    checkpoint: detail.checkpoint
      ? {
          status: detail.checkpoint.status,
          currentNode: detail.checkpoint.currentNode,
          reason: detail.checkpoint.reason
        }
      : null,
    workflow: detail.workflow
      ? {
          status: detail.workflow.status,
          currentNode: detail.workflow.currentNode,
          progress: detail.workflow.progress
        }
      : null
  }
}
