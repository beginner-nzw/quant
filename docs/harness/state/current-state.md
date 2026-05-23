# Current Harness State

## Bootstrap Status

Bootstrap Harness Window completed the pre-Window-0 setup.

This file is the starting state for Window 0.

## Current Phase

None approved.

Latest frozen phase: Phase 003 - Contract Hardening for Workbench and Fallback.

## Current Phase Status

No active phase is approved.

Phase 003 is completed with residual risk after Window 3 approved `docs/harness/handoffs/phase-003-review.md`.

Window 0 must recover from this file and `docs/harness/handoffs/phase-003-final.md`. The user does not need to manually summarize Phase 003.

## Last Completed Phase

Phase 003 - Contract Hardening for Workbench and Fallback.

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

## Open Architecture Drift

- `ai-orchestration-service` is a transition host for multiple domains originally planned as separate services.
- Gateway/auth/config/service discovery architecture from the original plan is not implemented.

## Open Authority Drift

- Phase 002 moved risk, strategy, report, market-intelligence, audit, config dashboard and workbench read paths out of `TaskQueryServiceImpl` into internal domain query services.
- Phase 003 documented and tested Java backend workbench boundaries: workbench remains display-only aggregation, must not write domain facts, and must not feed backend command/projection authority.
- Python fallback cleanup remains deferred to a later phase.
- Frontend must remain a consumer and must not infer business truth.

## Open Contract Drift

- Phase 001 split the former multi-domain `TaskQueryController` surface into domain-specific controllers.
- Non-task domain endpoints still keep legacy `/api/tasks/*` paths by approved Phase 001 constraint.
- Workbench Java backend display-only contract is guarded by Phase 003 tests/comments, but frontend/Python consumer boundaries and Python fallback reason propagation remain residual risks.
- Python fallback metadata should remain auditable.

## Active Transition Hosts

- `ai-orchestration-service`
- Internal domain query services inside `ai-orchestration-service`
- Legacy `/api/tasks/*` paths for non-task domain surfaces
- JSON files under `quant-ai-platform/ai-config`
- Mock/demo ingest paths
- Python fallback path

## Candidate Next Phases

- Phase 004 - Python AI Workflow Contract Cleanup.
- Deferred: Phase 005 - Decide Service Split or Continue Modular Monolith.
- Candidate input: Phase 006 - Legacy `/api/tasks/*` Contract Freeze for Non-Task Domains.

## Human Approval Status

Phase 001 was approved by the user, implemented by Window 2, reviewed by Window 3 and frozen by Window 4 as completed with residual risk.

Phase 002 was approved by the user after Window 0 steering decision, implemented by Window 2, fixed by Window 2 Fix Pass 1, reviewed and approved by Window 3 Review Fix 1, and frozen by Window 4 as completed with residual risk.

Phase 003 was approved by the user after Window 0 steering decision, implemented by Window 2, reviewed and approved by Window 3, and frozen by Window 4 as completed with residual risk.

Completed Phase 003 constraints:

- No breaking changes.
- URL paths must remain stable.
- No business behavior change.
- No new feature work.
- Backend-only.

No next phase is approved.

Next step must be Window 0. Window 0 must read `docs/harness/handoffs/phase-003-final.md`, discover the matching Phase 003 steering, architect, implementation and review handoffs, score candidate next phases using `docs/harness/10-steering-state-machine.md`, propose exactly one primary candidate and one fallback candidate, and wait for human approval before Window 1 starts.
