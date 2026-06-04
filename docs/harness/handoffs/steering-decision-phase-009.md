# Steering Decision - Phase 009

## Status

Window: Window 0 - Steering.

Decision: Phase 009 approved by user for Window 1 architecture planning.

Human approval status: approved.

This file does not approve implementation, does not authorize business code change, and does not start Window 2. Window 1 must produce `docs/harness/handoffs/phase-009-architect.md`, and the user must approve that handoff before any implementation window starts.

## Inputs Read

Fixed harness artifacts:

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
- `docs/harness/handoffs/phase-000-harness-baseline.md`

Handoff directory was listed.

Final handoffs discovered:

- `docs/harness/handoffs/phase-001-final.md`
- `docs/harness/handoffs/phase-002-final.md`
- `docs/harness/handoffs/phase-003-final.md`
- `docs/harness/handoffs/phase-004-final.md`
- `docs/harness/handoffs/phase-005-final.md`
- `docs/harness/handoffs/phase-006-final.md`
- `docs/harness/handoffs/phase-007-final.md`
- `docs/harness/handoffs/phase-008-final.md`

Latest phase consumed:

- Phase 008 - Transition Host Exit Criteria Inventory.

Matching Phase 008 handoffs consumed:

- `docs/harness/handoffs/steering-decision-phase-008.md`
- `docs/harness/handoffs/phase-008-architect.md`
- `docs/harness/handoffs/phase-008-implementation.md`
- `docs/harness/handoffs/phase-008-review.md`
- `docs/harness/handoffs/phase-008-final.md`

Additional durable inventory consumed:

- `docs/harness/12-transition-host-exit-criteria.md`

Missing matching handoff files:

- None for Phase 008.

## Startup Recovery Result

`docs/harness/state/current-state.md` records:

- Current phase: none approved.
- Latest frozen phase: Phase 008 - Transition Host Exit Criteria Inventory.
- Last completed phase: Phase 008 - Transition Host Exit Criteria Inventory.
- Open blockers: none registered.
- Candidate next phases: report boundary readiness, market/data-ingest ownership, risk/strategy projection ownership, auth/gateway decision, legacy route migration decision and config store decision.

The handoff directory agrees with the state file: the highest final handoff is `phase-008-final.md`.

Bootstrap Phase 001 is not current fact because Phase 001 through Phase 008 are completed and frozen.

## Current State Summary

- No active phase is approved.
- Phase 008 completed a static transition-host exit criteria inventory and did not approve extraction, route migration, gateway/auth, config-store migration, data-ingest split, permanent modular-monolith status, business behavior change or new feature work.
- `ai-orchestration-service` remains a multi-domain transition host, not final architecture.
- Phase 005 modular-monolith policy remains limited to the current governance horizon.
- Phase 006 frozen legacy non-task `/api/tasks/*` contract inventory remains in force.
- Phase 003, Phase 004 and Phase 007 workbench/fallback authority guardrails remain in force.
- D001 remains open because the transition host still contains multiple domains.
- D002 remains open because the legacy mixed `/api/tasks/*` namespace is guarded but not migrated.
- D003 remains open for future workbench/fallback surfaces.
- Open blockers: none.

## Decision Order Result

Decision order from `docs/harness/10-steering-state-machine.md`:

```text
main path breakage -> authority ambiguity -> contract ambiguity -> transition host reduction -> eval/test coverage -> new feature work
```

Evaluation:

- No main path breakage is registered.
- The highest remaining work is still authority and contract governance inside the `ai-orchestration-service` transition host.
- Phase 008 created a common inventory and readiness template. The next bounded step should apply that template to one high-value domain boundary, not jump directly to service extraction or route migration.
- Report is the best next bounded slice because report facts, evidence, versions, review commands, frontend report consumers and AI projection output sit on the main product path, have clear Phase 008 exit criteria, and can be hardened without breaking URLs or changing runtime behavior.
- Risk/strategy projection ownership is the fallback because it is also authority-critical, but it spans two domains plus shared projection and strategy command behavior, making it slightly broader and riskier than the report boundary readiness slice.

