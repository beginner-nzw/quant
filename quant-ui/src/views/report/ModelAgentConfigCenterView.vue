<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import { diagnoseMarketEventSource, fetchModelAgentConfigCenter, previewMarketEventSource, updateAgentConfig, updateEventAutoTriggerRule, updateEventSourceConfig, updateModelStrategy, updatePromptTemplate, updateRoleAccessConfig, updateWorkflowConfig } from '../../api/task'
import ModelAgentConfigStatsCards from '../../components/report/ModelAgentConfigStatsCards.vue'
import type { AgentConfigItem, EventAutoTriggerRuleItem, EventSourceConfigItem, EventSourcePreviewResult, EventSourceRequestDiagnosticResult, ModelAgentConfigCenterData, ModelAgentConfigStats, ModelStrategyItem, PromptTemplateItem, RoleAccessConfigItem, WorkflowConfigItem } from '../../types/task'
import { getBooleanTagType, getBooleanText } from '../../utils/task'
import { refreshRoleAccessConfigs } from '../../utils/roleAccess'

const text = {
  title: '模型与 Agent 配置管理',
  refresh: '刷新',
  runtimeTitle: '引擎运行配置',
  readonlyTitle: '当前为只读模式',
  readonlyHint: '当前角色只有查看权限。如需编辑，请为该角色配置 MODEL_AGENT_CONFIG_EDIT 权限。',
  workflowTitle: '工作流版本',
  workflowAction: '操作',
  editWorkflow: '编辑工作流',
  workflowDialogTitle: '工作流配置',
  workflowDialogHint: '保存后，新任务会按任务类型匹配最新工作流配置；已执行中的任务不回溯变更。',
  workflowSave: '保存工作流',
  workflowSaveSuccess: '工作流配置保存成功',
  workflowSaveFailed: '工作流配置保存失败',
  workflowSaveError: '工作流配置保存异常',
  agentTitle: 'Agent 注册清单',
  agentAction: '操作',
  editAgent: '编辑 Agent',
  agentDialogTitle: 'Agent 配置',
  agentDialogHint: '保存后，新任务会读取最新 Agent 配置。当前已接入真实运行时的是 enabled 和 timeoutSeconds。',
  agentSave: '保存 Agent',
  agentSaveSuccess: 'Agent 配置保存成功',
  agentSaveFailed: 'Agent 配置保存失败',
  agentSaveError: 'Agent 配置保存异常',
  modelTitle: '模型策略',
  eventAutoTriggerTitle: '事件自动触发规则',
  eventAutoTriggerAction: '操作',
  editEventAutoTrigger: '编辑规则',
  eventAutoTriggerEnabled: '全局启用',
  eventAutoTriggerConfigPath: '配置文件',
  eventAutoTriggerDialogTitle: '事件自动触发规则',
  eventAutoTriggerDialogHint: '保存后，新的高影响事件会按最新规则决定是否进入自动触发队列；入队后由事件消费者异步创建跟踪研究任务。',
  eventAutoTriggerSave: '保存规则',
  eventAutoTriggerSaveSuccess: '事件自动触发规则保存成功',
  eventAutoTriggerSaveFailed: '事件自动触发规则保存失败',
  eventAutoTriggerSaveError: '事件自动触发规则保存异常',
  eventSourceTitle: '事件源配置',
  eventSourceAction: '操作',
  editEventSource: '编辑事件源',
  previewEventSource: '预览源',
  eventSourceDialogTitle: '事件源配置',
  eventSourceDialogHint: '保存后，事件中心的模拟接入来源，以及 HTTP JSON / 巨潮包装层这两类真实接入来源配置都会按最新设置生效。',
  eventSourceSave: '保存事件源',
  eventSourceSaveSuccess: '事件源配置保存成功',
  eventSourceSaveFailed: '事件源配置保存失败',
  eventSourceSaveError: '事件源配置保存异常',
  eventSourcePreviewDialogTitle: '事件源预览',
  eventSourcePreviewHint: '预览会先生成请求诊断，再调用当前已保存的事件源配置做取数和标准化，不会导入事件。',
  eventSourcePreviewRun: '开始预览',
  eventSourcePreviewSuccess: '事件源预览成功',
  eventSourcePreviewFailed: '事件源预览失败',
  eventSourcePreviewError: '事件源预览异常',
  eventSourceDiagnosticTitle: '请求诊断',
  eventSourceDiagnosticHint: '这里展示的是模板渲染后的最终请求，便于你核对 method、url、headers、body 是否符合真实接口要求。',
  diagnosedAt: '诊断时间',
  requestStage: '请求阶段',
  requestUrl: '请求地址',
  previewItemCount: '预览条数',
  previewedAt: '预览时间',
  previewSampleCount: '样例事件数',
  eventSourceConfigPath: '配置文件',
  sourceCode: '来源编码',
  sourceName: '来源名称',
  sourceCategory: '来源分类',
  ingestMode: '接入模式',
  endpointUrl: 'HTTP地址',
  requestMethod: '请求方法',
  requestHeadersJson: '请求头 JSON',
  requestQueryJson: '查询参数 JSON',
  requestBodyJson: '请求体 JSON',
  responseItemsField: '结果字段路径',
  fieldMappingJson: '响应字段映射 JSON',
  upstreamUrl: '上游地址',
  upstreamMethod: '上游方法',
  upstreamHeadersJson: '上游请求头 JSON',
  upstreamQueryJson: '上游查询参数 JSON',
  upstreamBodyJson: '上游请求体 JSON',
  upstreamItemsField: '上游结果路径',
  upstreamFieldMappingJson: '上游字段映射 JSON',
  supportsMockIngest: '支持模拟接入',
  sslVerify: '校验证书',
  defaultEventType: '默认事件类型',
  defaultImpactLevel: '默认影响等级',
  ingestRecordCount: '接入次数',
  totalCount: '接入条数',
  failedCount: '失败次数',
  autoTriggeredCount: '自动入队数',
  lastIngestAt: '最近接入',
  lastResultStatus: '最近结果',
  lastErrorMessage: '最近错误',
  strategyAction: '操作',
  editStrategy: '编辑策略',
  strategyDialogTitle: '模型策略',
  strategyDialogHint: '保存后，新任务会按场景读取最新模型配置；已在执行中的任务不回溯变更。',
  strategySave: '保存策略',
  strategySaveSuccess: '模型策略保存成功',
  strategySaveFailed: '模型策略保存失败',
  strategySaveError: '模型策略保存异常',
  promptTitle: 'Prompt 模板',
  promptPath: '模板路径',
  promptAction: '操作',
  viewPrompt: '查看模板',
  editPrompt: '编辑模板',
  promptDialogTitle: 'Prompt 模板',
  promptDialogHint: '保存后，新任务会直接读取最新系统 Prompt，无需重启 ai-engine。',
  promptContent: '模板内容',
  promptSave: '保存模板',
  promptSaveSuccess: 'Prompt 模板保存成功',
  promptSaveFailed: 'Prompt 模板保存失败',
  promptSaveError: 'Prompt 模板保存异常',
  toolTitle: '工具白名单',
  roleAccessTitle: '角色与菜单权限',
  roleAccessAction: '操作',
  editRoleAccess: '编辑角色',
  roleAccessDialogTitle: '角色权限配置',
  roleAccessDialogHint: '保存后，前端菜单和路由访问会按最新角色权限配置刷新。',
  roleAccessSave: '保存角色权限',
  roleAccessSaveSuccess: '角色权限配置保存成功',
  roleAccessSaveFailed: '角色权限配置保存失败',
  roleAccessSaveError: '角色权限配置保存异常',
  auditTitle: '配置变更审计',
  loadFailed: '模型与 Agent 配置加载失败',
  loadError: '模型与 Agent 配置加载异常',
  engineCode: '引擎标识',
  env: '环境',
  hostPort: '监听地址',
  runtimeMode: '运行模式',
  workflowTimeout: '工作流总超时',
  consumerGroup: 'Kafka Consumer Group',
  kafkaBootstrap: 'Kafka Bootstrap',
  redis: 'Redis',
  topics: 'Topics',
  enabled: '启用',
  defaultSelected: '默认',
  workflowCode: '工作流编码',
  workflowVersion: '版本',
  workflowType: '类型',
  taskTypes: '任务类型',
  entryAgent: '入口 Agent',
  nodeCount: '节点数',
  nodeSequence: '节点链路',
  nodeTimeoutSummary: '节点超时',
  remark: '说明',
  agentCode: 'Agent 编码',
  agentName: 'Agent 名称',
  stageCode: '阶段',
  executionOrder: '顺序',
  timeoutSeconds: '超时(秒)',
  needHumanReview: '需人工复核',
  implementationMode: '实现模式',
  toolWhitelist: '工具白名单',
  inputKeys: '输入键',
  outputKeys: '输出键',
  strategyCode: '策略编码',
  ruleCode: '规则编码',
  ruleName: '规则名称',
  scenarioCode: '场景',
  eventTypes: '事件类型',
  impactLevels: '影响等级',
  analysisScope: '分析范围',
  provider: '提供方',
  modelName: '模型名称',
  baseUrl: '模型地址',
  accessMode: '接入模式',
  placeholder: '占位',
  fallbackEnabled: '允许回退',
  requestTimeoutSeconds: '请求超时(秒)',
  temperature: 'Temperature',
  maxTokens: 'Max Tokens',
  promptTemplateCode: '绑定 Prompt',
  boundAgents: '绑定 Agent',
  templateCode: '模板编码',
  templateName: '模板名称',
  sourceType: '来源',
  sourceChannel: '来源渠道',
  editable: '可编辑',
  titleTemplate: '任务标题模板',
  boundAgentCode: '绑定 Agent',
  variables: '变量',
  toolCode: '工具编码',
  toolName: '工具名称',
  toolType: '类型',
  scope: '作用域',
  roleCode: '角色编码',
  roleName: '角色名称',
  roleDescription: '角色说明',
  menuKeys: '菜单权限',
  permissionKeys: '能力权限',
  configType: '配置类型',
  targetConfig: '目标配置',
  operator: '操作人',
  operation: '操作',
  changedFields: '变更字段',
  changeSummary: '变更摘要',
  configPath: '配置文件',
  createdAt: '变更时间',
  priority: '优先级',
  targetType: '标的类型',
  targetCode: '标的代码',
  targetName: '标的名称',
  eventTitle: '事件标题',
  occurredAt: '发生时间',
  empty: '暂无配置数据'
} as const

