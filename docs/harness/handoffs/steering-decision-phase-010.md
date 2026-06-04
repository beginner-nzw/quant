# Steering Decision - Phase 010

## Status

Window: Window 0 - Steering.

Decision: Phase 010 approved by user for Window 1 architecture planning.

Human approval status: approved.

This file does not approve implementation, does not authorize business code change, and does not start Window 1. Window 1 may start only after the user explicitly approves the selected Phase 010 direction and approval constraints.

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

Latest phase consumed:

- Phase 009 - Report Boundary Readiness.

Matching Phase 009 handoffs consumed:

- `docs/harness/handoffs/steering-decision-phase-009.md`
- `docs/harness/handoffs/phase-009-architect.md`
- `docs/harness/handoffs/phase-009-implementation.md`
- `docs/harness/handoffs/phase-009-review.md`
- `docs/harness/handoffs/phase-009-final.md`

Additional durable artifacts consumed:

- `docs/harness/12-transition-host-exit-criteria.md`
- `docs/harness/13-report-boundary-readiness.md`

Missing matching handoff files:

- None for Phase 009.

No Phase 010 steering decision existed before this file was written.

## Startup Recovery Result

`docs/harness/state/current-state.md` records:

- Current phase: none approved.
- Latest frozen phase: Phase 009 - Report Boundary Readiness.
- Last completed phase: Phase 009 - Report Boundary Readiness.
- Open blockers: none registered.
- Candidate next phases: market event and data-ingest ownership, risk/strategy projection ownership, auth/gateway decision, legacy route migration decision, config store decision, and report extraction or report route-migration planning only if Window 0 and the user explicitly choose to act on Phase 009 readiness gates.

The handoff directory agrees with the state file: the highest final handoff is `phase-009-final.md`.

Bootstrap Phase 001 is not current fact because Phase 001 through Phase 009 are completed and frozen by Window 4.

## Current State Summary

- No active phase is approved.
- Phase 009 completed a docs-only report boundary readiness artifact and did not approve report-service extraction, route migration, endpoint aliases, gateway/auth, config-store migration, frontend/Python reshaping, Kafka/database changes, permanent modular-monolith status, business behavior change or new feature work.
- `ai-orchestration-service` remains a multi-domain transition host, not final architecture.
- Phase 005 modular-monolith policy remains limited to the current governance horizon.
- Phase 006 frozen legacy non-task `/api/tasks/*` contract inventory remains in force.
- Phase 007 frontend authority guardrails remain in force.
- Phase 008 common transition-host exit criteria remain in force.
- Phase 009 report-specific readiness gates remain in force.
- D001 remains open because `ai-orchestration-service` still hosts multiple domains.
- D002 remains open because legacy non-task `/api/tasks/*` contracts are guarded but not migrated.
- D003 remains open for future workbench/fallback metadata surfaces.
- D007 remains open for JSON config governance.
- D008 remains open for header-based demo auth.
- D009 remains open because no independent data-ingest-service exists and mock/source sync live under the transition host.
- Open blockers: none.

## Decision Order Result

Decision order from `docs/harness/10-steering-state-machine.md`:

```text
main path breakage -> authority ambiguity -> contract ambiguity -> transition host reduction -> eval/test coverage -> new feature work
```

Evaluation:

- No main path breakage is registered.
- The highest remaining work is authority and contract governance inside the `ai-orchestration-service` transition host.
- Phase 008 already produced the common transition-host inventory.
- Phase 009 applied that inventory to report and left market/data-ingest, risk/strategy, config, auth/gateway and route migration as later candidates.
- Market/data-ingest is the next bounded authority problem because Phase 008 records that market event facts, source configs, ingest histories, real source sync, mock ingest, preview/diagnose paths, CNINFO proxy, market intelligence display and Python market fallback context are still mixed inside the transition host.
- T3 explicitly forbids treating mock ingest as production data source or using mock source as risk/strategy authority.
- This is narrower and safer than implementing a data-ingest split, route migration, gateway/auth or config-store migration because Phase 010 can classify ownership and contracts without moving code or changing behavior.
- Risk/strategy projection ownership is the fallback because it is also authority-critical, but it spans two domains plus shared projection and strategy command behavior.

## Candidate Score Table

Scores are 0-2. Behavior risk is inverted: 2 means low risk.

