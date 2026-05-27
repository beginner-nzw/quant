# Risk Strategy Projection Boundary Readiness

## Status And Scope

Status: Phase 011 durable risk/strategy projection ownership boundary readiness artifact.

Scope: docs-only architecture and governance inventory for risk warning and strategy signal responsibilities inside the current `ai-orchestration-service` transition host.

This artifact applies the Phase 008 readiness template to the risk/strategy projection boundary after the Phase 009 report readiness artifact and the Phase 010 market/data-ingest readiness artifact. It clarifies belongs, authority, contract and behavior boundaries before any later risk-service extraction, strategy-service extraction, projection split, route migration, Kafka downstream event redesign, gateway/auth change, config-store migration, frontend reshaping, Python behavior change, database schema change or permanent modular monolith decision is considered.

This artifact does not implement or approve service extraction, projection split, route migration, route alias, endpoint rename, endpoint deletion, endpoint consolidation, gateway/auth work, config-store migration, database schema change, entity/DTO/VO reshaping, Redis change, Kafka topic or payload change, frontend reshaping, Python behavior change, business code change or new feature work.

Phase 005 remains the current governance-horizon policy: continue as a modular monolith inside `ai-orchestration-service`, while keeping that host transitional and not final architecture. Phase 006 remains the frozen legacy /api/tasks contract inventory. Phase 007 remains the frontend authority guardrail for workbench and fallback provenance consumers. Phase 008 remains the common transition-host exit criteria inventory. Phase 009 remains the report boundary readiness artifact. Phase 010 remains the market/data-ingest boundary readiness artifact.

## Inputs And Read-Only Inspection Sources

Harness and handoff inputs:

- `docs/harness/00-project-charter.md`
- `docs/harness/02-authority-matrix.md`
- `docs/harness/03-host-ownership.md`
- `docs/harness/04-contract-map.md`
- `docs/harness/05-transition-lifetime.md`
- `docs/harness/08-eval-checklist.md`
- `docs/harness/09-window-protocol.md`
- `docs/harness/state/current-state.md`
- `docs/harness/handoffs/steering-decision-phase-011.md`
- `docs/harness/handoffs/phase-011-architect.md`
- `docs/harness/12-transition-host-exit-criteria.md`
- `docs/harness/13-report-boundary-readiness.md`
- `docs/harness/14-market-data-ingest-boundary-readiness.md`

Read-only risk/strategy inventory sources:

- `RiskWarningController.java`
- `StrategySignalController.java`
- `RiskQueryService.java`
- `RiskQueryServiceImpl.java`
- `StrategyQueryService.java`
- `StrategyQueryServiceImpl.java`
- `StrategySignalService.java`
- `StrategySignalServiceImpl.java`
- `AiResultDomainProjectionService.java`
- `AiResultDomainProjectionServiceImpl.java`
- `TaskDomainEventPublisherService.java`
- `TaskDomainEventPublisherServiceImpl.java`
- `AiTaskResultConsumer.java`
- `LegacyTaskApiContractFreezeTest.java`
- `TaskControllerMappingTest.java`
- `TaskDomainEventPublisherServiceTests.java`
- common Kafka topic constants under `quant-ai-platform/quant-services/quant-common`
- `quant-ui/src/api/task.ts`
- `quant-ui/src/types/task.ts`
- `quant-ui/src/router/index.ts`
- `RiskWarningCenterView.vue`
- `StrategySignalCenterView.vue`
- `DashboardView.vue`
- `ResearchWorkbenchView.vue`
- `RiskWarningStatsCards.vue`
- `StrategySignalStatsCards.vue`
- `quant-ui/scripts/authority-boundary-check.mjs`
- `quant-ai-platform/quant-ai-engine/app/clients/backend_client.py`
- `quant-ai-platform/quant-ai-engine/app/services/market_data_service.py`
- risk/strategy context and fallback references under `quant-ai-platform/quant-ai-engine/app/agents`, `app/services` and `app/messaging`

Read-only inventory commands confirmed current endpoint, service, Kafka, frontend and Python facts. No Java, Python, frontend, config, database, Redis, Kafka, build, deployment or test file was modified for Phase 011.

