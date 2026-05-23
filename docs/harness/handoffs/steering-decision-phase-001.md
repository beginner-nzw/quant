# Steering Decision - Phase 001

## Status

Window 0 steering decision approved by user for Window 1 architecture planning.

This file does not approve implementation. Window 1 must still produce a phase architect handoff, and the user must approve that handoff before Window 2 starts.

## Inputs Read

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

## Current State Summary

- Current phase: Phase 001 - Split Controller Surface Inside `ai-orchestration-service`.
- Current phase status: approved for Window 1 architecture planning.
- Last completed phase: Phase 000.
- Open blockers: none registered.
- Main path breakage: none registered in current harness state.
- Open architecture drift: `ai-orchestration-service` is a multi-domain transition host; gateway/auth/config/service discovery are not implemented.
- Open authority drift: `TaskQueryServiceImpl` mixes read-model responsibilities; `research-workbench` must remain display-only; frontend must remain a contract consumer.
- Open contract drift: `TaskQueryController` exposes too many unrelated API surfaces; workbench/fallback contracts need stronger boundaries; Python fallback metadata must remain auditable.
- Active transition hosts: `ai-orchestration-service`, `TaskQueryController`, `TaskQueryServiceImpl`, JSON config files, mock/demo ingest paths.
- Human approval status: Phase 001 approved for Window 1 architecture planning after this steering decision.

## Decision Order Result

Decision order from `10-steering-state-machine.md`:

1. Main path breakage.
2. Authority ambiguity.
3. Contract ambiguity.
4. Transition host reduction.
5. Eval/test coverage for existing behavior.
6. New feature work.

Evaluation:

- No registered main path breakage currently outranks architecture work.
- Authority ambiguity exists, but the most immediate exposed boundary problem is that one controller presents many domains through one API surface.
- Contract ambiguity is explicit and high priority: `TaskQueryController` exposes tasks, market events, risk, strategy, reports, audit, config and workbench endpoints.
- Transition host reduction should begin inside the current service before any microservice split.
- Eval/test expansion and new feature work should not outrank D001-D004.

## Candidate Score Table

Scores are 0-2. Behavior risk is inverted: 2 means low risk.

| Candidate | Main path protection | Authority clarity | Contract clarity | Transition host reduction | Behavior risk | Verification feasibility | Total | Steering result |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| Phase 001 - Split Controller Surface Inside `ai-orchestration-service` | 1 | 1 | 2 | 2 | 2 | 2 | 10 | Primary |
| Phase 002 - Split `TaskQueryServiceImpl` Internal Query Services | 1 | 2 | 1 | 2 | 1 | 2 | 9 | Fallback |
| Phase 003 - Contract Hardening for Workbench and Fallback | 1 | 2 | 2 | 0 | 1 | 1 | 7 | Defer |
| Phase 004 - Python AI Workflow Contract Cleanup | 1 | 1 | 1 | 1 | 1 | 1 | 6 | Defer |
| Phase 005 - Decide Service Split or Continue Modular Monolith | 0 | 1 | 1 | 2 | 0 | 0 | 4 | Defer |

## Decision

Phase 001 is the approved next candidate for Window 1 architecture planning.

Window 0 does not start Window 1 and does not authorize implementation.

## Primary Candidate

Phase 001 - Split Controller Surface Inside `ai-orchestration-service`.

Bounded goal:

- Split the current multi-domain controller surface into domain-specific controller classes inside the same service.
- Keep existing URL paths stable unless the user explicitly approves breaking changes.
- Preserve current behavior and service internals.
- Map each controller surface back to `04-contract-map.md`.

Why this is the next bounded step:

- It directly addresses D001 and D002 without changing business behavior.
- It satisfies the first exit criterion of T1: split `TaskQueryController` into domain-specific controllers inside the same service.
- It reduces visible transition-host confusion before deeper service extraction.
- It is smaller and safer than splitting `TaskQueryServiceImpl` or deciding microservice extraction.
- It keeps the current transition host acknowledged as temporary, not final architecture.

## Fallback Candidate

Phase 002 - Split `TaskQueryServiceImpl` Internal Query Services.

Fallback condition:

- Use only if the user rejects Phase 001 but still wants to work on D001-D004 boundary drift.

Why it is not primary:

- It touches deeper read-model internals and carries higher behavior regression risk.
- It should ideally follow controller-surface separation so Window 1 can reason from stable external API groups into internal query ownership.
- It improves authority clarity more than Phase 001, but with a larger implementation surface.

## Why Other Phases Are Not Selected

- Phase 003 is valuable but mostly hardens tests/comments around workbench and fallback. It does not first reduce the largest active transition-host surface.
- Phase 004 is deferred because Python fallback cleanup is lower in the current decision order than Java authority/contract drift.
- Phase 005 is premature. Service extraction or modular-monolith decisions require stable controller contracts and internal query boundaries first.
- New feature work is not eligible while D001-D004 remain open.

## Window 1 Must Define

Window 1 must convert Phase 001 into an implementation-ready handoff and must define:

- Exact endpoint inventory currently exposed by `TaskQueryController`.
- Mapping from each endpoint to the contract class in `04-contract-map.md`.
- Controller ownership boundaries for task, report, risk, strategy, market, audit, config and workbench surfaces.
- Whether each surface is authoritative read-model, aggregation view, command contract or transition command.
- File ownership and allowed change set for the later Window 2 backend implementer.
- Verification commands, expected at minimum `mvn -q test` from `quant-ai-platform/quant-services`.
- Acceptance conditions proving no endpoint behavior change.
- How to detect accidental URL changes.
- Whether any existing tests must be updated or added to lock controller mappings.

## Explicitly Out Of Scope

- No Java service implementation extraction.
- No `TaskQueryServiceImpl` internal refactor.
- No URL path changes unless explicitly approved by the user.
- No database schema changes.
- No frontend changes.
- No Python changes.
- No new endpoints or product features.
- No auth/gateway/config-store/Nacos migration.
- No microservice extraction decision.
- No reclassification of `ai-orchestration-service` as final architecture.

## Human Approval Recorded

User approved this steering decision.

Recorded approval:

- Phase 001 may proceed to Window 1 architecture planning.
- Default constraints apply: no breaking changes, URL paths remain stable, no behavior change, no new feature work.
- Expected later implementation window type: backend-only, unless Window 1 proves another window type is required.
- Phase 002 remains the fallback candidate but is not active.
