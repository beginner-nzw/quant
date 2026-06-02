# Config Store Decision Boundary

## Status And Scope

Phase: Phase 012 - Config Store Decision Boundary.

Artifact status: durable governance artifact for the current phase.

Implementation type: docs-only. This artifact records the current config-store boundary and the next-governance-horizon decision. It does not change runtime code, config files, prompt templates, APIs, frontend routes, Python readers, Java services, database schema, Redis, Kafka, dependencies, deployment, gateway/auth, JWT, or business behavior.

Decision order for this artifact:

```text
belongs -> authority -> contract -> behavior
```

Phase 012 does not implement or approve config-store migration, config mutation, DB adoption, Nacos adoption, hybrid adoption, service extraction, route migration, route alias, endpoint rename, endpoint deletion, endpoint consolidation, DTO/VO/entity/schema changes, frontend reshaping, Python behavior change, gateway/auth work, permanent modular-monolith architecture, business code changes, or new feature work.

## Inputs And Inspection Sources

Harness and phase inputs:

- `docs/harness/00-project-charter.md`
- `docs/harness/02-authority-matrix.md`
- `docs/harness/03-host-ownership.md`
- `docs/harness/04-contract-map.md`
- `docs/harness/05-transition-lifetime.md`
- `docs/harness/08-eval-checklist.md`
- `docs/harness/09-window-protocol.md`
- `docs/harness/state/current-state.md`
- `docs/harness/handoffs/steering-decision-phase-012.md`
- `docs/harness/handoffs/phase-012-architect.md`
- Phase 005 through Phase 011 frozen summaries and guardrails from current harness state

Read-only implementation inspection:

- `quant-ai-platform/ai-config/**`
- `quant-ai-platform/prompt-templates/**`
- `ModelAgentConfigController`
- Java config services in `ai-orchestration-service`
- `TaskRoleAccessService` and task-create permission path in `research-task-service`
- frontend config center API/types/router/view/utilities in `quant-ui`
- Python config repositories and settings files in `quant-ai-engine`
- existing backend contract tests and frontend authority guard script

## Config Belongs Analysis

| Surface | Current belongs | Boundary |
| --- | --- | --- |
| agent config | `quant-ai-platform/ai-config/agent-configs.json` | JSON transition store |
| workflow config | `quant-ai-platform/ai-config/workflow-configs.json` | JSON transition store |
| model strategy config | `quant-ai-platform/ai-config/model-strategies.json` | JSON transition store |
| prompt templates | `quant-ai-platform/prompt-templates/*.txt` | file-backed prompt transition store |
| event source config | `quant-ai-platform/ai-config/event-source-configs.json` | JSON transition store shared with market/data-ingest boundary |
| event auto-trigger config | `quant-ai-platform/ai-config/event-auto-trigger-configs.json` | JSON transition store for market auto-trigger dependency |
| role access config | `quant-ai-platform/ai-config/role-access-configs.json` | JSON transition store plus header-based demo runtime input |
| config change audit | `quant-ai-platform/ai-config/config-change-audits.json` | file-backed config audit transition path |
| event ingest history | `quant-ai-platform/ai-config/event-ingest-histories.json` | file-backed ingest history transition path |
| Java config dashboard and update APIs | `ai-orchestration-service` | transition host, not final config architecture |
| task-create role-access reader | `research-task-service` | role config reader only, not config owner |
| Python config readers | `quant-ai-engine` | execution reader only, not config owner or mutation host |
| frontend config center and role utilities | `quant-ui` | consumer/display/UI gating host, not config or permission SoT |

`ai-orchestration-service` remains the current transition host for model, agent, workflow, prompt, event-source, event-trigger, role-access, config audit and ingest-history config surfaces. This continues the Phase 005 modular-monolith horizon policy but does not make the service final architecture.

## Config Authority Object Inventory

| Authority object | Current meaning | Current store decision |
| --- | --- | --- |
| `agent-configs.json` | agent enablement, timeout, metadata and execution participation config | current JSON transition store |
| `workflow-configs.json` | workflow selection and task-type workflow config | current JSON transition store |
| `model-strategies.json` | model strategy and scenario config | current JSON transition store |
| `prompt-templates/*.txt` | prompt template text | current prompt file transition store |
| `event-source-configs.json` | event source endpoint, parser and sync config | current JSON transition store |
| `event-auto-trigger-configs.json` | market event auto-trigger rule config | current JSON transition store |
| `role-access-configs.json` | current role, menu and permission config input | current JSON transition store plus request-header demo runtime |
| `config-change-audits.json` | current config change audit records | audit trail only, not config source of truth |
| `event-ingest-histories.json` | current ingest history records | ingest history transition fact, not production ingest ledger |

