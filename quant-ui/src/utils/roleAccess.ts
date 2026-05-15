import type { RoleAccessConfigItem } from '../types/task'
import { fetchRoleAccessConfigs } from '../api/task'
import { getCurrentUser, USER_ROLE } from './auth'

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

const DEFAULT_ROLE_ACCESS_CONFIGS: RoleAccessConfigItem[] = [
  {
    roleCode: USER_ROLE.RESEARCHER,
    roleName: '研究员',
    roleDescription: '负责创建投研任务、查看市场事件、阅读报告和使用投研工作台。',
    menuKeys: [
      MENU_KEY.TASK_LIST,
      MENU_KEY.TASK_CREATE,
      MENU_KEY.MARKET_EVENTS,
      MENU_KEY.MARKET_INTELLIGENCE,
      MENU_KEY.RESEARCH_WORKBENCH,
      MENU_KEY.STRATEGY_SIGNALS,
      MENU_KEY.RISK_WARNINGS,
      MENU_KEY.RESEARCH_REPORTS
    ],
    permissionKeys: [
      PERMISSION_KEY.TASK_VIEW,
      PERMISSION_KEY.TASK_CREATE
    ],
    remark: '默认业务角色，不具备审核、重试取消或平台配置权限。'
  },
  {
    roleCode: USER_ROLE.PM,
    roleName: '投资经理',
    roleDescription: '负责查看投研结果、跟踪市场事件与情报，并发起研究任务。',
    menuKeys: [
      MENU_KEY.TASK_LIST,
      MENU_KEY.TASK_CREATE,
      MENU_KEY.MARKET_EVENTS,
      MENU_KEY.MARKET_INTELLIGENCE,
      MENU_KEY.RESEARCH_WORKBENCH,
      MENU_KEY.STRATEGY_SIGNALS,
      MENU_KEY.RISK_WARNINGS,
      MENU_KEY.RESEARCH_REPORTS
    ],
    permissionKeys: [
      PERMISSION_KEY.TASK_VIEW,
      PERMISSION_KEY.TASK_CREATE
    ],
    remark: '业务管理角色，不参与审核、重试取消与平台配置。'
  },
  {
    roleCode: USER_ROLE.RISK_MANAGER,
    roleName: '风控人员',
    roleDescription: '负责风险预警、市场事件、审计与合规结果查看。',
    menuKeys: [
      MENU_KEY.TASK_LIST,
      MENU_KEY.TASK_CREATE,
      MENU_KEY.MARKET_EVENTS,
      MENU_KEY.MARKET_INTELLIGENCE,
      MENU_KEY.RESEARCH_WORKBENCH,
      MENU_KEY.STRATEGY_SIGNALS,
      MENU_KEY.RISK_WARNINGS,
      MENU_KEY.RESEARCH_REPORTS,
      MENU_KEY.AUDIT_COMPLIANCE
    ],
    permissionKeys: [
      PERMISSION_KEY.TASK_VIEW,
      PERMISSION_KEY.TASK_CREATE,
      PERMISSION_KEY.AUDIT_COMPLIANCE_VIEW
    ],
    remark: '可查看审计与合规中心，不具备报告审核、任务重试取消与平台配置权限。'
  },
  {
    roleCode: USER_ROLE.COMPLIANCE_AUDITOR,
    roleName: '合规审核',
    roleDescription: '负责报告审核、审计合规查看和配置中心只读访问。',
    menuKeys: [
      MENU_KEY.TASK_LIST,
      MENU_KEY.TASK_CREATE,
      MENU_KEY.MARKET_EVENTS,
      MENU_KEY.MARKET_INTELLIGENCE,
      MENU_KEY.RESEARCH_WORKBENCH,
      MENU_KEY.STRATEGY_SIGNALS,
      MENU_KEY.RISK_WARNINGS,
      MENU_KEY.RESEARCH_REPORTS,
      MENU_KEY.AUDIT_COMPLIANCE,
      MENU_KEY.MODEL_AGENT_CONFIG,
      MENU_KEY.REPORTS_PENDING,
      MENU_KEY.REPORTS_APPROVED,
      MENU_KEY.REPORTS_REJECTED
    ],
    permissionKeys: [
      PERMISSION_KEY.TASK_VIEW,
      PERMISSION_KEY.TASK_CREATE,
      PERMISSION_KEY.AUDIT_COMPLIANCE_VIEW,
      PERMISSION_KEY.REPORT_REVIEW,
      PERMISSION_KEY.MODEL_AGENT_CONFIG_VIEW
    ],
    remark: '审核角色，可读配置中心，但不能修改配置，也不能执行任务重试取消。'
  },
  {
    roleCode: USER_ROLE.ADMIN,
    roleName: '平台管理员',
    roleDescription: '负责全平台配置、审核流程和运行时治理。',
    menuKeys: [
      MENU_KEY.TASK_LIST,
      MENU_KEY.TASK_CREATE,
      MENU_KEY.MARKET_EVENTS,
      MENU_KEY.MARKET_INTELLIGENCE,
      MENU_KEY.RESEARCH_WORKBENCH,
      MENU_KEY.STRATEGY_SIGNALS,
      MENU_KEY.RISK_WARNINGS,
      MENU_KEY.RESEARCH_REPORTS,
      MENU_KEY.AUDIT_COMPLIANCE,
      MENU_KEY.MODEL_AGENT_CONFIG,
      MENU_KEY.REPORTS_PENDING,
      MENU_KEY.REPORTS_APPROVED,
      MENU_KEY.REPORTS_REJECTED
    ],
    permissionKeys: [
      PERMISSION_KEY.TASK_VIEW,
      PERMISSION_KEY.TASK_CREATE,
      PERMISSION_KEY.TASK_RETRY,
      PERMISSION_KEY.TASK_CANCEL,
      PERMISSION_KEY.AUDIT_COMPLIANCE_VIEW,
      PERMISSION_KEY.REPORT_REVIEW,
      PERMISSION_KEY.MODEL_AGENT_CONFIG_VIEW,
      PERMISSION_KEY.MODEL_AGENT_CONFIG_EDIT
    ],
    remark: '具备全部菜单访问、配置修改以及任务重试取消权限。'
  }
]

function normalizeRoleAccessConfigs(configs?: RoleAccessConfigItem[] | null) {
  if (!configs?.length || hasCorruptRoleText(configs)) {
    return DEFAULT_ROLE_ACCESS_CONFIGS
  }

  return configs.map((item) => ({
    roleCode: item.roleCode,
    roleName: item.roleName,
    roleDescription: item.roleDescription,
    menuKeys: item.menuKeys || [],
    permissionKeys: item.permissionKeys || [],
    remark: item.remark
  }))
}

function hasCorruptRoleText(configs: RoleAccessConfigItem[]) {
  return configs.some((item) => {
    const text = `${item.roleName || ''}${item.roleDescription || ''}${item.remark || ''}`
    return /[�]|鐮|鎶|椋|鍚|骞|浠|涓/.test(text)
  })
}

export function getRoleAccessConfigs() {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (raw) {
    try {
      return normalizeRoleAccessConfigs(JSON.parse(raw))
    } catch {
      // ignore invalid cache
    }
  }
  return DEFAULT_ROLE_ACCESS_CONFIGS
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
  return getRoleAccessConfigs()
}

export async function ensureRoleAccessConfigsLoaded() {
  return getRoleAccessConfigs()
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
