# Phase 013 Architect Handoff

## Status

Window: Window 1 - Phase Architect.

Phase: Phase 013 - Auth/Gateway Permission Authority Boundary.

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
- `docs/harness/handoffs/steering-decision-phase-013.md`

Additional current-state and durable boundary inputs:

- `docs/harness/12-transition-host-exit-criteria.md`
- `docs/harness/13-report-boundary-readiness.md`
- `docs/harness/14-market-data-ingest-boundary-readiness.md`
- `docs/harness/15-risk-strategy-projection-boundary-readiness.md`
- `docs/harness/16-config-store-decision-boundary.md`
- `docs/harness/handoffs/phase-012-final.md`

Read-only planning inspection:

- `quant-ai-platform/ai-config/role-access-configs.json`
- `quant-ai-platform/quant-services/quant-common/quant-common-security/src/main/java/com/quant/common/security/SecurityConstants.java`
- `quant-ai-platform/quant-services/quant-common/quant-common-security/src/main/java/com/quant/common/security/UserContextFilter.java`
- `quant-ai-platform/quant-services/quant-common/quant-common-security/src/main/java/com/quant/common/security/UserContext.java`
- `quant-ai-platform/quant-services/quant-common/quant-common-security/src/main/java/com/quant/common/security/SecurityUtils.java`
- `quant-ai-platform/quant-services/quant-common/quant-common-security/src/main/java/com/quant/common/security/UserRoleEnum.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/TaskQueryController.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/ReportController.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/MarketEventController.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/MarketIntelligenceController.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/RiskWarningController.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/StrategySignalController.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/AuditComplianceController.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/ModelAgentConfigController.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/ResearchWorkbenchController.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/RoleAccessConfigService.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/RoleAccessConfigServiceImpl.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/ModelAgentConfigDashboardQueryServiceImpl.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/ConfigChangeAuditServiceImpl.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/EventAutoTaskDispatchServiceImpl.java`
- `quant-ai-platform/quant-services/quant-business/research-task-service/src/main/java/com/quant/task/controller/ResearchTaskController.java`
- `quant-ai-platform/quant-services/quant-business/research-task-service/src/main/java/com/quant/task/service/TaskRoleAccessService.java`
- `quant-ai-platform/quant-services/quant-business/research-task-service/src/main/java/com/quant/task/service/impl/TaskRoleAccessServiceImpl.java`
- `quant-ui/src/api/task.ts`
- `quant-ui/src/router/index.ts`
- `quant-ui/src/utils/auth.ts`
- `quant-ui/src/utils/requestHeaders.ts`
- `quant-ui/src/utils/request.ts`
- `quant-ui/src/utils/roleAccess.ts`
- `quant-ui/src/utils/taskActionAccess.ts`
- frontend route/menu/action consumers discovered by `rg`
- existing Phase 006 backend contract inventory and Phase 007 frontend authority guard references

Phase 013 is not Phase 001, so the Phase 001 special Java reading list is not the controlling implementation scope for this handoff.

## 1. Phase Goal

Produce a docs-only permission authority boundary artifact:

- Required durable artifact: `docs/harness/17-auth-gateway-permission-boundary.md`.
- Clarify current permission belongs, authority, contract and behavior boundaries before any gateway/auth/JWT, route migration, service extraction, config-store migration or production security work is considered.
- Inventory current permission inputs and consumers:
  - `role-access-configs.json`
  - request headers `X-User-Id` and `X-User-Role`
  - `UserContextFilter`, `UserContext`, `SecurityUtils.currentUserRole()` and `SecurityUtils.currentUserId()`
  - `RoleAccessConfigService` in `ai-orchestration-service`
  - `TaskRoleAccessService` in `research-task-service`
  - frontend `auth.ts`, `requestHeaders.ts`, `roleAccess.ts`, `taskActionAccess.ts`, router meta and menu/action gating
  - existing backend `requirePermission` call sites and intentional no-explicit-permission read surfaces
- Record the conservative Phase 013 decision: header-based demo auth and `role-access-configs.json` remain the current transition permission mechanism for the next governance horizon. This is not production auth architecture.
- Record gateway/auth/JWT/auth-service/user-service/role-store options only as deferred future targets requiring later Window 0 selection and explicit human approval.
- Preserve Phase 005 modular-monolith horizon policy, Phase 006 legacy `/api/tasks/*` contract freeze, Phase 007 frontend authority guardrails, Phase 008 transition-host inventory, Phase 009 report readiness gates, Phase 010 market/data-ingest readiness gates, Phase 011 risk/strategy readiness gates and Phase 012 config-store boundary.

Window 2 must evaluate every statement in this order:

```text
belongs -> authority -> contract -> behavior
```