Authority rules:

- JSON config files and prompt template files remain the current runtime stores after Phase 012.
- DB, Nacos or hybrid store may be considered only as a future migration target that requires a later Window 0 decision and human approval.
- Frontend defaults, localStorage role access cache, route metadata, menu gating and selected local user role do not define backend config or permission truth.
- Request headers such as `X-User-Id` and `X-User-Role` remain demo/runtime permission inputs. They are not production identity or auth source of truth.
- Python defaults, built-in fallback prompts and `settings.model` fallback values remain execution fallback/context. They do not replace file-backed config facts.
- Config read models and dashboard displays are read models, not command authority.
- Config audit rows show mutation history but do not replace the config files as current runtime authority.
- Event ingest history does not replace event source config, market event source of truth or a future production ingestion ledger.

## Prompt Template Boundary

Current prompt template files:

- `risk_review_agent_template.txt`
- `report_generation_agent_template.txt`
- `planner_agent_template.txt`
- `intent_agent_template.txt`
- `financial_analysis_agent_template.txt`

`PromptTemplateConfigServiceImpl` reads and writes prompt template text through the existing file-backed prompt-template directory and appends config audit records through `ConfigChangeAuditService`. `PromptTemplateRepository` in Python reads `prompt-templates/*.txt` and may use a supplied fallback prompt when a template is missing or empty.

Phase 012 does not mutate prompt template files, add template files, change template codes, change prompt text shape, or promote Python fallback prompts to prompt-template authority.

## Role Access And Header-Based Demo Auth Boundary

| Surface | Current role in system | Boundary |
| --- | --- | --- |
| `role-access-configs.json` | role/menu/permission config input | current JSON transition store |
| `RoleAccessConfigService` | `ai-orchestration-service` permission checker and role-access config mutator | transition host service |
| `TaskRoleAccessService` | `research-task-service` task-create permission reader | reader only |
| `SecurityUtils.currentUserRole()` | reads request role context | demo/runtime input |
| frontend `auth.ts` | selected local demo user role | UI/runtime header source only |
| frontend `requestHeaders.ts` | sends `X-User-Id` and `X-User-Role` | request context only |
| frontend `roleAccess.ts` | caches and displays role access config | consumer/display/UI gating only |

Current permission behavior remains stable:

- `GET /api/tasks/model-agent-config` requires `PERMISSION_MODEL_AGENT_CONFIG_VIEW`.
- `GET /api/tasks/role-access-configs` has no explicit `requirePermission`.
- Config update commands under `/api/tasks/model-agent-config/**` require `PERMISSION_MODEL_AGENT_CONFIG_EDIT`.
- `POST /api/research/tasks` uses `TaskRoleAccessService.PERMISSION_TASK_CREATE` in `research-task-service`.

Header-based demo auth remains a transition mechanism under Phase 012. This artifact does not approve gateway/auth implementation, JWT, real login/session behavior, role DB storage, role-service extraction, or permission behavior changes.

## Config Change Audit Boundary

`ConfigChangeAuditService` appends config-change audit entries to `config-change-audits.json`. The audit file records who changed which config type, target key, before/after data and time according to current Java behavior.

Boundary:

- The audit file is an audit trail for existing config mutation commands.
- The audit file does not become the config source of truth.
- Phase 012 does not change append behavior, retention behavior, file path resolution, audit payload shape or audit display behavior.
- Moving audit to a database, Nacos event stream, Kafka topic, external audit service or central audit architecture is deferred and requires a later approved phase.

## Event Source, Auto Trigger And Ingest History Boundary

| Surface | Current meaning | Boundary |
| --- | --- | --- |
| `event-source-configs.json` | source endpoint/parser/sync configuration | JSON transition store |
| `event-auto-trigger-configs.json` | market event auto-trigger rule configuration | JSON transition store |
| `event-ingest-histories.json` | file-backed source ingest history | transition history fact |
| market source preview/diagnose/sync | behavior depending on event source config | context dependency from Phase 010 |
| `market.event.standardized` | current Kafka context from market ingestion path | not config authority |

Phase 010 market/data-ingest readiness remains in force. Event source config, mock/demo ingest, source preview, source diagnose, CNINFO proxy and ingest history stay transition responsibilities. Phase 012 does not implement data-ingest split, market-service extraction, Kafka redesign, source adapter redesign, database schema changes, or config-store migration for market config.

