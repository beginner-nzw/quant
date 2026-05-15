<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  fetchAuditComplianceStats,
  fetchMarketEventStats,
  fetchMarketIntelligenceStats,
  fetchReportCenterStats,
  fetchRiskWarningStats,
  fetchStrategySignalStats,
  fetchTaskStats
} from '../api/task'
import type {
  AuditComplianceStats,
  MarketEventStats,
  MarketIntelligenceStats,
  ReportCenterStats,
  RiskWarningStats,
  StrategySignalStats,
  TaskStats
} from '../types/task'
import { getCurrentUser } from '../utils/auth'
import { canAccessAuditCompliance, canCreateTasks, canManageModelAgentConfig } from '../utils/roleAccess'

const router = useRouter()
const currentUser = getCurrentUser()
const loading = ref(false)
const failedCount = ref(0)

const taskStats = ref<TaskStats>({
  totalCount: 0,
  runningCount: 0,
  successCount: 0,
  failedCount: 0,
  retriedCount: 0
})

const riskStats = ref<RiskWarningStats>({
  totalCount: 0,
  highCount: 0,
  mediumCount: 0,
  lowCount: 0,
  pendingReviewCount: 0,
  humanReviewCount: 0
})

const signalStats = ref<StrategySignalStats>({
  totalCount: 0,
  positiveCount: 0,
  neutralCount: 0,
  negativeCount: 0,
  highConfidenceCount: 0,
  pendingReviewCount: 0
})

const reportStats = ref<ReportCenterStats>({
  totalCount: 0,
  highConfidenceCount: 0,
  pendingReviewCount: 0,
  approvedCount: 0,
  humanReviewCount: 0
})

const intelligenceStats = ref<MarketIntelligenceStats>({
  totalCount: 0,
  riskAlertCount: 0,
  strategySignalCount: 0,
  reportInsightCount: 0,
  highPriorityCount: 0,
  pendingReviewCount: 0
})

const eventStats = ref<MarketEventStats>({
  totalCount: 0,
  activeCount: 0,
  highImpactCount: 0,
  trackedCount: 0,
  todayCount: 0
})

const auditStats = ref<AuditComplianceStats>({
  totalCount: 0,
  pendingReviewCount: 0,
  interceptedCount: 0,
  revisedReportCount: 0,
  humanReviewCount: 0,
  decisionTraceCount: 0,
  promptAuditCount: 0
})

const canViewAudit = computed(() => canAccessAuditCompliance(currentUser.userRole))
const canViewModelConfig = computed(() => canManageModelAgentConfig(currentUser.userRole))
const canCreateResearchTask = computed(() => canCreateTasks(currentUser.userRole))

const situationMetrics = computed(() => [
  {
    label: '运行任务',
    value: taskStats.value.runningCount,
    suffix: 'active',
    path: '/tasks',
    tone: 'blue'
  },
  {
    label: '高风险',
    value: riskStats.value.highCount,
    suffix: 'alerts',
    path: '/risk-warnings',
    tone: 'red'
  },
  {
    label: '高置信信号',
    value: signalStats.value.highConfidenceCount,
    suffix: 'signals',
    path: '/signals',
    tone: 'green'
  },
  {
    label: '待审报告',
    value: reportStats.value.pendingReviewCount,
    suffix: 'reviews',
    path: '/reports/pending',
    tone: 'amber'
  }
])

