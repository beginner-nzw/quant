# Market Data-Ingest Boundary Readiness

## Status And Scope

Status: Phase 010 durable market/data-ingest boundary readiness artifact.

Scope: docs-only architecture and governance inventory for market event and data-ingest responsibilities inside the current `ai-orchestration-service` transition host.

This artifact applies the Phase 008 readiness template to the market/data-ingest boundary after the Phase 009 report readiness artifact. It clarifies market belongs, authority, contract and behavior boundaries before any later market-service extraction, data-ingest-service extraction, route migration, config-store migration, Kafka/database change, frontend reshaping, Python behavior change or permanent modular monolith decision is considered.

This artifact does not implement or approve service extraction, data-ingest-service creation, route migration, route alias, endpoint rename, endpoint deletion, endpoint consolidation, gateway/auth work, config-store migration, database schema change, entity/DTO/VO reshaping, Kafka topic or payload change, frontend reshaping, Python behavior change, business code change or new feature work.

Phase 005 remains the current governance-horizon policy: continue as a modular monolith inside `ai-orchestration-service`, while keeping that host transitional and not final architecture. Phase 006 remains the frozen legacy /api/tasks contract inventory. Phase 007 remains the frontend authority guardrail for workbench and fallback provenance consumers. Phase 008 remains the common transition-host exit criteria inventory. Phase 009 remains the report boundary readiness artifact.

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
- `docs/harness/12-transition-host-exit-criteria.md`
- `docs/harness/13-report-boundary-readiness.md`
- `docs/harness/handoffs/steering-decision-phase-010.md`
- `docs/harness/handoffs/phase-010-architect.md`

Read-only market/data-ingest inventory sources:

- `MarketEventController.java`
- `MarketIntelligenceController.java`
- `MarketEventService.java`
- `MarketEventServiceImpl.java`
- `MarketQueryService.java`
- `MarketQueryServiceImpl.java`
- `EventSourceConfigService.java`
- `EventSourceConfigServiceImpl.java`
- `EventSourcePreviewService.java`
- `EventSourcePreviewServiceImpl.java`
- `MarketEventIngestHistoryService.java`
- `MarketEventIngestHistoryServiceImpl.java`
- `MarketEventAutoTriggerService.java`
- `MarketEventAutoTriggerServiceImpl.java`
- `MarketEventStandardizedPublisherService.java`
- `MarketEventStandardizedPublisherServiceImpl.java`
- `MarketEventStandardizedConsumer.java`
- `LegacyTaskApiContractFreezeTest.java`
- `TaskControllerMappingTest.java`
- `quant-ai-platform/ai-config/event-source-configs.json`
- `quant-ai-platform/ai-config/event-ingest-histories.json`
- `quant-ai-platform/ai-config/event-auto-trigger-configs.json`
- `quant-ui/src/api/task.ts`
- `quant-ui/src/types/task.ts`
- `quant-ui/src/router/index.ts`
- `MarketEventCenterView.vue`
- `MarketIntelligenceCenterView.vue`
- `quant-ui/scripts/authority-boundary-check.mjs`
- `quant-ai-platform/quant-ai-engine/app/services/market_data_service.py`
- `quant-ai-platform/quant-ai-engine/app/clients/backend_client.py`
- market context references under `quant-ai-platform/quant-ai-engine/app/agents` and `quant-ai-platform/quant-ai-engine/app/services`

Read-only inventory commands confirmed current endpoint, service, Kafka, frontend, Python and config facts. No Java, Python, frontend, config, database, build, deployment or test file was modified for Phase 010.

## Market/Data-Ingest Belongs Analysis

Current market event facts, read models, market intelligence read models, manual create/import commands, mock ingest, source sync, source preview, source diagnose, CNINFO proxy reads, ingest history, event source config reads and auto-trigger dependencies belong to `ai-orchestration-service` as transition-host responsibilities.

`ai-orchestration-service` currently owns the Java persistence and read-model boundary for:

- market event list, detail and stats
- market event create
- batch import preview and batch import
- mock ingest
- source sync
- source preview and source diagnose
- CNINFO proxy preview/read access
- event source config reads
- ingest history reads and file-backed append behavior
- market intelligence list and stats
- `market.event.standardized` publication and consumption as current Kafka context
- market event auto-trigger dependency handling as current context

