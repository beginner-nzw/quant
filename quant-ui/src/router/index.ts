import { createRouter, createWebHistory } from 'vue-router'
import BasicLayout from '../layout/BasicLayout.vue'
import {
  getCurrentUser
} from '../utils/auth'
import {
  MENU_KEY,
  PERMISSION_KEY,
  ensureRoleAccessConfigsLoaded,
  hasMenuAccess,
  hasPermission
} from '../utils/roleAccess'

const TaskListView = () => import('../views/task/TaskListView.vue')
const TaskDetailView = () => import('../views/task/TaskDetailView.vue')
const TaskCreateView = () => import('../views/task/TaskCreateView.vue')
const TaskReportView = () => import('../views/task/TaskReportView.vue')
const DashboardView = () => import('../views/DashboardView.vue')
const MarketEventCenterView = () => import('../views/report/MarketEventCenterView.vue')
const MarketIntelligenceCenterView = () => import('../views/report/MarketIntelligenceCenterView.vue')
const ResearchWorkbenchView = () => import('../views/report/ResearchWorkbenchView.vue')
const StrategySignalCenterView = () => import('../views/report/StrategySignalCenterView.vue')
const RiskWarningCenterView = () => import('../views/report/RiskWarningCenterView.vue')
const ResearchReportCenterView = () => import('../views/report/ResearchReportCenterView.vue')
const AuditComplianceCenterView = () => import('../views/report/AuditComplianceCenterView.vue')
const ModelAgentConfigCenterView = () => import('../views/report/ModelAgentConfigCenterView.vue')
const PendingReportWorkbenchView = () => import('../views/report/PendingReportWorkbenchView.vue')
const ApprovedReportWorkbenchView = () => import('../views/report/ApprovedReportWorkbenchView.vue')
const RejectedReportWorkbenchView = () => import('../views/report/RejectedReportWorkbenchView.vue')

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: BasicLayout,
      children: [
        {
          path: '',
          redirect: '/dashboard'
        },
        {
          path: '/dashboard',
          name: 'Dashboard',
          component: DashboardView
        },
        {
          path: '/tasks',
          name: 'TaskList',
          component: TaskListView
        },
        {
          path: '/tasks/:taskId',
          name: 'TaskDetail',
          component: TaskDetailView
        },
        {
          path: '/tasks/create',
          name: 'TaskCreate',
          meta: {
            requiredMenuKey: MENU_KEY.TASK_CREATE,
            requiredPermissionKey: PERMISSION_KEY.TASK_CREATE
          },
          component: TaskCreateView
        },
        {
          path: '/tasks/:taskId/report',
          name: 'TaskReport',
          component: TaskReportView
        },
        {
          path: '/market-events',
          name: 'MarketEventCenter',
          meta: {
            requiredMenuKey: MENU_KEY.MARKET_EVENTS
          },
          component: MarketEventCenterView
        },
        {
          path: '/intelligence',
          name: 'MarketIntelligenceCenter',
          component: MarketIntelligenceCenterView
        },
        {
          path: '/research-workbench',
          name: 'ResearchWorkbench',
          component: ResearchWorkbenchView
        },
        {
          path: '/signals',
          name: 'StrategySignalCenter',
          component: StrategySignalCenterView
        },
        {
          path: '/risk-warnings',
          name: 'RiskWarningCenter',
          component: RiskWarningCenterView
        },
        {
          path: '/reports/center',
          name: 'ResearchReportCenter',
          component: ResearchReportCenterView
        },
        {
          path: '/audit-compliance',
          name: 'AuditComplianceCenter',
          meta: {
            requiredMenuKey: MENU_KEY.AUDIT_COMPLIANCE,
            requiredPermissionKey: PERMISSION_KEY.AUDIT_COMPLIANCE_VIEW
          },
          component: AuditComplianceCenterView
        },
        {
          path: '/model-agent-config',
          name: 'ModelAgentConfigCenter',
          meta: {
            requiredMenuKey: MENU_KEY.MODEL_AGENT_CONFIG,
            requiredPermissionKey: PERMISSION_KEY.MODEL_AGENT_CONFIG_VIEW
          },
          component: ModelAgentConfigCenterView
        },
        {
          path: '/reports/pending',
          name: 'PendingReportWorkbench',
          meta: {
            requiredMenuKey: MENU_KEY.REPORTS_PENDING,
            requiredPermissionKey: PERMISSION_KEY.REPORT_REVIEW
          },
          component: PendingReportWorkbenchView
        },
        {
          path: '/reports/approved',
          name: 'ApprovedReportWorkbench',
          meta: {
            requiredMenuKey: MENU_KEY.REPORTS_APPROVED,
            requiredPermissionKey: PERMISSION_KEY.REPORT_REVIEW
          },
          component: ApprovedReportWorkbenchView
        },
        {
          path: '/reports/rejected',
          name: 'RejectedReportWorkbench',
          meta: {
            requiredMenuKey: MENU_KEY.REPORTS_REJECTED,
            requiredPermissionKey: PERMISSION_KEY.REPORT_REVIEW
          },
          component: RejectedReportWorkbenchView
        }
      ]
    }
  ]
})

router.beforeEach(async (to) => {
  const requiredMenuKey = to.meta.requiredMenuKey as string | undefined
  const requiredPermissionKey = to.meta.requiredPermissionKey as string | undefined
  if (!requiredMenuKey && !requiredPermissionKey) {
    return true
  }

  try {
    await ensureRoleAccessConfigsLoaded()
  } catch {
    // 后端未启动时使用本地默认权限，保证前端可独立预览。
  }
  const currentUser = getCurrentUser()
  if (requiredMenuKey && !hasMenuAccess(requiredMenuKey, currentUser.userRole)) {
    return {
      path: '/dashboard',
      replace: true
    }
  }

  if (requiredPermissionKey && !hasPermission(requiredPermissionKey, currentUser.userRole)) {
    return {
      path: '/dashboard',
      replace: true
    }
  }

  return true
})

export default router