const defaultStats: ModelAgentConfigStats = {
  workflowCount: 0,
  activeAgentCount: 0,
  modelStrategyCount: 0,
  eventAutoTriggerRuleCount: 0,
  eventSourceConfigCount: 0,
  promptTemplateCount: 0,
  toolWhitelistCount: 0,
  placeholderStrategyCount: 0,
  configAuditCount: 0,
  roleAccessConfigCount: 0
}

const loading = ref(false)
const data = ref<ModelAgentConfigCenterData | null>(null)
const canEditConfig = computed(() => data.value?.editable === true)
const workflowDialogVisible = ref(false)
const workflowSaving = ref(false)
const editingWorkflow = ref<WorkflowConfigItem | null>(null)
const workflowForm = ref({
  workflowVersion: '1.0.0',
  workflowType: 'LANGGRAPH_STATE_GRAPH',
  taskTypesText: '',
  enabled: true,
  defaultSelected: false,
  nodeSequenceText: '',
  remark: ''
})
const agentDialogVisible = ref(false)
const agentSaving = ref(false)
const editingAgent = ref<AgentConfigItem | null>(null)
const agentForm = ref({
  agentName: '',
  stageCode: '',
  executionOrder: 1,
  enabled: true,
  timeoutSeconds: 60,
  needHumanReview: false,
  implementationMode: '',
  version: '1.0.0',
  toolWhitelistText: '',
  inputKeysText: '',
  outputKeysText: '',
  remark: ''
})
const strategyDialogVisible = ref(false)
const strategySaving = ref(false)
const editingStrategy = ref<ModelStrategyItem | null>(null)
const strategyForm = ref({
  provider: '',
  modelName: '',
  baseUrl: '',
  accessMode: '',
  enabled: true,
  placeholder: false,
  fallbackEnabled: true,
  requestTimeoutSeconds: 60,
  temperature: 0.2,
  maxTokens: 800,
  promptTemplateCode: '',
  boundAgentsText: '',
  remark: ''
})
const eventAutoTriggerDialogVisible = ref(false)
const eventAutoTriggerSaving = ref(false)
const editingEventAutoTriggerRule = ref<EventAutoTriggerRuleItem | null>(null)
const eventAutoTriggerForm = ref({
  configEnabled: true,
  ruleName: '',
  enabled: true,
  eventTypesText: '',
  impactLevelsText: '',
  taskType: '',
  analysisScope: '',
  priority: '',
  sourceChannel: '',
  titleTemplate: '',
  remark: ''
})
const eventSourceDialogVisible = ref(false)
const eventSourceSaving = ref(false)
const editingEventSource = ref<EventSourceConfigItem | null>(null)
const eventSourceForm = ref({
  sourceName: '',
  sourceCategory: '',
  sourceChannel: '',
  ingestMode: '',
  enabled: true,
  supportsMockIngest: false,
  sslVerify: true,
  endpointUrl: '',
  requestMethod: 'GET',
  requestTimeoutSeconds: 15,
  requestHeadersJson: '',
  requestQueryJson: '',
  requestBodyJson: '',
  responseItemsField: '',
  fieldMappingJson: '',
  upstreamUrl: '',
  upstreamMethod: 'GET',
  upstreamHeadersJson: '',
  upstreamQueryJson: '',
  upstreamBodyJson: '',
  upstreamItemsField: '',
  upstreamFieldMappingJson: '',
  defaultEventType: '',
  defaultImpactLevel: '',
  remark: ''
})
const eventSourcePreviewDialogVisible = ref(false)
const eventSourcePreviewLoading = ref(false)
const previewingEventSource = ref<EventSourceConfigItem | null>(null)
const eventSourceDiagnosticResult = ref<EventSourceRequestDiagnosticResult | null>(null)
const eventSourcePreviewResult = ref<EventSourcePreviewResult | null>(null)
const eventSourcePreviewForm = ref({
  targetType: 'STOCK',
  targetCode: '600519.SH',
  targetName: '贵州茅台',
  itemCount: 3
})
const promptDialogVisible = ref(false)
const promptSaving = ref(false)
const editingPrompt = ref<PromptTemplateItem | null>(null)
const promptContent = ref('')
const roleAccessDialogVisible = ref(false)
const roleAccessSaving = ref(false)
const editingRoleAccess = ref<RoleAccessConfigItem | null>(null)
const roleAccessForm = ref({
  roleName: '',
  roleDescription: '',
  menuKeysText: '',
  permissionKeysText: '',
  remark: ''
})

const promptTemplateNameMap: Record<string, string> = {
  planner_agent_template: '任务规划模板',
  intent_agent_template: '意图识别模板',
  financial_analysis_agent_template: '财务分析模板',
  risk_review_agent_template: '风险复核模板',
  report_generation_agent_template: '报告生成模板'
}

const modelStrategyRemarkMap: Record<string, string> = {
  planner_langchain_strategy: '规划节点使用 LangChain + 百炼 OpenAI 兼容接口',
  intent_langchain_strategy: '意图节点使用 LangChain + 百炼 OpenAI 兼容接口',
  financial_langchain_strategy: '财务分析节点使用 LangChain + 百炼 OpenAI 兼容接口',
  risk_langchain_strategy: '风险复核节点使用 LangChain + 百炼 OpenAI 兼容接口',
  report_langchain_strategy: '报告生成节点使用 LangChain 主路径，自定义 HTTP 作为回退'
}

const promptTemplateRemarkMap: Record<string, string> = {
  planner_agent_template: '文件模板，保存后新任务直接生效，无需重启 ai-engine。',
  intent_agent_template: '文件模板，控制分析模式、关注维度和审核压力判断。',
  financial_analysis_agent_template: '文件模板，控制财务分析节点的系统 Prompt。',
  risk_review_agent_template: '文件模板，控制风险等级、风险点和人工复核判断。',
  report_generation_agent_template: '文件模板，控制结构化报告生成系统 Prompt。'
}

const promptTemplateRows = computed(() => {
  return (data.value?.promptTemplates || []).map((item) => ({
    ...item,
    templateName: promptTemplateNameMap[item.templateCode] || item.templateName,
    remark: promptTemplateRemarkMap[item.templateCode] || item.remark
  }))
})

const configTypeTextMap: Record<string, string> = {
  PROMPT_TEMPLATE: 'Prompt 模板',
  MODEL_STRATEGY: '模型策略',
  EVENT_AUTO_TRIGGER_RULE: '事件触发规则',
  EVENT_SOURCE_CONFIG: '事件源配置',
  AGENT_CONFIG: 'Agent 配置',
  WORKFLOW_CONFIG: '工作流配置',
  ROLE_ACCESS_CONFIG: '角色权限'
}

function shouldUseStrategyRemarkFallback(remark?: string) {
  return !!remark && (remark.includes('鍏煎') || remark.includes('浣跨敤 LangChain') || remark.includes('涓昏矾寰'))
}

const modelStrategyRows = computed(() => {
  return (data.value?.modelStrategies || []).map((item) => ({
    ...item,
    remark: shouldUseStrategyRemarkFallback(item.remark)
      ? modelStrategyRemarkMap[item.strategyCode] || item.remark
      : item.remark
  }))
})

function getRuntimeModeTagType(mode?: string) {
  switch (mode) {
    case 'RULE_PLACEHOLDER':
      return 'warning'
    case 'LANGCHAIN_WITH_FALLBACK':
      return 'success'
    default:
      return 'info'
  }
}

function getRuntimeModeText(mode?: string) {
  switch (mode) {
    case 'RULE_PLACEHOLDER':
      return '规则占位模式'
    case 'LANGCHAIN_WITH_FALLBACK':
      return 'LangChain 主路径 + 回退兜底'
    default:
      return mode || '-'
  }
}

function getConfigTypeText(type?: string) {
  return (type && configTypeTextMap[type]) || type || '-'
}

function getResultStatusTagType(status?: string) {
  switch (status) {
    case 'SUCCESS':
      return 'success'
    case 'PARTIAL_SUCCESS':
      return 'warning'
    case 'FAILED':
      return 'danger'
    default:
      return 'info'
  }
}

function getResultStatusText(status?: string) {
  switch (status) {
    case 'SUCCESS':
      return '成功'
    case 'PARTIAL_SUCCESS':
      return '部分成功'
    case 'FAILED':
      return '失败'
    default:
      return status || '-'
  }
}

function renderList(items?: string[]) {
  return items?.length ? items : []
}

function openWorkflowDialog(row: WorkflowConfigItem) {
  if (!canEditConfig.value) {
    return
  }
  editingWorkflow.value = row
  workflowForm.value = {
    workflowVersion: row.workflowVersion || '1.0.0',
    workflowType: row.workflowType || 'LANGGRAPH_STATE_GRAPH',
    taskTypesText: (row.taskTypes || []).join(', '),
    enabled: !!row.enabled,
    defaultSelected: !!row.defaultSelected,
    nodeSequenceText: (row.nodeSequence || []).join(', '),
    remark: row.remark || ''
  }
  workflowDialogVisible.value = true
}

async function handleSaveWorkflow() {
  if (!editingWorkflow.value || !canEditConfig.value) {
    return
  }

  workflowSaving.value = true
  try {
    const res = await updateWorkflowConfig(editingWorkflow.value.workflowCode, {
      workflowVersion: workflowForm.value.workflowVersion,
      workflowType: workflowForm.value.workflowType,
      taskTypes: workflowForm.value.taskTypesText.split(',').map((item) => item.trim()).filter(Boolean),
      enabled: workflowForm.value.enabled,
      defaultSelected: workflowForm.value.defaultSelected,
      nodeSequence: workflowForm.value.nodeSequenceText.split(',').map((item) => item.trim()).filter(Boolean),
      remark: workflowForm.value.remark
    })

    if (res.success) {
      ElMessage.success(res.message || text.workflowSaveSuccess)
      workflowDialogVisible.value = false
      await loadConfigCenter()
    } else {
      ElMessage.error(res.message || text.workflowSaveFailed)
    }
  } catch (error: any) {
    ElMessage.error(error?.message || text.workflowSaveError)
  } finally {
    workflowSaving.value = false
  }
}

