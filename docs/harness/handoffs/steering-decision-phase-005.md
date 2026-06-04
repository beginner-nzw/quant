# Steering Decision - Phase 005

## Status

Window: Window 0 - Steering.

Decision: Phase 005 approved by user for Window 1 architecture planning.

Human approval status: approved.

This file does not approve implementation, does not authorize business code change, and does not start Window 2. Window 1 must produce `docs/harness/handoffs/phase-005-architect.md`, and the user must approve that handoff before any implementation window starts.

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

Handoff directory was listed. Files discovered include completed/final handoffs for Phase 001, Phase 002, Phase 003, Phase 004, Phase 006 and Phase 007.

Phase 006 handoffs consumed for startup recovery:

- `docs/harness/handoffs/steering-decision-phase-006.md`
- `docs/harness/handoffs/phase-006-architect.md`
- `docs/harness/handoffs/phase-006-implementation.md`
- `docs/harness/handoffs/phase-006-review.md`
- `docs/harness/handoffs/phase-006-fix-1-implementation.md`
- `docs/harness/handoffs/phase-006-review-fix-1.md`
- `docs/harness/handoffs/phase-006-fix-2-implementation.md`
- `docs/harness/handoffs/phase-006-review-fix-2.md`
- `docs/harness/handoffs/phase-006-fix-3-implementation.md`
- `docs/harness/handoffs/phase-006-review-fix-3.md`
- `docs/harness/handoffs/phase-006-final.md`

Phase 007 handoffs additionally consumed to reconcile state ordering:

- `docs/harness/handoffs/steering-decision-phase-007.md`
- `docs/harness/handoffs/phase-007-architect.md`
- `docs/harness/handoffs/phase-007-implementation.md`
- `docs/harness/handoffs/phase-007-review.md`
- `docs/harness/handoffs/phase-007-final.md`

Missing matching handoff files:

- None for the consumed Phase 006 and Phase 007 recovery sets.

## Latest Phase Consumed

Latest completed phase from `docs/harness/state/current-state.md`:

- Phase 006 - Legacy `/api/tasks/*` Contract Freeze for Non-Task Domains.

Latest blocked phase:

- None registered.

Startup recovery note:

- `current-state.md` contains Phase 007 completion details, but its `Last Completed Phase` and "latest frozen phase" point to Phase 006.
- `phase-007-final.md` recommended Phase 006 as a later candidate, and `phase-006-final.md` shows Phase 006 was subsequently completed with Window 3 approval.
- This is treated as a recoverable ordering inconsistency, not a blocker, because the safe next step is clear from `phase-006-final.md`, `06-debt-register.md` and `07-phase-backlog.md`.

## Current State Summary

- Current phase: none approved.
- Current phase status: no active phase.
- Last completed phase: Phase 006 completed with residual risk.
- Phase 007 is also completed with residual risk and was read for context reconciliation.
- Open blockers: none registered.
- Open architecture drift: `ai-orchestration-service` remains a multi-domain transition host.
- Open authority drift: current known backend, Python and frontend workbench/fallback surfaces have guardrails, but future surfaces must preserve equivalent non-authoritative boundaries.
- Open contract drift: legacy non-task `/api/tasks/*` paths remain transition debt, but Phase 006 froze the approved inventory and guards against silent drift.
- Active transition hosts: `ai-orchestration-service`, internal domain query services, legacy non-task `/api/tasks/*` paths, research workbench display aggregation, JSON config files, mock/demo ingest paths and Python fallback path.
- Human approval status: Phase 005 approved for Window 1 architecture planning.

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
- Current known workbench/fallback authority ambiguity has been reduced by Phase 003, Phase 004 and Phase 007.
- Current legacy `/api/tasks/*` contract drift has been bounded by Phase 006; route migration remains transition debt, but any migration would require a later explicit breaking-change decision.
- The highest remaining high-severity issue is D001: `ai-orchestration-service` is still a multi-domain transition host.
- Therefore the next bounded step should address transition-host policy before any service extraction, route migration, gateway/auth work, config migration, new agents or product features.

## Candidate Score Table

Scores are 0-2. Behavior risk is inverted: 2 means low risk.

| Candidate | Main path protection | Authority clarity | Contract clarity | Transition host reduction | Behavior risk | Verification feasibility | Total | Steering result |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| Phase 005 - Decide Service Split or Continue Modular Monolith | 1 | 2 | 1 | 2 | 2 | 1 | 9 | Primary |
| Phase 008 - Transition Host Exit Criteria Inventory | 1 | 2 | 1 | 1 | 2 | 1 | 8 | Fallback |
| Domain URL migration / route cleanup | 0 | 1 | 2 | 1 | 0 | 0 | 4 | Defer |
| JSON config store decision | 0 | 1 | 1 | 1 | 1 | 1 | 5 | Defer |
| Generic eval or e2e expansion | 1 | 0 | 0 | 0 | 2 | 2 | 5 | Defer |
| New product feature or new agent work | 0 | 0 | 0 | 0 | 0 | 0 | 0 | Not eligible |

