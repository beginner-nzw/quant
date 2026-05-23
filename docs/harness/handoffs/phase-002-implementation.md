# Phase 002 Implementation Handoff

## Status

Phase 002 implementation completed by Window 2 - Backend Implementer.

No next phase was selected.

## Files Changed By This Window

Backend service interfaces:

- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/TaskQueryService.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/AuditConfigDashboardQueryService.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/AuditComplianceQueryService.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/ModelAgentConfigDashboardQueryService.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/ResearchWorkbenchQueryService.java`

Backend query implementations:

- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/TaskQueryServiceImpl.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/ReportQueryServiceImpl.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/RiskQueryServiceImpl.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/StrategyQueryServiceImpl.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/MarketQueryServiceImpl.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/AuditConfigDashboardQueryServiceImpl.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/AuditComplianceQueryServiceImpl.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/ModelAgentConfigDashboardQueryServiceImpl.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/ResearchWorkbenchQueryServiceImpl.java`

Allowed controller dependency rewiring:

- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/AuditComplianceController.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/ModelAgentConfigController.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/ResearchWorkbenchController.java`

Tests:

- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/TaskQueryServiceRiskProjectionTests.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/TaskQueryServiceRiskWarningTests.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/TaskQueryServiceStateTests.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/QueryServiceBoundaryTests.java`

Handoff:

- `docs/harness/handoffs/phase-002-implementation.md`

Pre-existing dirty or untracked files outside this Window's edits were left in place and not reverted.

## Architect Acceptance Completed

- `TaskQueryService` now exposes only task read-model and task trace methods used by `TaskQueryController`.
- `TaskQueryServiceImpl` no longer exposes risk, strategy, report, market-intelligence, audit, config or workbench public methods.
- Risk warning read-model methods now live in `RiskQueryServiceImpl`.
- Strategy signal read-model methods now live in `StrategyQueryServiceImpl`.
- Report center, task report, report review stats and report version read paths now live in `ReportQueryServiceImpl`.
- Market intelligence read-model methods now live in `MarketQueryServiceImpl`.
- Audit compliance dashboard read methods now live in `AuditComplianceQueryServiceImpl`.
- Model/agent config dashboard read method now lives in `ModelAgentConfigDashboardQueryServiceImpl`.
- Research workbench display aggregation now lives in `ResearchWorkbenchQueryServiceImpl`.
- `AuditConfigDashboardQueryService` was narrowed to role access config and market event source config list reads that existing controllers still consume.
- Non-task domain query implementations no longer import or inject `TaskQueryService`.
- `TaskQueryServiceImpl.getTaskFullDetail` calls `ReportQueryService.getTaskReportOnly` read-only, matching the allowed dependency direction.
- Existing fallback/preferred display hydration logic was moved as-is with the owning query implementation.
- Existing tests were redirected to the new owning query services, and `QueryServiceBoundaryTests` was added to guard against reintroducing `TaskQueryService` dependency in domain query implementations.

## Contracts Preserved

- All `/api/tasks/*` URL paths remain unchanged.
- HTTP methods remain unchanged.
- Controller request bindings remain unchanged.
- Controller permission checks remain unchanged.
- `Result.success(...)` response envelopes remain unchanged.
- VO/DTO/entity/mapper shapes were not changed.
- Existing Sentinel annotations on task list and full detail remain unchanged.
- Existing Redis cache keys and TTL intent for task state, task list, task stats, task full detail and task report remain unchanged.
- `research-workbench` remains display-only aggregation and is not consumed by backend commands as authority.

## Behavior Changes

No intended business behavior change.

Observed behavior change is internal ownership only: domain read-model methods are now served by their domain query service implementations instead of delegating through `TaskQueryServiceImpl`.

## Verification Results

Run from `D:\projects\bussiness\quant-ai-platform\quant-services`:

```powershell
mvn -q test
```

Result: passed with exit code 0.

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

## Blockers Or Residual Risks

No blocker encountered.

Residual risk: some display hydration helpers are duplicated privately across domain query implementations to preserve behavior and avoid adding a forbidden shared helper/adapter. This is intentional for Phase 002 and should be reviewed as part of future contract hardening, not expanded into a generic helper without a new approved phase.
