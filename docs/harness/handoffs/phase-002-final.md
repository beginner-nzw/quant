# Phase 002 Final Handoff

## Status

Phase status: completed with residual risk.

Window 4 froze Phase 002 after the latest Window 3 review approved Fix Pass 1.

Latest approving review:

- `docs/harness/handoffs/phase-002-review-fix-1.md`

Window 0 should start from `docs/harness/state/current-state.md` and this final handoff. The user does not need to manually summarize Phase 002.

## Completed Scope

Phase 002 split mixed read-model ownership out of `TaskQueryServiceImpl` while keeping external behavior stable.

Completed scope:

- `TaskQueryService` now exposes task read-model and task trace methods only.
- `TaskQueryServiceImpl` keeps task read-model/task trace behavior and the approved read-only `ReportQueryService.getTaskReportOnly(...)` call inside full detail.
- Risk warning read paths moved to `RiskQueryServiceImpl`.
- Strategy signal read paths moved to `StrategyQueryServiceImpl`.
- Report center, report review stats, report versions and task report read paths moved to `ReportQueryServiceImpl`.
- Market intelligence read paths moved to `MarketQueryServiceImpl`.
- Audit compliance dashboard read paths moved to `AuditComplianceQueryServiceImpl`.
- Model/agent config dashboard read path moved to `ModelAgentConfigDashboardQueryServiceImpl`.
- Research workbench display aggregation moved to `ResearchWorkbenchQueryServiceImpl`.
- Fix Pass 1 removed copied risk, strategy, report center and market-intelligence read-model entrypoints from workbench.
- Boundary tests now guard against non-task query implementations importing/injecting `TaskQueryService` and against workbench reintroducing copied domain read-model entrypoints.

Verification recorded by Window 2 and Window 3:

- `mvn -q test` passed from `D:\projects\bussiness\quant-ai-platform\quant-services`.
- Boundary grep showed only `TaskQueryServiceImpl` imports/implements `TaskQueryService`.
- Moved-method grep showed only the approved full-detail report read call in `TaskQueryServiceImpl`.

## Contract / Authority / Transition Changes

Contract state:

- No URL path, HTTP method, request binding, permission, response envelope, VO/DTO/entity/mapper shape, database schema, Kafka contract, frontend contract or Python contract was intentionally changed.
- All legacy non-task `/api/tasks/*` paths remain stable and remain contract debt.

Authority state:

- `TaskQueryServiceImpl` is no longer the mixed owner for risk, strategy, report, market-intelligence, audit, config dashboard or workbench read paths.
- Domain read-model ownership now sits with internal domain query services inside `ai-orchestration-service`.
- `research-workbench` remains display-only aggregation and must not be promoted to source of truth.

Transition state:

- `ai-orchestration-service` remains a multi-domain transition host.
- T1 exit criterion 2 is completed: `TaskQueryServiceImpl` has been split into internal domain query services.
- Internal domain query services are now active transition structure, not final microservice architecture.

## Unchanged Contracts

- Existing `/api/tasks/*` URL paths remain unchanged.
- Existing HTTP methods remain unchanged.
- Existing controller request bindings remain unchanged.
- Existing controller permission checks remain unchanged.
- Existing `Result.success(...)` response envelopes remain unchanged.
- Existing Sentinel annotations on task list and task full detail remain unchanged.
- Existing Redis cache key and TTL intent for task/report read paths remains unchanged.
- Existing VO/DTO/entity/mapper shapes remain unchanged.
- No frontend, Python, database schema, Kafka, Docker, gateway, auth, Nacos or Sentinel dashboard files were part of this phase.

## Remaining Debt

- D001 remains open but partially mitigated: `ai-orchestration-service` still hosts multiple domains.
- D002 remains open: non-task domain APIs still use legacy `/api/tasks/*` paths by approved stable-contract constraint.
- D003 remains open but partially mitigated: workbench is narrowed to display aggregation, but display-only/fallback authority needs stronger contract guardrails.
- D004 is closed by Phase 002: `TaskQueryServiceImpl` no longer owns the mixed non-task read paths.
- Python fallback metadata remains a later contract concern.
- Some private display hydration helpers remain local to owning query services to preserve behavior without introducing a forbidden shared helper/adapter.

## Latest State For Window 0

Current state after this handoff:

- Current phase: none approved.
- Current phase status: no active phase; Phase 002 is completed with residual risk.
- Last completed phase: Phase 002 - Split `TaskQueryServiceImpl` Internal Query Services.
- Open blockers: none registered.
- Human approval status: no next phase is approved.

Window 0 startup recovery should:

1. Read `docs/harness/state/current-state.md`.
2. Discover latest final handoff as `docs/harness/handoffs/phase-002-final.md`.
3. Read the matching Phase 002 steering, architect, implementation, review, fix implementation, review-fix and final handoffs.
4. Score candidate next phases using `docs/harness/10-steering-state-machine.md`.
5. Propose exactly one primary candidate and one fallback candidate.
6. Wait for human approval before Window 1 starts.

## Recommended Candidate Inputs For Window 0

Window 4 does not select the next phase.

Recommended candidate inputs:

- Phase 003 - Contract Hardening for Workbench and Fallback.
- Phase 004 - Python AI Workflow Contract Cleanup.
- Phase 005 - Decide Service Split or Continue Modular Monolith.

Window 0 should prefer higher-order authority and contract risks over new feature work unless the user explicitly overrides.

## Files Changed In This Handoff

- `docs/harness/state/current-state.md`
- `docs/harness/06-debt-register.md`
- `docs/harness/07-phase-backlog.md`
- `docs/harness/05-transition-lifetime.md`
- `docs/harness/handoffs/phase-002-final.md`
