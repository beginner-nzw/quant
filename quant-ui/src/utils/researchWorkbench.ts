interface ResearchWorkbenchQueryOptions {
  targetCode?: string | null
  targetName?: string | null
  from?: string | null
}

export function buildResearchWorkbenchQuery(options: ResearchWorkbenchQueryOptions) {
  return {
    targetCode: normalizeValue(options.targetCode),
    targetName: normalizeValue(options.targetName),
    from: normalizeValue(options.from)
  }
}

function normalizeValue(value?: string | null) {
  const normalized = value?.trim()
  return normalized || undefined
}
