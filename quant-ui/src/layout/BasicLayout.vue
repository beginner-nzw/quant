<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ROLE_OPTIONS,
  getCurrentUser,
  getDefaultUserIdByRole,
  setCurrentUser,
  type UserRole
} from '../utils/auth'
import {
  canAccessAuditCompliance,
  canCreateTasks,
  canManageModelAgentConfig,
  canReviewReports,
  getConfiguredRoleDescription,
  getConfiguredRoleLabel,
  hasMenuAccess,
  MENU_KEY,
  ROLE_ACCESS_UPDATED_EVENT
} from '../utils/roleAccess'

type WorkspaceKey = 'front' | 'governance'

interface ModuleItem {
  key: string
  workspace: WorkspaceKey
  label: string
  subtitle: string
  path: string
  glyph: string
  accent: string
  menuKey?: string
  requireCreate?: boolean
  requireAudit?: boolean
  requireModelConfig?: boolean
  requireReportReview?: boolean
}

const route = useRoute()
const router = useRouter()
const currentUser = ref(getCurrentUser())
const accessVersion = ref(0)

const modules: ModuleItem[] = [
  {
    key: 'dashboard',
    workspace: 'front',
    label: '指挥舱',
    subtitle: '全局任务、风险、信号和报告态势',
    path: '/dashboard',
    glyph: 'Q',
    accent: '#69d2c4'
  },
  {
    key: MENU_KEY.TASK_LIST,
    workspace: 'front',
    label: '任务流',
    subtitle: 'AI 任务、Agent 链路、失败重试',
    path: '/tasks',
    glyph: 'T',
    accent: '#5e8cff',
    menuKey: MENU_KEY.TASK_LIST
  },
  {
    key: MENU_KEY.TASK_CREATE,
    workspace: 'front',
    label: '研究发起',
    subtitle: '深度投研、风险扫描、跟踪研究',
    path: '/tasks/create',
    glyph: 'R',
    accent: '#f2c66d',
    menuKey: MENU_KEY.TASK_CREATE,
    requireCreate: true
  },
  {
    key: MENU_KEY.MARKET_EVENTS,
    workspace: 'front',
    label: '事件雷达',
    subtitle: '新闻、公告、舆情、政策事件接入',
    path: '/market-events',
    glyph: 'E',
    accent: '#ff8764',
    menuKey: MENU_KEY.MARKET_EVENTS
  },
  {
    key: MENU_KEY.MARKET_INTELLIGENCE,
    workspace: 'front',
    label: '情报中枢',
    subtitle: '风险、信号、报告洞察聚合',
    path: '/intelligence',
    glyph: 'I',
    accent: '#41b883',
    menuKey: MENU_KEY.MARKET_INTELLIGENCE
  },
  {
    key: MENU_KEY.RESEARCH_WORKBENCH,
    workspace: 'front',
    label: '标的工作台',
    subtitle: '围绕单一标的做持续研究',
    path: '/research-workbench',
    glyph: 'W',
    accent: '#2fb6d6',
    menuKey: MENU_KEY.RESEARCH_WORKBENCH
  },
  {
    key: MENU_KEY.STRATEGY_SIGNALS,
    workspace: 'front',
    label: '策略信号',
    subtitle: '信号方向、强度、证据和回测',
    path: '/signals',
    glyph: 'S',
    accent: '#6bcf8f',
    menuKey: MENU_KEY.STRATEGY_SIGNALS
  },
  {
    key: MENU_KEY.RISK_WARNINGS,
    workspace: 'front',
    label: '风险处置',
    subtitle: '预警等级、复核、后续跟踪',
    path: '/risk-warnings',
    glyph: 'X',
    accent: '#ff6b6b',
    menuKey: MENU_KEY.RISK_WARNINGS
  },
  {
    key: MENU_KEY.RESEARCH_REPORTS,
    workspace: 'front',
    label: '报告库',
    subtitle: '研究报告、证据引用、归档阅读',
    path: '/reports/center',
    glyph: 'P',
    accent: '#b58cff',
    menuKey: MENU_KEY.RESEARCH_REPORTS
  },
  {
    key: MENU_KEY.AUDIT_COMPLIANCE,
    workspace: 'governance',
    label: '审计链路',
    subtitle: 'Prompt、响应、证据、人工复核留痕',
    path: '/audit-compliance',
    glyph: 'A',
    accent: '#f2c66d',
    menuKey: MENU_KEY.AUDIT_COMPLIANCE,
    requireAudit: true
  },
  {
    key: MENU_KEY.MODEL_AGENT_CONFIG,
    workspace: 'governance',
    label: 'Agent 控制台',
    subtitle: '工作流、模型策略、Prompt、工具白名单',
    path: '/model-agent-config',
    glyph: 'G',
    accent: '#69d2c4',
    menuKey: MENU_KEY.MODEL_AGENT_CONFIG,
    requireModelConfig: true
  },
  {
    key: MENU_KEY.REPORTS_PENDING,
    workspace: 'governance',
    label: '待审队列',
    subtitle: '待审核报告与修订入口',
    path: '/reports/pending',
    glyph: 'D',
    accent: '#ffb020',
    menuKey: MENU_KEY.REPORTS_PENDING,
    requireReportReview: true
  },
  {
    key: MENU_KEY.REPORTS_APPROVED,
    workspace: 'governance',
    label: '通过档案',
    subtitle: '已批准研究结论归档',
    path: '/reports/approved',
    glyph: 'Y',
    accent: '#41b883',
    menuKey: MENU_KEY.REPORTS_APPROVED,
    requireReportReview: true
  },
  {
    key: MENU_KEY.REPORTS_REJECTED,
    workspace: 'governance',
    label: '驳回复盘',
    subtitle: '证据不足、合规风险和修订原因',
    path: '/reports/rejected',
    glyph: 'B',
    accent: '#ff6b6b',
    menuKey: MENU_KEY.REPORTS_REJECTED,
    requireReportReview: true
  }
]