const lanes = computed(() => [
  {
    name: '事件雷达',
    description: '市场新闻、公告、政策和舆情进入事件池，自动识别高影响事件。',
    metric: eventStats.value.highImpactCount,
    metricLabel: '高影响事件',
    path: '/market-events',
    glyph: '01',
    color: '#ff8764'
  },
  {
    name: '情报中枢',
    description: '将风险、策略、报告洞察合并为可分发、可转任务的情报项。',
    metric: intelligenceStats.value.highPriorityCount,
    metricLabel: '高优先级情报',
    path: '/intelligence',
    glyph: '02',
    color: '#69d2c4'
  },
  {
    name: 'Agent 任务流',
    description: 'LangGraph 拆解执行路径，沉淀每个 Agent 的状态、证据与耗时。',
    metric: taskStats.value.totalCount,
    metricLabel: '任务总量',
    path: '/tasks',
    glyph: '03',
    color: '#5e8cff'
  },
  {
    name: '报告交付',
    description: '结论、证据、风险提示与审核记录组合成可归档研究产物。',
    metric: reportStats.value.totalCount,
    metricLabel: '报告总量',
    path: '/reports/center',
    glyph: '04',
    color: '#b58cff'
  }
])

const operatingStats = computed(() => [
  { label: '事件总数', value: eventStats.value.totalCount, hint: `${eventStats.value.todayCount} 个今日新增` },
  { label: '风险分布', value: `${riskStats.value.highCount}/${riskStats.value.mediumCount}/${riskStats.value.lowCount}`, hint: '高 / 中 / 低' },
  { label: '信号方向', value: `${signalStats.value.positiveCount}/${signalStats.value.neutralCount}/${signalStats.value.negativeCount}`, hint: '偏多 / 中性 / 偏空' },
  { label: '报告通过', value: reportStats.value.approvedCount, hint: `${reportStats.value.highConfidenceCount} 份高置信` },
  { label: '人工复核', value: canViewAudit.value ? auditStats.value.humanReviewCount : '-', hint: canViewAudit.value ? `${auditStats.value.decisionTraceCount} 条决策链` : '当前角色不可见' }
])

const governanceCards = computed(() => [
  {
    name: 'Agent 控制台',
    description: '配置模型策略、Prompt、工作流节点和工具白名单。',
    path: '/model-agent-config',
    visible: canViewModelConfig.value
  },
  {
    name: '审计链路',
    description: '查看 AI 输出、人工复核、拦截和证据引用记录。',
    path: '/audit-compliance',
    visible: canViewAudit.value
  },
  {
    name: '报告审核队列',
    description: '处理待审、通过、驳回的研究报告流转。',
    path: '/reports/pending',
    visible: canViewAudit.value
  }
].filter((item) => item.visible))

async function loadDashboard() {
  loading.value = true
  failedCount.value = 0

  const results = await Promise.allSettled([
    fetchTaskStats(),
    fetchRiskWarningStats(),
    fetchStrategySignalStats(),
    fetchReportCenterStats(),
    fetchMarketIntelligenceStats(),
    fetchMarketEventStats()
  ])

  applyResult(results[0], (data) => {
    taskStats.value = data
  })
  applyResult(results[1], (data) => {
    riskStats.value = data
  })
  applyResult(results[2], (data) => {
    signalStats.value = data
  })
  applyResult(results[3], (data) => {
    reportStats.value = data
  })
  applyResult(results[4], (data) => {
    intelligenceStats.value = data
  })
  applyResult(results[5], (data) => {
    eventStats.value = data
  })

  if (canViewAudit.value) {
    const auditResult = await Promise.allSettled([fetchAuditComplianceStats()])
    applyResult(auditResult[0], (data) => {
      auditStats.value = data
    })
  }

  loading.value = false
}

function applyResult<T>(
  result: PromiseSettledResult<{ success: boolean, data: T }>,
  setter: (data: T) => void
) {
  if (result.status === 'fulfilled' && result.value.success) {
    setter(result.value.data)
    return
  }
  failedCount.value += 1
}

function goPath(path: string) {
  router.push(path)
}

onMounted(() => {
  loadDashboard()
})
</script>