## Risk/Strategy Belongs Analysis

Current risk warning facts, risk warning details, risk read models, strategy signal facts, strategy signal factors, strategy read models, strategy manual create/update commands and strategy status commands belong to `ai-orchestration-service` as transition-host responsibilities.

`ai-orchestration-service` currently owns the Java persistence and read-model boundary for:

- `risk_warning` and `risk_warning_detail`
- `strategy_signal` and `strategy_signal_factor`
- risk warning list and risk warning stats read models
- strategy signal list, strategy signal stats and strategy factor read models
- strategy manual create/update command handling
- strategy status command handling
- AI result projection into persisted risk and strategy records
- generated domain-event publication to `risk.warning.generated` and `strategy.signal.generated`
- current Redis strategy signal cache behavior as a strategy command implementation detail

`AiResultDomainProjectionService` is a current shared projection dependency. It writes report, evidence, risk and strategy records from `ai.task.result` payloads. Phase 011 documents that dependency only; it does not move, split, redesign or rename it.

`TaskDomainEventPublisherService` is a current generated-event publication dependency after projection. It may publish `risk.warning.generated` and `strategy.signal.generated` messages from existing projected data. Phase 011 documents those topics as current downstream publication contracts only; it does not activate new consumers, change payloads or redesign Kafka behavior.

`RiskQueryService` and `StrategyQueryService` are current read-model services. They may hydrate display rows with task, report, risk, strategy or market context, but they must not become command or projection authority beyond current behavior.

`StrategySignalService` is the current manual strategy command handler for create/update and status update. It remains in the transition host for this bounded horizon.

`quant-ai-engine` remains the AI execution producer and context consumer. Python may read risk/strategy lists for AI context and may produce risk output, report risk warnings, evidence references and fallback provenance, but Python does not own final risk or strategy business facts.

`quant-ui` remains a contract consumer and display host. The frontend may display risk and strategy read models and initiate existing strategy commands through backend contracts, but it must not infer risk/strategy truth outside those contracts.

`research-task-service` remains the formal host for task creation. Existing follow-up task creation or source-context prefill from risk/strategy screens is task-create input convenience only, not risk or strategy authority.

Phase 011 retains the current transition-host placement for another bounded horizon. It does not move risk or strategy responsibility out of `ai-orchestration-service`, does not choose a risk-service or strategy-service, and does not declare `ai-orchestration-service` final architecture.

## Risk Authority Object Inventory

Stable risk authority objects for the current phase:

| Authority object | Current meaning | Current host classification |
| --- | --- | --- |
| `risk_warning` | persisted risk warning fact root | `ai-orchestration-service` transition host |
| `risk_warning_detail` | persisted risk detail rows for warning explanation, indicators and supporting detail | `ai-orchestration-service` transition host |

Related risk context facts:

- `ai.task.result` is the current projection input contract. It is not final risk SoT by itself.
- `research_report`, report risk points, report highlights, report review status, `reportMeta` and `contextSnapshot` are report/projection input or display context unless selected data is persisted into `risk_warning` or `risk_warning_detail` through the existing approved projection path.
- `market_event`, market intelligence rows, Python market context, live market events and source preview data are market/execution context, not risk SoT by themselves.
- `risk.warning.generated` is a generated downstream publication event after projection. It is not a persisted risk authority object.
- `task_message_log`, audit records and Kafka idempotency records are audit/message context, not risk fact authority.
- Frontend filter state, dashboard cards, severity tags, modal state and route state are UI display state only.
- Python fallback and provenance fields are execution/audit metadata only.

Risk authority rules:

- `risk_warning` remains the current persisted risk warning fact root.
- `risk_warning_detail` remains the current persisted risk detail authority.
- Risk read-model rows may include report, task, market or display hydration fields. Those fields do not move report, task or market authority into risk.
- Python `riskWarnings`, `latestRiskWarningSummary`, prompt context and fallback snapshots remain execution/display/provenance context until Java projection persists selected data into approved risk authority objects.
- The generated `risk.warning.generated` Kafka message may coordinate downstream systems after projection, but it does not replace `risk_warning` or `risk_warning_detail` as SoT.

Forbidden risk authority moves:

- No new risk SoT may be created in Phase 011.
- No risk read model may become command authority.
- No frontend-derived severity, risk level, review status, follow-up state, display tag or filter state may define persisted risk facts.
- No report risk point, workbench risk summary, market intelligence row, Python fallback snapshot or prompt context may become risk authority unless persisted by the existing approved projection path.
- No generated Kafka event may be documented as the authoritative risk record.
- No documentation in this artifact claims risk ownership has moved out of `ai-orchestration-service`.

## Strategy Authority Object Inventory

Stable strategy authority objects for the current phase:

| Authority object | Current meaning | Current host classification |
| --- | --- | --- |
| `strategy_signal` | persisted strategy signal fact root, including signal direction/status and target context | `ai-orchestration-service` transition host |
| `strategy_signal_factor` | persisted factor authority for strategy signal explanations and factor details | `ai-orchestration-service` transition host |

Related strategy context facts:

- `ai.task.result` is the current projection input contract for AI-generated strategy signal data. It is not final strategy SoT by itself.
- Manual frontend forms are command input only. They become strategy authority only after backend persistence through `StrategySignalService`.
- `research_report`, report highlights, confidence score, risk points and review status are report/projection input or display context unless selected data is persisted into `strategy_signal` or `strategy_signal_factor` through existing approved projection or command paths.
- `risk_warning`, `risk_warning_detail`, risk adjustment fields and risk displays may inform strategy display or command input, but they do not become strategy SoT by themselves.
- `market_event`, market intelligence rows, Python market context, live market events and source preview data are market/execution context, not strategy SoT by themselves.
- `strategy.signal.generated` is a generated downstream publication event after projection. It is not a persisted strategy authority object.
- The current Redis cache used by strategy signal command behavior is a performance/implementation context, not strategy SoT.
- Frontend modal form state, selected signal state, status button state, dashboard cards and route state are UI state only.
- Python fallback and provenance fields are execution/audit metadata only.

Strategy authority rules:

- `strategy_signal` remains the current persisted strategy signal fact root.
- `strategy_signal_factor` remains the current persisted strategy factor authority.
- Strategy manual create/update and status commands persist through `StrategySignalService` and current strategy authority objects.
- Strategy read-model rows may include report review, task follow-up, risk adjustment or market display hydration fields. Those fields do not move report, task, risk or market authority into strategy.
- Python `strategySignals`, `latestStrategySignalSummary`, prompt context and fallback snapshots remain execution/display/provenance context until Java projection or approved command handling persists selected data into strategy authority objects.
- The generated `strategy.signal.generated` Kafka message may coordinate downstream systems after projection, but it does not replace `strategy_signal` or `strategy_signal_factor` as SoT.

Forbidden strategy authority moves:

- No new strategy SoT may be created in Phase 011.
- No strategy read model may become command authority.
- No frontend-derived direction, signal status, factor list, selected row, modal form state or display tag may define persisted strategy facts.
- No report highlight, workbench strategy summary, risk warning display, market intelligence row, Python fallback snapshot or prompt context may become strategy authority unless persisted by the existing approved projection or command path.
- No generated Kafka event may be documented as the authoritative strategy record.
- No documentation in this artifact claims strategy ownership has moved out of `ai-orchestration-service`.

## Risk Read-Model Surface Inventory

Risk read-model surfaces remain under the frozen legacy /api/tasks namespace:

| Endpoint | Response envelope/type | Binding shape | Current owner | Permission behavior | Authority note |
| --- | --- | --- | --- | --- | --- |
| `GET /api/tasks/risk-warnings` | `Result<RiskWarningPageVO>` | query object `RiskWarningPageQueryDTO` | `RiskWarningController` and `RiskQueryService` | no explicit `requirePermission` call in the current Phase 006 inventory | risk warning read model |
| `GET /api/tasks/risk-warning-stats` | `Result<RiskWarningStatsVO>` | none | `RiskWarningController` and `RiskQueryService` | no explicit `requirePermission` call in the current Phase 006 inventory | risk warning stats read model |

Current read-model notes:

- `RiskQueryServiceImpl.pageRiskWarnings` serves risk warning page data.
- `RiskQueryServiceImpl.getRiskWarningStats` serves risk warning stats.
- These read models may hydrate display fields from task/report context, but they do not own task, report, market or strategy facts.
- `GET /api/tasks/research-workbench` may display risk summaries, but it remains display-only aggregation.
- `GET /api/tasks/{taskId}/full` may include risk context inside task detail composition, not a second risk SoT.

## Strategy Read-Model And Command Surface Inventory

Strategy read-model surfaces remain under the frozen legacy /api/tasks namespace:

| Endpoint | Response envelope/type | Binding shape | Current owner | Permission behavior | Authority note |
| --- | --- | --- | --- | --- | --- |
| `GET /api/tasks/strategy-signals` | `Result<StrategySignalPageVO>` | query object `StrategySignalPageQueryDTO` | `StrategySignalController` and `StrategyQueryService` | no explicit `requirePermission` call in the current Phase 006 inventory | strategy signal read model |
| `GET /api/tasks/strategy-signal-stats` | `Result<StrategySignalStatsVO>` | none | `StrategySignalController` and `StrategyQueryService` | no explicit `requirePermission` call in the current Phase 006 inventory | strategy signal stats read model |
| `GET /api/tasks/strategy-signals/{signalId}/factors` | `Result<List<StrategySignalFactorItemVO>>` | path variable `signalId` | `StrategySignalController` and `StrategyQueryService` | no explicit `requirePermission` call in the current Phase 006 inventory | strategy factor read model |

Strategy command surfaces remain under the frozen legacy /api/tasks namespace:

| Endpoint | Response envelope/type | Binding shape | Current owner | Permission behavior | Authority note |
| --- | --- | --- | --- | --- | --- |
| `POST /api/tasks/strategy-signals` | `Result<String>` | request body `StrategySignalCreateDTO` | `StrategySignalController` and `StrategySignalService` | exactly `PERMISSION_REPORT_REVIEW` through `RoleAccessConfigService.requirePermission` | strategy manual create/update command |
| `POST /api/tasks/strategy-signals/{signalId}/status` | `Result<String>` | path variable `signalId`; request body `StrategySignalStatusUpdateDTO` | `StrategySignalController` and `StrategySignalService` | exactly `PERMISSION_REPORT_REVIEW` through `RoleAccessConfigService.requirePermission` | strategy status command |

Current strategy service notes:

- `StrategyQueryServiceImpl.pageStrategySignals` serves strategy signal page data.
- `StrategyQueryServiceImpl.getStrategySignalStats` serves strategy signal stats.
- `StrategyQueryServiceImpl.listStrategySignalFactors` serves persisted factor rows.
- `StrategySignalServiceImpl.createOrUpdate` handles manual create/update through backend persistence.
- `StrategySignalServiceImpl.updateStatus` handles status change through backend persistence.
- Current Redis strategy signal cache behavior remains an implementation detail and is not a source of truth.
- Phase 011 does not change validation, permission, audit/message-log, cache, response or frontend command behavior.

## AI Result Projection Dependency

`AiResultDomainProjectionService` is retained as the current shared projection dependency for this bounded horizon.

Current projection facts:

- `AiTaskResultConsumer` consumes `ai.task.result` and invokes the current Java projection path.
- `AiResultDomainProjectionServiceImpl` persists report/evidence/risk/strategy projections from the existing result message shape.
- `AiResultDomainProjectionServiceImpl.saveRiskWarning` is the current risk projection path into `risk_warning` and related detail data.
- `AiResultDomainProjectionServiceImpl.saveStrategySignal` is the current strategy projection path into `strategy_signal` and related factor data.
- The projection service is a dependency shared by report, evidence, risk and strategy. It is not declared final architecture by this artifact.

Projection boundary rules:

- `ai.task.result` remains a projection input contract, not final risk or strategy SoT by itself.
- Persisted `risk_warning`, `risk_warning_detail`, `strategy_signal` and `strategy_signal_factor` records remain the current authority objects after projection.
- Report risk points, report highlights and Python risk/strategy context may influence projection inputs, but they do not become risk/strategy authority unless persisted through the existing projection path.
- Phase 011 does not split, move, rename, wrap, redesign or add helpers around `AiResultDomainProjectionService`.
- Any future projection split requires a later Window 0 decision, human approval, a contract plan, a migration or compatibility plan, and focused verification for report, evidence, risk and strategy behavior.