`quant-ai-engine` may call existing backend contracts to read market events, market intelligence, event source configs and source preview output. Its `MarketDataService` builds backend-overlaid and fallback market context for AI execution. That context, including `dataSource`, `liveMarketEvents`, `marketIntelligence` and fallback snapshots, is execution/display/provenance context only and is not market SoT.

`quant-ui` remains a contract consumer and display host. It may render market event center and market intelligence center data and initiate existing backend commands. It must not infer or create market truth from frontend local state, filter state, import preview rows, downloaded template/results, display cards or route state.

`research-task-service` remains the formal host for task creation. Existing follow-up task creation or source-context prefill from market screens is task-create input convenience only and does not move market authority.

Phase 010 retains the current transition-host placement for another bounded horizon. It does not move market or data-ingest responsibility out of `ai-orchestration-service`, does not choose a data-ingest-service, and does not declare `ai-orchestration-service` final architecture.

## Authority Object Inventory

Stable market/data-ingest authority objects for the current phase:

| Authority object | Current meaning | Current host classification |
| --- | --- | --- |
| `market_event` | persisted market event fact root | `ai-orchestration-service` transition host |
| `market_event_relation` | persisted relationship authority for market event relations | `ai-orchestration-service` transition host |
| `market_event_analysis` | persisted derived analysis authority for market event analysis | `ai-orchestration-service` transition host |
| `event-source-configs.json` | current JSON-backed event source config facts | JSON config transition store used by `ai-orchestration-service` and Python readers |
| `event-ingest-histories.json` | current file-backed ingest history facts for transition behavior | JSON/file transition store, not a production data-ingest ledger |

Related context facts:

- `event-auto-trigger-configs.json` is a current config dependency for auto-trigger behavior, not market event SoT.
- `task_message_log` records produced/consumed Kafka message handling and remains audit/message-log context, not market event SoT.
- `market.event.standardized` is a Kafka coordination contract, not a persisted market fact by itself.
- Frontend route, filter, import preview and display state are not market fact authority.
- Python market context and fallback snapshots are execution context/provenance, not market data SoT.

Authority rules:

- `market_event` remains the current persisted market event fact root.
- `market_event_relation` remains the persisted relation authority.
- `market_event_analysis` remains the persisted analysis authority.
- `event-source-configs.json` remains the current event source config fact for this transition horizon.
- `event-ingest-histories.json` remains the current ingest history fact, while explicitly not becoming a production-grade data-ingest ledger.
- Market intelligence is display/read-model output derived from market events and related domain data. It must not replace `market_event`, `market_event_relation` or `market_event_analysis`.
- Source preview, source diagnose and CNINFO proxy output are preview/diagnostic/source-read outputs. They become market facts only through existing approved persistence into market authority objects.
- Mock ingest and demo/manual/generated input can create transition market event records through existing commands, but mock/demo source behavior is non-production and is not risk, strategy or report authority by itself.
- Python `MarketDataService` output, `dataSource: fallback`, backend-overlaid workbench context, `liveMarketEvents`, `marketIntelligence`, context snapshots and prompt context remain execution/display/provenance context only.

Forbidden authority moves:

- No new market SoT may be created in Phase 010.
- No read model may become market command authority.
- No frontend-derived event status, import preview row, source preview result, CNINFO proxy row or display hydration field may define persisted market facts.
- No Python fallback market snapshot or live preview overlay may become market data SoT.
- No mock/demo ingest may be documented as a production data source or as risk/strategy/report authority by itself.
- No documentation in this artifact claims market or data-ingest ownership has moved.

## Event Source Config And Ingest History Inventory

`event-source-configs.json` is the current JSON-backed source config fact. Current source config entries include source identity, `sourceCategory`, `sourceChannel` and `ingestMode` values such as `MOCK`, `HTTP_JSON`, `RSS_XML`, `CNINFO_PROXY`, `CNINFO_PUBLIC_CRAWLER` and HTML crawler modes.

`EventSourceConfigServiceImpl` reads the event source config file through the existing transition path. Phase 010 does not mutate the file, change its schema, migrate it to DB/Nacos, or create a config-store bridge.