This phase is docs-only by default. It must not implement gateway/auth/JWT, change permission behavior, mutate config, add tests, add static guards or change business code.

## 2. Belongs

Current belongs baseline:

- `quant-common-security` currently hosts the request-context plumbing: `UserContextFilter`, `UserContext`, `SecurityConstants`, `SecurityUtils` and `UserRoleEnum`.
- `UserContextFilter` reads `X-User-Id` and `X-User-Role`; when absent, it defaults to `guest` and `USER`.
- `UserContext` is a request-scoped ThreadLocal context. It is a runtime carrier, not identity authority.
- `SecurityUtils.currentUserRole()` and `SecurityUtils.currentUserId()` read the current request context. They are permission inputs, not production auth.
- `ai-orchestration-service` is the transition host for current role-access config read/update APIs and most permission checks through `RoleAccessConfigService`.
- `research-task-service` is the formal host for task creation. It reads `role-access-configs.json` through `TaskRoleAccessService` only to enforce `POST /api/research/tasks`; it is not the role config owner.
- `quant-ui` is a consumer/display/UI gating host. It stores the selected demo role locally, sends request headers, caches role-access config for menu/button visibility and route guards, and must not become backend permission authority.
- `role-access-configs.json` remains the current role/menu/permission config input under the JSON transition-store policy from Phase 012.
- There is no implemented gateway, auth-service, user-service, JWT/session service, OAuth/SSO flow, production role DB or route proxy.

In-scope permission surfaces:

- Request header input: `X-User-Id`, `X-User-Role`.
- Default request context: `guest`, `USER`.
- Role config file: `quant-ai-platform/ai-config/role-access-configs.json`.
- Backend permission constants:
  - `TASK_VIEW`
  - `TASK_CREATE`
  - `TASK_RETRY`
  - `TASK_CANCEL`
  - `AUDIT_COMPLIANCE_VIEW`
  - `REPORT_REVIEW`
  - `MODEL_AGENT_CONFIG_VIEW`
  - `MODEL_AGENT_CONFIG_EDIT`
- Backend explicit permission checks in:
  - task create
  - task retry
  - task cancel
  - report review
  - strategy create/update
  - strategy status update
  - market create/import/mock/source sync
  - market source preview/diagnose
  - audit compliance read endpoints
  - model-agent config center read
  - model-agent config update commands, including role-access update
- Intentional no-explicit-permission read surfaces under the current contract inventory.
- Frontend route/menu/action gating and header construction.
- Event auto task dispatch forwarding current request role context, with fallback to `ADMIN`, as current transition behavior only.
- Config audit and ingest history operator identity/role recording as audit metadata, not auth authority.

Context-only dependencies:

- Report, market, risk, strategy, audit and config readiness artifacts that list auth/gateway as a future blocker.
- Phase 006 backend contract tests guarding current permission behavior.
- Phase 007 frontend authority guard that keeps frontend display/provenance surfaces out of command authority.
- Phase 012 config-store decision boundary, because role access config remains JSON-backed.
- Header-based demo role selection in frontend layout and local storage.

Explicitly excluded:

- gateway implementation
- auth-service or user-service creation
- JWT/session/login/OAuth/SSO implementation
- production identity provider, role DB or role-store migration
- route proxy, gateway route migration or endpoint aliasing
- permission behavior widening or narrowing
- adding explicit permission checks to read endpoints that currently have none
- removing existing permission checks
- changing role mappings, permission keys, config schema or frontend role defaults
- config mutation, including edits to `role-access-configs.json`
- Java, Python, frontend, database, Kafka, Redis, deployment, dependency or build changes
- new product feature or new agent work

## 3. Authority

Current permission authority and input inventory:

| Surface | Current meaning | Current classification |
| --- | --- | --- |
| `role-access-configs.json` | role/menu/permission config input | JSON transition store; current permission config input |
| `X-User-Id` | request user id header | demo/runtime request input, not production identity |
| `X-User-Role` | request role header | demo/runtime request input, not production role authority |
| `UserContext` | request-scoped user id and role carrier | runtime context, not SoT |
| `SecurityUtils.currentUserRole()` | current request role reader | permission input reader |
| `RoleAccessConfigService` | `ai-orchestration-service` permission checker and role-access config mutator | transition host service |
| `TaskRoleAccessService` | `research-task-service` task-create permission checker | reader/checker only |
| frontend `auth.ts` | local selected demo user and role normalization | UI/runtime input only |
| frontend `requestHeaders.ts` | request header construction | runtime transport only |
| frontend `roleAccess.ts` | local role-access cache and UI/menu permission helpers | consumer/display/UI gating only |
| frontend `taskActionAccess.ts` | command button visibility helpers | UI affordance only |
| router/menu meta | navigation visibility/redirect gating | UI affordance only |

Current role mapping facts:

- Backend permission services accept the current request role directly and also map coarse access roles:
  - `ADMIN` maps to `ADMIN`.
  - `REVIEWER` maps to `COMPLIANCE_AUDITOR`.
  - `USER` maps to `RESEARCHER`, `PM` and `RISK_MANAGER`.
- Business roles in `role-access-configs.json` currently include:
  - `RESEARCHER`
  - `PM`
  - `RISK_MANAGER`
  - `COMPLIANCE_AUDITOR`
  - `ADMIN`
- Frontend stores a business role as `CurrentUser.userRole` and sends that role in `X-User-Role`.
- Frontend `getAccessRole()` still exposes a coarse `USER` / `REVIEWER` / `ADMIN` mapping as UI utility context. Phase 013 must document it only and must not change header behavior.

Authority rules:

- `role-access-configs.json` remains the current role/menu/permission config input for this transition horizon.
- Request headers are current demo/runtime inputs. They are not production identity or production role authority.
- Backend `requirePermission` calls remain the runtime enforcement points for endpoints that already have explicit checks.
- Frontend route/menu/action gating remains advisory UI affordance. Backend contracts remain the enforcement boundary for checked commands.
- Frontend localStorage, role cache, default role config and display state must not become backend permission truth.
- Config audit rows and ingest history operator fields record current user/role metadata; they do not create permission authority.
- Event auto task dispatch may forward current request role or fallback role as current behavior, but this is not a production service-to-service auth model.
- Phase 013 may say a future gateway/auth/JWT decision is needed. It must not claim that future target is approved or current authority.

Forbidden authority changes:

- No second current permission SoT may be introduced.
- No frontend cache/default/localStorage may define backend permission truth.
- No request header may be reclassified as production identity.
- No `role-access-configs.json` mutation or schema migration may occur.
- No DB/Nacos/JWT/auth-service/user-service/gateway role store may be documented as current runtime authority.
- No read model, dashboard, audit row, frontend route guard or workbench display may become permission command authority.
- No documentation may claim header-based demo auth, JSON role config, legacy `/api/tasks/*`, `ai-orchestration-service` or the modular monolith is final architecture.
- No documentation may close D001, D002, D003, D007 or D008.

## 4. Contract

Stable backend permission contract inventory:

| Surface | Endpoint | Current owner | Stable permission behavior |
| --- | --- | --- | --- |
| task create | `POST /api/research/tasks` | `ResearchTaskController` / `TaskRoleAccessService` | `TASK_CREATE` |
| task retry | `POST /api/tasks/{taskId}/retry` | `TaskQueryController` | `TASK_RETRY` |
| task cancel | `POST /api/tasks/{taskId}/cancel` | `TaskQueryController` | `TASK_CANCEL` |
| report review | `POST /api/tasks/{taskId}/report/review` | `ReportController` | `REPORT_REVIEW` |
| strategy create/update | `POST /api/tasks/strategy-signals` | `StrategySignalController` | `REPORT_REVIEW` |
| strategy status update | `POST /api/tasks/strategy-signals/{signalId}/status` | `StrategySignalController` | `REPORT_REVIEW` |
| market create | `POST /api/tasks/market-events` | `MarketEventController` | `TASK_CREATE` |
| market batch preview | `POST /api/tasks/market-events/batch-import/preview` | `MarketEventController` | `TASK_CREATE` |
| market batch import | `POST /api/tasks/market-events/batch-import` | `MarketEventController` | `TASK_CREATE` |
| market mock ingest | `POST /api/tasks/market-events/mock-ingest` | `MarketEventController` | `TASK_CREATE` |
| market source sync | `POST /api/tasks/market-events/source-sync/{sourceCode}` | `MarketEventController` | `TASK_CREATE` |
| market source preview | `POST /api/tasks/market-events/source-preview/{sourceCode}` | `MarketEventController` | `MODEL_AGENT_CONFIG_VIEW` |
| market source diagnose | `POST /api/tasks/market-events/source-diagnose/{sourceCode}` | `MarketEventController` | `MODEL_AGENT_CONFIG_VIEW` |
| audit compliance list | `GET /api/tasks/audit-compliance` | `AuditComplianceController` | `AUDIT_COMPLIANCE_VIEW` |
| audit compliance stats | `GET /api/tasks/audit-compliance-stats` | `AuditComplianceController` | `AUDIT_COMPLIANCE_VIEW` |
| config dashboard | `GET /api/tasks/model-agent-config` | `ModelAgentConfigController` | `MODEL_AGENT_CONFIG_VIEW` |
| prompt template update | `POST /api/tasks/model-agent-config/prompt-templates/{templateCode}` | `ModelAgentConfigController` | `MODEL_AGENT_CONFIG_EDIT` |
| model strategy update | `POST /api/tasks/model-agent-config/model-strategies/{strategyCode}` | `ModelAgentConfigController` | `MODEL_AGENT_CONFIG_EDIT` |
| event auto-trigger update | `POST /api/tasks/model-agent-config/event-auto-trigger-rules/{ruleCode}` | `ModelAgentConfigController` | `MODEL_AGENT_CONFIG_EDIT` |
| event source update | `POST /api/tasks/model-agent-config/event-sources/{sourceCode}` | `ModelAgentConfigController` | `MODEL_AGENT_CONFIG_EDIT` |
| agent config update | `POST /api/tasks/model-agent-config/agents/{agentCode}` | `ModelAgentConfigController` | `MODEL_AGENT_CONFIG_EDIT` |
| workflow config update | `POST /api/tasks/model-agent-config/workflows/{workflowCode}` | `ModelAgentConfigController` | `MODEL_AGENT_CONFIG_EDIT` |
| role access update | `POST /api/tasks/model-agent-config/role-access/{roleCode}` | `ModelAgentConfigController` | `MODEL_AGENT_CONFIG_EDIT` |

