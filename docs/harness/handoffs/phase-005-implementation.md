# Phase 005 Implementation Handoff

## Status

Window: Window 2 - Implementer.

Phase: Phase 005 - Decide Service Split or Continue Modular Monolith.

Mode: initial implementation.

Implementation shape: docs-only architecture/policy decision.

This phase did not change business code, tests, scripts, runtime configuration, frontend code, Python code, Java code, database files, Kafka files, dependencies or deployment files.

## Git Baseline

Before edits, `git status --short --untracked-files=all` showed pre-existing dirty or untracked files:

- `M docs/harness/state/current-state.md`
- `?? docs/harness/handoffs/phase-003-review.md`
- `?? docs/harness/handoffs/phase-004-architect.md`
- `?? docs/harness/handoffs/phase-004-review.md`
- `?? docs/harness/handoffs/phase-005-architect.md`
- `?? docs/harness/handoffs/phase-006-architect.md`
- `?? docs/harness/handoffs/phase-006-fix-1-implementation.md`
- `?? docs/harness/handoffs/phase-006-implementation.md`
- `?? docs/harness/handoffs/phase-006-review-fix-1.md`
- `?? docs/harness/handoffs/phase-006-review-fix-2.md`
- `?? docs/harness/handoffs/phase-006-review-fix-3.md`
- `?? docs/harness/handoffs/phase-006-review.md`
- `?? docs/harness/handoffs/phase-007-architect.md`
- `?? docs/harness/handoffs/phase-007-review.md`
- `?? docs/harness/handoffs/steering-decision-phase-004.md`
- `?? docs/harness/handoffs/steering-decision-phase-005.md`
- `?? docs/harness/handoffs/steering-decision-phase-006.md`
- `?? docs/harness/handoffs/steering-decision-phase-007.md`

These were treated as existing unrelated working tree state. This Window 2 pass only claims the new Phase 005 implementation handoff.

## Selected Option

Selected option: Option 1 - continue as a modular monolith inside `ai-orchestration-service` for the next governance horizon.

This is not a permanent final architecture decision. It is a bounded next-governance-horizon policy that keeps the current transition host in place while requiring later phase gates before any service extraction, route migration, auth/gateway work, config-store migration, data-ingest split or feature expansion.

Decision order:

```text
belongs -> authority -> contract -> behavior
```

## Rationale Summary

The safe Phase 005 decision is to keep `ai-orchestration-service` as the current transition host because the available evidence supports policy stabilization, not extraction.

- Belongs: current runtime ownership is known and already registered. Moving a domain host now would exceed the phase.
- Authority: report, risk, strategy, market, audit and config authoritative objects remain in the current transition host or current files; none has an approved new SoT host.
- Contract: Phase 006 froze legacy non-task `/api/tasks/*` paths as transitional contracts. Extraction now would require route, ownership and compatibility decisions that are not approved.
- Behavior: this docs-only decision preserves all runtime behavior and avoids pretending a service split has already happened.

## Rejected Options

Option 2 - prepare later `report-service` extraction:

Rejected for this phase. Report facts, versions, sections, evidence, review and report-center query surfaces are still coupled to current transition-host read models and legacy `/api/tasks/*` contracts. A later report extraction readiness phase can inventory contracts and prerequisites, but Phase 005 does not approve extraction.

Option 3 - prepare later `market-event-service` extraction:

Rejected for this phase. Market event management, mock ingest, market intelligence display and standardized event consumption still need data-ingest ownership and contract gates. Extracting or preparing extraction now would risk blurring mock/demo source boundaries and the Phase 006 legacy path freeze.

Option 4 - prepare later `risk-service` / `strategy-service` extraction:

Rejected for this phase. Risk and strategy facts are projected from AI results in the current transition host, and downstream topics remain placeholders. A later extraction readiness phase must first define projection ownership, command surfaces and event contracts.

Option 5 - add gateway/auth first:

Rejected for this phase. Gateway/auth is real transition debt, but Phase 005 evidence does not show it is the primary prerequisite for the D001 service-boundary policy. Header-based demo auth remains a transition mechanism until a later approved auth/gateway phase.

Option 6 - block/no-decision:

Rejected for this phase. There is enough documented evidence to select the conservative modular-monolith horizon without moving code, changing contracts or making a permanent final-architecture claim.

## Belongs Analysis

| Domain or surface | Current host for this horizon | Phase 005 policy |
| --- | --- | --- |
| Task creation and dispatch outbox | `research-task-service` | Stays formal host. No change to `POST /api/research/tasks`. |
| AI execution | `quant-ai-engine` | Stays formal execution host. No change to fallback/provenance behavior. |
| AI status/result/audit consumption | `ai-orchestration-service` | Stays current formal consumer/projection host. |
| Task runtime read model and retry/cancel | `ai-orchestration-service` | Stays current host. |
| Report read/review/version/evidence surfaces | `ai-orchestration-service` transition host | Stays transition-hosted until a later report boundary phase. |
| Risk warning surfaces | `ai-orchestration-service` transition host | Stays transition-hosted until a later risk boundary phase. |
| Strategy signal surfaces | `ai-orchestration-service` transition host | Stays transition-hosted until a later strategy boundary phase. |
| Market event and market intelligence surfaces | `ai-orchestration-service` transition host | Stays transition-hosted until data-ingest and market-event ownership are decided. |
| Audit/config dashboard surfaces | `ai-orchestration-service` transition host and JSON config files | Stays current transition shape. |
| Research workbench | `ai-orchestration-service` aggregation | Stays display-only aggregation, not a domain host. |
| Frontend display and navigation | `quant-ui` | Stays contract consumer only. |