const currentRoleLabel = computed(() => {
  void accessVersion.value
  return getConfiguredRoleLabel(currentUser.value.userRole)
})

const currentRoleDescription = computed(() => {
  void accessVersion.value
  return getConfiguredRoleDescription(currentUser.value.userRole)
})

const visibleModules = computed(() => modules.filter(canShowModule))
const frontModules = computed(() => visibleModules.value.filter((item) => item.workspace === 'front'))
const governanceModules = computed(() => visibleModules.value.filter((item) => item.workspace === 'governance'))
const hasGovernance = computed(() => governanceModules.value.length > 0)

const activeWorkspace = computed<WorkspaceKey>(() => {
  if (governanceModules.value.some(isActiveModule)) {
    return 'governance'
  }
  return 'front'
})

const activeModules = computed(() => {
  return activeWorkspace.value === 'front' ? frontModules.value : governanceModules.value
})

const activeModule = computed(() => {
  return visibleModules.value.find(isActiveModule) || frontModules.value[0]
})

const siblingModules = computed(() => {
  return activeModules.value.filter((item) => item.path !== activeModule.value?.path).slice(0, 4)
})

function canShowModule(item: ModuleItem) {
  void accessVersion.value
  const role = currentUser.value.userRole
  if (item.menuKey && !hasMenuAccess(item.menuKey, role)) {
    return false
  }
  if (item.requireCreate && !canCreateTasks(role)) {
    return false
  }
  if (item.requireAudit && !canAccessAuditCompliance(role)) {
    return false
  }
  if (item.requireModelConfig && !canManageModelAgentConfig(role)) {
    return false
  }
  if (item.requireReportReview && !canReviewReports(role)) {
    return false
  }
  return true
}

function isActiveModule(item: ModuleItem) {
  if (item.path === '/dashboard') {
    return route.path === '/dashboard'
  }
  if (item.path === '/tasks') {
    return route.path === '/tasks'
      || (route.path.startsWith('/tasks/') && route.path !== '/tasks/create')
  }
  return route.path === item.path
}

function goPath(path: string) {
  if (route.path !== path) {
    router.push(path)
  }
}

function switchWorkspace(workspace: WorkspaceKey) {
  const target = workspace === 'front' ? frontModules.value[0] : governanceModules.value[0]
  if (target) {
    goPath(target.path)
  }
}

function changeRole(role: UserRole) {
  currentUser.value = {
    userId: getDefaultUserIdByRole(role),
    userRole: role
  }
  setCurrentUser(currentUser.value)
  window.location.reload()
}

function handleRoleAccessUpdated() {
  accessVersion.value += 1
}

onMounted(() => {
  window.addEventListener(ROLE_ACCESS_UPDATED_EVENT, handleRoleAccessUpdated)
})

onBeforeUnmount(() => {
  window.removeEventListener(ROLE_ACCESS_UPDATED_EVENT, handleRoleAccessUpdated)
})
</script>