## Candidate Score Table

Scores are 0-2. Behavior risk is inverted: 2 means low risk.

| Candidate | Main path protection | Authority clarity | Contract clarity | Transition host reduction | Behavior risk | Verification feasibility | Total | Steering result |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| Phase 009 - Report Boundary Readiness | 2 | 2 | 2 | 1 | 2 | 2 | 11 | Primary |
| Risk/Strategy Projection Ownership Boundary | 2 | 2 | 2 | 1 | 1 | 1 | 9 | Fallback |
| Market Event and Data-Ingest Ownership | 2 | 2 | 1 | 2 | 1 | 1 | 9 | Defer |
| Config Store Decision | 1 | 2 | 1 | 1 | 1 | 1 | 7 | Defer |
| Auth/Gateway Decision | 1 | 2 | 1 | 1 | 1 | 1 | 7 | Defer |
| Generic eval/test expansion | 1 | 1 | 1 | 0 | 2 | 2 | 7 | Defer |
| Legacy route migration decision | 0 | 1 | 2 | 2 | 0 | 0 | 5 | Defer |
| Direct service extraction or permanent modular-monolith declaration | 1 | 1 | 1 | 2 | 0 | 0 | 5 | Defer |
| New feature or new agent work | 0 | 0 | 0 | 0 | 0 | 0 | 0 | Not eligible |

Tie-break:

- Risk/strategy and market/data-ingest both score 9.
- Risk/strategy is selected as fallback because it improves authority and contract boundaries around AI result projection more directly.
- Market/data-ingest is deferred because it contains real ingest, mock/demo ingest, CNINFO proxy, source adapters and config-file dependencies, so it needs a more explicit Window 1 split before it is safe as the next primary.

## Primary Candidate

Phase 009 - Report Boundary Readiness.

Bounded goal:

- Apply the Phase 008 readiness template to the report domain.
- Freeze report-domain belongs, authority and contract boundaries before any later report-service extraction, route migration, permanence decision or report contract reshaping is considered.
- Clarify the boundary among report facts, report evidence, report versions, report review commands, review audit, AI projection writer, fallback provenance metadata and frontend report consumers.
- Add or define guardrails only where Window 1 proves they are needed to prevent report boundary drift.

Why this is the next bounded step:

- Phase 008 says report exit requires report read, evidence, versioning, review command, review audit, projection writer and frontend report consumers to have a single approved target contract or explicitly retained transition path.
- Report is central to the product path but can still be governed without accepting breaking changes.
- It is smaller than extracting a `report-service`, safer than route migration and more concrete than another cross-domain inventory.
- It advances D001 follow-through without declaring `ai-orchestration-service` final architecture.

Expected phase shape:

- Architecture/governance plus focused backend/static guard work only if Window 1 defines a safe scope.
- No production behavior change by default.
- No route migration, alias, endpoint rename or endpoint deletion.
- No DTO/VO/entity/schema/Kafka/frontend/Python/config change.
- No service extraction.

## Fallback Candidate

Risk/Strategy Projection Ownership Boundary.

Fallback condition:

- Use this if the user rejects report boundary readiness or wants the next phase to target shared AI result projection before report-specific governance.

Bounded fallback goal:

- Clarify ownership and guardrails for `AiResultDomainProjectionService` output into `risk_warning`, `risk_warning_detail`, `strategy_signal` and `strategy_signal_factor`.
- Separate risk read-model authority, strategy manual command authority, AI projection writer authority, downstream placeholder topics and report/workbench display summaries.
- Preserve current `/api/tasks/risk-*` and `/api/tasks/strategy-*` contracts unless a later phase explicitly approves a migration.

Why it is not primary:

- It spans two domains and a shared projection path.
- Strategy has manual create/status commands while risk is projection-fed and REST read-only, so Window 1 would need a wider authority split.
- It remains valuable, but report readiness is the narrower first application of the Phase 008 gate template.

## Why Other Phases Are Not Selected

