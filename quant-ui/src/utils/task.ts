import {
  ANALYSIS_SCOPE,
  AUDIT_ACTION_CODE,
  AUDIT_OPERATOR_TYPE,
  AUDIT_RESULT_STATUS,
  AUDIT_STAGE,
  AUDIT_TYPE,
  MARKET_EVENT_IMPACT_LEVEL,
  MARKET_EVENT_STATUS,
  MARKET_EVENT_TYPE,
  MARKET_INTELLIGENCE_TYPE,
  REPORT_REVIEW_STATUS,
  RETRY_SOURCE,
  RETRY_STATUS,
  RISK_LEVEL,
  SIGNAL_DIRECTION,
  SIGNAL_STRENGTH,
  TASK_STAGE,
  TASK_STATUS,
  TASK_TYPE,
  type AnalysisScope,
  type AuditActionCode,
  type AuditOperatorType,
  type AuditResultStatus,
  type AuditStage,
  type AuditType,
  type MarketEventImpactLevel,
  type MarketEventStatus,
  type MarketEventType,
  type MarketIntelligenceType,
  type ReportReviewStatus,
  type RetrySource,
  type RetryStatus,
  type RiskLevel,
  type SignalDirection,
  type SignalStrength,
  type TaskStage,
  type TaskStatus,
  type TaskType
} from '../types/taskEnums'

export function getTaskStatusTagType(status?: TaskStatus | string) {
  switch (status) {
    case TASK_STATUS.SUCCESS:
      return 'success'
    case TASK_STATUS.FAILED:
      return 'danger'
    case TASK_STATUS.RUNNING:
      return 'warning'
    case TASK_STATUS.DISPATCHED:
    case TASK_STATUS.CANCELLED:
      return 'info'
    default:
      return 'info'
  }
}

export function getTaskStatusText(status?: TaskStatus | string) {
  switch (status) {
    case TASK_STATUS.SUCCESS:
      return '成功'
    case TASK_STATUS.FAILED:
      return '失败'
    case TASK_STATUS.RUNNING:
      return '运行中'
    case TASK_STATUS.DISPATCHED:
      return '已派发'
    case TASK_STATUS.INIT:
      return '初始化'
    case TASK_STATUS.CANCELLED:
      return '已取消'
    default:
      return status || '-'
  }
}

export function getTaskStageText(stage?: TaskStage | string) {
  switch (stage) {
    case TASK_STAGE.INIT:
      return '初始化'
    case TASK_STAGE.DISPATCHED:
      return '已派发'
    case TASK_STAGE.RETRY_DISPATCHED:
      return '重试已派发'
    case TASK_STAGE.RECEIVED:
      return '已接收'
    case TASK_STAGE.PLANNING:
      return '规划拆解'
    case TASK_STAGE.INTENT_UNDERSTANDING:
      return '意图理解'
    case TASK_STAGE.FINANCIAL_ANALYSIS:
      return '财务分析'
    case TASK_STAGE.RISK_REVIEW:
      return '风险复核'
    case TASK_STAGE.REPORT_GENERATION:
      return '报告生成'
    case TASK_STAGE.FAILED:
      return '执行失败'
    case TASK_STAGE.TIMEOUT:
      return '执行超时'
    case TASK_STAGE.FINISHED:
      return '执行完成'
    case TASK_STAGE.CANCELLED:
      return '已取消'
    default:
      return stage || '-'
  }
}

export function getTaskTypeText(taskType?: TaskType | string) {
  switch (taskType) {
    case TASK_TYPE.STOCK_RESEARCH:
      return '股票深度研究'
    case TASK_TYPE.FOLLOW_UP_RESEARCH:
      return '跟踪研究'
    case TASK_TYPE.REPORT_REVIEW:
      return '报告复核'
    case TASK_TYPE.RISK_REVIEW:
      return '风险复核'
    case TASK_TYPE.AUDIT_REVIEW:
      return '审计复核'
    default:
      return taskType || '-'
  }
}

