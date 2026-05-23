# Phase 002 Architect Handoff

## Status

Phase: Phase 002 - Split `TaskQueryServiceImpl` Internal Query Services.

Window: Window 1 - Phase Architect.

Status: ready for human approval before Window 2. This handoff does not authorize implementation by itself.

## Inputs Read

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
- `docs/harness/handoffs/steering-decision-phase-002.md`
- `docs/harness/handoffs/phase-001-final.md`
- Current `ai-orchestration-service` controller and query service code.

## 1. Phase Goal

Split the mixed read-model responsibilities currently concentrated in `TaskQueryServiceImpl` into domain-specific internal query services inside `ai-orchestration-service`.

The phase must reduce D001/D004 drift only. It must preserve all external API contracts, all legacy `/api/tasks/*` URL paths, all response VO shapes, all existing permissions, all cache semantics that are already observable, and all business behavior.

This is a backend-only refactor phase. It is not a feature phase.

## Decision Order

Window 2 must evaluate every edit in this order:

```text
belongs -> authority -> contract -> behavior
```

Behavior compatibility is necessary, but it is not enough if the code still routes non-task domains through `TaskQueryServiceImpl`.

## 2. Allowed File Scope

Window 2 may modify these backend files under `ai-orchestration-service`:

- `src/main/java/com/quant/aiorchestrator/service/TaskQueryService.java`
- `src/main/java/com/quant/aiorchestrator/service/ReportQueryService.java`
- `src/main/java/com/quant/aiorchestrator/service/RiskQueryService.java`
- `src/main/java/com/quant/aiorchestrator/service/StrategyQueryService.java`
- `src/main/java/com/quant/aiorchestrator/service/MarketQueryService.java`
- `src/main/java/com/quant/aiorchestrator/service/AuditConfigDashboardQueryService.java`, only to retire it or narrow it if the new split makes that necessary.
- `src/main/java/com/quant/aiorchestrator/service/impl/TaskQueryServiceImpl.java`
- `src/main/java/com/quant/aiorchestrator/service/impl/ReportQueryServiceImpl.java`
- `src/main/java/com/quant/aiorchestrator/service/impl/RiskQueryServiceImpl.java`
- `src/main/java/com/quant/aiorchestrator/service/impl/StrategyQueryServiceImpl.java`
- `src/main/java/com/quant/aiorchestrator/service/impl/MarketQueryServiceImpl.java`
- `src/main/java/com/quant/aiorchestrator/service/impl/AuditConfigDashboardQueryServiceImpl.java`, only to retire it or narrow it if the new split makes that necessary.

Window 2 may add these new backend query service files if needed:

- `src/main/java/com/quant/aiorchestrator/service/AuditComplianceQueryService.java`
- `src/main/java/com/quant/aiorchestrator/service/ModelAgentConfigDashboardQueryService.java`
- `src/main/java/com/quant/aiorchestrator/service/ResearchWorkbenchQueryService.java`
- Matching implementations under `src/main/java/com/quant/aiorchestrator/service/impl/`.

Window 2 may modify only these controller files, and only to change injected service dependencies or method call targets without changing mappings, annotations, permissions, request binding, or response envelopes:

- `src/main/java/com/quant/aiorchestrator/controller/AuditComplianceController.java`
- `src/main/java/com/quant/aiorchestrator/controller/ModelAgentConfigController.java`
- `src/main/java/com/quant/aiorchestrator/controller/ResearchWorkbenchController.java`

Window 2 may modify or add tests under:

- `src/test/java/com/quant/aiorchestrationservice/`

Window 2 must write:

- `docs/harness/handoffs/phase-002-implementation.md`

## 3. Forbidden File Scope

Window 2 must not modify:

- `quant-ui/**`
- `quant-ai-platform/quant-ai-engine/**`
- `quant-ai-platform/quant-services/quant-business/research-task-service/**`
- database migration or schema files
- `quant-ai-platform/ai-config/**`
- Maven POM files
- Docker, deployment, Nacos, Sentinel dashboard, gateway or auth-service files
- DTO/VO/entity/mapper files, unless a compile-only import cleanup proves unavoidable; response fields must not change.
- Kafka producers, consumers, topic constants, message DTOs or projection command services.

