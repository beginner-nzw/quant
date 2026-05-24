# Phase 005 Final Handoff

## Status

Window: Window 4 - Phase Handoff.

Phase: Phase 005 - Decide Service Split or Continue Modular Monolith.

Phase status: completed with residual risk.

Latest review consumed:

- `docs/harness/handoffs/phase-005-review.md`

Review decision: approve.

Fix passes: none.

## Inputs Read

Required harness files:

- `docs/harness/06-debt-register.md`
- `docs/harness/07-phase-backlog.md`
- `docs/harness/09-window-protocol.md`
- `docs/harness/10-steering-state-machine.md`
- `docs/harness/state/current-state.md`

Phase 005 handoffs:

- `docs/harness/handoffs/steering-decision-phase-005.md`
- `docs/harness/handoffs/phase-005-architect.md`
- `docs/harness/handoffs/phase-005-implementation.md`
- `docs/harness/handoffs/phase-005-review.md`

No Phase 005 fix implementation or review-fix handoff exists.

## Completed Scope

Phase 005 completed the docs-only service-boundary policy decision.

Selected option:

- Continue as a modular monolith inside `ai-orchestration-service` for the next governance horizon.

Meaning:

- `ai-orchestration-service` remains the current multi-domain transition host.
- This is not a permanent final-architecture declaration.
- This does not approve service extraction.
- This does not approve route migration, route aliases, gateway/auth implementation, config-store migration, data-ingest split or product feature work.

Implementation scope completed:

- Recorded the selected option in `docs/harness/handoffs/phase-005-implementation.md`.
- Rejected extraction-preparation and gateway/auth-first options for this phase.
- Preserved the `belongs -> authority -> contract -> behavior` decision order.
- Listed follow-up candidate phases for later Window 0 evaluation.
- Changed no business runtime files.

## Contract, Authority And Transition State

Contract state:

- All current URLs, HTTP methods, request bindings, response envelopes, response types and permission behavior remain unchanged.
- Phase 006 remains the frozen inventory for legacy non-task `/api/tasks/*` transitional contracts.
- Legacy non-task `/api/tasks/*` paths remain transition debt and are not final architecture.
- Frontend routes, API endpoint strings, call signatures, TypeScript shapes, Kafka topics, database schema, JSON config files and Python fallback/provenance behavior remain unchanged.

Authority state:

- No source of truth moved.
- No read model became command authority.
- Research workbench remains display-only aggregation.
- Python fallback provenance remains non-authoritative metadata.
- Phase 003, Phase 004 and Phase 007 guardrails remain in force for current known workbench and fallback/provenance consumers.

Transition state:

- `ai-orchestration-service` continues as the next-governance-horizon modular monolith and transition host.
- D001 is reduced from undecided service-boundary policy to bounded modular-monolith policy, but it remains open because the host still contains multiple domains.
- Any extraction, route migration, gateway/auth work or permanence claim requires a later Window 0 decision and human approval.

## Unchanged Contracts

- `POST /api/research/tasks`
- Existing task read/control endpoints under `/api/tasks`
- Phase 006 frozen non-task legacy `/api/tasks/*` endpoints
- Report, risk, strategy, market, market-intelligence, audit, config and workbench contracts documented in the harness
- Existing `quant-ui` routes and API consumer shapes
- Kafka topics: `ai.task.dispatch`, `ai.task.status`, `ai.task.result`, `ai.task.audit`, `market.event.standardized`
- Database schema and migrations
- `ai-config` JSON files
- Java, Python and frontend runtime behavior

## Remaining Debt

- D001 remains open: `ai-orchestration-service` still hosts multiple domains. Phase 005 chose a bounded modular-monolith horizon, not final architecture.
- D002 remains open: legacy non-task `/api/tasks/*` namespace debt is frozen by Phase 006 but not migrated.
- D003 remains open for future surfaces: current known workbench/fallback consumers are guarded, but later surfaces need equivalent non-authoritative boundaries.
- D005-D011 remain open as previously registered unless a later phase explicitly targets them.
- D012 remains open: mixed namespace report/review/config APIs remain guarded transitional contracts.

## Latest State For Window 0

Window 0 should automatically discover:

- Current phase: none approved.
- Last completed phase: Phase 005 - Decide Service Split or Continue Modular Monolith.
- Latest phase status: completed with residual risk.
- Open blockers: none registered.
- Active transition host policy: continue `ai-orchestration-service` as next-governance-horizon modular monolith, not final architecture.
- Highest-order remaining issue: D001 follow-through, especially transition-host exit criteria and per-domain readiness gates.
- Phase 001, Phase 002, Phase 003, Phase 004, Phase 005, Phase 006 and Phase 007 are completed/frozen and are no longer candidate phases.

Window 0 must read this final handoff plus the matching Phase 005 steering, architect, implementation and review handoffs. The user does not need to summarize Phase 005 manually.

## Recommended Candidate Inputs For Window 0

Window 4 does not select the next phase. Candidate inputs for Window 0 scoring:

- Phase 008 - Transition Host Exit Criteria Inventory.
- Report boundary readiness phase.
- Market event and data-ingest ownership phase.
- Risk/strategy projection ownership phase.
- Auth/gateway decision phase.
- Legacy route migration decision phase.
- Config store decision phase.

Window 0 must use `docs/harness/10-steering-state-machine.md`, propose exactly one primary candidate and one fallback candidate, and wait for human approval.

## Files Changed In This Handoff

- `docs/harness/state/current-state.md`
- `docs/harness/05-transition-lifetime.md`
- `docs/harness/06-debt-register.md`
- `docs/harness/07-phase-backlog.md`
- `docs/harness/handoffs/phase-005-final.md`

No business code, test code, runtime config, frontend, Python, Java, database, Kafka, dependency, build-config or deployment file was changed by Window 4.

## Window 4 Verification Notes

Window 4 ran before staging:

```powershell
git status --short --untracked-files=all
```

The working tree included pre-existing unrelated untracked handoff files from other windows. Window 4 must stage only the files listed in "Files Changed In This Handoff" and must not stage unrelated existing handoff files or business-code files.
