# Current Architecture

## Baseline

根据当前代码，项目实际架构已经从原计划的完整微服务架构收缩为：

```text
quant-ui
  -> research-task-service
  -> ai-orchestration-service
  -> Kafka / Redis / MySQL
  -> quant-ai-engine
```

这不是最终目标，但这是当前真实系统边界。后续治理必须先承认这个现实。

## Implemented Runtime Shape

### quant-ui

Vue 3 + Element Plus 前端，已经实现多业务页面：

- Dashboard
- 任务中心
- 任务创建
- 任务详情
- 任务报告
- 市场事件中心
- 市场情报中心
- 投研工作台
- 策略信号中心
- 风险预警中心
- 报告中心
- 审计合规中心
- 模型与 Agent 配置中心
- 报告审核工作台

当前前端是 consumer。它不能定义任务状态、报告、风险、策略、市场事件或审计事实。

### research-task-service

当前正式职责：

- 接收研究任务创建请求。
- 解析 taskType / analysisScope。
- 创建 research_task。
- 写任务初始状态缓存。
- 通过 outbox 产生 ai.task.dispatch。
- 负责创建任务入口的热点限流、分布式锁和基础权限检查。

当前不应承担：

- AI 工作流执行。
- 报告、风险、策略、审计领域投影。
- 前端综合读模型。

### ai-orchestration-service

当前实际承担了多个角色：

- AI 状态回传消费。
- AI 结果回传消费。
- AI 审计回传消费。
- 任务查询 read-model。
- 任务重试和取消控制。
- 报告查询、审核和版本。
- 市场事件管理、导入、预览和 mock ingest。
- 风险预警查询。
- 策略信号查询和状态维护。
- 市场情报查询。
- 审计合规查询。
- 模型、Agent、Workflow、RoleAccess 配置管理。
- 领域投影：AI 结果投影为 risk_warning、strategy_signal、report_evidence_ref、research_report_section。

这已经超出原计划中单一 ai-orchestration-service 的职责。当前允许它作为 transition host，但必须登记生命周期，不能继续无限吸附新业务。

### quant-ai-engine

Python + LangGraph AI 执行引擎。

当前实现节点：

- planner_agent
- intent_agent
- evidence_collection_agent
- financial_analysis_agent
- risk_review_agent
- report_generation_agent

当前工作流是配置驱动的线性 LangGraph 链路。

当前不具备完整计划中的：

- 独立 event_extraction_agent
- 独立 industry_research_agent
- 独立 strategy_reasoning_agent
- 独立 audit/compliance_agent
- 条件边
- 并行执行
- 人工复核节点
- checkpoint 恢复
- 打回重跑

### Middleware

已存在：

- MySQL
- Redis
- Kafka
- Zookeeper

未完整落地：

- Gateway
- Nacos 容器与真实配置中心闭环
- Sentinel Dashboard
- 统一观测平台
- AI worker 集群编排

## Original Plan Not Yet Realized

原计划中的以下独立服务尚未实现：

- quant-gateway
- auth-service
- user-service
- market-event-service
- strategy-service
- risk-service
- report-service
- audit-service
- subscription-service
- data-ingest-service
- config-service
- dashboard-metric-job
- cache-refresh-job
- retry-compensation-job

## Current Architectural Risk

最大风险不是“功能不够多”，而是：

```text
ai-orchestration-service 已经成为多业务域事实、读模型、配置、投影和控制的混合宿主。
```

如果继续在这里扩功能，后续会越来越难判断：

- 哪个对象是 SoT。
- 哪个接口只是 read-model。
- 哪个 service 是正式宿主。
- 哪个 fallback 只是展示兜底。
- 哪个 bridge 应该退役。

