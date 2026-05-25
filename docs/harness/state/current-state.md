# Current Harness State

## Bootstrap Status

Bootstrap Harness Window completed the pre-Window-0 setup.

This file is the starting state for Window 0.

## Current Phase

None approved.

Latest frozen phase: Phase 009 - Report Boundary Readiness.

## Current Phase Status

Phase 005 is completed with residual risk after Window 3 approved `docs/harness/handoffs/phase-005-review.md`.

Window 4 froze the result in `docs/harness/handoffs/phase-005-final.md`.

Phase 005 selected the conservative architecture policy: continue as a modular monolith inside `ai-orchestration-service` for the next governance horizon. This is not a permanent final-architecture declaration, does not approve service extraction, and does not approve route migration or breaking contract changes.

Phase 006 remains completed with residual risk after Window 3 approved `docs/harness/handoffs/phase-006-review-fix-3.md`.

Phase 007 remains completed with residual risk after Window 3 approved `docs/harness/handoffs/phase-007-review.md`.

Window 0 resolved the stale `Last Completed Phase` ordering note by consuming `docs/harness/handoffs/phase-005-final.md`, `docs/harness/handoffs/phase-006-final.md` and `docs/harness/handoffs/phase-007-final.md`.

Phase 008 is completed with residual risk after Window 3 approved `docs/harness/handoffs/phase-008-review.md`.

Window 4 froze the result in `docs/harness/handoffs/phase-008-final.md`.

Phase 008 produced the durable transition-host exit criteria inventory in `docs/harness/12-transition-host-exit-criteria.md`. It defined per-domain SoT, read-model, command, route dependency, guardrail, blocker, exit criteria and readiness-gate facts for report, market, risk, strategy, audit, config and workbench responsibilities inside `ai-orchestration-service`.

Phase 008 was docs-only governance work. It did not approve service extraction, route migration, gateway/auth, config-store migration, data-ingest split, permanent modular-monolith status, business behavior change or new feature work.

Phase 009 is completed with residual risk after Window 3 approved `docs/harness/handoffs/phase-009-review.md`.

Window 4 froze the result in `docs/harness/handoffs/phase-009-final.md`.

Phase 009 produced the durable report boundary readiness artifact in `docs/harness/13-report-boundary-readiness.md`. It applies the Phase 008 readiness template to the report domain and clarifies report facts, report evidence, report versions, report review commands, review audit, AI projection dependency, fallback provenance metadata and frontend report consumers before any later report extraction, route migration or permanence decision is considered.

Phase 009 was docs-only governance work. It did not approve report-service extraction, route migration, route aliases, endpoint rename/deletion/consolidation, gateway/auth, config-store migration, Python behavior change, frontend reshaping, Kafka/database changes, permanent modular-monolith status, business behavior change or new feature work.

## Last Completed Phase

Phase 009 - Report Boundary Readiness.

## Open Blockers

None registered.

## Completed Phase 001 Constraints

- No breaking changes.
- URL paths must remain stable.
- No business behavior change.
- No new feature work.
- Backend-only implementation.
- Window 3 reviewed and approved the implementation.

## Completed Phase 002 Constraints

- No breaking changes.
- URL paths must remain stable.
- No business behavior change.
- No new feature work.
- Backend-only implementation.
- Window 3 first required a fix pass, then approved `phase-002-review-fix-1.md`.
- `mvn -q test` passed from `quant-ai-platform/quant-services`.

## Completed Phase 003 Constraints

- No breaking changes.
- URL paths must remain stable.
- No business behavior change.
- No new feature work.
- Backend-only implementation.
- No executable production logic changed; Phase 003 added production contract comments and source-level backend boundary tests.
- Window 3 reviewed and approved `phase-003-review.md`.
- `mvn -q test` passed from `quant-ai-platform/quant-services`.

## Completed Phase 004 Constraints

- No breaking changes.
- URL paths must remain stable.
- No business behavior change.
- No new feature work.
- Mixed Python/backend scope was approved, but implementation changed only Python production/test files plus the implementation handoff.
- Fallback provenance was added only inside existing Python dictionaries and `reportMeta.contextSnapshot` map metadata.
- No Java production, frontend, DTO/VO/entity, database schema, Kafka topic or top-level Kafka payload field changed.
- Window 3 reviewed and approved `phase-004-review.md`.
- `python -m compileall app`, `python -m unittest discover -s tests` and `mvn -q test` passed.
- `python -m pytest` was unavailable because `pytest` is not installed in the current environment.

## Completed Phase 005 Constraints

