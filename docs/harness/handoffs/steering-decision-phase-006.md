# Steering Decision - Phase 006

## Status

Window: Window 0 - Steering.

Decision: Phase 006 approved by user for Window 1 architecture planning.

This file does not approve implementation and does not authorize any business code change. Window 1 must produce `docs/harness/handoffs/phase-006-architect.md`, and the user must approve that handoff before Window 2 starts.

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

Startup recovery handoffs discovered:

- `docs/harness/handoffs/phase-000-harness-baseline.md`
- `docs/harness/handoffs/steering-decision-phase-001.md`
- `docs/harness/handoffs/phase-001-architect.md`
- `docs/harness/handoffs/phase-001-implementation.md`
- `docs/harness/handoffs/phase-001-review.md`
- `docs/harness/handoffs/phase-001-final.md`
- `docs/harness/handoffs/steering-decision-phase-002.md`
- `docs/harness/handoffs/phase-002-architect.md`
- `docs/harness/handoffs/phase-002-implementation.md`
- `docs/harness/handoffs/phase-002-review.md`
- `docs/harness/handoffs/phase-002-fix-1-implementation.md`
- `docs/harness/handoffs/phase-002-review-fix-1.md`
- `docs/harness/handoffs/phase-002-final.md`
- `docs/harness/handoffs/steering-decision-phase-003.md`
- `docs/harness/handoffs/phase-003-architect.md`
- `docs/harness/handoffs/phase-003-implementation.md`
- `docs/harness/handoffs/phase-003-review.md`
- `docs/harness/handoffs/phase-003-final.md`
- `docs/harness/handoffs/steering-decision-phase-004.md`
- `docs/harness/handoffs/phase-004-architect.md`
- `docs/harness/handoffs/phase-004-implementation.md`
- `docs/harness/handoffs/phase-004-review.md`
- `docs/harness/handoffs/phase-004-final.md`
- `docs/harness/handoffs/steering-decision-phase-007.md`
- `docs/harness/handoffs/phase-007-architect.md`
- `docs/harness/handoffs/phase-007-implementation.md`
- `docs/harness/handoffs/phase-007-review.md`
- `docs/harness/handoffs/phase-007-final.md`

Latest phase handoffs consumed for startup recovery:

- `docs/harness/handoffs/steering-decision-phase-007.md`
- `docs/harness/handoffs/phase-007-architect.md`
- `docs/harness/handoffs/phase-007-implementation.md`
- `docs/harness/handoffs/phase-007-review.md`
- `docs/harness/handoffs/phase-007-final.md`

Missing matching handoff files:

- None.

## Latest Phase Consumed

Latest completed phase from `docs/harness/state/current-state.md`:

- Phase 007 - Frontend Consumer Authority Boundary Audit.

Latest final handoff found:

- `docs/harness/handoffs/phase-007-final.md`.

Startup recovery result:

- `current-state.md` and `phase-007-final.md` agree that Phase 007 is completed with residual risk.
- No active next phase is approved.
- No open blocker is registered.
- Bootstrap Phase 001 recommendation is no longer current fact because Phase 001, Phase 002, Phase 003, Phase 004 and Phase 007 final handoffs exist.

## Current State Summary

- Current phase: none approved.
- Current phase status: no active phase; Phase 007 is completed with residual risk.
- Last completed phase: Phase 007 - Frontend Consumer Authority Boundary Audit.
- Latest blocked phase: none registered.
- Latest approving review: `docs/harness/handoffs/phase-007-review.md`.
- Open blockers: none registered.
- Open architecture drift: `ai-orchestration-service` remains a multi-domain transition host; gateway/auth/config/service discovery architecture from the original plan is not implemented.
- Open authority drift: current known backend, Python and frontend workbench/fallback surfaces have guardrails, but future surfaces must preserve equivalent non-authoritative boundaries.
- Open contract drift: non-task domain endpoints still keep approved legacy `/api/tasks/*` paths; this is the largest remaining bounded contract debt.
- Active transition hosts: `ai-orchestration-service`, internal domain query services inside it, legacy non-task `/api/tasks/*` paths, research workbench display aggregation, JSON config files, mock/demo ingest paths and Python fallback path.
- Human approval status: no next phase is approved.

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
- Known current authority ambiguity around workbench and fallback surfaces was reduced by Phase 003, Phase 004 and Phase 007; residual authority risk remains mainly for future surfaces.
- Contract ambiguity remains open and concrete: non-task domains are still exposed under the legacy `/api/tasks/*` namespace.
- Transition host reduction remains important, but Phase 005 should not decide service split or modular-monolith permanence before the remaining D002 contract drift is frozen.
- Eval/test work should be attached to the selected contract phase rather than selected as generic coverage work.
- New feature work is not eligible while D001-D002 remain open.

## Candidate Score Table

Scores are 0-2. Behavior risk is inverted: 2 means low risk.

| Candidate | Main path protection | Authority clarity | Contract clarity | Transition host reduction | Behavior risk | Verification feasibility | Total | Steering result |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| Phase 006 - Legacy `/api/tasks/*` Contract Freeze for Non-Task Domains | 1 | 1 | 2 | 0 | 2 | 2 | 8 | Primary |
| Phase 005 - Decide Service Split or Continue Modular Monolith | 0 | 1 | 1 | 2 | 0 | 0 | 4 | Fallback only with explicit override |

