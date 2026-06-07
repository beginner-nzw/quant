# Phase 014 Implementation Handoff

## Status

Window: Window 2 - Implementer.

Phase: Phase 014 - Workbench Recomposition And Legacy Route Cutover.

Mode: implementation.

## Goal

Recompose the research workbench as an aggregation consumer and perform the approved legacy route cutover:

- workbench consumes stable task/report/risk/strategy/market contracts only;
- workbench remains display/navigation/prefill only and does not write domain facts, trigger business commands or pass domain truth;
- approved report route cutover uses `/api/reports/*` as the frontend stable report client while preserving legacy `/api/tasks/*` report routes as compatibility aliases;
- frontend API clients and guards are updated;
- workbench authority guard, route contract checks and frontend build verification are recorded.

## Files Changed

- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/ReportController.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java`
- `quant-ui/package.json`
- `quant-ui/scripts/authority-boundary-check.mjs`
- `quant-ui/scripts/report-contract-check.mjs`
- `quant-ui/src/api/task.ts`
- `quant-ui/src/views/report/ResearchWorkbenchView.vue`
- `docs/harness/handoffs/phase-014-implementation.md`

## Implementation Notes

- `src/api/task.ts` report exports now delegate to the stable `src/api/report.ts` client backed by `/api/reports/*`.
- `src/utils/reportWorkbench.ts` report list loading now uses `fetchReportCenter(...)` from the stable report client instead of `fetchTasks(...)` task pagination.
- Legacy backend report routes under `/api/tasks/*` remain present and are marked as deprecated compatibility aliases through `ReportController`.
- `LegacyTaskApiContractFreezeTest` now allows the approved `/api/reports` namespace while continuing to reject unapproved risk, strategy, market, audit, config and workbench namespace aliases.
- `ResearchWorkbenchView.vue` no longer passes `sourceReviewStatus` from aggregation output into task-create prefill.
- `reportWorkbench.ts` no longer passes report review status into task-create prefill from report workbench rows.
- `authority-boundary-check.mjs` now rejects direct frontend legacy report path calls and rejects workbench-derived `sourceReviewStatus` prefill.
- `report-contract-check.mjs` now asserts stable `/api/reports/*` client usage and compatibility wrapper delegation.
- `package.json` exposes `check:authority-boundary` and `check:report-contract` scripts.

## Authority And Contract Boundaries

- Workbench remains a display aggregation consumer.
- Workbench may navigate to task/report/risk/strategy/market centers and may prefill task creation with source ids and target display context.
- Workbench must not pass derived report review status, risk level, signal direction, signal strength, confidence score or fallback/provenance truth into commands.
- Report workbench lists must use stable report center data and may not use task pagination as report truth.
- Report frontend consumers use the stable report API client.
- Legacy report routes are retained for compatibility; this implementation does not delete or remap task, risk, strategy, market, audit, config or workbench endpoints.

## Verification

Run from `D:\projects\bussiness\quant-ui`:

- `npm run check:authority-boundary` passed.
- `npm run check:report-contract` passed.
- `npm run build` passed.

Require-fixes closure:

- Fixed `reportWorkbench.ts` to load pending/approved/rejected report workbench lists from stable `/api/reports/center` via `fetchReportCenter(...)`.
- Removed `sourceReviewStatus` from report workbench task-create prefill.
- Added guard coverage so `reportWorkbench.ts` cannot regress to `fetchTasks(...)` or `sourceReviewStatus` prefill.

Run from `D:\projects\bussiness\quant-ai-platform\quant-services`:

- `mvn -pl quant-business/ai-orchestration-service -am "-Dtest=ReportDomainContractTests,LegacyTaskApiContractFreezeTest,TaskControllerMappingTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` passed.

Notes:

- Running the targeted Maven tests directly from the `ai-orchestration-service` submodule failed because local `quant-common-*` SNAPSHOT dependencies were not installed. The parent reactor command above was used so Maven could build required modules.
- The first parent reactor attempt without `-Dsurefire.failIfNoSpecifiedTests=false` failed in common modules because the target test names exist only in `ai-orchestration-service`.

## Residual Risk

- Risk, strategy and market contracts still use the frozen `/api/tasks/*` stable paths unless a later phase approves their own namespace cutover.
- Existing dirty/untracked work outside the files listed above was treated as pre-existing workspace state and was not reverted.
