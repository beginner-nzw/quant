# Phase 003 Implementation Handoff

## Status

Phase: Phase 003 - Contract Hardening for Workbench and Fallback.

Window: Window 2 - Backend Implementer.

Mode: Initial implementation.

Result: Implementation complete; ready for Window 3 review.

## Inputs Read

- `docs/harness/00-project-charter.md`
- `docs/harness/02-authority-matrix.md`
- `docs/harness/03-host-ownership.md`
- `docs/harness/04-contract-map.md`
- `docs/harness/05-transition-lifetime.md`
- `docs/harness/08-eval-checklist.md`
- `docs/harness/09-window-protocol.md`
- `docs/harness/state/current-state.md`
- `docs/harness/handoffs/steering-decision-phase-003.md`
- `docs/harness/handoffs/phase-003-architect.md`

No Phase 003 implementation, review, or fix handoff existed at startup.

## Files Changed By This Window

Production comment-only changes:

- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/ResearchWorkbenchQueryService.java`
  - Added a display-only aggregation contract note.
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/ResearchWorkbenchQueryServiceImpl.java`
  - Added a contract boundary note near the class fields.
  - Added a display-hydration-only note near existing preferred/fallback selection.

Backend test changes:

- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/QueryServiceBoundaryTests.java`
  - Added a source boundary test that fails if workbench references move outside the controller, workbench query service, workbench DTO/VO, or tests.
  - Added a source boundary test that fails if `ResearchWorkbenchQueryServiceImpl` starts writing domain facts, writing Redis, or publishing events.

Handoff:

- `docs/harness/handoffs/phase-003-implementation.md`

No executable production logic was changed.

## Architect Acceptance Completed

- `research-workbench` remains a display-only aggregation.
- Backend command and projection paths are guarded from depending on `ResearchWorkbenchQueryService`, `ResearchWorkbenchVO`, `ResearchWorkbenchQueryDTO`, or `getResearchWorkbench(...)` by `workbenchContractReferencesStayInsideDisplaySurface`.
- `AiResultDomainProjectionServiceImpl` remains outside the workbench dependency surface.
- `ResearchWorkbenchQueryServiceImpl` is guarded against database writes, Redis writes, and event publishes by `researchWorkbenchAggregationDoesNotWriteDomainFactsOrPublishEvents`.
- Phase 002 removed read-model entrypoints remain guarded by the existing `researchWorkbenchDoesNotKeepDomainReadModelEntryPoints` test.
- Existing preferred/fallback selection is documented as display hydration only.
- No new fallback source, precedence rule, response field, message field, helper, adapter, bridge, facade, production fallback service, or endpoint was added.

## Contracts Kept Stable

Unchanged:

- `GET /api/tasks/research-workbench`
- HTTP method and request binding through `ResearchWorkbenchQueryDTO`
- `Result.success(...)` envelope
- `ResearchWorkbenchVO` response shape
- Authoritative/read-model endpoint paths listed in the architect handoff
- Command endpoint paths listed in the architect handoff
- DTO, VO, entity, mapper, database schema, Kafka contract, frontend, and Python surfaces
- Redis cache keys and TTLs
- Report/risk/strategy/market/audit projection behavior

## Behavior Change

No business behavior change.

This pass added comments and source-level regression tests only. Runtime query behavior, fallback precedence, null/empty handling, sorting, pagination limits, permissions, response envelopes, writes, and event publishing behavior were not changed.

## Verification Results

Required Maven verification:

- Command: `mvn -q test`
- Working directory: `D:\projects\bussiness\quant-ai-platform\quant-services`
- Result: passed, exit code 0.
- Note: test output includes an expected WARN/stack trace from `TaskOutboxPublisherServiceTests.publishPendingOnceShouldMarkFailedWhenKafkaSendFails`, which simulates `kafka down`; Maven still passed.

Required boundary inspections:

- Command: `rg -n "ResearchWorkbench|research-workbench|getResearchWorkbench" quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator`
- Result: matches were confined to `ResearchWorkbenchController`, `ResearchWorkbenchQueryService`, `ResearchWorkbenchQueryServiceImpl`, `ResearchWorkbenchQueryDTO`, and `ResearchWorkbench*VO` classes. No command service or projection service match was present.

- Command: `rg -n "\.(insert|update|updateById|delete|deleteById)\(" quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/ResearchWorkbenchQueryServiceImpl.java`
- Result: no matches; `rg` exit code 1 for no matches.

- Command: `rg -n "listRiskWarningRecords|listStrategySignalRecords|listReportCenterRecords|listMarketIntelligenceRecords|RiskWarningListItemVO|StrategySignalListItemVO|ReportCenterListItemVO|MarketIntelligenceListItemVO" quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/ResearchWorkbenchQueryServiceImpl.java`
- Result: no matches; `rg` exit code 1 for no matches.

Scope inspection:

- Command: `git diff --name-only`
- Result: showed pre-existing tracked dirty files from before this Phase 003 pass, including `.gitignore`, `TaskQueryController.java`, several query service files, and existing task query tests.
- Important note: the workspace was already dirty at startup. This Window 2 did not revert or claim those pre-existing changes. Phase 003 changes made by this window are limited to the allowed files listed above and this handoff.

## Blockers And Residual Risk

Blockers: none.

Residual risk:

- The repository worktree is already dirty with prior uncommitted and untracked phase artifacts. Window 3 should review Phase 003 changes against the allowed files listed in this handoff rather than treating all dirty files as new work from this pass.
- The new guardrails are source-level contract tests. They intentionally do not alter runtime behavior or introduce runtime contract marker objects.