`event-ingest-histories.json` is the current JSON/file-backed ingest history fact for transition behavior. Current history rows include manual create, mock ingest, source sync and CNINFO-related source metadata such as `sourceCode`, `sourceCategory` and `sourceChannel`.

`MarketEventIngestHistoryServiceImpl` appends ingest history through existing file-backed behavior. Phase 010 does not change append behavior, retention, schema, path resolution or audit behavior.

`event-auto-trigger-configs.json` is a related context dependency for auto-trigger behavior. It is not a market event authority object and is not redesigned by Phase 010.

Readiness implication: before any future config-store migration or data-ingest split, source config authority, ingest history authority, retention, audit, Java/Python reader contracts and rollback behavior must be decided by a later Window 0 decision plus human approval.

## Market Read-Model Surface Inventory

Market read-model surfaces remain under the frozen legacy /api/tasks namespace:

| Endpoint | Response envelope/type | Binding shape | Current owner | Authority note |
| --- | --- | --- | --- | --- |
| `GET /api/tasks/market-events` | `Result<MarketEventPageVO>` | query object `MarketEventPageQueryDTO` | `MarketEventController` | market event read model |
| `GET /api/tasks/market-event-stats` | `Result<MarketEventStatsVO>` | none | `MarketEventController` | market event stats read model |
| `GET /api/tasks/market-events/{eventId}` | `Result<MarketEventListItemVO>` | path variable `eventId` | `MarketEventController` | market event detail read model |
| `GET /api/tasks/market-events/ingest-history` | `Result<List<MarketEventIngestHistoryItemVO>>` | none | `MarketEventController` | ingest history read model |
| `GET /api/tasks/market-event-source-configs` | `Result<List<EventSourceConfigItemVO>>` | none | `MarketEventController` | event source config read model |
| `GET /api/tasks/market-events/cninfo-proxy` | `Result<CninfoProxyAnnouncementResponseVO>` | query object `MarketEventSourceSyncDTO` | `MarketEventController` | CNINFO proxy preview/read surface |
| `GET /api/tasks/market-intelligence` | `Result<MarketIntelligencePageVO>` | query object `MarketIntelligencePageQueryDTO` | `MarketIntelligenceController` | market intelligence display/read model |
| `GET /api/tasks/market-intelligence-stats` | `Result<MarketIntelligenceStatsVO>` | none | `MarketIntelligenceController` | market intelligence stats display/read model |

Current read-model notes:

- These read surfaces keep their current absence of explicit `requirePermission` calls in Phase 010.
- `MarketQueryServiceImpl` delegates market event read-model methods to `MarketEventService` and serves market intelligence read models.
- `GET /api/tasks/research-workbench` may display market events and latest market context, but it remains display-only aggregation.
- `GET /api/tasks/{taskId}/full` may include source event context as task detail composition, not a second market SoT.

## Market Command Surface Inventory

Market command surfaces remain under the frozen legacy /api/tasks namespace:

| Endpoint | Response envelope/type | Binding shape | Permission behavior | Current owner |
| --- | --- | --- | --- | --- |
| `POST /api/tasks/market-events` | `Result<MarketEventCreateResultVO>` | request body `MarketEventCreateDTO` | `PERMISSION_TASK_CREATE` | `MarketEventController` and `MarketEventService` |
| `POST /api/tasks/market-events/batch-import/preview` | `Result<MarketEventBatchPreviewResultVO>` | request body `MarketEventBatchImportDTO` | `PERMISSION_TASK_CREATE` | `MarketEventController` and `MarketEventService` |
| `POST /api/tasks/market-events/batch-import` | `Result<MarketEventBatchImportResultVO>` | request body `MarketEventBatchImportDTO` | `PERMISSION_TASK_CREATE` | `MarketEventController` and `MarketEventService` |
| `POST /api/tasks/market-events/mock-ingest` | `Result<MarketEventBatchImportResultVO>` | request body `MarketEventMockIngestDTO` | `PERMISSION_TASK_CREATE` | `MarketEventController` and `MarketEventService` |
| `POST /api/tasks/market-events/source-sync/{sourceCode}` | `Result<MarketEventBatchImportResultVO>` | path variable `sourceCode`; request body `MarketEventSourceSyncDTO` | `PERMISSION_TASK_CREATE` | `MarketEventController` and `MarketEventService` |
| `POST /api/tasks/market-events/source-preview/{sourceCode}` | `Result<EventSourcePreviewResultVO>` | path variable `sourceCode`; request body `MarketEventSourceSyncDTO` | `PERMISSION_MODEL_AGENT_CONFIG_VIEW` | `MarketEventController` |
| `POST /api/tasks/market-events/source-diagnose/{sourceCode}` | `Result<EventSourceRequestDiagnosticResultVO>` | path variable `sourceCode`; request body `MarketEventSourceSyncDTO` | `PERMISSION_MODEL_AGENT_CONFIG_VIEW` | `MarketEventController` |

