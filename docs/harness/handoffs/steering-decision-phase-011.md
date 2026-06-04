# Steering Decision - Phase 011

## Status

Window: Window 0 - Steering.

Decision: Phase 011 approved by user for Window 1 architecture planning.

Human approval status: approved.

This file does not approve implementation and does not start Window 1. Window 1 is the next required window after this approval, and it must produce a Phase 011 architect handoff before any implementation window can start.

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
- `docs/harness/handoffs/phase-009-final.md`
- `docs/harness/handoffs/phase-010-final.md`

Latest phase consumed:

- Phase 010 - Market Event and Data-Ingest Ownership Boundary.

Matching Phase 010 handoffs consumed:

- `docs/harness/handoffs/steering-decision-phase-010.md`
- `docs/harness/handoffs/phase-010-architect.md`
- `docs/harness/handoffs/phase-010-implementation.md`
- `docs/harness/handoffs/phase-010-review.md`
- `docs/harness/handoffs/phase-010-final.md`

Additional durable artifacts consumed because `current-state.md` requires Window 0 to consume the Phase 008/009/010 artifacts before scoring:

- `docs/harness/12-transition-host-exit-criteria.md`
- `docs/harness/13-report-boundary-readiness.md`
- `docs/harness/14-market-data-ingest-boundary-readiness.md`

Missing matching handoff files:

- None for Phase 010.

Phase 011 discovery:

- `docs/harness/handoffs/phase-011-final.md` does not exist.
- `docs/harness/handoffs/steering-decision-phase-011.md` did not exist before this file was written.

## Startup Recovery Result

`docs/harness/state/current-state.md` records:

- Current phase: none approved.
- Latest frozen phase: Phase 010 - Market Event and Data-Ingest Ownership Boundary.
- Last completed phase: Phase 010 - Market Event and Data-Ingest Ownership Boundary.
- Open blockers: none registered.
- Candidate next phases: risk/strategy projection ownership, auth/gateway decision, legacy route migration decision, config store decision, report extraction or report route-migration planning only if Window 0 and the user explicitly choose to act on Phase 009 readiness gates, and market-service extraction, data-ingest-service extraction, market route migration or market config-store planning only if Window 0 and the user explicitly choose to act on Phase 010 readiness gates.

The handoff directory agrees with the state file: the highest final handoff is `phase-010-final.md`.

Bootstrap Phase 001 is not current fact because Phase 001 through Phase 010 are completed and frozen by Window 4.

## Current State Summary

- No active phase is approved.
- Phase 010 completed docs-only market/data-ingest boundary readiness and did not approve market-service extraction, data-ingest-service extraction, route migration, route aliases, endpoint rename/deletion/consolidation, gateway/auth, config-store migration, database/schema changes, Kafka changes, frontend reshaping, Python behavior change, permanent modular-monolith status, business behavior change or new feature work.
- `ai-orchestration-service` remains a multi-domain transition host, not final architecture.
- Phase 005 modular-monolith policy remains limited to the current governance horizon.
- Phase 006 frozen legacy non-task `/api/tasks/*` contract inventory remains in force.
- Phase 007 frontend authority guardrails remain in force.
- Phase 008 common transition-host exit criteria remain in force.
- Phase 009 report-specific readiness gates remain in force.
- Phase 010 market/data-ingest-specific readiness gates remain in force.
- D001 remains open because `ai-orchestration-service` still hosts multiple domains.
- D002 remains open because legacy non-task `/api/tasks/*` contracts are guarded but not migrated.
- D003 remains open for future workbench/fallback/preview/display metadata surfaces.
- D007 remains open because JSON/file-backed config remains a transition fact.
- D008 remains open because header-based demo auth remains a transition fact.
- D009 remains open because no independent data-ingest-service exists and no ownership move or split was approved.
- Open blockers: none.

## Decision Order Result

Decision order from `docs/harness/10-steering-state-machine.md`:

```text
main path breakage -> authority ambiguity -> contract ambiguity -> transition host reduction -> eval/test coverage -> new feature work
```

Evaluation:

- No main path breakage is registered.
- The highest remaining work is authority and contract governance inside the `ai-orchestration-service` transition host.
- Phase 008 identified risk and strategy as transition-host domains that still need projection, command, read-model and display boundaries before any extraction or route work.
- Phase 009 report readiness records that `AiResultDomainProjectionService` writes report, evidence, risk and strategy facts from one result projection path, and defers splitting or retaining that projection owner to a later phase.
- Phase 010 market/data-ingest readiness records that risk and strategy may consume or display market context, but their facts remain under `risk_warning`, `risk_warning_detail`, `strategy_signal` and `strategy_signal_factor`.
- Risk/strategy is the next bounded authority problem because it sits directly on the AI result main path, crosses two domain fact sets, and combines read-only risk projection with strategy manual create/status commands.
- This is narrower and safer than service extraction, route migration, config-store migration or gateway/auth implementation because Phase 011 can classify ownership, contracts, blockers and readiness gates without moving code or changing behavior.
- Config store decision is the fallback because JSON-backed config and role access remain transition authority, but it is broader across Java readers, Python readers, audit, role access and event source config.

