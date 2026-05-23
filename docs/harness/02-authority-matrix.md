# Authority Matrix

本文件定义当前阶段各类语义的 authoritative object。

## Rules

1. 一个语义只能有一个权威来源。
2. read-model 可以存在，但不得升级为 SoT。
3. 聚合视图可以展示多域信息，但不得反向定义业务事实。
4. 前端只能消费 contract，不能合并真值。
5. Python AI Engine 是执行者和结果生产者，不拥有业务最终事实。

## Matrix

| Semantic | Authoritative Object | Current Host | Read Model / Projection | Non-Authoritative Surfaces |
| --- | --- | --- | --- | --- |
| 任务创建事实 | `research_task` | `research-task-service` | `/api/tasks`, `/api/tasks/{taskId}` | 前端表单缓存、Python init state |
| 任务运行态 | `research_task.status/current_stage` + `ai_workflow_instance` + `ai_agent_execution` | `ai-orchestration-service` consumers | `/api/tasks/{taskId}/state`, `/api/tasks/{taskId}/workflow`, `/api/tasks/{taskId}/agents` | `research-workbench`, 前端派生状态 |
| AI 任务派发事实 | `task_outbox_message` + Kafka `ai.task.dispatch` | `research-task-service` | task message log | 手写 Kafka 直接发送 |
| AI 执行轨迹 | `ai_agent_execution`, `audit_record`, `ai_prompt_audit`, Kafka `ai.task.audit` | `ai-orchestration-service`, `quant-ai-engine` | `/api/tasks/{taskId}/agents`, `/api/tasks/{taskId}/audits` | 报告 summary、前端时间线拼接 |
| 报告事实 | `research_report`, `research_report_version`, `research_report_section` | `ai-orchestration-service` transition host | `/api/tasks/{taskId}/report`, `/api/tasks/report-center` | `research-workbench.latestInsight` |
| 报告证据 | `report_evidence_ref`, `research_report_section` | `ai-orchestration-service` transition host | report evidence view | `reportMeta.evidenceRefs` after projection |
| 风险预警事实 | `risk_warning`, `risk_warning_detail` | `ai-orchestration-service` transition host | `/api/tasks/risk-warnings`, `/api/tasks/risk-warning-stats` | 报告 riskWarnings、workbench risk summary |
| 策略信号事实 | `strategy_signal`, `strategy_signal_factor` | `ai-orchestration-service` transition host | `/api/tasks/strategy-signals`, `/api/tasks/strategy-signal-stats` | 报告 confidence/risk points 派生展示 |
| 市场事件事实 | `market_event`, `market_event_relation`, `market_event_analysis` | `ai-orchestration-service` transition host | `/api/tasks/market-events`, `/api/tasks/market-intelligence` | mock ingest payload、Python fallback market context |
| 配置事实 | `ai-config/*.json` | `ai-orchestration-service` + `quant-ai-engine` file readers | `/api/tasks/model-agent-config` | 前端默认选项 |
| 权限事实 | `ai-config/role-access-configs.json` + request headers | `ai-orchestration-service` transition host | `/api/tasks/role-access-configs` | 前端 localStorage role display |
| 审计事实 | `audit_record`, `task_message_log`, `ai_prompt_audit`, config change audit files | `ai-orchestration-service` | `/api/tasks/audit-compliance` | 页面统计 fallback |
| 展示聚合 | none; aggregation only | `TaskQueryServiceImpl`, query services | `/api/tasks/research-workbench`, dashboard endpoints | 不得作为 SoT |

## Critical Clarifications

### research-workbench

`/api/tasks/research-workbench` 是展示聚合视图，不是任何业务事实的权威来源。

它可以展示：

- 最新报告摘要
- 风险点
- 策略信号
- 最近任务
- 市场事件

它不可以定义：

- 任务最终状态
- 风险预警事实
- 策略信号事实
- 报告事实

### Python fallback result

Python fallback result 只用于模型失败或上下文缺失时保持执行链路可结束。

它必须保留 fallback reason 或可追踪字段，不能无痕升级为业务事实。

### Frontend state

前端可以做展示态：

- loading
- empty
- error
- coarse display

前端不可以做业务真值判断：

- 合并任务状态
- 推断最终状态
- 从报告、风险、策略之间判断哪个才是权威