<template>
  <div v-loading="loading" class="ops-dashboard">
    <section class="mission-hero">
      <div class="hero-main">
        <span class="kicker">Quant Research Command Deck</span>
        <h2>从事件到报告的投研作战台</h2>
        <p>
          这里不再是菜单集合，而是一条业务链路：事件进入、任务拆解、Agent 执行、
          风险复核、报告交付。研究员和管理者看到的是同一套业务对象的不同视角。
        </p>

        <div class="hero-actions">
          <button v-if="canCreateResearchTask" type="button" class="primary-action" @click="goPath('/tasks/create')">
            发起深度研究
          </button>
          <button type="button" @click="goPath('/market-events')">
            查看事件雷达
          </button>
          <button type="button" @click="goPath('/research-workbench')">
            进入标的工作台
          </button>
        </div>
      </div>

      <div class="radar-card">
        <div class="radar-visual">
          <span class="ring ring-a" />
          <span class="ring ring-b" />
          <span class="ring ring-c" />
          <strong>{{ riskStats.highCount }}</strong>
          <small>High Risk</small>
        </div>
        <div class="radar-copy">
          <span>实时态势</span>
          <strong>{{ eventStats.activeCount }} 个活跃事件正在被跟踪</strong>
          <p>{{ signalStats.highConfidenceCount }} 个高置信策略信号，{{ reportStats.pendingReviewCount }} 份报告等待审核。</p>
        </div>
      </div>
    </section>

    <el-alert
      v-if="failedCount > 0"
      class="soft-alert"
      :title="`有 ${failedCount} 个统计接口暂不可用，已用 0 值兜底展示。`"
      type="warning"
      :closable="false"
      show-icon
    />

    <section class="situation-grid">
      <button
        v-for="item in situationMetrics"
        :key="item.label"
        type="button"
        class="situation-card"
        :class="`tone-${item.tone}`"
        @click="goPath(item.path)"
      >
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.suffix }}</small>
      </button>
    </section>

    <section class="flow-section">
      <div class="section-heading">
        <span>Operating Flow</span>
        <h3>核心业务链路</h3>
      </div>

      <div class="lane-grid">
        <button
          v-for="lane in lanes"
          :key="lane.name"
          type="button"
          class="lane-card"
          :style="{ '--lane': lane.color }"
          @click="goPath(lane.path)"
        >
          <span class="lane-index">{{ lane.glyph }}</span>
          <strong>{{ lane.name }}</strong>
          <p>{{ lane.description }}</p>
          <div>
            <b>{{ lane.metric }}</b>
            <small>{{ lane.metricLabel }}</small>
          </div>
        </button>
      </div>
    </section>

    <section class="split-layout">
      <article class="insight-panel">
        <div class="section-heading">
          <span>Business State</span>
          <h3>关键态势总览</h3>
        </div>

        <div class="stat-list">
          <div v-for="item in operatingStats" :key="item.label">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <small>{{ item.hint }}</small>
          </div>
        </div>
      </article>

      <article class="governance-panel">
        <div class="section-heading">
          <span>Governance</span>
          <h3>治理控制台</h3>
        </div>

        <div v-if="governanceCards.length" class="governance-list">
          <button
            v-for="item in governanceCards"
            :key="item.name"
            type="button"
            @click="goPath(item.path)"
          >
            <strong>{{ item.name }}</strong>
            <small>{{ item.description }}</small>
          </button>
        </div>
        <div v-else class="empty-governance">
          当前角色没有治理后台权限。业务前台仍可完成事件查看、投研发起、风险预警和报告阅读。
        </div>
      </article>
    </section>
  </div>
</template>

<style scoped>
.ops-dashboard {
  display: grid;
  gap: 22px;
}

.mission-hero,
.flow-section,
.insight-panel,
.governance-panel {
  border: 1px solid rgba(20, 32, 51, 0.08);
  border-radius: 34px;
  background: rgba(255, 255, 255, 0.66);
  box-shadow: 0 22px 70px rgba(32, 52, 80, 0.1);
  backdrop-filter: blur(18px);
}

.mission-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(320px, 0.85fr);
  gap: 22px;
  overflow: hidden;
  padding: 24px;
  background:
    radial-gradient(circle at 18% 20%, rgba(105, 210, 196, 0.22), transparent 30%),
    radial-gradient(circle at 82% 12%, rgba(242, 198, 109, 0.24), transparent 26%),
    rgba(255, 255, 255, 0.66);
}