Tie-break:

- No tie exists.
- Phase 006 is selected because it addresses the highest-order remaining issue in the decision order: contract ambiguity.
- Phase 005 is lower order because it is a transition-host or architecture-shape decision while D002 contract drift remains open.

## Primary Candidate

Phase 006 - Legacy `/api/tasks/*` Contract Freeze for Non-Task Domains.

Bounded goal:

- Declare which non-task domain surfaces are intentionally preserved under `/api/tasks/*`.
- Guard the approved legacy contracts against accidental URL, HTTP method, permission, request binding, response-envelope or controller-ownership drift.
- Preserve URL stability and current external behavior.
- Avoid introducing new aliases, route moves or breaking changes.

Why this is the next bounded step:

- Phase 001 split the controller surface but intentionally preserved legacy paths.
- Phase 002 split internal read-model services but intentionally preserved external contracts.
- Phase 003, Phase 004 and Phase 007 reduced workbench and fallback authority risk across backend, Python and frontend current surfaces.
- The largest remaining bounded risk is now D002: legacy non-task `/api/tasks/*` contracts can continue drifting unless they are explicitly frozen.
- This is smaller and safer than Phase 005 because it does not extract services, does not decide permanent host shape and does not change runtime behavior.

## Fallback Candidate

Phase 005 - Decide Service Split or Continue Modular Monolith.

Fallback condition:

- Use Phase 005 only if the user explicitly rejects Phase 006 or explicitly accepts making a transition-host decision while the D002 legacy contract debt remains open.

Bounded fallback goal:

- Decide whether to continue as a modular monolith inside `ai-orchestration-service` or begin extracting domain services later.
- Record the decision as architecture policy only.
- Require explicit human approval for any service extraction, breaking change, gateway/auth dependency, DB/Nacos move or URL migration.

Why it is not primary:

- It targets transition host reduction, which is lower in the state-machine decision order than unresolved contract ambiguity.
- Its behavior and verification risk is high because a service split decision can imply broader follow-on changes that are not yet safely bounded.
- It is a bigger architecture decision, not the next smallest contract-hardening step.

## Why Other Phases Are Not Selected

- Phase 001 is not eligible because it is completed and frozen.
- Phase 002 is not eligible because it is completed and frozen.
- Phase 003 is not eligible because it is completed and frozen.
- Phase 004 is not eligible because it is completed and frozen.
- Phase 007 is not eligible because it is completed and frozen.
- Generic eval/test expansion is not selected because the next tests should be scoped to the Phase 006 contract freeze.
- New product feature work is not eligible while D001-D002 remain open.
- Gateway, auth-service, Nacos, Sentinel, real ingest, config-store migration and new agent work are farther goals and would skip the current contract ambiguity.

## Window 1 Must Define

Window 1 must convert Phase 006 into an implementation-ready handoff and must define:

- The exact non-task endpoint inventory that is intentionally preserved under `/api/tasks/*`.
- Which endpoints are authoritative read models, display aggregations or command contracts according to `04-contract-map.md`.
- Which controller classes currently own those endpoints after Phase 001.
- Which request bindings, HTTP methods, response envelopes and permission or role-access expectations must remain stable.
- Which mapping assertions, Spring MVC tests, source checks or other focused contract tests are allowed.
- The rule that no new URL aliases, route migrations or breaking changes are allowed without later explicit human approval.
- The rule that no frontend, Python, DTO/VO/entity, database schema, Kafka, `ai-config`, dependency or business behavior change is expected.
- The expected verification commands, likely including `mvn -q test` from `quant-ai-platform/quant-services`.
- Stop rules if freezing the contract requires changing existing URL paths, endpoint shapes, permission behavior, DTO/VO/entity shape, frontend calls or runtime behavior.

## Explicitly Out Of Scope

- No business feature work.
- No endpoint move or rename.
- No new domain URL aliases.
- No breaking change.
- No frontend route or API caller change.
- No DTO, VO, entity, database schema or Kafka change.
- No Python change.
- No Java production behavior change beyond narrowly scoped contract documentation or test-only guardrails unless Window 1 proves it is necessary and still behavior-preserving.
- No service extraction.
- No modular-monolith permanence decision unless the user rejects Phase 006 and explicitly approves Phase 005.
- No gateway, auth-service, Nacos, Sentinel, deployment or data-ingest work.
- No reclassification of `ai-orchestration-service` or legacy `/api/tasks/*` paths as final architecture.

## Human Approval Recorded

User approved this steering decision.

Recorded approval:

- Selected phase: Phase 006 - Legacy `/api/tasks/*` Contract Freeze for Non-Task Domains.
- No breaking changes.
- Keep all URL paths stable.
- No business behavior change.
- No new feature work.
- Expected later implementation window type: backend-focused contract/test/documentation work, as precisely bounded by Window 1.

Window 0 stops here. Window 1 is the next required window and must not start implementation.
