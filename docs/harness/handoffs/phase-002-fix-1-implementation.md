# Phase 002 Fix 1 Implementation Handoff

## Status

Phase 002 fix pass completed by Window 2 - Backend Implementer.

Mode: Fix Pass.

Latest review handoff: `docs/harness/handoffs/phase-002-review.md`.

No next phase was selected.

## Corresponding Review Finding

P1 - `ResearchWorkbenchQueryServiceImpl` keeps duplicated domain read-model entrypoints.

Review conclusion: require fixes.

Required fix from review:

- Remove unused copied risk, strategy, report center and market-intelligence list/read-model entrypoints from `ResearchWorkbenchQueryServiceImpl`, or narrow them into workbench-specific display helpers actually used by `getResearchWorkbench(...)`.
- Keep necessary duplicated utility logic local and display-only.
- Do not keep full risk/strategy/report/market list read-model copies in the workbench host.

## Files Changed By This Fix Pass

- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/ResearchWorkbenchQueryServiceImpl.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/QueryServiceBoundaryTests.java`
- `docs/harness/handoffs/phase-002-fix-1-implementation.md`

No other files were intentionally changed by this fix pass.

## Fix Implemented

- Removed unused workbench-local domain list/read-model entrypoints:
  - `listRiskWarningRecords`
  - `listStrategySignalRecords`
  - `listReportCenterRecords`
  - `listMarketIntelligenceRecords`
- Removed the copied domain list VO mapping paths from workbench:
  - `RiskWarningListItemVO`
  - `StrategySignalListItemVO`
  - `ReportCenterListItemVO`
  - `MarketIntelligenceListItemVO`
- Narrowed workbench disposition summary calculation to display-only follow-up status collection.
- Added workbench-specific display predicates:
  - `hasReportRiskDisposition`
  - `hasReportStrategySignalDisposition`
  - `hasMarketIntelligenceDisposition`
- Preserved the previous report fallback inclusion conditions for disposition summaries without constructing domain list VOs.
- Retained `StrategySignalFactorMapper` constructor compatibility because existing tests instantiate the implementation directly with the phase-002 constructor shape.
- Added a focused boundary test to prevent `ResearchWorkbenchQueryServiceImpl` from reintroducing copied domain read-model entrypoints or domain list VO mapper traces.

## Why This Does Not Expand Scope

The fix only addresses the latest Window 3 finding.

It does not add endpoints, DTO/VO fields, helpers, adapters, bridges, fallbacks, database writes, Kafka paths, frontend code or Python code.

The new test is a focused boundary assertion for the exact review finding and lives under the architect-approved test scope.

## Architect Acceptance Completed By This Fix

- Workbench no longer keeps duplicated risk, strategy, report center or market-intelligence list/read-model entrypoints.
- Workbench remains a display-only aggregation surface.
- Domain read-model ownership remains with `RiskQueryServiceImpl`, `StrategyQueryServiceImpl`, `ReportQueryServiceImpl` and `MarketQueryServiceImpl`.
- Existing controller mappings, URLs, permissions and response envelopes were not changed.
- Existing fallback/preferred display hydration used by workbench insight and disposition summaries remains display-only.

## Contracts Preserved

- All `/api/tasks/*` URL paths remain unchanged.
- HTTP methods remain unchanged.
- Request bindings remain unchanged.
- Permissions remain unchanged.
- `Result.success(...)` envelopes remain unchanged.
- VO/DTO/entity/mapper shapes remain unchanged.
- No cache key or TTL behavior was changed.
- `research-workbench` remains display aggregation and is not used as backend command authority.

## Behavior Changes

No intended external behavior change.

Internal behavior change: workbench disposition summaries no longer construct domain list VO objects to read `followUpStatus`; they compute follow-up status directly from the same workbench follow-up task summary inputs and keep the same report inclusion predicates.

## Verification Results

Run from `D:\projects\bussiness`:

```powershell
rg -n "listRiskWarningRecords|listStrategySignalRecords|listReportCenterRecords|listMarketIntelligenceRecords|RiskWarningListItemVO|StrategySignalListItemVO|ReportCenterListItemVO|MarketIntelligenceListItemVO" quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/ResearchWorkbenchQueryServiceImpl.java
```

Result: no matches, exit code 1.

Run from `D:\projects\bussiness`:

```powershell
rg -n "TaskQueryService" quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl
```

Result: only `TaskQueryServiceImpl` itself matched.

```text
TaskQueryServiceImpl.java:14 import com.quant.aiorchestrator.service.TaskQueryService;
TaskQueryServiceImpl.java:38 public class TaskQueryServiceImpl implements TaskQueryService
```

Run from `D:\projects\bussiness`:

```powershell
rg -n "pageRiskWarnings|getRiskWarningStats|pageStrategySignals|getStrategySignalStats|pageReportCenter|getReportCenterStats|pageMarketIntelligence|getMarketIntelligenceStats|pageAuditCompliance|getAuditComplianceStats|getModelAgentConfigCenter|getResearchWorkbench|getTaskReportOnly|getReportReviewStats" quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/TaskQueryService.java quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/TaskQueryServiceImpl.java
```

Result: only the allowed `ReportQueryService.getTaskReportOnly` read-only call inside `TaskQueryServiceImpl.getTaskFullDetail` matched.

```text
TaskQueryServiceImpl.java:422 vo.setReport(reportQueryService.getTaskReportOnly(taskId));
```

Run from `D:\projects\bussiness\quant-ai-platform\quant-services`:

```powershell
mvn -q test
```

First run result: failed during test compilation because `ResearchWorkbenchQueryServiceImpl` constructor compatibility changed after removing the unused factor mapper field.

Follow-up action: restored the constructor-compatible `StrategySignalFactorMapper` field without using it for workbench read-model logic.

Final run result: passed with exit code 0.

## Blockers Or Residual Risks

No blocker encountered.

Residual risk: `ResearchWorkbenchQueryServiceImpl` still keeps private display hydration helpers required by the workbench insight and disposition summary surface. They are scoped to `getResearchWorkbench(...)` behavior and should remain display-only unless a later approved phase changes the contract.

## Re-Review Needed

Yes. Window 3 should re-review this focused fix pass against `docs/harness/handoffs/phase-002-review.md`.