Window 2 must not modify controller URL mappings or permissions in any controller, including the files it may touch for dependency rewiring.

## 4. Stable URL / API / Behavior Contracts

All external paths, HTTP methods, request parameters, request bodies, permissions and `Result.success(...)` response envelopes must remain stable.

Stable task endpoints:

- `GET /api/tasks`
- `GET /api/tasks/failed`
- `GET /api/tasks/stats`
- `GET /api/tasks/{taskId}`
- `GET /api/tasks/{taskId}/state`
- `GET /api/tasks/{taskId}/steps`
- `GET /api/tasks/{taskId}/workflow`
- `GET /api/tasks/{taskId}/agents`
- `GET /api/tasks/{taskId}/audits`
- `GET /api/tasks/{taskId}/retries`
- `GET /api/tasks/{taskId}/full`
- `POST /api/tasks/{taskId}/retry`
- `POST /api/tasks/{taskId}/cancel`

Stable report endpoints:

- `GET /api/tasks/report-center`
- `GET /api/tasks/report-center-stats`
- `GET /api/tasks/report-review-stats`
- `GET /api/tasks/{taskId}/report`
- `GET /api/tasks/{taskId}/report/versions`
- `GET /api/tasks/{taskId}/report/versions/compare`
- `GET /api/tasks/{taskId}/report/versions/{versionNo}`
- `GET /api/tasks/{taskId}/report/review-logs`
- `POST /api/tasks/{taskId}/report/review`

Stable risk, strategy, market, audit, config and workbench endpoints:

- `GET /api/tasks/risk-warnings`
- `GET /api/tasks/risk-warning-stats`
- `GET /api/tasks/strategy-signals`
- `GET /api/tasks/strategy-signal-stats`
- `GET /api/tasks/strategy-signals/{signalId}/factors`
- `POST /api/tasks/strategy-signals`
- `POST /api/tasks/strategy-signals/{signalId}/status`
- `GET /api/tasks/market-events`
- `GET /api/tasks/market-event-stats`
- `GET /api/tasks/market-events/{eventId}`
- `GET /api/tasks/market-events/ingest-history`
- `GET /api/tasks/market-event-source-configs`
- `POST /api/tasks/market-events`
- `POST /api/tasks/market-events/batch-import/preview`
- `POST /api/tasks/market-events/batch-import`
- `POST /api/tasks/market-events/mock-ingest`
- `POST /api/tasks/market-events/source-sync/{sourceCode}`
- `POST /api/tasks/market-events/source-preview/{sourceCode}`
- `POST /api/tasks/market-events/source-diagnose/{sourceCode}`
- `GET /api/tasks/market-events/cninfo-proxy`
- `GET /api/tasks/market-intelligence`
- `GET /api/tasks/market-intelligence-stats`
- `GET /api/tasks/audit-compliance`
- `GET /api/tasks/audit-compliance-stats`
- `GET /api/tasks/model-agent-config`
- `GET /api/tasks/role-access-configs`
- `POST /api/tasks/model-agent-config/prompt-templates/{templateCode}`
- `POST /api/tasks/model-agent-config/model-strategies/{strategyCode}`
- `POST /api/tasks/model-agent-config/event-auto-trigger-rules/{ruleCode}`
- `POST /api/tasks/model-agent-config/event-sources/{sourceCode}`
- `POST /api/tasks/model-agent-config/agents/{agentCode}`
- `POST /api/tasks/model-agent-config/workflows/{workflowCode}`
- `POST /api/tasks/model-agent-config/role-access/{roleCode}`
- `GET /api/tasks/research-workbench`

Stable behavior includes:

- Existing pagination defaults and sorting order.
- Existing cache keys and TTLs for task state, task detail/full detail, task stats and task report.
- Existing fallback/preferred display hydration behavior.
- Existing permission checks in controllers.
- Existing Sentinel resources for `pageTasks` and `getTaskFullDetail`.
- Existing null/empty/error behavior.

## 5. Current Method Inventory And Required Destination

Current public methods in `TaskQueryServiceImpl` must be split as follows:

| Current method | Required destination |
| --- | --- |
| `getTaskDetail` | keep in `TaskQueryServiceImpl` |
| `getTaskState` | keep in `TaskQueryServiceImpl` |
| `listTaskSteps` | keep in `TaskQueryServiceImpl` |
| `getWorkflowInstance` | keep in `TaskQueryServiceImpl` |
| `listAgentExecutions` | keep in `TaskQueryServiceImpl` |
| `listAuditRecords` | keep in `TaskQueryServiceImpl` as task execution trace read-model |
| `pageTasks` | keep in `TaskQueryServiceImpl` |
| `listRetryLogs` | keep in `TaskQueryServiceImpl` |
| `getTaskFullDetail` | keep in `TaskQueryServiceImpl`; it may call `ReportQueryService.getTaskReportOnly` read-only |
| `getTaskStats` | keep in `TaskQueryServiceImpl` |
| `pageRiskWarnings` | move to `RiskQueryServiceImpl` |
| `getRiskWarningStats` | move to `RiskQueryServiceImpl` |
| `pageStrategySignals` | move to `StrategyQueryServiceImpl` |
| `getStrategySignalStats` | move to `StrategyQueryServiceImpl` |
| `pageReportCenter` | move to `ReportQueryServiceImpl` |
| `getReportCenterStats` | move to `ReportQueryServiceImpl` |
| `getTaskReportOnly` | move to `ReportQueryServiceImpl` |
| `getReportReviewStats` | move to `ReportQueryServiceImpl` |
| `pageMarketIntelligence` | move to `MarketQueryServiceImpl` |
| `getMarketIntelligenceStats` | move to `MarketQueryServiceImpl` |
| `pageAuditCompliance` | move to `AuditComplianceQueryServiceImpl`, or to a narrowed `AuditConfigDashboardQueryServiceImpl` if Window 2 keeps that interface |
| `getAuditComplianceStats` | move to `AuditComplianceQueryServiceImpl`, or to a narrowed `AuditConfigDashboardQueryServiceImpl` if Window 2 keeps that interface |
| `getModelAgentConfigCenter` | move to `ModelAgentConfigDashboardQueryServiceImpl`, or to a narrowed `AuditConfigDashboardQueryServiceImpl` if Window 2 keeps that interface |
| `getResearchWorkbench` | move to `ResearchWorkbenchQueryServiceImpl` |

After the split, `TaskQueryService` should expose only the task endpoint methods that `TaskQueryController` needs.

Private methods and nested records must move with the owning public method. Shared calculations may be duplicated locally if needed to preserve boundaries. Do not create a generic shared helper just to avoid duplication.

Current private method clusters to move:

- Task cluster: `selectTaskById`, `refreshTaskStateCache`, `toTaskStepVO`, `toAgentExecutionVO`, `toAuditRecordVO`, `shouldDisplayTaskErrorMessage`.
- Risk cluster: `RiskWarningFollowUpSummary`, `RiskProjection`, `listRiskWarningRecords`, `loadActiveRiskWarnings`, `listRiskWarningRecordsFromDomain`, both `toRiskWarningItem` overloads, `buildDomainRiskReasons`, `isDomainRiskHumanReview`, `resolveDomainRiskLevel`, `buildDomainRiskSourceTags`, `listRiskWarningRecordsFromReports`, `sortRiskWarningRecords`, `matchesRiskWarningQuery`, `mergeRiskReasons`, `resolveRiskWarningFollowUpSummary`, `defaultRiskWarningFollowUpSummary`, `resolveRiskWarningFollowUpStatus`, risk projection helpers.
- Strategy cluster: `StrategySignalFollowUpSummary`, `listStrategySignalRecords`, `loadActiveStrategySignals`, `listStrategySignalRecordsFromDomain`, both `toStrategySignalItem` overloads, `resolveDomainSignalDirection`, `resolveDomainSignalStrength`, `buildDomainSignalSources`, `buildDomainSignalSourceTags`, `listStrategySignalRecordsFromReports`, `sortStrategySignalRecords`, `matchesStrategySignalQuery`, `resolveStrategySummary`, `resolveSignalSources`, `buildSignalSourceTags`, `resolveStrategySignalFollowUpSummary`, `defaultStrategySignalFollowUpSummary`, `resolveStrategySignalFollowUpStatus`, `resolveSignalDirection`, `resolveSignalStrength`, `calculateSignalScore`, `countKeywords`.
- Report cluster: `listReportCenterRecords`, `toReportCenterItem`, `matchesReportCenterQuery`, `resolveReportCenterSummary`, `isReportRevised`, `isSummaryRevised`, `isHighlightsRevised`, `isRiskPointsRevised`, `buildRiskSourceTags`, `resolveReportType`, `toTaskReportVO`, `hydrateTaskReportDomainFields`, `toReportSection`, `toReportEvidenceItem`, `toEvidenceRefText`, `mergeEvidenceItems`, `evidenceItemKey`, `mergeTextRefs`, `buildDomainRiskWarningMessages`, `isCurrentTaskReportCache`, `resolveTaskReportType`, `resolveDisplaySummary`, `resolveDisplayList`, `hydrateTaskReportContextFields`, `mergeObjectMap`, `normalizeObjectMap`, `extractReportMetaNode`, `readObjectMap`, `readEvidenceItems`.
- Market intelligence cluster: `MarketIntelligenceFollowUpSummary`, `listMarketIntelligenceRecords`, `toMarketIntelligenceItem`, `matchesMarketIntelligenceQuery`, `resolveMarketIntelligenceType`, `resolveMarketIntelligenceFollowUpSummary`, `defaultMarketIntelligenceFollowUpSummary`, `resolveMarketIntelligenceFollowUpStatus`, `buildMarketIntelligenceSourceTags`.
- Audit compliance cluster: `listAuditComplianceRecords`, `toAuditComplianceItem`, `matchesAuditComplianceQuery`, `hasDecisionTrace`, `hasPromptAuditTrail`, `isRevisedReport`, `resolveOriginalSummary`, `normalizeAuditResultStatus`, `firstNonNullOf`.
- Model / Agent config dashboard cluster: `buildWorkflowConfig`, `buildAgentConfig`, `applyAgentConfigsToWorkflows`, `buildWorkflowTimeoutSummary`, `resolveWorkflowTimeoutSeconds`, `buildModelStrategy`, `resolveRuntimeMode`, `buildPromptTemplate`, `buildToolWhitelist`, `enrichEventSourceConfigStats`, `defaultInt`.
- Workbench display aggregation cluster: `resolveRecentTaskLimit`, `toResearchWorkbenchInsight`, `toResearchWorkbenchRecentTask`, `populateResearchWorkbenchDispositionSummaries`, `buildDomainRiskInsightPoints`, `emptyResearchWorkbenchDispositionSummary`, `buildResearchWorkbenchDispositionSummary`, `loadResearchWorkbenchFollowUpTasks`, `groupFollowUpTasksBySourceTaskId`, `groupFollowUpTasksBySourceReportId`, `readPreferredTextList`.
- Shared text/date utilities: `readTextList`, `readTextList(JsonNode)`, `hasText`, `containsIgnoreCase`, `firstNonNull`, `normalizeText`, `defaultVersionNo`. These should move into the smallest owning service that needs them. If two services need them, duplicate private methods rather than adding a new helper service.

## 6. Allowed New Class / Method Types

Allowed:

- Domain query service interfaces and implementations for read-model boundaries listed above.
- Private mapper-to-VO methods inside the owning query implementation.
- Private filter, sort, pagination and cache hydration methods inside the owning query implementation.
- Private or nested records used only by one owning query implementation.
- Tests that assert service boundary, method ownership and behavior parity.

Allowed dependency direction:

- Controllers call their domain query services.
- `TaskQueryServiceImpl` may call `ReportQueryService.getTaskReportOnly` only for `getTaskFullDetail`.
- Domain query services may depend on mappers, config services and command/domain services already used in the same domain.
- Domain query services must not depend on `TaskQueryService` for non-task behavior.

## 7. Forbidden Helpers / Adapters / Fallbacks / Bridges

Do not add:

- Any new `*Helper`, `*Adapter`, `*Bridge`, `*Facade`, `*Fallback`, service locator, reflection dispatcher or generic read-model router.
- A new service whose primary job is to delegate back to `TaskQueryService`.
- A new fallback path, fallback value, preferred/fallback precedence, or hidden merge rule.
- A new endpoint, URL alias, compatibility URL, DTO field or VO field.
- A new source of truth for task/report/risk/strategy/market/audit/config semantics.
- A cross-domain command path that depends on `research-workbench` or market-intelligence aggregation output.

Existing fallback/preferred-field behavior may only be moved as-is. It must remain display hydration unless a domain authority rule already says otherwise.

## 8. Acceptance Conditions

Phase 002 is acceptable only if all conditions hold:

- `TaskQueryServiceImpl` contains task read-model and task trace logic only.
- `TaskQueryService` no longer exposes risk, strategy, report, market-intelligence, audit, config or workbench methods.
- `ReportQueryServiceImpl`, `RiskQueryServiceImpl`, `StrategyQueryServiceImpl`, `MarketQueryServiceImpl` and the audit/config/workbench query service implementations no longer delegate their moved read-model methods back to `TaskQueryService`.
- Existing controller mappings remain unchanged.
- Existing permissions remain unchanged.
- Existing Sentinel annotations remain unchanged.
- Existing VO/DTO/entity/mapper shapes remain unchanged.
- No database writes are introduced by query services, except existing Redis cache writes already present in task/report read paths with the same keys and TTL intent.
- `research-workbench` remains display-only aggregation and is not consumed by backend commands as authority.
- Fallback/preferred-field logic remains observable display hydration and is not promoted to business truth.
- Relevant tests are updated from `TaskQueryServiceImpl` to the new owning query service where domain logic moved.
- At least one boundary test or equivalent focused test prevents non-task domain query implementations from importing/injecting `TaskQueryService` again.
- `mvn -q test` passes from `quant-ai-platform/quant-services`.

## 9. Required Verification Commands

Window 2 must run this command from `D:\projects\bussiness\quant-ai-platform\quant-services`:

```powershell
mvn -q test
```

Window 2 must also run or manually inspect these boundary checks from `D:\projects\bussiness`:

```powershell
rg -n "TaskQueryService" quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl
```

Expected result: no non-task domain query implementation should import or inject `TaskQueryService`. `TaskQueryServiceImpl` itself is allowed.

```powershell
rg -n "pageRiskWarnings|getRiskWarningStats|pageStrategySignals|getStrategySignalStats|pageReportCenter|getReportCenterStats|pageMarketIntelligence|getMarketIntelligenceStats|pageAuditCompliance|getAuditComplianceStats|getModelAgentConfigCenter|getResearchWorkbench|getTaskReportOnly|getReportReviewStats" quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/TaskQueryService.java quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/TaskQueryServiceImpl.java
```

Expected result: no matches in `TaskQueryService.java` or `TaskQueryServiceImpl.java`, except Window 2 may keep a call to `ReportQueryService.getTaskReportOnly` inside `getTaskFullDetail`.

Window 2 should include exact command outcomes in `phase-002-implementation.md`.

Frontend and Python verification commands are not required because frontend and Python are forbidden in this phase.

## 10. Blocker Stop Rules

Window 2 must stop and write a blocker note instead of expanding scope if it discovers any of these:

- Preserving behavior requires changing any URL path, HTTP method, DTO, VO, permission or controller mapping.
- The refactor requires a database schema change, Kafka contract change, frontend change or Python change.
- A cyclic service dependency is needed to compile.
- A generic helper, bridge, adapter, facade or fallback service appears necessary to avoid duplication.
- Existing tests prove that a current behavior is internally inconsistent and cannot be preserved with a mechanical ownership move.
- Moving `getTaskReportOnly` or workbench logic would silently change fallback/preferred-field precedence.
- Query logic needs to become a command or mutate domain facts.

When blocked, Window 2 must:

1. Stop business-code edits at the smallest reversible point.
2. Record the blocker in `docs/harness/handoffs/phase-002-implementation.md`.
3. State which acceptance condition cannot be met.
4. Ask for human decision instead of choosing Phase 003 or any new fallback work.

## Window 2 Shape

Use one backend Window 2 implementation window. Do not partition into multiple implementers unless the user explicitly approves a revised handoff.
