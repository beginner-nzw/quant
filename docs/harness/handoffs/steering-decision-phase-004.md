# Steering Decision - Phase 004

## Status

Window: Window 0 - Steering.

Decision: Phase 004 approved by user for Window 1 architecture planning.

This file does not approve implementation and does not authorize any business code change. Window 1 must produce `docs/harness/handoffs/phase-004-architect.md`, and the user must approve that handoff before Window 2 starts.

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

Latest phase handoffs consumed for startup recovery:

- `docs/harness/handoffs/steering-decision-phase-003.md`
- `docs/harness/handoffs/phase-003-architect.md`
- `docs/harness/handoffs/phase-003-implementation.md`
- `docs/harness/handoffs/phase-003-review.md`
- `docs/harness/handoffs/phase-003-final.md`

Missing matching handoff files:

- None.

## Latest Phase Consumed

Latest completed phase from `docs/harness/state/current-state.md`:

- Phase 003 - Contract Hardening for Workbench and Fallback.

Latest final handoff found:

- `docs/harness/handoffs/phase-003-final.md`.

Startup recovery result:

- `current-state.md` and `phase-003-final.md` agree that Phase 003 is completed with residual risk.
- No active next phase is approved.
- No open blocker is registered.
- Bootstrap Phase 001 recommendation is no longer current fact because Phase 001, Phase 002 and Phase 003 final handoffs exist.

## Current State Summary

- Current phase: none approved.
- Current phase status: no active phase; Phase 003 is completed with residual risk.
- Last completed phase: Phase 003 - Contract Hardening for Workbench and Fallback.
- Latest approving review: `docs/harness/handoffs/phase-003-review.md`.
- Open blockers: none registered.
- Open architecture drift: `ai-orchestration-service` remains a multi-domain transition host; gateway/auth/config/service discovery architecture from the original plan is not implemented.
- Open authority drift: Java backend workbench boundaries are guarded, but Python fallback auditability and frontend/Python consumer authority boundaries remain residual risks.
- Open contract drift: non-task domain endpoints still keep legacy `/api/tasks/*` paths; Python fallback metadata should remain visible and auditable.
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
- Authority ambiguity remains in Python fallback behavior: fallback results can complete the AI workflow, but must not silently become model-generated or business-authoritative truth.
- Contract ambiguity remains around fallback reason or equivalent audit signal visibility across Python result generation and Java projection/audit inspection.
- Contract ambiguity also remains for legacy non-task `/api/tasks/*` paths, but that is lower than the remaining authority ambiguity in the decision order.
- Transition host reduction remains important, but Phase 005 service-split decisions should wait until fallback and legacy contract boundaries are clearer.
- Eval/test coverage should be part of the selected authority/contract phase, not selected as generic test work.
- New feature work is not eligible while D001-D003 remain open.

## Candidate Score Table

Scores are 0-2. Behavior risk is inverted: 2 means low risk.

| Candidate | Main path protection | Authority clarity | Contract clarity | Transition host reduction | Behavior risk | Verification feasibility | Total | Steering result |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| Phase 004 - Python AI Workflow Contract Cleanup | 1 | 2 | 2 | 1 | 1 | 1 | 8 | Primary |
| Phase 006 - Legacy `/api/tasks/*` Contract Freeze for Non-Task Domains | 1 | 1 | 2 | 0 | 2 | 2 | 8 | Fallback |
| Phase 005 - Decide Service Split or Continue Modular Monolith | 0 | 1 | 1 | 2 | 0 | 0 | 4 | Defer |

Tie-break:

- Phase 004 and Phase 006 both score 8.
- Phase 004 is selected because it addresses the higher-order remaining authority ambiguity first.
- Phase 006 remains the fallback because it is the next bounded contract-hardening step if the user rejects Python/mixed-scope work.

## Primary Candidate

Phase 004 - Python AI Workflow Contract Cleanup.

Bounded goal:

- Separate AI execution fallback from business truth.
- Ensure existing model fallback outputs carry a fallback reason or equivalent auditable signal.
- Confirm report, risk and financial fallback state is visible in result metadata or agent audit.
- Confirm Java projection/audit consumers can inspect fallback metadata without treating fallback content as authoritative business fact.
- Preserve current workflow behavior and do not add new agents.