## Candidate Score Table

Scores are 0-2. Behavior risk is inverted: 2 means low risk.

| Candidate | Main path protection | Authority clarity | Contract clarity | Transition host reduction | Behavior risk | Verification feasibility | Total | Steering result |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| Phase 011 - Risk/Strategy Projection Ownership Boundary | 2 | 2 | 2 | 1 | 2 | 1 | 10 | Primary |
| Config Store Decision Boundary | 1 | 2 | 2 | 1 | 1 | 1 | 8 | Fallback |
| Auth/Gateway Permission Authority Decision | 1 | 2 | 1 | 1 | 1 | 1 | 7 | Defer |
| Generic eval/static guard expansion | 1 | 1 | 1 | 0 | 2 | 2 | 7 | Defer by decision order |
| Report extraction or report route-migration planning | 1 | 1 | 2 | 2 | 0 | 0 | 6 | Defer |
| Market-service/data-ingest extraction, route or config planning | 1 | 1 | 2 | 2 | 0 | 0 | 6 | Defer |
| Legacy route migration decision | 0 | 1 | 2 | 2 | 0 | 0 | 5 | Defer |
| Direct service extraction or permanent modular-monolith declaration | 1 | 1 | 1 | 2 | 0 | 0 | 5 | Defer |
| New feature or new agent work | 0 | 0 | 0 | 0 | 0 | 0 | 0 | Not eligible |

Tie-break notes:

- Risk/strategy wins because it improves authority and contract clarity before lower-order transition reduction and feature work.
- Config store scores next because JSON config and role access are still transition facts, but a config decision touches many readers and audit paths.
- Auth/gateway remains authority-relevant, but current harness records no production auth requirement or main-path breakage, and config/role truth remains unsettled.

## Primary Candidate

Phase 011 - Risk/Strategy Projection Ownership Boundary.

Bounded goal:

- Apply the Phase 008 readiness template to risk warning and strategy signal responsibilities.
- Clarify belongs, authority and contract boundaries for risk projection, risk read models, strategy AI projection, strategy manual create command, strategy status command, strategy factor queries, downstream placeholder topics, report/workbench/market display references, frontend consumers and Python AI context.
- Decide whether `AiResultDomainProjectionService` is explicitly retained as the current projection dependency for this bounded horizon or must be split only by a later human-approved phase.
- Define readiness gates before any later risk-service extraction, strategy-service extraction, projection split, route migration, Kafka downstream event activation, gateway/auth change, frontend/Python reshaping, database change or permanence decision.

Why this is the next bounded step:

- Phase 009 and Phase 010 both depend on risk/strategy staying separate from report and market display context.
- Risk and strategy are still unprocessed Phase 008 domains with concrete authority objects and legacy `/api/tasks/*` contracts.
- `AiResultDomainProjectionService` is a shared projection dependency across report, evidence, risk and strategy, so documenting this boundary is required before any extraction or route migration can be responsible.
- This is not the farther goal of splitting services or projection code. It should produce a governance/readiness artifact and preserve all runtime contracts.

Expected phase shape:

- Docs-only architecture/governance work by default.
- No production behavior change.
- No service extraction, projection split, route migration, alias, endpoint rename or endpoint deletion.
- No DTO/VO/entity/schema/Kafka/frontend/Python/config change.
- Focused static/backend guard work only if Window 1 defines a narrow scope and the user explicitly approves it.

## Fallback Candidate

Config Store Decision Boundary.

Fallback condition:

- Use this if the user rejects risk/strategy projection ownership or wants the next phase to resolve JSON config and role-access transition authority before touching risk/strategy.

Bounded fallback goal:

- Clarify config authority for `ai-config/*.json`, role access config, event source config, event auto-trigger config, config change audit files and Java/Python reader contracts.
- Decide whether the next governance horizon continues JSON config, chooses DB, chooses Nacos, or requires a hybrid target for a later approved migration.
- Preserve current config APIs and audited mutation behavior unless a later phase explicitly approves migration.

Why it is not primary:

- It is broader than risk/strategy because it spans Java config mutation APIs, Python readers, audit files, role access, event source config, workflow/model/prompt config and possible DB/Nacos decisions.
- Human approval is required before any JSON config migration.
- It remains a good fallback because D007 is open and config authority affects later gateway/auth, market/data-ingest and AI workflow decisions.

## Why Other Phases Are Not Selected