Command rules:

- Create, batch import, mock ingest and source sync are transition-host commands inside `ai-orchestration-service`.
- Batch import preview is an existing command-like validation/preview surface. Preview rows are not market facts until imported through existing persistence.
- Source preview and source diagnose are preview/diagnostic surfaces with model-agent config view permission. Their output is not market fact authority by itself.
- Phase 010 does not change permission behavior, request binding, response envelope, response type, validation behavior, ingest behavior, audit/message-log behavior or frontend command behavior.

## Source Sync, Preview, Diagnose And CNINFO Proxy Boundary

Source sync is the existing command path that can persist selected source output into market authority objects through `MarketEventService.syncMarketEventSource`.

Source preview and source diagnose are existing transition/source diagnostics. They may call current source mechanisms and return preview or diagnostic output, but they are not source-of-truth surfaces unless an approved import/sync path persists selected data into `market_event`, `market_event_relation` or `market_event_analysis`.

CNINFO proxy output is a preview/read surface exposed through `GET /api/tasks/market-events/cninfo-proxy` and related current service behavior. CNINFO proxy rows are not market facts until existing approved persistence writes selected data into market authority objects.

Existing source mechanisms, including CNINFO proxy and source adapters, remain transition/source mechanisms. Phase 010 documents them as current facts only; it does not create a new adapter, bridge, wrapper, proxy, resolver, route alias or target data-ingest architecture.

Future readiness implication: before any data-ingest-service split, real source sync, preview-only access, diagnose-only access, proxy reads, adapter ownership, retry/idempotency, rate limiting, audit and failure provenance must be separated and approved.

## Mock/Demo Ingest Boundary

Mock ingest remains a transition/demo mechanism. T3 in `docs/harness/05-transition-lifetime.md` forbids treating mock ingest as a production data source or using mock source as risk/strategy authority.

Mock/demo payloads may create market event records only through existing approved commands. Once persisted, the resulting rows are current market event records, but the mock/demo source label and provenance remain important and must not be erased in future governance.

Mock ingest is not proof that a real market source, risk signal, strategy signal or report fact exists. Report, risk and strategy domains may display or project information that references market context only through their own approved authority/projection rules.

Phase 010 does not change mock ingest UI, backend behavior, source labels, generated payload shape, import behavior or ingest history append behavior.

## Market Intelligence Display/Read-Model Section

Market intelligence list and stats are current read-model/display surfaces served by `MarketIntelligenceController` and `MarketQueryServiceImpl`.

Market intelligence may combine market event data with related report/risk/strategy context for display. It does not replace `market_event`, `market_event_relation`, `market_event_analysis`, report authority objects, risk authority objects or strategy authority objects.

Market intelligence rows can be used by frontend views and Python market context as display/execution context. They must not become market SoT or command authority without a later approved phase.

Phase 010 preserves `GET /api/tasks/market-intelligence`, `GET /api/tasks/market-intelligence-stats`, frontend route `/intelligence`, API functions `fetchMarketIntelligence` and `fetchMarketIntelligenceStats`, and the current TypeScript shapes.

## Kafka `market.event.standardized` Context Dependency

`market.event.standardized` remains the current market event Kafka coordination topic.

`MarketEventStandardizedPublisherService` publishes standardized market event messages after current market event persistence paths. `MarketEventStandardizedConsumer` consumes the same topic, uses message-log/idempotency support through `TaskMessageLogService`, and records consumed, skipped or failed handling.

Kafka authority rules:

- `market.event.standardized` is a coordination contract, not market SoT by itself.
- `MarketEventStandardizedPublisherService` and `MarketEventStandardizedConsumer` are current context dependencies, not moved or redesigned owners.
- `task_message_log` records message handling and audit/idempotency context, not market event fact authority.
- Phase 010 does not change topic constants, payload fields, producer behavior, consumer behavior, group IDs, idempotency behavior, error handling or downstream topics.

Any future Kafka or data-ingest split must preserve topic contract, message idempotency, audit visibility, retry/failure semantics and authority separation.

## Market Event Auto-Trigger Context Dependency

`event-auto-trigger-configs.json` and `MarketEventAutoTriggerService` are current context dependencies. Auto-trigger behavior can depend on market events and event source config, but Phase 010 does not move, redesign, enable, disable or reclassify it.

Auto-trigger config is not a market event fact root. It remains part of JSON-backed transition config and must be handled together with config governance if a future config-store migration is selected.

Any future market/data-ingest split must decide whether auto-trigger belongs with market events, workflow/task creation, config governance or another approved host. That decision is deferred.

## Frontend Market Consumer Section

Stable frontend routes:

- `/market-events`
- `/intelligence`

Stable frontend API functions:

- `fetchMarketEvents`
- `fetchMarketEvent`
- `fetchMarketEventStats`
- `fetchMarketEventIngestHistory`
- `fetchMarketEventSourceConfigs`
- `createMarketEvent`
- `previewBatchImportMarketEvents`
- `batchImportMarketEvents`
- `mockIngestMarketEvents`
- `syncMarketEventSource`
- `previewMarketEventSource`
- `diagnoseMarketEventSource`
- `fetchMarketIntelligence`
- `fetchMarketIntelligenceStats`

Stable frontend market types include:

- `MarketEventCreateForm`
- `MarketEventCreateResult`
- `MarketEventBatchImportForm`
- `MarketEventMockIngestForm`
- `MarketEventSourceSyncForm`
- `EventSourcePreviewResult`
- `EventSourceRequestDiagnosticResult`
- `MarketEventBatchPreviewResult`
- `MarketEventBatchImportResult`
- `MarketEventIngestHistoryItem`
- `EventSourceConfigItem`
- `MarketEventStats`
- `MarketEventListItem`
- `MarketEventPageData`
- `MarketIntelligenceStats`
- `MarketIntelligenceListItem`
- `MarketIntelligencePageData`

Frontend boundary rules:

- `MarketEventCenterView.vue` may display market event data, ingest history, event source configs and command results from existing contracts.
- `MarketIntelligenceCenterView.vue` may display market intelligence list and stats from existing contracts.
- Frontend local filters, selected rows, import preview rows, modal state, form state, route state and display cards are UI state only.
- Frontend may initiate existing backend commands, but backend contracts and authority objects determine persisted market facts.
- The existing `authority-boundary-check.mjs` guard from Phase 007 remains a current guardrail for workbench and fallback provenance; Phase 010 does not edit or extend it.

## Python Market Context And Fallback Provenance

`MarketDataService` in `quant-ai-engine` builds market context for AI execution. It starts from a fallback snapshot and overlays backend workbench, market event, risk, strategy, market intelligence and live source preview context when available.

Current Python backend client paths include:

- `list_market_events`
- `list_market_intelligence`
- `list_market_event_source_configs`
- `preview_market_event_source`

Current market context/provenance fields include:

- `dataSource`
- `recentMarketEvents`
- `recentEventCount`
- `riskWarnings`
- `riskWarningCount`
- `strategySignals`
- `strategySignalCount`
- `marketIntelligence`
- `marketIntelligenceCount`
- `liveMarketEvents`
- `liveMarketEventSourceCode`
- `liveMarketEventSourceName`
- `sourceCode`
- `sourceName`
- fallback reason/provenance fields propagated into report metadata such as `marketDataFallbackReason`

Python boundary rules:

- Python produces execution context and provenance metadata. It does not own final market facts.
- `dataSource: fallback` and fallback market snapshots remain clearly provenance-bearing and non-authoritative.
- Backend-overlaid `marketIntelligence`, `liveMarketEvents` and source preview items remain context for planning, financial, risk and report agents unless existing Java persistence writes selected data into market authority objects.
- Prompt context and report/risk/financial fallback outputs may reference market context, but those references do not create market SoT.
- Phase 010 does not change Python workflow, backend client paths, fallback behavior, payload shape, prompt context, Kafka result payloads or provenance fields.