Stable no-explicit-permission read surfaces:

- Task reads: `GET /api/tasks`, `/api/tasks/stats`, `/api/tasks/failed`, `/api/tasks/{taskId}`, `/api/tasks/{taskId}/state`, `/api/tasks/{taskId}/steps`, `/api/tasks/{taskId}/workflow`, `/api/tasks/{taskId}/agents`, `/api/tasks/{taskId}/audits`, `/api/tasks/{taskId}/retries`, `/api/tasks/{taskId}/full`.
- Report reads: `GET /api/tasks/{taskId}/report`, report versions, report compare, report review logs, report center, report center stats and report review stats.
- Risk reads: `GET /api/tasks/risk-warnings`, `GET /api/tasks/risk-warning-stats`.
- Strategy reads: `GET /api/tasks/strategy-signals`, `GET /api/tasks/strategy-signal-stats`, `GET /api/tasks/strategy-signals/{signalId}/factors`.
- Market reads: `GET /api/tasks/market-events`, market event stats, market event detail, ingest history, source configs and CNINFO proxy.
- Market intelligence reads: `GET /api/tasks/market-intelligence`, `GET /api/tasks/market-intelligence-stats`.
- Workbench aggregation: `GET /api/tasks/research-workbench`.
- Role access read model: `GET /api/tasks/role-access-configs`.

Stable request-header contract:

- Frontend axios requests attach `X-User-Id`, `X-User-Role` and `X-Trace-Id`.
- Backend `UserContextFilter` reads `X-User-Id` and `X-User-Role`.
- Missing backend headers default to `guest` and `USER`.
- Header names, defaults and propagation behavior must not change in Phase 013.

Stable frontend routes and permission consumers:

- `/tasks/create` uses `TASK_CREATE` route guard.
- `/audit-compliance` uses `AUDIT_COMPLIANCE_VIEW` route guard.
- `/model-agent-config` uses `MODEL_AGENT_CONFIG_VIEW` route guard.
- `/reports/pending`, `/reports/approved` and `/reports/rejected` use `REPORT_REVIEW` route guard.
- `BasicLayout.vue`, dashboard, task list/detail/report, report center/workbenches, market/risk/strategy centers and config center may hide or show controls using frontend role-access helpers.
- `taskActionAccess.ts` may compute button visibility for retry, cancel, report review and create-task affordances. This stays UI-only.

Stable frontend API functions:

- `createTask`
- `retryTask`
- `cancelTask`
- `reviewTaskReport`
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
- config update functions under `model-agent-config`

Stable contract details:

- URL paths and HTTP methods stay unchanged.
- Controller owners stay unchanged.
- `Result<T>` response envelopes stay unchanged.
- Request bindings stay unchanged.
- Response types stay unchanged.
- Permission behavior stays unchanged, including both explicit checks and intentional absence of explicit checks.
- Frontend route paths, API function names, endpoint strings, call signatures, TypeScript shapes, local role utility behavior and request-header behavior stay unchanged.
- DTO, VO, entity, mapper, database table, Redis key, Kafka topic, Kafka payload, JSON config shape, Python payload and deployment behavior stay unchanged.

## 5. Allowed File Scope

Window 2 may modify only Phase 013 documentation files.

Required output files:

- `docs/harness/17-auth-gateway-permission-boundary.md`
- `docs/harness/handoffs/phase-013-implementation.md`

Optional output file, only if the permission inventory becomes too large for the primary document:

- `docs/harness/handoffs/phase-013-permission-inventory.md`

Allowed read-only inspection areas:

- `docs/harness/**`
- `quant-ai-platform/ai-config/role-access-configs.json`
- security context files under `quant-common-security`
- permission-related Java controller/service/test/resource files under `ai-orchestration-service`
- role-access-related Java files under `research-task-service`
- existing Phase 006 backend contract tests
- frontend API/type/router/layout/view/utility files related to auth, headers, role access, route guards, menus and command buttons under `quant-ui/src`
- `quant-ui/scripts/authority-boundary-check.mjs`

Window 2 must not write to those read-only inspection areas.

## 6. Forbidden File Scope

Window 2 must not modify:

- any Java production or test file under `quant-ai-platform/quant-services/**`
- any Python file under `quant-ai-platform/quant-ai-engine/**`
- any frontend file under `quant-ui/**`
- any file under `quant-ai-platform/ai-config/**`
- any file under `quant-ai-platform/prompt-templates/**`
- `application-*.yml`, `local.yml`, `local.private.yml` or other runtime settings files
- database migration, schema, SQL, seed, entity, mapper, DTO or VO files
- Redis key constants or cache behavior files
- Kafka topic constants, producers, consumers, message DTOs or listener code
- Maven, npm, Vite, TypeScript, Docker, deployment, gateway, Nacos, Sentinel or service-discovery files
- dependency or lock files
- `docs/harness/state/current-state.md`
- `docs/harness/02-authority-matrix.md`
- `docs/harness/03-host-ownership.md`
- `docs/harness/04-contract-map.md`
- `docs/harness/05-transition-lifetime.md`
- `docs/harness/06-debt-register.md`
- `docs/harness/07-phase-backlog.md`
- `docs/harness/08-eval-checklist.md`
- `docs/harness/09-window-protocol.md`
- `docs/harness/10-steering-state-machine.md`
- `docs/harness/12-transition-host-exit-criteria.md`
- `docs/harness/13-report-boundary-readiness.md`
- `docs/harness/14-market-data-ingest-boundary-readiness.md`
- `docs/harness/15-risk-strategy-projection-boundary-readiness.md`
- `docs/harness/16-config-store-decision-boundary.md`
- prior phase handoffs

Window 4 may later update state, debt, backlog or transition lifetime after Window 3 approval. Window 2 must not do that in Phase 013.

If satisfying Phase 013 appears to require any forbidden file, Window 2 must stop as blocked.

## 7. Must Stay Stable

Stable URL / API / behavior:

- Every endpoint listed in this handoff keeps the same path, HTTP method, controller owner, request binding, response envelope, response type and permission behavior.
- No auth URL, gateway URL, route alias, compatibility endpoint, proxy route or new namespace is added.
- No endpoint is deleted, renamed, consolidated, split or moved.
- No existing explicit `requirePermission` call is removed, moved or changed.
- No explicit `requirePermission` call is added to a read-model endpoint that currently has none.
- No permission key is renamed, added or deleted.
- No role code is renamed, added or deleted.
- No access-role mapping is changed.
- No `X-User-Id`, `X-User-Role` or `X-Trace-Id` header behavior changes.
- No default backend user id or role changes.
- No frontend route, API function, endpoint string, call signature, TypeScript shape, role localStorage key, request header utility, route guard, menu gating or action gating behavior changes.
- No role-access JSON content or schema changes.
- No Java path-resolution behavior, role config read/write behavior, audit append behavior, validation behavior, event auto task dispatch header behavior or task-create permission behavior changes.
- No database table, Redis key, Kafka topic, Kafka payload, Python payload, config file, prompt template, runtime setting, build, dependency or deployment behavior changes.

Stable architecture:

- Header-based demo auth remains a transition mechanism, not production security.
- `role-access-configs.json` remains the current role/menu/permission config input under the Phase 012 JSON transition-store policy.
- `ai-orchestration-service` remains the permission/config transition host for current role-access APIs and current domain command checks, not final auth architecture.
- `research-task-service` remains the formal host for task creation and a role-access reader for task-create permission, not a config or auth owner.
- `quant-ui` remains a UI consumer and request-header source, not permission source of truth.
- Legacy `/api/tasks/*` remains a frozen transition namespace, not final route architecture.
- Phase 013 does not close D001, D002, D003, D007 or D008.
- Phase 013 does not approve gateway/auth/JWT implementation, service extraction, route migration, config-store migration, role-store migration, frontend reshaping, Python behavior change, Kafka/database/Redis changes, permanent modular-monolith architecture or new feature work.

## 8. Allowed New Class / Method Types

No business-code classes or methods are allowed.

Allowed documentation-only structures:

- Markdown sections and tables.
- Permission belongs inventory.
- Permission authority matrix.
- Header-based demo auth boundary table.
- Role-access config inventory.
- Backend `requirePermission` call-site inventory.
- Intentional no-explicit-permission read-surface inventory.
- Frontend header/menu/route/action consumer inventory.
- Role mapping inventory for access roles and business roles.
- Stable URL/API/permission contract table.
- Future auth/gateway readiness gates.
- Deferred-decision lists.
- Stop-rule lists.
- Belongs/authority/contract/behavior acceptance checklist.