| Candidate | Main path protection | Authority clarity | Contract clarity | Transition host reduction | Behavior risk | Verification feasibility | Total | Steering result |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| Phase 010 - Market Event and Data-Ingest Ownership Boundary | 2 | 2 | 2 | 2 | 1 | 1 | 10 | Primary |
| Risk/Strategy Projection Ownership Boundary | 2 | 2 | 2 | 1 | 1 | 1 | 9 | Fallback |
| Config Store Decision | 1 | 2 | 1 | 1 | 1 | 1 | 7 | Defer |
| Auth/Gateway Decision | 1 | 2 | 1 | 1 | 1 | 1 | 7 | Defer |
| Generic eval/test expansion | 1 | 1 | 1 | 0 | 2 | 2 | 7 | Defer |
| Report extraction or report route-migration planning | 1 | 1 | 2 | 2 | 0 | 0 | 6 | Defer |
| Legacy route migration decision | 0 | 1 | 2 | 2 | 0 | 0 | 5 | Defer |
| Direct service extraction or permanent modular-monolith declaration | 1 | 1 | 1 | 2 | 0 | 0 | 5 | Defer |
| New feature or new agent work | 0 | 0 | 0 | 0 | 0 | 0 | 0 | Not eligible |

Tie-break notes:

- Market/data-ingest scores highest because it directly addresses D001 and D009 while also clarifying D002 market route contracts and D003 market fallback/display boundaries.
- Risk/strategy remains close, but it is broader because strategy has manual command and status update surfaces while risk is projection-fed and read-only at REST.

## Primary Candidate

Phase 010 - Market Event and Data-Ingest Ownership Boundary.

Bounded goal:

- Apply the Phase 008 readiness template to the market event and data-ingest boundary.
- Clarify belongs, authority and contract boundaries for market event facts, market intelligence display, real source sync, mock/demo ingest, source preview/diagnose, CNINFO proxy, ingest history, event source config, frontend market consumers and Python market fallback context.
- Decide whether current data-ingest responsibilities are explicitly retained inside `ai-orchestration-service` for another bounded horizon or require a later human-approved split.
- Define readiness gates before any later data-ingest-service extraction, route migration, config-store migration, Kafka/database change, frontend reshaping, Python behavior change or permanence decision.

Why this is the next bounded step:

- Phase 009 completed the report readiness slice. The next unprocessed Phase 008 domain with a concrete ownership risk is market/data-ingest.
- Market event facts and market context feed downstream report, risk, strategy and workbench displays. Keeping mock/demo ingest, real source sync, event source config and market intelligence display clearly separated protects main-path interpretation without adding features.
- This step advances authority and contract clarity before transition-host reduction work.
- It is not the farther data-ingest-service goal. It should classify ownership, contracts, blockers and readiness gates first, while preserving all current behavior and URLs.

Expected phase shape:

- Docs-only architecture/governance work by default.
- No production behavior change.
- No service extraction, route migration, alias, endpoint rename or endpoint deletion.
- No DTO/VO/entity/schema/Kafka/frontend/Python/config change.
- Focused static/backend guard work only if Window 1 defines a narrow scope and the user approves it.

## Fallback Candidate

Risk/Strategy Projection Ownership Boundary.

Fallback condition:

- Use this if the user rejects market/data-ingest ownership or wants the next phase to target AI result projection ownership before market ingest boundaries.

Bounded fallback goal:

- Clarify ownership and guardrails for `AiResultDomainProjectionService` output into `risk_warning`, `risk_warning_detail`, `strategy_signal` and `strategy_signal_factor`.
- Separate risk read-model authority, strategy manual command authority, strategy status command authority, AI projection writer authority, downstream placeholder topics and report/workbench display summaries.
- Preserve current `/api/tasks/risk-*` and `/api/tasks/strategy-*` contracts unless a later phase explicitly approves migration.

Why it is not primary:

- It spans two domains and shared projection logic.
- Strategy has manual create/status commands while risk is projection-fed and read-only at REST, so Window 1 would need a wider authority split.
- It remains a good fallback because it is the next strongest authority/contract candidate after market/data-ingest.

## Why Other Phases Are Not Selected

