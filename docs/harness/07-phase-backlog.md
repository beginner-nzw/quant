# Phase Backlog

## Priority Rules

1. Fix main path breakage.
2. Freeze authority and contract.
3. Return logic to formal host.
4. Reduce transition hosts.
5. Add eval/tests to prevent regression.
6. Add new product features.

## Phase 000 - Bootstrap Harness Baseline

Status: completed by current harness files.

Goal:

- Record current architecture.
- Record authority matrix.
- Record host ownership.
- Record contract risks.
- Record transition lifetime.
- Record debt and eval checklist.
- Encode Window 0 as a constrained state machine plus human approval point.
- Provide prompts for Window 0-4.

No business code changes.

## Next Steering Inputs After Phase 011

Window 0 should evaluate the candidate phases below using `10-steering-state-machine.md`.

Phase 001, Phase 002, Phase 003, Phase 004, Phase 005, Phase 006, Phase 007, Phase 008, Phase 009, Phase 010 and Phase 011 are no longer candidates. They were completed and frozen by Window 4.

Recommended candidate inputs for Window 0 evaluation:

- Config Store Decision Boundary.
- Auth/gateway decision phase.
- Legacy route migration decision phase.
- Risk-service extraction, strategy-service extraction, projection-split planning, risk/strategy route migration or risk/strategy Kafka downstream planning only if Window 0 and the user explicitly choose to act on Phase 011 readiness gates.
- Report extraction or report route-migration planning only if Window 0 and the user explicitly choose to act on Phase 009 readiness gates.
- Market-service extraction, data-ingest-service extraction, market route migration or market config-store planning only if Window 0 and the user explicitly choose to act on Phase 010 readiness gates.

Window 4 does not select the next phase. Window 0 must score and propose exactly one primary candidate and one fallback candidate.

## Phase 001 - Split Controller Surface Inside ai-orchestration-service

Status: completed with residual risk.

Completed in:

- `docs/harness/handoffs/phase-001-architect.md`
- `docs/harness/handoffs/phase-001-implementation.md`
- `docs/harness/handoffs/phase-001-review.md`
- `docs/harness/handoffs/phase-001-final.md`

Goal:

Reduce `TaskQueryController` as a multi-domain API surface without changing behavior.

Scope:

- Split controller class by domain inside `ai-orchestration-service`.
- Keep existing URL paths initially unless a breaking change is explicitly approved.
- Candidate controllers:
  - `TaskController` or `TaskQueryController` for task APIs.
  - `MarketEventController`.
  - `RiskWarningController`.
  - `StrategySignalController`.
  - `ReportController`.
  - `AuditComplianceController`.
  - `ModelAgentConfigController`.
  - `ResearchWorkbenchController`.
- Do not change service internals yet.

Acceptance:

- Completed with no intended endpoint behavior change.
- `mvn -q test` passed from `quant-ai-platform/quant-services` during Window 2 and Window 3.
- New controllers map cleanly to the approved Phase 001 endpoint inventory.
- `TaskQueryController` no longer owns market, risk, strategy, report, market-intelligence, audit, config or workbench endpoint methods.

Residual scope:

- Legacy `/api/tasks/*` paths were intentionally preserved and remain contract debt.
- `TaskQueryServiceImpl` internal read-model responsibilities remain mixed.

## Phase 002 - Split TaskQueryServiceImpl Internal Query Services

Status: completed with residual risk.

Completed in:

- `docs/harness/handoffs/phase-002-architect.md`
- `docs/harness/handoffs/phase-002-implementation.md`
- `docs/harness/handoffs/phase-002-review.md`
- `docs/harness/handoffs/phase-002-fix-1-implementation.md`
- `docs/harness/handoffs/phase-002-review-fix-1.md`
- `docs/harness/handoffs/phase-002-final.md`

Goal:

Reduce mixed read-model ownership inside `TaskQueryServiceImpl`.

Scope:

- Extract internal query services in `ai-orchestration-service`.
- Preserve external contracts.
- Suggested services:
  - `TaskReadModelService`
  - `ReportReadModelService`
  - `RiskWarningReadModelService`
  - `StrategySignalReadModelService`
  - `MarketEventReadModelService`
  - `ResearchWorkbenchReadModelService`
  - `AuditComplianceReadModelService`

Acceptance:

- Completed with no intended external behavior change.
- `mvn -q test` passed from `quant-ai-platform/quant-services` during implementation, fix pass and review.
- `TaskQueryService` and `TaskQueryServiceImpl` now expose task read-model and task trace methods only.
- Risk, strategy, report, market-intelligence, audit compliance, model/agent config dashboard and workbench read paths live in internal domain query services.
- Workbench copied domain read-model entrypoints were removed in Fix Pass 1 and are guarded by a boundary test.

Residual scope:

- `ai-orchestration-service` remains a multi-domain transition host.
- Legacy non-task `/api/tasks/*` paths remain contract debt.
- Workbench/fallback display-only contracts still need stronger guardrails.
- Some private display hydration helpers remain in domain/workbench services to preserve behavior without adding forbidden shared helpers.

## Phase 003 - Contract Hardening for Workbench and Fallback

Status: completed with residual risk.

Completed in:

- `docs/harness/handoffs/phase-003-architect.md`
- `docs/harness/handoffs/phase-003-implementation.md`
- `docs/harness/handoffs/phase-003-review.md`
- `docs/harness/handoffs/phase-003-final.md`

Goal:

Prevent `research-workbench` and fallback merges from becoming SoT.

Scope:

- Add comments/tests around display-only aggregation.
- Add contract tests for workbench output not being used as authoritative task/risk/report/strategy truth.
- Add test coverage for fallback reason propagation if feasible.

Acceptance:

- Completed with no intended external behavior change.
- `mvn -q test` passed from `quant-ai-platform/quant-services` during implementation and review.
- Production Phase 003 changes were comments/Javadoc-style contract notes only.
- Source-level backend boundary tests fail if workbench references move outside the display surface or if workbench aggregation starts writing domain facts or publishing events.
- Workbench remains `GET /api/tasks/research-workbench` display-only aggregation with stable URL, request binding, response envelope and VO shape.
- Existing preferred/fallback selection is documented as display hydration only.

Residual scope:

- Python fallback execution and reason propagation remain Phase 004 candidate work.
- Frontend and Python consumers remain outside Phase 003 backend-only scope.
- Legacy non-task `/api/tasks/*` paths remain contract debt.

## Phase 004 - Python AI Workflow Contract Cleanup

Status: completed with residual risk.

Completed in:

- `docs/harness/handoffs/phase-004-architect.md`
- `docs/harness/handoffs/phase-004-implementation.md`
- `docs/harness/handoffs/phase-004-review.md`
- `docs/harness/handoffs/phase-004-final.md`

Goal:

Separate AI execution fallback from business truth.

Scope:

- Ensure each model fallback carries fallback reason/audit signal.
- Confirm report/risk/financial fallback is visible in result meta or agent audit.
- Do not add new agents yet.

Acceptance:

- Completed with no intended external behavior change.
- Existing Kafka topics, URL paths, frontend contracts, DTO/VO/entity shapes, database schema and top-level Kafka payload fields remained unchanged.
- Planner and intent fallback remain marked as `RULE_FALLBACK` with non-empty fallback reasons and are covered by focused Python tests.
- Financial and risk rule fallback now expose non-empty fallback reasons in existing Python result dictionaries.
- Report `contextSnapshot` now carries report, financial, risk and market fallback provenance as optional metadata.
- Java production projection was not changed; it can inspect provenance through existing `reportMeta.contextSnapshot` / raw payload storage without using it as authority.
- `python -m compileall app`, `python -m unittest discover -s tests` and `mvn -q test` passed during implementation and review.

Residual scope:

- `pytest` is not installed in the current environment.
- Frontend consumer authority boundaries remain outside Phase 004.
- Future fallback surfaces must preserve non-authoritative provenance metadata before they are treated as acceptable transition behavior.

## Phase 005 - Decide Service Split or Continue Modular Monolith

Status: completed with residual risk.

Completed in:

- `docs/harness/handoffs/steering-decision-phase-005.md`
- `docs/harness/handoffs/phase-005-architect.md`
- `docs/harness/handoffs/phase-005-implementation.md`
- `docs/harness/handoffs/phase-005-review.md`
- `docs/harness/handoffs/phase-005-final.md`

Goal:

After internal boundaries stabilize, decide whether to extract domain services.

Options:

1. Continue as modular monolith inside `ai-orchestration-service`.
2. Extract report-service.
3. Extract market-event-service.
4. Extract risk-service / strategy-service.
5. Add gateway/auth first.

Requires human approval.

Acceptance:

- Completed as docs-only architecture/policy work with no business code, test, script, dependency, frontend, Python, Java, database, Kafka, config or deployment change.
- Selected Option 1: continue as a modular monolith inside `ai-orchestration-service` for the next governance horizon.
- Did not reclassify `ai-orchestration-service` as final architecture.
- Did not reclassify legacy non-task `/api/tasks/*` paths as final architecture.
- Preserved all current URLs, HTTP methods, request/response contracts, frontend routes, Kafka topics, database schema and runtime behavior.
- Preserved Phase 006 legacy `/api/tasks/*` contract freeze and Phase 007 frontend authority guardrails.
- Window 3 reviewed and approved `phase-005-review.md`.

Residual scope:

- D001 remains open because `ai-orchestration-service` still hosts multiple domains.
- The modular-monolith policy is limited to the next governance horizon and must be revisited through later phases.
- Future extraction, route migration, gateway/auth work, config-store migration, data-ingest split, service discovery or feature work requires a separate Window 0 decision and human approval.

## Phase 008 - Transition Host Exit Criteria Inventory

Status: completed with residual risk.

Completed in:

- `docs/harness/handoffs/steering-decision-phase-008.md`
- `docs/harness/handoffs/phase-008-architect.md`
- `docs/harness/handoffs/phase-008-implementation.md`
- `docs/harness/handoffs/phase-008-review.md`
- `docs/harness/handoffs/phase-008-final.md`

Goal:

Define per-domain exit gates for report, market, risk, strategy, audit, config and workbench responsibilities inside `ai-orchestration-service`.

Scope:

- Inventory current transition-host responsibilities by domain.
- Record SoT/read-model placement, command surfaces, legacy route dependencies and extraction blockers.
- Preserve Phase 005 modular-monolith policy for the current horizon.
- Preserve Phase 006 legacy `/api/tasks/*` contract freeze.
- Do not choose or implement extraction.

Acceptance:

- Completed as docs-only architecture/governance work with no runtime behavior change.
- Produced `docs/harness/12-transition-host-exit-criteria.md` as the durable transition-host inventory.
- Covered report, market, risk, strategy, audit, config and workbench responsibilities inside `ai-orchestration-service`.
- Recorded per-domain SoT/read-model placement, command surfaces, aggregation/display surfaces, legacy route dependencies, storage/config/Kafka dependencies, frontend consumers, Python touchpoints, guardrails, extraction blockers, exit criteria and readiness gates.
- Treated task runtime/control, AI status/result/audit consumers, `market.event.standardized` consumption and `AiResultDomainProjectionService` as context dependencies, not Phase 008 extraction targets.
- Preserved Phase 005 modular-monolith horizon policy, Phase 006 legacy `/api/tasks/*` contract freeze and Phase 003/004/007 workbench/fallback authority guardrails.
- Did not choose service extraction, route migration, gateway/auth, config-store migration, data-ingest split, permanent modular-monolith status or new feature work.
- No business code, URL, DTO/VO/entity, database schema, Kafka, frontend, Python, config, dependency, build-config or deployment change.
- Window 3 reviewed and approved `phase-008-review.md`.

