# Phase 006 Architect Handoff

## Status

Window: Window 1 - Phase Architect.

Phase: Phase 006 - Legacy `/api/tasks/*` Contract Freeze for Non-Task Domains.

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
- `docs/harness/handoffs/steering-decision-phase-006.md`

Backend code inspected for current Phase 006 contract facts:

- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/TaskQueryController.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/MarketEventController.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/RiskWarningController.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/StrategySignalController.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/ReportController.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/MarketIntelligenceController.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/AuditComplianceController.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/ModelAgentConfigController.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/ResearchWorkbenchController.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/RoleAccessConfigService.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/TaskControllerMappingTest.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/QueryServiceBoundaryTests.java`

Phase 006 is not Phase 001, so the Phase 001 special implementation reading list was not the controlling scope for this handoff.

## 1. Phase Goal

Freeze the approved legacy `/api/tasks/*` contracts for non-task domains so D002 stops drifting while URL stability remains required.

The bounded goal is:

- Declare the current non-task domain endpoints intentionally preserved under `/api/tasks/*`.
- Guard URL path, HTTP method, controller ownership, request binding, `Result<T>` response envelope and permission behavior against accidental drift.
- Preserve all existing behavior, including legacy namespace shape.
- Avoid new URL aliases, endpoint moves, domain route migrations or breaking changes.

This is a backend-focused contract/test/documentation phase. It is not a feature phase, not a service split phase, not a frontend phase and not a route cleanup phase.

Window 2 must evaluate every edit in this order:

```text
belongs -> authority -> contract -> behavior
```

Passing tests is not enough if a change weakens the legacy contract boundary or reclassifies the legacy namespace as final architecture.

## 2. Belongs

The legacy non-task `/api/tasks/*` endpoints currently belong to `ai-orchestration-service` as a transition host.

Allowed transition-host responsibilities in this phase:

- report query and review contracts
- risk warning query contracts
- strategy signal query and status/update contracts
- market event query, ingest, preview and source-sync contracts
- market intelligence display contracts
- audit compliance query contracts
- model/agent/workflow/role config dashboard and update contracts
- research workbench display aggregation contract

Not allowed:

- Moving these endpoints to another service.
- Adding a gateway, alias controller or compatibility bridge.
- Treating the legacy `/api/tasks/*` namespace as the target architecture.
- Moving formal task creation into `ai-orchestration-service`.
- Moving AI execution, Python fallback or frontend truth decisions into this phase.

`TaskQueryController` owns task endpoints only. Phase 006 must not add non-task endpoints back into `TaskQueryController`.

## 3. Authority

The contract freeze must preserve the authority classes from `02-authority-matrix.md` and `04-contract-map.md`.

Authoritative or transition read models:

- `GET /api/tasks/{taskId}/report` and report version endpoints expose report read models from the transition host.
- `GET /api/tasks/risk-warnings` exposes the risk warning read model.
- `GET /api/tasks/strategy-signals` exposes the strategy signal read model.
- `GET /api/tasks/market-events` exposes the market event read model.
- audit and config read endpoints expose their current transition read/config views.

Display aggregations:

- `GET /api/tasks/research-workbench` remains display-only aggregation.
- `GET /api/tasks/market-intelligence` remains a display/read-model surface and must not replace `market_event` authority.
- stats endpoints remain dashboard/display aggregation unless the existing domain contract already says otherwise.

Command contracts:

- report review, market event create/import/mock/source-sync, strategy signal create/status and config update endpoints remain command contracts in the current transition host.
- Existing permission calls are part of the behavior contract and must stay stable.

Forbidden authority changes:

- No endpoint may become a second source of truth.
- No aggregation view may become command input authority.
- No frontend, Python, DTO, VO, entity, database, Kafka or config truth may be introduced or changed.
- No fallback, preferred-field merge or display hydration behavior may be promoted to domain truth.

## 4. Stable Contract Inventory

All endpoints below intentionally keep the legacy `/api/tasks/*` path for Phase 006. This inventory is the target for Window 2 contract guards.

### Report Contracts

| Endpoint | Controller | Binding and response | Permission behavior |
| --- | --- | --- | --- |
| `GET /api/tasks/report-center` | `ReportController` | `ReportCenterPageQueryDTO` query object -> `Result<ReportCenterPageVO>` | no explicit `requirePermission` |
| `GET /api/tasks/report-center-stats` | `ReportController` | no request body -> `Result<ReportCenterStatsVO>` | no explicit `requirePermission` |
| `GET /api/tasks/{taskId}/report` | `ReportController` | `@PathVariable("taskId")` -> `Result<TaskReportVO>` | no explicit `requirePermission` |
| `GET /api/tasks/{taskId}/report/versions` | `ReportController` | `@PathVariable("taskId")` -> `Result<List<ReportVersionVO>>` | no explicit `requirePermission` |
| `GET /api/tasks/{taskId}/report/versions/compare` | `ReportController` | `@PathVariable("taskId")`, `@RequestParam("fromVersionNo")`, `@RequestParam("toVersionNo")` -> `Result<ReportVersionCompareVO>` | no explicit `requirePermission` |
| `GET /api/tasks/{taskId}/report/versions/{versionNo}` | `ReportController` | `@PathVariable("taskId")`, `@PathVariable("versionNo")` -> `Result<ReportVersionVO>` | no explicit `requirePermission` |
| `GET /api/tasks/{taskId}/report/review-logs` | `ReportController` | `@PathVariable("taskId")` -> `Result<List<TaskReportReviewLogVO>>` | no explicit `requirePermission` |
| `POST /api/tasks/{taskId}/report/review` | `ReportController` | `@PathVariable("taskId")`, `@RequestBody TaskReportReviewDTO` -> `Result<String>` | `PERMISSION_REPORT_REVIEW` |
| `GET /api/tasks/report-review-stats` | `ReportController` | no request body -> `Result<ReportReviewStatsVO>` | no explicit `requirePermission` |

### Risk Warning Contracts

| Endpoint | Controller | Binding and response | Permission behavior |
| --- | --- | --- | --- |
| `GET /api/tasks/risk-warnings` | `RiskWarningController` | `RiskWarningPageQueryDTO` query object -> `Result<RiskWarningPageVO>` | no explicit `requirePermission` |
| `GET /api/tasks/risk-warning-stats` | `RiskWarningController` | no request body -> `Result<RiskWarningStatsVO>` | no explicit `requirePermission` |

### Strategy Signal Contracts

| Endpoint | Controller | Binding and response | Permission behavior |
| --- | --- | --- | --- |
| `GET /api/tasks/strategy-signals` | `StrategySignalController` | `StrategySignalPageQueryDTO` query object -> `Result<StrategySignalPageVO>` | no explicit `requirePermission` |
| `GET /api/tasks/strategy-signal-stats` | `StrategySignalController` | no request body -> `Result<StrategySignalStatsVO>` | no explicit `requirePermission` |
| `POST /api/tasks/strategy-signals` | `StrategySignalController` | `@RequestBody StrategySignalCreateDTO` -> `Result<String>` | `PERMISSION_REPORT_REVIEW` |
| `GET /api/tasks/strategy-signals/{signalId}/factors` | `StrategySignalController` | `@PathVariable("signalId")` -> `Result<List<StrategySignalFactorItemVO>>` | no explicit `requirePermission` |
| `POST /api/tasks/strategy-signals/{signalId}/status` | `StrategySignalController` | `@PathVariable("signalId")`, `@RequestBody StrategySignalStatusUpdateDTO` -> `Result<String>` | `PERMISSION_REPORT_REVIEW` |

### Market Event Contracts

| Endpoint | Controller | Binding and response | Permission behavior |
| --- | --- | --- | --- |
| `GET /api/tasks/market-events` | `MarketEventController` | `MarketEventPageQueryDTO` query object -> `Result<MarketEventPageVO>` | no explicit `requirePermission` |
| `GET /api/tasks/market-event-stats` | `MarketEventController` | no request body -> `Result<MarketEventStatsVO>` | no explicit `requirePermission` |
| `GET /api/tasks/market-events/{eventId}` | `MarketEventController` | `@PathVariable("eventId")` -> `Result<MarketEventListItemVO>` | no explicit `requirePermission` |
| `GET /api/tasks/market-events/ingest-history` | `MarketEventController` | no request body -> `Result<List<MarketEventIngestHistoryItemVO>>` | no explicit `requirePermission` |
| `GET /api/tasks/market-event-source-configs` | `MarketEventController` | no request body -> `Result<List<EventSourceConfigItemVO>>` | no explicit `requirePermission` |
| `POST /api/tasks/market-events` | `MarketEventController` | `@RequestBody MarketEventCreateDTO` -> `Result<MarketEventCreateResultVO>` | `PERMISSION_TASK_CREATE` |
| `POST /api/tasks/market-events/batch-import/preview` | `MarketEventController` | `@RequestBody MarketEventBatchImportDTO` -> `Result<MarketEventBatchPreviewResultVO>` | `PERMISSION_TASK_CREATE` |
| `POST /api/tasks/market-events/batch-import` | `MarketEventController` | `@RequestBody MarketEventBatchImportDTO` -> `Result<MarketEventBatchImportResultVO>` | `PERMISSION_TASK_CREATE` |
| `POST /api/tasks/market-events/mock-ingest` | `MarketEventController` | `@RequestBody MarketEventMockIngestDTO` -> `Result<MarketEventBatchImportResultVO>` | `PERMISSION_TASK_CREATE` |
| `POST /api/tasks/market-events/source-sync/{sourceCode}` | `MarketEventController` | `@PathVariable("sourceCode")`, `@RequestBody MarketEventSourceSyncDTO` -> `Result<MarketEventBatchImportResultVO>` | `PERMISSION_TASK_CREATE` |
| `POST /api/tasks/market-events/source-preview/{sourceCode}` | `MarketEventController` | `@PathVariable("sourceCode")`, `@RequestBody MarketEventSourceSyncDTO` -> `Result<EventSourcePreviewResultVO>` | `PERMISSION_MODEL_AGENT_CONFIG_VIEW` |
| `POST /api/tasks/market-events/source-diagnose/{sourceCode}` | `MarketEventController` | `@PathVariable("sourceCode")`, `@RequestBody MarketEventSourceSyncDTO` -> `Result<EventSourceRequestDiagnosticResultVO>` | `PERMISSION_MODEL_AGENT_CONFIG_VIEW` |
| `GET /api/tasks/market-events/cninfo-proxy` | `MarketEventController` | `MarketEventSourceSyncDTO` query object -> `Result<CninfoProxyAnnouncementResponseVO>` | no explicit `requirePermission` |

### Market Intelligence Contracts

| Endpoint | Controller | Binding and response | Permission behavior |
| --- | --- | --- | --- |
| `GET /api/tasks/market-intelligence` | `MarketIntelligenceController` | `MarketIntelligencePageQueryDTO` query object -> `Result<MarketIntelligencePageVO>` | no explicit `requirePermission` |
| `GET /api/tasks/market-intelligence-stats` | `MarketIntelligenceController` | no request body -> `Result<MarketIntelligenceStatsVO>` | no explicit `requirePermission` |

### Audit Compliance Contracts

| Endpoint | Controller | Binding and response | Permission behavior |
| --- | --- | --- | --- |
| `GET /api/tasks/audit-compliance` | `AuditComplianceController` | `AuditCompliancePageQueryDTO` query object -> `Result<AuditCompliancePageVO>` | `PERMISSION_AUDIT_COMPLIANCE_VIEW` |
| `GET /api/tasks/audit-compliance-stats` | `AuditComplianceController` | no request body -> `Result<AuditComplianceStatsVO>` | `PERMISSION_AUDIT_COMPLIANCE_VIEW` |

### Model, Agent, Workflow And Role Config Contracts

| Endpoint | Controller | Binding and response | Permission behavior |
| --- | --- | --- | --- |
| `GET /api/tasks/model-agent-config` | `ModelAgentConfigController` | no request body -> `Result<ModelAgentConfigCenterVO>` | `PERMISSION_MODEL_AGENT_CONFIG_VIEW` |
| `GET /api/tasks/role-access-configs` | `ModelAgentConfigController` | no request body -> `Result<List<RoleAccessConfigItemVO>>` | no explicit `requirePermission` |
| `POST /api/tasks/model-agent-config/prompt-templates/{templateCode}` | `ModelAgentConfigController` | `@PathVariable("templateCode")`, `@RequestBody PromptTemplateUpdateDTO` -> `Result<String>` | `PERMISSION_MODEL_AGENT_CONFIG_EDIT` |
| `POST /api/tasks/model-agent-config/model-strategies/{strategyCode}` | `ModelAgentConfigController` | `@PathVariable("strategyCode")`, `@RequestBody ModelStrategyUpdateDTO` -> `Result<String>` | `PERMISSION_MODEL_AGENT_CONFIG_EDIT` |
| `POST /api/tasks/model-agent-config/event-auto-trigger-rules/{ruleCode}` | `ModelAgentConfigController` | `@PathVariable("ruleCode")`, `@RequestBody EventAutoTriggerRuleUpdateDTO` -> `Result<String>` | `PERMISSION_MODEL_AGENT_CONFIG_EDIT` |
| `POST /api/tasks/model-agent-config/event-sources/{sourceCode}` | `ModelAgentConfigController` | `@PathVariable("sourceCode")`, `@RequestBody EventSourceConfigUpdateDTO` -> `Result<String>` | `PERMISSION_MODEL_AGENT_CONFIG_EDIT` |
| `POST /api/tasks/model-agent-config/agents/{agentCode}` | `ModelAgentConfigController` | `@PathVariable("agentCode")`, `@RequestBody AgentConfigUpdateDTO` -> `Result<String>` | `PERMISSION_MODEL_AGENT_CONFIG_EDIT` |
| `POST /api/tasks/model-agent-config/workflows/{workflowCode}` | `ModelAgentConfigController` | `@PathVariable("workflowCode")`, `@RequestBody WorkflowConfigUpdateDTO` -> `Result<String>` | `PERMISSION_MODEL_AGENT_CONFIG_EDIT` |
| `POST /api/tasks/model-agent-config/role-access/{roleCode}` | `ModelAgentConfigController` | `@PathVariable("roleCode")`, `@RequestBody RoleAccessConfigUpdateDTO` -> `Result<String>` | `PERMISSION_MODEL_AGENT_CONFIG_EDIT` |

### Research Workbench Contract

| Endpoint | Controller | Binding and response | Permission behavior |
| --- | --- | --- | --- |
| `GET /api/tasks/research-workbench` | `ResearchWorkbenchController` | `ResearchWorkbenchQueryDTO` query object -> `Result<ResearchWorkbenchVO>` | no explicit `requirePermission` |

## 5. Allowed File Scope

Window 2 may modify backend test files under:

- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/`

Preferred implementation shape:

- Extend `TaskControllerMappingTest.java` for exact legacy mapping ownership if that remains the clearest home.
- Add a focused package-private test class, for example `LegacyTaskApiContractFreezeTest.java`, if response envelope, request binding and permission assertions would make `TaskControllerMappingTest.java` too broad.
- Use reflection and source-level assertions only for controller contract facts that are already stable in current code.

Window 2 may add or adjust comments/Javadoc only in these controller files if source-level documentation is needed near the contract surface:

- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/MarketEventController.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/RiskWarningController.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/StrategySignalController.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/ReportController.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/MarketIntelligenceController.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/AuditComplianceController.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/ModelAgentConfigController.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/ResearchWorkbenchController.java`

Optional documentation file, only if Window 2 needs a durable contract-map note beyond tests and comments:

- `docs/harness/04-contract-map.md`, limited to documenting the Phase 006 legacy `/api/tasks/*` non-task endpoint freeze. Do not update phase status there.

Required Window 2 handoff file:

- `docs/harness/handoffs/phase-006-implementation.md`

## 6. Forbidden File Scope

Window 2 must not modify:

- executable production logic in any Java file
- `TaskQueryController.java`, except comments only if a no-non-task-regression note is necessary
- service, service implementation, mapper, entity, DTO or VO files
- permission constants or role access semantics
- Kafka producers, consumers, topic constants or message DTOs
- database migration, schema or SQL files
- `quant-ai-platform/quant-ai-engine/**`
- `quant-ui/**`
- `quant-ai-platform/ai-config/**`
- Maven POM files or dependencies
- Docker, deployment, gateway, auth-service, Nacos, Sentinel or service discovery files
- `docs/harness/state/current-state.md`
- `docs/harness/05-transition-lifetime.md`, `docs/harness/06-debt-register.md`, `docs/harness/07-phase-backlog.md` or prior phase handoffs; those belong to Window 4 after review approval

If satisfying Phase 006 appears to require any forbidden file, Window 2 must stop as blocked.

## 7. Must Stay Stable

Stable URL and API rules:

- Every endpoint in the inventory above keeps the same path.
- Every endpoint keeps the same HTTP method.
- Every non-task controller keeps `@RequestMapping("/api/tasks")`.
- No endpoint moves to `/api/reports`, `/api/risks`, `/api/strategy`, `/api/market`, `/api/audit`, `/api/config` or any other new namespace in this phase.
- No new alias endpoint is added for a legacy endpoint.
- No endpoint is deleted or consolidated.
- No request binding changes from query object to request body or from request body to query object.
- Existing `@PathVariable` and `@RequestParam` names stay stable.
- Existing `@RequestBody` presence and default required behavior stay stable.
- Every endpoint keeps the `com.quant.common.core.model.Result<T>` response envelope.
- Existing response generic types stay stable.
- Existing explicit `roleAccessConfigService.requirePermission(...)` calls stay stable.
- Existing absence of explicit permission checks stays stable in this phase because adding a check would be a behavior change.

Stable behavior rules:

- No pagination, sorting, filtering, null, empty or error behavior changes.
- No cache key, Redis TTL, Kafka, database write, audit or config mutation behavior changes.
- No Sentinel resource changes.
- No frontend-visible field shape changes.
- No workbench, fallback or display hydration behavior changes.

## 8. Allowed New Class / Method Types

Allowed in tests:

- A new package-private JUnit test class under `src/test/java/com/quant/aiorchestrationservice/`.
- New JUnit test methods in `TaskControllerMappingTest.java`.
- Private test helper methods that inspect controller annotations, return types, method parameters and source text.
- Private immutable records or local value objects used only by the test class to represent expected endpoint contracts.
- Test allowlists for the exact endpoint inventory above.

Allowed in production:

- Comments or Javadoc-style notes only in the allowed controller files.

Allowed in documentation:

- A concise Phase 006 section in `docs/harness/04-contract-map.md` if needed to record the approved legacy inventory.

Not allowed:

- New production classes.
- New production methods.
- New production annotations that affect runtime behavior.
- New runtime contract marker objects.
- New dependencies or plugins.

## 9. Forbidden Helpers / Adapters / Fallbacks / Bridges

Do not add:

- Any production `*Helper`, `*Adapter`, `*Bridge`, `*Facade`, `*Fallback`, `*Resolver`, `*Router` or `*Mapper`.
- A compatibility controller that duplicates existing legacy endpoints under new paths.
- A route migration layer.
- A backend URL alias.
- A source-of-truth resolver for report, risk, strategy, market, audit, config or workbench semantics.
- A generic endpoint registry used by production code.
- A new fallback source, fallback metadata field, fallback precedence rule or display hydration rule.
- A frontend bridge or API alias.
- A Python fallback bridge.
- A shared production contract checker.

Test-local helper methods are allowed only when they are private to test code and cannot affect runtime behavior.

## 10. Acceptance Conditions

Phase 006 is acceptable only if all conditions hold:

- The non-task legacy `/api/tasks/*` endpoint inventory above is documented in source tests and, if needed, `04-contract-map.md`.
- Focused backend tests fail if any inventoried endpoint path, HTTP method or controller owner drifts.
- Focused backend tests fail if inventoried endpoints stop returning `Result<T>` envelopes or change their declared response generic type.
- Focused backend tests fail if inventoried `@PathVariable`, `@RequestParam` or `@RequestBody` bindings drift where they are part of the current contract.
- Focused backend tests or source assertions fail if current explicit permission calls are removed, changed or added contrary to the inventory.
- Focused backend tests or source assertions fail if a new non-task legacy endpoint appears under `/api/tasks/*` without updating the Phase 006 contract inventory.
- `TaskQueryController` remains task-only and does not regain market, risk, strategy, report, market-intelligence, audit, config or workbench endpoint methods.
- No new domain URL aliases are introduced.
- No endpoint move, rename, deletion or consolidation occurs.
- No frontend, Python, DTO, VO, entity, mapper, database schema, Kafka, `ai-config`, dependency or build-config change occurs.
- No executable Java production behavior changes occur.
- Workbench remains display-only aggregation.
- Market intelligence remains a display/read-model surface and does not replace market event authority.
- Existing command permission behavior remains stable.
- `mvn -q test` passes from `quant-ai-platform/quant-services`.

## 11. Required Verification Commands

Window 2 must run from `D:\projects\bussiness\quant-ai-platform\quant-services`:

```powershell
mvn -q test
```

Window 2 must also run or record these source checks from `D:\projects\bussiness`:

```powershell
rg -n "@RequestMapping|@GetMapping|@PostMapping|@PutMapping|@DeleteMapping" quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller
```

Expected result: legacy non-task endpoints remain under the approved controller classes and keep `/api/tasks` base mappings.

```powershell
rg -n "requirePermission" quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller
```

Expected result: permission calls match the Phase 006 inventory. Any added, removed or changed permission is a behavior change unless explicitly approved in a later phase.

```powershell
rg -n "/api/(reports|risk|risks|strategy|strategies|market|markets|audit|config|workbench)" quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller
```

Expected result: no new domain URL namespace appears in controller mappings for this phase. If this command matches comments only, Window 2 must say so in the implementation handoff.

```powershell
git diff --name-only
```

Expected result: only allowed Phase 006 test files, optional controller comments, optional `docs/harness/04-contract-map.md`, and `docs/harness/handoffs/phase-006-implementation.md` should appear as Window 2 changes. Existing unrelated dirty files must not be reverted or bundled into the Phase 006 implementation claim.

Frontend, Python and ai-engine verification commands are not required because those areas are forbidden in this phase.

## 12. Blocker Stop Rules

Window 2 must stop and write a blocker in `docs/harness/handoffs/phase-006-implementation.md` if any of these become necessary:

- Changing any URL path, HTTP method, controller base path or endpoint owner.
- Adding any new URL alias or moving a legacy endpoint to a domain namespace.
- Changing request binding, response envelope, response generic type or permission behavior.
- Changing executable production logic to make a contract test pass.
- Changing DTO, VO, entity, mapper, database schema, Kafka, Python, frontend, `ai-config`, dependency or build config.
- Adding a helper, adapter, bridge, facade, fallback, resolver, router, mapper or compatibility controller in production code.
- Treating `ai-orchestration-service` or legacy `/api/tasks/*` paths as final architecture.
- Reclassifying workbench, market intelligence, stats or fallback/provenance metadata as authority.
- Current code already violates the Phase 006 inventory and cannot be documented as stable without changing behavior.
- Tests would require brittle assertions against unrelated formatting instead of real contract facts.

When blocked, Window 2 must:

1. Stop edits at the smallest reversible point.
2. Record the exact file, method and contract that forced the blocker.
3. State which acceptance condition cannot be met.
4. State the smallest breaking, behavior or policy change that would be required.
5. Ask for human decision instead of expanding the phase.

## Window 2 Shape

Use one backend-focused Window 2 implementation window.

Do not partition into frontend or Python implementers. Do not start Phase 005. Do not decide service extraction or permanent modular-monolith policy. Do not proceed until the user approves this Phase 006 architect handoff.

## Human Approval Request

Please approve this Phase 006 architect handoff before Window 2 starts.

Default approval meaning:

- No breaking changes.
- Keep all URL paths stable.
- No business behavior change.
- No new feature work.
- Backend implementation may add focused contract tests, source-level contract comments and optional `04-contract-map.md` documentation only inside the file boundaries above.