Belongs conclusion:

`ai-orchestration-service` remains a transition host for the next governance horizon. It is not reclassified as final architecture. No domain is moved to a new runtime service in Phase 005.

## Authority Analysis

Authority stays exactly where the harness currently records it:

| Semantic | Authoritative object or source | Phase 005 authority result |
| --- | --- | --- |
| Task creation fact | `research_task` | No change. |
| Task runtime state | `research_task.status/current_stage`, `ai_workflow_instance`, `ai_agent_execution` | No change. |
| AI dispatch fact | `task_outbox_message` and Kafka `ai.task.dispatch` | No change. |
| AI execution trace | `ai_agent_execution`, `audit_record`, `ai_prompt_audit`, Kafka `ai.task.audit` | No change. |
| Report fact | `research_report`, `research_report_version`, `research_report_section` | No change. |
| Report evidence | `report_evidence_ref`, `research_report_section` | No change. |
| Risk warning fact | `risk_warning`, `risk_warning_detail` | No change. |
| Strategy signal fact | `strategy_signal`, `strategy_signal_factor` | No change. |
| Market event fact | `market_event`, `market_event_relation`, `market_event_analysis` | No change. |
| Config fact | `ai-config/*.json` plus audited config APIs | No change. |
| Permission fact | current JSON role config plus request headers | No production-security reinterpretation. |
| Audit fact | `audit_record`, `task_message_log`, `ai_prompt_audit`, config change audit files | No change. |
| Workbench aggregation | none; aggregation only | Remains non-authoritative display. |
| Python fallback provenance | existing metadata surfaces | Remains provenance only, not model-generated truth. |

Authority risks that remain open:

- D001 remains open because the transition host still contains multiple domains.
- D002 remains open as namespace debt, though Phase 006 freezes the legacy `/api/tasks/*` inventory.
- D003 remains open for future surfaces; Phase 003, Phase 004 and Phase 007 guard only current known surfaces.

Any future SoT move requires a later human-approved phase.

## Contract Stability Analysis

Stable backend API contracts:

- `POST /api/research/tasks`
- all existing task read/control endpoints under `/api/tasks`
- all Phase 006 frozen non-task legacy `/api/tasks/*` endpoints
- report, risk, strategy, market, market-intelligence, audit, config and workbench contracts documented in `04-contract-map.md`

Stable frontend contracts:

- all existing `quant-ui` routes
- all existing API endpoint strings, HTTP methods, function names, call signatures, response envelopes and TypeScript field shapes
- Phase 007 frontend authority guardrails for workbench and fallback provenance consumers

Stable Kafka contracts:

- `ai.task.dispatch`
- `ai.task.status`
- `ai.task.result`
- `ai.task.audit`
- `market.event.standardized`
- downstream placeholder topics already listed in `04-contract-map.md`

No URL path, HTTP method, endpoint owner, request binding, response envelope, response type, permission behavior, frontend route, Kafka topic, database schema or runtime configuration changed.

Legacy non-task `/api/tasks/*` paths remain frozen transitional contracts. They are not declared final architecture.

## Behavior And Verification Risk

Behavior result:

- No user-visible business behavior changed.
- No runtime behavior changed.
- No service extraction was performed.
- No route migration, route alias, compatibility bridge, gateway proxy or service wrapper was added.
- No database schema, Kafka payload, frontend, Python or Java behavior changed.

Verification risk:

- This was docs-only work, so Maven, npm and Python verification were not required by the Phase 005 architect handoff.
- The meaningful checks are documentation-scope checks: diff scope and required decision content.

## Required Follow-up Phases

These are follow-up candidates only. They are not selected or approved by this Window 2 implementation.

1. Transition host exit criteria inventory.

   Goal: define per-domain exit gates for report, market, risk, strategy, audit, config and workbench responsibilities inside `ai-orchestration-service`.

2. Report boundary readiness phase.

   Goal: inventory report facts, review commands, version contracts, evidence references and report-center dependencies before any report-service extraction is considered.

3. Market event and data-ingest ownership phase.

   Goal: separate mock/demo ingest rules from real source sync ownership before any market-event-service extraction is considered.

4. Risk/strategy projection ownership phase.

   Goal: define whether projected risk and strategy facts remain local read models or become independently owned service facts.

5. Auth/gateway decision phase.

   Goal: decide gateway/auth ownership and replace or constrain header-based demo auth before production-grade service boundaries are attempted.