## Related Display-Only Surfaces

Workbench:

- `GET /api/tasks/research-workbench` can display market events and latest market context.
- Workbench remains display-only aggregation and existing task-create source-context prefill support only.
- Workbench output must not feed market command authority, projection authority or market SoT.

Report:

- Report pages, report metadata and report context snapshots may display market context, `marketDataSource`, `marketDataFallbackReason`, market intelligence references or live market event references.
- Report facts remain under report authority objects after Java projection, not under market display fields.

Risk:

- Risk warning pages and Python risk analysis may reference market events, market intelligence or live market events.
- Risk facts remain under `risk_warning` and `risk_warning_detail`.

Strategy:

- Strategy signal pages and Python strategy context may reference source event IDs or market intelligence.
- Strategy facts remain under `strategy_signal` and `strategy_signal_factor`.

Task full detail:

- `GET /api/tasks/{taskId}/full` may include source event context as task detail composition. That composition does not become an alternate market SoT.

## Stable URL And API Contract Table

All market contracts below must remain stable unless a later approved phase explicitly accepts a migration or breaking change:

| HTTP | Path | Classification | Controller owner | Response envelope/type | Permission behavior |
| --- | --- | --- | --- | --- | --- |
| GET | `/api/tasks/market-events` | market event read model | `MarketEventController` | `Result<MarketEventPageVO>` | no explicit market read permission check in current contract |
| GET | `/api/tasks/market-event-stats` | market event stats read model | `MarketEventController` | `Result<MarketEventStatsVO>` | no explicit market read permission check in current contract |
| GET | `/api/tasks/market-events/{eventId}` | market event detail read model | `MarketEventController` | `Result<MarketEventListItemVO>` | no explicit market read permission check in current contract |
| GET | `/api/tasks/market-events/ingest-history` | ingest history read model | `MarketEventController` | `Result<List<MarketEventIngestHistoryItemVO>>` | no explicit market read permission check in current contract |
| GET | `/api/tasks/market-event-source-configs` | event source config read model | `MarketEventController` | `Result<List<EventSourceConfigItemVO>>` | no explicit market read permission check in current contract |
| GET | `/api/tasks/market-events/cninfo-proxy` | CNINFO proxy preview/read surface | `MarketEventController` | `Result<CninfoProxyAnnouncementResponseVO>` | no explicit market read permission check in current contract |
| GET | `/api/tasks/market-intelligence` | market intelligence display/read model | `MarketIntelligenceController` | `Result<MarketIntelligencePageVO>` | no explicit market intelligence read permission check in current contract |
| GET | `/api/tasks/market-intelligence-stats` | market intelligence stats display/read model | `MarketIntelligenceController` | `Result<MarketIntelligenceStatsVO>` | no explicit market intelligence read permission check in current contract |
| POST | `/api/tasks/market-events` | market event create command | `MarketEventController` | `Result<MarketEventCreateResultVO>` | `PERMISSION_TASK_CREATE` |
| POST | `/api/tasks/market-events/batch-import/preview` | batch import preview command | `MarketEventController` | `Result<MarketEventBatchPreviewResultVO>` | `PERMISSION_TASK_CREATE` |
| POST | `/api/tasks/market-events/batch-import` | batch import command | `MarketEventController` | `Result<MarketEventBatchImportResultVO>` | `PERMISSION_TASK_CREATE` |
| POST | `/api/tasks/market-events/mock-ingest` | mock/demo ingest command | `MarketEventController` | `Result<MarketEventBatchImportResultVO>` | `PERMISSION_TASK_CREATE` |
| POST | `/api/tasks/market-events/source-sync/{sourceCode}` | source sync command | `MarketEventController` | `Result<MarketEventBatchImportResultVO>` | `PERMISSION_TASK_CREATE` |
| POST | `/api/tasks/market-events/source-preview/{sourceCode}` | source preview command | `MarketEventController` | `Result<EventSourcePreviewResultVO>` | `PERMISSION_MODEL_AGENT_CONFIG_VIEW` |
| POST | `/api/tasks/market-events/source-diagnose/{sourceCode}` | source diagnose command | `MarketEventController` | `Result<EventSourceRequestDiagnosticResultVO>` | `PERMISSION_MODEL_AGENT_CONFIG_VIEW` |

