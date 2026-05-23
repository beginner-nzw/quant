# Current Harness State

## Bootstrap Status

Bootstrap Harness Window completed the pre-Window-0 setup.

This file is the starting state for Window 0.

## Current Phase

None approved.

Latest frozen phase: Phase 007 - Frontend Consumer Authority Boundary Audit.

## Current Phase Status

No active phase is approved.

Phase 007 is completed with residual risk after Window 3 approved `docs/harness/handoffs/phase-007-review.md`.

Window 0 must recover from this file and `docs/harness/handoffs/phase-007-final.md`. The user does not need to manually summarize Phase 007.

## Last Completed Phase

Phase 007 - Frontend Consumer Authority Boundary Audit.

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

## Completed Phase 004 Constraints

- No breaking changes.
- URL paths must remain stable.
- No business behavior change.
- No new feature work.
- Mixed Python/backend scope was approved, but implementation changed only Python production/test files plus the implementation handoff.
- Fallback provenance was added only inside existing Python dictionaries and `reportMeta.contextSnapshot` map metadata.
- No Java production, frontend, DTO/VO/entity, database schema, Kafka topic or top-level Kafka payload field changed.
- Window 3 reviewed and approved `phase-004-review.md`.
- `python -m compileall app`, `python -m unittest discover -s tests` and `mvn -q test` passed.
- `python -m pytest` was unavailable because `pytest` is not installed in the current environment.

## Completed Phase 007 Constraints

- No breaking changes.
- URL paths and frontend routes remained stable.
- No user-visible business behavior change.
- No new feature work.
- Frontend-focused implementation only.
- Production source changes were comments/JSDoc-style authority notes; the new guard script is not imported by production code.
- `ResearchWorkbenchData` is documented as display-only aggregation and guarded away from command APIs.
- `TaskReportContextSnapshot`, `reportMeta`, `generationMode`, `fallbackReason` and related fallback provenance are documented as display/audit metadata only.
- Workbench output remains display, navigation and existing task-create source-context prefill only.
- Existing frontend API endpoint strings, HTTP methods, function names, call signatures, response envelopes and TypeScript shapes remained unchanged.
- No Java, Python, database, Kafka, `ai-config`, package/dependency or build-config file changed.
- Window 3 reviewed and approved `phase-007-review.md`.
- `node scripts/authority-boundary-check.mjs` and `npm run build` passed from `quant-ui`.

## Open Architecture Drift

- `ai-orchestration-service` is a transition host for multiple domains originally planned as separate services.
- Gateway/auth/config/service discovery architecture from the original plan is not implemented.

## Open Authority Drift

- Phase 002 moved risk, strategy, report, market-intelligence, audit, config dashboard and workbench read paths out of `TaskQueryServiceImpl` into internal domain query services.
- Phase 003 documented and tested Java backend workbench boundaries: workbench remains display-only aggregation, must not write domain facts, and must not feed backend command/projection authority.
- Phase 004 made in-scope Python fallback provenance auditable for planner, intent, financial, risk, report and market fallback paths using existing metadata surfaces.
- Phase 007 documented and guarded frontend consumer boundaries for current workbench aggregation and fallback provenance surfaces.
- Fallback metadata remains provenance only and must not become model-generated truth or business SoT.
- Future frontend, backend or Python surfaces that expose workbench or fallback metadata must keep equivalent non-authoritative provenance guardrails.

## Open Contract Drift

- Phase 001 split the former multi-domain `TaskQueryController` surface into domain-specific controllers.
- Non-task domain endpoints still keep legacy `/api/tasks/*` paths by approved Phase 001 constraint.
- Workbench Java backend display-only contract is guarded by Phase 003 tests/comments.
- Phase 004 preserved Kafka topics, top-level payload fields, URL paths, frontend contracts, DTO/VO/entity shapes and database schema while adding optional fallback provenance inside existing map metadata.
- Phase 007 preserved existing frontend routes, API endpoint strings, HTTP methods, function names, call signatures, response envelopes and TypeScript shapes.
- Legacy non-task `/api/tasks/*` paths remain contract debt.
- Future fallback surfaces must continue preserving fallback provenance as non-authoritative metadata.

## Active Transition Hosts

- `ai-orchestration-service`
- Internal domain query services inside `ai-orchestration-service`
- Legacy `/api/tasks/*` paths for non-task domain surfaces
- Research workbench display aggregation
- JSON files under `quant-ai-platform/ai-config`
- Mock/demo ingest paths
- Python fallback path, now audited for Phase 004 in-scope provenance and Phase 007 current frontend consumers but still a transition mechanism

## Candidate Next Phases

- Phase 006 - Legacy `/api/tasks/*` Contract Freeze for Non-Task Domains.
- Deferred: Phase 005 - Decide Service Split or Continue Modular Monolith.
- Phase 001, Phase 002, Phase 003, Phase 004 and Phase 007 are no longer candidates because they are completed and frozen by Window 4.

Window 0 may propose a different bounded candidate only if it follows `docs/harness/10-steering-state-machine.md`, records the reason, and waits for human approval.

## Human Approval Status

Phase 001 was approved by the user, implemented by Window 2, reviewed by Window 3 and frozen by Window 4 as completed with residual risk.

Phase 002 was approved by the user after Window 0 steering decision, implemented by Window 2, fixed by Window 2 Fix Pass 1, reviewed and approved by Window 3 Review Fix 1, and frozen by Window 4 as completed with residual risk.

Phase 003 was approved by the user after Window 0 steering decision, implemented by Window 2, reviewed and approved by Window 3, and frozen by Window 4 as completed with residual risk.

Phase 004 was approved by the user after Window 0 steering decision in `docs/harness/handoffs/steering-decision-phase-004.md`, planned by Window 1, implemented by Window 2, reviewed and approved by Window 3, and frozen by Window 4 as completed with residual risk.

Phase 007 was approved by the user after Window 0 steering decision in `docs/harness/handoffs/steering-decision-phase-007.md`, planned by Window 1, implemented by Window 2, reviewed and approved by Window 3, and frozen by Window 4 as completed with residual risk.

Next step must be Window 0. Window 0 must read `docs/harness/handoffs/phase-007-final.md`, discover the matching Phase 007 steering, architect, implementation and review handoffs, score candidate next phases using `docs/harness/10-steering-state-machine.md`, propose exactly one primary candidate and one fallback candidate, and wait for human approval before Window 1 starts.
