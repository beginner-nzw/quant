# Steering Decision - Phase 003

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

Latest phase handoffs consumed for startup recovery:

- `docs/harness/handoffs/steering-decision-phase-002.md`
- `docs/harness/handoffs/phase-002-architect.md`
- `docs/harness/handoffs/phase-002-implementation.md`
- `docs/harness/handoffs/phase-002-review.md`
- `docs/harness/handoffs/phase-002-fix-1-implementation.md`
- `docs/harness/handoffs/phase-002-review-fix-1.md`
- `docs/harness/handoffs/phase-002-final.md`

Missing matching handoff files:

- None.

## Latest Phase Consumed

Latest completed phase from `docs/harness/state/current-state.md`:

- Phase 002 - Split `TaskQueryServiceImpl` Internal Query Services.

Latest final handoff found:

- `docs/harness/handoffs/phase-002-final.md`.

Startup recovery result before this steering approval:

- `current-state.md` and `phase-002-final.md` agree that Phase 002 is completed with residual risk.
- No active next phase is approved.
- No open blocker is registered.
- Bootstrap Phase 001 recommendation is no longer current fact because Phase 001 and Phase 002 final handoffs exist.

## Current State Summary

- Current phase: Phase 003 - Contract Hardening for Workbench and Fallback.
- Current phase status: approved for Window 1 architecture planning only.
- Last completed phase: Phase 002 - Split `TaskQueryServiceImpl` Internal Query Services.
- Latest approving review: `docs/harness/handoffs/phase-002-review-fix-1.md`.
- Open blockers: none registered.
- Open architecture drift: `ai-orchestration-service` remains a multi-domain transition host.
- Open authority drift: `research-workbench` remains display-only and must not become SoT; frontend must remain a consumer; Python fallback metadata remains an auditable contract concern.
- Open contract drift: non-task domain endpoints keep legacy `/api/tasks/*` paths; workbench/fallback contracts still need stronger guardrails.
- Active transition hosts: `ai-orchestration-service`, internal domain query services inside it, legacy non-task `/api/tasks/*` paths, JSON config files, mock/demo ingest paths.
- Human approval status: Phase 003 approved by user with default constraints.

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
- Authority ambiguity remains in the workbench/fallback area because display aggregation and fallback hydration must not become business truth.
- Contract ambiguity remains because workbench and fallback behavior need explicit guardrails after Phase 002 narrowed internal query ownership.
- Transition host reduction already advanced in Phase 001 and Phase 002; the next bounded step should harden the contracts before service extraction.
- Eval/test coverage is part of Phase 003, but it is selected because it protects authority and contract boundaries, not as generic test cleanup.
- New feature work is not eligible while D001-D003 remain open.

## Candidate Score Table

Scores are 0-2. Behavior risk is inverted: 2 means low risk.

| Candidate | Main path protection | Authority clarity | Contract clarity | Transition host reduction | Behavior risk | Verification feasibility | Total | Steering result |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| Phase 003 - Contract Hardening for Workbench and Fallback | 1 | 2 | 2 | 0 | 2 | 2 | 9 | Primary |
| Phase 004 - Python AI Workflow Contract Cleanup | 1 | 2 | 1 | 0 | 1 | 1 | 6 | Fallback |
| Phase 005 - Decide Service Split or Continue Modular Monolith | 0 | 1 | 1 | 2 | 0 | 0 | 4 | Defer |

## Decision

Phase 003 is the approved next candidate for Window 1 architecture planning.

Window 0 does not approve itself, does not start Window 1, and does not authorize implementation.

## Primary Candidate

Phase 003 - Contract Hardening for Workbench and Fallback.

Bounded goal:

- Add contract-level guardrails so `research-workbench` remains display-only aggregation.
- Add focused tests or comments that prevent backend command paths from using workbench output as authoritative task/report/risk/strategy/market truth.
- Add feasible coverage for fallback reason visibility without changing business behavior.
- Preserve external API contracts and legacy URL paths.

Why this is the next bounded step:

- Phase 001 split the controller surface and Phase 002 split internal query ownership.
- The next residual high-priority debt is D003: workbench and fallback authority can still be misunderstood as SoT.
- Phase 003 strengthens authority and contract boundaries before attempting Python cleanup or service extraction.
- It is smaller and safer than Phase 005, and it does not prematurely treat transition hosts as final architecture.

## Fallback Candidate

Phase 004 - Python AI Workflow Contract Cleanup.

Fallback condition:

- Use only if the user rejects Phase 003 or explicitly wants fallback metadata cleanup to start in the Python AI engine first.

Why it is not primary:

- It improves fallback auditability, but it is lower than Phase 003 because the current post-Phase-002 residual risk is contract hardening around workbench and fallback boundaries in the integrated system.
- It may require Python verification where test availability is weaker, while Phase 003 can first define contract expectations for later Python cleanup.

## Why Other Phases Are Not Selected

- Phase 005 is premature because service split or modular-monolith permanence should not be decided until workbench/fallback contracts are hardened.
- Phase 001 is not eligible because it is already completed and frozen.
- Phase 002 is not eligible because it is already completed and frozen.
- New feature work is not eligible while D001-D003 remain open.

## Window 1 Must Define

Window 1 must convert Phase 003 into an implementation-ready handoff and must define:

- The exact workbench contract boundaries:
  - `research-workbench` is display-only aggregation.
  - It must not define task status, report truth, risk warning truth, strategy signal truth, market event truth or audit truth.
  - No backend command may depend on workbench output as authority.
- The exact fallback contract boundaries:
  - Which fallback/preferred-field behavior is display hydration only.
  - Which fallback reason or audit signal must remain observable.
  - Which fallback metadata checks belong in Phase 003 and which should be deferred to Phase 004.
- The authoritative endpoints and owning query services that must remain SoT/read-model boundaries after Phase 002.
- The allowed file scope for tests, comments, or contract assertions.
- Whether the implementation window should be backend-only by default or whether any Python work is truly necessary and separately bounded.
- Acceptance conditions proving no URL, response envelope, permission, DTO/VO/entity, database schema, Kafka, frontend or business behavior change.
- Verification commands, expected at minimum `mvn -q test` from `quant-ai-platform/quant-services`.
- Focused regression tests or static boundary checks that fail if workbench becomes a command source or domain truth source.

## Explicitly Out Of Scope

- No business behavior change.
- No new product features.
- No URL path changes.
- No endpoint aliases or compatibility routes.
- No frontend changes unless Window 1 proves a contract documentation need and the user explicitly approves mixed scope.
- No Python implementation changes unless Window 1 explicitly partitions them and the user approves mixed scope.
- No new agents, workflow branching, retry semantics, checkpointing or model behavior changes.
- No database schema changes.
- No Kafka contract changes.
- No gateway, auth-service, Nacos, Sentinel dashboard or deployment work.
- No report/risk/strategy/market microservice extraction.
- No reclassification of `ai-orchestration-service` or its internal query services as final architecture.
- No promotion of workbench, fallback snapshots or preferred/fallback display hydration to business truth.

## Human Approval Recorded

User approved this steering decision.

- Selected phase: Phase 003 - Contract Hardening for Workbench and Fallback.
- Default constraints apply: no breaking changes, keep URL paths stable, no business behavior change, no new feature work.
- Expected implementation window type after Window 1: backend-only.
- Fallback if rejected: Phase 004 - Python AI Workflow Contract Cleanup.