Allowed scripts/classes/methods:

- None.

Window 2 must not add Java classes, Java tests, frontend scripts, Python scripts, build steps, runtime code, config files, migration files or static guard scripts in this phase.

## 9. Forbidden Helpers / Adapters / Fallbacks / Bridges

Do not add or approve:

- gateway proxy
- auth-service, user-service, role-service or session service
- JWT, OAuth, SSO, login, refresh-token or session adapter
- any production `*Helper`, `*Adapter`, `*Bridge`, `*Facade`, `*Fallback`, `*Resolver`, `*Router`, `*Mapper`, `*Provider` or compatibility layer
- any frontend auth adapter, API adapter, permission resolver or truth resolver
- any route alias, compatibility endpoint or gateway route bridge
- any role-store bridge, config-store bridge, DB adapter, Nacos adapter or hybrid adapter
- any migration runner, sync job, dual-write path or rollback tool
- any service-to-service auth wrapper or event-auto auth bridge
- any test helper or static guard script
- any Kafka, Redis or database compatibility bridge
- any temporary auth/permission wrapper around current services

Window 2 may document existing filters, services, frontend utilities, role mappings, config files and existing guards as current facts, but must not create new paths or approve them as target architecture.

## 10. Required Auth/Gateway Permission Boundary Artifact Shape

`docs/harness/17-auth-gateway-permission-boundary.md` must include:

- Status and scope.
- Inputs and read-only inspection sources.
- Auth/gateway belongs analysis.
- Current permission authority and input inventory.
- Header-based demo auth boundary.
- Role-access config boundary.
- Backend request context boundary.
- Backend permission service inventory.
- Backend explicit permission call-site inventory.
- Intentional no-explicit-permission read-surface inventory.
- `research-task-service` task-create permission reader inventory.
- Frontend request-header, local role, role cache, route/menu and action-gating inventory.
- Stable URL/API/permission contract table.
- Current guardrails inherited from Phase 005 through Phase 012.
- Next-governance-horizon decision: header-based demo auth remains current transition behavior, while production gateway/auth/JWT remains deferred.
- Future gateway/auth readiness gates and blockers.
- Route migration, service extraction, config-store, role-store and production security dependencies.
- Deferred decisions.
- Stop rules for later phases.

The artifact must explicitly state that it does not implement or approve gateway/auth/JWT, auth-service, user-service, login/session, role DB, route migration, endpoint aliases, permission behavior changes, DTO/VO/entity/schema changes, Redis changes, Kafka changes, frontend changes, Python changes, config mutation, config-store migration, business behavior changes or new feature work.

## 11. Acceptance Conditions

Phase 013 is acceptable only if all conditions hold:

- `docs/harness/17-auth-gateway-permission-boundary.md` exists and is the primary durable auth/gateway permission boundary artifact.
- `docs/harness/handoffs/phase-013-implementation.md` records exact files changed and verification outcomes.
- The artifact covers request headers, backend request context, role-access config, backend permission services, explicit backend permission checks, intentional no-explicit-permission read surfaces, frontend role/header/menu/action consumers and task-create permission behavior.
- The artifact names `role-access-configs.json` as the current role/menu/permission config input.
- The artifact states that `X-User-Id` and `X-User-Role` are demo/runtime inputs, not production identity or production role authority.
- The artifact records the current default request context: `guest` and `USER`.
- The artifact records current role mapping between coarse access roles and business role codes without changing it.
- The artifact preserves every URL, HTTP method, request binding, response envelope, response type, permission key, explicit permission check, intentional no-explicit-permission read surface, frontend route, frontend API function, frontend role utility, localStorage behavior, request-header behavior and TypeScript shape.
- The artifact records that header-based demo auth continues only as a transition mechanism for the next governance horizon.
- The artifact records gateway/auth/JWT/auth-service/user-service/role-store work only as deferred future decisions requiring later Window 0 selection and explicit human approval.
- The artifact preserves Phase 005 through Phase 012 constraints.
- The artifact does not choose or implement gateway/auth/JWT, service extraction, route migration, route aliases, endpoint deletion/rename/consolidation, permission behavior change, role-access config mutation, config-store migration, frontend reshaping, Python behavior change, Kafka/database/Redis changes, permanent modular-monolith architecture or new feature work.
- The artifact defines readiness gates for any later gateway/auth/JWT, production role authority, route migration, service extraction or config/role-store migration phase.
- No Java, Python, frontend, database, Redis, Kafka, `ai-config`, prompt-template, dependency, build-config, deployment, state, debt, backlog, transition lifetime, durable Phase 008/009/010/011/012 artifact or prior handoff file changes occur.
- Any future-phase recommendation is explicitly labeled as deferred and requiring a later Window 0 decision plus human approval.
- `git diff --name-only` shows only allowed Phase 013 documentation files as Window 2 changes, aside from pre-existing unrelated dirty files that are clearly excluded from the Window 2 claim.

