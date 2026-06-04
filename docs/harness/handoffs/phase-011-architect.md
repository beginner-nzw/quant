# Phase 011 Architect Handoff

## Status

Window: Window 1 - Phase Architect.

Phase: Phase 011 - Risk/Strategy Projection Ownership Boundary.

This handoff is architecture planning only. It does not authorize implementation. Window 2 may start only after the user explicitly approves this file.

## Inputs Read

Required harness files:

- `docs/harness/00-project-charter.md`
- `docs/harness/01-current-architecture.md`
- `docs/harness/02-authority-matrix.md`
- `docs/harness/03-host-ownership.md`
- `docs/harness/04-contract-map.md`
- `docs/harness/05-transition-lifetime.md`
- `docs/harness/06-debt-register.md`
- `docs/harness/07-phase-backlog.md`
- `docs/harness/08-eval-checklist.md`
- `docs/harness/09-window-protocol.md`
- `docs/harness/10-steering-state-machine.md`
- `docs/harness/state/current-state.md`
- `docs/harness/handoffs/steering-decision-phase-011.md`

Additional state read because Phase 011 applies the Phase 008 template after Phase 009 and Phase 010:

- `docs/harness/12-transition-host-exit-criteria.md`
- `docs/harness/13-report-boundary-readiness.md`
- `docs/harness/14-market-data-ingest-boundary-readiness.md`
- `docs/harness/handoffs/phase-009-architect.md`
- `docs/harness/handoffs/phase-010-architect.md`

Read-only planning inspection:

- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/RiskWarningController.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/StrategySignalController.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/RiskQueryService.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/RiskQueryServiceImpl.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/StrategyQueryService.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/StrategyQueryServiceImpl.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/StrategySignalService.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/StrategySignalServiceImpl.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/AiResultDomainProjectionService.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/AiResultDomainProjectionServiceImpl.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/TaskDomainEventPublisherService.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/TaskDomainEventPublisherServiceImpl.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/consumer/AiTaskResultConsumer.java`
- risk/strategy DTO, VO, entity, mapper and test references found by `rg`
- `quant-ui/src/api/task.ts`
- `quant-ui/src/types/task.ts`
- `quant-ui/src/router/index.ts`
- `quant-ui/src/views/report/RiskWarningCenterView.vue`
- `quant-ui/src/views/report/StrategySignalCenterView.vue`
- `quant-ui/src/views/DashboardView.vue`
- `quant-ui/src/views/report/ResearchWorkbenchView.vue`
- `quant-ui/scripts/authority-boundary-check.mjs`
- `quant-ai-platform/quant-ai-engine/app/clients/backend_client.py`
- `quant-ai-platform/quant-ai-engine/app/services/market_data_service.py`
- risk/strategy context references under `quant-ai-platform/quant-ai-engine/app/agents`, `app/services` and `app/messaging`

Phase 011 is not Phase 001, so the Phase 001 special Java reading list is not the controlling implementation scope for this handoff.

## 1. Phase Goal

Produce a docs-only risk/strategy projection ownership boundary readiness artifact that applies the Phase 008 readiness template to risk warning and strategy signal responsibilities.

The bounded goal is:

- Clarify risk/strategy belongs, authority, contract and behavior boundaries before any later risk-service extraction, strategy-service extraction, projection split, route migration, Kafka downstream event redesign, frontend/Python reshaping, database change or permanent architecture decision.
- Inventory risk warning facts, risk warning details, strategy signal facts, strategy signal factors, AI result projection, generated domain-event publication, strategy manual create command, strategy status command, frontend consumers and Python execution context.
- State that `AiResultDomainProjectionService` is retained as the current shared projection dependency for this bounded horizon. Phase 011 must document it only; it must not split, move, rename or redesign it.
- State that `TaskDomainEventPublisherService` is a current downstream publication dependency after projection. Phase 011 may document existing `risk.warning.generated` and `strategy.signal.generated` publication behavior, but must not activate a new consumer, change payloads or redesign downstream topics.
- Preserve Phase 005 modular-monolith horizon policy, Phase 006 legacy `/api/tasks/*` contract freeze, Phase 007 frontend authority guardrails, Phase 008 transition-host inventory, Phase 009 report readiness gates and Phase 010 market/data-ingest readiness gates.
- Define risk/strategy-specific blockers, readiness gates and stop rules for later phases without choosing or implementing extraction.

Window 2 must evaluate every statement in this order:

```text
belongs -> authority -> contract -> behavior
```

This phase is docs-only by default. Existing Phase 006 backend contract tests already guard the current risk and strategy endpoint inventory. Window 2 must document existing guards and future guard needs, not add code or tests in this phase.

## 2. Belongs

Current belongs baseline:

- `ai-orchestration-service` currently hosts risk warning facts, risk warning details, risk read models, strategy signal facts, strategy signal factors, strategy read models and current strategy commands as transition-host responsibilities.
- `risk_warning` and `risk_warning_detail` persistence currently live in `ai-orchestration-service` and remain the current risk fact boundary for this phase.
- `strategy_signal` and `strategy_signal_factor` persistence currently live in `ai-orchestration-service` and remain the current strategy fact boundary for this phase.
- `AiResultDomainProjectionService` currently projects successful `ai.task.result` payloads into report/evidence/risk/strategy records. It is in scope as a current dependency to document only.
- `TaskDomainEventPublisherService` currently publishes generated report/risk/strategy events after projection. It is in scope as a current dependency to document only.
- `RiskQueryService` and `StrategyQueryService` are current read-model services. They may hydrate display rows with report, task, risk and strategy context, but they must not become command or projection authority beyond current behavior.
- `StrategySignalService` is the current manual strategy command handler for create/update and status update. It remains in the transition host for this bounded horizon.
- `quant-ai-engine` remains the AI execution producer and Python context consumer. Python may read risk/strategy lists for AI context and may produce risk output/provenance, but Python does not own final risk or strategy business facts.
- `quant-ui` remains a contract consumer and display host. It may display risk and strategy read models and initiate existing strategy commands, but it must not infer risk/strategy truth outside backend contracts.
- `research-task-service` remains the formal host for task creation. Existing follow-up task creation or source-context prefill from risk/strategy screens is task-create input convenience only, not risk/strategy authority.

In-scope risk/strategy surfaces:

- risk warning list and stats read models
- strategy signal list and stats read models
- strategy signal factor query
- strategy signal manual create/update command
- strategy signal status command
- AI result projection into `risk_warning`, `risk_warning_detail`, `strategy_signal` and `strategy_signal_factor`
- current generated domain-event publication to `risk.warning.generated` and `strategy.signal.generated`
- current Redis strategy signal cache behavior as context only
- frontend risk warning center, strategy signal center and dashboard stats consumers
- workbench, report and market/intelligence displays that show risk or strategy fields as display context
- Python backend client and market context reads for risk/strategy execution context

Context-only dependencies:

- report fields that display risk points, report review status, confidence or strategy summary
- market event and market intelligence context that may influence risk or strategy displays
- `GET /api/tasks/research-workbench` as display-only aggregation
- `GET /api/tasks/{taskId}/full` as task detail composition that may include report/risk/strategy context
- model, agent, workflow, prompt, event-source and role-access config APIs
- header-based demo auth and JSON role access config
- task message log, audit records and Kafka idempotency as current audit/message context

Explicitly excluded:

- risk-service extraction
- strategy-service extraction
- projection service split, move, rename or redesign
- route migration, aliases, endpoint rename, endpoint deletion or endpoint consolidation
- downstream topic redesign, new consumers or Kafka payload changes
- gateway/auth/JWT work
- config-store migration from JSON files
- database schema, entity, mapper, DTO or VO reshaping
- Redis key or cache behavior changes
- Python risk workflow, fallback, provenance, prompt or result payload behavior changes
- frontend route/API/type/display/command reshaping
- new risk feature, new strategy feature, new product feature, new adapter or new agent

## 3. Authority

Stable risk authority objects for the current phase:

- `risk_warning`
- `risk_warning_detail`

Stable strategy authority objects for the current phase:

- `strategy_signal`
- `strategy_signal_factor`

Related context facts:

- `research_report`, `research_report_section`, `report_evidence_ref`, `reportMeta`, `contextSnapshot`, report risk points and report highlights are report/projection input or display context, not risk or strategy SoT unless selected data is persisted through existing approved projection into risk/strategy authority objects.
- `market_event`, market intelligence rows, Python market context, live market events and source preview data are market/execution context, not risk or strategy SoT by themselves.
- `ai.task.result` is the current projection input contract. It is not the final risk or strategy SoT by itself.
- `risk.warning.generated` and `strategy.signal.generated` are downstream publication contracts emitted after current projection. They are not persisted risk/strategy SoT by themselves.
- `task_message_log` records message handling and remains audit/idempotency context, not risk or strategy fact authority.
- Frontend routes, filters, modal forms, local state, dashboard cards and display tags are UI state only.
- Python fallback and provenance fields are execution/audit metadata only.

Authority rules:

- `risk_warning` remains the current persisted risk warning fact root.
- `risk_warning_detail` remains the persisted risk detail authority for risk warning detail rows.
- `strategy_signal` remains the current persisted strategy signal fact root.
- `strategy_signal_factor` remains the persisted factor authority for strategy signal explanation rows.
- `AiResultDomainProjectionService` remains the current shared projection dependency for report/evidence/risk/strategy persistence.
- `TaskDomainEventPublisherService` may publish downstream messages after persisted projection facts exist; those messages do not become a second source of truth.
- Risk read-model rows may include report review, follow-up task and display hydration fields; those fields do not move report/task authority into risk.
- Strategy read-model rows may include report review, risk adjustment, follow-up task and display hydration fields; those fields do not move report/risk/task authority into strategy.
- Strategy manual create and status commands persist through `StrategySignalService` and current `strategy_signal` / `strategy_signal_factor` objects. Frontend create/edit forms do not become authority before backend persistence.
- Python `riskWarnings`, `strategySignals`, `latestRiskWarningSummary`, `latestStrategySignalSummary`, prompt context and fallback snapshots remain execution/display/provenance context.

Forbidden authority changes:

- No new risk or strategy SoT may be created.
- No read model may become command authority.
- No frontend-derived risk level, signal direction, signal status, review status, follow-up status, display tag or form state may define persisted facts.
- No report risk point, report highlight, workbench summary, market intelligence row, Python fallback snapshot or prompt context may become risk/strategy authority unless persisted by the existing approved projection/command path.
- No generated Kafka event may be documented as the authoritative risk or strategy record.
- No documentation may claim risk or strategy ownership has moved out of `ai-orchestration-service`.
- No documentation may declare `ai-orchestration-service`, `AiResultDomainProjectionService`, generated topics, Redis caches, JSON config files or legacy `/api/tasks/*` paths final architecture.

## 4. Contract

Stable risk URL/API inventory:

| Endpoint | Classification | Current owner |
| --- | --- | --- |
| `GET /api/tasks/risk-warnings` | risk warning read model | `RiskWarningController` in `ai-orchestration-service` |
| `GET /api/tasks/risk-warning-stats` | risk warning stats read model | `RiskWarningController` in `ai-orchestration-service` |

Stable strategy URL/API inventory:

| Endpoint | Classification | Current owner |
| --- | --- | --- |
| `GET /api/tasks/strategy-signals` | strategy signal read model | `StrategySignalController` in `ai-orchestration-service` |
| `GET /api/tasks/strategy-signal-stats` | strategy signal stats read model | `StrategySignalController` in `ai-orchestration-service` |
| `GET /api/tasks/strategy-signals/{signalId}/factors` | strategy factor read model | `StrategySignalController` in `ai-orchestration-service` |
| `POST /api/tasks/strategy-signals` | strategy manual create/update command | `StrategySignalController` in `ai-orchestration-service` |
| `POST /api/tasks/strategy-signals/{signalId}/status` | strategy status command | `StrategySignalController` in `ai-orchestration-service` |

Stable backend contract details:

- URL paths and HTTP methods stay unchanged.
- `RiskWarningController` remains owner for the listed risk contracts.
- `StrategySignalController` remains owner for the listed strategy contracts.
- `Result<T>` response envelopes stay unchanged.
- Request binding stays unchanged:
  - `GET /api/tasks/risk-warnings`: query object `RiskWarningPageQueryDTO`
  - `GET /api/tasks/risk-warning-stats`: no request binding
  - `GET /api/tasks/strategy-signals`: query object `StrategySignalPageQueryDTO`
  - `GET /api/tasks/strategy-signal-stats`: no request binding
  - `GET /api/tasks/strategy-signals/{signalId}/factors`: path variable `signalId`
  - `POST /api/tasks/strategy-signals`: request body `StrategySignalCreateDTO`
  - `POST /api/tasks/strategy-signals/{signalId}/status`: path variable `signalId` and request body `StrategySignalStatusUpdateDTO`
- Response types stay unchanged:
  - `Result<RiskWarningPageVO>`
  - `Result<RiskWarningStatsVO>`
  - `Result<StrategySignalPageVO>`
  - `Result<StrategySignalStatsVO>`
  - `Result<List<StrategySignalFactorItemVO>>`
  - `Result<String>` for strategy command outputs
- Current risk read-model endpoints keep their absence of explicit `requirePermission` calls.
- Current strategy read/factor endpoints keep their absence of explicit `requirePermission` calls.
- `POST /api/tasks/strategy-signals` keeps exactly `PERMISSION_REPORT_REVIEW`.
- `POST /api/tasks/strategy-signals/{signalId}/status` keeps exactly `PERMISSION_REPORT_REVIEW`.
- DTO, VO, entity, mapper, database table, Redis key, Kafka topic, Kafka payload, Python payload and TypeScript shapes stay unchanged.

Stable frontend routes and functions:

- `/risk-warnings`
- `/signals`
- `/dashboard` risk/strategy stats cards
- `/research-workbench` as display aggregation only
- `fetchRiskWarnings`
- `fetchRiskWarningStats`
- `fetchStrategySignals`
- `fetchStrategySignalStats`
- `createStrategySignal`
- `fetchStrategySignalFactors`
- `updateStrategySignalStatus`

Stable frontend risk/strategy types include:

- `RiskWarningStats`
- `RiskWarningListItem`
- `RiskWarningPageData`
- `StrategySignalStats`
- `StrategySignalListItem`
- `StrategySignalPageData`
- `StrategySignalFactorItem`
- `StrategySignalCreateFactorForm`
- `StrategySignalCreateForm`

Stable Kafka/Python contracts:

- `ai.task.result` remains the Java projection input.
- `risk.warning.generated` and `strategy.signal.generated` remain current generated domain-event topics. Their topic names, payloads and publication behavior must not change in Phase 011.
- Python backend client paths for `list_risk_warnings` and `list_strategy_signals` keep `/api/tasks/risk-warnings` and `/api/tasks/strategy-signals`.
- Python `MarketDataService` risk/strategy context fields stay within existing execution/provenance surfaces.

## 5. Allowed File Scope

Window 2 may modify only Phase 011 documentation files.

Required output files:

- `docs/harness/15-risk-strategy-projection-boundary-readiness.md`
- `docs/harness/handoffs/phase-011-implementation.md`

Optional output file, only if the risk/strategy inventory becomes too large for the primary document:

- `docs/harness/handoffs/phase-011-risk-strategy-inventory.md`

Allowed read-only inspection areas:

- risk/strategy-related Java controller/service/projection/publisher/consumer/entity/mapper/DTO/VO/test files under `ai-orchestration-service`
- common Kafka topic/message constants under `quant-ai-platform/quant-services/quant-common/**`
- risk/strategy-related frontend API/type/router/view/component/utility files under `quant-ui/src`
- `quant-ui/scripts/authority-boundary-check.mjs`
- risk/strategy context and fallback-related Python files under `quant-ai-platform/quant-ai-engine/app`
- existing harness docs and previous phase handoffs

Window 2 must not write to those read-only inspection areas.

## 6. Forbidden File Scope

Window 2 must not modify:

- any Java production or test file under `quant-ai-platform/quant-services/**`
- any Python file under `quant-ai-platform/quant-ai-engine/**`
- any frontend file under `quant-ui/**`
- `quant-ai-platform/ai-config/**`
- database migration, schema, SQL, seed or mapper files
- Redis key constants or cache behavior files
- Kafka topic constants, producers, consumers, message DTOs or listener code
- Maven, npm, Vite, TypeScript, Docker, deployment, gateway, Nacos, Sentinel or service-discovery files
- dependency or lock files
- `docs/harness/state/current-state.md`
- `docs/harness/02-authority-matrix.md`
- `docs/harness/03-host-ownership.md`
- `docs/harness/04-contract-map.md`
- `docs/harness/05-transition-lifetime.md`
- `docs/harness/06-debt-register.md`
- `docs/harness/07-phase-backlog.md`
- `docs/harness/08-eval-checklist.md`
- `docs/harness/09-window-protocol.md`
- `docs/harness/10-steering-state-machine.md`
- `docs/harness/12-transition-host-exit-criteria.md`
- `docs/harness/13-report-boundary-readiness.md`
- `docs/harness/14-market-data-ingest-boundary-readiness.md`
- prior phase handoffs

Window 4 may later update state, debt, backlog or transition lifetime after Window 3 approval. Window 2 must not do that in Phase 011.

If satisfying Phase 011 appears to require any forbidden file, Window 2 must stop as blocked.

## 7. Must Stay Stable

Stable URL / API / behavior:

- Every risk and strategy endpoint listed in this handoff keeps the same path, method, controller owner, binding, response envelope, response type and permission behavior.
- No risk URL moves to `/api/risk`, `/api/risk-warnings`, `/api/risk-service` or any other new namespace.
- No strategy URL moves to `/api/strategy`, `/api/signals`, `/api/strategy-signals`, `/api/strategy-service` or any other new namespace.
- No route alias, compatibility endpoint, gateway proxy, bridge or wrapper is added.
- No endpoint is deleted, renamed, consolidated or split.
- No frontend route, API function name, endpoint string, call signature or TypeScript shape changes.
- No risk warning projection, risk query, risk stats, strategy query, strategy stats, strategy factor query, strategy create/update, strategy status, generated-event publication, cache, permission, audit/message-log, Kafka or display behavior changes.
- No database table, entity, mapper, DTO, VO, Redis key, Kafka topic, Kafka payload, Python payload or JSON config changes.
- No Python risk fallback, strategy context, backend overlay, prompt context, report generation, evidence collection or Kafka result payload behavior changes.

Stable architecture:

- `ai-orchestration-service` remains the risk/strategy transition host for this bounded horizon, not final risk or strategy architecture.
- `AiResultDomainProjectionService` remains the current shared projection dependency, not final architecture.
- `TaskDomainEventPublisherService` remains the current generated-event publisher dependency, not a downstream service boundary decision.
- Legacy `/api/tasks/*` remains a frozen transition namespace, not final route architecture.
- JSON config files and header-based demo auth remain transition mechanisms, not production config/auth architecture.
- Phase 011 does not close D001, D002, D003, D007 or D008.
- Phase 011 does not approve risk-service extraction, strategy-service extraction, projection split, route migration, gateway/auth, config-store migration, Python behavior change, frontend reshaping, Kafka/database changes or permanent modular-monolith architecture.

## 8. Allowed New Class / Method Types

No business-code classes or methods are allowed.

Allowed documentation-only structures:

- Markdown sections and tables.
- Risk/strategy boundary matrices.
- Risk endpoint inventory.
- Strategy endpoint inventory.
- Projection dependency inventory.
- Generated-domain-event dependency inventory.
- Frontend consumer inventory.
- Python risk/strategy context and fallback inventory.
- Belongs/authority/contract/behavior gate checklists.
- Deferred-decision lists.
- Future guardrail recommendations.
- Stop-rule lists.

Allowed scripts/classes/methods:

- None.

Window 2 must not add Java test classes, source files, frontend scripts, Python scripts, build steps or runtime code in this phase.

## 9. Forbidden Helpers / Adapters / Fallbacks / Bridges

Do not add or approve:

- any production `*Helper`, `*Adapter`, `*Bridge`, `*Facade`, `*Fallback`, `*Resolver`, `*Router`, `*Mapper` or compatibility layer
- any test helper or static guard script
- any risk route alias or strategy route alias
- any gateway or proxy bridge
- any frontend API adapter
- any frontend truth resolver for risk or strategy data
- any Python fallback bridge or new fallback provenance field
- any projection bridge, projection wrapper or projection compatibility layer
- any Kafka compatibility bridge, topic alias or message wrapper
- any config-store bridge
- any temporary risk-service or strategy-service wrapper
- any database migration helper
- any new audit/risk/strategy synchronization bridge

Window 2 may document existing projection code, generated-event publication, Redis cache behavior, Python fallback/provenance paths and existing guards as current facts, but must not create new paths or approve them as target architecture.

## 10. Required Risk/Strategy Readiness Artifact Shape

`docs/harness/15-risk-strategy-projection-boundary-readiness.md` must include:

- Status and scope.
- Inputs and read-only inspection sources.
- Risk/strategy belongs analysis.
- Risk authority object inventory.
- Strategy authority object inventory.
- Risk read-model surface inventory.
- Strategy read-model and command surface inventory.
- AI result projection dependency section.
- Generated domain-event publication section for `risk.warning.generated` and `strategy.signal.generated`.
- Strategy manual command and status command boundary section.
- Risk/strategy read-model hydration and report/workbench/market display boundary section.
- Frontend risk/strategy consumer section.
- Python risk/strategy context and fallback provenance section.
- Stable URL/API contract table.
- Current guardrails inherited from Phase 004, Phase 005, Phase 006, Phase 007, Phase 008, Phase 009 and Phase 010.
- Extraction, route-migration, projection-split, Kafka, auth/gateway and config-store blockers.
- Risk/strategy-specific readiness gates before any future extraction, route migration, projection split, Kafka redesign or permanence decision.
- Deferred decisions.
- Stop rules for later phases.

The artifact must explicitly state that it does not implement or approve extraction, projection split, route migration, endpoint aliases, DTO/VO/entity/schema changes, Redis changes, Kafka changes, frontend changes, Python changes, config-store migration, gateway/auth work, business behavior changes or new feature work.

## 11. Acceptance Conditions

Phase 011 is acceptable only if all conditions hold:

- `docs/harness/15-risk-strategy-projection-boundary-readiness.md` exists and is the primary durable risk/strategy projection ownership readiness artifact.
- `docs/harness/handoffs/phase-011-implementation.md` records exact files changed and verification outcomes.
- The readiness artifact covers risk warning facts, risk warning details, risk read models, strategy signal facts, strategy signal factors, strategy read models, strategy manual create/update command, strategy status command, AI result projection, generated domain-event publication, frontend consumers and Python context consumers.
- The artifact names the current risk authority objects: `risk_warning` and `risk_warning_detail`.
- The artifact names the current strategy authority objects: `strategy_signal` and `strategy_signal_factor`.
- The artifact states that report risk points, report highlights, workbench summaries, market intelligence rows, Python risk/strategy context, fallback provenance, generated Kafka messages, frontend local state and dashboard cards are not risk/strategy SoT unless selected data is persisted through existing approved projection or command paths.
- The artifact treats `AiResultDomainProjectionService` as a current shared projection dependency, not as a moved, split or redesigned owner.
- The artifact treats `TaskDomainEventPublisherService`, `risk.warning.generated` and `strategy.signal.generated` as current generated-event dependencies, not as redesigned downstream architecture.
- The artifact preserves all risk/strategy URLs, methods, bindings, response envelopes, response types, permission behavior, frontend routes, frontend API functions and TypeScript shapes.
- The artifact preserves Phase 005, Phase 006, Phase 007, Phase 008, Phase 009 and Phase 010 constraints.
- The artifact does not choose risk-service extraction, strategy-service extraction, projection splitting, route migration, route aliases, endpoint deletion/rename/consolidation, gateway/auth, config-store migration, Python behavior change, frontend reshaping, Kafka/database changes, permanent modular-monolith architecture or new feature work.
- The artifact defines risk/strategy-specific readiness gates for any later extraction, route migration, projection split, Kafka downstream change or permanence decision.
- No Java, Python, frontend, database, Redis, Kafka, `ai-config`, dependency, build-config, deployment, state, debt, backlog, transition lifetime, durable Phase 008/009/010 artifacts or prior handoff file changes occur.
- Any future-phase recommendation is explicitly labeled as deferred and requiring a later Window 0 decision plus human approval.
- `git diff --name-only` shows only allowed Phase 011 documentation files as Window 2 changes, aside from pre-existing unrelated dirty files that are clearly excluded from the Window 2 claim.

## 12. Required Verification Commands

Window 2 must run from `D:\projects\bussiness`:

```powershell
git diff --name-only
```

Expected result: only allowed Phase 011 docs should be part of the Window 2 change claim. Existing unrelated dirty files must not be reverted or bundled into the Phase 011 implementation claim.

Window 2 must run:

```powershell
Test-Path docs/harness/15-risk-strategy-projection-boundary-readiness.md
```

Expected result: `True`.

Window 2 must run:

```powershell
rg -n "risk_warning|risk_warning_detail|strategy_signal|strategy_signal_factor|RiskWarningController|StrategySignalController|RiskQueryService|StrategyQueryService|StrategySignalService|AiResultDomainProjectionService|TaskDomainEventPublisherService|risk.warning.generated|strategy.signal.generated|frontend|Python|fallback|readiness gate|legacy /api/tasks|Phase 005|Phase 006|Phase 007|Phase 008|Phase 009|Phase 010" docs/harness/15-risk-strategy-projection-boundary-readiness.md docs/harness/handoffs/phase-011-implementation.md
```

Expected result: the risk/strategy readiness artifact and implementation handoff contain the required domain coverage and inherited guardrail references.

Window 2 must run:

```powershell
rg -n "service extraction|projection split|route migration|route alias|breaking change|gateway/auth|config-store|database schema|Redis|Kafka|frontend reshaping|Python behavior|business code|new feature|permanent modular" docs/harness/15-risk-strategy-projection-boundary-readiness.md docs/harness/handoffs/phase-011-implementation.md
```

Expected result: any matches are in out-of-scope, blocker, deferred-decision, prerequisite or future-phase sections, not in completed implementation claims.

Window 2 must run or record these read-only inventory checks from `D:\projects\bussiness`:

```powershell
rg -n "@RequestMapping|@GetMapping|@PostMapping|requirePermission|risk-warnings|risk-warning-stats|strategy-signals|strategy-signal-stats" quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/RiskWarningController.java quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/StrategySignalController.java
```

```powershell
rg -n "RiskWarningController|StrategySignalController|/api/tasks/risk|/api/tasks/strategy|RiskWarningPageVO|RiskWarningStatsVO|StrategySignalPageVO|StrategySignalStatsVO|StrategySignalFactorItemVO|StrategySignalCreateDTO|StrategySignalStatusUpdateDTO|PERMISSION_REPORT_REVIEW" quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/TaskControllerMappingTest.java
```

```powershell
rg -n "pageRiskWarnings|getRiskWarningStats|pageStrategySignals|getStrategySignalStats|listStrategySignalFactors|createOrUpdate|updateStatus|saveRiskWarning|saveStrategySignal|publishRiskWarningGenerated|publishStrategySignalGenerated|risk_warning|strategy_signal" quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl
```

```powershell
rg -n "RISK_WARNING_GENERATED|STRATEGY_SIGNAL_GENERATED|risk.warning.generated|strategy.signal.generated" quant-ai-platform/quant-services/quant-common quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java
```

```powershell
rg -n "fetchRiskWarnings|fetchRiskWarningStats|fetchStrategySignals|fetchStrategySignalStats|createStrategySignal|fetchStrategySignalFactors|updateStrategySignalStatus|/risk-warnings|/signals|RiskWarning|StrategySignal" quant-ui/src/api/task.ts quant-ui/src/types/task.ts quant-ui/src/router/index.ts quant-ui/src/views/report/RiskWarningCenterView.vue quant-ui/src/views/report/StrategySignalCenterView.vue quant-ui/src/views/DashboardView.vue quant-ui/src/views/report/ResearchWorkbenchView.vue quant-ui/src/components/report/RiskWarningStatsCards.vue quant-ui/src/components/report/StrategySignalStatsCards.vue
```

```powershell
rg -n "list_risk_warnings|list_strategy_signals|riskWarnings|strategySignals|latestRiskWarningSummary|latestStrategySignalSummary|risk_review_agent|fallback|strategySignal" quant-ai-platform/quant-ai-engine/app/clients/backend_client.py quant-ai-platform/quant-ai-engine/app/services/market_data_service.py quant-ai-platform/quant-ai-engine/app/agents quant-ai-platform/quant-ai-engine/app/services quant-ai-platform/quant-ai-engine/app/messaging
```

Window 2 must run from `D:\projects\bussiness\quant-ui`:

```powershell
node scripts/authority-boundary-check.mjs
```

Expected result: the existing Phase 007 static guard still passes. If it fails, Window 2 must record the failure as a blocker and must not patch frontend code in this phase.

Maven, npm build and Python runtime verification are not required because Phase 011 forbids Java, frontend, Python and test-code changes. If Window 2 touches any such area, it must stop as blocked instead of validating with builds.

## 13. Blocker Stop Rules

Window 2 must stop and write a blocker in `docs/harness/handoffs/phase-011-implementation.md` if any of these become necessary:

- Changing any URL path, HTTP method, endpoint owner, request binding, response envelope, response type, frontend route, frontend API function, TypeScript shape or permission behavior.
- Adding a route alias, compatibility bridge, gateway proxy, frontend API adapter, projection wrapper, service wrapper, Kafka compatibility bridge or generated-event adapter.
- Moving risk, strategy, projection or generated-event publication code from `ai-orchestration-service` into another service.
- Splitting, moving, redesigning or renaming `AiResultDomainProjectionService`.
- Creating or modifying Java, Python, frontend, database, Redis, Kafka, config, dependency, test or deployment files.
- Changing DTO, VO, entity, mapper, schema, topic, payload, Redis key, JSON config or API type shapes.
- Reclassifying report risk points, report highlights, workbench summaries, market intelligence rows, generated Kafka messages, frontend display/form state, Python context or fallback provenance as risk/strategy authority.
- Treating `risk.warning.generated` or `strategy.signal.generated` as a replacement source of truth.
- Declaring `ai-orchestration-service`, `AiResultDomainProjectionService`, generated topics, Redis caches, JSON config files or legacy `/api/tasks/*` paths final architecture.
- Closing D001, D002, D003, D007 or D008.
- Selecting risk-service extraction, strategy-service extraction, projection split, route migration, gateway/auth, config-store migration, Python behavior change, frontend reshaping, Kafka/database change or permanent modular-monolith outcome.
- Needing code behavior changes to make the risk/strategy readiness artifact true.
- Finding that risk/strategy authority cannot be described without changing the approved Phase 011 scope.
- Needing human approval for breaking changes, service extraction, route migration, projection split, Kafka redesign, config-store migration, gateway/auth implementation or new product features.
- The existing `node scripts/authority-boundary-check.mjs` guard fails and cannot be treated as an environment-only issue.

When blocked, Window 2 must:

1. Stop edits at the smallest reversible point.
2. Record the exact file, route, domain object, consumer, dependency or decision point that forced the blocker.
3. State which acceptance condition cannot be met.
4. State the smallest future phase or human decision required to unblock.
5. Ask for human decision instead of expanding scope.

## Window 2 Shape

Use one docs-only architecture/governance Window 2 implementation window.

Do not partition into backend, frontend or Python implementers. Do not start service extraction, projection split, route migration, gateway/auth work, config migration, Kafka redesign, test implementation, frontend guard edits, Python edits or product feature work. Do not proceed until the user approves this Phase 011 architect handoff.

## Human Approval Request

Please approve this Phase 011 architect handoff before Window 2 starts.

Default approval meaning:

- No breaking changes.
- Keep all URL paths and frontend routes stable.
- No business behavior change.
- No new feature work.
- Window 2 may perform docs-only risk/strategy projection ownership boundary readiness work inside the allowed file boundaries above.
