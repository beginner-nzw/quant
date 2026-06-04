# Phase 010 Architect Handoff

## Status

Window: Window 1 - Phase Architect.

Phase: Phase 010 - Market Event and Data-Ingest Ownership Boundary.

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
- `docs/harness/handoffs/steering-decision-phase-010.md`

Additional state read because Phase 010 applies the Phase 008 template after Phase 009:

- `docs/harness/12-transition-host-exit-criteria.md`
- `docs/harness/13-report-boundary-readiness.md`
- `docs/harness/handoffs/phase-009-final.md`
- `docs/harness/handoffs/phase-009-architect.md`

Read-only planning inspection:

- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/MarketEventController.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/MarketIntelligenceController.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/MarketEventService.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/MarketQueryService.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/EventSourceConfigService.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/EventSourcePreviewService.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/MarketEventIngestHistoryService.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/MarketEventAutoTriggerService.java`
- market-related implementation, mapper, entity, consumer and contract-test references found by `rg`
- `quant-ai-platform/ai-config/event-source-configs.json`
- `quant-ai-platform/ai-config/event-ingest-histories.json`
- `quant-ai-platform/ai-config/event-auto-trigger-configs.json`
- `quant-ai-platform/quant-ai-engine/app/services/market_data_service.py`
- `quant-ai-platform/quant-ai-engine/app/clients/backend_client.py`
- market-related Python references found by `rg`
- `quant-ui/src/api/task.ts`
- `quant-ui/src/types/task.ts`
- `quant-ui/src/router/index.ts`
- `quant-ui/src/views/report/MarketEventCenterView.vue`
- `quant-ui/src/views/report/MarketIntelligenceCenterView.vue`

Phase 010 is not Phase 001, so the Phase 001 special Java reading list is not the controlling implementation scope for this handoff.

## 1. Phase Goal

Produce a docs-only market event and data-ingest boundary readiness artifact that applies the Phase 008 readiness template to the market/data-ingest domain.

The bounded goal is:

- Clarify market/data-ingest belongs, authority, contract and behavior boundaries before any later market-service extraction, data-ingest-service extraction, route migration, config-store migration, Kafka/database change, frontend reshaping, Python behavior change or permanent architecture decision.
- Inventory market event facts, market event relations, market event analysis, market intelligence display, real source sync, mock/demo ingest, batch import, source preview/diagnose, CNINFO proxy, ingest history, event source config, event auto-trigger config dependencies, frontend consumers and Python market fallback context.
- Classify current data-ingest responsibilities as transition-host responsibilities inside `ai-orchestration-service` for this bounded horizon, without declaring that host final architecture.
- Preserve Phase 005 modular-monolith horizon policy, Phase 006 legacy `/api/tasks/*` contract freeze, Phase 007 frontend authority guardrails, Phase 008 transition-host inventory and Phase 009 report readiness gates.
- Define market/data-ingest-specific blockers, readiness gates and stop rules for later phases without choosing or implementing extraction.

Window 2 must evaluate every statement in this order:

```text
belongs -> authority -> contract -> behavior
```

This phase is docs-only by design. Existing Phase 006 backend contract tests already guard the current market endpoint inventory. Window 2 must document existing guards and future guard needs, not add code or tests in this phase.

## 2. Belongs

Current belongs baseline:

- `ai-orchestration-service` currently hosts market event facts, market event read models, market intelligence read models, manual create/import commands, mock/demo ingest, source sync, source preview/diagnose, CNINFO proxy, ingest history, event-source config reads and market event auto-trigger handling as a transition host.
- `market_event`, `market_event_relation` and `market_event_analysis` persistence currently lives in `ai-orchestration-service` and remains the current market fact boundary for this phase.
- `event-source-configs.json` is a current JSON-backed config fact for event sources. It is shared with the config domain and must not be moved or mutated in Phase 010.
- `event-ingest-histories.json` is a current JSON/file-backed ingest history fact for transition behavior. It is not a production data-ingest store.
- `event-auto-trigger-configs.json` and `MarketEventStandardizedConsumer` are context dependencies for event auto-trigger behavior, not extraction targets in Phase 010.
- `MarketEventStandardizedPublisherService` and `MarketEventStandardizedConsumer` are in scope as current Kafka context dependencies for `market.event.standardized`; Phase 010 must not change topic or payload behavior.
- `quant-ai-engine` may read market events, market intelligence, source configs and source preview through existing backend contracts to build market context. Python market context and fallback snapshots remain execution context/provenance, not market SoT.
- `quant-ui` remains a contract consumer and UI host. It may display market event and market intelligence data and initiate existing backend commands, but it must not infer or create market truth outside backend contracts.
- `research-task-service` remains the formal host for task creation. Existing follow-up task creation or source-context prefill from market screens is UI convenience and task-create input only, not market authority.

In-scope market/data-ingest surfaces:

- market event list, detail and stats read models
- market intelligence list and stats read models
- market event create command
- batch import preview and batch import commands
- mock/demo ingest command
- real source sync command
- source preview and source diagnose commands
- CNINFO proxy preview/read surface
- event source config read surface
- ingest history read surface and file-backed append behavior as current fact
- market event standardized publication/consumption as current Kafka context
- market event auto-trigger dependency as current context
- frontend market event and market intelligence consumers
- Python market data backend overlay and fallback market snapshot consumers

Context-only dependencies:

- risk and strategy projections that may reference `sourceEventId`
- report, risk, strategy and workbench displays that may show market context
- `GET /api/tasks/research-workbench` as display-only aggregation
- `GET /api/tasks/{taskId}/full` as task detail composition that may include source event context
- model/agent/workflow/prompt config APIs except event source and auto-trigger config references needed for inventory
- downstream placeholder topics for risk, strategy, report and notification
- header-based demo auth and role access config

Explicitly excluded:

- data-ingest-service extraction
- market-service extraction
- route migration, aliases, endpoint rename, endpoint deletion or endpoint consolidation
- gateway/auth/JWT work
- config-store migration from JSON files
- database schema, entity, mapper, DTO or VO reshaping
- Kafka topic, payload, producer or consumer behavior changes
- Python market data, fallback, provenance or workflow behavior changes
- frontend route/API/type/display/command reshaping
- new market source features, new product features, new adapters or new agents

## 3. Authority

Stable market/data-ingest authority objects for current phase:

- `market_event`
- `market_event_relation`
- `market_event_analysis`
- `event-source-configs.json` for current event source config facts
- `event-ingest-histories.json` for current ingest history file facts

Related context facts:

- `event-auto-trigger-configs.json` is a current config dependency for auto-trigger behavior, not a market event SoT.
- `task_message_log` records Kafka/ingest related message handling and remains audit/message-log context, not market event SoT.
- `market.event.standardized` is a Kafka coordination contract, not a persisted market fact by itself.

Authority rules:

- `market_event` remains the current persisted market event fact root.
- `market_event_relation` remains the persisted relation authority for market event relationships.
- `market_event_analysis` remains the persisted analysis authority for derived market event analysis.
- `event-source-configs.json` remains the current event source config fact for this transition horizon.
- `event-ingest-histories.json` remains the current ingest history fact, but it must not be treated as a production-grade data-ingest ledger.
- Market intelligence output is a read model/display aggregation derived from market event and related domain data. It must not replace `market_event`, `market_event_relation` or `market_event_analysis`.
- Mock ingest and demo/manual/generated input can create transition market event records through existing commands, but the artifact must label mock/demo source behavior as non-production and non-authoritative for risk/strategy/report facts by itself.
- Source preview and source diagnose outputs are preview/diagnostic data. They must not become market fact authority unless existing import/sync behavior persists selected data into market authority objects.
- CNINFO proxy output is a source preview/read surface. It must not become market fact authority unless existing import/sync behavior persists selected data into market authority objects.
- Python `MarketDataService` output, `dataSource: fallback`, backend-overlaid workbench context, live preview context, `liveMarketEvents`, `marketIntelligence`, risk/strategy/report context snapshots and prompt context remain execution/display/provenance context only.
- Frontend local state, import preview rows, downloaded templates/results, filters and display cards do not define market facts.

Forbidden authority changes:

- No new market SoT may be created.
- No read model may become command authority.
- No frontend-derived event status, import preview row, source preview result, CNINFO proxy row or display hydration field may define persisted market facts.
- No Python fallback market snapshot or live preview overlay may become market data SoT.
- No mock/demo ingest may be documented as production data source or risk/strategy/report authority by itself.
- No documentation may claim market or data-ingest ownership has moved out of `ai-orchestration-service`.
- No documentation may declare `ai-orchestration-service`, JSON config files, mock ingest, source adapters, CNINFO proxy or legacy `/api/tasks/*` paths final architecture.

## 4. Contract

Stable market URL/API inventory:

| Endpoint | Classification | Current owner |
| --- | --- | --- |
| `GET /api/tasks/market-events` | market event read model | `MarketEventController` in `ai-orchestration-service` |
| `GET /api/tasks/market-event-stats` | market event stats read model | `MarketEventController` in `ai-orchestration-service` |
| `GET /api/tasks/market-events/{eventId}` | market event detail read model | `MarketEventController` in `ai-orchestration-service` |
| `GET /api/tasks/market-events/ingest-history` | ingest history read model | `MarketEventController` in `ai-orchestration-service` |
| `GET /api/tasks/market-event-source-configs` | event source config read model | `MarketEventController` in `ai-orchestration-service` |
| `GET /api/tasks/market-events/cninfo-proxy` | CNINFO proxy preview/read surface | `MarketEventController` in `ai-orchestration-service` |
| `GET /api/tasks/market-intelligence` | market intelligence display/read model | `MarketIntelligenceController` in `ai-orchestration-service` |
| `GET /api/tasks/market-intelligence-stats` | market intelligence stats display/read model | `MarketIntelligenceController` in `ai-orchestration-service` |
| `POST /api/tasks/market-events` | market event create command | `MarketEventController` in `ai-orchestration-service` |
| `POST /api/tasks/market-events/batch-import/preview` | batch import preview command | `MarketEventController` in `ai-orchestration-service` |
| `POST /api/tasks/market-events/batch-import` | batch import command | `MarketEventController` in `ai-orchestration-service` |
| `POST /api/tasks/market-events/mock-ingest` | mock/demo ingest command | `MarketEventController` in `ai-orchestration-service` |
| `POST /api/tasks/market-events/source-sync/{sourceCode}` | source sync command | `MarketEventController` in `ai-orchestration-service` |
| `POST /api/tasks/market-events/source-preview/{sourceCode}` | source preview command | `MarketEventController` in `ai-orchestration-service` |
| `POST /api/tasks/market-events/source-diagnose/{sourceCode}` | source diagnose command | `MarketEventController` in `ai-orchestration-service` |

Stable backend contract details:

- URL paths and HTTP methods stay unchanged.
- `MarketEventController` remains owner for the market-event, ingest-history, source-config, source-preview/source-diagnose/source-sync and CNINFO proxy contracts listed above.
- `MarketIntelligenceController` remains owner for market intelligence list/stats contracts listed above.
- `Result<T>` response envelopes stay unchanged.
- Request binding stays unchanged, including query DTOs, path variables and request bodies.
- Existing read-model endpoints keep their current absence of explicit `requirePermission` calls in Phase 010.
- `POST /api/tasks/market-events`, `POST /api/tasks/market-events/batch-import/preview`, `POST /api/tasks/market-events/batch-import`, `POST /api/tasks/market-events/mock-ingest` and `POST /api/tasks/market-events/source-sync/{sourceCode}` keep `PERMISSION_TASK_CREATE`.
- `POST /api/tasks/market-events/source-preview/{sourceCode}` and `POST /api/tasks/market-events/source-diagnose/{sourceCode}` keep `PERMISSION_MODEL_AGENT_CONFIG_VIEW`.
- `GET /api/tasks/market-events/cninfo-proxy` keeps its current query-object binding and current absence of an explicit permission check.
- DTO, VO, entity, mapper, database table, JSON config, Kafka topic, Kafka payload, Python payload and TypeScript shapes stay unchanged.

Stable frontend routes and functions:

- `/market-events`
- `/intelligence`
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

Stable Kafka/config/Python contracts:

- `market.event.standardized` remains the current market event Kafka coordination topic.
- `MarketEventStandardizedPublisherService` and `MarketEventStandardizedConsumer` behavior stays unchanged.
- `event-source-configs.json`, `event-ingest-histories.json` and `event-auto-trigger-configs.json` remain JSON/file-backed transition facts.
- Python backend client paths for market events, market intelligence, event source configs and source preview stay unchanged.
- Python market fallback and backend overlay fields stay within existing payload/metadata surfaces.

## 5. Allowed File Scope

Window 2 may modify only Phase 010 documentation files.

Required output files:

- `docs/harness/14-market-data-ingest-boundary-readiness.md`
- `docs/harness/handoffs/phase-010-implementation.md`

Optional output file, only if the market/data-ingest inventory becomes too large for the primary document:

- `docs/harness/handoffs/phase-010-market-data-ingest-inventory.md`

Allowed read-only inspection areas:

- market/data-ingest-related Java controller/service/projection/entity/mapper/consumer/test files under `ai-orchestration-service`
- market/data-ingest-related frontend API/type/router/view/component/utility files under `quant-ui/src`
- `quant-ui/scripts/authority-boundary-check.mjs`
- market-data-related Python files under `quant-ai-platform/quant-ai-engine/app`
- `quant-ai-platform/ai-config/event-source-configs.json`
- `quant-ai-platform/ai-config/event-ingest-histories.json`
- `quant-ai-platform/ai-config/event-auto-trigger-configs.json`
- existing harness docs and previous phase handoffs

Window 2 must not write to those read-only inspection areas.

## 6. Forbidden File Scope

Window 2 must not modify:

- any Java production or test file under `quant-ai-platform/quant-services/**`
- any Python file under `quant-ai-platform/quant-ai-engine/**`
- any frontend file under `quant-ui/**`
- `quant-ai-platform/ai-config/**`
- database migration, schema, SQL, seed or mapper files
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
- prior phase handoffs

Window 4 may later update state, debt, backlog or transition lifetime after Window 3 approval. Window 2 must not do that in Phase 010.

If satisfying Phase 010 appears to require any forbidden file, Window 2 must stop as blocked.

## 7. Must Stay Stable

Stable URL / API / behavior:

- Every market endpoint listed in this handoff keeps the same path, method, controller owner, binding, response envelope, response type and permission behavior.
- No market URL moves to `/api/market`, `/api/market-events`, `/api/data-ingest`, `/api/event-sources`, `/api/intelligence` or any other new namespace.
- No route alias, compatibility endpoint, gateway proxy, bridge or wrapper is added.
- No endpoint is deleted, renamed, consolidated or split.
- No frontend route, API function name, endpoint string, call signature or TypeScript shape changes.
- No market event create, batch import, mock ingest, source sync, source preview, source diagnose, CNINFO proxy, market intelligence, ingest history, event source config, auto-trigger, permission, audit/message-log, Kafka or display behavior changes.
- No database table, entity, mapper, DTO, VO, Redis key, Kafka topic, Kafka payload, Python payload or JSON config changes.
- No Python fallback, backend overlay, source preview call, market context, prompt context or report/risk/financial consumer behavior changes.

Stable architecture:

- `ai-orchestration-service` remains the market/data-ingest transition host for this bounded horizon, not final market or data-ingest architecture.
- Legacy `/api/tasks/*` remains a frozen transition namespace, not final route architecture.
- JSON config files remain transition storage, not target config architecture.
- Mock ingest and source preview/diagnose remain transition/demo mechanisms, not production data-ingest architecture.
- CNINFO proxy and existing source adapters remain transition/source mechanisms, not a final adapter architecture.
- Phase 010 does not close D001, D002, D003, D007 or D009.
- Phase 010 does not approve market-service extraction, data-ingest-service extraction, route migration, gateway/auth, config-store migration, Python behavior change, frontend reshaping, Kafka/database changes or permanent modular-monolith architecture.

## 8. Allowed New Class / Method Types

No business-code classes or methods are allowed.

Allowed documentation-only structures:

- Markdown sections and tables.
- Market/data-ingest boundary matrices.
- Market endpoint inventory.
- Market source/config/ingest-history inventory.
- Frontend consumer inventory.
- Python market context and fallback inventory.
- Kafka/context dependency inventory.
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
- any market route alias or URL bridge
- any data-ingest adapter, source adapter, CNINFO proxy replacement, gateway proxy or compatibility wrapper
- any frontend API adapter or frontend truth resolver for market data
- any Python fallback bridge or new fallback provenance field
- any config-store bridge
- any temporary market-service or data-ingest-service wrapper
- any database migration helper
- any new audit/message synchronization bridge

Window 2 may document existing source adapters, CNINFO proxy behavior, Python fallback/provenance paths and existing guards as current facts, but must not create new paths or approve them as target architecture.

## 10. Required Market/Data-Ingest Readiness Artifact Shape

`docs/harness/14-market-data-ingest-boundary-readiness.md` must include:

- Status and scope.
- Inputs and read-only inspection sources.
- Market/data-ingest belongs analysis.
- Market authority object inventory.
- Event source config and ingest history inventory.
- Market read-model surface inventory.
- Market command surface inventory.
- Source sync, preview, diagnose and CNINFO proxy boundary section.
- Mock/demo ingest boundary section.
- Market intelligence display/read-model section.
- Kafka `market.event.standardized` context dependency section.
- Market event auto-trigger context dependency section.
- Frontend market consumer section.
- Python market context and fallback provenance section.
- Related display-only surfaces section for workbench, report, risk and strategy market context.
- Stable URL/API contract table.
- Current guardrails inherited from Phase 004, Phase 005, Phase 006, Phase 007, Phase 008 and Phase 009.
- Extraction, route-migration, data-ingest and config-store blockers.
- Market/data-ingest-specific readiness gates before any future extraction, route migration, data-ingest split, config-store migration or permanence decision.
- Deferred decisions.
- Stop rules for later phases.

The artifact must explicitly state that it does not implement or approve extraction, route migration, endpoint aliases, DTO/VO/entity/schema changes, Kafka changes, frontend changes, Python changes, config-store migration, gateway/auth work, data-ingest-service creation, business behavior changes or new feature work.

## 11. Acceptance Conditions

Phase 010 is acceptable only if all conditions hold:

- `docs/harness/14-market-data-ingest-boundary-readiness.md` exists and is the primary durable market/data-ingest boundary readiness artifact.
- `docs/harness/handoffs/phase-010-implementation.md` records exact files changed and verification outcomes.
- The readiness artifact covers market event facts, relations, analysis, market intelligence, source sync, source preview, source diagnose, CNINFO proxy, mock/demo ingest, batch import, ingest history, event source config, event auto-trigger dependencies, frontend market consumers and Python market fallback context.
- The artifact names the current authority objects: `market_event`, `market_event_relation`, `market_event_analysis`, `event-source-configs.json` and `event-ingest-histories.json`.
- The artifact states that market intelligence, source preview/diagnose output, CNINFO proxy output, mock/demo source payloads, Python fallback snapshots, workbench fields and frontend display/import preview state are not market SoT unless existing approved persistence writes selected data into authority objects.
- The artifact treats `market.event.standardized`, `MarketEventStandardizedPublisherService`, `MarketEventStandardizedConsumer` and market event auto-trigger behavior as current context dependencies, not as moved or redesigned owners.
- The artifact preserves all market URLs, methods, bindings, response envelopes, response types, permission behavior, frontend routes, frontend API functions and TypeScript shapes.
- The artifact preserves Phase 005, Phase 006, Phase 007, Phase 008 and Phase 009 constraints.
- The artifact does not choose market-service extraction, data-ingest-service extraction, route migration, route aliases, endpoint deletion/rename/consolidation, gateway/auth, config-store migration, Python behavior change, frontend reshaping, Kafka/database changes, permanent modular-monolith architecture or new feature work.
- The artifact defines market/data-ingest-specific readiness gates for any later extraction, route migration, data-ingest split, config-store migration or permanence decision.
- No Java, Python, frontend, database, Kafka, `ai-config`, dependency, build-config, deployment, state, debt, backlog, transition lifetime, durable Phase 008/009 artifacts or prior handoff file changes occur.
- Any future-phase recommendation is explicitly labeled as deferred and requiring a later Window 0 decision plus human approval.
- `git diff --name-only` shows only allowed Phase 010 documentation files as Window 2 changes, aside from pre-existing unrelated dirty files that are clearly excluded from the Window 2 claim.

## 12. Required Verification Commands

Window 2 must run from `D:\projects\bussiness`:

```powershell
git diff --name-only
```

Expected result: only allowed Phase 010 docs should be part of the Window 2 change claim. Existing unrelated dirty files must not be reverted or bundled into the Phase 010 implementation claim.

Window 2 must run:

```powershell
Test-Path docs/harness/14-market-data-ingest-boundary-readiness.md
```

Expected result: `True`.

Window 2 must run:

```powershell
rg -n "market_event|market_event_relation|market_event_analysis|event-source-configs|event-ingest-histories|market intelligence|mock ingest|source sync|source preview|source diagnose|CNINFO|market.event.standardized|MarketEventStandardized|auto-trigger|Python|fallback|frontend|readiness gate|legacy /api/tasks|Phase 005|Phase 006|Phase 007|Phase 008|Phase 009" docs/harness/14-market-data-ingest-boundary-readiness.md docs/harness/handoffs/phase-010-implementation.md
```

Expected result: the market/data-ingest readiness artifact and implementation handoff contain the required coverage and inherited guardrail references.

Window 2 must run:

```powershell
rg -n "service extraction|data-ingest-service|route migration|route alias|breaking change|gateway/auth|config-store|database schema|Kafka|frontend reshaping|Python behavior|business code|new feature|permanent modular" docs/harness/14-market-data-ingest-boundary-readiness.md docs/harness/handoffs/phase-010-implementation.md
```

Expected result: any matches are in out-of-scope, blocker, deferred-decision, prerequisite or future-phase sections, not in completed implementation claims.

Window 2 must run or record these read-only inventory checks from `D:\projects\bussiness`:

```powershell
rg -n "@RequestMapping|@GetMapping|@PostMapping|requirePermission|market-events|market-event|market-intelligence|source-sync|source-preview|source-diagnose|cninfo-proxy" quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/MarketEventController.java quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/MarketIntelligenceController.java
```

```powershell
rg -n "MarketEventController|MarketIntelligenceController|/api/tasks/market|MarketEventPageVO|MarketEventStatsVO|MarketEventCreateDTO|MarketEventSourceSyncDTO|PERMISSION_TASK_CREATE|PERMISSION_MODEL_AGENT_CONFIG_VIEW" quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/TaskControllerMappingTest.java
```

```powershell
rg -n "pageMarketEvents|getMarketEventStats|getMarketEvent|listMarketEventIngestHistory|createMarketEvent|previewImportMarketEvents|importMarketEvents|mockIngestMarketEvents|syncMarketEventSource|previewCninfoProxyAnnouncements|pageMarketIntelligence|getMarketIntelligenceStats|appendHistory|event-source-configs|event-ingest-histories" quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl
```

```powershell
rg -n "market.event.standardized|MARKET_EVENT_STANDARDIZED|MarketEventStandardized|KafkaListener|publish\\(" quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/consumer
```

```powershell
rg -n "fetchMarketEvents|fetchMarketEvent|fetchMarketEventStats|fetchMarketEventIngestHistory|fetchMarketEventSourceConfigs|createMarketEvent|previewBatchImportMarketEvents|batchImportMarketEvents|mockIngestMarketEvents|syncMarketEventSource|previewMarketEventSource|diagnoseMarketEventSource|fetchMarketIntelligence|fetchMarketIntelligenceStats|/market-events|/intelligence" quant-ui/src/api/task.ts quant-ui/src/types/task.ts quant-ui/src/router/index.ts quant-ui/src/views/report/MarketEventCenterView.vue quant-ui/src/views/report/MarketIntelligenceCenterView.vue
```

```powershell
rg -n "MarketDataService|list_market_events|list_market_intelligence|list_market_event_source_configs|preview_market_event_source|dataSource|fallback|liveMarketEvents|marketIntelligence|sourceCode|sourceName" quant-ai-platform/quant-ai-engine/app/services/market_data_service.py quant-ai-platform/quant-ai-engine/app/clients/backend_client.py quant-ai-platform/quant-ai-engine/app/agents quant-ai-platform/quant-ai-engine/app/services
```

```powershell
rg -n "sourceCode|ingestMode|MOCK|HTTP_JSON|RSS_XML|CNINFO_PROXY|CNINFO_PUBLIC_CRAWLER|event-auto-trigger|sourceChannel|sourceCategory" quant-ai-platform/ai-config/event-source-configs.json quant-ai-platform/ai-config/event-ingest-histories.json quant-ai-platform/ai-config/event-auto-trigger-configs.json
```

Window 2 must run from `D:\projects\bussiness\quant-ui`:

```powershell
node scripts/authority-boundary-check.mjs
```

Expected result: the existing Phase 007 static guard still passes. If it fails, Window 2 must record the failure as a blocker and must not patch frontend code in this phase.

Maven, npm build and Python runtime verification are not required because Phase 010 forbids Java, frontend, Python and test-code changes. If Window 2 touches any such area, it must stop as blocked instead of validating with builds.

## 13. Blocker Stop Rules

Window 2 must stop and write a blocker in `docs/harness/handoffs/phase-010-implementation.md` if any of these become necessary:

- Changing any URL path, HTTP method, endpoint owner, request binding, response envelope, response type, frontend route, frontend API function, TypeScript shape or permission behavior.
- Adding a route alias, compatibility bridge, gateway proxy, frontend API adapter, service wrapper, source adapter, data-ingest adapter or CNINFO proxy replacement.
- Moving market or data-ingest code from `ai-orchestration-service` into another service.
- Creating or modifying Java, Python, frontend, database, Kafka, config, dependency, test or deployment files.
- Changing DTO, VO, entity, mapper, schema, topic, payload, Redis key, JSON config or API type shapes.
- Reclassifying market intelligence, source preview/diagnose output, CNINFO proxy output, ingest history, workbench fields, frontend display/import preview state or Python fallback context as market authority.
- Treating mock/demo ingest as production source or as risk/strategy/report authority by itself.
- Declaring `ai-orchestration-service`, JSON config files, source adapters, CNINFO proxy or legacy `/api/tasks/*` paths final architecture.
- Closing D001, D002, D003, D007 or D009.
- Selecting market-service extraction, data-ingest-service extraction, route migration, gateway/auth, config-store migration, Python behavior change, frontend reshaping, Kafka/database change or permanent modular-monolith outcome.
- Needing code behavior changes to make the market/data-ingest readiness artifact true.
- Finding that market/data-ingest authority cannot be described without changing the approved Phase 010 scope.
- Needing human approval for breaking changes, service extraction, route migration, config-store migration, gateway/auth implementation, data-ingest-service creation or new product features.
- The existing `node scripts/authority-boundary-check.mjs` guard fails and cannot be treated as an environment-only issue.

When blocked, Window 2 must:

1. Stop edits at the smallest reversible point.
2. Record the exact file, route, domain object, consumer, dependency or decision point that forced the blocker.
3. State which acceptance condition cannot be met.
4. State the smallest future phase or human decision required to unblock.
5. Ask for human decision instead of expanding scope.

## Window 2 Shape

Use one docs-only architecture/governance Window 2 implementation window.

Do not partition into backend, frontend or Python implementers. Do not start service extraction, route migration, gateway/auth work, config migration, data-ingest split, test implementation, frontend guard edits, Python edits or product feature work. Do not proceed until the user approves this Phase 010 architect handoff.

## Human Approval Request

Please approve this Phase 010 architect handoff before Window 2 starts.

Default approval meaning:

- No breaking changes.
- Keep all URL paths and frontend routes stable.
- No business behavior change.
- No new feature work.
- Window 2 may perform docs-only market/data-ingest boundary readiness work inside the allowed file boundaries above.