<template>
  <div class="research-os">
    <div class="ambient ambient-a" />
    <div class="ambient ambient-b" />

    <header class="global-header">
      <button class="brand" type="button" @click="goPath('/dashboard')">
        <span class="brand-symbol">Q</span>
        <span>
          <strong>Quant AI</strong>
          <small>Research Operating System</small>
        </span>
      </button>

      <div class="workspace-tabs" aria-label="Workspace switch">
        <button
          type="button"
          :class="{ active: activeWorkspace === 'front' }"
          @click="switchWorkspace('front')"
        >
          业务前台
        </button>
        <button
          type="button"
          :class="{ active: activeWorkspace === 'governance', muted: !hasGovernance }"
          :disabled="!hasGovernance"
          @click="switchWorkspace('governance')"
        >
          治理后台
        </button>
      </div>

      <div class="header-actions">
        <button
          v-if="canCreateTasks(currentUser.userRole)"
          type="button"
          class="quick-action"
          @click="goPath('/tasks/create')"
        >
          新建研究
        </button>
        <el-dropdown>
          <button type="button" class="role-button">
            <span>{{ currentRoleLabel }}</span>
            <small>{{ currentUser.userRole }}</small>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item
                v-for="option in ROLE_OPTIONS"
                :key="option.role"
                @click="changeRole(option.role)"
              >
                {{ option.label }} ({{ option.role }})
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <section class="module-board">
      <div class="module-board-copy">
        <span>{{ activeWorkspace === 'front' ? 'Research Front Office' : 'Governance Console' }}</span>
        <h1>{{ activeModule?.label || '工作台' }}</h1>
        <p>{{ activeModule?.subtitle || '金融量化投研与风险预警多智能体平台' }}</p>
      </div>

      <div class="module-rail">
        <button
          v-for="item in activeModules"
          :key="item.key"
          type="button"
          class="module-chip"
          :class="{ active: isActiveModule(item) }"
          :style="{ '--accent': item.accent }"
          @click="goPath(item.path)"
        >
          <span>{{ item.glyph }}</span>
          <strong>{{ item.label }}</strong>
        </button>
      </div>
    </section>

    <div class="workspace-layout">
      <aside class="context-panel">
        <div class="active-orb" :style="{ '--accent': activeModule?.accent || '#69d2c4' }">
          <span>{{ activeModule?.glyph || 'Q' }}</span>
        </div>

        <div class="context-copy">
          <span>当前模块</span>
          <strong>{{ activeModule?.label || '指挥舱' }}</strong>
          <p>{{ activeModule?.subtitle || '任务、风险、信号和报告统一入口。' }}</p>
        </div>

        <div class="pipeline">
          <div>事件接入</div>
          <div>任务拆解</div>
          <div>Agent 执行</div>
          <div>审核交付</div>
        </div>

        <div class="role-brief">
          <span>权限身份</span>
          <strong>{{ currentRoleLabel }}</strong>
          <p>{{ currentRoleDescription || '使用当前角色的默认菜单和能力权限。' }}</p>
        </div>

        <div v-if="siblingModules.length" class="nearby">
          <span>相邻工作区</span>
          <button
            v-for="item in siblingModules"
            :key="item.key"
            type="button"
            @click="goPath(item.path)"
          >
            <strong>{{ item.label }}</strong>
            <small>{{ item.subtitle }}</small>
          </button>
        </div>
      </aside>

      <main class="stage">
        <router-view />
      </main>
    </div>
  </div>
</template>

<style scoped>
.research-os {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  padding: 18px;
  background:
    linear-gradient(135deg, #eff5f8 0%, #e7eef3 38%, #f7efe4 100%);
  color: #142033;
}

.research-os::before {
  content: '';
  position: fixed;
  inset: 0;
  pointer-events: none;
  background-image:
    linear-gradient(rgba(20, 32, 51, 0.045) 1px, transparent 1px),
    linear-gradient(90deg, rgba(20, 32, 51, 0.045) 1px, transparent 1px);
  background-size: 44px 44px;
  mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.75), transparent 72%);
}

.ambient {
  position: fixed;
  pointer-events: none;
  filter: blur(10px);
  opacity: 0.68;
}

.ambient-a {
  top: -140px;
  right: 8vw;
  width: 420px;
  height: 420px;
  border-radius: 999px;
  background: radial-gradient(circle, rgba(105, 210, 196, 0.48), transparent 68%);
}

.ambient-b {
  left: -140px;
  bottom: 8vh;
  width: 360px;
  height: 360px;
  border-radius: 999px;
  background: radial-gradient(circle, rgba(242, 198, 109, 0.38), transparent 68%);
}