- No breaking changes.
- URL paths and HTTP methods remained stable.
- No business behavior change.
- No new feature work.
- Docs-only architecture/policy implementation.
- Selected option: continue as a modular monolith inside `ai-orchestration-service` for the next governance horizon.
- `ai-orchestration-service` remains a transition host, not final architecture.
- Legacy non-task `/api/tasks/*` paths remain frozen transitional contracts under Phase 006, not final architecture.
- No service extraction, route migration, route alias, endpoint rename, gateway/auth implementation, config-store migration, data-ingest split or feature work was approved.
- No Java, Python, frontend, database, Kafka, `ai-config`, dependency, build-config or deployment file changed.
- Phase 003, Phase 004, Phase 006 and Phase 007 guardrails remain in force.
- Window 3 reviewed and approved `phase-005-review.md`.
- Maven, npm and Python verification were not required because Phase 005 changed documentation only.

## Completed Phase 006 Constraints

- No breaking changes.
- URL paths and HTTP methods remained stable.
- No business behavior change.
- No new feature work.
- Backend-focused contract/test implementation only.
- Java production code, controller runtime annotations and executable behavior remained unchanged.
- No frontend, Python, DTO/VO/entity, mapper, database schema, Kafka, `ai-config`, dependency or build-config file changed.
- The approved non-task legacy `/api/tasks/*` endpoint inventory is documented and guarded in backend tests.
- Focused tests guard endpoint path, HTTP method, controller owner, `Result<T>` response envelope, declared generic response type, request binding shape, request-param required/default behavior, explicit permission calls and intentional absence of explicit permission calls.
- Focused tests guard against unapproved `/api/tasks` endpoint additions across controller mapping shapes, including GET, POST, PUT, DELETE, PATCH and method-level `@RequestMapping`.
- Window 3 required three fix passes, then approved `phase-006-review-fix-3.md`.
- `mvn -q test` passed from `quant-ai-platform/quant-services`; output included the existing simulated `kafka down` warning stack trace from `TaskOutboxPublisherServiceTests.publishPendingOnceShouldMarkFailedWhenKafkaSendFails`.

## Completed Phase 007 Constraints

- No breaking changes.
- URL paths and frontend routes remained stable.
- No user-visible business behavior change.
- No new feature work.
- Frontend-focused implementation only.
- Production source changes were comments/JSDoc-style authority notes; the new guard script is not imported by production code.
- `ResearchWorkbenchData` is documented as display-only aggregation and guarded away from command APIs.
- `TaskReportContextSnapshot`, `reportMeta`, `generationMode`, `fallbackReason` and related fallback provenance are documented as display/audit metadata only.
- Workbench output remains display, navigation and existing task-create source-context prefill only.
- Existing frontend API endpoint strings, HTTP methods, function names, call signatures, response envelopes and TypeScript shapes remained unchanged.
- No Java, Python, database, Kafka, `ai-config`, package/dependency or build-config file changed.
- Window 3 reviewed and approved `phase-007-review.md`.
- `node scripts/authority-boundary-check.mjs` and `npm run build` passed from `quant-ui`.

## Completed Phase 008 Constraints

- No breaking changes.
- URL paths and frontend routes remained stable.
- No business behavior change.
- No new feature work.
- Docs-only architecture/governance implementation.
- `docs/harness/12-transition-host-exit-criteria.md` is now the durable inventory for report, market, risk, strategy, audit, config and workbench transition-host responsibilities.
- The inventory records per-domain SoT, current host classification, read-model surfaces, command surfaces, aggregation/display surfaces, legacy route dependencies, storage/config/Kafka dependencies, frontend consumers, Python touchpoints, guardrails, extraction blockers, exit criteria and later readiness gates.
- Task runtime/control, AI status/result/audit consumers, `market.event.standardized` consumption and `AiResultDomainProjectionService` are documented as context dependencies, not as Phase 008 extraction targets.
- Phase 005 modular-monolith horizon policy, Phase 006 legacy `/api/tasks/*` contract freeze and Phase 003/004/007 workbench/fallback authority guardrails remain in force.
- No Java, Python, frontend, database, Kafka, `ai-config`, dependency, build-config, deployment or business runtime file changed.
- Window 3 reviewed and approved `phase-008-review.md`.
- Maven, npm and Python runtime verification were not required because Phase 008 changed documentation only.

## Completed Phase 009 Constraints