.hero-main {
  min-height: 390px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 18px;
}

.kicker,
.section-heading span,
.radar-copy span {
  display: block;
  color: #527086;
  font-size: 12px;
  font-weight: 950;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.hero-main h2 {
  max-width: 760px;
  margin: 16px 0 0;
  color: #142033;
  font-size: clamp(46px, 6vw, 82px);
  font-weight: 950;
  letter-spacing: -0.09em;
  line-height: 0.93;
}

.hero-main p {
  max-width: 720px;
  margin: 22px 0 0;
  color: #526276;
  font-size: 17px;
  line-height: 1.95;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 30px;
}

.hero-actions button,
.lane-card,
.situation-card,
.governance-list button {
  border: 0;
  font: inherit;
  cursor: pointer;
}

.hero-actions button {
  padding: 14px 20px;
  border-radius: 999px;
  background: #fff;
  color: #243047;
  font-weight: 950;
  box-shadow: inset 0 0 0 1px rgba(20, 32, 51, 0.08), 0 12px 28px rgba(32, 52, 80, 0.08);
}

.hero-actions .primary-action {
  background: #142033;
  color: #fff;
  box-shadow: 0 18px 38px rgba(20, 32, 51, 0.22);
}

.radar-card {
  display: grid;
  align-content: center;
  gap: 22px;
  padding: 24px;
  border-radius: 30px;
  background:
    linear-gradient(160deg, #142033, #1c3854),
    #142033;
  color: #fff;
}

.radar-visual {
  position: relative;
  width: min(310px, 80vw);
  aspect-ratio: 1;
  display: grid;
  place-items: center;
  margin: 0 auto;
  border-radius: 999px;
  background:
    radial-gradient(circle, rgba(105, 210, 196, 0.22), transparent 32%),
    repeating-conic-gradient(from 0deg, rgba(105, 210, 196, 0.18) 0deg 8deg, transparent 8deg 18deg);
}

.ring {
  position: absolute;
  border: 1px solid rgba(105, 210, 196, 0.26);
  border-radius: 999px;
}

.ring-a {
  inset: 14%;
}

.ring-b {
  inset: 28%;
}

.ring-c {
  inset: 42%;
}

.radar-visual strong,
.radar-visual small {
  position: relative;
  z-index: 1;
  display: block;
  text-align: center;
}

.radar-visual strong {
  font-size: 72px;
  font-weight: 950;
  letter-spacing: -0.08em;
}

.radar-visual small {
  margin-top: 72px;
  margin-left: -98px;
  color: rgba(255, 255, 255, 0.62);
  font-weight: 900;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.radar-copy strong {
  display: block;
  margin-top: 8px;
  font-size: 22px;
  font-weight: 950;
}

.radar-copy p {
  margin: 10px 0 0;
  color: rgba(255, 255, 255, 0.64);
  line-height: 1.8;
}

.soft-alert {
  border-radius: 18px;
}

.situation-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.situation-card {
  position: relative;
  overflow: hidden;
  min-height: 154px;
  padding: 20px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.72);
  color: #142033;
  text-align: left;
  box-shadow: inset 0 0 0 1px rgba(20, 32, 51, 0.08), 0 18px 50px rgba(32, 52, 80, 0.09);
}

.situation-card::after {
  content: '';
  position: absolute;
  right: -36px;
  bottom: -48px;
  width: 136px;
  height: 136px;
  border-radius: 999px;
  background: currentColor;
  opacity: 0.13;
}

.situation-card span,
.situation-card strong,
.situation-card small {
  position: relative;
  z-index: 1;
  display: block;
}

.situation-card span {
  color: #687789;
  font-weight: 900;
}

.situation-card strong {
  margin-top: 18px;
  font-size: 48px;
  font-weight: 950;
  letter-spacing: -0.08em;
}

.situation-card small {
  color: #7a8798;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.tone-blue {
  color: #2653b1;
}

.tone-red {
  color: #cf3b38;
}

.tone-green {
  color: #158c73;
}

.tone-amber {
  color: #b26b11;
}

.flow-section,
.insight-panel,
.governance-panel {
  padding: 24px;
}

.section-heading h3 {
  margin: 8px 0 0;
  color: #142033;
  font-size: clamp(24px, 3vw, 38px);
  font-weight: 950;
  letter-spacing: -0.06em;
}

.lane-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-top: 22px;
}

.lane-card {
  min-height: 270px;
  display: flex;
  flex-direction: column;
  padding: 20px;
  border-radius: 28px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.86), rgba(255, 255, 255, 0.54)),
    #fff;
  color: #142033;
  text-align: left;
  box-shadow: inset 0 0 0 1px rgba(20, 32, 51, 0.08);
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.lane-card:hover {
  transform: translateY(-3px);
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--lane), transparent 48%), 0 22px 58px rgba(32, 52, 80, 0.14);
}

