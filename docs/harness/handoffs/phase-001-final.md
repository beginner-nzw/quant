# Phase 001 Final Handoff

## Status

Phase status: completed with residual risk.

Window 4 froze Phase 001 after Window 3 approved the implementation.

Residual risk:

- Phase 001 added reflection mapping coverage and passed the Maven suite, but did not add a full Spring web-context boot test.
- `.gitignore` has a harness tracking exception noted by Window 3. This is workspace metadata, not application authority, contract or behavior drift.

## Inputs Read

- `docs/harness/05-transition-lifetime.md`
- `docs/harness/06-debt-register.md`
- `docs/harness/07-phase-backlog.md`
- `docs/harness/09-window-protocol.md`
- `docs/harness/10-steering-state-machine.md`
- `docs/harness/state/current-state.md`
- `docs/harness/handoffs/steering-decision-phase-001.md`
- `docs/harness/handoffs/phase-001-architect.md`
- `docs/harness/handoffs/phase-001-implementation.md`
- `docs/harness/handoffs/phase-001-review.md`

## Completed Scope

Phase 001 split the former multi-domain `TaskQueryController` surface into domain-specific controllers inside `ai-orchestration-service`.

Completed controller ownership:

- `TaskQueryController`: task read-model, runtime trace, retry and cancel endpoints.
- `MarketEventController`: market event read, transition command and source preview endpoints.
- `RiskWarningController`: risk warning read-model endpoints.
- `StrategySignalController`: strategy signal read-model and transition command endpoints.
- `ReportController`: report read-model, report center, review and version endpoints.
- `MarketIntelligenceController`: market intelligence aggregation endpoints.
- `AuditComplianceController`: audit compliance read-model/dashboard endpoints.
- `ModelAgentConfigController`: model, agent, workflow, event source and role config endpoints.
- `ResearchWorkbenchController`: research workbench display aggregation endpoint.

Completed verification from Window 2 and Window 3:

- `mvn -q test` passed from `quant-ai-platform/quant-services`.
- Controller mapping scan matched the approved Phase 001 inventory.
- `TaskControllerMappingTest` locks endpoint-to-controller ownership.
- `git diff --check` reported no whitespace errors, with only existing CRLF normalization warnings.

## Contract / Authority / Transition Changes

Contract changes:

- Controller ownership is now domain-specific.
- Existing URL paths, HTTP methods, request bindings, permission checks, Sentinel annotations and response envelopes remained unchanged.
- Legacy non-task `/api/tasks/*` paths remain as approved contract debt.

Authority changes:

- No source of truth changed.
- Task creation remains in `research-task-service`.
- `research-workbench` remains display-only aggregation.
- Frontend remains a contract consumer.
- Python fallback truth and audit metadata were out of scope.

Transition changes:

- T1 exit criterion 1 is now complete: `TaskQueryController` has been split into domain-specific controllers inside the same service.
- `TaskQueryController` is no longer listed as an active multi-domain transition host.
- `ai-orchestration-service` remains a multi-domain transition host.
- `TaskQueryServiceImpl` remains the main internal read-model transition host.

## Unchanged Contracts

- `/api/tasks` task read-model and task control contracts.
- `/api/tasks/*` market, risk, strategy, report, market-intelligence, audit, config and workbench paths.
- `Result.success(...)` response envelope shape.
- Existing permission checks and role constants.
- Existing Sentinel resources for `pageTasks` and `getTaskFullDetail`.
- Existing service, mapper, entity, DTO, VO, frontend, Python, config, Maven, Docker and deployment contracts.

## Remaining Debt

- D001 remains open but partially mitigated by Phase 001; `ai-orchestration-service` still hosts multiple domains and `TaskQueryServiceImpl` remains mixed.
- D002 remains open because non-task domains still use legacy `/api/tasks/*` paths.
- D003 remains open because `research-workbench` still needs stronger display-only hardening.
- D004 remains open because `TaskQueryServiceImpl` still mixes task/report/risk/strategy/market/workbench query responsibilities.
- D005-D012 remain open unless a later phase explicitly resolves them.

## Recommended Candidate Inputs For Window 0

Candidate input set only; Window 4 does not select the next phase.

- Phase 002 - Split `TaskQueryServiceImpl` Internal Query Services.
- Phase 003 - Contract Hardening for Workbench and Fallback.
- Phase 004 - Python AI Workflow Contract Cleanup.

Window 0 must use `docs/harness/10-steering-state-machine.md` to score candidates, propose exactly one primary candidate and one fallback candidate, then ask for human approval.

## Files Changed In This Handoff

- `docs/harness/state/current-state.md`
- `docs/harness/05-transition-lifetime.md`
- `docs/harness/06-debt-register.md`
- `docs/harness/07-phase-backlog.md`
- `docs/harness/handoffs/phase-001-final.md`

No business code was changed by Window 4.

## Window 0 Next Startup Reads

Window 0 should read:

- `docs/harness/state/current-state.md`
- `docs/harness/10-steering-state-machine.md`
- `docs/harness/07-phase-backlog.md`
- `docs/harness/06-debt-register.md`
- `docs/harness/05-transition-lifetime.md`
- `docs/harness/02-authority-matrix.md`
- `docs/harness/03-host-ownership.md`
- `docs/harness/04-contract-map.md`
- `docs/harness/08-eval-checklist.md`
- `docs/harness/handoffs/phase-001-final.md`
- `docs/harness/handoffs/phase-001-review.md`
- `docs/harness/handoffs/phase-001-implementation.md`
- `docs/harness/handoffs/phase-001-architect.md`
- `docs/harness/handoffs/steering-decision-phase-001.md`

Window 0 may inspect code only to verify whether a handoff claim is still true. It must not implement.
