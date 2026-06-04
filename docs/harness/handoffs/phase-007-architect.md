# Phase 007 Architect Handoff

## Status

Window: Window 1 - Phase Architect.

Phase: Phase 007 - Frontend Consumer Authority Boundary Audit.

This handoff is architecture planning only. It does not authorize implementation. Window 2 may start only after the user explicitly approves this file.

## Inputs Read

Required harness files:

- `docs/harness/00-project-charter.md`
- `docs/harness/01-current-architecture.md`
- `docs/harness/02-authority-matrix.md`
- `docs/harness/03-host-ownership.md`
- `docs/harness/04-contract-map.md`
- `docs/harness/05-transition-lifetime.md`
- `docs/harness/06-debt-register.md`
- `docs/harness/07-phase-backlog.md`
- `docs/harness/08-eval-checklist.md`
- `docs/harness/09-window-protocol.md`
- `docs/harness/10-steering-state-machine.md`
- `docs/harness/state/current-state.md`
- `docs/harness/handoffs/steering-decision-phase-007.md`
- `docs/harness/handoffs/phase-004-final.md`

Frontend code inspected for Phase 007 boundaries:

- `quant-ui/package.json`
- `quant-ui/src/api/task.ts`
- `quant-ui/src/router/index.ts`
- `quant-ui/src/types/task.ts`
- `quant-ui/src/utils/researchWorkbench.ts`
- `quant-ui/src/utils/reportWorkbench.ts`
- `quant-ui/src/utils/task.ts`
- `quant-ui/src/utils/taskActionAccess.ts`
- `quant-ui/src/utils/taskActions.ts`
- `quant-ui/src/utils/taskCreate.ts`
- `quant-ui/src/utils/taskNavigation.ts`
- `quant-ui/src/utils/auth.ts`
- `quant-ui/src/utils/roleAccess.ts`
- `quant-ui/src/views/task/TaskCreateView.vue`
- `quant-ui/src/views/task/TaskDetailView.vue`
- `quant-ui/src/views/task/TaskReportView.vue`
- `quant-ui/src/views/report/ResearchWorkbenchView.vue`
- `quant-ui/src/views/report/ResearchReportCenterView.vue`
- `quant-ui/src/views/report/RiskWarningCenterView.vue`
- `quant-ui/src/views/report/StrategySignalCenterView.vue`
- `quant-ui/src/views/report/MarketEventCenterView.vue`
- `quant-ui/src/views/report/MarketIntelligenceCenterView.vue`
- `quant-ui/src/views/report/AuditComplianceCenterView.vue`
- `quant-ui/src/views/report/ModelAgentConfigCenterView.vue`
- `quant-ui/src/components/task/TaskReportCard.vue`
- `quant-ui/src/components/report/ReportEvidenceView.vue`

Phase 007 is not Phase 001, so the Phase 001 Java controller reading list is not the primary scope for this handoff.

## 1. Phase Goal

Audit and guard frontend consumers so they do not treat display aggregation, fallback provenance or mixed-domain read responses as business source of truth.

The bounded goal is:

- Document or guard that `GET /api/tasks/research-workbench` is display-only aggregation.
- Document or guard that `reportMeta`, `contextSnapshot`, `generationMode`, `fallbackReason`, `reportFallbackReason`, planner/intent fallback fields and market fallback provenance are audit/display metadata only.
- Keep frontend command decisions dependent on explicit backend command contracts and user actions, not on frontend-derived truth from workbench, fallback metadata or display hydration.
- Preserve all existing URLs, routes, response envelopes, TypeScript API shapes and user-visible business behavior.

This is a frontend-focused authority-boundary phase. It is not a feature phase, not a route cleanup phase, not a backend contract phase and not a Python fallback phase.

Window 2 must evaluate every edit in this order:

```text
belongs -> authority -> contract -> behavior
```

## 2. Belongs

Frontend belongs to `quant-ui` as a consumer.

Allowed frontend responsibilities:

- Render backend read models.
- Render backend projection output.
- Render Python fallback provenance when it is already present inside existing metadata surfaces.
- Hold UI state, loading, empty and error states.
- Navigate between routes.
- Pre-fill task creation forms from existing row/detail/source context as demo workflow context.
- Submit commands only through existing backend command APIs.

Frontend does not own:

- task status truth
- report truth
- risk warning truth
- strategy signal truth
- market event truth
- audit truth
- config truth
- fallback/model-vs-rule truth
- role/security truth beyond the existing demo header/localStorage behavior

Workbench-specific belongs rule:

- `ResearchWorkbenchView.vue` may display `ResearchWorkbenchData` and use it for navigation or task-create prefill context.
- It must not become a frontend authority resolver for task/report/risk/strategy/market/audit facts.