.lane-index {
  width: 52px;
  height: 52px;
  display: grid;
  place-items: center;
  border-radius: 18px;
  background: color-mix(in srgb, var(--lane), white 78%);
  color: color-mix(in srgb, var(--lane), #142033 30%);
  font-size: 18px;
  font-weight: 950;
}

.lane-card strong {
  display: block;
  margin-top: 18px;
  font-size: 20px;
  font-weight: 950;
}

.lane-card p {
  margin: 10px 0 0;
  color: #657287;
  line-height: 1.8;
}

.lane-card div {
  margin-top: auto;
  padding-top: 18px;
}

.lane-card b,
.lane-card small {
  display: block;
}

.lane-card b {
  font-size: 34px;
  font-weight: 950;
  color: var(--lane);
}

.lane-card small {
  color: #748396;
}

.split-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(320px, 0.95fr);
  gap: 22px;
}

.stat-list {
  display: grid;
  gap: 10px;
  margin-top: 22px;
}

.stat-list div {
  display: grid;
  grid-template-columns: 120px minmax(120px, 1fr) minmax(160px, 1fr);
  align-items: center;
  gap: 18px;
  padding: 15px 16px;
  border-radius: 20px;
  background: rgba(20, 32, 51, 0.045);
}

.stat-list span {
  color: #687789;
  font-weight: 900;
}

.stat-list strong {
  color: #142033;
  font-size: 28px;
  font-weight: 950;
  letter-spacing: -0.04em;
}

.stat-list small {
  color: #748396;
}

.governance-list {
  display: grid;
  gap: 12px;
  margin-top: 22px;
}

.governance-list button {
  padding: 18px;
  border-radius: 22px;
  background: #142033;
  color: #fff;
  text-align: left;
  box-shadow: 0 18px 42px rgba(20, 32, 51, 0.16);
}

.governance-list strong,
.governance-list small {
  display: block;
}

.governance-list strong {
  font-size: 17px;
  font-weight: 950;
}

.governance-list small {
  margin-top: 8px;
  color: rgba(255, 255, 255, 0.64);
  line-height: 1.7;
}

.empty-governance {
  margin-top: 22px;
  padding: 20px;
  border-radius: 22px;
  background: rgba(20, 32, 51, 0.055);
  color: #667589;
  line-height: 1.8;
}

@media (max-width: 1280px) {
  .mission-hero,
  .split-layout {
    grid-template-columns: 1fr;
  }

  .lane-grid,
  .situation-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .mission-hero,
  .flow-section,
  .insight-panel,
  .governance-panel {
    padding: 16px;
    border-radius: 24px;
  }

  .hero-main {
    min-height: auto;
    padding: 4px;
  }

  .lane-grid,
  .situation-grid,
  .stat-list div {
    grid-template-columns: 1fr;
  }
}
</style>