## Generated Domain-Event Publication

`TaskDomainEventPublisherService` is retained as the current generated-event publication dependency after projection.

Current generated-event facts:

- `TaskDomainEventPublisherServiceImpl.publishRiskWarningGenerated` publishes to `risk.warning.generated`.
- `TaskDomainEventPublisherServiceImpl.publishStrategySignalGenerated` publishes to `strategy.signal.generated`.
- Topic constants currently include `RISK_WARNING_GENERATED`, `STRATEGY_SIGNAL_GENERATED`, `risk.warning.generated` and `strategy.signal.generated`.
- Existing backend tests assert current generated topic names and message type constants.

Generated-event boundary rules:

- `risk.warning.generated` and `strategy.signal.generated` remain generated downstream publication contracts, not replacement SoT.
- Generated messages are emitted from existing projected data. They must not be documented as authority over `risk_warning`, `risk_warning_detail`, `strategy_signal` or `strategy_signal_factor`.
- Phase 011 does not activate new consumers, change publication timing, change topic names, change payloads, add compatibility topics, add Kafka bridges or redesign downstream architecture.
- Any future Kafka downstream change requires a later Window 0 decision, human approval, topic/payload inventory, consumer ownership plan, idempotency/audit plan and compatibility decision.

## Strategy Manual Command And Status Command Boundary

Strategy manual create/update and status update remain backend commands inside `ai-orchestration-service`.

Command rules:

- Frontend forms and buttons are command initiators only.
- `POST /api/tasks/strategy-signals` persists through `StrategySignalService` and the current strategy authority objects.
- `POST /api/tasks/strategy-signals/{signalId}/status` persists through `StrategySignalService` and the current strategy authority objects.
- Both strategy command endpoints keep exactly `PERMISSION_REPORT_REVIEW`.
- Manual command behavior does not move strategy ownership to frontend display state.
- Manual command behavior does not make report, risk, market, workbench or Python context a strategy SoT.

Future readiness implication: before any strategy-service extraction or strategy command move, command owner, route owner, permission authority, audit behavior, status state machine, Redis cache behavior, frontend command flow and compatibility behavior must be decided by a later phase.

## Read-Model Hydration And Display Boundaries

Risk and strategy read models may carry display hydration from adjacent domains, but those fields do not move authority.

Report display references:

- Report risk points, report highlights, confidence score, review status and evidence references remain report facts, projection inputs or display context unless selected data is persisted through risk/strategy authority paths.
- Report pages may show risk or strategy context, but report display does not own risk or strategy facts.

Workbench display references:

- `GET /api/tasks/research-workbench` remains display-only aggregation.
- Workbench risk summary and strategy summary are not risk or strategy SoT.
- Workbench output must not feed backend risk or strategy command/projection authority.

Market display references:

- Market intelligence rows, market events, live event context and source preview data may appear near risk/strategy displays.
- Market context is not risk or strategy SoT unless an approved projection or command path persists selected data into risk/strategy authority objects.

Dashboard and full-detail references:

- Dashboard cards are display summaries over existing contracts.
- `GET /api/tasks/{taskId}/full` may compose task/report/risk/strategy context, but composition is not a second risk or strategy authority path.

## Frontend Risk/Strategy Consumers

Stable frontend routes:

- `/risk-warnings`
- `/signals`
- `/dashboard`
- `/research-workbench`

Stable frontend API functions:

- `fetchRiskWarnings`
- `fetchRiskWarningStats`
- `fetchStrategySignals`
- `fetchStrategySignalStats`
- `createStrategySignal`
- `fetchStrategySignalFactors`
- `updateStrategySignalStatus`

Stable frontend risk/strategy types:

- `RiskWarningStats`
- `RiskWarningListItem`
- `RiskWarningPageData`
- `StrategySignalStats`
- `StrategySignalListItem`
- `StrategySignalPageData`
- `StrategySignalFactorItem`
- `StrategySignalCreateFactorForm`
- `StrategySignalCreateForm`

Frontend boundary rules:

- `RiskWarningCenterView.vue` consumes risk read-model contracts for display and follow-up task convenience only.
- `StrategySignalCenterView.vue` consumes strategy read-model contracts and initiates existing backend strategy commands.
- `DashboardView.vue` displays risk and strategy stats cards only.
- `ResearchWorkbenchView.vue` links to risk and strategy routes and displays aggregation context only.
- `RiskWarningStatsCards.vue` and `StrategySignalStatsCards.vue` are display components only.
- Frontend local filters, selected rows, modal state, form state, status button state and route state do not define persisted risk or strategy facts.
- Phase 011 does not change frontend routes, API endpoint strings, function names, TypeScript shapes, command behavior, display behavior or authority guard scripts.

## Python Risk/Strategy Context And Fallback Provenance

Stable Python context consumers:

- `backend_client.py` exposes `list_risk_warnings` and `list_strategy_signals` using `/api/tasks/risk-warnings` and `/api/tasks/strategy-signals`.
- `market_data_service.py` may attach `riskWarnings`, `riskWarningCount`, `latestRiskWarningSummary`, `strategySignals`, `strategySignalCount` and `latestStrategySignalSummary` to market context.
- `risk_review_agent.py` produces risk output and `fallbackReason` metadata.
- `prompt_builder_service.py`, `financial_analysis_agent.py`, `report_generation_agent.py` and `evidence_collection_agent.py` may read risk/strategy context for prompt, report or evidence generation.
- Python messaging emits AI result payloads that Java consumes through `ai.task.result`.

Python boundary rules:

- Python risk/strategy context is execution/display/provenance context, not final business authority.
- Python fallback snapshots, `dataSource: fallback`, `fallbackReason`, risk fallback output and report fallback provenance remain visible metadata.
- Python does not own persisted `risk_warning`, `risk_warning_detail`, `strategy_signal` or `strategy_signal_factor` records.
- Python-produced `riskWarnings` and strategy context become persisted risk/strategy facts only through the existing Java projection or approved backend command path.
- Phase 011 does not change Python workflow, fallback, provenance, prompt context, backend client paths, result payload fields or Kafka behavior.

## Stable URL/API Contract Table

| Surface | Method and path | Binding | Response envelope/type | Current owner | Stable behavior |
| --- | --- | --- | --- | --- | --- |
| risk warning list | `GET /api/tasks/risk-warnings` | `RiskWarningPageQueryDTO` query object | `Result<RiskWarningPageVO>` | `RiskWarningController` | no explicit permission call |
| risk warning stats | `GET /api/tasks/risk-warning-stats` | none | `Result<RiskWarningStatsVO>` | `RiskWarningController` | no explicit permission call |
| strategy signal list | `GET /api/tasks/strategy-signals` | `StrategySignalPageQueryDTO` query object | `Result<StrategySignalPageVO>` | `StrategySignalController` | no explicit permission call |
| strategy signal stats | `GET /api/tasks/strategy-signal-stats` | none | `Result<StrategySignalStatsVO>` | `StrategySignalController` | no explicit permission call |
| strategy signal factors | `GET /api/tasks/strategy-signals/{signalId}/factors` | path variable `signalId` | `Result<List<StrategySignalFactorItemVO>>` | `StrategySignalController` | no explicit permission call |
| strategy manual create/update | `POST /api/tasks/strategy-signals` | request body `StrategySignalCreateDTO` | `Result<String>` | `StrategySignalController` | exactly `PERMISSION_REPORT_REVIEW` |
| strategy status update | `POST /api/tasks/strategy-signals/{signalId}/status` | path variable `signalId`; request body `StrategySignalStatusUpdateDTO` | `Result<String>` | `StrategySignalController` | exactly `PERMISSION_REPORT_REVIEW` |

Stable contract rules:

- URL paths and HTTP methods stay unchanged.
- Controller owners stay unchanged.
- `Result<T>` response envelopes stay unchanged.
- Binding shapes stay unchanged.
- Response types stay unchanged.
- Permission behavior stays unchanged.
- DTO, VO, entity, mapper, database table, Redis key, Kafka topic, Kafka payload, Python payload and TypeScript shapes stay unchanged.
- No route alias, compatibility endpoint, gateway proxy, bridge, wrapper or adapter is added.

