# Phase 008 Architect Handoff

## Status

Window: Window 1 - Phase Architect.

Phase: Phase 008 - Transition Host Exit Criteria Inventory.

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
- `docs/harness/handoffs/steering-decision-phase-008.md`

Additional handoffs read to preserve frozen constraints:

- `docs/harness/handoffs/phase-005-final.md`
- `docs/harness/handoffs/phase-006-final.md`
- `docs/harness/handoffs/phase-007-final.md`
- `docs/harness/handoffs/phase-005-architect.md`
- `docs/harness/handoffs/phase-006-architect.md`
- `docs/harness/handoffs/phase-007-architect.md`

Read-only code and file inventory inspected for Phase 008 planning:

- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/**`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/**`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/consumer/**`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/mapper/**`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/domain/entity/**`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/domain/dto/**`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/domain/vo/**`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/**`
- `quant-ai-platform/ai-config/**`
- `quant-ui/src/api/task.ts`
- `quant-ui/src/types/task.ts`
- `quant-ui/src/router/index.ts`
- `quant-ui/src/views/report/**`
- `quant-ui/src/views/task/**`
- `quant-ui/src/components/report/**`
- `quant-ui/src/components/task/**`
- `quant-ui/src/utils/**`
- `quant-ai-platform/quant-ai-engine/app/**`
- `quant-ai-platform/quant-ai-engine/tests/**`

Phase 008 is not Phase 001, so the Phase 001 special implementation reading list is not the controlling scope for this handoff.

## 1. Phase Goal

Produce a docs-only transition-host exit criteria inventory for `ai-orchestration-service`.

The bounded goal is:

- Inventory current `ai-orchestration-service` transition-host responsibilities by domain.
- Define per-domain SoT, current host, read-model placement, command surfaces, aggregation surfaces, legacy route dependencies, current guardrails, extraction blockers, exit criteria and readiness gates.
- Cover at minimum report, market, risk, strategy, audit, config and workbench responsibilities inside `ai-orchestration-service`.
- Preserve the Phase 005 next-governance-horizon modular-monolith policy.
- Preserve the Phase 006 frozen legacy non-task `/api/tasks/*` contract inventory.
- Preserve the Phase 003, Phase 004 and Phase 007 workbench/fallback authority guardrails.
- Prepare later Window 0 decisions, without choosing extraction, route migration, permanence, gateway/auth, data-ingest split, config-store migration or new features.

This is a docs-only architecture/governance phase. It is not a service extraction phase, not a route migration phase, not a backend/frontend/Python implementation phase and not a test-code phase.

Window 2 must evaluate every statement in this order:

```text
belongs -> authority -> contract -> behavior
```

Passing a writing checklist is not enough if the inventory blurs SoT, weakens the Phase 006 route freeze, treats a display aggregation as truth, or reclassifies the transition host as final architecture.

## 2. Belongs

Current belongs baseline:

- `research-task-service` remains the formal host for task creation, task routing support, duplicate/hot-target protection and task dispatch outbox.
- `ai-orchestration-service` remains the current host for AI status/result/audit consumption, task runtime read models, task retry/cancel control, AI result projection, report/risk/strategy/market/audit/config read surfaces and research workbench aggregation.
- `ai-orchestration-service` remains a transition host for report, market, risk, strategy, audit, config and workbench responsibilities.
- `quant-ai-engine` remains the formal host for AI workflow execution, model invocation and rule fallback during AI execution.
- `quant-ui` remains a contract consumer and display host. It must not define business truth.

In-scope transition-host domains:

- Report: report query, report center, report review, report versions and report evidence/reference surfaces.
- Market: market event query, create/import/mock/source-sync/preview/diagnose/CNINFO proxy, market event ingest history, event source config surfaces and market intelligence display/read-model surfaces.
- Risk: risk warning query, stats and AI-result projection dependencies.
- Strategy: strategy signal query, factor query, create/status command surfaces and AI-result projection dependencies.
- Audit: audit compliance query/stats, task message logs, AI prompt/audit records and config-change audit surfaces.
- Config: model, agent, workflow, prompt template, model strategy, event source, event auto trigger and role access JSON config APIs.
- Workbench: `research-workbench` display aggregation and its backend/frontend guardrails.

Context-only dependencies that Phase 008 must mention but must not redesign:

- Task runtime read model, retry and cancel control.
- AI status/result/audit Kafka consumers.
- `AiResultDomainProjectionService` as the current projection host feeding report/risk/strategy/evidence.
- Python fallback provenance and market data fallback.
- Frontend routes and API consumers.

Explicitly excluded from Phase 008 exit-gate ownership decisions:

- Task creation ownership in `research-task-service`.
- New microservice ownership for report, market, risk, strategy, audit or config.
- Gateway/auth/service-discovery ownership.
- Permanent modular-monolith ownership.
- Route migration ownership.
- Config-store target ownership.
- Real data-ingest-service ownership.

## 3. Authority

Window 2 must preserve the authority matrix from `02-authority-matrix.md`.

Stable authority facts:

- Task creation fact remains `research_task` under `research-task-service`.
- Task runtime state remains `research_task.status/current_stage` plus `ai_workflow_instance` and `ai_agent_execution` in the current runtime read model.
- Report facts remain `research_report`, `research_report_version` and `research_report_section` in the current transition host.
- Report evidence remains `report_evidence_ref` and `research_report_section` in the current transition host.
- Risk warning facts remain `risk_warning` and `risk_warning_detail` in the current transition host.
- Strategy signal facts remain `strategy_signal` and `strategy_signal_factor` in the current transition host.
- Market event facts remain `market_event`, `market_event_relation` and `market_event_analysis` in the current transition host.
- Config facts remain `ai-config/*.json`, with Java and Python file readers and audited transition config APIs.
- Permission facts remain `ai-config/role-access-configs.json` plus request headers for the current demo/runtime flow.
- Audit facts remain `audit_record`, `task_message_log`, `ai_prompt_audit` and config change audit files.
- Research workbench has no SoT. It is display aggregation only.
- Python fallback provenance is metadata only and must not become business truth.

For every in-scope domain, the Phase 008 inventory must state:

- authoritative objects
- current host and whether that host is formal or transitional
- read-model surfaces
- command surfaces
- non-authoritative aggregation/display surfaces
- current guardrails
- extraction blockers
- exit criteria
- later phase gates required before any ownership move

Forbidden authority changes:

- No new source of truth may be created.
- No read model may become command authority.
- No display aggregation may become business truth.
- No fallback/provenance metadata may become business truth.
- No frontend-derived state may define report, risk, strategy, market, audit, config or task truth.
- No documentation may claim an ownership move has happened.

## 4. Contract

Phase 008 must preserve all current external contracts.

Stable backend API groups:

- `POST /api/research/tasks`
- task read/control endpoints under `/api/tasks`
- all Phase 006 frozen non-task legacy `/api/tasks/*` endpoints
- report, risk, strategy, market, market-intelligence, audit, config and workbench contracts documented in `04-contract-map.md` and guarded by Phase 006

Stable frontend contracts:

- all existing `quant-ui` routes
- all existing `quant-ui/src/api/task.ts` endpoint strings, HTTP methods, function names and call signatures
- all existing TypeScript DTO-like field names and optionality
- all Phase 007 frontend display/metadata authority notes and guard intent

Stable Kafka contracts:

- `ai.task.dispatch`
- `ai.task.status`
- `ai.task.result`
- `ai.task.audit`
- `market.event.standardized`
- downstream placeholder topics already listed in `04-contract-map.md`

Stable config contracts:

- JSON files under `quant-ai-platform/ai-config`
- existing config API paths and audit expectations
- existing Java and Python file-reader assumptions

Stable behavior contracts:

- no user-visible business behavior change
- no endpoint move, alias, deletion, rename, consolidation or method change
- no request/response shape change
- no permission behavior change
- no database, Redis, Kafka, config, fallback, projection, audit or frontend command behavior change

The Phase 008 inventory may recommend future phases, but it must not approve or perform them.

## 5. Allowed File Scope

Window 2 may modify only documentation files for the Phase 008 inventory.

Required output files:

- `docs/harness/handoffs/phase-008-implementation.md`
- `docs/harness/12-transition-host-exit-criteria.md`

Optional output file, only if the inventory becomes too large for the primary document:

- `docs/harness/handoffs/phase-008-transition-host-inventory.md`

The primary durable inventory should remain in `docs/harness/12-transition-host-exit-criteria.md`. The implementation handoff must summarize the inventory, list exact files changed and record verification outcomes.

Allowed read-only inspection areas:

- Java controller, service, consumer, mapper, entity, DTO, VO and test files under `ai-orchestration-service`
- frontend API, type, route, view, component and utility files under `quant-ui/src`
- Python engine files under `quant-ai-platform/quant-ai-engine`
- config JSON files under `quant-ai-platform/ai-config`
- existing harness docs and previous phase handoffs

Window 2 must not write to those inspection areas.

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
- `docs/harness/03-host-ownership.md`
- `docs/harness/04-contract-map.md`
- `docs/harness/05-transition-lifetime.md`
- `docs/harness/06-debt-register.md`
- `docs/harness/07-phase-backlog.md`
- `docs/harness/08-eval-checklist.md`
- `docs/harness/09-window-protocol.md`
- `docs/harness/10-steering-state-machine.md`
- `docs/harness/11-cycle-runbook.md`
- prior phase handoffs

Window 4 may later update current state, debt, backlog or transition lifetime after Window 3 approval. Window 2 must not do that in Phase 008.

If satisfying Phase 008 appears to require any forbidden file, Window 2 must stop as blocked.

## 7. Must Stay Stable

Stable URL / API rules:

- All existing URL paths remain unchanged.
- All existing HTTP methods remain unchanged.
- Legacy non-task `/api/tasks/*` paths remain frozen transitional contracts.
- No domain URL aliases are added.
- No endpoint is deleted, moved, renamed, consolidated or split.
- No request binding, response envelope, response generic type or TypeScript shape changes.
- No permission behavior changes.
- No frontend route changes.

Stable architecture rules:

- No service is extracted.
- No route migration is implemented or approved.
- No gateway/auth/config-store/data-ingest/Nacos/Sentinel implementation is started.
- No transition host is declared final architecture.
- No legacy `/api/tasks/*` namespace is declared final architecture.
- No D001 closure claim is allowed unless the inventory still preserves later human approval gates.

Stable behavior rules:

- No database table, schema or migration changes.
- No Kafka topic, payload or consumer/producer changes.
- No frontend command, display or authority behavior changes.
- No Python workflow, fallback or provenance behavior changes.
- No JSON config mutation.
- No new product feature or new agent work.

Stable governance rules:

- Phase 005 remains the current modular-monolith horizon policy, not permanent architecture.
- Phase 006 remains the frozen contract inventory for legacy mixed-domain `/api/tasks/*` surfaces.
- Phase 007 remains the frontend display/metadata authority guard for current workbench and fallback provenance consumers.

## 8. Required Inventory Shape

`docs/harness/12-transition-host-exit-criteria.md` must include these sections:

- Status and scope.
- Inputs and read-only inspection sources.
- Domain inventory summary table.
- One section each for report, market, risk, strategy, audit, config and workbench.
- One context section for task runtime/control and AI consumers/projection dependencies.
- Cross-domain dependency map.
- Common readiness gate template for later domain phases.
- Domain-specific exit criteria.
- Extraction blockers.
- Explicitly deferred decisions.
- Stop rules for future phases.

Each domain section must include:

- belongs analysis
- authority objects
- current host classification
- read-model surfaces
- command surfaces
- aggregation/display surfaces
- legacy route dependencies
- database tables, JSON files or Kafka topics involved
- main Java controller/service/query/projection files
- frontend consumers
- Python touchpoints, or `none known` if none are found
- current guardrails from Phase 003, Phase 004, Phase 006 or Phase 007
- extraction blockers
- exit criteria
- readiness gates for any later extraction, route migration or permanence decision

The context section must explicitly state that task runtime/control, AI consumers and `AiResultDomainProjectionService` are dependencies for the inventory, not extraction targets selected by Phase 008.

## 9. Allowed New Class / Method Types

No business-code classes or methods are allowed.

Allowed documentation-only structures:

- Markdown sections, tables and checklists.
- Domain inventory matrices.
- Dependency maps in text or Mermaid.
- Exit criteria lists.
- Readiness gate checklists.
- Deferred-decision lists.
- Stop-rule lists.

Allowed scripts/classes/methods:

- None.

Window 2 must not add test classes, source files, scripts, build steps or runtime code in this phase.

## 10. Forbidden Helpers / Adapters / Fallbacks / Bridges

Do not add:

- any production `*Helper`, `*Adapter`, `*Bridge`, `*Facade`, `*Fallback`, `*Resolver`, `*Router`, `*Mapper` or compatibility layer
- any test helper or static guard script
- any API alias or route bridge
- any gateway or proxy bridge
- any frontend API adapter
- any Python fallback bridge
- any config-store bridge
- any temporary service wrapper
- any data migration helper
- any new ingestion adapter
- any new audit/config synchronization bridge

The Phase 008 inventory may document existing adapters or fallback paths as current facts, but it must not create new ones or approve them as target architecture.

## 11. Acceptance Conditions

Phase 008 is acceptable only if all conditions hold:

- `docs/harness/12-transition-host-exit-criteria.md` exists and is the primary durable inventory.
- `docs/harness/handoffs/phase-008-implementation.md` records exact files changed and verification outcomes.
- The inventory covers report, market, risk, strategy, audit, config and workbench responsibilities inside `ai-orchestration-service`.
- Each in-scope domain records SoT, current host, formal/transition status, read models, commands, aggregation/display surfaces, route dependencies, storage/config/Kafka dependencies, frontend consumers, Python touchpoints, current guardrails, blockers, exit criteria and later readiness gates.
- The inventory explicitly handles task runtime/control, AI status/result/audit consumers and `AiResultDomainProjectionService` as context dependencies, not as extraction targets selected by this phase.
- The inventory preserves Phase 005 modular-monolith policy as current horizon policy only.
- The inventory preserves Phase 006 legacy `/api/tasks/*` contract freeze.
- The inventory preserves Phase 003, Phase 004 and Phase 007 workbench/fallback guardrails.
- The inventory does not select or implement service extraction.
- The inventory does not approve route migration, route aliases, endpoint rename/deletion, gateway/auth, config-store migration, data-ingest split or permanent modular-monolith architecture.
- The inventory does not introduce a new source of truth or reclassify read models, aggregations, frontend fields or fallback provenance as authority.
- No Java, Python, frontend, database, Kafka, `ai-config`, dependency, build-config, deployment, state, debt, backlog or prior handoff file changes occur.
- Any future-phase recommendation is explicitly labeled as deferred and requiring a later Window 0 decision plus human approval.
- `git diff --name-only` shows only allowed Phase 008 documentation files as Window 2 changes, aside from pre-existing unrelated dirty files that are clearly excluded from the Window 2 claim.

## 12. Required Verification Commands

Window 2 must run from `D:\projects\bussiness`:

```powershell
git diff --name-only
```

Expected result: only allowed Phase 008 docs should be part of the Window 2 change claim. Existing unrelated dirty files must not be reverted or bundled into the Phase 008 implementation claim.

Window 2 must run:

```powershell
Test-Path docs/harness/12-transition-host-exit-criteria.md
```

Expected result: `True`.

Window 2 must run:

```powershell
rg -n "report|market|risk|strategy|audit|config|workbench|SoT|read-model|command surface|legacy route|extraction blocker|exit criteria|readiness gate|Phase 005|Phase 006|Phase 007" docs/harness/12-transition-host-exit-criteria.md docs/harness/handoffs/phase-008-implementation.md
```

Expected result: the durable inventory and implementation handoff contain the required domain coverage, guardrail references and readiness-gate language.

Window 2 must run:

```powershell
rg -n "service extraction|route migration|breaking change|gateway/auth|Nacos|Sentinel|database schema|Kafka|frontend|Python|business code|new feature|permanent modular" docs/harness/12-transition-host-exit-criteria.md docs/harness/handoffs/phase-008-implementation.md
```

Expected result: any matches are in out-of-scope, blocker, prerequisite, deferred-decision or future-phase sections, not in completed implementation claims.

Window 2 must run or record these read-only inventory checks from `D:\projects\bussiness`:

```powershell
rg -n "@RequestMapping|@GetMapping|@PostMapping|@PutMapping|@DeleteMapping|requirePermission" quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller
```

```powershell
rg -n "KafkaListener|ai\\.task|market\\.event|risk\\.warning|strategy\\.signal|report\\.generated|notification\\.dispatch" quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator quant-ai-platform/quant-ai-engine/app
```

```powershell
rg -n "class .*DO|@TableName|interface .*Mapper" quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator
```

```powershell
rg -n "export interface|export function|/api/tasks|/api/research/tasks" quant-ui/src/api/task.ts quant-ui/src/types/task.ts
```

```powershell
rg --files quant-ai-platform/ai-config
```

Expected result: Window 2 uses these read-only outputs to support the inventory. If an output reveals a domain dependency that is not covered, the inventory must add it or record it as a blocker.

Maven, npm and Python verification commands are not required because business code, frontend code, Python code and test code are forbidden in this phase. If Window 2 touches any such area, it must stop as blocked instead of validating with builds.

## 13. Blocker Stop Rules

Window 2 must stop and write a blocker in `docs/harness/handoffs/phase-008-implementation.md` if any of these become necessary:

- Changing any URL path, HTTP method, endpoint owner, request binding, response envelope, response type or permission behavior.
- Adding a route alias, compatibility bridge, gateway proxy or service wrapper.
- Moving any code from `ai-orchestration-service` into another service.
- Creating or modifying Java, Python, frontend, database, Kafka, config, dependency, test or deployment files.
- Changing DTO, VO, entity, mapper, schema, topic, payload or API type shapes.
- Reclassifying a read model, workbench aggregation, frontend display field or fallback provenance as authority.
- Declaring `ai-orchestration-service` or legacy `/api/tasks/*` paths to be final architecture.
- Closing D001 completely without preserving later human approval gates.
- Selecting a service extraction, route migration, gateway/auth, config-store migration, data-ingest split or permanent modular-monolith outcome.
- Needing code behavior changes to make the inventory true.
- Finding that current code has a transition-host responsibility outside report, market, risk, strategy, audit, config, workbench or the context-only task/AI dependencies, and that responsibility cannot be safely documented without changing the approved phase scope.
- Needing human approval for breaking changes, service extraction, route migration, config-store migration, gateway/auth implementation or new product features.

When blocked, Window 2 must:

1. Stop edits at the smallest reversible point.
2. Record the exact file, domain, route, dependency or decision point that forced the blocker.
3. State which acceptance condition cannot be met.
4. State the smallest future phase or human decision required to unblock.
5. Ask for human decision instead of expanding scope.

## Window 2 Shape

Use one docs-only architecture/governance Window 2 implementation window.

Do not partition into backend, frontend or Python implementers. Do not start service extraction, route migration, gateway/auth work, config migration, data-ingest split, test implementation or product feature work. Do not proceed until the user approves this Phase 008 architect handoff.

## Human Approval Request

Please approve this Phase 008 architect handoff before Window 2 starts.

Default approval meaning:

- No breaking changes.
- Keep all URL paths and frontend routes stable.
- No business behavior change.
- No new feature work.
- Window 2 may perform docs-only architecture/governance work inside the allowed file boundaries above.