function openAgentDialog(row: AgentConfigItem) {
  if (!canEditConfig.value) {
    return
  }
  editingAgent.value = row
  agentForm.value = {
    agentName: row.agentName || '',
    stageCode: row.stageCode || '',
    executionOrder: row.executionOrder || 1,
    enabled: !!row.enabled,
    timeoutSeconds: row.timeoutSeconds || 60,
    needHumanReview: !!row.needHumanReview,
    implementationMode: row.implementationMode || '',
    version: row.version || '1.0.0',
    toolWhitelistText: (row.toolWhitelist || []).join(', '),
    inputKeysText: (row.inputKeys || []).join(', '),
    outputKeysText: (row.outputKeys || []).join(', '),
    remark: row.remark || ''
  }
  agentDialogVisible.value = true
}

async function handleSaveAgent() {
  if (!editingAgent.value || !canEditConfig.value) {
    return
  }

  agentSaving.value = true
  try {
    const res = await updateAgentConfig(editingAgent.value.agentCode, {
      agentName: agentForm.value.agentName,
      stageCode: agentForm.value.stageCode,
      executionOrder: agentForm.value.executionOrder,
      enabled: agentForm.value.enabled,
      timeoutSeconds: agentForm.value.timeoutSeconds,
      needHumanReview: agentForm.value.needHumanReview,
      implementationMode: agentForm.value.implementationMode,
      version: agentForm.value.version,
      toolWhitelist: agentForm.value.toolWhitelistText.split(',').map((item) => item.trim()).filter(Boolean),
      inputKeys: agentForm.value.inputKeysText.split(',').map((item) => item.trim()).filter(Boolean),
      outputKeys: agentForm.value.outputKeysText.split(',').map((item) => item.trim()).filter(Boolean),
      remark: agentForm.value.remark
    })

    if (res.success) {
      ElMessage.success(res.message || text.agentSaveSuccess)
      agentDialogVisible.value = false
      await loadConfigCenter()
    } else {
      ElMessage.error(res.message || text.agentSaveFailed)
    }
  } catch (error: any) {
    ElMessage.error(error?.message || text.agentSaveError)
  } finally {
    agentSaving.value = false
  }
}

function openStrategyDialog(row: ModelStrategyItem) {
  if (!canEditConfig.value) {
    return
  }
  editingStrategy.value = row
  strategyForm.value = {
    provider: row.provider || '',
    modelName: row.modelName || '',
    baseUrl: row.baseUrl || '',
    accessMode: row.accessMode || '',
    enabled: !!row.enabled,
    placeholder: !!row.placeholder,
    fallbackEnabled: row.fallbackEnabled ?? true,
    requestTimeoutSeconds: row.requestTimeoutSeconds ?? 60,
    temperature: row.temperature ?? 0.2,
    maxTokens: row.maxTokens ?? 800,
    promptTemplateCode: row.promptTemplateCode || '',
    boundAgentsText: (row.boundAgents || []).join(', '),
    remark: row.remark || ''
  }
  strategyDialogVisible.value = true
}

async function handleSaveStrategy() {
  if (!editingStrategy.value || !canEditConfig.value) {
    return
  }

  strategySaving.value = true
  try {
    const res = await updateModelStrategy(editingStrategy.value.strategyCode, {
      provider: strategyForm.value.provider,
      modelName: strategyForm.value.modelName,
      baseUrl: strategyForm.value.baseUrl,
      accessMode: strategyForm.value.accessMode,
      enabled: strategyForm.value.enabled,
      placeholder: strategyForm.value.placeholder,
      fallbackEnabled: strategyForm.value.fallbackEnabled,
      requestTimeoutSeconds: strategyForm.value.requestTimeoutSeconds,
      temperature: strategyForm.value.temperature,
      maxTokens: strategyForm.value.maxTokens,
      promptTemplateCode: strategyForm.value.promptTemplateCode,
      boundAgents: strategyForm.value.boundAgentsText
        .split(',')
        .map((item) => item.trim())
        .filter(Boolean),
      remark: strategyForm.value.remark
    })

    if (res.success) {
      ElMessage.success(res.message || text.strategySaveSuccess)
      strategyDialogVisible.value = false
      await loadConfigCenter()
    } else {
      ElMessage.error(res.message || text.strategySaveFailed)
    }
  } catch (error: any) {
    ElMessage.error(error?.message || text.strategySaveError)
  } finally {
    strategySaving.value = false
  }
}

function openEventAutoTriggerDialog(row: EventAutoTriggerRuleItem) {
  if (!canEditConfig.value) {
    return
  }
  editingEventAutoTriggerRule.value = row
  eventAutoTriggerForm.value = {
    configEnabled: data.value?.eventAutoTriggerConfig?.enabled ?? true,
    ruleName: row.ruleName || '',
    enabled: !!row.enabled,
    eventTypesText: (row.eventTypes || []).join(', '),
    impactLevelsText: (row.impactLevels || []).join(', '),
    taskType: row.taskType || '',
    analysisScope: row.analysisScope || '',
    priority: row.priority || '',
    sourceChannel: row.sourceChannel || '',
    titleTemplate: row.titleTemplate || '',
    remark: row.remark || ''
  }
  eventAutoTriggerDialogVisible.value = true
}

async function handleSaveEventAutoTriggerRule() {
  if (!editingEventAutoTriggerRule.value || !canEditConfig.value) {
    return
  }

  eventAutoTriggerSaving.value = true
  try {
    const res = await updateEventAutoTriggerRule(editingEventAutoTriggerRule.value.ruleCode, {
      configEnabled: eventAutoTriggerForm.value.configEnabled,
      ruleName: eventAutoTriggerForm.value.ruleName,
      enabled: eventAutoTriggerForm.value.enabled,
      eventTypes: eventAutoTriggerForm.value.eventTypesText.split(',').map((item) => item.trim()).filter(Boolean),
      impactLevels: eventAutoTriggerForm.value.impactLevelsText.split(',').map((item) => item.trim()).filter(Boolean),
      taskType: eventAutoTriggerForm.value.taskType,
      analysisScope: eventAutoTriggerForm.value.analysisScope,
      priority: eventAutoTriggerForm.value.priority,
      sourceChannel: eventAutoTriggerForm.value.sourceChannel,
      titleTemplate: eventAutoTriggerForm.value.titleTemplate,
      remark: eventAutoTriggerForm.value.remark
    })

    if (res.success) {
      ElMessage.success(res.message || text.eventAutoTriggerSaveSuccess)
      eventAutoTriggerDialogVisible.value = false
      await loadConfigCenter()
    } else {
      ElMessage.error(res.message || text.eventAutoTriggerSaveFailed)
    }
  } catch (error: any) {
    ElMessage.error(error?.message || text.eventAutoTriggerSaveError)
  } finally {
    eventAutoTriggerSaving.value = false
  }
}

function openEventSourceDialog(row: EventSourceConfigItem) {
  if (!canEditConfig.value) {
    return
  }
  editingEventSource.value = row
  eventSourceForm.value = {
    sourceName: row.sourceName || '',
    sourceCategory: row.sourceCategory || '',
    sourceChannel: row.sourceChannel || '',
    ingestMode: row.ingestMode || '',
    enabled: !!row.enabled,
    supportsMockIngest: !!row.supportsMockIngest,
    sslVerify: row.sslVerify !== false,
    endpointUrl: row.endpointUrl || '',
    requestMethod: row.requestMethod || 'GET',
    requestTimeoutSeconds: row.requestTimeoutSeconds || 15,
    requestHeadersJson: row.requestHeadersJson || '',
    requestQueryJson: row.requestQueryJson || '',
    requestBodyJson: row.requestBodyJson || '',
    responseItemsField: row.responseItemsField || '',
    fieldMappingJson: row.fieldMappingJson || '',
    upstreamUrl: row.upstreamUrl || '',
    upstreamMethod: row.upstreamMethod || 'GET',
    upstreamHeadersJson: row.upstreamHeadersJson || '',
    upstreamQueryJson: row.upstreamQueryJson || '',
    upstreamBodyJson: row.upstreamBodyJson || '',
    upstreamItemsField: row.upstreamItemsField || '',
    upstreamFieldMappingJson: row.upstreamFieldMappingJson || '',
    defaultEventType: row.defaultEventType || '',
    defaultImpactLevel: row.defaultImpactLevel || '',
    remark: row.remark || ''
  }
  eventSourceDialogVisible.value = true
}

async function handleSaveEventSource() {
  if (!editingEventSource.value || !canEditConfig.value) {
    return
  }

  eventSourceSaving.value = true
  try {
    const res = await updateEventSourceConfig(editingEventSource.value.sourceCode, {
      sourceName: eventSourceForm.value.sourceName,
      sourceCategory: eventSourceForm.value.sourceCategory,
      sourceChannel: eventSourceForm.value.sourceChannel,
      ingestMode: eventSourceForm.value.ingestMode,
      enabled: eventSourceForm.value.enabled,
      supportsMockIngest: eventSourceForm.value.supportsMockIngest,
      sslVerify: eventSourceForm.value.sslVerify,
      endpointUrl: eventSourceForm.value.endpointUrl,
      requestMethod: eventSourceForm.value.requestMethod,
      requestTimeoutSeconds: eventSourceForm.value.requestTimeoutSeconds,
      requestHeadersJson: eventSourceForm.value.requestHeadersJson,
      requestQueryJson: eventSourceForm.value.requestQueryJson,
      requestBodyJson: eventSourceForm.value.requestBodyJson,
      responseItemsField: eventSourceForm.value.responseItemsField,
      fieldMappingJson: eventSourceForm.value.fieldMappingJson,
      upstreamUrl: eventSourceForm.value.upstreamUrl,
      upstreamMethod: eventSourceForm.value.upstreamMethod,
      upstreamHeadersJson: eventSourceForm.value.upstreamHeadersJson,
      upstreamQueryJson: eventSourceForm.value.upstreamQueryJson,
      upstreamBodyJson: eventSourceForm.value.upstreamBodyJson,
      upstreamItemsField: eventSourceForm.value.upstreamItemsField,
      upstreamFieldMappingJson: eventSourceForm.value.upstreamFieldMappingJson,
      defaultEventType: eventSourceForm.value.defaultEventType,
      defaultImpactLevel: eventSourceForm.value.defaultImpactLevel,
      remark: eventSourceForm.value.remark
    })

    if (res.success) {
      ElMessage.success(res.message || text.eventSourceSaveSuccess)
      eventSourceDialogVisible.value = false
      await loadConfigCenter()
    } else {
      ElMessage.error(res.message || text.eventSourceSaveFailed)
    }
  } catch (error: any) {
    ElMessage.error(error?.message || text.eventSourceSaveError)
  } finally {
    eventSourceSaving.value = false
  }
}