Fallback-specific belongs rule:

- `TaskReportView.vue` and `TaskReportCard.vue` may display provenance fields from `TaskReport.contextSnapshot`.
- They must not use fallback provenance to approve/reject reports, create risk/strategy facts, change task status, decide review status or overwrite backend projections.

## 3. Authority

The following frontend data classes are non-authoritative display consumers:

- `ResearchWorkbenchData`
- `ResearchWorkbenchInsight`
- `ResearchWorkbenchRecentTask`
- `ResearchWorkbenchDispositionSummary`
- `TaskReportContextSnapshot`
- `TaskReportMeta`
- report display hydration fields such as `displaySummary`, `displayHighlights`, `displayRiskPoints`
- fallback/provenance fields such as `generationMode`, `fallbackReason`, `planningFallbackReason`, `intentFallbackReason`, `reportFallbackReason`, `marketDataSource`

The following frontend surfaces consume authoritative backend read models, but still do not create truth locally:

- `TaskListView.vue` and `TaskDetailView.vue` consume task read-model fields.
- `TaskReportView.vue` and `TaskReportCard.vue` consume report read-model fields.
- `ResearchReportCenterView.vue` consumes report-center read-model fields.
- `RiskWarningCenterView.vue` consumes risk-warning read-model fields.
- `StrategySignalCenterView.vue` consumes strategy-signal read-model fields and can submit existing strategy-signal commands.
- `MarketEventCenterView.vue` consumes market-event read-model fields and can submit existing market-event commands.
- `MarketIntelligenceCenterView.vue` consumes market-intelligence display/read-model fields.
- `AuditComplianceCenterView.vue` consumes audit-compliance read-model fields.
- `ModelAgentConfigCenterView.vue` consumes transition config APIs, but frontend defaults must not become config truth.

Command authority rule:

- Frontend buttons may decide whether to show or enable an existing command using role permissions and the authoritative row/detail/report status returned by the matching backend endpoint.
- Frontend must not derive command eligibility from `ResearchWorkbenchData`, fallback provenance or `contextSnapshot`, except for the existing "has enough context to navigate/prefill" behavior.

## 4. Contract

Stable frontend route contracts:

- `/dashboard`
- `/tasks`
- `/tasks/:taskId`
- `/tasks/create`
- `/tasks/:taskId/report`
- `/market-events`
- `/intelligence`
- `/research-workbench`
- `/signals`
- `/risk-warnings`
- `/reports/center`
- `/audit-compliance`
- `/model-agent-config`
- `/reports/pending`
- `/reports/approved`
- `/reports/rejected`

Stable API contracts in `quant-ui/src/api/task.ts`:

- all existing URL paths
- all existing HTTP methods
- all existing function names and call signatures
- all existing `ApiResult<T>` response envelope expectations
- all existing TypeScript response/request types

Stable command contracts:

- `POST /api/research/tasks`
- `POST /api/tasks/{taskId}/retry`
- `POST /api/tasks/{taskId}/cancel`
- `POST /api/tasks/{taskId}/report/review`
- `POST /api/tasks/strategy-signals`
- `POST /api/tasks/strategy-signals/{signalId}/status`
- `POST /api/tasks/market-events`
- `POST /api/tasks/market-events/batch-import/preview`
- `POST /api/tasks/market-events/batch-import`
- `POST /api/tasks/market-events/mock-ingest`
- `POST /api/tasks/market-events/source-sync/{sourceCode}`
- `POST /api/tasks/model-agent-config/*`

Stable read/display contracts:

- `GET /api/tasks/research-workbench` remains display-only aggregation.
- `GET /api/tasks/{taskId}/full` and `GET /api/tasks/{taskId}/report` remain the frontend report/detail source for report pages.
- risk, strategy, market, report-center, market-intelligence and audit pages continue consuming their existing API functions.
- `TaskReportContextSnapshot` remains optional metadata; adding required fields or depending on optional provenance for behavior is forbidden.

## 5. Allowed File Scope

Window 2 may modify these frontend files for comments, JSDoc-style contract notes, type-only documentation, or narrowly scoped static guard integration. Runtime behavior must remain unchanged.

Primary allowed production files:

- `quant-ui/src/api/task.ts`
- `quant-ui/src/types/task.ts`
- `quant-ui/src/utils/researchWorkbench.ts`
- `quant-ui/src/utils/taskActionAccess.ts`
- `quant-ui/src/utils/taskActions.ts`
- `quant-ui/src/utils/taskCreate.ts`
- `quant-ui/src/utils/taskNavigation.ts`
- `quant-ui/src/views/report/ResearchWorkbenchView.vue`
- `quant-ui/src/views/task/TaskReportView.vue`
- `quant-ui/src/components/task/TaskReportCard.vue`