Stable contract constraints:

- URL paths and HTTP methods stay unchanged.
- `MarketEventController` remains owner for listed market event, ingest-history, source-config, source-preview/source-diagnose/source-sync and CNINFO proxy contracts.
- `MarketIntelligenceController` remains owner for listed market intelligence contracts.
- `Result<T>` response envelopes stay unchanged.
- Request bindings stay unchanged, including query DTOs, path variables and request bodies.
- Current absence of explicit permission checks on listed read-model endpoints stays unchanged in Phase 010.
- Listed command endpoints keep `PERMISSION_TASK_CREATE` or `PERMISSION_MODEL_AGENT_CONFIG_VIEW` exactly as inventoried.
- Frontend route paths, API function names, endpoint strings, call signatures and TypeScript shapes stay unchanged.
- DTO, VO, entity, mapper, database table, Redis key, Kafka topic, Kafka payload, Python payload and JSON config shapes stay unchanged.

## Current Guardrails Inherited

Phase 004:

- Python fallback provenance is visible in existing metadata surfaces.
- Fallback output must not appear as model-generated truth.
- Python market fallback context remains provenance-bearing and non-authoritative.

Phase 005:

- `ai-orchestration-service` remains a modular monolith only for the current governance horizon.
- The service remains a transition host, not final architecture.
- No service extraction, data-ingest-service split, route migration or permanent modular decision is approved.

Phase 006:

- Legacy /api/tasks market routes are frozen as transitional contracts.
- Backend contract tests guard path, method, controller owner, response envelope, binding shape and permission behavior.
- Domain namespace aliases such as `/api/market`, `/api/market-events`, `/api/data-ingest` or `/api/intelligence` are not approved.

Phase 007:

- Frontend workbench output remains display/navigation/source-context prefill only.
- Frontend fallback provenance, `contextSnapshot`, `reportMeta`, `generationMode`, `fallbackReason` and related fields are display/audit metadata only.
- The existing `authority-boundary-check.mjs` guard prevents current workbench and provenance fields from feeding command authority.

Phase 008:

- The transition-host exit criteria inventory defines the common readiness gate template.
- Market exit requires real ingest, mock ingest, source adapters, CNINFO proxy, market intelligence display, event source config and ingest history to be separated by responsibility and approved host before any ownership move.

Phase 009:

- Report readiness keeps report facts separate from market intelligence and market context display.
- Report `contextSnapshot`, fallback provenance and market context references remain metadata/projection input unless persisted by approved projection logic into report authority objects.

## Extraction, Route-Migration, Data-Ingest And Config-Store Blockers

Current blockers before any market-service extraction, data-ingest-service extraction, route migration, config-store migration or permanence decision:

- Market read models, market commands, source sync, source preview, source diagnose, CNINFO proxy, ingest history and source config reads still live under frozen legacy /api/tasks routes.
- `MarketEventController` owns read and command market event surfaces in the transition host.
- `MarketIntelligenceController` owns market intelligence display/read-model surfaces in the transition host.
- Mock ingest, real source sync, source preview/diagnose, CNINFO proxy and persistence are still co-located in `ai-orchestration-service`.
- `event-source-configs.json`, `event-ingest-histories.json` and `event-auto-trigger-configs.json` are JSON/file-backed transition facts, not a stable target config or ingest store.
- `MarketEventStandardizedPublisherService` and `MarketEventStandardizedConsumer` share the `market.event.standardized` coordination topic and message-log/idempotency behavior.
- Frontend market consumers are centralized in `quant-ui/src/api/task.ts`, `quant-ui/src/types/task.ts`, market routes and market views.
- Python market context uses backend APIs and fallback snapshots for AI execution context.
- Current permission behavior depends on header-based demo auth and JSON role access config, not production gateway/auth architecture.
- Route migration would need a breaking change or compatibility decision and a Phase 006 inventory update.
- Config-store migration would need Java/Python reader, audit, rollback and schema/versioning decisions.