Tie-break:

- No tie changes the result.
- Phase 005 is selected because it addresses the highest-order remaining actionable issue: transition host reduction and architecture policy for D001.

## Primary Candidate

Phase 005 - Decide Service Split or Continue Modular Monolith.

Bounded goal:

- Decide whether the project should continue as a modular monolith inside `ai-orchestration-service` for the next governance horizon, or prepare for later extraction of one or more domain services.
- Record the decision as architecture policy only.
- Preserve Phase 006 legacy contract freeze unless a later human-approved breaking-change phase explicitly changes routes.
- Avoid implementation, extraction, route migration or new feature work in the steering step.

Why this is the next bounded step:

- Phase 001 split controller surfaces.
- Phase 002 split internal read-model services.
- Phase 003, Phase 004 and Phase 007 reduced known workbench/fallback authority risk.
- Phase 006 froze the legacy non-task `/api/tasks/*` contract inventory.
- The remaining high-priority debt is D001: the multi-domain transition host itself.
- Phase 005 is smaller than actually extracting services because it decides policy and next constraints first.

## Fallback Candidate

Phase 008 - Transition Host Exit Criteria Inventory.

Fallback condition:

- Use this fallback only if the user decides Phase 005 is still too broad to approve now.

Bounded fallback goal:

- Produce a per-domain inventory of current transition-host responsibilities, SoT/read-model status, exit criteria, extraction blockers and contract dependencies.
- Do not choose modular monolith permanence or service extraction yet.
- Do not change code, routes, DTO/VO/entity shape, database schema, Kafka, frontend, Python or config.

Why it is not primary:

- It delays the architecture decision that Phase 006 final and `06-debt-register.md` now identify as the next high-priority issue.
- It improves readiness but does not reduce transition-host policy ambiguity as directly as Phase 005.

## Why Other Phases Are Not Selected

- Phase 001 is completed and frozen.
- Phase 002 is completed and frozen.
- Phase 003 is completed and frozen.
- Phase 004 is completed and frozen.
- Phase 006 is completed and frozen.
- Phase 007 is completed and frozen.
- Domain URL migration is deferred because it is likely a breaking-change or compatibility-path decision and Phase 006 explicitly preserved current URLs.
- Gateway/auth, real data ingest, service discovery, Nacos, Sentinel and deployment topology are farther goals and would skip the D001 transition-host policy decision.
- JSON config storage is real debt, but lower order than the multi-domain transition host decision.
- Generic test expansion should attach to a bounded architecture or contract phase, not replace the D001 decision.
- New feature and new agent work remain ineligible while transition-host policy is unresolved.

## Window 1 Must Define

Window 1 must convert Phase 005 into an implementation-ready architecture handoff and define:

- The exact decision options: continue modular monolith, extract report service later, extract market-event service later, extract risk/strategy services later, or add gateway/auth first.
- For each option, the affected domains, SoT objects, read models, command contracts, transition hosts and known blockers.
- Which contracts from `04-contract-map.md` and Phase 006 must remain stable.
- Whether breaking changes are allowed; default must be no breaking changes.
- Whether URL paths must remain stable; default must be keep all URL paths stable.
- Whether Phase 005 is docs-only architecture policy or permits any code; default must be docs-only/no business code.
- Acceptance criteria for the decision artifact: a clear selected option or explicit no-decision block, rationale by belongs/authority/contract/behavior, required follow-up phases, and out-of-scope implementation work.
- Stop rules if deciding Phase 005 would require service extraction, route migration, DTO/VO/entity changes, database schema changes, Kafka changes, frontend changes, Python changes, config-store migration, gateway/auth implementation or new product features.

## Explicitly Out Of Scope

- No business code change.
- No service extraction.
- No route move, route alias or endpoint rename.
- No breaking change unless separately approved by the user in a later phase.
- No frontend, Python, DTO, VO, entity, mapper, database schema, Kafka, `ai-config`, dependency or deployment change.
- No gateway/auth/Nacos/Sentinel implementation.
- No JSON config migration to DB or Nacos.
- No new agents or product features.
- No reclassification of `ai-orchestration-service` or legacy `/api/tasks/*` paths as final architecture without human approval.

## Human Approval Recorded

User approved this steering decision.

Recorded approval:

- Selected phase: Phase 005 - Decide Service Split or Continue Modular Monolith.
- No breaking changes.
- Keep all URL paths stable.
- No business behavior change.
- No new feature work.
- Expected later Window 2 type: docs-only architecture/policy work unless Window 1 identifies and the user approves a different shape.

Window 0 stops here. Window 1 is the next required window and must not start implementation.