- No breaking changes.
- URL paths and frontend routes remained stable.
- No business behavior change.
- No new feature work.
- Docs-only architecture/governance implementation.
- `docs/harness/13-report-boundary-readiness.md` is now the durable report-domain readiness artifact.
- The artifact records report belongs, authority objects, read-model surfaces, command surfaces, version/evidence/review-audit inventories, AI projection dependency, frontend consumers, Python/fallback provenance touchpoints, related display-only surfaces, stable URL/API contracts, inherited guardrails, extraction blockers, route-migration blockers, readiness gates, deferred decisions and stop rules.
- Stable report authority objects remain `research_report`, `research_report_version`, `research_report_section`, `report_evidence_ref`, `research_report_review_log` and `human_review_record`.
- `reportMeta`, raw payload, `contextSnapshot`, fallback provenance, workbench latest insight and frontend display fields remain metadata/display/projection input only, not report source of truth.
- `AiResultDomainProjectionService` remains a current projection dependency and was not moved, split or redesigned.
- Phase 005 modular-monolith horizon policy, Phase 006 legacy `/api/tasks/*` contract freeze, Phase 007 frontend authority guardrails and Phase 008 transition-host readiness template remain in force.
- No Java, Python, frontend, database, Kafka, `ai-config`, dependency, build-config, deployment or business runtime file changed.
- Window 3 reviewed and approved `phase-009-review.md`.
- `node scripts/authority-boundary-check.mjs` passed from `quant-ui`; Maven, npm build and Python runtime verification were not required because Phase 009 changed documentation only.

## Open Architecture Drift

- `ai-orchestration-service` remains a transition host for multiple domains originally planned as separate services. Phase 005 keeps this as the next-governance-horizon modular-monolith policy, not final architecture.
- Phase 008 now documents per-domain exit criteria and readiness gates for the current transition-host responsibilities, but it does not close D001 or approve an ownership move.
- Phase 009 now documents report-specific readiness gates and blockers, but it does not close D001 or approve report ownership movement, extraction, route migration or permanence.
- Gateway/auth/config/service discovery architecture from the original plan is not implemented.

## Open Authority Drift

- Phase 002 moved risk, strategy, report, market-intelligence, audit, config dashboard and workbench read paths out of `TaskQueryServiceImpl` into internal domain query services.
- Phase 003 documented and tested Java backend workbench boundaries: workbench remains display-only aggregation, must not write domain facts, and must not feed backend command/projection authority.
- Phase 004 made in-scope Python fallback provenance auditable for planner, intent, financial, risk, report and market fallback paths using existing metadata surfaces.
- Phase 005 preserved current source-of-truth placement and did not move runtime authority.
- Phase 007 documented and guarded frontend consumer boundaries for current workbench aggregation and fallback provenance surfaces.
- Phase 008 documented per-domain SoT, read-model, command and aggregation boundaries for report, market, risk, strategy, audit, config and workbench transition responsibilities without moving authority.
- Phase 009 documented report-specific authority objects, projection dependencies, review/audit records, fallback provenance boundaries and frontend report consumer limits without moving authority.
- Fallback metadata remains provenance only and must not become model-generated truth or business SoT.
- Future frontend, backend or Python surfaces that expose workbench or fallback metadata must keep equivalent non-authoritative provenance guardrails.

## Open Contract Drift

- Phase 001 split the former multi-domain `TaskQueryController` surface into domain-specific controllers.
- Non-task domain endpoints still keep legacy `/api/tasks/*` paths by approved Phase 001 constraint.
- Workbench Java backend display-only contract is guarded by Phase 003 tests/comments.
- Phase 004 preserved Kafka topics, top-level payload fields, URL paths, frontend contracts, DTO/VO/entity shapes and database schema while adding optional fallback provenance inside existing map metadata.
- Phase 005 preserved all current URLs, HTTP methods, request/response contracts, frontend routes, Kafka topics, database schema and runtime behavior.
- Phase 006 froze the approved legacy non-task `/api/tasks/*` endpoint inventory with backend contract tests. The legacy namespace remains a transition contract and must not drift without an approved phase handoff.
- Phase 007 preserved existing frontend routes, API endpoint strings, HTTP methods, function names, call signatures, response envelopes and TypeScript shapes.
- Phase 008 preserved all runtime contracts and recorded legacy route dependencies in the transition-host inventory without adding route aliases, migrations or endpoint changes.
- Phase 009 preserved all report runtime contracts and recorded the stable report URL/API inventory, frontend route/API inventory, permission behavior and response-shape boundaries without adding route aliases, migrations or endpoint changes.
- Legacy non-task `/api/tasks/*` paths remain transition debt, but the current approved inventory is documented and guarded.
- Future fallback surfaces must continue preserving fallback provenance as non-authoritative metadata.

## Active Transition Hosts

