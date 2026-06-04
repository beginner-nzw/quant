# Phase 014 Architect Handoff

## Status

Window: Window 1 - Phase Architect.

Phase: Phase 014 - Production Auth/Gateway Target Scoping and Identity/Role Authority Selection.

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
- `docs/harness/handoffs/steering-decision-phase-014.md`

Additional current-state, prior handoff and durable boundary inputs:

- `docs/harness/handoffs/steering-decision-phase-013.md`
- `docs/harness/handoffs/phase-013-architect.md`
- `docs/harness/handoffs/phase-013-implementation.md`
- `docs/harness/handoffs/phase-013-review.md`
- `docs/harness/handoffs/phase-013-final.md`
- `docs/harness/12-transition-host-exit-criteria.md`
- `docs/harness/13-report-boundary-readiness.md`
- `docs/harness/14-market-data-ingest-boundary-readiness.md`
- `docs/harness/15-risk-strategy-projection-boundary-readiness.md`
- `docs/harness/16-config-store-decision-boundary.md`
- `docs/harness/17-auth-gateway-permission-boundary.md`

Read-only planning inspection basis inherited from Phase 013:

- `quant-ai-platform/ai-config/role-access-configs.json`
- `quant-ai-platform/quant-services/quant-common/quant-common-security/src/main/java/com/quant/common/security/**`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/**`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/RoleAccessConfigService.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/RoleAccessConfigServiceImpl.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/EventAutoTaskDispatchServiceImpl.java`
- `quant-ai-platform/quant-services/quant-business/research-task-service/src/main/java/com/quant/task/controller/ResearchTaskController.java`
- `quant-ai-platform/quant-services/quant-business/research-task-service/src/main/java/com/quant/task/service/TaskRoleAccessService.java`
- `quant-ai-platform/quant-services/quant-business/research-task-service/src/main/java/com/quant/task/service/impl/TaskRoleAccessServiceImpl.java`
- existing Phase 006 backend contract inventory tests
- `quant-ui/src/api/task.ts`
- `quant-ui/src/router/index.ts`
- `quant-ui/src/layout/BasicLayout.vue`
- `quant-ui/src/utils/auth.ts`
- `quant-ui/src/utils/requestHeaders.ts`
- `quant-ui/src/utils/request.ts`
- `quant-ui/src/utils/roleAccess.ts`
- `quant-ui/src/utils/taskActionAccess.ts`
- `quant-ui/scripts/authority-boundary-check.mjs`

Phase 014 is not Phase 001, so the Phase 001 special Java reading list is not the controlling implementation scope for this handoff.

## 1. Phase Goal

Produce a docs-only production auth/gateway target-scoping artifact:

- Required durable artifact: `docs/harness/18-production-auth-gateway-target-scope.md`.
- Clarify future production identity authority and role authority targets without implementing them.
- Select a governance-only future target direction for later phases:
  - production identity must be supplied by a backend-owned ingress/auth boundary, such as a future gateway/JWT boundary backed by a future auth-service, user-service or external identity provider;
  - production role authority must be backend-owned and must not be frontend localStorage, request headers or UI route/menu gating;
  - `role-access-configs.json` remains the current transition role/menu/permission config input until a later approved role-store or config-store migration phase;
  - `X-User-Id` and `X-User-Role` remain local/demo compatibility inputs only, not production identity or role authority;
  - service-to-service propagation must have explicit actor, role, service-principal and audit semantics before route migration or service extraction.
- Define how current `X-User-Id`, `X-User-Role`, `role-access-configs.json`, frontend UI gating and backend `requirePermission` checks would be preserved, constrained, retired or migrated in later approved phases.
- Preserve Phase 006 route freeze, Phase 012 config-store boundary, Phase 013 permission inventory and all Phase 005 through Phase 011 guardrails.

Window 2 must evaluate every statement in this order:

```text
belongs -> authority -> contract -> behavior
```

This phase is docs-only. It must not implement gateway/auth/JWT, change permission behavior, mutate config, add tests, add static guards or change business code.

## 2. Belongs

Current belongs baseline inherited from Phase 013:

- `quant-common-security` currently hosts request-context plumbing: `SecurityConstants`, `UserContextFilter`, `UserContext`, `SecurityUtils` and `UserRoleEnum`.
- `X-User-Id` and `X-User-Role` are current demo/runtime request inputs. They are not production identity or role authority.
- `role-access-configs.json` is the current role/menu/permission config input under the Phase 012 JSON transition-store policy. It is not final role-store architecture.
- `ai-orchestration-service` is the transition host for current role-access config APIs and most current explicit permission checks through `RoleAccessConfigService`.
- `research-task-service` is the formal host for `POST /api/research/tasks` and reads role config through `TaskRoleAccessService` only for task-create permission.
- `quant-ui` is a UI/runtime header source and route/menu/action gating consumer. It must not become backend permission authority.
- There is no implemented production gateway, auth-service, user-service, role-service, JWT/session service, login/OAuth/SSO flow, production role DB or route proxy.

Future target belongs that Phase 014 may document only:

- A future ingress identity boundary may belong to `quant-gateway` or another explicitly approved auth/gateway component.
- A future identity issuer or validator may belong to a future auth-service, user-service or external IdP only after a later Window 0 decision and human approval.
- A future production role authority may belong to a backend-owned role store, auth/user service or config-store-backed role source only after a later Window 0 decision and human approval.
- A future service-to-service propagation model must belong to the approved gateway/auth/service boundary, not to frontend utilities, ad hoc headers or domain read models.

Context dependencies:

- Report, market, risk, strategy, audit, config and workbench readiness artifacts all list auth/gateway or permission authority as a blocker before extraction, route migration or permanence decisions.
- Phase 006 freezes legacy non-task `/api/tasks/*` paths.
- Phase 007 keeps frontend workbench/fallback consumers out of command authority.
- Phase 012 keeps JSON config and prompt files as current transition stores.
- Phase 013 keeps header-based demo auth as current transition behavior and defers production auth.

Explicitly excluded:

- gateway implementation
- auth-service, user-service, role-service or session service creation
- JWT/session/login/OAuth/SSO implementation
- production role DB, external IdP integration or role-store migration
- route proxy, gateway route migration or endpoint aliasing
- permission behavior widening or narrowing
- adding explicit permission checks to read endpoints that currently have none
- removing existing permission checks
- changing role mappings, permission keys, menu keys, header names, defaults, config schema or frontend role behavior
- mutating `role-access-configs.json` or any other config file
- Java, Python, frontend, database, Kafka, Redis, deployment, dependency, build or test changes
- new product feature or new agent work

## 3. Authority

Current authority facts that must remain stable:

| Surface | Current meaning | Current classification |
| --- | --- | --- |
| `role-access-configs.json` | role/menu/permission config input | JSON transition store; current permission config input |
| `X-User-Id` | request user id header | demo/runtime request input, not production identity |
| `X-User-Role` | request role header | demo/runtime request input, not production role authority |
| `X-Trace-Id` | request trace header | runtime tracing input, not permission authority |
| `UserContext` | request-scoped user id and role carrier | runtime context, not SoT |
| `SecurityUtils.currentUserRole()` | current request role reader | current permission input reader |
| `RoleAccessConfigService` | permission checker and role-access config mutator in `ai-orchestration-service` | transition host service, not final auth service |
| `TaskRoleAccessService` | task-create permission checker in `research-task-service` | reader/checker only, not role config owner |
| frontend `auth.ts` | selected local demo user and role normalization | UI/runtime input only |
| frontend `requestHeaders.ts` | `X-User-Id`, `X-User-Role`, `X-Trace-Id` construction | transport behavior only |
| frontend `roleAccess.ts` | role-access cache and menu/permission helpers | UI gating/display only |
| frontend `taskActionAccess.ts` | command button visibility helpers | UI affordance only |

Phase 014 governance-only target selection rules:

- The artifact must distinguish current runtime authority from future target authority.
- It may select a future identity target direction, but must label it as not implemented and not current runtime authority.
- It may select that production identity should be accepted only through a future gateway/auth boundary, not directly from demo headers.
- It may select that production role authority must be backend-owned, but must not migrate the current role store or mutate `role-access-configs.json`.
- It may state that demo headers should be preserved for local/demo compatibility until a later approved retirement phase, but must not change current header behavior.
- It may define service-to-service propagation requirements, including actor identity, service principal, role claims, trace id and audit metadata, but must not add propagation code or payload changes.
- It may define future audit identity semantics for config audit, ingest history, report review, market created-by metadata and event auto task dispatch, but must not change current audit fields or behavior.

Forbidden authority changes:

- No second current identity or role SoT may be introduced.
- No frontend cache, frontend default, localStorage value or route/menu/action gate may define backend permission truth.
- No request header may be reclassified as production identity or production role authority.
- No `role-access-configs.json` mutation or schema migration may occur.
- No DB, Nacos, JWT, auth-service, user-service, role-service, gateway or external IdP may be documented as current runtime authority.
- No read model, dashboard, audit row, ingest history row, workbench output or fallback provenance may become identity, role or permission authority.
- No documentation may claim header-based demo auth, JSON role config, legacy `/api/tasks/*`, `ai-orchestration-service` or the modular monolith is final architecture.
- No documentation may close D001, D002, D003, D007 or D008.

## 4. Contract

Stable URL / API / permission behavior:

- Every endpoint in the Phase 006 and Phase 013 inventories keeps the same path, HTTP method, controller owner, request binding, response envelope, response type and permission behavior.
- `POST /api/research/tasks` remains hosted by `research-task-service` and keeps `TASK_CREATE` behavior through `TaskRoleAccessService`.
- Task retry and cancel remain `POST /api/tasks/{taskId}/retry` and `POST /api/tasks/{taskId}/cancel` with their current permission behavior.
- Report review remains `POST /api/tasks/{taskId}/report/review` with current `REPORT_REVIEW` behavior.
- Strategy command surfaces, market command/import/source surfaces, audit compliance read surfaces and config update surfaces keep their current explicit permission checks.
- Intentional no-explicit-permission read surfaces from Phase 013 remain unchanged, including task reads, report reads, risk reads, strategy reads, market reads, market intelligence reads, workbench and `GET /api/tasks/role-access-configs`.
- No auth URL, gateway URL, route alias, compatibility endpoint, proxy route or new namespace is added.
- No endpoint is deleted, renamed, consolidated, split or moved.
- No permission key, menu key, role code, header name or default value is added, removed or renamed.
- No existing backend `requirePermission` call is added, removed, widened, narrowed, moved or renamed.
- No explicit permission check is added to a read-model endpoint that currently has none.
- `X-User-Id`, `X-User-Role`, `X-Trace-Id`, `guest`, `USER` and `quant_current_user` behavior remain stable.
- Frontend route metadata, API function names, endpoint strings, call signatures, TypeScript shapes, role localStorage key, request-header utility, menu gating and action gating behavior remain stable.
- DTO, VO, entity, mapper, schema, Kafka topic, Kafka payload, Redis key, JSON config shape, prompt-template shape, Python payload and runtime settings remain stable.

Contract language allowed in the Phase 014 artifact:

- future target contract options
- future migration prerequisites
- future compatibility constraints for demo headers
- future service-to-service propagation requirements
- future route migration prerequisites after auth authority is selected
- future role-store/config-store migration prerequisites after Phase 012 gates are satisfied

Contract language forbidden in the Phase 014 artifact:

- claiming a new current auth endpoint exists
- claiming gateway/JWT/session/login behavior is implemented
- claiming demo headers are retired now
- claiming current read endpoints now require permissions
- claiming route migration, endpoint aliasing or breaking changes are approved
- claiming frontend UI gating is backend enforcement
- claiming `role-access-configs.json` is final role authority

## 5. Allowed File Scope

Window 2 may modify only:

- `docs/harness/18-production-auth-gateway-target-scope.md`
- `docs/harness/handoffs/phase-014-implementation.md`

Window 2 may read, but must not write:

- required harness inputs listed above
- `docs/harness/12-transition-host-exit-criteria.md`
- `docs/harness/13-report-boundary-readiness.md`
- `docs/harness/14-market-data-ingest-boundary-readiness.md`
- `docs/harness/15-risk-strategy-projection-boundary-readiness.md`
- `docs/harness/16-config-store-decision-boundary.md`
- `docs/harness/17-auth-gateway-permission-boundary.md`
- Java, frontend, Python, config and test files needed for read-only inventory commands

Window 4 may later update state, debt, backlog or transition lifetime after Window 3 approval. Window 2 must not do that in Phase 014.

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
- durable Phase 008 through Phase 013 artifacts
- prior phase handoffs

If satisfying Phase 014 appears to require any forbidden file, Window 2 must stop as blocked.

## 7. Must Stay Stable

Stable behavior:

- No runtime behavior changes.
- No business behavior changes.
- No permission behavior changes.
- No frontend behavior changes.
- No Python behavior changes.
- No Kafka, Redis, database, config, prompt-template, build, dependency or deployment behavior changes.

Stable architecture:

- Header-based demo auth remains a transition mechanism, not production security.
- `role-access-configs.json` remains the current role/menu/permission config input under the Phase 012 JSON transition-store policy.
- Backend explicit `requirePermission` calls remain current enforcement points for checked endpoints.
- Intentional no-explicit-permission read surfaces remain current contract behavior.
- `ai-orchestration-service` remains the current transition host for role-access config APIs and most checked domain commands, not final auth architecture.
- `research-task-service` remains the formal host for task creation and a role-access reader for task-create permission, not a config or auth owner.
- `quant-ui` remains a UI consumer and request-header source, not permission source of truth.
- Legacy `/api/tasks/*` remains a frozen transition namespace, not final route architecture.
- Phase 014 does not close D001, D002, D003, D007 or D008.
- Phase 014 does not approve gateway/auth/JWT implementation, service extraction, route migration, config-store migration, role-store migration, frontend reshaping, Python behavior change, Kafka/database/Redis changes, permanent modular-monolith architecture or new feature work.

## 8. Allowed New Class / Method Types

No business-code classes or methods are allowed.

Allowed documentation-only structures:

- Markdown sections and tables.
- Current-fact inventory inherited from Phase 013.
- Identity-authority option matrix.
- Role-authority option matrix.
- Governance-only target selection section.
- Demo-header compatibility and retirement policy section.
- Service-to-service propagation requirement matrix.
- Audit identity semantics section.
- Current contract preservation table.
- Future route migration dependency section.
- Future config-store/role-store dependency section.
- Deferred-decision list.
- Later-phase candidate list.
- Stop-rule list.
- Belongs/authority/contract/behavior acceptance checklist.

Allowed scripts/classes/methods:

- None.

Window 2 must not add Java classes, Java tests, frontend scripts, Python scripts, build steps, runtime code, config files, migration files, static guard scripts, helper classes or adapters in this phase.

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

Window 2 may document future target options and requirements, but those must be labeled as deferred future work requiring later Window 0 selection and human approval.

## 10. Required Production Auth/Gateway Target-Scope Artifact Shape

`docs/harness/18-production-auth-gateway-target-scope.md` must include:

- Status and scope.
- Inputs and read-only inspection sources.
- Current inherited facts from Phase 013:
  - `X-User-Id` and `X-User-Role` are demo/runtime inputs.
  - `role-access-configs.json` is the current role/menu/permission config input.
  - backend explicit `requirePermission` checks are current enforcement points.
  - frontend route/menu/action gating is UI affordance only.
  - no production gateway/auth/JWT implementation exists.
- Production identity authority options and the governance-only selected target direction.
- Production role authority options and the governance-only selected target direction.
- Demo-header compatibility or retirement rules for later phases.
- Service-to-service propagation requirements for task creation, AI callbacks, event auto task dispatch, future extracted services and audit identity.
- Permission key, menu key and role-code compatibility rules.
- Audit identity semantics for config audit, ingest history, report review, market created-by metadata and system-triggered actions.
- Stable URL/API/permission contract rules inherited from Phase 006 and Phase 013.
- Dependencies on Phase 006 route freeze, Phase 012 config-store boundary and Phase 013 permission inventory.
- Dependencies on Phase 008 through Phase 011 domain readiness artifacts for route migration or service extraction.
- Deferred implementation decisions.
- Later candidate phases unlocked or clarified by Phase 014.
- Stop rules for later phases.

The artifact must explicitly state that it does not implement or approve gateway/auth/JWT, auth-service, user-service, role-service, login/session, OAuth, SSO, role DB, external IdP integration, route migration, endpoint aliases, permission behavior changes, DTO/VO/entity/schema changes, Redis changes, Kafka changes, frontend changes, Python changes, config mutation, config-store migration, role-store migration, business behavior changes or new feature work.

## 11. Acceptance Conditions

Phase 014 is acceptable only if all conditions hold:

- `docs/harness/18-production-auth-gateway-target-scope.md` exists and is the primary durable production auth/gateway target-scope artifact.
- `docs/harness/handoffs/phase-014-implementation.md` records exact files changed and verification outcomes.
- The artifact is docs-only and does not claim runtime implementation.
- The artifact restates the current Phase 013 facts for headers, role config, backend permission checks, no-explicit-permission read surfaces, frontend gating and current absence of production auth.
- The artifact selects or clearly scopes a future production identity authority target direction while labeling it as future-only and not current runtime authority.
- The artifact selects or clearly scopes a future production role authority target direction while keeping `role-access-configs.json` as the current transition input.
- The artifact defines service-to-service propagation requirements without changing headers, Kafka payloads, DTOs, frontend types or runtime behavior.
- The artifact defines demo-header compatibility or retirement rules for later phases without changing current `X-User-Id` or `X-User-Role` behavior.
- The artifact preserves every URL, HTTP method, request binding, response envelope, response type, permission key, explicit permission check, intentional no-explicit-permission read surface, frontend route, frontend API function, frontend role utility, localStorage behavior, request-header behavior and TypeScript shape.
- The artifact preserves Phase 005 through Phase 013 constraints.
- The artifact does not choose or implement gateway/auth/JWT, service extraction, route migration, route aliases, endpoint deletion/rename/consolidation, permission behavior change, role-access config mutation, config-store migration, role-store migration, frontend reshaping, Python behavior change, Kafka/database/Redis changes, permanent modular-monolith architecture or new feature work.
- The artifact defines readiness gates for later gateway/auth/JWT, production role authority, route migration, service extraction, config-store migration and role-store migration phases.
- No Java, Python, frontend, database, Redis, Kafka, `ai-config`, prompt-template, dependency, build-config, deployment, state, debt, backlog, transition lifetime, durable Phase 008/009/010/011/012/013 artifact or prior handoff file changes occur.
- Any future-phase recommendation is explicitly labeled as deferred and requiring a later Window 0 decision plus human approval.
- `git status --short --untracked-files=all` after Window 2 shows only allowed Phase 014 documentation files as new Window 2 changes, aside from pre-existing unrelated dirty files clearly excluded from the Window 2 claim.

## 12. Required Verification Commands

Window 2 must run from `D:\projects\bussiness` before edits:

```powershell
git status --short --untracked-files=all
```

Expected result: record pre-existing dirty/untracked files and exclude them from the Window 2 change claim.

Window 2 must run these read-only inventory checks from `D:\projects\bussiness` before or during documentation work:

```powershell
rg -n "HEADER_USER_ID|HEADER_USER_ROLE|DEFAULT_USER_ID|DEFAULT_USER_ROLE|X-User-Id|X-User-Role|X-Trace-Id|UserContext|currentUserRole|currentUserId|UserRoleEnum" quant-ai-platform/quant-services/quant-common/quant-common-security/src/main/java/com/quant/common/security
```

```powershell
rg -n "RoleAccessConfigService|TaskRoleAccessService|requirePermission|PERMISSION_|currentUserRole|currentUserId|role-access-configs|EventAutoTaskDispatchService|system|ADMIN" quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java quant-ai-platform/quant-services/quant-business/research-task-service/src/main/java
```

```powershell
rg -n "@RequestMapping|@GetMapping|@PostMapping|requirePermission|PERMISSION_|/api/tasks|/api/research/tasks" quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller quant-ai-platform/quant-services/quant-business/research-task-service/src/main/java/com/quant/task/controller/ResearchTaskController.java
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

Window 2 must run from `D:\projects\bussiness` after edits:

```powershell
Test-Path docs/harness/18-production-auth-gateway-target-scope.md
```

Expected result: `True`.

```powershell
Test-Path docs/harness/handoffs/phase-014-implementation.md
```

Expected result: `True`.

```powershell
rg -n "production auth|gateway|identity authority|role authority|service-to-service|demo header|X-User-Id|X-User-Role|role-access-configs|requirePermission|frontend route|menu|action gating|Phase 006|Phase 012|Phase 013|deferred|future Window 0|human approval|no behavior change" docs/harness/18-production-auth-gateway-target-scope.md docs/harness/handoffs/phase-014-implementation.md
```

Expected result: the durable artifact and implementation handoff contain the required target-scope facts, current contract facts, future-only labels and inherited guardrail references.

```powershell
rg -n "implemented|created gateway|created auth-service|created user-service|JWT implemented|session implemented|login implemented|OAuth implemented|SSO implemented|route migrated|route alias added|permission behavior changed|config mutated|role store migrated|service extracted|database schema changed|Redis changed|Kafka changed|frontend changed|Python changed|business code changed|new feature implemented|permanent modular monolith" docs/harness/18-production-auth-gateway-target-scope.md docs/harness/handoffs/phase-014-implementation.md
```

Expected result: there should be no matches that claim implemented work. If matches appear, they must be in explicit no-change, out-of-scope, blocker, deferred-decision, future-target or stop-rule sections.

```powershell
git status --short --untracked-files=all
```

Expected result: Window 2 records the final dirty/untracked state and claims only allowed Phase 014 documentation files, excluding pre-existing unrelated files.

Maven, npm build and Python runtime verification are not required because Phase 014 forbids Java, frontend, Python and test-code changes. If Window 2 touches any such area, it must stop as blocked instead of validating with builds.

## 13. Blocker Stop Rules

Window 2 must stop and write a blocker in `docs/harness/handoffs/phase-014-implementation.md` if any of these become necessary:

- Changing any URL path, HTTP method, endpoint owner, request binding, response envelope, response type, frontend route, frontend API function, TypeScript shape or permission behavior.
- Adding, removing, widening or narrowing any backend `requirePermission` call.
- Adding a permission check to a read-model surface that currently has no explicit check.
- Changing role codes, permission keys, menu keys, role mappings, header names, default header behavior or local role behavior.
- Mutating `role-access-configs.json` or any other config file.
- Adding gateway/auth/JWT/session/login/OAuth/SSO/auth-service/user-service/role-service code.
- Adding route aliases, compatibility bridges, gateway proxies, frontend API adapters, permission resolvers, auth adapters, role-store bridges, config-store bridges, DB adapters, Nacos adapters, service wrappers, migration runners, dual-write paths, rollback runners or sync jobs.
- Moving permission, role-access, task-create auth, config or route responsibility into another service.
- Creating or modifying Java, Python, frontend, database, Redis, Kafka, config, prompt-template, dependency, test, build or deployment files.
- Changing DTO, VO, entity, mapper, schema, topic, payload, Redis key, JSON shape, prompt-template shape, API type or frontend type shapes.
- Reclassifying frontend localStorage, frontend defaults, request headers, route guards, menu/button gating, audit rows or ingest history rows as production permission authority.
- Declaring `ai-orchestration-service`, JSON config files, header-based demo auth, request headers, frontend role cache or legacy `/api/tasks/*` paths final architecture.
- Closing D001, D002, D003, D007 or D008.
- Selecting a future target in a way that implies current implementation approval, breaking-change approval, permission behavior change, route migration, service extraction, config-store migration, role-store migration, frontend reshaping, Python behavior change, Kafka/database/Redis change or permanent modular-monolith outcome.
- Needing code behavior changes to make the production auth/gateway target-scope artifact true.
- Finding that identity authority, role authority or service-to-service propagation cannot be scoped without a broader human decision than Phase 014 approved.
- The existing `node scripts/authority-boundary-check.mjs` guard fails and cannot be treated as an environment-only issue.

When blocked, Window 2 must:

1. Stop edits at the smallest reversible point.
2. Record the exact file, route, permission key, role mapping, consumer, dependency or decision point that forced the blocker.
3. State which acceptance condition cannot be met.
4. State the smallest future phase or human decision required to unblock.
5. Ask for human decision instead of expanding scope.

## Window 2 Shape

Use one docs-only architecture/governance Window 2 implementation window.

Do not partition into backend, frontend or Python implementers. Do not start gateway/auth/JWT work, service extraction, route migration, config migration, role-store migration, test implementation, frontend guard edits, Python edits, Java edits, config edits, deployment edits or product feature work. Do not proceed until the user approves this Phase 014 architect handoff.

## Human Approval Request

Please approve this Phase 014 architect handoff before Window 2 starts.

Default approval meaning:

- No breaking changes.
- Keep all URL paths and frontend routes stable.
- No permission behavior change.
- No business behavior change.
- No new feature work.
- No gateway/auth/JWT implementation.
- No config mutation.
- Window 2 may perform docs-only production auth/gateway target-scoping work inside the allowed file boundaries above.
