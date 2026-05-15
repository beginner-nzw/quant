export function getRouteQueryValue(value: unknown) {
  if (Array.isArray(value)) {
    return value[0] ? String(value[0]) : ''
  }
  return value ? String(value) : ''
}

export function resolveSourcePath(from: unknown, excludedPaths: string[] = ['/tasks']) {
  const normalized = normalizePath(getRouteQueryValue(from))
  if (!normalized) {
    return ''
  }
  return excludedPaths.includes(normalized) ? '' : normalized
}

export function buildFromQuery(from?: string | null) {
  const normalized = normalizePath(from)
  return normalized ? { from: normalized } : undefined
}

function normalizePath(value?: string | null) {
  const normalized = value?.trim()
  return normalized || ''
}