## Java Config API And Service Inventory

Stable controller owner:

- `ModelAgentConfigController` with class-level `@RequestMapping("/api/tasks")`

Stable config services and implementations:

- `AgentConfigService` / `AgentConfigServiceImpl` for `agent-configs.json`
- `WorkflowConfigService` / `WorkflowConfigServiceImpl` for `workflow-configs.json`
- `ModelStrategyConfigService` / `ModelStrategyConfigServiceImpl` for `model-strategies.json`
- `PromptTemplateConfigService` / `PromptTemplateConfigServiceImpl` for `prompt-templates/*.txt`
- `EventSourceConfigService` / `EventSourceConfigServiceImpl` for `event-source-configs.json`
- `EventAutoTriggerConfigService` / `EventAutoTriggerConfigServiceImpl` for `event-auto-trigger-configs.json`
- `RoleAccessConfigService` / `RoleAccessConfigServiceImpl` for `role-access-configs.json`
- `ConfigChangeAuditService` / `ConfigChangeAuditServiceImpl` for `config-change-audits.json`
- `MarketEventIngestHistoryService` / `MarketEventIngestHistoryServiceImpl` for `event-ingest-histories.json`
- `ModelAgentConfigDashboardQueryServiceImpl` for config dashboard aggregation/read model

Current Java behavior uses existing `quant.ai.*` path properties, `Files.readString`, `Files.writeString`, path resolution helpers and audit append calls. Phase 012 records those as current facts and does not modify them.

## Research Task Service Role-Access Reader Inventory

`research-task-service` participates only as a reader of role-access config for task creation:

- `ResearchTaskController` calls `taskRoleAccessService.requirePermission(TaskRoleAccessService.PERMISSION_TASK_CREATE)` before task creation.
- `TaskRoleAccessServiceImpl` reads `role-access-configs.json` through the existing `quant.ai.role-access-config` property and fallback path candidates.
- `SecurityUtils.currentUserRole()` supplies the current role from request context.

This does not make `research-task-service` a config owner. It remains the formal host for create research task behavior, while role access config remains a JSON transition input.

## Python Config Reader Inventory

Current Python readers:

- `AgentConfigRepository` reads `agent-configs.json`.
- `WorkflowConfigRepository` reads `workflow-configs.json`.
- `ModelStrategyRepository` reads `model-strategies.json` and falls back to `settings.model` values for missing strategy fields.
- `PromptTemplateRepository` reads `prompt-templates/*.txt` and falls back to a supplied `fallback_prompt`.
- `settings.py` reads `local.yml` and optional `local.private.yml` as runtime settings.

Python remains an execution host and config reader. It does not own final business facts, config mutation, role authority, gateway/auth, or prompt-template authority. Phase 012 does not change Python reader paths, fallback prompt behavior, `settings.model` fallback behavior, AI execution behavior, local settings, payloads or provenance fields.

## Frontend Config Consumer Inventory

Stable frontend route:

- `/model-agent-config`

Stable API functions:

- `fetchModelAgentConfigCenter`
- `fetchRoleAccessConfigs`
- `updatePromptTemplate`
- `updateModelStrategy`
- `updateEventAutoTriggerRule`
- `updateEventSourceConfig`
- `updateAgentConfig`
- `updateWorkflowConfig`
- `updateRoleAccessConfig`

Stable frontend consumer files:

- `quant-ui/src/api/task.ts`
- `quant-ui/src/types/task.ts`
- `quant-ui/src/router/index.ts`
- `quant-ui/src/views/report/ModelAgentConfigCenterView.vue`
- `quant-ui/src/utils/roleAccess.ts`
- `quant-ui/src/utils/auth.ts`
- `quant-ui/src/utils/requestHeaders.ts`
- `quant-ui/src/utils/taskActionAccess.ts`
- `quant-ui/scripts/authority-boundary-check.mjs`

Frontend remains a display, UI state, local selected role and request-header consumer. Its defaults, cache, route metadata and local role utilities must not become config or permission source of truth.

## Stable URL And API Contract Table

