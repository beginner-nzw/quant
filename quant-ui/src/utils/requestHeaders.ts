import type { CurrentUser } from './auth'

export const REQUEST_HEADER_USER_ID = 'X-User-Id'
export const REQUEST_HEADER_USER_ROLE = 'X-User-Role'
export const REQUEST_HEADER_TRACE_ID = 'X-Trace-Id'

export function createTraceId() {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`
}

export function buildUserHeaders(user: CurrentUser) {
  return {
    [REQUEST_HEADER_USER_ID]: user.userId,
    [REQUEST_HEADER_USER_ROLE]: user.userRole
  }
}

export function buildTraceHeaders(traceId = createTraceId()) {
  return {
    [REQUEST_HEADER_TRACE_ID]: traceId
  }
}

export function buildRequestHeaders(user: CurrentUser, traceId = createTraceId()) {
  return {
    ...buildUserHeaders(user),
    ...buildTraceHeaders(traceId)
  }
}