Secondary allowed production files, only if a boundary note is needed at the exact consumer:

- `quant-ui/src/views/task/TaskCreateView.vue`
- `quant-ui/src/views/task/TaskDetailView.vue`
- `quant-ui/src/views/report/ResearchReportCenterView.vue`
- `quant-ui/src/views/report/RiskWarningCenterView.vue`
- `quant-ui/src/views/report/StrategySignalCenterView.vue`
- `quant-ui/src/views/report/MarketEventCenterView.vue`
- `quant-ui/src/views/report/MarketIntelligenceCenterView.vue`
- `quant-ui/src/views/report/AuditComplianceCenterView.vue`
- `quant-ui/src/views/report/ModelAgentConfigCenterView.vue`
- `quant-ui/src/utils/reportWorkbench.ts`
- `quant-ui/src/router/index.ts`, comments only if route-level stability needs to be documented

Allowed frontend static guard files:

- new files under `quant-ui/scripts/`, for example `quant-ui/scripts/authority-boundary-check.mjs`

Allowed handoff file:

- `docs/harness/handoffs/phase-007-implementation.md`

## 6. Forbidden File Scope

Window 2 must not modify:

- any Java production or test files under `quant-ai-platform/quant-services/**`
- any Python files under `quant-ai-platform/quant-ai-engine/**`
- `quant-ai-platform/ai-config/**`
- database migration, schema or SQL files
- Kafka topic, message, DTO, VO, entity or mapper definitions
- Maven POM files
- Docker, deployment, gateway, auth-service, Nacos, Sentinel or service discovery files
- `quant-ui/package.json`, `package-lock.json`, `vite.config.ts`, `tsconfig*.json`
- `quant-ui/src/utils/request.ts` or `quant-ui/src/utils/requestHeaders.ts`
- `quant-ui/src/utils/auth.ts` or `quant-ui/src/utils/roleAccess.ts`, except read-only inspection
- visual styling files or unrelated components
- prior phase handoffs or core harness policy files
- `docs/harness/state/current-state.md`

If Window 2 finds a backend, Python, config, route or package-script change is necessary, it must stop as a blocker.

## 7. Allowed New Class / Method Types

Allowed in static guard scripts:

- small local functions such as `readText`, `listFiles`, `assertNoMatch`, `assertAllowedFilesOnly`
- local allowlists for expected frontend source files
- regex or parser-based assertions that inspect frontend source references

Allowed in frontend production files:

- comments or JSDoc-style contract notes
- type-only documentation on existing interfaces
- narrow type aliases only if they do not add runtime output, change API shape or force call-site behavior changes

Not allowed in frontend production files:

- new runtime classes
- new exported services
- new API functions
- new command functions
- new route guards
- new source-of-truth resolver functions
- new data normalization functions that change values
- new required fields or changed optionality in TypeScript interfaces
- new dependencies

## 8. Forbidden Helpers / Adapters / Fallbacks / Bridges

Do not add:

- `*Helper`, `*Adapter`, `*Bridge`, `*Facade`, `*Fallback`, `*Resolver` or `*Mapper` production objects for authority or truth routing.
- A frontend "truth resolver" for task, report, risk, strategy, market, audit, config or fallback semantics.
- A frontend API alias for any backend endpoint.
- A frontend bridge that reads workbench output and feeds retry, cancel, review, projection, config or signal/market commands.
- A fallback metadata adapter that turns `generationMode`, `fallbackReason` or `contextSnapshot` into business status.
- A shared display-hydration utility that changes existing field precedence.
- A new frontend cache that persists workbench, fallback or mixed-domain response data as truth.
- Any route alias, compatibility route or rewritten URL.

Static guard scripts may have local helper functions, but those helpers must not be imported by production code.

## 9. Acceptance Conditions

Phase 007 is acceptable only if all conditions hold:

- `ResearchWorkbenchData` is documented or statically guarded as display-only aggregation.
- `TaskReportContextSnapshot` fallback/provenance fields are documented or statically guarded as audit/display metadata only.
- Workbench output is not used to call retry, cancel, report review, strategy-signal update/create, market-event command, config update or backend projection-like behavior.
- Workbench output may only support display, route navigation and existing task-create prefill context.
- `contextSnapshot`, `reportMeta`, `generationMode`, `fallbackReason` and related provenance fields are not used by frontend command builders as business truth.
- Frontend display hydration remains display-only and does not create new precedence rules.
- Existing frontend routes remain unchanged.
- Existing `quant-ui/src/api/task.ts` endpoint paths, HTTP methods and function signatures remain unchanged.
- Existing TypeScript DTO-like interfaces keep the same field names and optionality.
- Existing user-visible business behavior remains unchanged.
- No backend, Python, database, Kafka, `ai-config`, package/dependency or router behavior change occurs.
- At least one focused static guard or documented source-level boundary would fail or become visibly stale if future frontend code promotes workbench or fallback metadata into command truth.
- `npm run build` passes from `quant-ui`.