## 12. Required Verification Commands

Window 2 must run from `D:\projects\bussiness` before edits:

```powershell
git status --short --untracked-files=all
```

Expected result: record pre-existing dirty/untracked files and exclude them from the Window 2 change claim.

Window 2 must run from `D:\projects\bussiness` after edits:

```powershell
git diff --name-only
```

Expected result: only allowed Phase 013 docs should be part of the Window 2 change claim. Existing unrelated dirty files must not be reverted or bundled into the Phase 013 implementation claim.

Window 2 must run:

```powershell
Test-Path docs/harness/17-auth-gateway-permission-boundary.md
```

Expected result: `True`.

Window 2 must run:

```powershell
rg -n "role-access-configs|X-User-Id|X-User-Role|guest|USER|UserContextFilter|UserContext|SecurityUtils|RoleAccessConfigService|TaskRoleAccessService|requirePermission|PERMISSION_TASK_CREATE|PERMISSION_TASK_RETRY|PERMISSION_TASK_CANCEL|PERMISSION_REPORT_REVIEW|PERMISSION_AUDIT_COMPLIANCE_VIEW|PERMISSION_MODEL_AGENT_CONFIG_VIEW|PERMISSION_MODEL_AGENT_CONFIG_EDIT|auth.ts|requestHeaders.ts|roleAccess.ts|taskActionAccess.ts|header-based demo auth|readiness gate|Phase 005|Phase 006|Phase 007|Phase 008|Phase 009|Phase 010|Phase 011|Phase 012" docs/harness/17-auth-gateway-permission-boundary.md docs/harness/handoffs/phase-013-implementation.md
```

Expected result: the durable artifact and implementation handoff contain the required permission facts, consumers and inherited guardrail references.

Window 2 must run:

```powershell
rg -n "gateway|auth-service|user-service|JWT|session|login|OAuth|SSO|role DB|route migration|route alias|breaking change|permission behavior change|config mutation|config-store migration|service extraction|database schema|Redis|Kafka|frontend reshaping|Python behavior|business code|new feature|permanent modular" docs/harness/17-auth-gateway-permission-boundary.md docs/harness/handoffs/phase-013-implementation.md
```

Expected result: any matches are in out-of-scope, blocker, deferred-decision, prerequisite, future-target or no-change sections, not in completed implementation claims.

Window 2 must run or record these read-only inventory checks from `D:\projects\bussiness`:

```powershell
rg -n "HEADER_USER_ID|HEADER_USER_ROLE|DEFAULT_USER_ID|DEFAULT_USER_ROLE|X-User-Id|X-User-Role|UserContext|currentUserRole|currentUserId|UserRoleEnum" quant-ai-platform/quant-services/quant-common/quant-common-security/src/main/java/com/quant/common/security
```

```powershell
rg -n "RoleAccessConfigService|TaskRoleAccessService|requirePermission|PERMISSION_|currentUserRole|currentUserId|role-access-configs" quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java quant-ai-platform/quant-services/quant-business/research-task-service/src/main/java
```

```powershell
rg -n "@RequestMapping|@GetMapping|@PostMapping|requirePermission|PERMISSION_|/api/tasks|/api/research/tasks" quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller quant-ai-platform/quant-services/quant-business/research-task-service/src/main/java/com/quant/task/controller/ResearchTaskController.java
```

```powershell
rg -n "RoleAccessConfigService|TaskRoleAccessService|/api/tasks|/api/research/tasks|PERMISSION_TASK_CREATE|PERMISSION_TASK_RETRY|PERMISSION_TASK_CANCEL|PERMISSION_REPORT_REVIEW|PERMISSION_AUDIT_COMPLIANCE_VIEW|PERMISSION_MODEL_AGENT_CONFIG_VIEW|PERMISSION_MODEL_AGENT_CONFIG_EDIT" quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/TaskControllerMappingTest.java
```

```powershell
rg -n "X-User-Id|X-User-Role|quant_current_user|quant_role_access_configs|ROLE_ACCESS_UPDATED_EVENT|USER_ROLE|PERMISSION_KEY|MENU_KEY|fetchRoleAccessConfigs|updateRoleAccessConfig|canCreateTasks|canRetryTasks|canCancelTasks|canAccessAuditCompliance|canReviewReports|canManageModelAgentConfig|canEditModelAgentConfig|requiredMenuKey|requiredPermissionKey" quant-ui/src/api/task.ts quant-ui/src/router/index.ts quant-ui/src/layout/BasicLayout.vue quant-ui/src/utils/auth.ts quant-ui/src/utils/requestHeaders.ts quant-ui/src/utils/request.ts quant-ui/src/utils/roleAccess.ts quant-ui/src/utils/taskActionAccess.ts quant-ui/src/views quant-ui/scripts/authority-boundary-check.mjs
```