| Endpoint | Method | Classification | Owner | Permission behavior |
| --- | --- | --- | --- | --- |
| `/api/tasks/model-agent-config` | GET | config dashboard read model | `ModelAgentConfigController` | `PERMISSION_MODEL_AGENT_CONFIG_VIEW` |
| `/api/tasks/role-access-configs` | GET | role access read model | `ModelAgentConfigController` | no explicit `requirePermission` |
| `/api/tasks/model-agent-config/prompt-templates/{templateCode}` | POST | prompt template update command | `ModelAgentConfigController` | `PERMISSION_MODEL_AGENT_CONFIG_EDIT` |
| `/api/tasks/model-agent-config/model-strategies/{strategyCode}` | POST | model strategy update command | `ModelAgentConfigController` | `PERMISSION_MODEL_AGENT_CONFIG_EDIT` |
| `/api/tasks/model-agent-config/event-auto-trigger-rules/{ruleCode}` | POST | event auto-trigger update command | `ModelAgentConfigController` | `PERMISSION_MODEL_AGENT_CONFIG_EDIT` |
| `/api/tasks/model-agent-config/event-sources/{sourceCode}` | POST | event source update command | `ModelAgentConfigController` | `PERMISSION_MODEL_AGENT_CONFIG_EDIT` |
| `/api/tasks/model-agent-config/agents/{agentCode}` | POST | agent config update command | `ModelAgentConfigController` | `PERMISSION_MODEL_AGENT_CONFIG_EDIT` |
| `/api/tasks/model-agent-config/workflows/{workflowCode}` | POST | workflow config update command | `ModelAgentConfigController` | `PERMISSION_MODEL_AGENT_CONFIG_EDIT` |
| `/api/tasks/model-agent-config/role-access/{roleCode}` | POST | role access update command | `ModelAgentConfigController` | `PERMISSION_MODEL_AGENT_CONFIG_EDIT` |

Related stable config-dependent surfaces:

- `GET /api/tasks/market-event-source-configs`
- `GET /api/tasks/market-events/ingest-history`
- existing market source sync, preview and diagnose endpoints that depend on event source config
- `POST /api/research/tasks` permission behavior that depends on `role-access-configs.json`

Contract guardrails:

- URL paths and HTTP methods stay unchanged.
- Controller owners stay unchanged.
- `Result<T>` response envelopes stay unchanged.
- Request bindings stay unchanged.
- Response types stay unchanged, including `ModelAgentConfigCenterVO`, `RoleAccessConfigItemVO` and update `Result<String>` responses.
- Existing frontend routes, API function names, endpoint strings, call signatures and TypeScript shapes stay unchanged.
- Existing Python reader file names and path behavior stay unchanged.
- Existing Java path-resolution, file read/write, audit append, validation and display-path behavior stay unchanged.

## Inherited Guardrails

Phase 005:

- Continue as a modular monolith inside `ai-orchestration-service` for the next governance horizon only.
- Do not treat modular monolith as final architecture.

Phase 006:

- Legacy non-task `/api/tasks/*` paths are frozen transitional contracts.
- Config endpoints under `/api/tasks` remain stable and guarded.

Phase 007:

- Frontend workbench and fallback provenance guardrails remain in force.
- Frontend consumer code must not become backend authority.

Phase 008:

- Transition-host exit criteria and readiness gate inventory remain in force for report, market, risk, strategy, audit, config and workbench.

Phase 009:

- Report readiness gates remain in force. Config decisions do not approve report-service extraction or report route migration.

Phase 010:

- Market/data-ingest readiness gates remain in force. Event source and ingest-history config remain transition responsibilities.

Phase 011:

- Risk/strategy readiness gates remain in force. Role access and config decisions do not approve risk-service extraction, strategy-service extraction or projection splitting.

Phase 012 preserves D001, D002, D003, D007 and D008 as open debt. It does not close them.

## Store Decision Outcome

For the next governance horizon, JSON config files and prompt template files remain the current runtime stores.

The selected Phase 012 decision is conservative:

- Current runtime store: JSON files under `quant-ai-platform/ai-config` plus prompt template files under `quant-ai-platform/prompt-templates`.
- Current mutation host: existing config update commands in `ai-orchestration-service`.
- Current readers: existing Java services, `research-task-service` role-access reader, Python config repositories and frontend consumers.
- Current audit: `config-change-audits.json`.
- Current ingest history: `event-ingest-histories.json`.
- Future DB, Nacos or hybrid target: deferred. Any selection of a target store requires later Window 0 scoring and explicit human approval.

This outcome is a no-change governance decision. It does not implement DB, Nacos, hybrid storage, config-store migration, dual-write, cache synchronization, migration runner, rollback runner, service extraction, route migration, gateway/auth, JWT, frontend reshaping, Python behavior changes, Kafka changes, Redis changes, database schema changes, or business code changes.