.global-header,
.module-board,
.workspace-layout {
  position: relative;
  z-index: 1;
}

.global-header {
  display: grid;
  grid-template-columns: minmax(220px, 0.8fr) auto minmax(260px, 0.8fr);
  align-items: center;
  gap: 18px;
  padding: 12px;
  border: 1px solid rgba(20, 32, 51, 0.09);
  border-radius: 30px;
  background: rgba(255, 255, 255, 0.68);
  box-shadow: 0 22px 70px rgba(32, 52, 80, 0.1);
  backdrop-filter: blur(22px);
}

.brand,
.role-button,
.quick-action,
.workspace-tabs button,
.module-chip,
.nearby button {
  border: 0;
  font: inherit;
  cursor: pointer;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  padding: 8px;
  background: transparent;
  color: inherit;
  text-align: left;
}

.brand-symbol {
  width: 46px;
  height: 46px;
  display: grid;
  place-items: center;
  border-radius: 18px;
  background: #142033;
  color: #69d2c4;
  font-size: 24px;
  font-weight: 950;
  box-shadow: inset 0 -10px 24px rgba(105, 210, 196, 0.16);
}

.brand strong,
.brand small,
.role-button span,
.role-button small {
  display: block;
}

.brand strong {
  font-size: 18px;
  font-weight: 950;
  letter-spacing: -0.04em;
}

.brand small {
  color: #687789;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.workspace-tabs {
  display: flex;
  gap: 6px;
  padding: 6px;
  border-radius: 999px;
  background: rgba(20, 32, 51, 0.06);
}

.workspace-tabs button {
  min-width: 112px;
  padding: 11px 18px;
  border-radius: 999px;
  background: transparent;
  color: #5d6c7e;
  font-weight: 900;
}

.workspace-tabs button.active {
  background: #142033;
  color: #fff;
  box-shadow: 0 14px 26px rgba(20, 32, 51, 0.18);
}

.workspace-tabs button.muted {
  opacity: 0.42;
  cursor: not-allowed;
}

.header-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.quick-action {
  padding: 13px 18px;
  border-radius: 999px;
  background: linear-gradient(135deg, #69d2c4, #f2c66d);
  color: #142033;
  font-weight: 950;
  box-shadow: 0 18px 36px rgba(105, 210, 196, 0.22);
}

.role-button {
  min-width: 156px;
  padding: 10px 14px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.74);
  color: #142033;
  text-align: left;
  box-shadow: inset 0 0 0 1px rgba(20, 32, 51, 0.08);
}

.role-button span {
  font-weight: 950;
}

.role-button small {
  color: #748396;
  font-size: 11px;
}

.module-board {
  display: grid;
  grid-template-columns: minmax(280px, 0.38fr) minmax(0, 1fr);
  gap: 20px;
  margin-top: 18px;
}

.module-board-copy {
  min-height: 170px;
  padding: 24px;
  border-radius: 32px;
  background:
    radial-gradient(circle at 100% 0%, rgba(105, 210, 196, 0.24), transparent 38%),
    #142033;
  color: #fff;
  box-shadow: 0 28px 80px rgba(20, 32, 51, 0.22);
}

.module-board-copy span {
  color: #69d2c4;
  font-size: 12px;
  font-weight: 950;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.module-board-copy h1 {
  margin: 14px 0 0;
  font-size: clamp(36px, 4.2vw, 62px);
  font-weight: 950;
  letter-spacing: -0.08em;
  line-height: 0.96;
}

.module-board-copy p {
  max-width: 520px;
  margin: 14px 0 0;
  color: rgba(255, 255, 255, 0.68);
  line-height: 1.8;
}

.module-rail {
  display: grid;
  grid-template-columns: repeat(4, minmax(130px, 1fr));
  gap: 12px;
}

.module-chip {
  min-height: 78px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.68);
  color: #243047;
  text-align: left;
  box-shadow: inset 0 0 0 1px rgba(20, 32, 51, 0.08);
  transition: transform 0.18s ease, box-shadow 0.18s ease, background 0.18s ease;
}

.module-chip:hover,
.module-chip.active {
  transform: translateY(-2px);
  background: rgba(255, 255, 255, 0.92);
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--accent), transparent 45%), 0 18px 46px rgba(32, 52, 80, 0.14);
}

