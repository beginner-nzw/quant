# Phase 002 Review Handoff

## Status

Phase: Phase 002 - Split `TaskQueryServiceImpl` Internal Query Services.

Window: Window 3 - Review / Eval.

Decision: require fixes.

Window 4 allowed: no.

## Inputs Read

- `docs/harness/00-project-charter.md`
- `docs/harness/02-authority-matrix.md`
- `docs/harness/03-host-ownership.md`
- `docs/harness/04-contract-map.md`
- `docs/harness/05-transition-lifetime.md`
- `docs/harness/06-debt-register.md`
- `docs/harness/08-eval-checklist.md`
- `docs/harness/09-window-protocol.md`
- `docs/harness/state/current-state.md`
- `docs/harness/handoffs/steering-decision-phase-002.md`
- `docs/harness/handoffs/phase-002-architect.md`
- `docs/harness/handoffs/phase-002-implementation.md`

## Review Order

Reviewed in the required order:

1. belongs
2. authority
3. contract
4. behavior

## Findings

### P1 - `ResearchWorkbenchQueryServiceImpl` keeps duplicated domain read-model entrypoints

Result: require fixes.

The phase goal was to move domain read-model ownership out of `TaskQueryServiceImpl` into the owning internal query services, while keeping workbench as display-only aggregation. The implementation does remove the public methods from `TaskQueryServiceImpl`, but `ResearchWorkbenchQueryServiceImpl` now contains private copies of domain list/read-model entrypoints for risk, strategy, report center and market intelligence.

Evidence:

- Architect explicitly mapped these domain methods to their owning services, not to workbench:
  - `pageRiskWarnings` -> `RiskQueryServiceImpl`: `docs/harness/handoffs/phase-002-architect.md:195`
  - `pageStrategySignals` -> `StrategyQueryServiceImpl`: `docs/harness/handoffs/phase-002-architect.md:197`
  - `pageReportCenter` -> `ReportQueryServiceImpl`: `docs/harness/handoffs/phase-002-architect.md:199`
  - `pageMarketIntelligence` -> `MarketQueryServiceImpl`: `docs/harness/handoffs/phase-002-architect.md:203`
- Architect separately limited the workbench cluster to display aggregation helpers such as `resolveRecentTaskLimit`, `toResearchWorkbenchInsight`, `populateResearchWorkbenchDispositionSummaries` and follow-up grouping: `docs/harness/handoffs/phase-002-architect.md:223`
- Contract map says `/api/tasks/research-workbench` is aggregation and must not define task/report/risk/strategy truth: `docs/harness/04-contract-map.md:30`
- Duplicated private domain list/read-model entrypoints are present in workbench:
  - `listRiskWarningRecords(...)`: `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/ResearchWorkbenchQueryServiceImpl.java:94`
  - `listStrategySignalRecords(...)`: `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/ResearchWorkbenchQueryServiceImpl.java:730`
  - `listReportCenterRecords(...)`: `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/ResearchWorkbenchQueryServiceImpl.java:1368`
  - `listMarketIntelligenceRecords(...)`: `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/ResearchWorkbenchQueryServiceImpl.java:1553`
- The owning service implementations also contain these same read-model entrypoints:
  - `RiskQueryServiceImpl.java:101`
  - `StrategyQueryServiceImpl.java:109`
  - `ReportQueryServiceImpl.java:99`
  - `MarketQueryServiceImpl.java:126`
- `getResearchWorkbench(...)` starts at `ResearchWorkbenchQueryServiceImpl.java:1887` and builds the workbench directly; the copied `list*Records(...)` entrypoints above are not called by that public method.

Why this matters:

- belongs: risk, strategy, report center and market-intelligence read-model entrypoints belong in their owning query services, not in the workbench aggregation service.
- authority: even as private unused code, the duplicate read-model algorithms create a second place where domain truth semantics can drift.
- contract: this does not appear to change external paths or response envelopes today, but it weakens the intended workbench restriction before Phase 003 contract hardening.
- behavior: tests pass, but the governance acceptance is not fully met.

Required fix:

Remove the unused copied domain list/read-model entrypoints from `ResearchWorkbenchQueryServiceImpl`, or narrow them into workbench-specific display helpers that are actually used by `getResearchWorkbench(...)`. Keep any necessary duplicated utility logic local and display-only, but do not keep full risk/strategy/report/market list read-model copies in the workbench host.

## Positive Checks

- `TaskQueryService` now exposes only task read-model and task trace methods.
- `TaskQueryServiceImpl` public methods are task read-model / task trace methods only.
- Boundary check found no non-task domain query implementation importing or injecting `TaskQueryService`.
- The only match for moved public method names in `TaskQueryServiceImpl` is the architect-approved `ReportQueryService.getTaskReportOnly(taskId)` call inside `getTaskFullDetail`.
- Controller mappings, permissions and Sentinel annotations inspected for the affected surfaces did not show a Phase 002 contract drift.
- No new database writes were found in query services beyond existing Redis cache writes in task/report read paths.

## Verification

Run from `D:\projects\bussiness\quant-ai-platform\quant-services`:

```powershell
mvn -q test
```

Result: passed with exit code 0.

Run from `D:\projects\bussiness`:

```powershell
rg -n "TaskQueryService" quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl
```

Result: only `TaskQueryServiceImpl` matched.

Run from `D:\projects\bussiness`:

```powershell
rg -n "pageRiskWarnings|getRiskWarningStats|pageStrategySignals|getStrategySignalStats|pageReportCenter|getReportCenterStats|pageMarketIntelligence|getMarketIntelligenceStats|pageAuditCompliance|getAuditComplianceStats|getModelAgentConfigCenter|getResearchWorkbench|getTaskReportOnly|getReportReviewStats" quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/TaskQueryService.java quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/TaskQueryServiceImpl.java
```

Result: only `TaskQueryServiceImpl.java:422` matched for the allowed `ReportQueryService.getTaskReportOnly(taskId)` call.

## Window 1 Acceptance

Window 1 acceptance is not fully satisfied.

Satisfied:

- `TaskQueryServiceImpl` contains task read-model and trace public methods only.
- `TaskQueryService` no longer exposes moved non-task methods.
- Non-task domain query implementations do not delegate back to `TaskQueryService`.
- Existing URL mappings, permission checks and Sentinel annotations inspected remain stable.
- `mvn -q test` passes.

Not satisfied:

- Workbench still contains duplicated risk/strategy/report/market read-model entrypoint logic, which conflicts with the required destination and workbench display-only boundary.

## Window 4 Gate

Do not enter Window 4 yet.

Window 2 should fix the P1 belongs/authority finding first, then Window 3 should re-review the focused diff and rerun the required checks.
