# Steering Decision - Phase 008

## Status

Window: Window 0 - Steering.

Decision: Phase 008 approved by user for Window 1 architecture planning.

Human approval status: approved.

This file does not approve implementation, does not authorize business code change, and does not start Window 2. Window 1 must produce `docs/harness/handoffs/phase-008-architect.md`, and the user must approve that handoff before any implementation window starts.

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

Phase 005 handoffs consumed because `current-state.md` lists Phase 005 as `Last Completed Phase` and `phase-005-final.md` says the next Window 0 must recover from Phase 005:

- `docs/harness/handoffs/steering-decision-phase-005.md`
- `docs/harness/handoffs/phase-005-architect.md`
- `docs/harness/handoffs/phase-005-implementation.md`
- `docs/harness/handoffs/phase-005-review.md`
- `docs/harness/handoffs/phase-005-final.md`

Phase 007 handoffs consumed because the handoff directory contains a higher-numbered final handoff and `current-state.md` also records Phase 007 as completed:

- `docs/harness/handoffs/steering-decision-phase-007.md`
- `docs/harness/handoffs/phase-007-architect.md`
- `docs/harness/handoffs/phase-007-implementation.md`
- `docs/harness/handoffs/phase-007-review.md`
- `docs/harness/handoffs/phase-007-final.md`

Additional reconciliation handoff consumed:

- `docs/harness/handoffs/phase-006-final.md`

Missing matching handoff files:

- None for the consumed Phase 005 and Phase 007 handoff sets.

## Startup Recovery Result

`current-state.md` contains an ordering inconsistency:

- `Last Completed Phase` says Phase 005.
- The same state file also says Phase 006 and Phase 007 remain completed with residual risk.
- The handoff directory contains `phase-006-final.md` and `phase-007-final.md`.
- `phase-005-final.md`, `07-phase-backlog.md`, and `06-debt-register.md` all say Phase 001 through Phase 007 are completed/frozen and are no longer candidates.

This is not treated as a blocker because the safe next step is explicit and consistent across the durable files: continue from completed Phase 005 policy, completed Phase 006 contract freeze, and completed Phase 007 frontend guardrails into D001 follow-through.

## Current State Summary

- Current phase: none approved.
- Current phase status: no active phase.
- Latest completed/frozen set: Phase 001 through Phase 007 are completed with residual risk.
- Current-state named last completed phase: Phase 005 - Decide Service Split or Continue Modular Monolith.
- Highest-numbered final handoff present: Phase 007 - Frontend Consumer Authority Boundary Audit.
- Latest policy result: Phase 005 selected continuing `ai-orchestration-service` as a next-governance-horizon modular monolith, not final architecture.
- Open blockers: none registered.
- Open architecture drift: `ai-orchestration-service` remains a multi-domain transition host; gateway/auth/config/service-discovery architecture from the original plan is not implemented.
- Open authority drift: current known backend, Python and frontend workbench/fallback surfaces have guardrails, but future surfaces must preserve equivalent non-authoritative boundaries.
- Open contract drift: legacy non-task `/api/tasks/*` paths remain transition debt, though Phase 006 froze the approved inventory and tests.
- Active transition hosts: `ai-orchestration-service`, internal domain query services, legacy non-task `/api/tasks/*` paths, research workbench display aggregation, JSON config files, mock/demo ingest, and Python fallback.

Bootstrap Phase 001 recommendation is no longer current fact because Phase 001 and higher final handoffs exist.

## Decision Order Result

Decision order from `10-steering-state-machine.md`:

```text
main path breakage -> authority ambiguity -> contract ambiguity -> transition host reduction -> eval/test coverage -> new feature work
```

Evaluation:

- No main path breakage is registered.
- Known workbench/fallback authority ambiguity has been reduced by Phase 003, Phase 004 and Phase 007, but future surfaces must preserve the same boundary.
- Legacy non-task `/api/tasks/*` contract ambiguity has been mitigated by Phase 006 contract freeze; route migration is still transition debt but would require later explicit approval.
- The highest-order remaining issue is D001 follow-through: `ai-orchestration-service` remains a multi-domain transition host after Phase 005 selected a bounded modular-monolith policy.
- The next step should define exit criteria and per-domain readiness gates before any service extraction, route migration, gateway/auth work, config-store migration, ingest split, or product feature.

## Candidate Score Table

Scores are 0-2. Behavior risk is inverted: 2 means low risk.

| Candidate | Main path protection | Authority clarity | Contract clarity | Transition host reduction | Behavior risk | Verification feasibility | Total | Steering result |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| Phase 008 - Transition Host Exit Criteria Inventory | 1 | 2 | 2 | 2 | 2 | 2 | 11 | Primary |
| Report boundary readiness phase | 1 | 2 | 2 | 1 | 2 | 1 | 9 | Fallback |
| Risk/strategy projection ownership phase | 1 | 2 | 2 | 1 | 1 | 1 | 8 | Defer |
| Market event and data-ingest ownership phase | 1 | 2 | 1 | 1 | 1 | 1 | 7 | Defer |
| Auth/gateway decision phase | 1 | 1 | 1 | 1 | 1 | 1 | 6 | Defer |
| Config store decision phase | 0 | 1 | 1 | 1 | 1 | 1 | 5 | Defer |
| Generic eval/test expansion | 1 | 0 | 0 | 0 | 2 | 2 | 5 | Defer |
| Legacy route migration decision phase | 0 | 1 | 2 | 1 | 0 | 0 | 4 | Defer |
| New feature or new agent work | 0 | 0 | 0 | 0 | 0 | 0 | 0 | Not eligible |

Tie-break:

- No tie changes the primary result.
- If the user rejects a cross-domain inventory phase, the fallback should be a narrower domain-readiness phase that still follows D001 before feature work.

