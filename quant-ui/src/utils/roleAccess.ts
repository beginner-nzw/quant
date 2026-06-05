import type { RoleAccessConfigItem } from '../types/task'
import { fetchRoleAccessConfigs } from '../api/task'
import { getCurrentUser } from './auth'

export const MENU_KEY = {
  TASK_LIST: 'TASK_LIST',
  TASK_CREATE: 'TASK_CREATE',
  MARKET_EVENTS: 'MARKET_EVENTS',
  MARKET_INTELLIGENCE: 'MARKET_INTELLIGENCE',
  RESEARCH_WORKBENCH: 'RESEARCH_WORKBENCH',
  STRATEGY_SIGNALS: 'STRATEGY_SIGNALS',
  RISK_WARNINGS: 'RISK_WARNINGS',
  RESEARCH_REPORTS: 'RESEARCH_REPORTS',
  AUDIT_COMPLIANCE: 'AUDIT_COMPLIANCE',
  MODEL_AGENT_CONFIG: 'MODEL_AGENT_CONFIG',
  REPORTS_PENDING: 'REPORTS_PENDING',
  REPORTS_APPROVED: 'REPORTS_APPROVED',
  REPORTS_REJECTED: 'REPORTS_REJECTED'
} as const

export const PERMISSION_KEY = {
  TASK_VIEW: 'TASK_VIEW',
  TASK_CREATE: 'TASK_CREATE',
  TASK_RETRY: 'TASK_RETRY',
  TASK_CANCEL: 'TASK_CANCEL',
  AUDIT_COMPLIANCE_VIEW: 'AUDIT_COMPLIANCE_VIEW',
  REPORT_REVIEW: 'REPORT_REVIEW',
  MODEL_AGENT_CONFIG_VIEW: 'MODEL_AGENT_CONFIG_VIEW',
  MODEL_AGENT_CONFIG_EDIT: 'MODEL_AGENT_CONFIG_EDIT'
} as const

const STORAGE_KEY = 'quant_role_access_configs'
export const ROLE_ACCESS_UPDATED_EVENT = 'quant-role-access-updated'

function normalizeRoleAccessConfigs(configs?: RoleAccessConfigItem[] | null) {
  if (!configs?.length) return []

  return configs.map((item) => ({
    roleCode: item.roleCode,
    roleName: item.roleName,
    roleDescription: item.roleDescription,
    menuKeys: item.menuKeys || [],
    permissionKeys: item.permissionKeys || [],
    remark: item.remark
  }))
}

export function getRoleAccessConfigs() {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) return []

  try {
    return normalizeRoleAccessConfigs(JSON.parse(raw))
  } catch {
    return []
  }
}

export function setRoleAccessConfigs(configs: RoleAccessConfigItem[]) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(normalizeRoleAccessConfigs(configs)))
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent(ROLE_ACCESS_UPDATED_EVENT))
  }
}

export async function refreshRoleAccessConfigs() {
  const res = await fetchRoleAccessConfigs()
  if (res.success && res.data?.length) {
    setRoleAccessConfigs(res.data)
    return normalizeRoleAccessConfigs(res.data)
  }
  return []
}

export async function ensureRoleAccessConfigsLoaded() {
  const cached = getRoleAccessConfigs()
  if (cached.length) return cached
  return refreshRoleAccessConfigs()
}

export function getRoleAccessConfig(userRole = getCurrentUser().userRole) {
  return getRoleAccessConfigs().find((item) => item.roleCode === userRole)
}

export function getConfiguredRoleLabel(userRole = getCurrentUser().userRole) {
  return getRoleAccessConfig(userRole)?.roleName || userRole
}

export function getConfiguredRoleDescription(userRole = getCurrentUser().userRole) {
  return getRoleAccessConfig(userRole)?.roleDescription || ''
}

export function hasMenuAccess(menuKey: string, userRole = getCurrentUser().userRole) {
  return !!getRoleAccessConfig(userRole)?.menuKeys?.includes(menuKey)
}

export function hasPermission(permissionKey: string, userRole = getCurrentUser().userRole) {
  return !!getRoleAccessConfig(userRole)?.permissionKeys?.includes(permissionKey)
}

export function canCreateTasks(userRole = getCurrentUser().userRole) {
  return hasPermission(PERMISSION_KEY.TASK_CREATE, userRole)
}

export function canRetryTasks(userRole = getCurrentUser().userRole) {
  return hasPermission(PERMISSION_KEY.TASK_RETRY, userRole)
}

export function canCancelTasks(userRole = getCurrentUser().userRole) {
  return hasPermission(PERMISSION_KEY.TASK_CANCEL, userRole)
}

export function canAccessAuditCompliance(userRole = getCurrentUser().userRole) {
  return hasPermission(PERMISSION_KEY.AUDIT_COMPLIANCE_VIEW, userRole)
}

export function canReviewReports(userRole = getCurrentUser().userRole) {
  return hasPermission(PERMISSION_KEY.REPORT_REVIEW, userRole)
}

export function canManageModelAgentConfig(userRole = getCurrentUser().userRole) {
  return hasPermission(PERMISSION_KEY.MODEL_AGENT_CONFIG_VIEW, userRole)
}

export function canEditModelAgentConfig(userRole = getCurrentUser().userRole) {
  return hasPermission(PERMISSION_KEY.MODEL_AGENT_CONFIG_EDIT, userRole)
}
