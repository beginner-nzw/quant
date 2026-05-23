# Phase 004 Final Handoff

## Status

Window: Window 4 - Handoff.

Phase: Phase 004 - Python AI Workflow Contract Cleanup.

Phase status: completed with residual risk.

Latest review: `docs/harness/handoffs/phase-004-review.md`.

Latest Window 3 decision: approve.

Fix passes: none.

Window 4 did not change business code and did not select the next phase.

## Inputs Read

Required harness files:

- `docs/harness/05-transition-lifetime.md`
- `docs/harness/06-debt-register.md`
- `docs/harness/07-phase-backlog.md`
- `docs/harness/09-window-protocol.md`
- `docs/harness/10-steering-state-machine.md`
- `docs/harness/state/current-state.md`

Phase 004 handoffs:

- `docs/harness/handoffs/steering-decision-phase-004.md`
- `docs/harness/handoffs/phase-004-architect.md`
- `docs/harness/handoffs/phase-004-implementation.md`
- `docs/harness/handoffs/phase-004-review.md`

No Phase 004 fix implementation or review-fix handoff exists.

## Completed Scope

Phase 004 completed the approved Python AI workflow contract cleanup.

- Financial rule fallback now exposes a non-empty `fallbackReason` with `generationMode == "RULE_FALLBACK"`.
- Risk rule fallback now exposes a non-empty `fallbackReason` with `generationMode == "RULE_FALLBACK"`.
- Report `contextSnapshot` now carries report, financial, risk and market fallback provenance as optional map metadata.
- Planner and intent existing fallback reason behavior is guarded by focused Python tests.
- Python fallback provenance is visible through existing result metadata surfaces, including `reportMeta.contextSnapshot` / raw payload storage.
- Java production projection was not changed and must not use fallback provenance to create, delete, score or otherwise decide domain facts.

Verification recorded by Window 2 and confirmed by Window 3:

- `python -m compileall app` passed from `quant-ai-platform/quant-ai-engine`.
- `python -m unittest discover -s tests` passed with 43 tests from `quant-ai-platform/quant-ai-engine`.
- `mvn -q test` passed from `quant-ai-platform/quant-services`.
- `python -m pytest` was unavailable because `pytest` is not installed in the current environment.

## Contract And Authority State

Authority state changes:

- Python fallback provenance is now auditable for the in-scope fallback paths.
- Fallback metadata remains provenance only, not model-generated truth or business SoT.
- Workbench remains display-only aggregation under the Phase 003 guardrails.
- Frontend consumer authority boundaries remain residual risk for later steering.

Transition state changes:

- `docs/harness/05-transition-lifetime.md` now records Phase 004 progress against T4 Python Rule Fallback exit criteria.
- The Python fallback path remains an allowed transition mechanism, but future fallback surfaces must carry equivalent non-authoritative provenance.

## Unchanged Contracts

The following contracts stayed unchanged:

- Kafka topics and envelope fields.
- Python and Java top-level Kafka payload field lists.
- URL paths and HTTP methods.
- Frontend routes and API consumers.
- DTO, VO, entity and mapper shapes.
- Database schema.
- Java controller and production projection behavior.
- Workflow order, enabled-agent selection, task status transitions, final status and final stage.
- Existing fallback source ordering and fallback content behavior, except optional provenance metadata.

## Remaining Debt

- D001 remains open: `ai-orchestration-service` is still a multi-domain transition host.
- D002 remains open: non-task domain APIs still use legacy `/api/tasks/*` paths.
- D003 remains open but further mitigated: Phase 003 covered Java backend workbench authority, and Phase 004 covered Python fallback provenance; frontend consumer authority boundaries remain.
- D011 remains open: frontend tests are still absent and `pytest` is unavailable in the current environment.
- Phase 005 service split or modular-monolith permanence remains deferred until remaining authority and contract drift are better frozen.

## Latest State For Window 0

Window 0 should automatically recover this state from harness files:

- Current phase: none approved.
- Latest frozen phase: Phase 004 - Python AI Workflow Contract Cleanup.
- Phase 004 status: completed with residual risk.
- Last completed phase: Phase 004.
- Open blockers: none registered.
- Latest approving review: `docs/harness/handoffs/phase-004-review.md`.
- Startup recovery file: `docs/harness/handoffs/phase-004-final.md`.

Window 0 must read this final handoff plus the matching Phase 004 steering, architect, implementation and review handoffs. The user does not need to manually summarize Phase 004.

## Recommended Candidate Inputs For Window 0

Window 4 does not select the next phase. Window 0 must score candidates using `docs/harness/10-steering-state-machine.md`, propose exactly one primary candidate and one fallback candidate, and wait for human approval.

Recommended candidate inputs:

- Phase 006 - Legacy `/api/tasks/*` Contract Freeze for Non-Task Domains.
- Phase 007 - Frontend Consumer Authority Boundary Audit.
- Phase 005 - Decide Service Split or Continue Modular Monolith.

Phase 001, Phase 002, Phase 003 and Phase 004 are no longer candidates because they are completed and frozen.

## Files Changed In This Handoff

- `docs/harness/state/current-state.md`
- `docs/harness/05-transition-lifetime.md`
- `docs/harness/06-debt-register.md`
- `docs/harness/07-phase-backlog.md`
- `docs/harness/handoffs/phase-004-final.md`