```powershell
rg -n "roleCode|permissionKeys|menuKeys|TASK_CREATE|TASK_RETRY|TASK_CANCEL|AUDIT_COMPLIANCE_VIEW|REPORT_REVIEW|MODEL_AGENT_CONFIG_VIEW|MODEL_AGENT_CONFIG_EDIT|RESEARCHER|PM|RISK_MANAGER|COMPLIANCE_AUDITOR|ADMIN" quant-ai-platform/ai-config/role-access-configs.json
```

Window 2 must run from `D:\projects\bussiness\quant-ui`:

```powershell
node scripts/authority-boundary-check.mjs
```

Expected result: the existing Phase 007 static guard still passes. If it fails, Window 2 must record the failure as a blocker and must not patch frontend code in this phase.

Maven, npm build and Python runtime verification are not required because Phase 013 forbids Java, frontend, Python and test-code changes. If Window 2 touches any such area, it must stop as blocked instead of validating with builds.

## 13. Blocker Stop Rules

Window 2 must stop and write a blocker in `docs/harness/handoffs/phase-013-implementation.md` if any of these become necessary:

- Changing any URL path, HTTP method, endpoint owner, request binding, response envelope, response type, frontend route, frontend API function, TypeScript shape or permission behavior.
- Adding, removing, widening or narrowing any backend `requirePermission` call.
- Adding a permission check to a read-model surface that currently has no explicit check.
- Changing role codes, permission keys, menu keys, role mappings, header names, default header behavior or local role behavior.
- Mutating `role-access-configs.json` or any other config file.
- Adding gateway/auth/JWT/session/login/OAuth/SSO/auth-service/user-service/role-service code.
- Adding a route alias, compatibility bridge, gateway proxy, frontend API adapter, permission resolver, auth adapter, role-store bridge, config-store bridge, DB adapter, Nacos adapter, service wrapper, migration runner, dual-write path, rollback runner or sync job.
- Moving permission, role-access, task-create auth, config or route responsibility into another service.
- Creating or modifying Java, Python, frontend, database, Redis, Kafka, config, prompt-template, dependency, test, build or deployment files.
- Changing DTO, VO, entity, mapper, schema, topic, payload, Redis key, JSON shape, prompt-template shape, API type or frontend type shapes.
- Reclassifying frontend localStorage, frontend defaults, request headers, route guards, menu/button gating, audit rows or ingest history rows as production permission authority.
- Declaring `ai-orchestration-service`, JSON config files, header-based demo auth, request headers, frontend role cache or legacy `/api/tasks/*` paths final architecture.
- Closing D001, D002, D003, D007 or D008.
- Selecting gateway/auth/JWT implementation, route migration, service extraction, config-store migration, role-store migration, frontend reshaping, Python behavior change, Kafka/database/Redis change or permanent modular-monolith outcome as implemented work.
- Needing code behavior changes to make the auth/gateway permission boundary artifact true.
- Finding that permission authority cannot be described without changing the approved Phase 013 scope.
- Needing human approval for breaking changes, permission behavior change, role config mutation, gateway/auth implementation, service extraction, route migration, config-store migration or new product features.
- The existing `node scripts/authority-boundary-check.mjs` guard fails and cannot be treated as an environment-only issue.

When blocked, Window 2 must:

1. Stop edits at the smallest reversible point.
2. Record the exact file, route, permission key, role mapping, consumer, dependency or decision point that forced the blocker.
3. State which acceptance condition cannot be met.
4. State the smallest future phase or human decision required to unblock.
5. Ask for human decision instead of expanding scope.

## Window 2 Shape

Use one docs-only architecture/governance Window 2 implementation window.

Do not partition into backend, frontend or Python implementers. Do not start gateway/auth/JWT work, service extraction, route migration, config migration, role-store migration, test implementation, frontend guard edits, Python edits, Java edits, config edits, deployment edits or product feature work. Do not proceed until the user approves this Phase 013 architect handoff.

## Human Approval Request

Please approve this Phase 013 architect handoff before Window 2 starts.

Default approval meaning:

- No breaking changes.
- Keep all URL paths and frontend routes stable.
- No permission behavior change.
- No business behavior change.
- No new feature work.
- No gateway/auth/JWT implementation.
- No config mutation.
- Window 2 may perform docs-only auth/gateway permission boundary work inside the allowed file boundaries above.
