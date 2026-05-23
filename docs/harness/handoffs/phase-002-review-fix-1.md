# Phase 002 Review Fix 1 Handoff

## Status

Phase: Phase 002 - Split `TaskQueryServiceImpl` Internal Query Services.

Window: Window 3 - Review / Eval.

Review mode: Re-review fix 1.

Decision: approve.

Window 4 allowed: yes.

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
- `docs/harness/handoffs/phase-002-review.md`
- `docs/harness/handoffs/phase-002-fix-1-implementation.md`

## Review Order

Reviewed in the required order:

1. belongs
2. authority
3. contract
4. behavior

## Prior Require-Fixes Finding

Closed.

The previous review required fixing P1: `ResearchWorkbenchQueryServiceImpl` kept duplicated risk, strategy, report center and market-intelligence read-model entrypoints. The fix pass removed those workbench-local copied entrypoints and added a boundary test for regression coverage.

Evidence:

- Previous finding and required fix: `docs/harness/handoffs/phase-002-review.md:39`, `docs/harness/handoffs/phase-002-review.md:73`
- Fix pass recorded removal of copied entrypoints: `docs/harness/handoffs/phase-002-fix-1-implementation.md:33`, `docs/harness/handoffs/phase-002-fix-1-implementation.md:35`
- Current workbench surface is the workbench service and `getResearchWorkbench(...)`: `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/ResearchWorkbenchQueryServiceImpl.java:43`, `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/ResearchWorkbenchQueryServiceImpl.java:559`
- Current workbench disposition logic is scoped to display summary helpers: `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/ResearchWorkbenchQueryServiceImpl.java:766`, `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/ResearchWorkbenchQueryServiceImpl.java:879`, `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/ResearchWorkbenchQueryServiceImpl.java:889`, `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/ResearchWorkbenchQueryServiceImpl.java:900`
- Regression test forbids the copied entrypoint names and domain list VO traces in workbench: `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/QueryServiceBoundaryTests.java:35`, `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/QueryServiceBoundaryTests.java:40`

Additional command evidence:

```powershell
rg -n "listRiskWarningRecords|listStrategySignalRecords|listReportCenterRecords|listMarketIntelligenceRecords|RiskWarningListItemVO|StrategySignalListItemVO|ReportCenterListItemVO|MarketIntelligenceListItemVO" quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/ResearchWorkbenchQueryServiceImpl.java
```

Result: no matches, exit code 1.

## Findings

No new findings.

## Positive Checks

- `TaskQueryService` exposes task read-model and task trace methods only: `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/TaskQueryService.java:17`
- The only moved-method match left in `TaskQueryServiceImpl` is the architect-approved read-only `ReportQueryService.getTaskReportOnly(taskId)` call inside full detail: `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/TaskQueryServiceImpl.java:422`
- Domain read-model entrypoints live in their owning query services:
  - Risk: `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/RiskQueryServiceImpl.java:71`
  - Strategy: `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/StrategyQueryServiceImpl.java:79`
  - Report: `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/ReportQueryServiceImpl.java:70`
  - Market intelligence: `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/MarketQueryServiceImpl.java:96`
  - Audit compliance: `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/AuditComplianceQueryServiceImpl.java:63`
  - Model/agent config dashboard: `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/ModelAgentConfigDashboardQueryServiceImpl.java:66`
  - Workbench display aggregation: `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/ResearchWorkbenchQueryServiceImpl.java:559`
- `ResearchWorkbenchController` preserves the legacy `/api/tasks/research-workbench` path and `Result.success(...)` envelope while calling the workbench query service: `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/ResearchWorkbenchController.java:13`, `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/ResearchWorkbenchController.java:19`, `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/ResearchWorkbenchController.java:21`
- Boundary test still prevents non-task domain implementations from importing or injecting `TaskQueryService`: `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/QueryServiceBoundaryTests.java:14`
- Manual grep found no database writes in the query service implementations under review.

## Verification

Run from `D:\projects\bussiness\quant-ai-platform\quant-services`:

```powershell
mvn -q test
```

Result: passed with exit code 0. The output includes the existing `kafka down` stack trace from a negative-path publisher test, but Maven completed successfully.

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

Result: only the allowed full-detail report read call matched.

```text
TaskQueryServiceImpl.java:422 vo.setReport(reportQueryService.getTaskReportOnly(taskId));
```

## Window 1 Acceptance

Window 1 acceptance is satisfied.

Satisfied:

- `TaskQueryServiceImpl` contains task read-model and task trace public methods only.
- `TaskQueryService` no longer exposes risk, strategy, report, market-intelligence, audit, config or workbench methods.
- Non-task domain query implementations do not delegate moved read-model methods back to `TaskQueryService`.
- Existing inspected controller mappings, permissions, Sentinel annotations and `Result.success(...)` response envelopes remain stable.
- Existing VO/DTO/entity/mapper shapes were not changed by the fix pass.
- No database writes were introduced by query services.
- `research-workbench` remains display-only aggregation and is not consumed by backend commands as authority.
- The prior workbench duplicated-read-model finding is closed.
- `mvn -q test` passes.

## Window 4 Gate

Window 4 may proceed.
