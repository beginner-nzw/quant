# Steering Decision - Phase 007

## Status

Window: Window 0 - Steering.

Decision: Phase 007 approved by user for Window 1 architecture planning.

This file does not approve implementation and does not authorize any business code change. Window 1 must produce `docs/harness/handoffs/phase-007-architect.md`, and the user must approve that handoff before Window 2 starts.

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

Latest phase handoffs consumed for startup recovery:

- `docs/harness/handoffs/steering-decision-phase-004.md`
- `docs/harness/handoffs/phase-004-architect.md`
- `docs/harness/handoffs/phase-004-implementation.md`
- `docs/harness/handoffs/phase-004-review.md`
- `docs/harness/handoffs/phase-004-final.md`

Missing matching handoff files:

- None.

## Latest Phase Consumed

Latest completed phase from `docs/harness/state/current-state.md`:

- Phase 004 - Python AI Workflow Contract Cleanup.

Latest final handoff found:

- `docs/harness/handoffs/phase-004-final.md`.

Startup recovery result:

- `current-state.md` and `phase-004-final.md` agree that Phase 004 is completed with residual risk.
- No active next phase is approved.
- No open blocker is registered.
- Bootstrap Phase 001 recommendation is no longer current fact because Phase 001, Phase 002, Phase 003 and Phase 004 final handoffs exist.

## Current State Summary

- Current phase: none approved.
- Current phase status: no active phase; Phase 004 is completed with residual risk.
- Last completed phase: Phase 004 - Python AI Workflow Contract Cleanup.
- Latest approving review: `docs/harness/handoffs/phase-004-review.md`.
- Open blockers: none registered.
- Open architecture drift: `ai-orchestration-service` remains a multi-domain transition host; gateway/auth/config/service discovery architecture from the original plan is not implemented.
- Open authority drift: Phase 003 guarded Java backend workbench boundaries, and Phase 004 made Python fallback provenance auditable, but frontend consumer authority boundaries remain residual risk.
- Open contract drift: non-task domain endpoints still keep legacy `/api/tasks/*` paths; future fallback surfaces must preserve fallback provenance as non-authoritative metadata.
- Active transition hosts: `ai-orchestration-service`, internal domain query services inside `ai-orchestration-service`, legacy non-task `/api/tasks/*` paths, JSON config files, mock/demo ingest paths, and Python fallback path.
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
- Authority ambiguity remains: frontend consumers must not promote workbench aggregation, fallback provenance, or mixed-domain display responses into business source-of-truth behavior.
- Contract ambiguity remains: legacy non-task domain APIs still live under `/api/tasks/*`.
- Transition host reduction remains important, but Phase 005 service-split decisions should wait until remaining authority and contract drift are better frozen.
- Eval/test work should be attached to the selected authority or contract phase, not selected as generic test work.
- New feature work is not eligible while D001-D003 remain open.

## Candidate Score Table

Scores are 0-2. Behavior risk is inverted: 2 means low risk.

| Candidate | Main path protection | Authority clarity | Contract clarity | Transition host reduction | Behavior risk | Verification feasibility | Total | Steering result |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| Phase 007 - Frontend Consumer Authority Boundary Audit | 1 | 2 | 2 | 0 | 2 | 1 | 8 | Primary |
| Phase 006 - Legacy `/api/tasks/*` Contract Freeze for Non-Task Domains | 1 | 1 | 2 | 0 | 2 | 2 | 8 | Fallback |
| Phase 005 - Decide Service Split or Continue Modular Monolith | 0 | 1 | 1 | 2 | 0 | 0 | 4 | Defer |

Tie-break:

- Phase 007 and Phase 006 both score 8.
- Phase 007 is selected because it addresses the higher-order remaining authority ambiguity first.
- Phase 006 remains the fallback because it is the next bounded contract-hardening step if the user rejects frontend scope or wants backend-only contract freeze first.

## Primary Candidate

Phase 007 - Frontend Consumer Authority Boundary Audit.

Bounded goal:

- Audit frontend API consumers for workbench, report, risk, strategy, market and AI result/fallback metadata surfaces.
- Document or guard which frontend surfaces are display-only consumers of Java projections or Python provenance metadata.
- Prevent frontend code from treating workbench aggregation, fallback provenance, or mixed-domain display responses as business source of truth.
- Preserve existing URLs, response envelopes, route behavior and user-visible business behavior.

Why this is the next bounded step:

- Phase 003 already hardened the Java backend workbench boundary.
- Phase 004 already made in-scope Python fallback provenance auditable.
- The remaining D003 authority risk now sits at the frontend consumer boundary.
- This is smaller and safer than Phase 005 because it does not decide service extraction or permanent host shape.
- This is not a feature phase; it is an authority-boundary audit and guardrail phase over existing behavior.

## Fallback Candidate

Phase 006 - Legacy `/api/tasks/*` Contract Freeze for Non-Task Domains.

Fallback condition:

- Use Phase 006 if the user rejects frontend scope, requires the next phase to remain backend-only, or wants the legacy route contract frozen before frontend consumer audit.

Bounded fallback goal:

- Declare and guard approved legacy `/api/tasks/*` non-task domain contracts.
- Add focused contract tests or mapping assertions that prevent accidental URL, method, permission, request binding, response-envelope or controller-ownership drift.
- Preserve URL stability and do not introduce new aliases or breaking route moves.

Why it is not primary:

- It improves D002 contract clarity and has stronger verification feasibility.
- It does not directly close the higher-order residual authority ambiguity that remains after Phase 003 and Phase 004: frontend consumers can still accidentally treat display or provenance metadata as truth.

## Why Other Phases Are Not Selected

- Phase 005 is premature because service split or modular-monolith permanence should not be decided while D002 and D003 remain open.
- Phase 001 is not eligible because it is completed and frozen.
- Phase 002 is not eligible because it is completed and frozen.
- Phase 003 is not eligible because it is completed and frozen.
- Phase 004 is not eligible because it is completed and frozen.
- New feature work is not eligible while D001-D003 remain open.

## Window 1 Must Define

Window 1 must convert Phase 007 into an implementation-ready handoff and must define:

- The exact frontend files and API consumers in scope, likely covering workbench, report, risk, strategy, market, audit and task-detail surfaces.
- Which frontend data is display-only, which data is authoritative backend projection output, and which fallback fields are provenance metadata only.
- The rule that frontend must not infer task status truth, report truth, risk warning truth, strategy signal truth, market event truth or audit truth from aggregation views.
- The rule that `research-workbench` remains display-only and cannot feed frontend command decisions as source-of-truth data.
- The rule that Python fallback provenance, including `reportMeta.contextSnapshot` metadata, remains audit/display metadata and cannot become model-generated truth or business SoT.
- Whether any tests, type checks, static assertions or comments are allowed, and the exact verification command, expected at minimum `npm run build` from `quant-ui` if frontend files are changed.
- Whether backend tests are needed only to support frontend boundary assertions, with no Java production change expected.
- The exact acceptance criteria proving URLs, response envelopes, route behavior and user-visible business behavior remain unchanged.
- Stop rules if a backend contract change, DTO/VO/entity change, URL alias, new frontend feature, or business behavior change appears necessary.

## Explicitly Out Of Scope

- No new product feature.
- No backend URL path change.
- No new URL alias.
- No DTO, VO, entity, database schema or Kafka change.
- No Java production projection change.
- No Python fallback behavior change.
- No service extraction or modular-monolith permanence decision.
- No gateway, auth-service, Nacos, Sentinel dashboard or deployment work.
- No new frontend business truth resolver.
- No promotion of workbench aggregation or fallback provenance into business truth.
- No reclassification of `ai-orchestration-service`, legacy `/api/tasks/*` paths, mock ingest or Python fallback as final architecture.

## Human Approval Recorded

User approved this steering decision.

Recorded approval:

- Selected phase: Phase 007 - Frontend Consumer Authority Boundary Audit.
- No breaking changes.
- Keep URL paths stable.
- No business behavior change.
- No new feature work.
- Expected later implementation window type: frontend-focused, with tests/static assertions/docs as defined by Window 1.

Window 0 stops here. Window 1 is the next required window and must not start implementation.