function openEventSourcePreviewDialog(row: EventSourceConfigItem) {
  previewingEventSource.value = row
  eventSourceDiagnosticResult.value = null
  eventSourcePreviewResult.value = null
  eventSourcePreviewForm.value = {
    targetType: 'STOCK',
    targetCode: '600519.SH',
    targetName: '贵州茅台',
    itemCount: 3
  }
  eventSourcePreviewDialogVisible.value = true
}

function buildEventSourcePreviewPayload() {
  return {
    sourceCode: previewingEventSource.value?.sourceCode || '',
    targetType: eventSourcePreviewForm.value.targetType,
    targetCode: eventSourcePreviewForm.value.targetCode,
    targetName: eventSourcePreviewForm.value.targetName,
    itemCount: eventSourcePreviewForm.value.itemCount
  }
}

async function handlePreviewEventSource() {
  if (!previewingEventSource.value) {
    return
  }

  eventSourcePreviewLoading.value = true
  eventSourceDiagnosticResult.value = null
  eventSourcePreviewResult.value = null
  try {
    const payload = buildEventSourcePreviewPayload()
    const diagnosticRes = await diagnoseMarketEventSource(previewingEventSource.value.sourceCode, payload)
    if (diagnosticRes.success) {
      eventSourceDiagnosticResult.value = diagnosticRes.data
    } else {
      eventSourceDiagnosticResult.value = null
      eventSourcePreviewResult.value = null
      ElMessage.error(diagnosticRes.message || text.eventSourcePreviewFailed)
      return
    }

    const res = await previewMarketEventSource(previewingEventSource.value.sourceCode, payload)

    if (res.success) {
      eventSourcePreviewResult.value = res.data
      ElMessage.success(res.message || text.eventSourcePreviewSuccess)
    } else {
      eventSourcePreviewResult.value = null
      ElMessage.error(res.message || text.eventSourcePreviewFailed)
    }
  } catch (error: any) {
    eventSourcePreviewResult.value = null
    ElMessage.error(error?.message || text.eventSourcePreviewError)
  } finally {
    eventSourcePreviewLoading.value = false
  }
}

function openPromptDialog(row: PromptTemplateItem) {
  editingPrompt.value = row
  promptContent.value = row.templateContent || ''
  promptDialogVisible.value = true
}

async function handleSavePrompt() {
  if (!editingPrompt.value || !canEditConfig.value) {
    return
  }

  promptSaving.value = true
  try {
    const res = await updatePromptTemplate(editingPrompt.value.templateCode, promptContent.value)
    if (res.success) {
      ElMessage.success(res.message || text.promptSaveSuccess)
      promptDialogVisible.value = false
      await loadConfigCenter()
    } else {
      ElMessage.error(res.message || text.promptSaveFailed)
    }
  } catch (error: any) {
    ElMessage.error(error?.message || text.promptSaveError)
  } finally {
    promptSaving.value = false
  }
}

function openRoleAccessDialog(row: RoleAccessConfigItem) {
  if (!canEditConfig.value) {
    return
  }
  editingRoleAccess.value = row
  roleAccessForm.value = {
    roleName: row.roleName || '',
    roleDescription: row.roleDescription || '',
    menuKeysText: (row.menuKeys || []).join(', '),
    permissionKeysText: (row.permissionKeys || []).join(', '),
    remark: row.remark || ''
  }
  roleAccessDialogVisible.value = true
}

async function handleSaveRoleAccess() {
  if (!editingRoleAccess.value || !canEditConfig.value) {
    return
  }

  roleAccessSaving.value = true
  try {
    const res = await updateRoleAccessConfig(editingRoleAccess.value.roleCode, {
      roleName: roleAccessForm.value.roleName,
      roleDescription: roleAccessForm.value.roleDescription,
      menuKeys: roleAccessForm.value.menuKeysText.split(',').map((item) => item.trim()).filter(Boolean),
      permissionKeys: roleAccessForm.value.permissionKeysText.split(',').map((item) => item.trim()).filter(Boolean),
      remark: roleAccessForm.value.remark
    })
    if (res.success) {
      ElMessage.success(res.message || text.roleAccessSaveSuccess)
      roleAccessDialogVisible.value = false
      await refreshRoleAccessConfigs()
      await loadConfigCenter()
    } else {
      ElMessage.error(res.message || text.roleAccessSaveFailed)
    }
  } catch (error: any) {
    ElMessage.error(error?.message || text.roleAccessSaveError)
  } finally {
    roleAccessSaving.value = false
  }
}

async function loadConfigCenter() {
  loading.value = true
  try {
    const res = await fetchModelAgentConfigCenter()
    if (res.success) {
      data.value = res.data
    } else {
      data.value = null
      ElMessage.error(res.message || text.loadFailed)
    }
  } catch (error: any) {
    data.value = null
    ElMessage.error(error?.message || text.loadError)
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadConfigCenter()
})
</script>