## Primary Candidate

Phase 008 - Transition Host Exit Criteria Inventory.

Bounded goal:

- Inventory current `ai-orchestration-service` transition-host responsibilities by domain.
- Record each domain's SoT, read-model placement, command surfaces, legacy route dependencies, current guardrails, extraction blockers and exit criteria.
- Produce readiness gates that later Window 0 decisions can use before considering extraction, permanence, route migration, gateway/auth, data-ingest split or config-store migration.

Why this is the next bounded step:

- Phase 001 split controller surface.
- Phase 002 split internal query service boundaries.
- Phase 003, Phase 004 and Phase 007 reduced known workbench/fallback authority risks.
- Phase 006 froze legacy non-task `/api/tasks/*` contracts.
- Phase 005 selected a bounded modular-monolith policy, but did not define per-domain exit gates.
- D001 remains open, and `06-debt-register.md` explicitly says next work should start from D001 follow-through: transition-host exit criteria and per-domain readiness gates.

This is the next step because it prepares governed boundary movement without performing a larger architecture move. It is smaller than service extraction, safer than route migration, and more foundational than choosing a single downstream domain first.

Expected phase shape:

- Docs-only architecture/governance phase by default.
- No business behavior change.
- No URL, DTO/VO/entity, database, Kafka, frontend, Python, config, dependency or deployment change.
- Code inspection may be allowed only if Window 1 needs to verify current contract/host facts; implementation should not modify business code.

## Fallback Candidate

Report boundary readiness phase.

Fallback condition:

- Use this only if the user rejects a cross-domain Phase 008 inventory as too broad and wants one narrower domain-boundary slice first.

Bounded fallback goal:

- Inventory report facts, report review commands, report versions, evidence references, report-center dependencies, frontend consumers and legacy `/api/tasks/*` route dependencies.
- Define report-specific readiness gates before any later `report-service` extraction or route migration can be considered.
- Preserve Phase 005 modular-monolith policy and Phase 006 legacy path freeze.

Why it is not primary:

- It improves one important domain, but leaves the cross-domain transition-host exit criteria undefined.
- It risks producing domain-specific readiness rules before the project has a consistent template for report, market, risk, strategy, audit, config and workbench.

## Why Other Phases Are Not Selected

- Phase 001 through Phase 007 are completed and frozen.
- Risk/strategy projection ownership is important, but it should use the Phase 008 exit-criteria template or follow as a domain-specific readiness phase.
- Market event and data-ingest ownership is real debt, but mock ingest and real ingest ownership should be evaluated after the common transition-host inventory shape is defined.
- Auth/gateway decision is deferred because Phase 005 did not identify it as the primary prerequisite for D001 follow-through.
- Legacy route migration is deferred because it is likely a breaking-change or compatibility-path decision, and Phase 006 intentionally preserved current URLs.
- Config store decision is deferred because JSON config is registered transition debt, but lower order than D001 service-boundary readiness.
- Generic eval/test expansion should attach to a bounded authority/contract phase, not replace transition-host exit criteria.
- New feature and new agent work remain ineligible while D001 and transition-host lifecycle questions remain open.

## Window 1 Must Define

Window 1 must convert Phase 008 into an implementation-ready architecture handoff and define:

- Exact domains in scope: at minimum report, market, risk, strategy, audit, config and workbench responsibilities inside `ai-orchestration-service`.
- Whether task runtime read model, task retry/cancel, AI status/result/audit consumers and AI domain projection are included or explicitly excluded.
- For each in-scope domain: authoritative objects, current host, read models, command surfaces, aggregation surfaces, Kafka topics, database tables or config files, frontend consumers and Python touchpoints.
- Which legacy `/api/tasks/*` routes are dependencies and how Phase 006 inventory must remain stable.
- Which current guardrails from Phase 003, Phase 004 and Phase 007 must remain in force.
- Exit criteria for each transition-host responsibility.
- Extraction readiness gates and blockers without selecting or implementing extraction.
- A verification approach suitable for docs-only governance work, likely `rg` checks over the handoff plus optional read-only code inventory if required.
- Stop rules if satisfying Phase 008 appears to require code edits, route migration, breaking changes, DTO/VO/entity changes, database/Kafka changes, frontend/Python behavior changes, config-store migration, service extraction or gateway/auth implementation.

Window 1 must also state default approval constraints:

- No breaking changes.
- Keep URL paths stable.
- No business behavior change.
- No new feature work.
- Docs-only architecture/governance work unless the user explicitly approves a different shape.

## Explicitly Out Of Scope

- No business code change.
- No service extraction.
- No route move, route alias, endpoint rename, endpoint deletion or route migration.
- No breaking change.
- No DTO, VO, entity, mapper, schema, SQL, database migration, Kafka topic or payload change.
- No frontend route, API shape, command behavior or display behavior change.
- No Python workflow, fallback or provenance behavior change.
- No `ai-config` mutation or config-store migration.
- No gateway/auth/Nacos/Sentinel/deployment work.
- No new helper, adapter, bridge, fallback, proxy, wrapper or compatibility layer.
- No new product feature or new agent work.
- No reclassification of `ai-orchestration-service` or legacy `/api/tasks/*` paths as final architecture.

## Human Approval Recorded

User approved this steering decision.

Recorded approval:

- Selected phase: Phase 008 - Transition Host Exit Criteria Inventory.
- No breaking changes.
- Keep all URL paths stable.
- No business behavior change.
- No new feature work.
- Expected Window 2 shape later: docs-only architecture/governance work, unless Window 1 identifies and you approve a different bounded shape.

Window 0 stops here. Window 1 is the next required window and must not start implementation.
