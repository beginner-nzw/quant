# Current Harness State

## Bootstrap Status

Bootstrap Harness Window completed the pre-Window-0 setup.

This file is the starting state for Window 0.

## Current Phase

Phase 003 - Contract Hardening for Workbench and Fallback is approved for Window 1 architecture planning.

Latest frozen phase: Phase 002 - Split `TaskQueryServiceImpl` Internal Query Services.

## Current Phase Status

Phase 003 is approved for Window 1 architecture planning only.

Window 0 wrote and user approved `docs/harness/handoffs/steering-decision-phase-003.md`.

This approval does not authorize implementation. Window 1 must produce `docs/harness/handoffs/phase-003-architect.md`, and the user must approve that handoff before Window 2 starts.

## Last Completed Phase

Phase 002 - Split `TaskQueryServiceImpl` Internal Query Services.

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

## Open Architecture Drift

- `ai-orchestration-service` is a transition host for multiple domains originally planned as separate services.
- Gateway/auth/config/service discovery architecture from the original plan is not implemented.

## Open Authority Drift

- Phase 002 moved risk, strategy, report, market-intelligence, audit, config dashboard and workbench read paths out of `TaskQueryServiceImpl` into internal domain query services.
- `research-workbench` aggregation remains display-only and must not become SoT.
- Frontend must remain a consumer and must not infer business truth.

## Open Contract Drift

- Phase 001 split the former multi-domain `TaskQueryController` surface into domain-specific controllers.
- Non-task domain endpoints still keep legacy `/api/tasks/*` paths by approved Phase 001 constraint.
- Workbench/fallback contracts still need stronger boundaries and regression tests before they can be considered frozen.
- Python fallback metadata should remain auditable.

## Active Transition Hosts

- `ai-orchestration-service`
- Internal domain query services inside `ai-orchestration-service`
- Legacy `/api/tasks/*` paths for non-task domain surfaces
- JSON files under `quant-ai-platform/ai-config`
- Mock/demo ingest paths

## Candidate Next Phases

- Active: Phase 003 - Contract Hardening for Workbench and Fallback.
- Fallback: Phase 004 - Python AI Workflow Contract Cleanup.
- Deferred: Phase 005 - Decide Service Split or Continue Modular Monolith.

## Human Approval Status

Phase 001 was approved by the user, implemented by Window 2, reviewed by Window 3 and frozen by Window 4 as completed with residual risk.

Phase 002 was approved by the user after Window 0 steering decision, implemented by Window 2, fixed by Window 2 Fix Pass 1, reviewed and approved by Window 3 Review Fix 1, and frozen by Window 4 as completed with residual risk.

Phase 003 was approved by the user after Window 0 steering decision.

Approval constraints:

- No breaking changes.
- URL paths must remain stable.
- No business behavior change.
- No new feature work.
- Expected later implementation window type: backend-only.

Next step must be Window 1. Window 1 must read `docs/harness/handoffs/steering-decision-phase-003.md`, produce `docs/harness/handoffs/phase-003-architect.md`, and wait for human approval before any implementation starts.