- Phase 001 through Phase 009 are completed and frozen.
- Report extraction or report route-migration planning is deferred because Phase 009 produced readiness gates only; it did not satisfy extraction or route-migration blockers, and acting now would likely require breaking-change or compatibility decisions.
- Legacy route migration is deferred because Phase 006 intentionally froze current URLs and route migration is likely a breaking-change or compatibility-path phase.
- Auth/gateway is deferred because header-based demo auth is registered transition debt, but no production auth requirement or main-path breakage is currently registered.
- Config store decision is deferred because JSON config is transition debt, but changing or choosing DB/Nacos would affect Java and Python readers, config audit, role access and event source config. Market/data-ingest can clarify event-source ownership first without moving the store.
- Generic eval/test expansion should attach to a bounded authority/contract phase instead of becoming standalone work while D001/D002 remain open.
- Direct service extraction, route migration or permanent modular-monolith declaration is too far. Phase 008 and Phase 009 created readiness gates; they did not satisfy them.
- New features and new agents remain ineligible while authority, contract and transition-host lifecycle work remain open.

## Window 1 Must Define

Window 1 must turn the primary candidate into an implementation-ready handoff and define:

- Exact market/data-ingest scope: market event list/detail/stats, market intelligence, create event, batch import preview/import, mock ingest, source sync, source preview, source diagnose, CNINFO proxy, ingest history, event source config and event auto-trigger dependencies if relevant.
- Which surfaces are market facts, read models, commands, demo/mock mechanisms, source-adapter mechanisms, config facts, ingest history facts, aggregation/display surfaces or Python fallback/provenance context.
- Current authority objects: `market_event`, `market_event_relation`, `market_event_analysis`, `event-source-configs.json` and `event-ingest-histories.json` as current config/file facts.
- Current command ownership for create, batch import, mock ingest, source sync, preview and diagnose.
- Current read-model ownership for market event list/detail/stats, ingest history, source config, CNINFO proxy, market intelligence and market intelligence stats.
- Exact legacy route inventory to preserve, including current `/api/tasks/market-*` and `/api/tasks/market-intelligence*` contracts.
- Contract constraints for URL path, HTTP method, request binding, response envelope, response type, permission behavior, frontend API function names and TypeScript shapes.
- How mock/demo ingest must remain non-production and non-authoritative for risk/strategy/report facts.
- How Python market fallback context and fallback market snapshots must remain provenance-bearing and non-authoritative.
- Whether config files and event source configs are context-only dependencies or in-scope governance facts.
- Whether `market.event.standardized` consumption/publication is context-only or in-scope contract documentation.
- Whether frontend market consumers are read-only display consumers, command initiators for existing backend commands, or out of scope.
- Acceptance conditions for belongs, authority, contract and behavior.
- Required verification commands, likely read-only inventory checks and docs coverage checks if the phase remains docs-only.
- Stop rules if implementation appears to require data-ingest-service extraction, route migration, route aliases, endpoint changes, config-store migration, schema changes, Kafka topic/payload changes, frontend/Python behavior changes or business behavior changes.

Window 1 must also state default approval constraints:

- No breaking changes.
- Keep URL paths stable.
- No business behavior change.
- No new feature work.
- Expected Window 2 type: docs-only by default, unless Window 1 justifies a narrower backend/static guard scope and the user approves it.

## Explicitly Out Of Scope

- No business code change from Window 0.
- No Window 2 implementation before Window 1 and human approval.
- No data-ingest-service extraction.
- No market-service extraction.
- No route migration, route alias, endpoint rename, endpoint deletion or endpoint consolidation.
- No breaking change.
- No change to DTO, VO, entity, mapper, database schema, SQL migration, Kafka topic or Kafka payload.
- No frontend route, API function, TypeScript shape, command behavior or display behavior change by default.
- No Python workflow, fallback, provenance or market data behavior change.
- No `ai-config` mutation or config-store migration.
- No gateway/auth/Nacos/Sentinel/deployment work.
- No new helper, adapter, bridge, fallback, proxy, wrapper or compatibility layer unless a later Window 1 explicitly identifies and the user approves it.
- No new product feature, new market source feature or new agent work.
- No reclassification of `ai-orchestration-service`, JSON config files, mock ingest or legacy `/api/tasks/*` paths as final architecture.

## Human Approval Request

User approved the Phase 010 direction.

Recorded approval:

- Selected phase: Phase 010 - Market Event and Data-Ingest Ownership Boundary.
- Breaking changes: not allowed.
- URL paths: must remain stable.
- Business behavior change: not allowed.
- New feature work: not allowed.
- Expected Window 2 shape after Window 1: docs-only by default, unless Window 1 justifies a narrow backend/static guard scope and the user approves it.

Window 0 stops here. Window 1 is the next required window and must not start implementation.