## 10. Required Verification Commands

Window 2 must run from `D:\projects\bussiness\quant-ui`:

```powershell
npm run build
```

If Window 2 adds `quant-ui/scripts/authority-boundary-check.mjs`, it must also run from `D:\projects\bussiness\quant-ui`:

```powershell
node scripts/authority-boundary-check.mjs
```

Window 2 must run or record these source checks from `D:\projects\bussiness`:

```powershell
rg -n "fetchResearchWorkbench|ResearchWorkbenchData|latestInsight|riskDispositionSummary|strategySignalDispositionSummary|marketIntelligenceDispositionSummary" quant-ui/src
```

Expected result: workbench references remain confined to API typing, `ResearchWorkbenchView.vue`, display/navigation query builders, static guard code and explicit display-only action access. They must not appear in frontend command execution utilities except as non-command display/prefill context.

```powershell
rg -n "contextSnapshot|reportMeta|generationMode|fallbackReason|reportFallbackReason|planningFallbackReason|intentFallbackReason|marketDataSource" quant-ui/src/views quant-ui/src/components quant-ui/src/utils quant-ui/src/api
```

Expected result: fallback/provenance references remain in report display components, type declarations or static guard code. They must not drive retry, cancel, report review status, strategy signal commands, market event commands, config updates or route guards.

```powershell
rg -n "retryTask|cancelTask|reviewTaskReport|createStrategySignal|updateStrategySignalStatus|createMarketEvent|batchImportMarketEvents|mockIngestMarketEvents|syncMarketEventSource|updatePromptTemplate|updateModelStrategy|updateEventAutoTriggerRule|updateEventSourceConfig|updateAgentConfig|updateWorkflowConfig|updateRoleAccessConfig" quant-ui/src/views/report/ResearchWorkbenchView.vue quant-ui/src/utils/researchWorkbench.ts quant-ui/src/utils/taskActionAccess.ts
```

Expected result: no matches, unless the match is a comment inside an approved boundary note or static guard assertion. Workbench must not call command APIs.

```powershell
git diff --name-only
```

Expected result: only approved Phase 007 frontend files and `docs/harness/handoffs/phase-007-implementation.md` should appear as Window 2 changes. Existing unrelated dirty files must not be reverted or bundled into the Phase 007 implementation claim.

Backend and Python verification commands are not required because backend and Python changes are forbidden. If Window 2 cannot run `npm run build` because dependencies are absent or the environment is missing Node, it must record the exact environment failure in the implementation handoff.

## 11. Blocker Stop Rules

Window 2 must stop and write a blocker in `docs/harness/handoffs/phase-007-implementation.md` if any of these become necessary:

- Changing backend URL paths, HTTP methods, request bindings, permissions or response envelopes.
- Adding a new frontend route, route alias or endpoint alias.
- Changing `quant-ui/src/api/task.ts` function signatures or endpoint strings.
- Changing TypeScript DTO-like field names, optionality or shapes.
- Changing visible user behavior to prove the authority boundary.
- Adding a frontend truth resolver, fallback adapter, bridge, facade, helper or mapper in production code.
- Using workbench aggregation to decide retry, cancel, review, strategy, market, audit or config command eligibility.
- Using fallback provenance to decide report truth, risk truth, signal truth, market truth, task status or audit truth.
- Modifying Java, Python, database, Kafka, `ai-config`, package dependencies or build config.
- Static checks would require brittle assertions against unrelated formatting instead of real boundary rules.
- Current frontend behavior already promotes workbench or fallback metadata into command truth and cannot be documented as display/prefill-only without changing behavior.

When blocked, Window 2 must:

1. Stop edits at the smallest reversible point.
2. Record the exact file and code path that forced the blocker.
3. State which acceptance condition cannot be met.
4. State the smallest contract or behavior change that would be required.
5. Ask for human decision instead of expanding the phase.

## Window 2 Shape

Use one frontend-focused Window 2 implementation window.

Do not partition into backend or Python implementers. Do not start Phase 006 or Phase 005. Do not proceed until the user approves this Phase 007 architect handoff.

## Human Approval Request

Please approve this Phase 007 architect handoff before Window 2 starts.

Default approval meaning:

- No breaking changes.
- Keep all URLs and frontend routes stable.
- No backend, Python, database, Kafka, config or dependency changes.
- No user-visible business behavior change.
- Frontend implementation may add source-level boundary comments and focused static guard scripts only inside the file boundaries above.
