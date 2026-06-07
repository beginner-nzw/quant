export const USER_ROLE = {
  RESEARCHER: 'RESEARCHER',
  PM: 'PM',
  RISK_MANAGER: 'RISK_MANAGER',
  COMPLIANCE_AUDITOR: 'COMPLIANCE_AUDITOR',
  ADMIN: 'ADMIN'
} as const

export type UserRole = typeof USER_ROLE[keyof typeof USER_ROLE]
export type AccessRole = 'USER' | 'REVIEWER' | 'ADMIN'

export interface CurrentUser {
  userId: string
  userRole: UserRole
}

const STORAGE_KEY = 'quant_current_user'
const SESSION_STORAGE_KEY = 'quant_session'

const ROLE_LABELS: Record<UserRole, string> = {
  [USER_ROLE.RESEARCHER]: '研究员',
  [USER_ROLE.PM]: '投资经理',
  [USER_ROLE.RISK_MANAGER]: '风控人员',
  [USER_ROLE.COMPLIANCE_AUDITOR]: '合规审核',
  [USER_ROLE.ADMIN]: '平台管理员'
}

export const AUDIT_COMPLIANCE_ROLES = [
  USER_ROLE.RISK_MANAGER,
  USER_ROLE.COMPLIANCE_AUDITOR,
  USER_ROLE.ADMIN
] as const

export const REPORT_REVIEW_ROLES = [
  USER_ROLE.COMPLIANCE_AUDITOR,
  USER_ROLE.ADMIN
] as const

export const MODEL_AGENT_CONFIG_ROLES = [
  USER_ROLE.COMPLIANCE_AUDITOR,
  USER_ROLE.ADMIN
] as const

export const ROLE_OPTIONS = [
  { role: USER_ROLE.RESEARCHER, label: ROLE_LABELS[USER_ROLE.RESEARCHER] },
  { role: USER_ROLE.PM, label: ROLE_LABELS[USER_ROLE.PM] },
  { role: USER_ROLE.RISK_MANAGER, label: ROLE_LABELS[USER_ROLE.RISK_MANAGER] },
  { role: USER_ROLE.COMPLIANCE_AUDITOR, label: ROLE_LABELS[USER_ROLE.COMPLIANCE_AUDITOR] },
  { role: USER_ROLE.ADMIN, label: ROLE_LABELS[USER_ROLE.ADMIN] }
] as const

const DEFAULT_USER_ID_BY_ROLE: Record<UserRole, string> = {
  [USER_ROLE.RESEARCHER]: 'researcher',
  [USER_ROLE.PM]: 'pm',
  [USER_ROLE.RISK_MANAGER]: 'risk_manager',
  [USER_ROLE.COMPLIANCE_AUDITOR]: 'compliance_auditor',
  [USER_ROLE.ADMIN]: 'admin'
}

export function getDefaultUserIdByRole(role: UserRole) {
  return DEFAULT_USER_ID_BY_ROLE[role]
}

export function getRoleLabel(role: UserRole) {
  return ROLE_LABELS[role]
}

export function normalizeCurrentUser(user?: Partial<CurrentUser> | null): CurrentUser {
  const userRole = resolveUserRole(user?.userRole)

  return {
    userId: user?.userId || getDefaultUserIdByRole(userRole),
    userRole
  }
}

export function getCurrentUser(): CurrentUser {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (raw) {
    try {
      return normalizeCurrentUser(JSON.parse(raw))
    } catch {
      // ignore invalid cache
    }
  }
  return normalizeCurrentUser()
}

export function setCurrentUser(user: CurrentUser) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(normalizeCurrentUser(user)))
}

export interface AuthSession {
  accessToken?: string
  tokenType?: string
  expiresAt?: string
  issuer?: string
  subject?: string
  approvedSso?: boolean
}

export function getAuthSession(): AuthSession | null {
  const raw = localStorage.getItem(SESSION_STORAGE_KEY)
  if (!raw) return null

  try {
    return normalizeAuthSession(JSON.parse(raw))
  } catch {
    return null
  }
}

export function setAuthSession(session: AuthSession | null) {
  if (!session?.accessToken) {
    localStorage.removeItem(SESSION_STORAGE_KEY)
    return
  }
  localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(normalizeAuthSession(session)))
}

export function clearAuthSession() {
  localStorage.removeItem(SESSION_STORAGE_KEY)
}

export function hasActiveAuthSession(session = getAuthSession()) {
  if (!session?.accessToken) return false
  if (!session.expiresAt) return true
  const expiresAt = Date.parse(session.expiresAt)
  return Number.isNaN(expiresAt) || expiresAt > Date.now()
}

export function getSessionMode(session = getAuthSession()) {
  if (!session?.accessToken) return 'DEMO_HEADER'
  return session.approvedSso ? 'APPROVED_SSO' : 'JWT'
}

function normalizeAuthSession(session?: Partial<AuthSession> | null): AuthSession {
  return {
    accessToken: session?.accessToken || '',
    tokenType: session?.tokenType || 'Bearer',
    expiresAt: session?.expiresAt,
    issuer: session?.issuer,
    subject: session?.subject,
    approvedSso: Boolean(session?.approvedSso)
  }
}

export function getAccessRole(userRole: UserRole): AccessRole {
  switch (userRole) {
    case USER_ROLE.COMPLIANCE_AUDITOR:
      return 'REVIEWER'
    case USER_ROLE.ADMIN:
      return 'ADMIN'
    default:
      return 'USER'
  }
}

export function hasAnyRole(roles: readonly UserRole[], userRole = getCurrentUser().userRole) {
  return roles.includes(userRole)
}

export function isAdmin(userRole = getCurrentUser().userRole) {
  return userRole === USER_ROLE.ADMIN
}

function resolveUserRole(value?: string | null): UserRole {
  switch (value) {
    case USER_ROLE.RESEARCHER:
    case USER_ROLE.PM:
    case USER_ROLE.RISK_MANAGER:
    case USER_ROLE.COMPLIANCE_AUDITOR:
    case USER_ROLE.ADMIN:
      return value
    case 'USER':
      return USER_ROLE.RESEARCHER
    case 'REVIEWER':
      return USER_ROLE.COMPLIANCE_AUDITOR
    case 'ADMIN':
      return USER_ROLE.ADMIN
    default:
      return USER_ROLE.RESEARCHER
  }
}