.module-chip span {
  width: 38px;
  height: 38px;
  flex: 0 0 auto;
  display: grid;
  place-items: center;
  border-radius: 14px;
  background: color-mix(in srgb, var(--accent), white 78%);
  color: color-mix(in srgb, var(--accent), #142033 35%);
  font-weight: 950;
}

.module-chip strong {
  font-size: 14px;
  font-weight: 950;
}

.workspace-layout {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 20px;
  margin-top: 20px;
  align-items: start;
}

.context-panel {
  position: sticky;
  top: 18px;
  display: grid;
  gap: 16px;
  padding: 18px;
  border: 1px solid rgba(20, 32, 51, 0.08);
  border-radius: 32px;
  background: rgba(255, 255, 255, 0.62);
  box-shadow: 0 22px 70px rgba(32, 52, 80, 0.1);
  backdrop-filter: blur(20px);
}

.active-orb {
  position: relative;
  width: 110px;
  height: 110px;
  display: grid;
  place-items: center;
  margin: 2px auto 0;
  border-radius: 999px;
  background:
    radial-gradient(circle, color-mix(in srgb, var(--accent), white 22%), color-mix(in srgb, var(--accent), #142033 68%));
  color: #fff;
  font-size: 42px;
  font-weight: 950;
  box-shadow: 0 22px 50px color-mix(in srgb, var(--accent), transparent 68%);
}

.active-orb::before,
.active-orb::after {
  content: '';
  position: absolute;
  inset: -12px;
  border: 1px solid color-mix(in srgb, var(--accent), transparent 45%);
  border-radius: inherit;
}

.active-orb::after {
  inset: -26px;
  opacity: 0.42;
}

.context-copy,
.role-brief {
  padding: 14px;
  border-radius: 22px;
  background: rgba(20, 32, 51, 0.045);
}

.context-copy span,
.role-brief span,
.nearby > span {
  display: block;
  color: #687789;
  font-size: 12px;
  font-weight: 900;
}

.context-copy strong,
.role-brief strong {
  display: block;
  margin-top: 4px;
  color: #142033;
  font-size: 18px;
  font-weight: 950;
}

.context-copy p,
.role-brief p {
  margin: 8px 0 0;
  color: #667589;
  font-size: 13px;
  line-height: 1.7;
}

.pipeline {
  display: grid;
  gap: 8px;
  counter-reset: step;
}

.pipeline div {
  position: relative;
  padding: 12px 12px 12px 42px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.68);
  color: #344256;
  font-size: 13px;
  font-weight: 900;
  box-shadow: inset 0 0 0 1px rgba(20, 32, 51, 0.06);
}

.pipeline div::before {
  counter-increment: step;
  content: counter(step);
  position: absolute;
  top: 50%;
  left: 12px;
  width: 22px;
  height: 22px;
  display: grid;
  place-items: center;
  transform: translateY(-50%);
  border-radius: 999px;
  background: #142033;
  color: #fff;
  font-size: 11px;
}

.nearby {
  display: grid;
  gap: 8px;
}

.nearby button {
  padding: 12px;
  border-radius: 18px;
  background: transparent;
  color: #243047;
  text-align: left;
  box-shadow: inset 0 0 0 1px rgba(20, 32, 51, 0.08);
}

.nearby button:hover {
  background: rgba(255, 255, 255, 0.7);
}

.nearby strong,
.nearby small {
  display: block;
}

.nearby strong {
  font-size: 13px;
  font-weight: 950;
}

.nearby small {
  margin-top: 4px;
  overflow: hidden;
  color: #748396;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stage {
  min-width: 0;
  padding: 22px;
  border: 1px solid rgba(20, 32, 51, 0.08);
  border-radius: 34px;
  background: rgba(248, 251, 253, 0.72);
  box-shadow: 0 22px 70px rgba(32, 52, 80, 0.1);
  backdrop-filter: blur(22px);
}

@media (max-width: 1280px) {
  .global-header,
  .module-board,
  .workspace-layout {
    grid-template-columns: 1fr;
  }

  .context-panel {
    position: relative;
    top: auto;
    grid-template-columns: 160px minmax(0, 1fr);
    align-items: center;
  }

  .pipeline,
  .nearby,
  .role-brief {
    grid-column: 1 / -1;
  }
}

@media (max-width: 820px) {
  .research-os {
    padding: 10px;
  }

  .global-header {
    justify-items: stretch;
  }

  .header-actions,
  .workspace-tabs {
    justify-content: stretch;
  }

  .workspace-tabs button,
  .quick-action,
  .role-button {
    width: 100%;
  }

  .module-rail,
  .context-panel {
    grid-template-columns: 1fr;
  }

  .stage {
    padding: 14px;
    border-radius: 24px;
  }
}
</style>