Residual scope:

- D001 remains open because `ai-orchestration-service` still hosts multiple domains.
- Phase 008 created static governance inventory only; it did not add executable guards.
- Later extraction, permanence, route migration, gateway/auth, config-store, data-ingest, frontend, Python, Kafka or database changes require a new Window 0 decision and human approval.

## Phase 009 - Report Boundary Readiness

Status: completed with residual risk.

Completed in:

- `docs/harness/handoffs/steering-decision-phase-009.md`
- `docs/harness/handoffs/phase-009-architect.md`
- `docs/harness/handoffs/phase-009-implementation.md`
- `docs/harness/handoffs/phase-009-review.md`
- `docs/harness/handoffs/phase-009-final.md`

Goal:

Apply the Phase 008 readiness template to the report domain before any later report-service extraction, route migration, permanence decision or report contract reshaping is considered.

Scope:

- Produce `docs/harness/13-report-boundary-readiness.md` as the durable report-domain readiness artifact.
- Clarify report belongs, authority objects, read-model surfaces, command surfaces, version/evidence/review-audit inventories, AI projection dependency, frontend consumers, Python/fallback provenance touchpoints and related display-only surfaces.
- Preserve Phase 005 modular-monolith horizon policy, Phase 006 legacy `/api/tasks/*` contract freeze, Phase 007 frontend authority guardrails and Phase 008 transition-host readiness template.
- Do not choose or implement report extraction, route migration, endpoint aliases, gateway/auth, config-store migration, Python behavior change, frontend reshaping, Kafka/database changes or new feature work.

Acceptance:

- Completed as docs-only architecture/governance work with no runtime behavior change.
- Produced `docs/harness/13-report-boundary-readiness.md` as the durable report boundary readiness artifact.
- Recorded stable report authority objects: `research_report`, `research_report_version`, `research_report_section`, `report_evidence_ref`, `research_report_review_log` and `human_review_record`.
- Recorded that `reportMeta`, raw payload, `contextSnapshot`, fallback provenance, workbench latest insight and frontend display fields are not report SoT.
- Treated `AiResultDomainProjectionService` as a current projection dependency, not a moved or redesigned owner.
- Preserved all report URLs, HTTP methods, request bindings, response envelopes, permission behavior, frontend routes, frontend API functions and TypeScript shapes.
- No Java, Python, frontend, database, Kafka, `ai-config`, dependency, build-config, deployment or business runtime file changed.
- Window 3 reviewed and approved `phase-009-review.md`.
- Existing `node scripts/authority-boundary-check.mjs` passed from `quant-ui`; Maven, npm build and Python runtime verification were not required because Phase 009 changed documentation only.

Residual scope:

- D001 remains open because `ai-orchestration-service` still hosts multiple domains.
- D002 remains open because report and other non-task domain surfaces still use frozen legacy `/api/tasks/*` routes.
- Phase 009 created static report governance/readiness documentation only; it did not add executable guardrails beyond relying on existing Phase 006 and Phase 007 guards.
- Later report extraction, route migration, projection split, auth/gateway work, config-store migration, frontend/Python reshaping, Kafka/database changes, permanent modular-monolith decisions or new report features require a new Window 0 decision and human approval.

## Phase 010 - Market Event and Data-Ingest Ownership Boundary

Status: completed with residual risk.

Completed in:

- `docs/harness/handoffs/steering-decision-phase-010.md`
- `docs/harness/handoffs/phase-010-architect.md`
- `docs/harness/handoffs/phase-010-implementation.md`
- `docs/harness/handoffs/phase-010-review.md`
- `docs/harness/handoffs/phase-010-final.md`

Goal:

Apply the Phase 008 readiness template to the market event and data-ingest boundary before any later market-service extraction, data-ingest-service extraction, route migration, config-store migration, Kafka/database change, frontend reshaping, Python behavior change or permanence decision is considered.

Scope:

- Produce `docs/harness/14-market-data-ingest-boundary-readiness.md` as the durable market/data-ingest boundary readiness artifact.
- Clarify market/data-ingest belongs, authority objects, read-model surfaces, command surfaces, event source config and ingest history facts, source sync/preview/diagnose, CNINFO proxy, mock/demo ingest, market intelligence, Kafka context, auto-trigger dependencies, frontend market consumers and Python market context/fallback provenance.
- Preserve Phase 005 modular-monolith horizon policy, Phase 006 legacy `/api/tasks/*` contract freeze, Phase 007 frontend authority guardrails, Phase 008 transition-host readiness template and Phase 009 report readiness gates.
- Do not choose or implement market-service extraction, data-ingest-service extraction, route migration, endpoint aliases, gateway/auth, config-store migration, Python behavior change, frontend reshaping, Kafka/database changes or new feature work.

Acceptance:

- Completed as docs-only architecture/governance work with no runtime behavior change.
- Produced `docs/harness/14-market-data-ingest-boundary-readiness.md` as the durable market/data-ingest boundary readiness artifact.
- Recorded stable market/data-ingest authority objects: `market_event`, `market_event_relation`, `market_event_analysis`, `event-source-configs.json` and `event-ingest-histories.json`.
- Recorded that market intelligence, source preview/diagnose output, CNINFO proxy output, mock/demo source payloads, Python fallback snapshots, workbench fields and frontend display/import-preview state are not market SoT unless selected data is persisted through existing approved market authority paths.
- Treated `market.event.standardized`, `MarketEventStandardizedPublisherService`, `MarketEventStandardizedConsumer` and market event auto-trigger behavior as current context dependencies, not moved or redesigned owners.
- Preserved all market URLs, HTTP methods, request bindings, response envelopes, permission behavior, frontend routes, frontend API functions, TypeScript shapes, JSON config/file facts, Kafka context and Python backend-client paths.
- No Java, Python, frontend, database, Kafka, `ai-config`, dependency, build-config, deployment or business runtime file changed.
- Window 3 reviewed and approved `phase-010-review.md`.
- Existing `node scripts/authority-boundary-check.mjs` passed from `quant-ui`; Maven, npm build and Python runtime verification were not required because Phase 010 changed documentation only.

Residual scope:

- D001 remains open because `ai-orchestration-service` still hosts multiple domains.
- D002 remains open because market and other non-task domain surfaces still use frozen legacy `/api/tasks/*` routes.
- D003 remains open for future workbench/fallback/preview/display metadata surfaces.
- D007 remains open because event source, ingest history and auto-trigger files remain JSON/file-backed transition facts.
- D009 remains open because no independent data-ingest-service exists and no ownership move or split was approved.
- Phase 010 created static market/data-ingest governance/readiness documentation only; it did not add executable guardrails beyond relying on existing Phase 006 and Phase 007 guards.
- Later market-service extraction, data-ingest-service extraction, route migration, config-store migration, source adapter/CNINFO proxy redesign, auth/gateway work, frontend/Python reshaping, Kafka/database changes, permanent modular-monolith decisions or new market features require a new Window 0 decision and human approval.

## Phase 011 - Risk/Strategy Projection Ownership Boundary

Status: completed with residual risk.

Completed in:

- `docs/harness/handoffs/steering-decision-phase-011.md`
- `docs/harness/handoffs/phase-011-architect.md`
- `docs/harness/handoffs/phase-011-implementation.md`
- `docs/harness/handoffs/phase-011-review.md`
- `docs/harness/handoffs/phase-011-final.md`

Goal:

Apply the Phase 008 readiness template to risk warning and strategy signal responsibilities before any later risk-service extraction, strategy-service extraction, projection split, route migration, Kafka downstream redesign, gateway/auth change, config-store migration, frontend/Python reshaping, database change or permanence decision is considered.

Scope:

- Produce `docs/harness/15-risk-strategy-projection-boundary-readiness.md` as the durable risk/strategy projection ownership boundary readiness artifact.
- Clarify risk/strategy belongs, authority objects, read-model surfaces, strategy command surfaces, `AiResultDomainProjectionService`, generated domain-event publication, frontend risk/strategy consumers and Python risk/strategy context/fallback provenance.
- Preserve Phase 005 modular-monolith horizon policy, Phase 006 legacy `/api/tasks/*` contract freeze, Phase 007 frontend authority guardrails, Phase 008 transition-host readiness template, Phase 009 report readiness gates and Phase 010 market/data-ingest readiness gates.
- Do not choose or implement risk-service extraction, strategy-service extraction, projection splitting, route migration, endpoint aliases, gateway/auth, config-store migration, Python behavior change, frontend reshaping, Kafka/database changes, Redis changes or new feature work.

Acceptance:

- Completed as docs-only architecture/governance work with no runtime behavior change.
- Produced `docs/harness/15-risk-strategy-projection-boundary-readiness.md` as the durable risk/strategy projection ownership boundary readiness artifact.
- Recorded stable risk authority objects: `risk_warning` and `risk_warning_detail`.
- Recorded stable strategy authority objects: `strategy_signal` and `strategy_signal_factor`.
- Recorded that report risk points, report highlights, workbench summaries, market intelligence rows, Python risk/strategy context, fallback provenance, generated Kafka messages, frontend local state and dashboard cards are not risk/strategy SoT unless selected data is persisted through existing approved projection or command paths.
- Treated `AiResultDomainProjectionService` as a current shared projection dependency, not a moved, split, renamed or redesigned owner.
- Treated `TaskDomainEventPublisherService`, `risk.warning.generated` and `strategy.signal.generated` as current generated-event dependencies, not redesigned downstream architecture and not replacement source of truth.
- Preserved all risk/strategy URLs, HTTP methods, request bindings, response envelopes, response types, permission behavior, frontend routes, frontend API functions, TypeScript shapes, Redis context, Kafka generated-event context and Python backend-client paths.
- No Java, Python, frontend, database, Redis, Kafka, `ai-config`, dependency, build-config, deployment or business runtime file changed.
- Window 3 reviewed and approved `phase-011-review.md`.
- Existing `node scripts/authority-boundary-check.mjs` passed from `quant-ui`; Maven, npm build and Python runtime verification were not required because Phase 011 changed documentation only.

Residual scope:

- D001 remains open because `ai-orchestration-service` still hosts multiple domains.
- D002 remains open because risk/strategy and other non-task domain surfaces still use frozen legacy `/api/tasks/*` routes.
- D003 remains open for future workbench/fallback/preview/display/generated-event metadata surfaces.
- D007 remains open because JSON-backed config remains a transition fact used by related permission/config decisions.
- D008 remains open because header-based demo auth remains a transition fact and current strategy command permission still depends on current role-access behavior.
- Phase 011 created static risk/strategy governance/readiness documentation only; it did not add executable guardrails beyond relying on existing Phase 006 and Phase 007 guards.
- Later risk-service extraction, strategy-service extraction, projection split, route migration, Kafka downstream redesign, gateway/auth work, config-store migration, frontend/Python reshaping, database/Redis changes, permanent modular-monolith decisions or new risk/strategy features require a new Window 0 decision and human approval.

## Phase 006 - Legacy `/api/tasks/*` Contract Freeze for Non-Task Domains

Status: completed with residual risk.

Completed in:

- `docs/harness/handoffs/steering-decision-phase-006.md`
- `docs/harness/handoffs/phase-006-architect.md`
- `docs/harness/handoffs/phase-006-implementation.md`
- `docs/harness/handoffs/phase-006-review.md`
- `docs/harness/handoffs/phase-006-fix-1-implementation.md`
- `docs/harness/handoffs/phase-006-review-fix-1.md`
- `docs/harness/handoffs/phase-006-fix-2-implementation.md`
- `docs/harness/handoffs/phase-006-review-fix-2.md`
- `docs/harness/handoffs/phase-006-fix-3-implementation.md`
- `docs/harness/handoffs/phase-006-review-fix-3.md`
- `docs/harness/handoffs/phase-006-final.md`