## Market/Data-Ingest-Specific Readiness Gates

Before any future market extraction, data-ingest split, route migration, config-store migration or permanent modular monolith decision, a later phase must satisfy these readiness gate conditions:

1. Belongs gate: market event facts, market event read models, market intelligence display, event source config, ingest history, source sync, source preview, source diagnose, CNINFO proxy and auto-trigger dependencies have one approved host or an explicitly retained transition path.
2. Authority gate: `market_event`, `market_event_relation`, `market_event_analysis`, `event-source-configs.json` and `event-ingest-histories.json` stay the named authority objects unless a later approved phase changes them.
3. Source boundary gate: real source sync, mock ingest, batch import, source preview, source diagnose and CNINFO proxy are separated by responsibility, persistence behavior, audit/provenance and production/demo status.
4. Kafka gate: `market.event.standardized`, `MarketEventStandardizedPublisherService`, `MarketEventStandardizedConsumer`, message idempotency and task message log behavior are explicitly preserved, split or moved through an approved plan.
5. Config gate: event source config, ingest history and auto-trigger config storage, schema/versioning, Java/Python readers, audit, rollback and retention are decided before config-store migration.
6. Contract gate: all current market URLs, HTTP methods, request bindings, response envelopes, response types, permission behavior, frontend routes, frontend API functions and TypeScript shapes are inventoried and either preserved or given an approved migration plan.
7. Consumer gate: market event center, market intelligence center, workbench market displays, report/risk/strategy market context consumers and Python market context consumers are mapped to the approved contract.
8. Fallback provenance gate: Python `dataSource: fallback`, fallback market snapshots, live preview context, `marketIntelligence`, `liveMarketEvents` and market context snapshot fields remain metadata/execution context unless persisted by approved market authority logic.
9. Permission gate: create/import/mock ingest/source sync/source preview/source diagnose permission behavior is explicitly preserved or replaced by an approved auth/gateway/role authority decision.
10. Verification gate: backend contract tests or static guards cover any approved behavior, route or consumer risk; frontend and Python verification are added only if a later phase changes those areas.
11. Rollback/exit gate: any temporary compatibility path has an owner, retirement trigger and review point.

## Deferred Decisions

The following decisions are deferred to later Window 0 selection plus human approval:

- market-service extraction
- data-ingest-service extraction or data-ingest-service creation
- market route migration, route alias, endpoint rename, endpoint deletion or endpoint consolidation
- breaking change acceptance
- gateway/auth/JWT implementation
- config-store migration from JSON files
- database schema, entity, mapper, DTO or VO migration
- Kafka topic or payload migration
- source adapter replacement, CNINFO proxy replacement or source proxy redesign
- Python workflow, fallback, provenance, backend overlay or market data behavior change
- frontend route, API function or TypeScript shape reshaping
- frontend reshaping of market event center or market intelligence center behavior
- splitting market event persistence from source ingest responsibilities
- declaring legacy /api/tasks paths final architecture
- declaring `ai-orchestration-service` permanent modular monolith architecture
- new market source feature, new product feature, new agent or new ingest workflow

## Stop Rules For Later Phases

Stop and return to Window 0/user decision if a later phase requires:

- changing URL paths, HTTP methods, request binding, response envelope, response type, TypeScript shape or permission behavior without approval
- creating route aliases, compatibility bridges, gateway proxies, adapters, fallbacks, wrappers, resolvers or frontend truth resolvers
- moving market or data-ingest code out of `ai-orchestration-service` without an approved extraction phase
- using market intelligence, source preview output, source diagnose output, CNINFO proxy output, ingest history, workbench fields, frontend display/import preview state or Python fallback context as market SoT
- using mock/demo ingest as a production source or as risk/strategy/report authority by itself
- using fallback provenance as model-generated truth
- treating `ai-orchestration-service`, JSON config files, source adapters, CNINFO proxy or legacy /api/tasks as final market/data-ingest architecture
- closing D001, D002, D003, D007 or D009 without preserving later human approval gates
- modifying Java, Python, frontend, database, Kafka, config, dependency, build or deployment files outside an approved file scope
- changing business behavior to make a governance document true
- adding a new feature to justify a governance move