Why this is the next bounded step:

- Phase 003 completed Java backend workbench guardrails and explicitly deferred Python fallback reason propagation.
- `05-transition-lifetime.md` T4 lists Python rule fallback exit criteria as still pending.
- `06-debt-register.md` keeps D003 open because Python fallback auditability and consumer boundaries remain outside Phase 003.
- The phase is smaller than a service split and more urgent than freezing legacy URL debt because fallback auditability is an authority issue, not only a namespace issue.
- This phase continues contract/authority hardening rather than adding product behavior.

## Fallback Candidate

Phase 006 - Legacy `/api/tasks/*` Contract Freeze for Non-Task Domains.

Fallback condition:

- Use only if the user rejects Phase 004, rejects Python/mixed implementation scope, or explicitly wants the next phase to remain backend-only Java contract hardening.

Bounded fallback goal:

- Declare and guard approved legacy `/api/tasks/*` non-task domain contracts.
- Add focused contract tests or mapping assertions that prevent accidental URL, method, permission, request binding, response-envelope or controller-ownership drift.
- Preserve URL stability and do not introduce new aliases or breaking route moves.

Why it is not primary:

- It improves D002 contract clarity and has lower behavior risk, but it does not address the higher-order remaining Python fallback authority ambiguity.
- It freezes an existing transition contract rather than reducing the risk that fallback data is mistaken for model-generated or business-authoritative truth.

## Why Other Phases Are Not Selected

- Phase 005 is premature because service split or modular-monolith permanence should not be decided while D002 and Python fallback authority/contract drift remain open.
- Phase 001 is not eligible because it is completed and frozen.
- Phase 002 is not eligible because it is completed and frozen.
- Phase 003 is not eligible because it is completed and frozen.
- New feature work is not eligible while D001-D003 remain open.

## Window 1 Must Define

Window 1 must convert Phase 004 into an implementation-ready handoff and must define:

- The exact Python fallback surfaces in scope, including model-call failure, disabled-model, incomplete-output, financial/report/risk fallback and any existing market-data fallback path.
- Which fallback reason or equivalent audit signal must be present for each in-scope fallback.
- Whether fallback metadata is carried in result metadata, agent audit records, both, or another already-approved contract field.
- Whether Java projection can inspect the metadata with no schema, Kafka topic or frontend contract change.
- Which files are allowed for Python and Java/backend changes, and whether Window 2 should be Python-only or partitioned/mixed.
- The exact behavior that must remain unchanged: workflow completion, fallback selection, report/risk/financial content shape, task status handling and Java projection semantics.
- Which tests or compile checks are required, expected at minimum `python -m compileall app` from `quant-ai-platform/quant-ai-engine`, and Java checks if Java inspection or projection tests are included.
- Acceptance conditions proving fallback remains auditable but is not promoted to business truth.
- Stop rules if new DTO/VO fields, Kafka message fields, database schema changes, frontend changes, new agents, new fallback sources or behavior changes appear necessary.

## Explicitly Out Of Scope

- No new agents.
- No workflow branching, parallel execution, checkpointing, retry redesign or model behavior change.
- No new product feature.
- No URL path change.
- No frontend change unless the user explicitly approves a mixed scope after Window 1 proves it is necessary.
- No database schema change.
- No Kafka topic change.
- No service extraction or modular-monolith permanence decision.
- No gateway, auth-service, Nacos, Sentinel dashboard or deployment work.
- No removal or downgrade of existing fallback unless explicitly approved by the user.
- No promotion of fallback output, fallback market snapshots or display hydration into business truth.
- No reclassification of `ai-orchestration-service` or Python fallback as final architecture.

## Human Approval Recorded

User approved this steering decision.

Recorded approval:

- Selected phase: Phase 004 - Python AI Workflow Contract Cleanup.
- Default constraints apply: no breaking changes, keep URL paths stable, no business behavior change, no new feature work.
- Expected later implementation window type: mixed Python/backend, because Phase 004 must define Python fallback auditability and Java projection/audit inspection boundaries.
- Phase 006 remains the fallback candidate if Phase 004 is later rejected, blocked, or narrowed out by human decision.

Window 0 stops here. Window 1 is the next required window and must not start implementation.
