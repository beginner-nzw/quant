# Host Ownership

本文件定义当前阶段逻辑应该落在哪个宿主上。

## Ownership Rules

1. 创建任务入口属于 `research-task-service`。
2. AI 执行属于 `quant-ai-engine`。
3. AI 回传消费、状态落库、业务投影当前属于 `ai-orchestration-service`。
4. 多业务聚合查询当前可以留在 `ai-orchestration-service`，但只是 transition host。
5. 前端不得承载业务真值逻辑。

## Current Hosts

### research-task-service

Formal host for:

- create research task
- task routing support for create request
- Redis duplicate/hot target protection for create
- task dispatch outbox creation
- task dispatch outbox publisher

Not host for:

- task read-model aggregation
- risk/report/strategy projection
- model/agent/workflow configuration
- AI workflow execution

### ai-orchestration-service

Formal host for current phase:

- consume `ai.task.status`
- consume `ai.task.result`
- consume `ai.task.audit`
- task retry and cancel control
- task runtime read-model
- task trace read-model
- AI result projection into report/risk/strategy/evidence
- message idempotency log

Transition host for current phase:

- report center query and review
- market event management
- market intelligence query
- risk warning query
- strategy signal query
- audit compliance query
- model / agent / workflow / role access config UI API
- research workbench aggregation

These transition host responsibilities must not expand without updating `05-transition-lifetime.md`.

### quant-ai-engine

Formal host for:

- Kafka dispatch consumption
- LangGraph workflow execution
- per-node status emission
- AI result message emission
- AI audit message emission
- model / LangChain invocation
- rule fallback during AI execution

Not host for:

- business final SoT
- report/risk/strategy final ownership
- user permission truth
- frontend contract definition

### quant-ui

Formal host for:

- route navigation
- UI state
- forms
- visual display
- local selected user role for demo/runtime headers

Not host for:

- task status truth
- report truth
- risk warning truth
- strategy signal truth
- market event truth
- audit truth

## Current Ownership Hotspots

### TaskQueryController

Current issue:

It exposes task, market event, risk, strategy, report, audit, model config, role config, workbench and report review endpoints in one controller.

Target:

Short term: split by controller classes inside the same service.

Medium term: split query service boundaries.

Long term: decide whether to extract independent domain services.

### TaskQueryServiceImpl

Current issue:

It is a large aggregation and read-model service with mixed responsibilities.

Target:

Short term: classify methods into task, report, risk, strategy, market, workbench, audit sections.

Medium term: extract internal service classes under `ai-orchestration-service`.

Long term: evaluate microservice split after contract stabilization.

### AiResultDomainProjectionService

Current issue:

It projects AI result into risk, strategy, evidence and report sections.

Target:

Keep as projection host for now, but define projection rules in `04-contract-map.md`. It must not become a general query or business command host.