<template>
  <div v-loading="loading">
    <ModelAgentConfigStatsCards :stats="data?.stats || defaultStats" />

    <el-card style="margin-top: 16px;">
      <template #header>
        <div style="display:flex;align-items:center;justify-content:space-between;">
          <div style="font-weight: 700;">{{ text.title }}</div>
          <el-button @click="loadConfigCenter">{{ text.refresh }}</el-button>
        </div>
      </template>

      <el-alert
        v-if="!canEditConfig"
        :title="text.readonlyTitle"
        :description="text.readonlyHint"
        type="warning"
        :closable="false"
        style="margin-bottom: 16px;"
      />

      <el-card shadow="never">
        <template #header>
          <div style="font-weight: 700;">{{ text.runtimeTitle }}</div>
        </template>
        <el-descriptions :column="2" border v-if="data?.engineRuntime">
          <el-descriptions-item :label="text.engineCode">{{ data.engineRuntime.engineCode }}</el-descriptions-item>
          <el-descriptions-item :label="text.env">{{ data.engineRuntime.env }}</el-descriptions-item>
          <el-descriptions-item :label="text.hostPort">{{ data.engineRuntime.host }}:{{ data.engineRuntime.port }}</el-descriptions-item>
          <el-descriptions-item :label="text.runtimeMode">
            <el-tag :type="getRuntimeModeTagType(data.engineRuntime.runtimeMode)">
              {{ getRuntimeModeText(data.engineRuntime.runtimeMode) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="text.workflowTimeout">{{ data.engineRuntime.workflowTimeoutSeconds }}s</el-descriptions-item>
          <el-descriptions-item :label="text.consumerGroup">{{ data.engineRuntime.consumerGroup }}</el-descriptions-item>
          <el-descriptions-item :label="text.kafkaBootstrap">{{ data.engineRuntime.kafkaBootstrapServers }}</el-descriptions-item>
          <el-descriptions-item :label="text.redis">{{ data.engineRuntime.redisEndpoint }}</el-descriptions-item>
          <el-descriptions-item :label="text.topics" :span="2">
            {{ data.engineRuntime.dispatchTopic }} / {{ data.engineRuntime.statusTopic }} / {{ data.engineRuntime.resultTopic }} / {{ data.engineRuntime.auditTopic }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card shadow="never" style="margin-top: 16px;">
        <template #header>
          <div style="font-weight: 700;">{{ text.workflowTitle }}</div>
        </template>
        <el-table :data="data?.workflows || []" border :empty-text="text.empty">
          <el-table-column prop="workflowCode" :label="text.workflowCode" min-width="180" />
          <el-table-column prop="workflowVersion" :label="text.workflowVersion" width="90" />
          <el-table-column prop="workflowType" :label="text.workflowType" width="170" />
          <el-table-column :label="text.taskTypes" min-width="220">
            <template #default="{ row }">
              <div style="display:flex;flex-wrap:wrap;gap:6px;">
                <el-tag v-for="item in renderList(row.taskTypes)" :key="item" size="small" type="info">{{ item }}</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="entryAgent" :label="text.entryAgent" width="160" />
          <el-table-column prop="nodeCount" :label="text.nodeCount" width="90" />
          <el-table-column :label="text.enabled" width="90">
            <template #default="{ row }">
              <el-tag :type="getBooleanTagType(row.enabled, 'success', 'info')">{{ getBooleanText(row.enabled) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="text.defaultSelected" width="90">
            <template #default="{ row }">
              <el-tag :type="getBooleanTagType(row.defaultSelected, 'success', 'info')">{{ getBooleanText(row.defaultSelected) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="text.nodeSequence" min-width="260">
            <template #default="{ row }">
              <div style="display:flex;flex-wrap:wrap;gap:6px;">
                <el-tag v-for="item in renderList(row.nodeSequence)" :key="item" size="small">{{ item }}</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="nodeTimeoutSummary" :label="text.nodeTimeoutSummary" min-width="220" />
          <el-table-column v-if="canEditConfig" :label="text.workflowAction" width="120" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openWorkflowDialog(row)">{{ text.editWorkflow }}</el-button>
            </template>
          </el-table-column>
          <el-table-column prop="remark" :label="text.remark" min-width="220" show-overflow-tooltip />
        </el-table>
      </el-card>

      <el-card shadow="never" style="margin-top: 16px;">
        <template #header>
          <div style="font-weight: 700;">{{ text.agentTitle }}</div>
        </template>
        <el-table :data="data?.agents || []" border :empty-text="text.empty">
          <el-table-column prop="agentCode" :label="text.agentCode" min-width="180" />
          <el-table-column prop="agentName" :label="text.agentName" min-width="180" />
          <el-table-column prop="stageCode" :label="text.stageCode" width="170" />
          <el-table-column prop="executionOrder" :label="text.executionOrder" width="80" />
          <el-table-column prop="timeoutSeconds" :label="text.timeoutSeconds" width="90" />
          <el-table-column :label="text.enabled" width="90">
            <template #default="{ row }">
              <el-tag :type="getBooleanTagType(row.enabled, 'success', 'info')">{{ getBooleanText(row.enabled) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="text.needHumanReview" width="110">
            <template #default="{ row }">
              <el-tag :type="getBooleanTagType(row.needHumanReview, 'warning', 'info')">{{ getBooleanText(row.needHumanReview) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="implementationMode" :label="text.implementationMode" width="190" />
          <el-table-column prop="version" :label="text.workflowVersion" width="90" />
          <el-table-column :label="text.toolWhitelist" min-width="220">
            <template #default="{ row }">
              <div style="display:flex;flex-wrap:wrap;gap:6px;">
                <el-tag v-for="item in renderList(row.toolWhitelist)" :key="item" size="small">{{ item }}</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column :label="text.inputKeys" min-width="180">
            <template #default="{ row }">
              <div style="display:flex;flex-wrap:wrap;gap:6px;">
                <el-tag v-for="item in renderList(row.inputKeys)" :key="item" size="small" type="info">{{ item }}</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column :label="text.outputKeys" min-width="180">
            <template #default="{ row }">
              <div style="display:flex;flex-wrap:wrap;gap:6px;">
                <el-tag v-for="item in renderList(row.outputKeys)" :key="item" size="small" type="success">{{ item }}</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column v-if="canEditConfig" :label="text.agentAction" width="120" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openAgentDialog(row)">{{ text.editAgent }}</el-button>
            </template>
          </el-table-column>
          <el-table-column prop="remark" :label="text.remark" min-width="220" show-overflow-tooltip />
        </el-table>
      </el-card>

      <el-card shadow="never" style="margin-top: 16px;">
        <template #header>
          <div style="font-weight: 700;">{{ text.modelTitle }}</div>
        </template>
        <el-table :data="modelStrategyRows" border :empty-text="text.empty">
          <el-table-column prop="strategyCode" :label="text.strategyCode" min-width="180" />
          <el-table-column prop="scenarioCode" :label="text.scenarioCode" min-width="180" />
          <el-table-column prop="provider" :label="text.provider" width="120" />
          <el-table-column prop="modelName" :label="text.modelName" min-width="160" />
          <el-table-column prop="baseUrl" :label="text.baseUrl" min-width="260" show-overflow-tooltip />
          <el-table-column prop="accessMode" :label="text.accessMode" min-width="150" />
          <el-table-column :label="text.enabled" width="90">
            <template #default="{ row }">
              <el-tag :type="getBooleanTagType(row.enabled, 'success', 'info')">{{ getBooleanText(row.enabled) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="text.placeholder" width="90">
            <template #default="{ row }">
              <el-tag :type="getBooleanTagType(row.placeholder, 'warning', 'success')">{{ getBooleanText(row.placeholder) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="text.fallbackEnabled" width="100">
            <template #default="{ row }">
              <el-tag :type="getBooleanTagType(row.fallbackEnabled, 'warning', 'info')">{{ getBooleanText(row.fallbackEnabled) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="requestTimeoutSeconds" :label="text.requestTimeoutSeconds" width="130" />
          <el-table-column prop="temperature" :label="text.temperature" width="110" />
          <el-table-column prop="maxTokens" :label="text.maxTokens" width="110" />
          <el-table-column prop="promptTemplateCode" :label="text.promptTemplateCode" min-width="170" />
          <el-table-column :label="text.boundAgents" min-width="220">
            <template #default="{ row }">
              <div style="display:flex;flex-wrap:wrap;gap:6px;">
                <el-tag v-for="item in renderList(row.boundAgents)" :key="item" size="small">{{ item }}</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column v-if="canEditConfig" :label="text.strategyAction" width="120" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openStrategyDialog(row)">{{ text.editStrategy }}</el-button>
            </template>
          </el-table-column>
          <el-table-column prop="remark" :label="text.remark" min-width="260" show-overflow-tooltip />
        </el-table>
      </el-card>

      <el-card shadow="never" style="margin-top: 16px;">
        <template #header>
          <div style="font-weight: 700;">{{ text.promptTitle }}</div>
        </template>
        <el-table :data="promptTemplateRows" border :empty-text="text.empty">
          <el-table-column prop="templateCode" :label="text.templateCode" min-width="180" />
          <el-table-column prop="templateName" :label="text.templateName" min-width="160" />
          <el-table-column prop="version" :label="text.workflowVersion" width="90" />
          <el-table-column prop="sourceType" :label="text.sourceType" min-width="160" />
          <el-table-column :label="text.editable" width="90">
            <template #default="{ row }">
              <el-tag :type="getBooleanTagType(row.editable, 'warning', 'info')">{{ getBooleanText(row.editable) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="text.enabled" width="90">
            <template #default="{ row }">
              <el-tag :type="getBooleanTagType(row.enabled, 'success', 'info')">{{ getBooleanText(row.enabled) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="boundAgentCode" :label="text.boundAgentCode" min-width="180" />
          <el-table-column :label="text.variables" min-width="220">
            <template #default="{ row }">
              <div style="display:flex;flex-wrap:wrap;gap:6px;">
                <el-tag v-for="item in renderList(row.variables)" :key="item" size="small" type="info">{{ item }}</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="templatePath" :label="text.promptPath" min-width="220" show-overflow-tooltip />
          <el-table-column :label="text.promptAction" width="160" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openPromptDialog(row)">
                {{ row.editable && canEditConfig ? text.editPrompt : text.viewPrompt }}
              </el-button>
            </template>
          </el-table-column>
          <el-table-column prop="remark" :label="text.remark" min-width="240" show-overflow-tooltip />
        </el-table>
      </el-card>

      <el-card shadow="never" style="margin-top: 16px;">
        <template #header>
          <div style="display:flex;align-items:center;justify-content:space-between;gap:12px;">
            <div style="font-weight: 700;">{{ text.eventAutoTriggerTitle }}</div>
            <div style="display:flex;align-items:center;gap:12px;flex-wrap:wrap;">
              <el-tag :type="getBooleanTagType(data?.eventAutoTriggerConfig?.enabled, 'success', 'info')">
                {{ text.eventAutoTriggerEnabled }}: {{ getBooleanText(data?.eventAutoTriggerConfig?.enabled) }}
              </el-tag>
              <span style="font-size:12px;color:#909399;">
                {{ text.eventAutoTriggerConfigPath }}: {{ data?.eventAutoTriggerConfig?.configPath || '-' }}
              </span>
            </div>
          </div>
        </template>
        <el-table :data="data?.eventAutoTriggerConfig?.rules || []" border :empty-text="text.empty">
          <el-table-column prop="ruleCode" :label="text.ruleCode" min-width="220" />
          <el-table-column prop="ruleName" :label="text.ruleName" min-width="220" />
          <el-table-column :label="text.enabled" width="90">
            <template #default="{ row }">
              <el-tag :type="getBooleanTagType(row.enabled, 'success', 'info')">{{ getBooleanText(row.enabled) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="text.eventTypes" min-width="220">
            <template #default="{ row }">
              <div style="display:flex;flex-wrap:wrap;gap:6px;">
                <el-tag v-for="item in renderList(row.eventTypes)" :key="item" size="small">{{ item }}</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column :label="text.impactLevels" min-width="180">
            <template #default="{ row }">
              <div style="display:flex;flex-wrap:wrap;gap:6px;">
                <el-tag v-for="item in renderList(row.impactLevels)" :key="item" size="small" type="warning">{{ item }}</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="taskType" :label="text.taskTypes" min-width="180" />
          <el-table-column prop="analysisScope" :label="text.analysisScope" min-width="180" />
          <el-table-column prop="priority" :label="text.priority" width="120" />
          <el-table-column prop="sourceChannel" :label="text.sourceChannel" width="140" />
          <el-table-column prop="titleTemplate" :label="text.titleTemplate" min-width="220" show-overflow-tooltip />
          <el-table-column v-if="canEditConfig" :label="text.eventAutoTriggerAction" width="120" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openEventAutoTriggerDialog(row)">{{ text.editEventAutoTrigger }}</el-button>
            </template>
          </el-table-column>
          <el-table-column prop="remark" :label="text.remark" min-width="260" show-overflow-tooltip />
        </el-table>
      </el-card>

      <el-card shadow="never" style="margin-top: 16px;">
        <template #header>
          <div style="display:flex;align-items:center;justify-content:space-between;gap:12px;">
            <div style="font-weight: 700;">{{ text.eventSourceTitle }}</div>
            <span style="font-size:12px;color:#909399;">
              {{ text.eventSourceConfigPath }}: {{ data?.eventSourceConfig?.configPath || '-' }}
            </span>
          </div>
        </template>
        <el-table :data="data?.eventSourceConfig?.sources || []" border :empty-text="text.empty">
          <el-table-column prop="sourceCode" :label="text.sourceCode" min-width="180" />
          <el-table-column prop="sourceName" :label="text.sourceName" min-width="180" />
          <el-table-column prop="sourceCategory" :label="text.sourceCategory" width="140" />
          <el-table-column prop="sourceChannel" :label="text.sourceChannel" min-width="160" />
          <el-table-column prop="ingestMode" :label="text.ingestMode" width="120" />
          <el-table-column prop="endpointUrl" :label="text.endpointUrl" min-width="220" show-overflow-tooltip />
          <el-table-column prop="requestMethod" :label="text.requestMethod" width="100" />
          <el-table-column prop="requestTimeoutSeconds" :label="text.requestTimeoutSeconds" width="120" />
          <el-table-column prop="requestHeadersJson" :label="text.requestHeadersJson" min-width="220" show-overflow-tooltip />
          <el-table-column prop="requestQueryJson" :label="text.requestQueryJson" min-width="220" show-overflow-tooltip />
          <el-table-column prop="requestBodyJson" :label="text.requestBodyJson" min-width="220" show-overflow-tooltip />
          <el-table-column prop="responseItemsField" :label="text.responseItemsField" min-width="160" show-overflow-tooltip />
          <el-table-column prop="fieldMappingJson" :label="text.fieldMappingJson" min-width="220" show-overflow-tooltip />
          <el-table-column prop="upstreamUrl" :label="text.upstreamUrl" min-width="220" show-overflow-tooltip />
          <el-table-column prop="upstreamMethod" :label="text.upstreamMethod" width="100" />
          <el-table-column prop="upstreamHeadersJson" :label="text.upstreamHeadersJson" min-width="220" show-overflow-tooltip />
          <el-table-column prop="upstreamQueryJson" :label="text.upstreamQueryJson" min-width="220" show-overflow-tooltip />
          <el-table-column prop="upstreamBodyJson" :label="text.upstreamBodyJson" min-width="220" show-overflow-tooltip />
          <el-table-column prop="upstreamItemsField" :label="text.upstreamItemsField" min-width="160" show-overflow-tooltip />
          <el-table-column prop="upstreamFieldMappingJson" :label="text.upstreamFieldMappingJson" min-width="220" show-overflow-tooltip />
          <el-table-column :label="text.enabled" width="90">
            <template #default="{ row }">
              <el-tag :type="getBooleanTagType(row.enabled, 'success', 'info')">{{ getBooleanText(row.enabled) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="text.supportsMockIngest" width="130">
            <template #default="{ row }">
              <el-tag :type="getBooleanTagType(row.supportsMockIngest, 'success', 'info')">{{ getBooleanText(row.supportsMockIngest) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="text.sslVerify" width="110">
            <template #default="{ row }">
              <el-tag :type="getBooleanTagType(row.sslVerify !== false, 'success', 'warning')">{{ getBooleanText(row.sslVerify !== false) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="defaultEventType" :label="text.defaultEventType" width="160" />
          <el-table-column prop="defaultImpactLevel" :label="text.defaultImpactLevel" width="140" />
          <el-table-column prop="ingestRecordCount" :label="text.ingestRecordCount" width="100" align="center" />
          <el-table-column prop="totalCount" :label="text.totalCount" width="100" align="center" />
          <el-table-column prop="failedCount" :label="text.failedCount" width="100" align="center" />
          <el-table-column prop="autoTriggeredCount" :label="text.autoTriggeredCount" width="110" align="center" />
          <el-table-column :label="text.lastResultStatus" width="110">
            <template #default="{ row }">
              <el-tag :type="getResultStatusTagType(row.lastResultStatus)">{{ getResultStatusText(row.lastResultStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="text.lastIngestAt" min-width="160">
            <template #default="{ row }">
              {{ row.lastIngestAt || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="lastErrorMessage" :label="text.lastErrorMessage" min-width="220" show-overflow-tooltip />
          <el-table-column :label="text.eventSourceAction" width="180" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openEventSourcePreviewDialog(row)">{{ text.previewEventSource }}</el-button>
              <el-button v-if="canEditConfig" link type="primary" @click="openEventSourceDialog(row)">{{ text.editEventSource }}</el-button>
            </template>
          </el-table-column>
          <el-table-column prop="remark" :label="text.remark" min-width="260" show-overflow-tooltip />
        </el-table>
      </el-card>

      <el-card shadow="never" style="margin-top: 16px;">
        <template #header>
          <div style="font-weight: 700;">{{ text.toolTitle }}</div>
        </template>
        <el-table :data="data?.toolWhitelists || []" border :empty-text="text.empty">
          <el-table-column prop="toolCode" :label="text.toolCode" min-width="220" />
          <el-table-column prop="toolName" :label="text.toolName" min-width="160" />
          <el-table-column prop="toolType" :label="text.toolType" width="140" />
          <el-table-column :label="text.enabled" width="90">
            <template #default="{ row }">
              <el-tag :type="getBooleanTagType(row.enabled, 'success', 'info')">{{ getBooleanText(row.enabled) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="scope" :label="text.scope" width="160" />
          <el-table-column prop="remark" :label="text.remark" min-width="260" show-overflow-tooltip />
        </el-table>
      </el-card>

      <el-card shadow="never" style="margin-top: 16px;">
        <template #header>
          <div style="font-weight: 700;">{{ text.roleAccessTitle }}</div>
        </template>
        <el-table :data="data?.roleAccessConfigs || []" border :empty-text="text.empty">
          <el-table-column prop="roleCode" :label="text.roleCode" width="170" />
          <el-table-column prop="roleName" :label="text.roleName" width="140" />
          <el-table-column prop="roleDescription" :label="text.roleDescription" min-width="260" show-overflow-tooltip />
          <el-table-column :label="text.menuKeys" min-width="260">
            <template #default="{ row }">
              <div style="display:flex;flex-wrap:wrap;gap:6px;">
                <el-tag v-for="item in renderList(row.menuKeys)" :key="item" size="small">{{ item }}</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column :label="text.permissionKeys" min-width="240">
            <template #default="{ row }">
              <div style="display:flex;flex-wrap:wrap;gap:6px;">
                <el-tag v-for="item in renderList(row.permissionKeys)" :key="item" size="small" type="success">{{ item }}</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column v-if="canEditConfig" :label="text.roleAccessAction" width="120" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openRoleAccessDialog(row)">{{ text.editRoleAccess }}</el-button>
            </template>
          </el-table-column>
          <el-table-column prop="remark" :label="text.remark" min-width="220" show-overflow-tooltip />
        </el-table>
      </el-card>

      <el-card shadow="never" style="margin-top: 16px;">
        <template #header>
          <div style="font-weight: 700;">{{ text.auditTitle }}</div>
        </template>
        <el-table :data="data?.configChangeAudits || []" border :empty-text="text.empty">
          <el-table-column prop="createdAt" :label="text.createdAt" width="170" />
          <el-table-column :label="text.configType" width="130">
            <template #default="{ row }">
              <el-tag type="info">{{ getConfigTypeText(row.configType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="text.targetConfig" min-width="180">
            <template #default="{ row }">
              <div>{{ row.targetName || row.targetCode || '-' }}</div>
              <div style="font-size: 12px; color: #909399;">{{ row.targetCode || '-' }}</div>
            </template>
          </el-table-column>
          <el-table-column :label="text.operator" min-width="140">
            <template #default="{ row }">
              <div>{{ row.operatorId || '-' }}</div>
              <div style="font-size: 12px; color: #909399;">{{ row.operatorRole || '-' }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="operation" :label="text.operation" width="100" />
          <el-table-column :label="text.changedFields" min-width="220">
            <template #default="{ row }">
              <div style="display:flex;flex-wrap:wrap;gap:6px;">
                <el-tag v-for="item in renderList(row.changedFields)" :key="item" size="small">{{ item }}</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="changeSummary" :label="text.changeSummary" min-width="220" show-overflow-tooltip />
          <el-table-column prop="configPath" :label="text.configPath" min-width="260" show-overflow-tooltip />
        </el-table>
      </el-card>
    </el-card>

    <el-dialog
      v-model="roleAccessDialogVisible"
      :title="`${text.roleAccessDialogTitle} - ${editingRoleAccess?.roleCode || ''}`"
      width="760px"
      destroy-on-close
    >
      <div v-if="editingRoleAccess" style="display: grid; row-gap: 16px;">
        <el-alert :title="text.roleAccessDialogHint" type="info" :closable="false" />

        <el-descriptions :column="2" border>
          <el-descriptions-item :label="text.roleCode">{{ editingRoleAccess.roleCode }}</el-descriptions-item>
          <el-descriptions-item :label="text.roleName">{{ editingRoleAccess.roleName }}</el-descriptions-item>
        </el-descriptions>

        <el-form label-position="top">
          <el-form-item :label="text.roleName">
            <el-input v-model="roleAccessForm.roleName" />
          </el-form-item>

          <el-form-item :label="text.roleDescription">
            <el-input v-model="roleAccessForm.roleDescription" type="textarea" :rows="3" resize="vertical" />
          </el-form-item>

          <el-form-item :label="text.menuKeys">
            <el-input v-model="roleAccessForm.menuKeysText" placeholder="TASK_LIST, TASK_CREATE, MODEL_AGENT_CONFIG" />
          </el-form-item>

          <el-form-item :label="text.permissionKeys">
            <el-input v-model="roleAccessForm.permissionKeysText" placeholder="REPORT_REVIEW, TASK_RETRY, TASK_CANCEL, MODEL_AGENT_CONFIG_VIEW" />
          </el-form-item>

          <el-form-item :label="text.remark">
            <el-input v-model="roleAccessForm.remark" type="textarea" :rows="4" resize="vertical" />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <div style="display: flex; justify-content: flex-end; gap: 12px;">
          <el-button @click="roleAccessDialogVisible = false">关闭</el-button>
          <el-button
            type="primary"
            :loading="roleAccessSaving"
            @click="handleSaveRoleAccess"
          >
            {{ text.roleAccessSave }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="workflowDialogVisible"
      :title="`${text.workflowDialogTitle} - ${editingWorkflow?.workflowCode || ''}`"
      width="760px"
      destroy-on-close
    >
      <div v-if="editingWorkflow" style="display: grid; row-gap: 16px;">
        <el-alert :title="text.workflowDialogHint" type="info" :closable="false" />

        <el-descriptions :column="2" border>
          <el-descriptions-item :label="text.workflowCode">{{ editingWorkflow.workflowCode }}</el-descriptions-item>
          <el-descriptions-item :label="text.entryAgent">{{ editingWorkflow.entryAgent }}</el-descriptions-item>
        </el-descriptions>

        <el-form label-position="top">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item :label="text.workflowVersion">
                <el-input v-model="workflowForm.workflowVersion" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item :label="text.workflowType">
                <el-input v-model="workflowForm.workflowType" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item :label="text.taskTypes">
            <el-input
              v-model="workflowForm.taskTypesText"
              placeholder="STOCK_RESEARCH, FOLLOW_UP_RESEARCH"
            />
          </el-form-item>

          <el-form-item :label="text.nodeSequence">
            <el-input
              v-model="workflowForm.nodeSequenceText"
              placeholder="planner_agent, intent_agent, financial_analysis_agent, report_generation_agent"
            />
          </el-form-item>

          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item :label="text.enabled">
                <el-switch v-model="workflowForm.enabled" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item :label="text.defaultSelected">
                <el-switch v-model="workflowForm.defaultSelected" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item :label="text.remark">
            <el-input v-model="workflowForm.remark" type="textarea" :rows="4" resize="vertical" />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <div style="display: flex; justify-content: flex-end; gap: 12px;">
          <el-button @click="workflowDialogVisible = false">关闭</el-button>
          <el-button
            type="primary"
            :loading="workflowSaving"
            @click="handleSaveWorkflow"
          >
            {{ text.workflowSave }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="agentDialogVisible"
      :title="`${text.agentDialogTitle} - ${editingAgent?.agentCode || ''}`"
      width="760px"
      destroy-on-close
    >
      <div v-if="editingAgent" style="display: grid; row-gap: 16px;">
        <el-alert :title="text.agentDialogHint" type="info" :closable="false" />

        <el-descriptions :column="2" border>
          <el-descriptions-item :label="text.agentCode">{{ editingAgent.agentCode }}</el-descriptions-item>
          <el-descriptions-item :label="text.stageCode">{{ editingAgent.stageCode }}</el-descriptions-item>
        </el-descriptions>

        <el-form label-position="top">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item :label="text.agentName">
                <el-input v-model="agentForm.agentName" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item :label="text.stageCode">
                <el-input v-model="agentForm.stageCode" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="16">
            <el-col :span="8">
              <el-form-item :label="text.executionOrder">
                <el-input-number v-model="agentForm.executionOrder" :min="1" style="width: 100%;" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item :label="text.timeoutSeconds">
                <el-input-number v-model="agentForm.timeoutSeconds" :min="1" style="width: 100%;" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item :label="text.needHumanReview">
                <el-switch v-model="agentForm.needHumanReview" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="16">
            <el-col :span="8">
              <el-form-item :label="text.enabled">
                <el-switch v-model="agentForm.enabled" />
              </el-form-item>
            </el-col>
            <el-col :span="16">
              <el-form-item :label="text.implementationMode">
                <el-input v-model="agentForm.implementationMode" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item :label="text.workflowVersion">
            <el-input v-model="agentForm.version" />
          </el-form-item>

          <el-form-item :label="text.toolWhitelist">
            <el-input v-model="agentForm.toolWhitelistText" placeholder="tool_a, tool_b" />
          </el-form-item>

          <el-form-item :label="text.inputKeys">
            <el-input v-model="agentForm.inputKeysText" placeholder="task_id, target_code" />
          </el-form-item>

          <el-form-item :label="text.outputKeys">
            <el-input v-model="agentForm.outputKeysText" placeholder="plan_result, agent_audits" />
          </el-form-item>

          <el-form-item :label="text.remark">
            <el-input v-model="agentForm.remark" type="textarea" :rows="4" resize="vertical" />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <div style="display: flex; justify-content: flex-end; gap: 12px;">
          <el-button @click="agentDialogVisible = false">关闭</el-button>
          <el-button
            type="primary"
            :loading="agentSaving"
            @click="handleSaveAgent"
          >
            {{ text.agentSave }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="strategyDialogVisible"
      :title="`${text.strategyDialogTitle} - ${editingStrategy?.strategyCode || ''}`"
      width="760px"
      destroy-on-close
    >
      <div v-if="editingStrategy" style="display: grid; row-gap: 16px;">
        <el-alert :title="text.strategyDialogHint" type="info" :closable="false" />

        <el-descriptions :column="2" border>
          <el-descriptions-item :label="text.strategyCode">{{ editingStrategy.strategyCode }}</el-descriptions-item>
          <el-descriptions-item :label="text.scenarioCode">{{ editingStrategy.scenarioCode }}</el-descriptions-item>
        </el-descriptions>

        <el-form label-position="top">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item :label="text.provider">
                <el-input v-model="strategyForm.provider" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item :label="text.modelName">
                <el-input v-model="strategyForm.modelName" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item :label="text.baseUrl">
            <el-input v-model="strategyForm.baseUrl" />
          </el-form-item>

          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item :label="text.accessMode">
                <el-input v-model="strategyForm.accessMode" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item :label="text.promptTemplateCode">
                <el-input v-model="strategyForm.promptTemplateCode" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="16">
            <el-col :span="8">
              <el-form-item :label="text.requestTimeoutSeconds">
                <el-input-number v-model="strategyForm.requestTimeoutSeconds" :min="1" :step="5" style="width: 100%;" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item :label="text.temperature">
                <el-input-number v-model="strategyForm.temperature" :min="0" :max="2" :step="0.1" style="width: 100%;" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item :label="text.maxTokens">
                <el-input-number v-model="strategyForm.maxTokens" :min="1" :step="100" style="width: 100%;" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="16">
            <el-col :span="8">
              <el-form-item :label="text.enabled">
                <el-switch v-model="strategyForm.enabled" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item :label="text.placeholder">
                <el-switch v-model="strategyForm.placeholder" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item :label="text.fallbackEnabled">
                <el-switch v-model="strategyForm.fallbackEnabled" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item :label="text.boundAgents">
            <el-input
              v-model="strategyForm.boundAgentsText"
              placeholder="planner_agent, intent_agent"
            />
          </el-form-item>

          <el-form-item :label="text.remark">
            <el-input v-model="strategyForm.remark" type="textarea" :rows="4" resize="vertical" />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <div style="display: flex; justify-content: flex-end; gap: 12px;">
          <el-button @click="strategyDialogVisible = false">关闭</el-button>
          <el-button
            type="primary"
            :loading="strategySaving"
            @click="handleSaveStrategy"
          >
            {{ text.strategySave }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="eventAutoTriggerDialogVisible"
      :title="`${text.eventAutoTriggerDialogTitle} - ${editingEventAutoTriggerRule?.ruleCode || ''}`"
      width="760px"
      destroy-on-close
    >
      <div v-if="editingEventAutoTriggerRule" style="display: grid; row-gap: 16px;">
        <el-alert :title="text.eventAutoTriggerDialogHint" type="info" :closable="false" />

        <el-descriptions :column="2" border>
          <el-descriptions-item :label="text.ruleCode">{{ editingEventAutoTriggerRule.ruleCode }}</el-descriptions-item>
          <el-descriptions-item :label="text.ruleName">{{ editingEventAutoTriggerRule.ruleName }}</el-descriptions-item>
          <el-descriptions-item :label="text.eventAutoTriggerConfigPath" :span="2">
            {{ data?.eventAutoTriggerConfig?.configPath || '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <el-form label-position="top">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item :label="text.eventAutoTriggerEnabled">
                <el-switch v-model="eventAutoTriggerForm.configEnabled" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item :label="text.enabled">
                <el-switch v-model="eventAutoTriggerForm.enabled" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item :label="text.ruleName">
            <el-input v-model="eventAutoTriggerForm.ruleName" />
          </el-form-item>

          <el-form-item :label="text.eventTypes">
            <el-input v-model="eventAutoTriggerForm.eventTypesText" placeholder="ANNOUNCEMENT, EARNINGS, POLICY" />
          </el-form-item>

          <el-form-item :label="text.impactLevels">
            <el-input v-model="eventAutoTriggerForm.impactLevelsText" placeholder="HIGH, MEDIUM" />
          </el-form-item>

          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item :label="text.taskTypes">
                <el-input v-model="eventAutoTriggerForm.taskType" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item :label="text.analysisScope">
                <el-input v-model="eventAutoTriggerForm.analysisScope" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item :label="text.priority">
                <el-input v-model="eventAutoTriggerForm.priority" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item :label="text.sourceChannel">
                <el-input v-model="eventAutoTriggerForm.sourceChannel" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item :label="text.titleTemplate">
            <el-input v-model="eventAutoTriggerForm.titleTemplate" />
          </el-form-item>

          <el-form-item :label="text.remark">
            <el-input v-model="eventAutoTriggerForm.remark" type="textarea" :rows="4" resize="vertical" />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <div style="display: flex; justify-content: flex-end; gap: 12px;">
          <el-button @click="eventAutoTriggerDialogVisible = false">关闭</el-button>
          <el-button
            type="primary"
            :loading="eventAutoTriggerSaving"
            @click="handleSaveEventAutoTriggerRule"
          >
            {{ text.eventAutoTriggerSave }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="eventSourceDialogVisible"
      :title="`${text.eventSourceDialogTitle} - ${editingEventSource?.sourceCode || ''}`"
      width="760px"
      destroy-on-close
    >
      <div v-if="editingEventSource" style="display: grid; row-gap: 16px;">
        <el-alert :title="text.eventSourceDialogHint" type="info" :closable="false" />

        <el-descriptions :column="2" border>
          <el-descriptions-item :label="text.sourceCode">{{ editingEventSource.sourceCode }}</el-descriptions-item>
          <el-descriptions-item :label="text.eventSourceConfigPath">
            {{ data?.eventSourceConfig?.configPath || '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <el-form label-position="top">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item :label="text.sourceName">
                <el-input v-model="eventSourceForm.sourceName" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item :label="text.sourceCategory">
                <el-input v-model="eventSourceForm.sourceCategory" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item :label="text.sourceChannel">
                <el-input v-model="eventSourceForm.sourceChannel" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item :label="text.ingestMode">
                <el-input v-model="eventSourceForm.ingestMode" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item :label="text.endpointUrl">
                <el-input v-model="eventSourceForm.endpointUrl" placeholder="https://example.com/api/events" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item :label="text.requestMethod">
                <el-input v-model="eventSourceForm.requestMethod" placeholder="GET / POST" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item :label="text.requestTimeoutSeconds">
                <el-input-number v-model="eventSourceForm.requestTimeoutSeconds" :min="1" :max="300" style="width: 100%;" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item :label="text.responseItemsField">
                <el-input v-model="eventSourceForm.responseItemsField" placeholder="items / data.items" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item :label="text.requestHeadersJson">
            <el-input
              v-model="eventSourceForm.requestHeadersJson"
              type="textarea"
              :rows="3"
              resize="vertical"
              placeholder='{"User-Agent":"quant-ai-platform","Authorization":"Bearer ..."}'
            />
          </el-form-item>

          <el-form-item :label="text.requestQueryJson">
            <el-input
              v-model="eventSourceForm.requestQueryJson"
              type="textarea"
              :rows="3"
              resize="vertical"
              placeholder='{"stock":"{{targetCode}}","limit":"{{itemCount}}"}'
            />
          </el-form-item>

          <el-form-item :label="text.requestBodyJson">
            <el-input
              v-model="eventSourceForm.requestBodyJson"
              type="textarea"
              :rows="4"
              resize="vertical"
              placeholder='{"symbol":"{{targetCode}}","name":"{{targetName}}","pageSize":"{{itemCount}}"}'
            />
          </el-form-item>

          <el-form-item :label="text.fieldMappingJson">
            <el-input
              v-model="eventSourceForm.fieldMappingJson"
              type="textarea"
              :rows="4"
              resize="vertical"
              placeholder='{"targetCode":["secCode","symbol"],"eventTitle":"announcementTitle","occurredAt":"publishTime"}'
            />
          </el-form-item>

          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item :label="text.upstreamUrl">
                <el-input v-model="eventSourceForm.upstreamUrl" placeholder="https://upstream.example.com/announcements" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item :label="text.upstreamMethod">
                <el-input v-model="eventSourceForm.upstreamMethod" placeholder="GET / POST" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="16">
            <el-col :span="24">
              <el-form-item :label="text.upstreamItemsField">
                <el-input v-model="eventSourceForm.upstreamItemsField" placeholder="items / data.items / records" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item :label="text.upstreamHeadersJson">
            <el-input
              v-model="eventSourceForm.upstreamHeadersJson"
              type="textarea"
              :rows="3"
              resize="vertical"
              placeholder='{"Referer":"https://www.cninfo.com.cn","User-Agent":"Mozilla/5.0"}'
            />
          </el-form-item>

          <el-form-item :label="text.upstreamQueryJson">
            <el-input
              v-model="eventSourceForm.upstreamQueryJson"
              type="textarea"
              :rows="3"
              resize="vertical"
              placeholder='{"stockCode":"{{targetCode}}","pageSize":"{{itemCount}}"}'
            />
          </el-form-item>

          <el-form-item :label="text.upstreamBodyJson">
            <el-input
              v-model="eventSourceForm.upstreamBodyJson"
              type="textarea"
              :rows="4"
              resize="vertical"
              placeholder='{"targetCode":"{{targetCode}}","targetName":"{{targetName}}","limit":"{{itemCount}}"}'
            />
          </el-form-item>

          <el-form-item :label="text.upstreamFieldMappingJson">
            <el-input
              v-model="eventSourceForm.upstreamFieldMappingJson"
              type="textarea"
              :rows="4"
              resize="vertical"
              placeholder='{"secCode":["code","symbol"],"announcementTitle":"title","announcementTime":"publishTime"}'
            />
          </el-form-item>

          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item :label="text.defaultEventType">
                <el-input v-model="eventSourceForm.defaultEventType" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item :label="text.defaultImpactLevel">
                <el-input v-model="eventSourceForm.defaultImpactLevel" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="16">
            <el-col :span="8">
              <el-form-item :label="text.enabled">
                <el-switch v-model="eventSourceForm.enabled" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item :label="text.supportsMockIngest">
                <el-switch v-model="eventSourceForm.supportsMockIngest" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item :label="text.sslVerify">
                <el-switch v-model="eventSourceForm.sslVerify" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item :label="text.remark">
            <el-input v-model="eventSourceForm.remark" type="textarea" :rows="4" resize="vertical" />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <div style="display: flex; justify-content: flex-end; gap: 12px;">
          <el-button @click="eventSourceDialogVisible = false">关闭</el-button>
          <el-button
            type="primary"
            :loading="eventSourceSaving"
            @click="handleSaveEventSource"
          >
            {{ text.eventSourceSave }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="promptDialogVisible"
      :title="`${text.promptDialogTitle} - ${editingPrompt?.templateName || ''}`"
      width="760px"
      destroy-on-close
    >
      <div v-if="editingPrompt" style="display: grid; row-gap: 16px;">
        <el-alert :title="text.promptDialogHint" type="info" :closable="false" />

        <el-descriptions :column="1" border>
          <el-descriptions-item :label="text.templateCode">{{ editingPrompt.templateCode }}</el-descriptions-item>
          <el-descriptions-item :label="text.boundAgentCode">{{ editingPrompt.boundAgentCode || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="text.promptPath">{{ editingPrompt.templatePath || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-form label-position="top">
          <el-form-item :label="text.promptContent">
            <el-input
              v-model="promptContent"
              type="textarea"
              :rows="14"
              resize="vertical"
              :readonly="!editingPrompt.editable || !canEditConfig"
            />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <div style="display: flex; justify-content: flex-end; gap: 12px;">
          <el-button @click="promptDialogVisible = false">关闭</el-button>
          <el-button
            v-if="editingPrompt?.editable && canEditConfig"
            type="primary"
            :loading="promptSaving"
            @click="handleSavePrompt"
          >
            {{ text.promptSave }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="eventSourcePreviewDialogVisible"
      :title="`${text.eventSourcePreviewDialogTitle} - ${previewingEventSource?.sourceCode || ''}`"
      width="960px"
      destroy-on-close
    >
      <div v-if="previewingEventSource" style="display: grid; row-gap: 16px;">
        <el-alert :title="text.eventSourcePreviewHint" type="info" :closable="false" />

        <el-descriptions :column="2" border>
          <el-descriptions-item :label="text.sourceCode">{{ previewingEventSource.sourceCode }}</el-descriptions-item>
          <el-descriptions-item :label="text.sourceName">{{ previewingEventSource.sourceName }}</el-descriptions-item>
          <el-descriptions-item :label="text.ingestMode">{{ previewingEventSource.ingestMode || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="text.endpointUrl">{{ previewingEventSource.endpointUrl || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="text.upstreamUrl" :span="2">{{ previewingEventSource.upstreamUrl || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="text.requestHeadersJson" :span="2">{{ previewingEventSource.requestHeadersJson || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="text.requestQueryJson" :span="2">{{ previewingEventSource.requestQueryJson || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="text.requestBodyJson" :span="2">{{ previewingEventSource.requestBodyJson || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="text.fieldMappingJson" :span="2">{{ previewingEventSource.fieldMappingJson || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="text.upstreamHeadersJson" :span="2">{{ previewingEventSource.upstreamHeadersJson || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="text.upstreamQueryJson" :span="2">{{ previewingEventSource.upstreamQueryJson || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="text.upstreamBodyJson" :span="2">{{ previewingEventSource.upstreamBodyJson || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="text.upstreamFieldMappingJson" :span="2">{{ previewingEventSource.upstreamFieldMappingJson || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-form label-position="top">
          <el-row :gutter="16">
            <el-col :span="8">
              <el-form-item :label="text.targetType">
                <el-input v-model="eventSourcePreviewForm.targetType" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item :label="text.targetCode">
                <el-input v-model="eventSourcePreviewForm.targetCode" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item :label="text.targetName">
                <el-input v-model="eventSourcePreviewForm.targetName" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="16">
            <el-col :span="8">
              <el-form-item :label="text.previewItemCount">
                <el-input-number v-model="eventSourcePreviewForm.itemCount" :min="1" :max="20" style="width: 100%;" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>

        <div style="display: flex; justify-content: flex-end;">
          <el-button type="primary" :loading="eventSourcePreviewLoading" @click="handlePreviewEventSource">
            {{ text.eventSourcePreviewRun }}
          </el-button>
        </div>

        <template v-if="eventSourceDiagnosticResult?.items?.length">
          <div style="display: grid; row-gap: 12px;">
            <el-alert :title="text.eventSourceDiagnosticHint" type="warning" :closable="false" />

            <el-descriptions :column="2" border>
              <el-descriptions-item :label="text.eventSourceDiagnosticTitle">{{ eventSourceDiagnosticResult.sourceName || eventSourceDiagnosticResult.sourceCode }}</el-descriptions-item>
              <el-descriptions-item :label="text.diagnosedAt">{{ eventSourceDiagnosticResult.diagnosedAt || '-' }}</el-descriptions-item>
            </el-descriptions>

            <el-card
              v-for="item in eventSourceDiagnosticResult.items"
              :key="`${item.stageCode || item.stageName || 'request'}-${item.requestMethod || ''}`"
              shadow="never"
            >
              <template #header>
                <div style="font-weight: 600;">{{ item.stageName || item.stageCode || '-' }}</div>
              </template>

              <div style="display: grid; row-gap: 12px;">
                <el-descriptions :column="2" border>
                  <el-descriptions-item :label="text.requestStage">{{ item.stageName || item.stageCode || '-' }}</el-descriptions-item>
                  <el-descriptions-item :label="text.requestMethod">{{ item.requestMethod || '-' }}</el-descriptions-item>
                  <el-descriptions-item :label="text.requestTimeoutSeconds">{{ item.requestTimeoutSeconds ?? '-' }}</el-descriptions-item>
                  <el-descriptions-item :label="text.requestUrl" :span="2">{{ item.requestUrl || '-' }}</el-descriptions-item>
                </el-descriptions>

                <div style="display: grid; row-gap: 8px;">
                  <div style="font-size: 13px; color: #606266;">{{ text.requestHeadersJson }}</div>
                  <div style="white-space: pre-wrap; word-break: break-all; font-family: monospace; font-size: 12px; line-height: 1.6; padding: 12px; border-radius: 8px; background: #f5f7fa; color: #303133;">
                    {{ item.requestHeadersJson || '-' }}
                  </div>
                </div>

                <div style="display: grid; row-gap: 8px;">
                  <div style="font-size: 13px; color: #606266;">{{ text.requestBodyJson }}</div>
                  <div style="white-space: pre-wrap; word-break: break-all; font-family: monospace; font-size: 12px; line-height: 1.6; padding: 12px; border-radius: 8px; background: #f5f7fa; color: #303133;">
                    {{ item.requestBodyJson || '-' }}
                  </div>
                </div>
              </div>
            </el-card>
          </div>
        </template>

        <template v-if="eventSourcePreviewResult">
          <el-descriptions :column="2" border>
            <el-descriptions-item :label="text.previewedAt">{{ eventSourcePreviewResult.previewedAt || '-' }}</el-descriptions-item>
            <el-descriptions-item :label="text.previewSampleCount">{{ eventSourcePreviewResult.itemCount ?? 0 }}</el-descriptions-item>
          </el-descriptions>

          <el-table :data="eventSourcePreviewResult.items || []" border max-height="360">
            <el-table-column prop="targetCode" :label="text.targetCode" min-width="120" />
            <el-table-column prop="targetName" :label="text.targetName" min-width="140" />
            <el-table-column prop="eventType" :label="text.eventTypes" width="140" />
            <el-table-column prop="eventTitle" :label="text.eventTitle" min-width="220" show-overflow-tooltip />
            <el-table-column prop="impactLevel" :label="text.impactLevels" width="120" />
            <el-table-column prop="sourceChannel" :label="text.sourceChannel" min-width="140" />
            <el-table-column prop="occurredAt" :label="text.occurredAt" min-width="160" />
          </el-table>
        </template>
      </div>

      <template #footer>
        <div style="display: flex; justify-content: flex-end;">
          <el-button @click="eventSourcePreviewDialogVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