export function getAnalysisScopeText(scope?: AnalysisScope | string) {
  switch (scope) {
    case ANALYSIS_SCOPE.DEEP_RESEARCH:
      return '深度研究'
    case ANALYSIS_SCOPE.INTELLIGENCE_FOLLOW_UP:
      return '情报跟踪'
    case ANALYSIS_SCOPE.SIGNAL_FOLLOW_UP:
      return '信号跟踪'
    case ANALYSIS_SCOPE.REPORT_FOLLOW_UP:
      return '报告跟踪'
    case ANALYSIS_SCOPE.REPORT_REVIEW_RECHECK:
      return '报告复核'
    case ANALYSIS_SCOPE.RISK_RECHECK:
      return '风险复核'
    case ANALYSIS_SCOPE.AUDIT_RECHECK:
      return '审计复核'
    default:
      return scope || '-'
  }
}

export function getSourceDomainText(sourceDomain?: string) {
  switch (sourceDomain) {
    case 'MARKET_EVENT':
      return '市场事件中心'
    case 'MARKET_INTELLIGENCE':
      return '市场情报中心'
    case 'STRATEGY_SIGNAL':
      return '策略信号中心'
    case 'REPORT_CENTER':
      return '研究报告中心'
    case 'RISK_WARNING':
      return '风险预警中心'
    case 'AUDIT_COMPLIANCE':
      return '审计与合规中心'
    case 'REPORT_WORKBENCH':
      return '报告审核工作台'
    case 'TASK_REPORT':
      return '报告详情'
    case 'RESEARCH_WORKBENCH':
      return '投研工作台'
    case 'TASK_DETAIL':
      return '任务详情'
    default:
      return sourceDomain || '-'
  }
}

export function getPriorityTagType(priority?: string) {
  switch (priority) {
    case 'HIGH':
      return 'danger'
    case 'MEDIUM':
      return 'warning'
    case 'LOW':
      return 'info'
    default:
      return 'info'
  }
}

export function getPriorityText(priority?: string) {
  switch (priority) {
    case 'HIGH':
      return '高'
    case 'MEDIUM':
      return '中'
    case 'LOW':
      return '低'
    default:
      return priority || '-'
  }
}

export function getReviewStatusTagType(status?: ReportReviewStatus | string) {
  switch (status) {
    case REPORT_REVIEW_STATUS.APPROVED:
      return 'success'
    case REPORT_REVIEW_STATUS.REJECTED:
      return 'danger'
    case REPORT_REVIEW_STATUS.PENDING:
      return 'warning'
    default:
      return 'info'
  }
}

export function getReviewStatusText(status?: ReportReviewStatus | string) {
  switch (status) {
    case REPORT_REVIEW_STATUS.APPROVED:
      return '已通过'
    case REPORT_REVIEW_STATUS.REJECTED:
      return '已驳回'
    case REPORT_REVIEW_STATUS.PENDING:
      return '待审核'
    default:
      return status || '-'
  }
}

export function getRiskLevelTagType(level?: RiskLevel | string) {
  switch (level) {
    case RISK_LEVEL.HIGH:
      return 'danger'
    case RISK_LEVEL.MEDIUM:
      return 'warning'
    case RISK_LEVEL.LOW:
      return 'info'
    default:
      return 'info'
  }
}

export function getRiskLevelText(level?: RiskLevel | string) {
  switch (level) {
    case RISK_LEVEL.HIGH:
      return '高风险'
    case RISK_LEVEL.MEDIUM:
      return '中风险'
    case RISK_LEVEL.LOW:
      return '低风险'
    default:
      return level || '-'
  }
}

export function getSignalDirectionTagType(direction?: SignalDirection | string) {
  switch (direction) {
    case SIGNAL_DIRECTION.POSITIVE:
      return 'success'
    case SIGNAL_DIRECTION.NEGATIVE:
      return 'danger'
    case SIGNAL_DIRECTION.NEUTRAL:
      return 'warning'
    default:
      return 'info'
  }
}

export function getSignalDirectionText(direction?: SignalDirection | string) {
  switch (direction) {
    case SIGNAL_DIRECTION.POSITIVE:
      return '偏多'
    case SIGNAL_DIRECTION.NEGATIVE:
      return '偏空'
    case SIGNAL_DIRECTION.NEUTRAL:
      return '中性'
    default:
      return direction || '-'
  }
}