Goal:

Declare and guard the approved legacy `/api/tasks/*` non-task domain contracts so D002 stops drifting while URL stability remains required.

Scope:

- Document which non-task domain surfaces are intentionally preserved under `/api/tasks/*`.
- Add focused contract tests or mapping assertions that prevent accidental URL, method, permission, request binding or response-envelope drift.
- Do not introduce new URL aliases or breaking route moves unless explicitly approved by the user in a future steering decision.

Acceptance:

- Completed with no runtime or user-visible behavior change.
- Existing legacy paths remain stable and explicitly documented as approved transitional contracts.
- Backend tests fail if non-task domain endpoints move, disappear, change HTTP method, change controller ownership, change `Result<T>` response envelope or declared generic response type, drift request binding, drift permission behavior or appear without updating the approved inventory.
- Tests guard `/api/tasks` mappings across current controller mapping shapes, including GET, POST, PUT, DELETE, PATCH, method-level `@RequestMapping`, full method-level paths and normalized base/method path combinations.
- No Java production, frontend, DTO/VO/entity, database schema, Kafka, Python, `ai-config`, dependency or business behavior change.
- `mvn -q test` passed from `quant-ai-platform/quant-services` during implementation/fix passes and final review; output included the existing simulated `kafka down` warning stack trace.

Residual scope:

- Legacy non-task `/api/tasks/*` paths remain transitional namespace debt, though the current approved inventory is now frozen.
- Source-level permission and mapping guards rely on current controller package/source conventions.
- Any later approved endpoint declaration or permission style change must deliberately update the Phase 006 inventory and tests.

## Phase 007 - Frontend Consumer Authority Boundary Audit

Status: completed with residual risk.

Completed in:

- `docs/harness/handoffs/steering-decision-phase-007.md`
- `docs/harness/handoffs/phase-007-architect.md`
- `docs/harness/handoffs/phase-007-implementation.md`
- `docs/harness/handoffs/phase-007-review.md`
- `docs/harness/handoffs/phase-007-final.md`

Goal:

Keep frontend and display consumers from treating workbench aggregation, fallback provenance or legacy mixed-domain response data as business SoT.

Scope:

- Audit frontend API consumers for workbench, report, risk, strategy, market and AI result/fallback metadata surfaces.
- Document which frontend surfaces are display-only consumers of Java projections or Python provenance metadata.
- Add focused tests or static assertions where feasible to prevent frontend code from promoting display metadata into command, projection or source-of-truth behavior.
- Preserve all existing URLs, response envelopes, route behavior and user-visible business behavior unless a later Window 0 decision explicitly approves otherwise.

Acceptance:

- Completed with no intended user-visible business behavior change.
- Frontend consumer authority boundaries are documented and guarded for the in-scope workbench and fallback provenance surfaces.
- Workbench output remains display, navigation and existing task-create source-context prefill only.
- Fallback provenance remains display/audit metadata only and is guarded away from frontend command/review authority.
- Existing routes, API endpoint strings, HTTP methods, function names, call signatures, response envelopes and TypeScript shapes remained unchanged.
- No new product feature, backend contract change, DTO/VO/entity change, database schema change, Kafka change, Java change, Python change, package/dependency change or build-config change.
- `node scripts/authority-boundary-check.mjs` and `npm run build` passed from `quant-ui`.

Residual scope:

- The frontend static guard is a focused source check, not a full TypeScript AST policy engine.
- Future workbench or fallback provenance surfaces must add equivalent non-authoritative guardrails.
- Legacy non-task `/api/tasks/*` paths are now guarded by Phase 006 but remain transition debt until a later explicit route migration or architecture decision.

## Current Rule

Do not start any implementation directly from this backlog. Window 0 must select the phase candidate, and the user must approve it before Window 1 starts.
