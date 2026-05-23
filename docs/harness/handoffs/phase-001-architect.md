# Phase 001 Architect Handoff

## Status

Window 1 architecture handoff for Phase 001.

This handoff does not approve implementation. Window 2 may start only after human approval of this file.

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
- `docs/harness/handoffs/steering-decision-phase-001.md`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/TaskQueryController.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/TaskQueryService.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/TaskQueryServiceImpl.java`
- `quant-ui/src/api/task.ts`
- `quant-ui/src/router/index.ts`

## Work Order

Window 2 must evaluate and implement in this order:

```text
belongs -> authority -> contract -> behavior
```

## 1. Phase Goal

Split the current multi-domain `TaskQueryController` API surface into domain-specific controller classes inside `ai-orchestration-service`.

The phase is a controller-surface cleanup only:

- Keep every existing URL path stable.
- Keep HTTP methods stable.
- Keep request DTO binding, path variable names, request parameter names and optional request bodies stable.
- Keep `Result.success(...)` response envelope behavior stable.
- Keep existing permission checks stable.
- Keep existing Sentinel resources and block handlers stable.
- Do not change business behavior, service internals, database access, frontend calls or Python workflow behavior.

This phase satisfies the first T1 exit criterion in `05-transition-lifetime.md`: split `TaskQueryController` into domain-specific controllers inside the same transition host.

## 2. Belongs Boundary

Allowed host:

- `ai-orchestration-service`, controller layer only.

Controller ownership target:

| Controller | Owns endpoint surface | May depend on |
| --- | --- | --- |
| `TaskQueryController` | task read-model, task runtime trace, task retry/cancel control | `TaskQueryService`, `TaskRetryService`, `TaskControlService`, `RoleAccessConfigService` |
| `MarketEventController` | market event read/transition command/source preview surface | `MarketQueryService`, `MarketEventService`, `EventSourcePreviewService`, `AuditConfigDashboardQueryService`, `RoleAccessConfigService` |
| `RiskWarningController` | risk warning read-model surface | `RiskQueryService` |
| `StrategySignalController` | strategy signal read-model and transition command surface | `StrategyQueryService`, `StrategySignalService`, `RoleAccessConfigService` |
| `ReportController` | report read-model, report center, review and report-version surface | `ReportQueryService`, `TaskReportService`, `RoleAccessConfigService` |
| `MarketIntelligenceController` | market intelligence aggregation view | `MarketQueryService` |
| `AuditComplianceController` | audit compliance read-model/dashboard surface | `AuditConfigDashboardQueryService`, `RoleAccessConfigService` |
| `ModelAgentConfigController` | model/agent/workflow/role config view and transition config commands | `AuditConfigDashboardQueryService`, config services, `RoleAccessConfigService` |
| `ResearchWorkbenchController` | research workbench display aggregation | `AuditConfigDashboardQueryService` |

Do not move any endpoint to `research-task-service`, `quant-ui`, `quant-ai-engine`, gateway, auth, config service or a new microservice.

## 3. Authority Boundary

Controller splitting must not change the authoritative object for any semantic in `02-authority-matrix.md`.

- Task creation remains under `research-task-service` and `POST /api/research/tasks`.
- Task runtime/read models remain the current `ai-orchestration-service` read-model surface.
- Report, risk, strategy, market event, audit and config remain transition-hosted in `ai-orchestration-service`.
- `research-workbench` remains display-only aggregation and must not be made SoT.
- Frontend remains a consumer of backend contracts and must not infer domain truth.
- Python fallback status and AI execution truth are out of scope.

Window 2 must not use the split as a reason to reclassify `ai-orchestration-service` as final architecture.

## 4. Allowed File Scope

Window 2 may modify only these implementation files:

- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/TaskQueryController.java`
- New controller classes under:
  - `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/`

Window 2 may add focused tests only if needed to lock endpoint mappings:

- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/*Controller*Test.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/*ControllerMapping*Test.java`

Window 2 must write its implementation handoff:

- `docs/harness/handoffs/phase-001-implementation.md`

## 5. Forbidden File Scope

Do not modify:

- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/TaskQueryService.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/TaskQueryServiceImpl.java`
- Any service implementation, mapper, entity, DTO, VO, enum, manager, consumer, producer or projection class.
- Any SQL migration or database schema file.
- Any `ai-config/*.json` file.
- Any `pom.xml`, Docker, deployment or middleware configuration file.
- Any `quant-ui` file, including `quant-ui/src/api/task.ts` and `quant-ui/src/router/index.ts`.
- Any `quant-ai-engine` file.
- Harness policy/state files other than the Window 2 implementation handoff.

If a compile error appears to require changing a forbidden file, Window 2 must stop and report a blocker.

## 6. Stable URL / API / Behavior Contract

All endpoints below must remain externally identical.

### Task Surface

| Method | Path | Target controller |
| --- | --- | --- |
| `GET` | `/api/tasks` | `TaskQueryController` |
| `GET` | `/api/tasks/failed` | `TaskQueryController` |
| `GET` | `/api/tasks/stats` | `TaskQueryController` |
| `GET` | `/api/tasks/{taskId}` | `TaskQueryController` |
| `GET` | `/api/tasks/{taskId}/state` | `TaskQueryController` |
| `GET` | `/api/tasks/{taskId}/steps` | `TaskQueryController` |
| `GET` | `/api/tasks/{taskId}/workflow` | `TaskQueryController` |
| `GET` | `/api/tasks/{taskId}/agents` | `TaskQueryController` |
| `GET` | `/api/tasks/{taskId}/audits` | `TaskQueryController` |
| `GET` | `/api/tasks/{taskId}/retries` | `TaskQueryController` |
| `GET` | `/api/tasks/{taskId}/full` | `TaskQueryController` |
| `POST` | `/api/tasks/{taskId}/retry` | `TaskQueryController` |
| `POST` | `/api/tasks/{taskId}/cancel` | `TaskQueryController` |

### Market Event Surface

| Method | Path | Target controller |
| --- | --- | --- |
| `GET` | `/api/tasks/market-events` | `MarketEventController` |
| `GET` | `/api/tasks/market-event-stats` | `MarketEventController` |
| `GET` | `/api/tasks/market-events/{eventId}` | `MarketEventController` |
| `GET` | `/api/tasks/market-events/ingest-history` | `MarketEventController` |
| `GET` | `/api/tasks/market-event-source-configs` | `MarketEventController` |
| `GET` | `/api/tasks/market-events/cninfo-proxy` | `MarketEventController` |
| `POST` | `/api/tasks/market-events` | `MarketEventController` |
| `POST` | `/api/tasks/market-events/batch-import/preview` | `MarketEventController` |
| `POST` | `/api/tasks/market-events/batch-import` | `MarketEventController` |
| `POST` | `/api/tasks/market-events/mock-ingest` | `MarketEventController` |
| `POST` | `/api/tasks/market-events/source-sync/{sourceCode}` | `MarketEventController` |
| `POST` | `/api/tasks/market-events/source-preview/{sourceCode}` | `MarketEventController` |
| `POST` | `/api/tasks/market-events/source-diagnose/{sourceCode}` | `MarketEventController` |

### Risk Surface

| Method | Path | Target controller |
| --- | --- | --- |
| `GET` | `/api/tasks/risk-warnings` | `RiskWarningController` |
| `GET` | `/api/tasks/risk-warning-stats` | `RiskWarningController` |

### Strategy Surface

| Method | Path | Target controller |
| --- | --- | --- |
| `GET` | `/api/tasks/strategy-signals` | `StrategySignalController` |
| `GET` | `/api/tasks/strategy-signal-stats` | `StrategySignalController` |
| `GET` | `/api/tasks/strategy-signals/{signalId}/factors` | `StrategySignalController` |
| `POST` | `/api/tasks/strategy-signals` | `StrategySignalController` |
| `POST` | `/api/tasks/strategy-signals/{signalId}/status` | `StrategySignalController` |

### Report Surface

| Method | Path | Target controller |
| --- | --- | --- |
| `GET` | `/api/tasks/report-center` | `ReportController` |
| `GET` | `/api/tasks/report-center-stats` | `ReportController` |
| `GET` | `/api/tasks/report-review-stats` | `ReportController` |
| `GET` | `/api/tasks/{taskId}/report` | `ReportController` |
| `GET` | `/api/tasks/{taskId}/report/versions` | `ReportController` |
| `GET` | `/api/tasks/{taskId}/report/versions/compare` | `ReportController` |
| `GET` | `/api/tasks/{taskId}/report/versions/{versionNo}` | `ReportController` |
| `GET` | `/api/tasks/{taskId}/report/review-logs` | `ReportController` |
| `POST` | `/api/tasks/{taskId}/report/review` | `ReportController` |

### Market Intelligence Surface

| Method | Path | Target controller |
| --- | --- | --- |
| `GET` | `/api/tasks/market-intelligence` | `MarketIntelligenceController` |
| `GET` | `/api/tasks/market-intelligence-stats` | `MarketIntelligenceController` |

### Audit Compliance Surface

| Method | Path | Target controller |
| --- | --- | --- |
| `GET` | `/api/tasks/audit-compliance` | `AuditComplianceController` |
| `GET` | `/api/tasks/audit-compliance-stats` | `AuditComplianceController` |

### Model / Agent Config Surface

| Method | Path | Target controller |
| --- | --- | --- |
| `GET` | `/api/tasks/model-agent-config` | `ModelAgentConfigController` |
| `GET` | `/api/tasks/role-access-configs` | `ModelAgentConfigController` |
| `POST` | `/api/tasks/model-agent-config/prompt-templates/{templateCode}` | `ModelAgentConfigController` |
| `POST` | `/api/tasks/model-agent-config/model-strategies/{strategyCode}` | `ModelAgentConfigController` |
| `POST` | `/api/tasks/model-agent-config/event-auto-trigger-rules/{ruleCode}` | `ModelAgentConfigController` |
| `POST` | `/api/tasks/model-agent-config/event-sources/{sourceCode}` | `ModelAgentConfigController` |
| `POST` | `/api/tasks/model-agent-config/agents/{agentCode}` | `ModelAgentConfigController` |
| `POST` | `/api/tasks/model-agent-config/workflows/{workflowCode}` | `ModelAgentConfigController` |
| `POST` | `/api/tasks/model-agent-config/role-access/{roleCode}` | `ModelAgentConfigController` |

### Research Workbench Surface

| Method | Path | Target controller |
| --- | --- | --- |
| `GET` | `/api/tasks/research-workbench` | `ResearchWorkbenchController` |

## 7. Allowed New Class / Method Types

Allowed:

- New `@RestController` classes in `com.quant.aiorchestrator.controller`.
- Constructor-injected controller dependencies with `@RequiredArgsConstructor`.
- Controller methods that exactly preserve existing mapping annotations and delegate to existing services.
- Existing permission checks copied exactly from the old method.
- Existing `@SentinelResource` annotations copied exactly where present:
  - `pageTasks`
  - `getTaskFullDetail`
- Focused controller mapping tests that assert the URL inventory remains stable.

Controller methods may do only these things:

1. Accept the same request inputs as the old method.
2. Call the same permission check, if the old method had one.
3. Delegate to the same existing service method with the same arguments.
4. Return the same `Result.success(...)` shape.

## 8. Forbidden Helpers / Adapters / Fallbacks / Bridges

Do not add:

- New service helper classes.
- Controller facade services.
- Adapter layers between controllers and existing services.
- Compatibility bridge endpoints.
- Legacy duplicate controllers that keep old routes while new routes also exist.
- Catch-all or wildcard mappings such as `/{path}` or `/**`.
- Frontend API remapping helpers.
- Backend fallback data paths.
- Request/response conversion helpers.
- New DTO or VO aliases.
- New auth, role, gateway or tenant bridge logic.

This phase is a move-only controller split, not a hidden service refactor.

## 9. Acceptance Conditions

Window 2 implementation is acceptable only if all are true:

- `TaskQueryController` no longer owns market, risk, strategy, report, market-intelligence, audit, config or workbench endpoint methods.
- Each moved endpoint exists exactly once after the split.
- No endpoint path, HTTP method, request DTO, path variable, request parameter, response type, response envelope or permission check changes.
- No new endpoint is introduced.
- No existing endpoint is removed.
- No frontend file is changed.
- No service, mapper, entity, DTO, VO, enum, manager, consumer, producer or projection file is changed.
- Spring application context does not fail due to duplicate or ambiguous request mappings.
- `mvn -q test` passes from `quant-ai-platform/quant-services`.
- Window 2 records the final endpoint grouping and verification results in `docs/harness/handoffs/phase-001-implementation.md`.

## 10. Required Verification Commands

From `D:\projects\bussiness\quant-ai-platform\quant-services`:

```powershell
mvn -q test
```

From `D:\projects\bussiness`:

```powershell
rg -n "@(RequestMapping|GetMapping|PostMapping)" quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller
```

Window 2 must compare the mapping scan output against the endpoint inventory in this handoff.

`npm run build` is not required for this phase because frontend files are out of scope. If Window 2 believes a frontend change is required, that is a blocker, not an instruction to run the frontend build.

## 11. Blocker Stop Rules

Window 2 must stop and report a blocker if any of these occur:

- A URL path change appears necessary.
- A frontend change appears necessary.
- A service/interface/DTO/VO/entity/mapper change appears necessary.
- The controller split creates duplicate or ambiguous mappings that cannot be resolved without changing URL paths.
- A permission check cannot be preserved exactly.
- A Sentinel block handler cannot be preserved exactly.
- `mvn -q test` fails for reasons caused by the controller split and fixing it would require leaving the allowed file scope.
- Any business behavior change is discovered or requested.

When blocked, Window 2 must not widen scope. It must document:

- What was attempted.
- Which allowed boundary was hit.
- The exact endpoint or file involved.
- The smallest approval request needed to proceed.

## Human Approval Gate

Human approval is required before Window 2 begins.

Approval request:

Please approve or reject this Phase 001 Architect handoff for Window 2 backend implementation.