export function getSignalStrengthTagType(strength?: SignalStrength | string) {
  switch (strength) {
    case SIGNAL_STRENGTH.STRONG:
      return 'success'
    case SIGNAL_STRENGTH.MEDIUM:
      return 'warning'
    case SIGNAL_STRENGTH.WEAK:
      return 'info'
    default:
      return 'info'
  }
}

export function getSignalStrengthText(strength?: SignalStrength | string) {
  switch (strength) {
    case SIGNAL_STRENGTH.STRONG:
      return '强'
    case SIGNAL_STRENGTH.MEDIUM:
      return '中'
    case SIGNAL_STRENGTH.WEAK:
      return '弱'
    default:
      return strength || '-'
  }
}

export function getMarketIntelligenceTypeTagType(type?: MarketIntelligenceType | string) {
  switch (type) {
    case MARKET_INTELLIGENCE_TYPE.RISK_ALERT:
      return 'danger'
    case MARKET_INTELLIGENCE_TYPE.STRATEGY_SIGNAL:
      return 'success'
    case MARKET_INTELLIGENCE_TYPE.REPORT_INSIGHT:
      return 'info'
    default:
      return 'info'
  }
}

export function getMarketIntelligenceTypeText(type?: MarketIntelligenceType | string) {
  switch (type) {
    case MARKET_INTELLIGENCE_TYPE.RISK_ALERT:
      return '风险情报'
    case MARKET_INTELLIGENCE_TYPE.STRATEGY_SIGNAL:
      return '策略情报'
    case MARKET_INTELLIGENCE_TYPE.REPORT_INSIGHT:
      return '报告洞察'
    default:
      return type || '-'
  }
}

export function getMarketEventTypeTagType(type?: MarketEventType | string) {
  switch (type) {
    case MARKET_EVENT_TYPE.RISK_ALERT:
      return 'danger'
    case MARKET_EVENT_TYPE.POLICY:
      return 'warning'
    case MARKET_EVENT_TYPE.EARNINGS:
      return 'success'
    case MARKET_EVENT_TYPE.NEWS:
    case MARKET_EVENT_TYPE.ANNOUNCEMENT:
    case MARKET_EVENT_TYPE.OTHER:
    default:
      return 'info'
  }
}

export function getMarketEventTypeText(type?: MarketEventType | string) {
  switch (type) {
    case MARKET_EVENT_TYPE.NEWS:
      return '新闻'
    case MARKET_EVENT_TYPE.ANNOUNCEMENT:
      return '公告'
    case MARKET_EVENT_TYPE.EARNINGS:
      return '业绩'
    case MARKET_EVENT_TYPE.POLICY:
      return '政策'
    case MARKET_EVENT_TYPE.RISK_ALERT:
      return '风险预警'
    case MARKET_EVENT_TYPE.OTHER:
      return '其他'
    default:
      return type || '-'
  }
}

export function getMarketEventImpactTagType(level?: MarketEventImpactLevel | string) {
  switch (level) {
    case MARKET_EVENT_IMPACT_LEVEL.HIGH:
      return 'danger'
    case MARKET_EVENT_IMPACT_LEVEL.MEDIUM:
      return 'warning'
    case MARKET_EVENT_IMPACT_LEVEL.LOW:
      return 'info'
    default:
      return 'info'
  }
}

export function getMarketEventImpactText(level?: MarketEventImpactLevel | string) {
  switch (level) {
    case MARKET_EVENT_IMPACT_LEVEL.HIGH:
      return '高影响'
    case MARKET_EVENT_IMPACT_LEVEL.MEDIUM:
      return '中影响'
    case MARKET_EVENT_IMPACT_LEVEL.LOW:
      return '低影响'
    default:
      return level || '-'
  }
}

