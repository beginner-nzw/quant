# Phase 007 Implementation Handoff

## Status

Window: Window 2 - Frontend Implementer.

Phase: Phase 007 - Frontend Consumer Authority Boundary Audit.

Mode: initial implementation. `docs/harness/handoffs/phase-007-implementation.md` did not exist at startup.

## Inputs Read

- `docs/harness/00-project-charter.md`
- `docs/harness/02-authority-matrix.md`
- `docs/harness/03-host-ownership.md`
- `docs/harness/04-contract-map.md`
- `docs/harness/05-transition-lifetime.md`
- `docs/harness/08-eval-checklist.md`
- `docs/harness/09-window-protocol.md`
- `docs/harness/state/current-state.md`
- `docs/harness/handoffs/steering-decision-phase-007.md`
- `docs/harness/handoffs/phase-007-architect.md`

Startup discovery selected Phase 007 because `phase-007-architect.md` exists and `phase-007-final.md` does not. No Phase 007 implementation or review handoff existed.

## Files Changed By This Window 2

- `quant-ui/src/api/task.ts`
- `quant-ui/src/types/task.ts`
- `quant-ui/src/utils/taskActionAccess.ts`
- `quant-ui/src/views/report/ResearchWorkbenchView.vue`
- `quant-ui/src/views/task/TaskDetailView.vue`
- `quant-ui/src/views/task/TaskReportView.vue`
- `quant-ui/src/components/task/TaskReportCard.vue`
- `quant-ui/scripts/authority-boundary-check.mjs`
- `docs/harness/handoffs/phase-007-implementation.md`

`quant-ui/src/views/task/TaskDetailView.vue` was used only for an allowed secondary boundary note at the exact existing `reportMeta.reportId` source-context prefill consumer.

Pre-existing unrelated dirty/untracked harness files were present before implementation and were not edited by this Window 2 pass.

## Implementation Summary

- Added source-level authority notes that `ResearchWorkbenchData` is display-only aggregation and may support rendering, navigation and task-create prefill context only.
- Added source-level authority notes that `TaskReportContextSnapshot` is display/audit provenance only and must not become task, report, risk, signal, market, audit or model truth.
- Added boundary notes at workbench and report/detail prefill consumers to make the existing usage explicit as display/navigation/source-context prefill, not command authority.
- Added `quant-ui/scripts/authority-boundary-check.mjs`, a focused static guard that:
  - Fails if workbench consumer files reference command API names such as retry, cancel, report review, strategy, market or config update commands.
  - Fails if fallback/provenance tokens appear in command helper or route helper files.
  - Fails if report review command handlers consume `contextSnapshot`, fallback provenance or `reportMeta`.
  - Allows only the existing `reportMeta.reportId` source-context prefill in task-create navigation paths.
  - Verifies the workbench API path and the two source-level authority notes remain present.

## Architect Acceptance Completed

- `ResearchWorkbenchData` is documented as display-only aggregation.
- `TaskReportContextSnapshot` provenance fields are documented as audit/display metadata only.
- Workbench output remains used only for display, navigation and existing task-create prefill context.
- Workbench output is statically guarded from retry, cancel, report review, strategy signal, market event and config command APIs.
- Fallback/provenance fields are statically guarded from report review commands and command/route helpers.
- Existing report/detail task-create prefill still carries only source IDs and is documented as source-context prefill.
- Existing routes, URL paths, response envelopes, API function names and call signatures were not changed.
- Existing TypeScript DTO-like field names and optionality were not changed.
- No backend, Python, database, Kafka, `ai-config`, dependency or build config files were changed.

## Contracts Preserved

- `GET /api/tasks/research-workbench` remains unchanged in `fetchResearchWorkbench`.
- No `quant-ui/src/api/task.ts` endpoint strings, HTTP methods, function names or signatures were changed.
- No frontend route definitions were changed.
- No TypeScript field names, optionality or response/request shapes were changed.
- No Java, Python, database, Kafka, config or package contract changed.

## Behavior Changes

No user-visible business behavior change was made.

The production source changes are comments/JSDoc only. The new guard script is not imported by production code and does not alter runtime behavior.

## Verification Results

Run from `D:\projects\bussiness\quant-ui`:

```powershell
node scripts/authority-boundary-check.mjs
```

Result: passed with `authority-boundary-check passed`.

Run from `D:\projects\bussiness\quant-ui`:

```powershell
npm run build
```

Result: passed. `vue-tsc --build` and `vite build` completed successfully.

Run from `D:\projects\bussiness`:

```powershell
rg -n "fetchResearchWorkbench|ResearchWorkbenchData|latestInsight|riskDispositionSummary|strategySignalDispositionSummary|marketIntelligenceDispositionSummary" quant-ui/src
```

Result: matches remain in API typing, `ResearchWorkbenchView.vue`, `ResearchWorkbenchStatsCards.vue`, `taskActionAccess.ts`, type declarations and display-only evidence/report labels. No command execution utility uses workbench output.

Run from `D:\projects\bussiness`:

```powershell
rg -n "contextSnapshot|reportMeta|generationMode|fallbackReason|reportFallbackReason|planningFallbackReason|intentFallbackReason|marketDataSource" quant-ui/src/views quant-ui/src/components quant-ui/src/utils quant-ui/src/api
```

Result: matches remain in report display components, report page display/export helpers, type-facing display usage, and documented `reportMeta.reportId` source-context prefill. The static guard verifies these fields do not feed report review commands or command/route helpers beyond the allowed source ID prefill.

Run from `D:\projects\bussiness`:

```powershell
rg -n "retryTask|cancelTask|reviewTaskReport|createStrategySignal|updateStrategySignalStatus|createMarketEvent|batchImportMarketEvents|mockIngestMarketEvents|syncMarketEventSource|updatePromptTemplate|updateModelStrategy|updateEventAutoTriggerRule|updateEventSourceConfig|updateAgentConfig|updateWorkflowConfig|updateRoleAccessConfig" quant-ui/src/views/report/ResearchWorkbenchView.vue quant-ui/src/utils/researchWorkbench.ts quant-ui/src/utils/taskActionAccess.ts
```

Result: no matches. PowerShell command returned no output, which is the expected result for this check.

Run from `D:\projects\bussiness`:

```powershell
git diff --name-only
```

Result: showed the approved frontend source files plus pre-existing `docs/harness/state/current-state.md`. It does not show untracked files; `quant-ui/scripts/authority-boundary-check.mjs` and this implementation handoff are visible through `git status --short`.

## Blockers And Residual Risk

No blocker was encountered.

Residual risk: the static guard is a focused source check, not a full TypeScript AST policy engine. It intentionally guards the Phase 007 boundary surfaces and exact command paths identified by the architect handoff without expanding scope or changing runtime behavior.