## Current Guardrails Inherited From Prior Phases

Phase 004 guardrails:

- Python fallback provenance must remain visible as metadata.
- Fallback outputs must not become model-generated truth or business SoT.

Phase 005 guardrails:

- The modular-monolith policy applies only to the current governance horizon.
- `ai-orchestration-service` remains a transition host, not final architecture.

Phase 006 guardrails:

- Legacy /api/tasks contracts are frozen as transitional contracts.
- Current risk and strategy endpoint path, method, owner, envelope, binding, response type and permission behavior are guarded by backend tests.

Phase 007 guardrails:

- Frontend workbench and fallback provenance consumers remain display/audit metadata consumers.
- Frontend must not derive business truth from workbench or fallback metadata.

Phase 008 guardrails:

- Risk and strategy remain transition-host domains with separate SoT objects.
- Extraction requires belongs, authority, contract and behavior readiness gates.

Phase 009 guardrails:

- Report risk points, report highlights and report fallback provenance remain report/display/projection context unless persisted through approved authority paths.
- `AiResultDomainProjectionService` was documented as a shared projection dependency, not moved.

Phase 010 guardrails:

- Market events, market intelligence, source preview, CNINFO proxy output, mock/demo ingest and Python market context remain market/display/execution/provenance context unless persisted through approved authority paths.
- Market context does not become risk or strategy SoT by itself.

## Extraction And Migration Blockers

Risk-service extraction blockers:

- Risk projection currently shares `AiResultDomainProjectionService` with report, evidence and strategy projection.
- Risk read-model routes still live under frozen legacy /api/tasks contracts.
- Generated `risk.warning.generated` publication depends on current Java projection and message constants.
- Report, workbench, dashboard, market context and Python consumers display or reuse risk data as context.
- Auth remains header/config based; no gateway/auth target is approved.

Strategy-service extraction blockers:

- Strategy projection currently shares `AiResultDomainProjectionService` with report, evidence and risk projection.
- Strategy manual create/update and status commands live in `StrategySignalService` inside the transition host.
- Strategy read-model and command routes still live under frozen legacy /api/tasks contracts.
- Strategy command permission uses current `PERMISSION_REPORT_REVIEW` behavior.
- Current Redis cache behavior, generated `strategy.signal.generated` publication, frontend command flow and Python context consumers require an approved migration plan before any move.

Route-migration blockers:

- Phase 006 froze legacy /api/tasks paths.
- No breaking change has been approved.
- No route alias, compatibility endpoint, gateway proxy or migration plan has been approved.
- Frontend API functions and Python backend client paths consume current routes.

Projection-split blockers:

- `AiResultDomainProjectionService` writes report/evidence/risk/strategy projections in one current path.
- `ai.task.result` payload behavior is shared across Java projection and Python engine output.
- Splitting projection would require report, evidence, risk, strategy, Kafka and audit verification that is outside Phase 011.

Kafka blockers:

- `risk.warning.generated` and `strategy.signal.generated` have current topic constants and tests, but no Phase 011 downstream redesign is approved.
- Any topic, payload, consumer, idempotency or compatibility change requires a later approved Kafka contract phase.

Gateway/auth and config-store blockers:

- Role permission behavior remains header/config based.
- JSON config files remain transition mechanisms.
- No JWT, gateway, Nacos, DB config store or production auth decision is approved.

## Risk/Strategy-Specific Readiness Gates

Before any future risk-service extraction:

- belongs gate: risk fact writes, risk read models, projection input ownership, generated event ownership and audit ownership have one approved host.
- authority gate: `risk_warning` and `risk_warning_detail` remain the only risk SoT or are replaced only through an approved migration.
- contract gate: current risk URLs, response envelopes, binding shapes, frontend calls and Python client paths have an approved migration or compatibility plan.
- behavior gate: AI projection, risk stats, risk list display, generated event publication, report/workbench/dashboard displays and fallback provenance have focused verification.

Before any future strategy-service extraction:

- belongs gate: strategy fact writes, strategy commands, strategy read models, generated event ownership, Redis cache behavior and audit ownership have one approved host.
- authority gate: `strategy_signal` and `strategy_signal_factor` remain the only strategy SoT or are replaced only through an approved migration.
- contract gate: current strategy URLs, response envelopes, binding shapes, permission behavior, frontend calls and Python client paths have an approved migration or compatibility plan.
- behavior gate: AI projection, manual create/update, status update, factor list, strategy stats, generated event publication, dashboard/workbench displays and fallback provenance have focused verification.

Before any future projection split:

- belongs gate: report, evidence, risk and strategy projection responsibilities are assigned without shared hidden authority.
- authority gate: projection inputs remain inputs and persisted objects remain SoT.
- contract gate: `ai.task.result`, generated events, read-model outputs and frontend/Python consumers have a stable compatibility plan.
- behavior gate: projection idempotency, ordering, partial failure handling, audit visibility and fallback provenance have focused verification.

Before any future Kafka downstream change:

- belongs gate: producer, consumer, retry/idempotency and audit owners are assigned.
- authority gate: generated messages are not replacement SoT unless a later approved migration explicitly changes authority.
- contract gate: topic names, message types, payload fields and compatibility windows are documented.
- behavior gate: publication timing, duplicate handling, missing consumer behavior and failure visibility are tested.

Before any future route migration or permanence decision:

- belongs gate: final host or continued transition host is selected by Window 0 plus human approval.
- authority gate: no display, frontend, Python, fallback or generated-event surface becomes hidden authority.
- contract gate: breaking change or compatibility plan is approved.
- behavior gate: existing risk/strategy screens, API callers, Python context calls and generated events are verified.

## Deferred Decisions

Deferred to later Window 0 decisions plus human approval:

- Whether to extract a risk-service.
- Whether to extract a strategy-service.
- Whether to split, move or retain `AiResultDomainProjectionService`.
- Whether to move generated risk/strategy event publication to another host.
- Whether to activate downstream consumers for `risk.warning.generated` or `strategy.signal.generated`.
- Whether to migrate risk/strategy routes out of legacy /api/tasks.
- Whether to introduce route aliases or compatibility endpoints.
- Whether to redesign Kafka payloads, idempotency or consumer ownership.
- Whether to replace header-based demo auth with gateway/auth/JWT behavior.
- Whether to migrate JSON config to DB, Nacos or a hybrid config-store.
- Whether to reshape frontend risk/strategy routes, API functions, TypeScript shapes or command flows.
- Whether to reshape Python risk workflow, fallback provenance, prompt context or result payload behavior.
- Whether to declare a permanent modular monolith architecture.

## Stop Rules For Later Phases

Stop and return to Window 0 or human decision before any later implementation that requires:

- Changing any risk or strategy URL path, HTTP method, endpoint owner, request binding, response envelope, response type, frontend route, frontend API function, TypeScript shape or permission behavior.
- Adding a route alias, compatibility bridge, gateway proxy, frontend API adapter, projection wrapper, service wrapper, Kafka compatibility bridge or generated-event adapter.
- Moving risk, strategy, projection or generated-event publication code from `ai-orchestration-service` into another service.
- Splitting, moving, redesigning or renaming `AiResultDomainProjectionService`.
- Creating or modifying Java, Python, frontend, database, Redis, Kafka, config, dependency, test or deployment files without an approved phase that allows that scope.
- Changing DTO, VO, entity, mapper, database schema, topic, payload, Redis key, JSON config or API type shapes.
- Reclassifying report risk points, report highlights, workbench summaries, market intelligence rows, generated Kafka messages, frontend display/form state, Python context or fallback provenance as risk/strategy authority.
- Treating `risk.warning.generated` or `strategy.signal.generated` as a replacement source of truth.
- Declaring `ai-orchestration-service`, `AiResultDomainProjectionService`, generated topics, Redis caches, JSON config files or legacy /api/tasks paths final architecture.
- Closing D001, D002, D003, D007 or D008 without a later approved final or governance phase.
- Selecting risk-service extraction, strategy-service extraction, projection split, route migration, gateway/auth, config-store migration, Python behavior change, frontend reshaping, Kafka/database change or permanent modular-monolith outcome.
- Needing code behavior changes to make this risk/strategy readiness artifact true.