- Phase 001 through Phase 008 are completed and frozen.
- Market event and data-ingest ownership is important, but it mixes real ingest, mock/demo ingest, source adapters, CNINFO proxy, ingest history, market intelligence and event-source config. That is larger than the next bounded report slice.
- Auth/gateway decision is deferred because header-based demo auth is registered transition debt, but no production auth requirement or main-path breakage is currently registered.
- Legacy route migration is deferred because it is likely a breaking-change or compatibility-path decision, and Phase 006 intentionally froze current URLs.
- Config store decision is deferred because JSON config is current transition debt, but moving or choosing DB/Nacos would affect both Java and Python config readers and needs a separate human-approved migration phase.
- Generic eval/test expansion should attach to a bounded authority/contract phase instead of becoming a standalone phase while D001/D002 remain open.
- Direct service extraction, route migration or permanent modular-monolith declaration is too far. Phase 008 created readiness gates; it did not satisfy them.
- New features and new agents remain ineligible while authority, contract and transition-host lifecycle work remain open.

## Window 1 Must Define

Window 1 must turn the primary candidate into an implementation-ready handoff and define:

- Exact report-domain scope: report detail, report versions, report comparison, report center, review logs, review stats, report evidence and report review command.
- Whether `AiResultDomainProjectionService` is in scope only as a report/evidence projection dependency or as a file that may receive guard/test changes.
- Whether task runtime, audit compliance, risk/strategy projections, market context and config are context-only dependencies or in-scope files.
- Authoritative report objects: `research_report`, `research_report_version`, `research_report_section`, `report_evidence_ref`, and review/audit records as applicable.
- Which surfaces are read-models, commands, aggregation/display, metadata-only fallback provenance or frontend display consumers.
- Exact legacy route inventory to preserve, including `/api/tasks/{taskId}/report*`, `/api/tasks/report-center*`, `/api/tasks/report-review-stats` and review command paths.
- Contract constraints for URL path, HTTP method, request binding, response envelope, response type, permission behavior and frontend TypeScript shape.
- Guardrails needed to keep `reportMeta.contextSnapshot`, fallback provenance, frontend report display fields and workbench latest insight from becoming report SoT.
- Acceptance conditions for belongs, authority, contract and behavior.
- Verification commands, likely including source/static checks and backend tests if Window 1 allows test-only guard work.
- Stop rules if implementation appears to require route migration, breaking changes, service extraction, DTO/VO/entity/schema/Kafka/frontend/Python/config changes or business behavior changes.

Window 1 must also state default approval constraints:

- No breaking changes.
- Keep URL paths stable.
- No business behavior change.
- No new feature work.
- Expected Window 2 type: backend/docs governance or test-only guard work only if Window 1 explicitly scopes it; otherwise docs-only.

## Explicitly Out Of Scope

- No business code change from Window 0.
- No Window 2 implementation before Window 1 and human approval.
- No report-service extraction.
- No route migration, route alias, endpoint rename, endpoint deletion or endpoint consolidation.
- No breaking change.
- No change to DTO, VO, entity, mapper, database schema, SQL migration, Kafka topic or Kafka payload.
- No frontend route, API function, TypeScript shape, command behavior or display behavior change by default.
- No Python workflow, fallback, provenance or report generation behavior change.
- No `ai-config` mutation or config-store migration.
- No gateway/auth/Nacos/Sentinel/deployment work.
- No new helper, adapter, bridge, fallback, proxy, wrapper or compatibility layer unless a later Window 1 explicitly identifies and the user approves it.
- No new product feature or new agent work.
- No reclassification of `ai-orchestration-service` or legacy `/api/tasks/*` paths as final architecture.

## Human Approval Request

User approved the Phase 009 direction.

Recorded approval:

- Selected phase: Phase 009 - Report Boundary Readiness.
- No breaking changes.
- Keep URL paths stable.
- No business behavior change.
- No new feature work.
- Expected Window 2 shape after Window 1: docs-only by default, unless Window 1 justifies a narrower backend test/static guard scope and the user approves it.

Window 0 stops here. Window 1 is the next required window and must not start implementation.