- `ai-orchestration-service`, continued by Phase 005 as the next-governance-horizon modular monolith, inventoried by Phase 008, refined for report readiness by Phase 009 and still not final architecture
- Internal domain query services inside `ai-orchestration-service`
- Legacy `/api/tasks/*` paths for non-task domain surfaces, now frozen as approved transitional contracts by Phase 006
- Research workbench display aggregation
- JSON files under `quant-ai-platform/ai-config`
- Mock/demo ingest paths
- Python fallback path, now audited for Phase 004 in-scope provenance and Phase 007 current frontend consumers but still a transition mechanism

## Candidate Next Phases

No active candidate is approved.

Recommended candidate inputs for Window 0 evaluation:

- Market event and data-ingest ownership phase.
- Risk/strategy projection ownership phase.
- Auth/gateway decision phase.
- Legacy route migration decision phase.
- Config store decision phase.
- Report extraction or report route-migration planning only if Window 0 and the user explicitly choose to act on Phase 009 readiness gates.

Phase 001, Phase 002, Phase 003, Phase 004, Phase 005, Phase 006, Phase 007, Phase 008 and Phase 009 are no longer candidates because they are completed and frozen by Window 4.

Window 0 must score candidates using `docs/harness/10-steering-state-machine.md`, propose exactly one primary candidate and one fallback candidate, and wait for human approval.

## Human Approval Status

Phase 009 was approved by the user after Window 0 steering decision in `docs/harness/handoffs/steering-decision-phase-009.md`.

Phase 009 approval constraints:

- No breaking changes.
- URL paths must remain stable.
- No business behavior change.
- No new feature work.
- Expected Window 2 type is docs-only by default, unless Window 1 justifies a narrower backend test/static guard scope and the user approves it.

Phase 009 was planned by Window 1, implemented by Window 2 as docs-only architecture/governance work, reviewed and approved by Window 3, and frozen by Window 4 as completed with residual risk.

Phase 008 was approved by the user after Window 0 steering decision in `docs/harness/handoffs/steering-decision-phase-008.md`.

Phase 008 approval constraints:

- No breaking changes.
- URL paths must remain stable.
- No business behavior change.
- No new feature work.
- Expected Window 2 type was docs-only architecture/governance work.

Phase 008 was planned by Window 1, implemented by Window 2 as docs-only architecture/governance work, reviewed and approved by Window 3, and frozen by Window 4 as completed with residual risk.

Phase 005 was approved by the user after Window 0 steering decision in `docs/harness/handoffs/steering-decision-phase-005.md`, planned by Window 1, implemented by Window 2 as docs-only architecture/policy work, reviewed and approved by Window 3, and frozen by Window 4 as completed with residual risk.

Phase 005 approval constraints:

- No breaking changes.
- URL paths must remain stable.
- No business behavior change.
- No new feature work.
- Expected Window 2 type was docs-only architecture/policy work.

Phase 001 was approved by the user, implemented by Window 2, reviewed by Window 3 and frozen by Window 4 as completed with residual risk.

Phase 002 was approved by the user after Window 0 steering decision, implemented by Window 2, fixed by Window 2 Fix Pass 1, reviewed and approved by Window 3 Review Fix 1, and frozen by Window 4 as completed with residual risk.

Phase 003 was approved by the user after Window 0 steering decision, implemented by Window 2, reviewed and approved by Window 3, and frozen by Window 4 as completed with residual risk.

Phase 004 was approved by the user after Window 0 steering decision in `docs/harness/handoffs/steering-decision-phase-004.md`, planned by Window 1, implemented by Window 2, reviewed and approved by Window 3, and frozen by Window 4 as completed with residual risk.

Phase 007 was approved by the user after Window 0 steering decision in `docs/harness/handoffs/steering-decision-phase-007.md`, planned by Window 1, implemented by Window 2, reviewed and approved by Window 3, and frozen by Window 4 as completed with residual risk.

Phase 006 was approved by the user after Window 0 steering decision in `docs/harness/handoffs/steering-decision-phase-006.md`, planned by Window 1, implemented by Window 2 with three fix passes, reviewed and approved by Window 3 Review Fix 3, and frozen by Window 4 as completed with residual risk.

Next step must be Window 0. Window 0 must read `docs/harness/handoffs/phase-009-final.md`, discover the matching Phase 009 steering, architect, implementation and review handoffs, consume `docs/harness/13-report-boundary-readiness.md`, score candidate next phases using `docs/harness/10-steering-state-machine.md`, propose exactly one primary candidate and one fallback candidate, and wait for human approval before Window 1 starts.