- Phase 001 through Phase 010 are completed and frozen.
- Auth/gateway is deferred because T6 is open, but the harness records no production auth requirement or main-path breakage, and role/config authority should be clarified before replacing header-based demo auth.
- Legacy route migration is deferred because Phase 006 intentionally froze current URLs; migration would need breaking-change or compatibility approval and a Phase 006 inventory update.
- Report extraction or report route-migration planning is deferred because Phase 009 produced readiness gates only; projection ownership, auth/permission behavior and route blockers remain.
- Market-service extraction, data-ingest-service extraction, market route migration or market config-store planning is deferred because Phase 010 produced readiness gates only; D009 and D007 remain open and no split/migration was approved.
- Generic eval/static guard expansion is deferred by decision order. It should attach to a bounded authority or contract phase instead of replacing risk/strategy authority work.
- Direct service extraction or permanent modular-monolith declaration is too far because D001 remains open and the current transition host is explicitly not final architecture.
- New feature and new agent work remain ineligible while authority, contract and transition-host lifecycle work remain open.

## Window 1 Must Define

Window 1 must turn the primary candidate into an implementation-ready handoff and define:

- Exact risk/strategy scope: risk warning list/stats, strategy signal list/stats, strategy factors, strategy create command, strategy status command, AI result projection into risk and strategy facts, downstream placeholder topics and related display references.
- Which surfaces are risk facts, strategy facts, read models, commands, projection dependencies, aggregation/display surfaces, frontend consumers, Python execution context or fallback/provenance metadata.
- Current risk authority objects: `risk_warning` and `risk_warning_detail`.
- Current strategy authority objects: `strategy_signal` and `strategy_signal_factor`.
- Whether `AiResultDomainProjectionService` is in-scope as a current dependency to document only, or whether any stronger guard is proposed for user approval.
- Current stable endpoint inventory to preserve:
  - `GET /api/tasks/risk-warnings`
  - `GET /api/tasks/risk-warning-stats`
  - `GET /api/tasks/strategy-signals`
  - `GET /api/tasks/strategy-signal-stats`
  - `GET /api/tasks/strategy-signals/{signalId}/factors`
  - `POST /api/tasks/strategy-signals`
  - `POST /api/tasks/strategy-signals/{signalId}/status`
- Contract constraints for URL path, HTTP method, request binding, response envelope, response type, permission behavior, frontend API function names and TypeScript shapes.
- How report risk points, report strategy highlights, workbench risk/strategy summaries, market context, Python risk fallback and Python strategy context remain non-authoritative unless persisted through approved projection into authority objects.
- Whether downstream `risk.warning.generated` and `strategy.signal.generated` topics are context-only placeholders or in-scope contract documentation. They must not be activated or redesigned in Phase 011 by default.
- Whether frontend risk/strategy consumers are display/read-model consumers, existing backend command initiators for strategy commands, or out of scope.
- Acceptance conditions for belongs, authority, contract and behavior.
- Required verification commands, likely read-only inventory checks and docs coverage checks if the phase remains docs-only.
- Stop rules if implementation appears to require service extraction, projection splitting, route migration, route aliases, endpoint changes, permission behavior changes, Kafka topic/payload changes, database/schema changes, frontend/Python behavior changes, config-store changes or business behavior changes.

Window 1 must also state default approval constraints:

- No breaking changes.
- Keep URL paths stable.
- No business behavior change.
- No new feature work.
- Expected Window 2 type: docs-only by default, unless Window 1 justifies a narrow backend/static guard scope and the user approves it.

## Explicitly Out Of Scope

- No business code change from Window 0.
- No Window 2 implementation before Window 1 and human approval.
- No risk-service extraction.
- No strategy-service extraction.
- No split, move, redesign or rename of `AiResultDomainProjectionService`.
- No activation, redesign or payload change for `risk.warning.generated` or `strategy.signal.generated`.
- No route migration, route alias, endpoint rename, endpoint deletion or endpoint consolidation.
- No breaking change.
- No change to DTO, VO, entity, mapper, database schema, SQL migration, Redis key, Kafka topic or Kafka payload.
- No frontend route, API function, TypeScript shape, command behavior or display behavior change by default.
- No Python workflow, fallback, provenance, prompt context or result payload behavior change.
- No `ai-config` mutation or config-store migration.
- No gateway/auth/JWT/Nacos/Sentinel/deployment work.
- No new helper, adapter, bridge, fallback, proxy, wrapper, resolver or compatibility layer unless a later Window 1 explicitly identifies it and the user approves it.
- No new product feature, new strategy feature, new risk feature or new agent work.
- No reclassification of `ai-orchestration-service`, legacy `/api/tasks/*` paths, JSON config files, workbench aggregation or fallback provenance as final architecture.

## Human Approval Request

User approved:

- Selected phase: Phase 011 - Risk/Strategy Projection Ownership Boundary.
- Breaking changes: not allowed.
- URL paths: must remain stable.
- Business behavior change: not allowed.
- New feature work: not allowed.
- Expected Window 2 shape after Window 1: docs-only by default, unless Window 1 justifies a narrow backend/static guard scope and the user approves it.

Fallback candidate if a later user override rejects the approved primary:

- Config Store Decision Boundary, with the same default constraints unless the user explicitly overrides them.

Window 0 stops here. Window 1 is now allowed to start architecture planning for Phase 011, but no implementation is approved.
