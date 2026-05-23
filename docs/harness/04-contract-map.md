# Contract Map

本文件定义当前阶段后端对前端和 Python AI Engine 暴露的 contract。

## API Contract Classes

### Authoritative Read Models

These endpoints are allowed to answer business truth for their domain.

| Endpoint | Semantics | Authority |
| --- | --- | --- |
| `GET /api/tasks/{taskId}` | task detail | task domain read-model |
| `GET /api/tasks/{taskId}/state` | task runtime state | task runtime read-model |
| `GET /api/tasks/{taskId}/workflow` | workflow instance | workflow trace read-model |
| `GET /api/tasks/{taskId}/agents` | agent execution trace | AI trace read-model |
| `GET /api/tasks/{taskId}/audits` | audit records | audit read-model |
| `GET /api/tasks/{taskId}/report` | task report | report read-model |
| `GET /api/tasks/{taskId}/report/versions` | report versions | report version read-model |
| `GET /api/tasks/risk-warnings` | risk warning list | risk warning read-model |
| `GET /api/tasks/strategy-signals` | strategy signal list | strategy signal read-model |
| `GET /api/tasks/market-events` | market event list | market event read-model |

### Aggregation Views

These endpoints are display aggregations. They are not SoT.

| Endpoint | Purpose | Restriction |
| --- | --- | --- |
| `GET /api/tasks/research-workbench` | target-centric workbench aggregation | must not define task/report/risk/strategy truth |
| `GET /api/tasks/market-intelligence` | market intelligence display | must not replace market_event SoT |
| `GET /api/tasks/stats` | dashboard/task stats | display only |
| `GET /api/tasks/report-center` | report center list | read-model only |
| `GET /api/tasks/audit-compliance` | audit dashboard | read-model only |
| `GET /api/tasks/model-agent-config` | config dashboard | config view only |

### Command Contracts

| Endpoint | Command | Formal Host |
| --- | --- | --- |
| `POST /api/research/tasks` | create research task | research-task-service |
| `POST /api/tasks/{taskId}/retry` | retry AI task | ai-orchestration-service |
| `POST /api/tasks/{taskId}/cancel` | cancel AI task | ai-orchestration-service |
| `POST /api/tasks/{taskId}/report/review` | review report | ai-orchestration-service transition host |
| `POST /api/tasks/market-events` | create market event | ai-orchestration-service transition host |
| `POST /api/tasks/market-events/mock-ingest` | mock ingest market events | demo/transition command |
| `POST /api/tasks/model-agent-config/*` | update AI config JSON files | transition config command |

## Kafka Contracts

| Topic | Direction | Meaning | Notes |
| --- | --- | --- | --- |
| `ai.task.dispatch` | Java -> Python | dispatch AI task | produced through task outbox |
| `ai.task.status` | Python -> Java | running progress | only RUNNING progress should update runtime state |
| `ai.task.result` | Python -> Java | terminal result | final status and report meta |
| `ai.task.audit` | Python -> Java | execution audit | audit trail |
| `market.event.standardized` | Java -> Java | standardized market event | consumed by ai-orchestration-service |
| `risk.warning.generated` | Java -> downstream | generated risk warning event | downstream not yet implemented |
| `strategy.signal.generated` | Java -> downstream | generated strategy signal event | downstream not yet implemented |
| `report.generated` | Java -> downstream | generated report event | downstream not yet implemented |
| `notification.dispatch` | Java -> downstream | notification event | no notification service yet |

## Known Contract Risks

### R1: One Controller Exposes Many Domains

`TaskQueryController` currently exposes endpoints for tasks, market events, risk, strategy, report, audit, config and workbench.

Risk:

The URL namespace `/api/tasks/*` makes many non-task domains look like task-owned surfaces.

Decision:

Short term allowed, but new endpoints must not be added here without updating this contract map.

### R2: Workbench Aggregation Can Be Mistaken For SoT

`research-workbench` reads and mixes report, risk warning, strategy signal, market event and task facts.

Risk:

Future code may consume workbench output as source truth.

Decision:

Workbench is display-only. No business command or AI final fact can depend on it as authoritative truth.

### R3: Fallback and Preferred/Fallback Merges

Some read-model code uses preferred/fallback field selection and evidence/risk merge.

Risk:

Fallback may become hidden truth routing.

Decision:

Fallback is allowed only for display hydration unless a domain-specific authority rule says otherwise.

### R4: Python Market Data Fallback

`MarketDataService` builds fallback snapshots and overlays backend data.

Risk:

Fallback market data can look like real financial data.

Decision:

Fallback snapshots must remain clearly labeled with data source / fallback reason and must not be used as market data SoT.

### R5: Config JSON Files Are Mutable Runtime Contract

Workflow, agent, model and role config are stored in JSON files and edited through API.

Risk:

Runtime behavior can change without DB migrations or versioned release discipline.

Decision:

Current phase allows JSON config, but every config update must remain audited and visible in transition lifetime.