## Migration Blockers And Prerequisites

Before any later config-store migration can be selected or implemented, a future phase must define readiness gates for:

- single-writer authority and write conflict rules
- schema/versioning model for each config object
- audit retention and rollback requirements
- Java reader compatibility and cutover behavior
- Python reader compatibility and cutover behavior
- prompt template storage, fallback and rollout behavior
- role-access and auth ownership interaction
- event source and ingest-history ownership interaction
- deployment, environment and local-dev behavior
- data migration, validation, rollback and observability
- backward compatibility for existing `/api/tasks/*` config contracts
- security model for config mutation and read visibility

Current blockers:

- Role access still depends on header-based demo auth.
- Event source config and ingest history are tied to the Phase 010 market/data-ingest transition boundary.
- Python reads file-backed config directly.
- Frontend config center is coupled to existing `/api/tasks/model-agent-config` contracts.
- Legacy `/api/tasks/*` paths are frozen transitional contracts under Phase 006.
- No approved target store, schema, migration runner, rollback runner or dual-read/dual-write strategy exists.

## Gateway/Auth, Route, Service And Data-Ingest Dependencies

Gateway/auth:

- Any real auth service, JWT, session, gateway identity or role ownership change must be selected in a later phase.
- Phase 012 keeps `X-User-Role` and `X-User-Id` as demo/runtime request headers.

Route migration:

- Any route migration, route alias, endpoint rename, endpoint deletion, endpoint consolidation or compatibility endpoint must be selected in a later phase with contract updates and human approval.

Service extraction:

- Any config service extraction, report service extraction, market/data-ingest service extraction, risk service extraction, strategy service extraction or projection split must be selected in a later phase.

Data ingest:

- Event source config and ingest history remain under the Phase 010 transition boundary until a later data-ingest ownership decision is approved.

## Deferred Decisions

Deferred decisions requiring later Window 0 selection and human approval:

- whether DB, Nacos or hybrid config store becomes the future target
- whether prompt templates move with JSON config or use a separate store
- whether role access stays file-backed, moves to auth service, moves to DB or moves to another authority
- whether config audit moves to a shared audit service or database
- whether event ingest history remains config-adjacent or moves to a data-ingest ledger
- whether legacy `/api/tasks/*` config routes are migrated
- whether gateway/auth, JWT or production identity is introduced
- whether `ai-orchestration-service` remains modular monolith or config ownership is extracted

## Stop Rules For Later Phases

Stop and request a new decision before:

- changing config URL paths, HTTP methods, endpoint owners, request bindings, response envelopes or response types
- mutating JSON config files or prompt template files as part of governance documentation work
- changing Java config path resolution, Python config path resolution, audit append behavior, ingest history append behavior or validation behavior
- adding route aliases, compatibility bridges, gateway proxies, frontend API adapters, config-store bridges, DB adapters, Nacos adapters, migration runners, rollback runners, sync jobs or dual-write paths
- moving config, prompt-template, role-access, config-audit or ingest-history responsibility out of `ai-orchestration-service`
- moving task-create permission behavior out of `research-task-service`
- changing frontend route, API function, TypeScript shape, local role utility behavior or request-header behavior
- changing DTO, VO, entity, mapper, database schema, Redis key, Kafka topic, Kafka payload, Python payload, JSON shape or prompt-template shape
- treating frontend defaults, frontend localStorage, request headers, Python fallbacks/defaults, config audit rows or ingest history rows as replacement config authority
- declaring `ai-orchestration-service`, JSON config files, prompt templates, header-based demo auth, legacy `/api/tasks/*` paths or permanent modular architecture final
- closing D001, D002, D003, D007 or D008
- implementing config-store migration, gateway/auth, service extraction, route migration, frontend reshaping, Python behavior change, Kafka/database/Redis changes or new feature work

## Acceptance Checklist

- Belongs: config facts stay in current JSON/prompt file stores; Java/Python/frontend readers stay in their current host roles.
- Authority: no second current config SoT is introduced; JSON transition store remains current runtime authority.
- Contract: config URLs, methods, bindings, response envelopes, response types, permission behavior, frontend routes/functions/types and Python reader paths remain stable.
- Behavior: no runtime behavior changes; no business code changes; no config mutation; no migration.
- Transition lifetime: JSON config, prompt templates, header demo auth, legacy `/api/tasks/*`, mock/demo ingest and `ai-orchestration-service` remain transition facts, not final architecture.