6. Legacy route migration decision phase.

   Goal: decide whether any domain URL migration or breaking change is approved. Until then, Phase 006 legacy `/api/tasks/*` contracts remain stable.

7. Config store decision phase.

   Goal: decide whether JSON runtime config remains file-backed or moves to DB/Nacos in a later approved phase.

## Out Of Scope

The following remain explicitly out of scope for Phase 005:

- business code change
- service extraction
- extract report-service, market-event-service, risk-service or strategy-service
- route migration, route alias, endpoint rename, endpoint move or endpoint deletion
- breaking change
- gateway/auth implementation
- Nacos implementation or config-store migration
- Sentinel implementation
- database schema or migration change
- Kafka topic, payload, producer or consumer change
- frontend route, command, display or type-shape change
- Python workflow, fallback or provenance change
- Java DTO, VO, entity, mapper, service, controller or test change
- dependency, build-config, Docker, deployment or service-discovery change
- new helper, adapter, fallback, bridge, facade, resolver, router, mapper, proxy or wrapper
- new product feature or new agent work

## Architect Acceptance Completed

- Selected exactly one allowed policy outcome: Option 1, continue modular monolith for the next governance horizon.
- Justified the selection in `belongs -> authority -> contract -> behavior` order.
- Preserved current URLs, HTTP methods, request/response contracts, frontend routes, Kafka topics, database schema and runtime behavior.
- Preserved Phase 006 legacy `/api/tasks/*` contract freeze.
- Preserved Phase 007 frontend authority-boundary guardrails.
- Did not reclassify `ai-orchestration-service` as final architecture.
- Did not reclassify legacy non-task `/api/tasks/*` paths as final architecture.
- Identified follow-up phases required before extraction, route migration, gateway/auth work, config-store migration, data-ingest split or product feature work.
- Changed no business code, tests, scripts, dependencies, configs, database files, frontend files or Python files.

## Files Changed By This Window 2 Pass

- `docs/harness/handoffs/phase-005-implementation.md`

No separate `phase-005-service-boundary-decision.md` was created because the durable decision is fully recorded in this implementation handoff.

No optional edits were made to `docs/harness/03-host-ownership.md`, `docs/harness/04-contract-map.md` or `docs/harness/05-transition-lifetime.md`; Window 4 can update durable state after review approval.

## Contracts Kept Unchanged

- URL paths: unchanged.
- HTTP methods: unchanged.
- Request bindings: unchanged.
- Response envelopes and generic response types: unchanged.
- Permission behavior: unchanged.
- Frontend routes and API call signatures: unchanged.
- Kafka topics and payloads: unchanged.
- Database schema and migrations: unchanged.
- JSON config files: unchanged.
- Python fallback/provenance behavior: unchanged.

## Behavior Changes

None.

## Verification Results

Architect-required commands were run from `D:\projects\bussiness`.

```powershell
git diff --name-only
```

Result: command completed. Output listed only the pre-existing unrelated tracked modification:

```text
docs/harness/state/current-state.md
```

This Window 2 pass did not modify `docs/harness/state/current-state.md`. The new implementation handoff is untracked until staged, so it is visible through `git status --short --untracked-files=all` rather than `git diff --name-only`.

```powershell
rg -n "selected option|Selected option|block/no-decision|belongs|authority|contract|behavior|/api/tasks|Phase 006|Phase 007|follow-up|out of scope" docs/harness/handoffs/phase-005-implementation.md
```

Result: passed. Matches confirmed the handoff contains the selected option, decision-order analysis, `/api/tasks` and Phase 006/Phase 007 stability references, follow-up sections and out-of-scope sections.

```powershell
rg -n "route migration|service extraction|extract .*service|breaking change|gateway/auth|Nacos|Sentinel|database schema|Kafka|frontend|Python" docs/harness/handoffs/phase-005-implementation.md
```

Result: passed. Matches appear in preserved-contract, behavior-risk, follow-up, out-of-scope or acceptance sections. They do not claim any forbidden implementation work was completed.

Additional status check:

```powershell
git status --short --untracked-files=all
```

Result: command completed. The only new file added by this Window 2 pass is:

```text
?? docs/harness/handoffs/phase-005-implementation.md
```

The other dirty and untracked files were present at the start of this Window 2 pass and remain excluded from the implementation claim.

## Blockers And Residual Risks

Blockers encountered:

- None.

Residual risks:

- D001 remains open; the modular-monolith decision is time-bounded and does not close transition-host debt.
- D002 remains open; legacy non-task `/api/tasks/*` namespace debt is frozen by Phase 006 but not migrated.
- D003 remains open for future surfaces; current known workbench and fallback consumers have guardrails, but future surfaces need equivalent boundaries.
- Extraction readiness remains unproven until later phases inventory per-domain contracts, ownership, authority and migration gates.

## Need For Review

Window 3 must review this Phase 005 implementation before Window 4. This implementation does not authorize Window 4 by itself.
