# Phase Backlog

## Priority Rules

1. Fix main path breakage.
2. Freeze authority and contract.
3. Return logic to formal host.
4. Reduce transition hosts.
5. Add eval/tests to prevent regression.
6. Add new product features.

## Phase 000 - Bootstrap Harness Baseline

Status: completed by current harness files.

Goal:

- Record current architecture.
- Record authority matrix.
- Record host ownership.
- Record contract risks.
- Record transition lifetime.
- Record debt and eval checklist.
- Encode Window 0 as a constrained state machine plus human approval point.
- Provide prompts for Window 0-4.

No business code changes.

## Next Steering Inputs After Phase 002

Window 0 should evaluate the candidate phases below using `10-steering-state-machine.md`.

Phase 001 and Phase 002 are no longer candidates. They were completed and frozen by Window 4.

Recommended candidate inputs for Window 0 evaluation:

- Phase 003 - Contract Hardening for Workbench and Fallback.
- Phase 004 - Python AI Workflow Contract Cleanup.
- Phase 005 - Decide Service Split or Continue Modular Monolith.

Window 4 does not select the next phase. Window 0 must score and propose exactly one primary candidate and one fallback candidate.

## Phase 001 - Split Controller Surface Inside ai-orchestration-service

Status: completed with residual risk.

Completed in:

- `docs/harness/handoffs/phase-001-architect.md`
- `docs/harness/handoffs/phase-001-implementation.md`
- `docs/harness/handoffs/phase-001-review.md`
- `docs/harness/handoffs/phase-001-final.md`

Goal:

Reduce `TaskQueryController` as a multi-domain API surface without changing behavior.

Scope:

- Split controller class by domain inside `ai-orchestration-service`.
- Keep existing URL paths initially unless a breaking change is explicitly approved.
- Candidate controllers:
  - `TaskController` or `TaskQueryController` for task APIs.
  - `MarketEventController`.
  - `RiskWarningController`.
  - `StrategySignalController`.
  - `ReportController`.
  - `AuditComplianceController`.
  - `ModelAgentConfigController`.
  - `ResearchWorkbenchController`.
- Do not change service internals yet.

Acceptance:

- Completed with no intended endpoint behavior change.
- `mvn -q test` passed from `quant-ai-platform/quant-services` during Window 2 and Window 3.
- New controllers map cleanly to the approved Phase 001 endpoint inventory.
- `TaskQueryController` no longer owns market, risk, strategy, report, market-intelligence, audit, config or workbench endpoint methods.

Residual scope:

- Legacy `/api/tasks/*` paths were intentionally preserved and remain contract debt.
- `TaskQueryServiceImpl` internal read-model responsibilities remain mixed.

## Phase 002 - Split TaskQueryServiceImpl Internal Query Services

Status: completed with residual risk.

Completed in:

- `docs/harness/handoffs/phase-002-architect.md`
- `docs/harness/handoffs/phase-002-implementation.md`
- `docs/harness/handoffs/phase-002-review.md`
- `docs/harness/handoffs/phase-002-fix-1-implementation.md`
- `docs/harness/handoffs/phase-002-review-fix-1.md`
- `docs/harness/handoffs/phase-002-final.md`

Goal:

Reduce mixed read-model ownership inside `TaskQueryServiceImpl`.

Scope:

- Extract internal query services in `ai-orchestration-service`.
- Preserve external contracts.
- Suggested services:
  - `TaskReadModelService`
  - `ReportReadModelService`
  - `RiskWarningReadModelService`
  - `StrategySignalReadModelService`
  - `MarketEventReadModelService`
  - `ResearchWorkbenchReadModelService`
  - `AuditComplianceReadModelService`

Acceptance:

- Completed with no intended external behavior change.
- `mvn -q test` passed from `quant-ai-platform/quant-services` during implementation, fix pass and review.
- `TaskQueryService` and `TaskQueryServiceImpl` now expose task read-model and task trace methods only.
- Risk, strategy, report, market-intelligence, audit compliance, model/agent config dashboard and workbench read paths live in internal domain query services.
- Workbench copied domain read-model entrypoints were removed in Fix Pass 1 and are guarded by a boundary test.

Residual scope:

- `ai-orchestration-service` remains a multi-domain transition host.
- Legacy non-task `/api/tasks/*` paths remain contract debt.
- Workbench/fallback display-only contracts still need stronger guardrails.
- Some private display hydration helpers remain in domain/workbench services to preserve behavior without adding forbidden shared helpers.

## Phase 003 - Contract Hardening for Workbench and Fallback

Goal:

Prevent `research-workbench` and fallback merges from becoming SoT.

Scope:

- Add comments/tests around display-only aggregation.
- Add contract tests for workbench output not being used as authoritative task/risk/report/strategy truth.
- Add test coverage for fallback reason propagation if feasible.

Acceptance:

- Tests fail if workbench is used as authority in backend command path.
- Fallback paths remain observable.

## Phase 004 - Python AI Workflow Contract Cleanup

Goal:

Separate AI execution fallback from business truth.

Scope:

- Ensure each model fallback carries fallback reason/audit signal.
- Confirm report/risk/financial fallback is visible in result meta or agent audit.
- Do not add new agents yet.

Acceptance:

- Python tests or compile checks pass.
- Java projection can inspect fallback metadata.

## Phase 005 - Decide Service Split or Continue Modular Monolith

Goal:

After internal boundaries stabilize, decide whether to extract domain services.

Options:

1. Continue as modular monolith inside `ai-orchestration-service`.
2. Extract report-service.
3. Extract market-event-service.
4. Extract risk-service / strategy-service.
5. Add gateway/auth first.

Requires human approval.

## Current Rule

Do not start any implementation directly from this backlog. Window 0 must select the phase candidate, and the user must approve it before Window 1 starts.
