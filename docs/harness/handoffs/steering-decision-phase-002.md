# Steering Decision - Phase 002

## Status

Window 0 steering decision approved by user for Window 1 architecture planning.

This file does not approve implementation. Window 1 must still produce a phase architect handoff, and the user must approve that handoff before Window 2 starts.

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

Startup recovery handoffs:

- `docs/harness/handoffs/steering-decision-phase-001.md`
- `docs/harness/handoffs/phase-001-architect.md`
- `docs/harness/handoffs/phase-001-implementation.md`
- `docs/harness/handoffs/phase-001-review.md`
- `docs/harness/handoffs/phase-001-final.md`

## Latest Phase Consumed

Latest completed phase from `docs/harness/state/current-state.md`:

- Phase 001 - Split Controller Surface Inside `ai-orchestration-service`.

Latest final handoff found:

- `docs/harness/handoffs/phase-001-final.md`.

Missing matching handoff files:

- None.

Startup recovery result:

- Phase 001 is completed with residual risk.
- No active next phase is approved.
- Window 0 may score the next candidate set.

## Current State Summary

- Current phase: Phase 002 - Split `TaskQueryServiceImpl` Internal Query Services.
- Current phase status: approved for Window 1 architecture planning.
- Last completed phase: Phase 001.
- Open blockers: none registered.
- Main path breakage: none registered in current harness state or Phase 001 final handoff.
- Open architecture drift: `ai-orchestration-service` remains a multi-domain transition host.
- Open authority drift: `TaskQueryServiceImpl` mixes multiple read-model responsibilities; `research-workbench` must remain display-only; frontend must remain a consumer.
- Open contract drift: non-task domain endpoints retain approved legacy `/api/tasks/*` paths; workbench/fallback contracts need stronger boundaries; Python fallback metadata must remain auditable.
- Active transition hosts: `ai-orchestration-service`, `TaskQueryServiceImpl`, legacy non-task `/api/tasks/*` paths, JSON config files and mock/demo ingest paths.
- Human approval status: Phase 002 approved for Window 1 architecture planning after this steering decision.

## Decision Order Result

Decision order from `10-steering-state-machine.md`:

1. Main path breakage.
2. Authority ambiguity.
3. Contract ambiguity.
4. Transition host reduction.
5. Eval/test coverage for existing behavior.
6. New feature work.

Evaluation:

- No main path breakage is registered.
- Authority ambiguity remains high because `TaskQueryServiceImpl` mixes task, report, risk, strategy, market, audit and workbench read-model responsibilities.
- Contract ambiguity remains, but Phase 001 already split the controller surface; the next bounded way to reduce drift is to split the internal read-model ownership behind those surfaces.
- Transition host reduction should continue inside `ai-orchestration-service` before any microservice extraction.
- Eval/test-only hardening is valuable but should follow the current D001/D004 internal boundary split unless implementation risk proves too high.
- New product features are not eligible while D001-D004 remain open.

## Candidate Score Table

Scores are 0-2. Behavior risk is inverted: 2 means low risk.

| Candidate | Main path protection | Authority clarity | Contract clarity | Transition host reduction | Behavior risk | Verification feasibility | Total | Steering result |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| Phase 002 - Split `TaskQueryServiceImpl` Internal Query Services | 1 | 2 | 1 | 2 | 1 | 2 | 9 | Primary |
| Phase 003 - Contract Hardening for Workbench and Fallback | 1 | 2 | 2 | 0 | 1 | 1 | 7 | Fallback |
| Phase 004 - Python AI Workflow Contract Cleanup | 1 | 1 | 1 | 1 | 1 | 1 | 6 | Defer |
| Phase 005 - Decide Service Split or Continue Modular Monolith | 0 | 1 | 1 | 2 | 0 | 0 | 4 | Defer |

## Decision

Phase 002 is the approved next candidate for Window 1 architecture planning.

Window 0 does not start Window 1 and does not authorize implementation.

## Primary Candidate

Phase 002 - Split `TaskQueryServiceImpl` Internal Query Services.

Bounded goal:

- Split mixed read-model responsibilities currently concentrated in `TaskQueryServiceImpl` into internal domain query services inside `ai-orchestration-service`.
- Preserve all external API contracts and all legacy URL paths.
- Preserve business behavior.
- Keep the current service as a transition host, not final architecture.

Why this is the next bounded step:

- Phase 001 completed the controller surface split; the next direct residual hotspot is the internal read-model host named by D001 and D004.
- It improves authority clarity by separating task/report/risk/strategy/market/workbench/audit read-model ownership.
- It advances T1 exit criterion 2: split `TaskQueryServiceImpl` into internal domain query services.
- It is more bounded than changing URL namespaces or extracting microservices.
- It prepares safer later work on workbench/fallback contracts because the aggregation and domain read-model code will have clearer homes.

## Fallback Candidate

Phase 003 - Contract Hardening for Workbench and Fallback.

Fallback condition:

- Use only if the user rejects Phase 002 or wants a lower-refactor phase before touching `TaskQueryServiceImpl`.

Why it is not primary:

- It improves authority and contract guardrails, but it does not reduce the active internal transition host.
- It may be easier to harden workbench and fallback behavior after Phase 002 clarifies where domain read models and aggregation logic live.

## Why Other Phases Are Not Selected

- Phase 004 is deferred because Python fallback cleanup is lower in the decision order than the remaining Java read-model authority and transition-host drift.
- Phase 005 is premature. The project should not decide service extraction or modular-monolith permanence until internal query boundaries and core contract hardening are clearer.
- New feature work is not eligible while D001-D004 remain open.

## Window 1 Must Define

Window 1 must convert Phase 002 into an implementation-ready handoff and must define:

- Exact method inventory currently inside `TaskQueryServiceImpl`.
- Mapping from each method to a domain read-model or aggregation boundary:
  - task runtime/read-model
  - report read-model
  - risk warning read-model
  - strategy signal read-model
  - market event read-model
  - market intelligence aggregation
  - research workbench display aggregation
  - audit compliance/config dashboard views
- Which extracted internal services are allowed, including names, package location and dependencies.
- Which contracts must remain external-only and unchanged, especially all `/api/tasks/*` paths.
- Which fallback/preferred-field/merge logic is display hydration only versus domain read-model behavior.
- Which files Window 2 may modify and which files are forbidden.
- Whether this phase is one backend implementer window or requires a partitioned multi-implementer plan.
- Acceptance conditions proving no behavior regression and no new source of truth.
- Verification commands, expected at minimum `mvn -q test` from `quant-ai-platform/quant-services`.
- Any focused tests needed to prevent domain query services from drifting back into one mixed host.

## Explicitly Out Of Scope

- No URL path changes.
- No controller API contract changes.
- No frontend changes.
- No Python changes.
- No database schema changes.
- No new endpoints or product features.
- No gateway/auth/config-store/Nacos work.
- No report/risk/strategy/market microservice extraction.
- No reclassification of `ai-orchestration-service` as final architecture.
- No changing `research-workbench` into a source of truth.
- No silent fallback promotion to business truth.

## Human Approval Recorded

User approved this steering decision.

Recorded approval:

- Phase 002 may proceed to Window 1 architecture planning.
- Default constraints apply: no breaking changes, URL paths remain stable, no behavior change, no new feature work.
- Expected later implementation window type: backend-only, unless Window 1 proves a partitioned backend plan is required.
- Phase 003 remains the fallback candidate but is not active.