export function getMarketEventStatusTagType(status?: MarketEventStatus | string) {
  switch (status) {
    case MARKET_EVENT_STATUS.ACTIVE:
      return 'danger'
    case MARKET_EVENT_STATUS.RESOLVED:
      return 'success'
    case MARKET_EVENT_STATUS.IGNORED:
      return 'info'
    default:
      return 'info'
  }
}

export function getMarketEventStatusText(status?: MarketEventStatus | string) {
  switch (status) {
    case MARKET_EVENT_STATUS.ACTIVE:
      return '进行中'
    case MARKET_EVENT_STATUS.RESOLVED:
      return '已解决'
    case MARKET_EVENT_STATUS.IGNORED:
      return '已忽略'
    default:
      return status || '-'
  }
}

export function getBacktestStatusTagType(status?: string) {
  switch (status) {
    case 'NOT_READY':
      return 'info'
    default:
      return 'info'
  }
}

export function getBacktestStatusText(status?: string) {
  switch (status) {
    case 'NOT_READY':
      return '待接入'
    default:
      return status || '-'
  }
}

export function getRetrySourceText(source?: RetrySource | string) {
  switch (source) {
    case RETRY_SOURCE.MANUAL:
      return '人工重试'
    default:
      return source || '-'
  }
}

export function getRetryStatusTagType(status?: RetryStatus | string) {
  switch (status) {
    case RETRY_STATUS.DISPATCHED:
      return 'success'
    case RETRY_STATUS.SUBMITTED:
      return 'warning'
    default:
      return 'info'
  }
}

export function getRetryStatusText(status?: RetryStatus | string) {
  switch (status) {
    case RETRY_STATUS.SUBMITTED:
      return '已提交'
    case RETRY_STATUS.DISPATCHED:
      return '已派发'
    default:
      return status || '-'
  }
}

export function getAuditTypeText(type?: AuditType | string) {
  switch (type) {
    case AUDIT_TYPE.TASK_CONTROL:
      return '任务控制'
    case AUDIT_TYPE.AI_TASK_AUDIT:
      return 'AI 审计'
    default:
      return type || '-'
  }
}

export function getAuditStageText(stage?: AuditStage | string) {
  switch (stage) {
    case AUDIT_STAGE.CANCELLED:
      return '任务取消'
    case AUDIT_STAGE.WORKFLOW_FINISHED:
      return '工作流完成'
    default:
      return stage || '-'
  }
}

export function getAuditOperatorTypeText(type?: AuditOperatorType | string) {
  switch (type) {
    case AUDIT_OPERATOR_TYPE.HUMAN:
      return '人工'
    case AUDIT_OPERATOR_TYPE.AGENT:
      return 'Agent'
    default:
      return type || '-'
  }
}

export function getAuditActionText(code?: AuditActionCode | string) {
  switch (code) {
    case AUDIT_ACTION_CODE.TASK_CANCEL:
      return '取消任务'
    case AUDIT_ACTION_CODE.AUDIT_SUMMARY:
      return '生成审计摘要'
    default:
      return code || '-'
  }
}

export function getAuditResultTagType(status?: AuditResultStatus | string) {
  switch (status) {
    case AUDIT_RESULT_STATUS.SUCCESS:
      return 'success'
    case AUDIT_RESULT_STATUS.FAILED:
      return 'danger'
    default:
      return 'info'
  }
}

export function getAuditResultText(status?: AuditResultStatus | string) {
  switch (status) {
    case AUDIT_RESULT_STATUS.SUCCESS:
      return '成功'
    case AUDIT_RESULT_STATUS.FAILED:
      return '失败'
    default:
      return status || '-'
  }
}

export function getHumanReviewTagType(value?: boolean | number | null) {
  return isHumanReviewRequired(value) ? 'warning' : 'info'
}

export function getHumanReviewText(value?: boolean | number | null) {
  return isHumanReviewRequired(value) ? '是' : '否'
}

export function getBooleanTagType(value?: boolean | null, trueType = 'warning', falseType = 'info') {
  return value ? trueType : falseType
}

export function getBooleanText(value?: boolean | null, trueText = '是', falseText = '否') {
  return value ? trueText : falseText
}

function isHumanReviewRequired(value?: boolean | number | null) {
  return value === true || value === 1
}
