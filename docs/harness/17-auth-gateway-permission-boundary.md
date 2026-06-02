# Auth/Gateway Permission Boundary

## Status And Scope

Phase: Phase 013 - Auth/Gateway Permission Authority Boundary.

This is a docs-only governance artifact. It records the current permission boundary before any later gateway/auth/JWT, auth-service, user-service, session, login, OAuth, SSO, role DB, route migration, route alias, service extraction, config-store migration, role-store migration, frontend reshaping, Python behavior change, Redis change, Kafka change, database schema change, business behavior change or new feature work is considered.

Phase 013 does not implement or approve gateway/auth/JWT, auth-service, user-service, login/session, role DB, route migration, endpoint aliases, permission behavior changes, DTO/VO/entity/schema changes, Redis changes, Kafka changes, frontend changes, Python changes, config mutation, config-store migration, business code changes or new feature work.

Review order remains:

```text
belongs -> authority -> contract -> behavior
```

## Inputs And Read-Only Inspection Sources

Harness inputs:

- `docs/harness/00-project-charter.md`
- `docs/harness/02-authority-matrix.md`
- `docs/harness/03-host-ownership.md`
- `docs/harness/04-contract-map.md`
- `docs/harness/05-transition-lifetime.md`
- `docs/harness/08-eval-checklist.md`
- `docs/harness/09-window-protocol.md`
- `docs/harness/state/current-state.md`
- `docs/harness/handoffs/steering-decision-phase-013.md`
- `docs/harness/handoffs/phase-013-architect.md`
- Phase 005 through Phase 012 durable guardrail artifacts and handoffs.

Read-only code/config inspection sources:

- `quant-ai-platform/ai-config/role-access-configs.json`
- `quant-ai-platform/quant-services/quant-common/quant-common-security/src/main/java/com/quant/common/security/SecurityConstants.java`
- `quant-ai-platform/quant-services/quant-common/quant-common-security/src/main/java/com/quant/common/security/UserContextFilter.java`
- `quant-ai-platform/quant-services/quant-common/quant-common-security/src/main/java/com/quant/common/security/UserContext.java`
- `quant-ai-platform/quant-services/quant-common/quant-common-security/src/main/java/com/quant/common/security/SecurityUtils.java`
- `quant-ai-platform/quant-services/quant-common/quant-common-security/src/main/java/com/quant/common/security/UserRoleEnum.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/**`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/RoleAccessConfigService.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/RoleAccessConfigServiceImpl.java`
- `quant-ai-platform/quant-services/quant-business/research-task-service/src/main/java/com/quant/task/controller/ResearchTaskController.java`
- `quant-ai-platform/quant-services/quant-business/research-task-service/src/main/java/com/quant/task/service/TaskRoleAccessService.java`
- `quant-ai-platform/quant-services/quant-business/research-task-service/src/main/java/com/quant/task/service/impl/TaskRoleAccessServiceImpl.java`
- Phase 006 backend contract tests: `LegacyTaskApiContractFreezeTest.java` and `TaskControllerMappingTest.java`
- `quant-ui/src/api/task.ts`
- `quant-ui/src/router/index.ts`
- `quant-ui/src/layout/BasicLayout.vue`
- `quant-ui/src/utils/auth.ts`
- `quant-ui/src/utils/requestHeaders.ts`
- `quant-ui/src/utils/request.ts`
- `quant-ui/src/utils/roleAccess.ts`
- `quant-ui/src/utils/taskActionAccess.ts`
- `quant-ui/src/views/**`
- `quant-ui/scripts/authority-boundary-check.mjs`

## Auth/Gateway Belongs Analysis

Current belongs facts:

| Area | Current host | Boundary |
| --- | --- | --- |
| Request context plumbing | `quant-common-security` | `UserContextFilter`, `UserContext`, `SecurityConstants`, `SecurityUtils` and `UserRoleEnum` carry request user and role. They are not production identity authority. |
| Role/menu/permission config input | `quant-ai-platform/ai-config/role-access-configs.json` | Current JSON transition store under the Phase 012 config-store decision. It is not final config or role-store architecture. |
| Role-access config API and most command checks | `ai-orchestration-service` | Transition host for current role-access config read/update APIs and `RoleAccessConfigService` permission checks. It is not final auth architecture. |
| Task creation permission check | `research-task-service` | Formal host for `POST /api/research/tasks`; reads role config through `TaskRoleAccessService` for `TASK_CREATE` only. It is not role config owner. |
| UI role selection and gating | `quant-ui` | UI/runtime header source, local role cache, route/menu/action affordance. It must not become backend permission source of truth. |
| Gateway/auth/JWT/session/login | Not implemented | Future production auth target only, requiring later Window 0 selection and explicit human approval. |

There is no implemented gateway, auth-service, user-service, role-service, production role DB, JWT/session service, OAuth/SSO flow, login flow or route proxy.

## Current Permission Authority And Input Inventory

| Surface | Current meaning | Current classification |
| --- | --- | --- |
| `role-access-configs.json` | Role/menu/permission config input | JSON transition store; current permission config input |
| `X-User-Id` | Request user id header | Demo/runtime request input, not production identity |
| `X-User-Role` | Request role header | Demo/runtime request input, not production role authority |
| `X-Trace-Id` | Request trace header from frontend | Runtime tracing input, not permission authority |
| `UserContext` | Request-scoped user id and role carrier | Runtime context, not SoT |
| `UserContextFilter` | Reads request headers and installs `UserContext` | Request context plumbing, not production auth |
| `SecurityUtils.currentUserId()` | Reads current request user id | Runtime metadata input |
| `SecurityUtils.currentUserRole()` | Reads current request role | Permission input reader |
| `RoleAccessConfigService` | Permission checker and role-access config mutator in `ai-orchestration-service` | Transition host service |
| `TaskRoleAccessService` | Task-create permission checker in `research-task-service` | Reader/checker for task create only |
| Frontend `auth.ts` | Local selected demo user and role normalization | UI/runtime input only |
| Frontend `requestHeaders.ts` | Header construction for `X-User-Id`, `X-User-Role` and `X-Trace-Id` | Runtime transport only |
| Frontend `roleAccess.ts` | Role-access cache and menu/permission helpers | UI gating/display only |
| Frontend `taskActionAccess.ts` | Command button visibility helpers | UI affordance only |
| Router/menu metadata | Navigation visibility/redirect gating | UI affordance only |
| Audit and ingest history operator fields | Record current user/role metadata | Audit metadata, not auth authority |

Authority rules:

- `role-access-configs.json` remains the current role/menu/permission config input for this transition horizon.
- `X-User-Id` and `X-User-Role` are demo/runtime inputs, not production identity or production role authority.
- Backend `requirePermission` calls remain the runtime enforcement points for endpoints that already have explicit checks.
- Frontend route/menu/action gating remains advisory UI affordance. Backend contracts remain the enforcement boundary for checked commands.
- Frontend localStorage, role cache, default role config and display state must not become backend permission truth.
- Config audit rows and ingest history operator fields record current user/role metadata; they do not create permission authority.
- Future gateway/auth/JWT work is deferred and not approved by this artifact.

## Header-Based Demo Auth Boundary

Current request-header facts:

| Fact | Current value |
| --- | --- |
| User id header | `X-User-Id` |
| User role header | `X-User-Role` |
| Trace header | `X-Trace-Id` |
| Missing backend user id default | `guest` |
| Missing backend role default | `USER` |
| Frontend local user storage key | `quant_current_user` |

`SecurityConstants` defines `HEADER_USER_ID`, `HEADER_USER_ROLE`, `DEFAULT_USER_ID` and `DEFAULT_USER_ROLE`. `UserContextFilter` reads the headers, defaults missing values to `guest` and `USER`, sets `UserContext`, and clears it after request processing.

`quant-ui/src/utils/request.ts` uses `buildRequestHeaders()` from `requestHeaders.ts` on axios requests. The frontend currently sends `X-User-Id`, `X-User-Role` and `X-Trace-Id` from the selected local demo user.

Boundary:

- Header-based demo auth continues as current transition behavior for the next governance horizon.
- Header names, defaults and propagation behavior stay unchanged in Phase 013.
- Request headers are not production identity, production role authority or final auth architecture.
- A later production gateway/auth/JWT decision must define identity source, role source, token/session semantics, service-to-service propagation and compatibility behavior before any implementation.

## Role-Access Config Boundary

Current role config input:

- File: `quant-ai-platform/ai-config/role-access-configs.json`
- Current role codes: `RESEARCHER`, `PM`, `RISK_MANAGER`, `COMPLIANCE_AUDITOR`, `ADMIN`
- Current permission keys observed in config and services:
  - `TASK_VIEW`
  - `TASK_CREATE`
  - `TASK_RETRY`
  - `TASK_CANCEL`
  - `AUDIT_COMPLIANCE_VIEW`
  - `REPORT_REVIEW`
  - `MODEL_AGENT_CONFIG_VIEW`
  - `MODEL_AGENT_CONFIG_EDIT`
- Current menu keys observed in config/frontend:
  - `TASK_LIST`
  - `TASK_CREATE`
  - `MARKET_EVENTS`
  - `MARKET_INTELLIGENCE`
  - `RESEARCH_WORKBENCH`
  - `STRATEGY_SIGNALS`
  - `RISK_WARNINGS`
  - `RESEARCH_REPORTS`
  - `AUDIT_COMPLIANCE`
  - `MODEL_AGENT_CONFIG`
  - `REPORTS_PENDING`
  - `REPORTS_APPROVED`
  - `REPORTS_REJECTED`

Current role-to-permission shape:

| Role code | Current permission keys |
| --- | --- |
| `RESEARCHER` | `TASK_VIEW`, `TASK_CREATE` |
| `PM` | `TASK_VIEW`, `TASK_CREATE` |
| `RISK_MANAGER` | `TASK_VIEW`, `TASK_CREATE`, `AUDIT_COMPLIANCE_VIEW` |
| `COMPLIANCE_AUDITOR` | `TASK_VIEW`, `TASK_CREATE`, `AUDIT_COMPLIANCE_VIEW`, `REPORT_REVIEW`, `MODEL_AGENT_CONFIG_VIEW` |
| `ADMIN` | `TASK_VIEW`, `TASK_CREATE`, `TASK_RETRY`, `TASK_CANCEL`, `AUDIT_COMPLIANCE_VIEW`, `REPORT_REVIEW`, `MODEL_AGENT_CONFIG_VIEW`, `MODEL_AGENT_CONFIG_EDIT` |

Current coarse access-role mapping:

| Coarse role | Mapped business role candidates |
| --- | --- |
| `USER` | `USER`, `RESEARCHER`, `PM`, `RISK_MANAGER` |
| `REVIEWER` | `REVIEWER`, `COMPLIANCE_AUDITOR` |
| `ADMIN` | `ADMIN` |

Frontend `getAccessRole()` maps business roles to coarse UI context as follows:

| Business role | Frontend coarse access role |
| --- | --- |
| `RESEARCHER` | `USER` |
| `PM` | `USER` |
| `RISK_MANAGER` | `USER` |
| `COMPLIANCE_AUDITOR` | `REVIEWER` |
| `ADMIN` | `ADMIN` |

Boundary:

- Phase 013 does not mutate `role-access-configs.json`.
- Phase 013 does not add, remove or rename role codes, permission keys or menu keys.
- Phase 013 does not change coarse access-role mapping.
- `role-access-configs.json` remains a current transition config input under Phase 012; it is not final role-store architecture.

## Backend Request Context Boundary

`UserContext` is request-scoped ThreadLocal context. `SecurityUtils.currentUserId()` and `SecurityUtils.currentUserRole()` read it for current runtime behavior.

Observed runtime metadata consumers:

- `RoleAccessConfigServiceImpl` and `TaskRoleAccessServiceImpl` use `SecurityUtils.currentUserRole()` for permission checks.
- `ConfigChangeAuditServiceImpl` records `operatorId` and `operatorRole` from `SecurityUtils`.
- `MarketEventIngestHistoryServiceImpl` records operator metadata from `SecurityUtils`.
- `MarketEventServiceImpl` records created-by metadata from `SecurityUtils.currentUserId()`.
- `TaskReportServiceImpl` records reviewer role from `SecurityUtils.currentUserRole()`.
- `EventAutoTaskDispatchServiceImpl` forwards `X-User-Id` and `X-User-Role`, falling back to `system` and `ADMIN` where current behavior does so.

Boundary:

- Request context is current runtime context only.
- Audit, ingest history and reviewer fields are metadata/provenance surfaces, not permission authority.
- Event auto task dispatch header forwarding is current transition behavior only, not production service-to-service auth.

## Backend Permission Service Inventory

| Service | Host | Current responsibility | Boundary |
| --- | --- | --- | --- |
| `RoleAccessConfigService` | `ai-orchestration-service` | Defines `PERMISSION_TASK_VIEW`, `PERMISSION_TASK_CREATE`, `PERMISSION_TASK_RETRY`, `PERMISSION_TASK_CANCEL`, `PERMISSION_AUDIT_COMPLIANCE_VIEW`, `PERMISSION_REPORT_REVIEW`, `PERMISSION_MODEL_AGENT_CONFIG_VIEW`, `PERMISSION_MODEL_AGENT_CONFIG_EDIT`; checks current role; exposes role-access config read/update behavior. | Transition host service, not final auth-service. |
| `RoleAccessConfigServiceImpl` | `ai-orchestration-service` | Reads `role-access-configs.json`, expands coarse access roles, runs `requirePermission()`, records config audit on role-access config updates. | No behavior or schema change in Phase 013. |
| `TaskRoleAccessService` | `research-task-service` | Defines `PERMISSION_TASK_CREATE` and checks task-create permission for `POST /api/research/tasks`. | Reader/checker only; not role config owner. |
| `TaskRoleAccessServiceImpl` | `research-task-service` | Reads `role-access-configs.json`, expands coarse access roles, runs `requirePermission()` for task creation. | Formal task-create host, not auth or config owner. |

## Backend Explicit Permission Call-Site Inventory

| Surface | Endpoint | Owner | Current explicit permission |
| --- | --- | --- | --- |
| Task create | `POST /api/research/tasks` | `ResearchTaskController` / `TaskRoleAccessService` | `PERMISSION_TASK_CREATE` / `TASK_CREATE` |
| Task retry | `POST /api/tasks/{taskId}/retry` | `TaskQueryController` / `RoleAccessConfigService` | `PERMISSION_TASK_RETRY` / `TASK_RETRY` |
| Task cancel | `POST /api/tasks/{taskId}/cancel` | `TaskQueryController` / `RoleAccessConfigService` | `PERMISSION_TASK_CANCEL` / `TASK_CANCEL` |
| Report review | `POST /api/tasks/{taskId}/report/review` | `ReportController` / `RoleAccessConfigService` | `PERMISSION_REPORT_REVIEW` / `REPORT_REVIEW` |
| Strategy create/update | `POST /api/tasks/strategy-signals` | `StrategySignalController` / `RoleAccessConfigService` | `PERMISSION_REPORT_REVIEW` / `REPORT_REVIEW` |
| Strategy status update | `POST /api/tasks/strategy-signals/{signalId}/status` | `StrategySignalController` / `RoleAccessConfigService` | `PERMISSION_REPORT_REVIEW` / `REPORT_REVIEW` |
| Market create | `POST /api/tasks/market-events` | `MarketEventController` / `RoleAccessConfigService` | `PERMISSION_TASK_CREATE` / `TASK_CREATE` |
| Market batch preview | `POST /api/tasks/market-events/batch-import/preview` | `MarketEventController` / `RoleAccessConfigService` | `PERMISSION_TASK_CREATE` / `TASK_CREATE` |
| Market batch import | `POST /api/tasks/market-events/batch-import` | `MarketEventController` / `RoleAccessConfigService` | `PERMISSION_TASK_CREATE` / `TASK_CREATE` |
| Market mock ingest | `POST /api/tasks/market-events/mock-ingest` | `MarketEventController` / `RoleAccessConfigService` | `PERMISSION_TASK_CREATE` / `TASK_CREATE` |
| Market source sync | `POST /api/tasks/market-events/source-sync/{sourceCode}` | `MarketEventController` / `RoleAccessConfigService` | `PERMISSION_TASK_CREATE` / `TASK_CREATE` |
| Market source preview | `POST /api/tasks/market-events/source-preview/{sourceCode}` | `MarketEventController` / `RoleAccessConfigService` | `PERMISSION_MODEL_AGENT_CONFIG_VIEW` / `MODEL_AGENT_CONFIG_VIEW` |
| Market source diagnose | `POST /api/tasks/market-events/source-diagnose/{sourceCode}` | `MarketEventController` / `RoleAccessConfigService` | `PERMISSION_MODEL_AGENT_CONFIG_VIEW` / `MODEL_AGENT_CONFIG_VIEW` |
| Audit compliance list | `GET /api/tasks/audit-compliance` | `AuditComplianceController` / `RoleAccessConfigService` | `PERMISSION_AUDIT_COMPLIANCE_VIEW` / `AUDIT_COMPLIANCE_VIEW` |
| Audit compliance stats | `GET /api/tasks/audit-compliance-stats` | `AuditComplianceController` / `RoleAccessConfigService` | `PERMISSION_AUDIT_COMPLIANCE_VIEW` / `AUDIT_COMPLIANCE_VIEW` |
| Config dashboard | `GET /api/tasks/model-agent-config` | `ModelAgentConfigController` / `RoleAccessConfigService` | `PERMISSION_MODEL_AGENT_CONFIG_VIEW` / `MODEL_AGENT_CONFIG_VIEW` |
| Prompt template update | `POST /api/tasks/model-agent-config/prompt-templates/{templateCode}` | `ModelAgentConfigController` / `RoleAccessConfigService` | `PERMISSION_MODEL_AGENT_CONFIG_EDIT` / `MODEL_AGENT_CONFIG_EDIT` |
| Model strategy update | `POST /api/tasks/model-agent-config/model-strategies/{strategyCode}` | `ModelAgentConfigController` / `RoleAccessConfigService` | `PERMISSION_MODEL_AGENT_CONFIG_EDIT` / `MODEL_AGENT_CONFIG_EDIT` |
| Event auto-trigger update | `POST /api/tasks/model-agent-config/event-auto-trigger-rules/{ruleCode}` | `ModelAgentConfigController` / `RoleAccessConfigService` | `PERMISSION_MODEL_AGENT_CONFIG_EDIT` / `MODEL_AGENT_CONFIG_EDIT` |
| Event source update | `POST /api/tasks/model-agent-config/event-sources/{sourceCode}` | `ModelAgentConfigController` / `RoleAccessConfigService` | `PERMISSION_MODEL_AGENT_CONFIG_EDIT` / `MODEL_AGENT_CONFIG_EDIT` |
| Agent config update | `POST /api/tasks/model-agent-config/agents/{agentCode}` | `ModelAgentConfigController` / `RoleAccessConfigService` | `PERMISSION_MODEL_AGENT_CONFIG_EDIT` / `MODEL_AGENT_CONFIG_EDIT` |
| Workflow config update | `POST /api/tasks/model-agent-config/workflows/{workflowCode}` | `ModelAgentConfigController` / `RoleAccessConfigService` | `PERMISSION_MODEL_AGENT_CONFIG_EDIT` / `MODEL_AGENT_CONFIG_EDIT` |
| Role access update | `POST /api/tasks/model-agent-config/role-access/{roleCode}` | `ModelAgentConfigController` / `RoleAccessConfigService` | `PERMISSION_MODEL_AGENT_CONFIG_EDIT` / `MODEL_AGENT_CONFIG_EDIT` |

Boundary:

- No explicit `requirePermission` call is added, removed, widened, narrowed, moved or renamed by Phase 013.
- Permission behavior for every endpoint above remains unchanged.

## Intentional No-Explicit-Permission Read-Surface Inventory

These surfaces are recorded as current contract behavior. Phase 013 does not add explicit permission checks to them.

Task reads:

- `GET /api/tasks`
- `GET /api/tasks/stats`
- `GET /api/tasks/failed`
- `GET /api/tasks/{taskId}`
- `GET /api/tasks/{taskId}/state`
- `GET /api/tasks/{taskId}/steps`
- `GET /api/tasks/{taskId}/workflow`
- `GET /api/tasks/{taskId}/agents`
- `GET /api/tasks/{taskId}/audits`
- `GET /api/tasks/{taskId}/retries`
- `GET /api/tasks/{taskId}/full`

Report reads:

- `GET /api/tasks/{taskId}/report`
- `GET /api/tasks/{taskId}/report/versions`
- `GET /api/tasks/{taskId}/report/versions/compare`
- `GET /api/tasks/{taskId}/report/versions/{versionNo}`
- `GET /api/tasks/{taskId}/report/review-logs`
- `GET /api/tasks/report-center`
- `GET /api/tasks/report-center-stats`
- `GET /api/tasks/report-review-stats`

Risk, strategy, market, intelligence, workbench and role-access reads:

- `GET /api/tasks/risk-warnings`
- `GET /api/tasks/risk-warning-stats`
- `GET /api/tasks/strategy-signals`
- `GET /api/tasks/strategy-signal-stats`
- `GET /api/tasks/strategy-signals/{signalId}/factors`
- `GET /api/tasks/market-events`
- `GET /api/tasks/market-event-stats`
- `GET /api/tasks/market-events/{eventId}`
- `GET /api/tasks/market-events/ingest-history`
- `GET /api/tasks/market-event-source-configs`
- `GET /api/tasks/market-events/cninfo-proxy`
- `GET /api/tasks/market-intelligence`
- `GET /api/tasks/market-intelligence-stats`
- `GET /api/tasks/research-workbench`
- `GET /api/tasks/role-access-configs`

Boundary:

- The intentional absence of explicit permission checks remains part of the frozen current contract.
- Any later decision to add checks, change read behavior or split these endpoints requires a later Window 0 decision and explicit human approval.

## Research-Task-Service Task-Create Permission Reader Inventory

`research-task-service` remains the formal host for task creation.

Current facts:

- `ResearchTaskController` is mapped at `POST /api/research/tasks`.
- It calls `taskRoleAccessService.requirePermission(TaskRoleAccessService.PERMISSION_TASK_CREATE)`.
- `TaskRoleAccessService.PERMISSION_TASK_CREATE` is `TASK_CREATE`.
- `TaskRoleAccessServiceImpl` reads `role-access-configs.json` through the existing configured path and fallback resolution.
- `TaskRoleAccessServiceImpl` expands coarse access roles with the same current mapping shape: `USER` to `RESEARCHER`, `PM`, `RISK_MANAGER`; `REVIEWER` to `COMPLIANCE_AUDITOR`; `ADMIN` to `ADMIN`.

Boundary:

- `research-task-service` checks task-create permission but does not own role config or production auth.
- Phase 013 does not change task-create URL, request binding, response envelope, permission key, config path behavior or role mapping.

## Frontend Request Header, Local Role, Role Cache, Route/Menu And Action-Gating Inventory

Current frontend facts:

| Frontend area | Current behavior | Boundary |
| --- | --- | --- |
| `auth.ts` | Defines business roles `RESEARCHER`, `PM`, `RISK_MANAGER`, `COMPLIANCE_AUDITOR`, `ADMIN`; stores current demo user under `quant_current_user`; maps business roles to coarse `USER`, `REVIEWER`, `ADMIN` with `getAccessRole()`. | UI/runtime input only. |
| `requestHeaders.ts` | Builds `X-User-Id`, `X-User-Role` and `X-Trace-Id`. | Runtime transport only. |
| `request.ts` | Adds headers to axios requests from current local user. | Transport behavior only. |
| `roleAccess.ts` | Defines `MENU_KEY`, `PERMISSION_KEY`, local default role config, storage key `quant_role_access_configs`, update event `ROLE_ACCESS_UPDATED_EVENT`, `fetchRoleAccessConfigs()`, `hasMenuAccess()`, `hasPermission()` and helper functions. | UI cache and gating only. |
| `taskActionAccess.ts` | Uses `canCreateTasks`, `canRetryTasks`, `canCancelTasks` and `canReviewReports` for button visibility. | UI affordance only. |
| `router/index.ts` | Uses `requiredMenuKey` and `requiredPermissionKey` route metadata and redirects to `/dashboard` when local role cache says no access. | UI navigation affordance only. |
| `BasicLayout.vue` | Uses menu keys and role helper functions to hide/show menu items and create-task entry points. | UI affordance only. |
| `task.ts` role API | Calls `fetchRoleAccessConfigs()` and `updateRoleAccessConfig()`. | API consumer only; backend remains enforcement boundary. |

Current route guard metadata:

| Route | Current route guard metadata |
| --- | --- |
| `/tasks/create` | `requiredMenuKey: TASK_CREATE`, `requiredPermissionKey: TASK_CREATE` |
| `/market-events` | `requiredMenuKey: MARKET_EVENTS` |
| `/audit-compliance` | `requiredMenuKey: AUDIT_COMPLIANCE`, `requiredPermissionKey: AUDIT_COMPLIANCE_VIEW` |
| `/model-agent-config` | `requiredMenuKey: MODEL_AGENT_CONFIG`, `requiredPermissionKey: MODEL_AGENT_CONFIG_VIEW` |
| `/reports/pending` | `requiredMenuKey: REPORTS_PENDING`, `requiredPermissionKey: REPORT_REVIEW` |
| `/reports/approved` | `requiredMenuKey: REPORTS_APPROVED`, `requiredPermissionKey: REPORT_REVIEW` |
| `/reports/rejected` | `requiredMenuKey: REPORTS_REJECTED`, `requiredPermissionKey: REPORT_REVIEW` |

Current frontend command/API consumers that rely on backend enforcement or UI gating include:

- `createTask`
- `retryTask`
- `cancelTask`
- `reviewReport`
- `createMarketEvent`
- `previewBatchImportMarketEvents`
- `batchImportMarketEvents`
- `mockIngestMarketEvents`
- `syncMarketEventSource`
- `previewMarketEventSource`
- `diagnoseMarketEventSource`
- `createStrategySignal`
- `updateStrategySignalStatus`
- `fetchAuditCompliance`
- `fetchAuditComplianceStats`
- `fetchModelAgentConfigCenter`
- `fetchRoleAccessConfigs`
- `updateRoleAccessConfig`

Boundary:

- Frontend route/menu/action checks are not backend permission authority.
- Frontend localStorage, default role config and role cache must not define backend truth.
- Phase 013 does not change frontend routes, API function names, endpoint strings, call signatures, TypeScript shapes, localStorage behavior, request-header behavior, menu gating or action gating.

## Stable URL/API/Permission Contract

Stable contract rules:

- Every endpoint keeps the same path, HTTP method, controller owner, request binding, response envelope, response type and permission behavior.
- `Result<T>` response envelopes remain unchanged.
- Existing explicit permission checks remain exactly where they are.
- Intentional no-explicit-permission read surfaces remain unchanged.
- No gateway URL, auth URL, route alias, compatibility endpoint, proxy route or new namespace is added.
- No permission key, role code, menu key, header name or default value is added, removed or renamed.
- No frontend route, frontend API function, endpoint string, call signature, TypeScript shape, localStorage key, route guard, menu gating or action gating behavior changes.
- No DTO, VO, entity, mapper, database table, Redis key, Kafka topic, Kafka payload, JSON config shape, prompt-template shape, Python payload, runtime setting, build, dependency or deployment behavior changes.

The backend explicit permission table and intentional no-explicit-permission inventory above are the stable Phase 013 permission contract inventory.

## Inherited Guardrails From Phase 005 Through Phase 012

- Phase 005: `ai-orchestration-service` continues as the next-governance-horizon modular monolith transition host, not final architecture.
- Phase 006: legacy non-task `/api/tasks/*` endpoint paths are frozen transitional contracts guarded by backend tests.
- Phase 007: frontend workbench and fallback provenance consumers remain display/audit/UI-only and must not become command authority.
- Phase 008: transition-host responsibilities remain inventoried; service extraction is not approved.
- Phase 009: report boundary readiness remains docs-only; report route migration or report-service extraction is not approved.
- Phase 010: market/data-ingest boundary readiness remains docs-only; market/data-ingest extraction, source adapter redesign or route migration is not approved.
- Phase 011: risk/strategy projection boundary remains docs-only; risk/strategy service extraction, projection split and Kafka redesign are not approved.
- Phase 012: JSON config files and prompt template files remain current runtime transition stores; DB/Nacos/hybrid config-store migration is not approved.

Phase 013 preserves these guardrails and does not close D001, D002, D003, D007 or D008.

## Next-Governance-Horizon Decision

For the next governance horizon:

- Header-based demo auth remains the current transition permission input mechanism.
- `role-access-configs.json` remains the current role/menu/permission config input under the Phase 012 JSON transition-store policy.
- Backend `requirePermission` calls remain current enforcement points for explicitly checked endpoints.
- Frontend role selection, route guards, menu gating and action gating remain UI affordances.
- Production gateway/auth/JWT, auth-service, user-service, role-service, session/login/OAuth/SSO and role DB remain deferred future decisions.

This decision is conservative. It records the current boundary so later phases can reason about auth/gateway blockers, but it does not select or implement production security.

## Future Gateway/Auth Readiness Gates And Blockers

Any later gateway/auth/JWT or production role-authority phase must pass these gates before implementation:

- Select identity authority: gateway, auth-service, user-service, external IdP or another approved source.
- Select role authority: existing JSON transition store, DB role store, external directory, Nacos/config store or another approved source.
- Define token/session behavior, including JWT/session/login/OAuth/SSO compatibility if selected.
- Define service-to-service user/role propagation, including how event auto task dispatch obtains authority.
- Define compatibility with `X-User-Id` and `X-User-Role` demo headers and whether they remain, change, or are retired.
- Define permission key, menu key and role-code migration rules without widening/narrowing existing behavior accidentally.
- Define audit identity semantics for config audit, ingest history, report review and created-by metadata.
- Define frontend route/menu/action gating interaction with backend enforcement.
- Define route migration or gateway proxy plan only after auth authority is selected.
- Define config-store or role-store migration plan only after Phase 012 gates are satisfied and a later Window 0 decision plus human approval selects the target.
- Define backend tests/static guards for any approved behavior change.

Current blockers for gateway/auth, route migration, service extraction and role-store migration:

- Production identity authority is not selected.
- Production role authority is not selected.
- Header-based demo auth is still transition behavior.
- `role-access-configs.json` remains JSON transition config, not final role store.
- Legacy `/api/tasks/*` paths remain frozen under Phase 006.
- `ai-orchestration-service` remains a transition host, not final architecture.
- Frontend UI gating is advisory and not enough to replace backend enforcement.

## Route Migration, Service Extraction, Config-Store, Role-Store And Production Security Dependencies

Later route migration depends on:

- Approved production auth/gateway boundary.
- Compatibility or breaking-change decision for current URLs.
- Updated Phase 006 contract inventory if endpoints move, alias, split, merge or gain new permission behavior.

Later service extraction depends on:

- Auth/gateway decision.
- Route and contract migration decision.
- Service-to-service identity and role propagation.
- Role config ownership decision.
- Domain-specific readiness gates from Phase 008 through Phase 012.

Later config-store or role-store migration depends on:

- Phase 012 schema/versioning, single-writer, audit retention and rollback readiness.
- Explicit target selection by Window 0 and human approval.
- Java/Python/frontend reader compatibility plan.

Later production security depends on:

- Identity and role source selection.
- Token/session/login behavior.
- Gateway or auth-service operational boundary.
- Audit and compliance semantics.
- Backward compatibility or approved breaking-change plan for demo headers.

## Deferred Decisions

The following remain deferred and require later Window 0 selection plus explicit human approval:

- Whether to implement gateway/auth/JWT.
- Whether to create auth-service, user-service, role-service or session service.
- Whether to introduce login, OAuth, SSO, refresh-token or production session flows.
- Whether to migrate role config from `role-access-configs.json` to DB, Nacos, hybrid storage or an external role authority.
- Whether to retire or replace `X-User-Id` and `X-User-Role`.
- Whether to add, remove, widen or narrow backend `requirePermission` calls.
- Whether to add permission checks to currently no-explicit-permission read surfaces.
- Whether to migrate, alias, split, merge, rename or delete endpoints.
- Whether to extract services or make modular-monolith permanence claims.
- Whether to reshape frontend auth, Python behavior, Kafka/database/Redis contracts or deployment architecture.

## Stop Rules For Later Phases

Stop and require a new approved phase if work would require:

- Changing any URL path, HTTP method, endpoint owner, request binding, response envelope, response type, frontend route, frontend API function, TypeScript shape or permission behavior.
- Adding, removing, widening or narrowing any backend `requirePermission` call.
- Adding a permission check to a read-model surface that currently has none.
- Changing role codes, permission keys, menu keys, role mappings, header names, default header behavior or local role behavior.
- Mutating `role-access-configs.json` or any other config file.
- Adding gateway/auth/JWT/session/login/OAuth/SSO/auth-service/user-service/role-service code.
- Adding a route alias, compatibility bridge, gateway proxy, frontend API adapter, permission resolver, auth adapter, role-store bridge, config-store bridge, DB adapter, Nacos adapter, service wrapper, migration runner, dual-write path, rollback runner or sync job.
- Moving permission, role-access, task-create auth, config or route responsibility into another service.
- Creating or modifying Java, Python, frontend, database, Redis, Kafka, config, prompt-template, dependency, test, build or deployment files.
- Reclassifying frontend localStorage, frontend defaults, request headers, route guards, menu/button gating, audit rows or ingest history rows as production permission authority.
- Declaring `ai-orchestration-service`, JSON config files, header-based demo auth, request headers, frontend role cache or legacy `/api/tasks/*` paths final architecture.
- Closing D001, D002, D003, D007 or D008 without a later approved governance phase.
