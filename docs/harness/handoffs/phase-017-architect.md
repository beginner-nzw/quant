# Phase 017 Architect Handoff

## Status

Window: Window 1 - Phase Architect.

Phase: Phase 017 - Production Role Authority Selection Boundary.

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
- `docs/harness/handoffs/steering-decision-phase-017.md`

Additional current-state and durable boundary inputs:

- `docs/harness/handoffs/phase-016-final.md`
- `docs/harness/16-config-store-decision-boundary.md`
- `docs/harness/17-auth-gateway-permission-boundary.md`
- `docs/harness/18-production-auth-gateway-target-scope.md`
- `docs/harness/19-production-identity-issuer-validator-boundary.md`
- `docs/harness/20-production-identity-issuer-boundary.md`

Phase 017 is not Phase 001, so the Phase 001 special Java reading list is not the controlling implementation scope for this handoff.

## 1. Phase Goal

Produce a docs-only production role authority boundary artifact:

- Required durable artifact: `docs/harness/21-production-role-authority-boundary.md`.
- Select, at governance level only, the preferred future production role authority direction, or explicitly keep role authority deferred with narrower readiness criteria.
- Use Phase 012 config-store constraints, Phase 013 current permission inventory, Phase 014 role-authority gates, Phase 015 backend-owned ingress/gateway JWT validator placement and Phase 016 external IdP or enterprise directory issuer direction.
- Compare candidate production role authority directions:
  - external IdP or enterprise directory role/group claims
  - backend DB role store
  - auth-service or user-service role ownership
  - config-store-backed role source
  - bounded continuation of `role-access-configs.json` as a transition input
  - deliberate continued role-authority deferral with explicit readiness criteria
- Define role-assignment authority, role-permission mapping authority, permission-key/menu-key compatibility, frontend UI-gating boundaries, backend enforcement boundaries, role auditability, config-store dependency and migration/readiness gates.
- Preserve current role codes, permission keys, menu keys, backend `requirePermission` calls, intentional no-explicit-permission read surfaces, frontend route/menu/action gating behavior and `role-access-configs.json` shape.

Window 2 must evaluate every statement in this order:

```text
belongs -> authority -> contract -> behavior
```

This phase is docs-only. It must not implement gateway/auth/JWT, external IdP integration, auth-service, user-service, role-service, login/session, OAuth, SSO, permission behavior changes, config mutation, role-store migration, config-store migration, tests, static guards or business code.

## 2. Allowed File Scope

Window 2 may modify only:

- `docs/harness/21-production-role-authority-boundary.md`
- `docs/harness/handoffs/phase-017-implementation.md`

Window 2 may read, but must not write:

- required harness inputs listed above
- durable Phase 012 through Phase 016 artifacts listed above
- Java, frontend, Python, config and test files needed for read-only inventory commands

Window 4 may later update state, debt, backlog or transition lifetime after Window 3 approval. Window 2 must not do that in Phase 017.

## 3. Forbidden File Scope

Window 2 must not modify:

- any Java production or test file under `quant-ai-platform/quant-services/**`
- any Python file under `quant-ai-platform/quant-ai-engine/**`
- any frontend file under `quant-ui/**`
- any file under `quant-ai-platform/ai-config/**`
- any file under `quant-ai-platform/prompt-templates/**`
- runtime settings such as `application-*.yml`, `local.yml` or `local.private.yml`
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
- durable Phase 012 through Phase 016 artifacts
- prior phase handoffs

If satisfying Phase 017 appears to require any forbidden file, Window 2 must stop as blocked.

## 4. Must Stay Stable

Stable URL / API / behavior:

- All URL paths, HTTP methods, endpoint owners, request bindings, response envelopes and response types.
- All frontend routes, frontend API function names, endpoint strings, call signatures, TypeScript shapes and localStorage behavior.
- Current `X-User-Id`, `X-User-Role`, `X-Trace-Id`, missing-header defaults `guest` and `USER`, and frontend selected-user key `quant_current_user`.
- Current backend `UserContext`, `UserContextFilter`, `SecurityUtils`, `SecurityConstants` and `UserRoleEnum` runtime behavior.
- Current backend explicit `requirePermission` checks and intentional no-explicit-permission read surfaces.
- Current role codes, permission keys, menu keys, coarse access-role mapping and `role-access-configs.json` shape.
- DTOs, VOs, entities, mappers, database schema, Redis keys, Kafka topics, Kafka payloads, JSON config shapes, prompt-template shapes, Python payloads and runtime settings.

Stable architecture:

- Header-based demo auth remains a transition mechanism, not production security.
- `role-access-configs.json` remains the current transition role/menu/permission config input under Phase 012.
- External IdP or enterprise directory remains only the preferred future production identity issuer direction from Phase 016.
- Backend-owned ingress/gateway JWT validation remains only the preferred future validator placement from Phase 015.
- `UserContext` remains runtime context, not production identity, profile or role authority.
- `ai-orchestration-service` remains a transition host, not final auth, gateway, identity, role or config architecture.
- `research-task-service` remains the formal host for task creation and only reads role config for task-create permission.
- `quant-ui` remains a UI/runtime header source and route/menu/action gating consumer, not backend permission, identity or role authority.
- Legacy `/api/tasks/*` remains a frozen transition namespace, not final route architecture.
- Phase 017 does not close D001, D002, D003, D007 or D008.

## 5. Allowed New Class / Method Types

No business-code classes or methods are allowed.

Allowed documentation-only structures:

- Markdown sections and tables.
- Current inherited fact inventory from Phase 012, Phase 013, Phase 014, Phase 015 and Phase 016.
- Production role authority option matrix.
- Governance-only selected role authority direction, or explicit bounded deferral if selection is not safe.
- Role assignment authority rules.
- Role-permission mapping authority rules.
- Permission-key, menu-key, role-code and coarse access-role compatibility rules.
- Frontend UI-gating boundary rules.
- Backend enforcement boundary rules.
- User profile, token claim, issuer, validator and service-principal dependency rules.
- Role auditability and audit identity dependency rules.
- Config-store and role-store migration dependency rules.
- Demo-header compatibility constraints.
- Route, service-extraction and gateway/JWT dependency rules.
- Rollback and compatibility constraints for later implementation phases.
- Deferred-decision list.
- Stop-rule list.
- Belongs/authority/contract/behavior acceptance checklist.

Allowed scripts/classes/methods:

- None.

Window 2 must not add Java classes, Java tests, frontend scripts, Python scripts, build steps, runtime code, config files, migration files, static guard scripts, helper classes or adapters in this phase.

## 6. Forbidden Helpers / Adapters / Fallbacks / Bridges

Do not add or approve:

- gateway proxy
- auth-service, user-service, role-service or session service
- JWT, OAuth, SSO, login, refresh-token or session adapter
- external IdP connector, directory connector, group-sync connector or token introspection adapter
- role mapper, claim mapper, group mapper, permission mapper or menu mapper implementation
- any production `*Helper`, `*Adapter`, `*Bridge`, `*Facade`, `*Fallback`, `*Resolver`, `*Router`, `*Mapper`, `*Provider` or compatibility layer
- any frontend auth adapter, API adapter, permission resolver, identity resolver, role resolver or truth resolver
- any route alias, compatibility endpoint or gateway route bridge
- any role-store bridge, config-store bridge, DB adapter, Nacos adapter or hybrid adapter
- any migration runner, sync job, dual-write path or rollback tool
- any service-to-service auth wrapper, event-auto auth bridge or callback identity wrapper
- any test helper or static guard script
- any Kafka, Redis or database compatibility bridge
- any temporary auth/role/permission wrapper around current services

Window 2 may document future target options and requirements, but those must be labeled as deferred future work requiring later Window 0 selection and human approval.

## 7. Belongs

Current belongs baseline inherited from Phase 012 through Phase 016:

| Area | Current host | Phase 017 boundary |
| --- | --- | --- |
| Role/menu/permission config input | `quant-ai-platform/ai-config/role-access-configs.json` | Current JSON transition input. Not final role-store architecture. |
| Request context plumbing | `quant-common-security` | Carries request user, role and trace context. Not production identity or role authority. |
| Demo role input | request headers / frontend request utilities | `X-User-Role` remains a local/demo compatibility input only. |
| Runtime user and role context | `UserContextFilter`, `UserContext`, `SecurityUtils` | Runtime carrier only. Not a selected production role authority. |
| Most checked command permissions | `ai-orchestration-service` / `RoleAccessConfigService` | Transition host enforcement for current checked endpoints. Not final auth or role service. |
| Task-create permission | `research-task-service` / `TaskRoleAccessService` | Formal task-create host and permission reader only. Not auth or role config owner. |
| UI role selection and gating | `quant-ui` | UI affordance and demo header source only. Not identity, role or permission SoT. |
| Future production identity issuer | not implemented | Preferred future direction is external IdP or enterprise directory from Phase 016. |
| Future production identity validator | not implemented | Preferred placement is backend-owned ingress/gateway JWT validation from Phase 015. |
| Future production role authority | not implemented | Phase 017 may select or defer the future role authority direction at governance level only. |

Future belongs options that may be documented only:

- External IdP or directory groups/claims may become a role input or authority only after later approved integration and claim mapping.
- A backend DB role store may own role assignments, role-permission mappings or both only after later approved role-store migration.
- An auth-service, user-service or role-service may own roles only after later approved service creation or extraction.
- A config-store-backed role source may own role mappings only after Phase 012 migration gates are satisfied and later approved.
- `role-access-configs.json` may continue as a bounded transition input only if explicitly labeled non-final and paired with exit/readiness criteria.

Phase 017 must not create any of these future hosts.

## 8. Authority

Current authority facts that must remain stable:

| Surface | Current meaning | Current classification |
| --- | --- | --- |
| `role-access-configs.json` | role/menu/permission config input | JSON transition store; current permission config input |
| `X-User-Role` | request role header | demo/runtime input, not production role authority |
| `X-User-Id` | request user id header | demo/runtime input, not production identity |
| `UserContext` | request-scoped user id and role carrier | runtime context, not SoT |
| `SecurityUtils.currentUserRole()` | current request role reader | current permission input reader |
| `RoleAccessConfigService` | permission checker and role-access config mutator in `ai-orchestration-service` | transition host service, not final auth or role service |
| `TaskRoleAccessService` | task-create permission checker in `research-task-service` | reader/checker only, not role config owner |
| frontend local role utilities | selected local demo user and role normalization | UI/runtime input only |
| frontend route/menu/action gating | UI visibility and navigation affordance | not backend enforcement or role authority |

Phase 017 governance-only authority rules:

- The artifact must distinguish identity issuer, identity validator, runtime user context, user profile source, role authority, role assignment authority and role-permission mapping authority.
- The artifact may select a preferred future production role authority direction, or deliberately defer role authority with narrower readiness criteria.
- Any selected role authority direction must be labeled future-only and not current runtime authority.
- If external IdP or directory claims are selected or preferred as a future role source, the artifact must state whether claims/groups are role assignment authority, role input only, or require backend mapping before becoming application role authority.
- If backend DB/auth-service/user-service/config-store-backed authority is selected or preferred, the artifact must state which part it owns: role assignment, role-permission mapping, menu mapping, or display/profile enrichment.
- `role-access-configs.json` remains current transition input and must not be reclassified as final architecture.
- Backend `requirePermission` calls remain current enforcement points for checked endpoints and must not change.
- Intentional no-explicit-permission read surfaces remain stable and must not receive new authority claims.
- Frontend gating remains UI affordance and must not become backend permission or role truth.
- Audit identity and role auditability may be specified as future dependencies only and must not change current audit fields.
- Service-principal and service-to-service role semantics may be specified as future dependencies only.
- No documentation in Phase 017 closes D001, D002, D003, D007 or D008.

Forbidden authority changes:

- No new current role SoT may be introduced.
- No current request header may be reclassified as production role authority.
- No external IdP, directory, DB role store, auth-service, user-service, role-service or config store may be documented as implemented current authority.
- No frontend cache, localStorage, route guard, menu gating or action gating may become backend permission truth.
- No read model, workbench output, audit row, ingest history row, fallback provenance, report metadata or Kafka callback may become role authority.
- No `role-access-configs.json` mutation, config-store migration or role-store migration may occur.
- No documentation may claim header-based demo auth, JSON role config, legacy `/api/tasks/*`, `ai-orchestration-service` or the modular monolith is final architecture.

## 9. Contract

Stable contract rules:

- Every endpoint keeps the same path, HTTP method, controller owner, request binding, response envelope, response type and permission behavior.
- `POST /api/research/tasks` remains hosted by `research-task-service` and keeps current `TASK_CREATE` behavior through `TaskRoleAccessService`.
- Existing checked commands keep current explicit permission checks.
- Intentional no-explicit-permission read surfaces from Phase 013 remain unchanged.
- No auth URL, gateway URL, role URL, login URL, callback URL, route alias, compatibility endpoint, proxy route or new namespace is added.
- No endpoint is deleted, renamed, consolidated, split or moved.
- No permission key, menu key, role code, coarse access-role mapping, header name or default value is added, removed or renamed.
- No existing backend `requirePermission` call is added, removed, widened, narrowed, moved or renamed.
- No explicit permission check is added to a read-model endpoint that currently has none.
- `X-User-Id`, `X-User-Role`, `X-Trace-Id`, `guest`, `USER` and `quant_current_user` behavior remain stable.
- Frontend route metadata, API function names, endpoint strings, call signatures, TypeScript shapes, role localStorage key, request-header utility, menu gating and action gating behavior remain stable.
- `role-access-configs.json` file shape remains stable.
- DTO, VO, entity, mapper, schema, Kafka topic, Kafka payload, Redis key, JSON config shape, prompt-template shape, Python payload and runtime settings remain stable.

Contract language allowed in the Phase 017 artifact:

- future role authority options
- future selected or deferred role authority direction
- future role assignment and role-permission mapping prerequisites
- future role-code, permission-key and menu-key compatibility prerequisites
- future claim/group mapping prerequisites
- future user profile source constraints
- future service principal and service-to-service role handoff dependencies
- future audit identity and role-audit dependencies
- future gateway validator and issuer compatibility prerequisites
- future route-migration, service-extraction, config-store and role-store prerequisites
- future rollback and compatibility constraints

Contract language forbidden in the Phase 017 artifact:

- claiming a new current role/auth endpoint exists
- claiming gateway/JWT/session/login/OAuth/SSO behavior is implemented
- claiming an auth-service, user-service, role-service, external IdP, directory integration or DB role store exists
- claiming demo headers are retired now
- claiming current read endpoints now require permissions
- claiming route migration, endpoint aliasing or breaking changes are approved
- claiming frontend UI gating is backend enforcement
- claiming `role-access-configs.json` is final role authority
- claiming role claims/groups from a future issuer are trusted before a later approved gateway/JWT and claim-mapping phase

## 10. Behavior

No runtime behavior may change.

Stable behavior includes:

- no business behavior change
- no permission behavior change
- no frontend behavior change
- no Python behavior change
- no Kafka, Redis, database, config, prompt-template, build, dependency or deployment behavior change
- no gateway, auth, JWT, session, login, OAuth, SSO, external IdP, directory, role-store or config-store behavior
- no test or static guard behavior change

Phase 017 is acceptable only as static governance documentation plus a Window 2 implementation handoff.

## 11. Required Production Role Authority Boundary Artifact Shape

`docs/harness/21-production-role-authority-boundary.md` must include:

- Status and scope.
- Inputs and read-only inspection sources.
- Current inherited facts from Phase 012, Phase 013, Phase 014, Phase 015 and Phase 016:
  - `role-access-configs.json` is the current transition role/menu/permission config input.
  - `X-User-Id` and `X-User-Role` are demo/runtime inputs only.
  - missing backend values default to `guest` and `USER`.
  - `UserContext` is current runtime context, not production identity or role authority.
  - backend explicit `requirePermission` calls are current enforcement points.
  - intentional no-explicit-permission read surfaces remain stable.
  - frontend route/menu/action gating is UI affordance only.
  - backend-owned ingress/gateway JWT validation is the preferred future validator placement.
  - external IdP or enterprise directory is the preferred future issuer direction.
  - no production gateway/auth/JWT, external IdP integration, auth-service, user-service, role-service or role store exists.
- Current role-code, permission-key, menu-key and coarse access-role inventory.
- Production role authority option matrix.
- Governance-only selected role authority direction, or explicit bounded deferral if selection is not safe.
- Rationale for why the selected or deferred role direction fits Phase 012 config-store constraints, Phase 015 validator placement and Phase 016 issuer direction.
- Belongs rules for role assignment, role-permission mapping, permission/menu key ownership, backend enforcement, frontend gating, role audit and migration ownership.
- Authority rules separating identity issuer, identity validator, runtime context, user profile source, role assignment authority, role-permission mapping authority and frontend UI affordance.
- Compatibility rules for current role codes, permission keys, menu keys, coarse access-role mapping, backend `requirePermission` calls and no-explicit-permission read surfaces.
- User profile source dependency rules without selecting profile ownership unless explicitly labeled deferred future work.
- Token claim, group claim, issuer/audience and gateway validator dependency rules.
- Service-principal validation and service-to-service role handoff dependencies.
- Audit identity and role auditability dependencies for human actor, delegated actor, service principal and system-triggered action.
- Demo-header compatibility rules.
- Stable URL/API/permission/header/frontend/config contract rules.
- Dependencies on Phase 006 route freeze, Phase 012 config-store boundary, Phase 013 permission inventory, Phase 014 target-scope artifact, Phase 015 validator placement and Phase 016 issuer direction.
- Dependencies for later gateway/JWT implementation design.
- Dependencies for later user profile source selection.
- Dependencies for later route migration, service extraction, config-store migration and role-store migration.
- Deferred implementation decisions.
- Stop rules for later phases.
- Belongs/authority/contract/behavior acceptance checklist.

The artifact must explicitly state that it does not implement or approve gateway/auth/JWT, external IdP integration, auth-service, user-service, role-service, login/session, OAuth, SSO, route migration, endpoint aliases, permission behavior changes, DTO/VO/entity/schema changes, Redis changes, Kafka changes, frontend changes, Python changes, config mutation, config-store migration, role-store migration, business behavior changes or new feature work.

## 12. Acceptance Conditions

Phase 017 is acceptable only if all conditions hold:

- `docs/harness/21-production-role-authority-boundary.md` exists and is the primary durable production role authority boundary artifact.
- `docs/harness/handoffs/phase-017-implementation.md` records exact files changed and verification outcomes.
- The artifact is docs-only and does not claim runtime implementation.
- The artifact restates current Phase 012, Phase 013, Phase 014, Phase 015 and Phase 016 facts for role config, headers, defaults, runtime context, backend permission checks, no-explicit-permission read surfaces, frontend gating, current absence of production auth/role infrastructure, future-only validator placement and future-only issuer direction.
- The artifact selects or explicitly defers a future production role authority direction while labeling it future-only and not current runtime authority.
- The artifact identifies whether future role authority means role assignment authority, role-permission mapping authority, menu mapping authority, or a split of those concerns.
- The artifact preserves demo-header compatibility without changing current `X-User-Id` or `X-User-Role` behavior.
- The artifact defines claim/group, user profile, token/session, service-principal, service-to-service handoff, config-store, role-store and audit identity readiness gates without changing code, headers, Kafka payloads, DTOs, frontend types or runtime behavior.
- The artifact preserves every URL, HTTP method, request binding, response envelope, response type, permission key, explicit permission check, intentional no-explicit-permission read surface, frontend route, frontend API function, frontend role utility, localStorage behavior, request-header behavior, TypeScript shape and `role-access-configs.json` shape.
- The artifact preserves Phase 005 through Phase 016 constraints.
- The artifact does not choose or implement gateway/auth/JWT, external IdP integration, auth-service, user-service, role-service, service extraction, route migration, route aliases, endpoint deletion/rename/consolidation, permission behavior change, role-access config mutation, config-store migration, role-store migration, frontend reshaping, Python behavior change, Kafka/database/Redis changes, permanent modular-monolith architecture or new feature work.
- No Java, Python, frontend, database, Redis, Kafka, `ai-config`, prompt-template, dependency, build-config, deployment, state, debt, backlog, transition lifetime, durable Phase 012/013/014/015/016 artifact or prior handoff file changes occur.
- Any future-phase recommendation is explicitly labeled as deferred and requiring a later Window 0 decision plus human approval.
- `git status --short --untracked-files=all` after Window 2 shows only allowed Phase 017 documentation files as new Window 2 changes, aside from pre-existing unrelated dirty files clearly excluded from the Window 2 claim.

## 13. Required Verification Commands

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
rg -n "X-User-Id|X-User-Role|quant_current_user|quant_role_access_configs|ROLE_ACCESS_UPDATED_EVENT|USER_ROLE|PERMISSION_KEY|MENU_KEY|fetchRoleAccessConfigs|updateRoleAccessConfig|requiredMenuKey|requiredPermissionKey" quant-ui/src/api/task.ts quant-ui/src/router/index.ts quant-ui/src/layout/BasicLayout.vue quant-ui/src/utils/auth.ts quant-ui/src/utils/requestHeaders.ts quant-ui/src/utils/request.ts quant-ui/src/utils/roleAccess.ts quant-ui/src/utils/taskActionAccess.ts quant-ui/src/views quant-ui/scripts/authority-boundary-check.mjs
```

```powershell
rg -n "roleCode|permissionKeys|menuKeys|TASK_VIEW|TASK_CREATE|TASK_RETRY|TASK_CANCEL|AUDIT_COMPLIANCE_VIEW|REPORT_REVIEW|MODEL_AGENT_CONFIG_VIEW|MODEL_AGENT_CONFIG_EDIT|TASK_LIST|MARKET_EVENTS|MARKET_INTELLIGENCE|RESEARCH_WORKBENCH|STRATEGY_SIGNALS|RISK_WARNINGS|RESEARCH_REPORTS|AUDIT_COMPLIANCE|MODEL_AGENT_CONFIG|REPORTS_PENDING|REPORTS_APPROVED|REPORTS_REJECTED|RESEARCHER|PM|RISK_MANAGER|COMPLIANCE_AUDITOR|ADMIN" quant-ai-platform/ai-config/role-access-configs.json
```

Window 2 must run from `D:\projects\bussiness\quant-ui`:

```powershell
node scripts/authority-boundary-check.mjs
```

Expected result: the existing Phase 007 static guard still passes. If it fails, Window 2 must record the failure as a blocker and must not patch frontend code in this phase.

Window 2 must run from `D:\projects\bussiness` after edits:

```powershell
Test-Path docs/harness/21-production-role-authority-boundary.md
```

Expected result: `True`.

```powershell
Test-Path docs/harness/handoffs/phase-017-implementation.md
```

Expected result: `True`.

```powershell
rg -n "role authority|production role|role-access-configs|permission key|menu key|role code|requirePermission|no-explicit-permission|frontend gating|X-User-Role|UserContext|external IdP|directory|gateway|JWT|Phase 006|Phase 012|Phase 013|Phase 014|Phase 015|Phase 016|deferred|future Window 0|human approval|no behavior change" docs/harness/21-production-role-authority-boundary.md docs/harness/handoffs/phase-017-implementation.md
```

Expected result: the durable artifact and implementation handoff contain the required role authority boundary facts, future-only labels and inherited guardrail references.

```powershell
rg -n "implemented|created gateway|created auth-service|created user-service|created role-service|JWT implemented|session implemented|login implemented|OAuth implemented|SSO implemented|IdP integrated|directory integrated|role store created|role store migrated|route migrated|route alias added|permission behavior changed|permission widened|permission narrowed|config mutated|config-store migrated|service extracted|database schema changed|Redis changed|Kafka changed|frontend changed|Python changed|business code changed|new feature implemented|permanent modular monolith" docs/harness/21-production-role-authority-boundary.md docs/harness/handoffs/phase-017-implementation.md
```

Expected result: there should be no matches that claim implemented work. If matches appear, they must be in explicit no-change, out-of-scope, blocker, deferred-decision, future-target or stop-rule sections.

```powershell
git status --short --untracked-files=all
```

Expected result: Window 2 records the final dirty/untracked state and claims only allowed Phase 017 documentation files, excluding pre-existing unrelated files.

Maven, npm build and Python runtime verification are not required because Phase 017 forbids Java, frontend, Python and test-code changes. If Window 2 touches any such area, it must stop as blocked instead of validating with builds.

## 14. Blocker Stop Rules

Window 2 must stop and write a blocker in `docs/harness/handoffs/phase-017-implementation.md` if any of these become necessary:

- Changing any URL path, HTTP method, endpoint owner, request binding, response envelope, response type, frontend route, frontend API function, TypeScript shape or permission behavior.
- Adding, removing, widening or narrowing any backend `requirePermission` call.
- Adding a permission check to a read-model surface that currently has no explicit check.
- Changing role codes, permission keys, menu keys, role mappings, header names, default header behavior or local role behavior.
- Mutating `role-access-configs.json` or any other config file.
- Adding gateway/auth/JWT/session/login/OAuth/SSO/auth-service/user-service/role-service/external-IdP/directory code.
- Adding route aliases, compatibility bridges, gateway proxies, frontend API adapters, identity resolvers, permission resolvers, role resolvers, auth adapters, role-store bridges, config-store bridges, DB adapters, Nacos adapters, service wrappers, migration runners, dual-write paths, rollback runners or sync jobs.
- Moving identity, permission, role-access, task-create auth, config or route responsibility into another service.
- Creating or modifying Java, Python, frontend, database, Redis, Kafka, config, prompt-template, dependency, test, build or deployment files.
- Changing DTO, VO, entity, mapper, schema, topic, payload, Redis key, JSON shape, prompt-template shape, API type or frontend type shapes.
- Reclassifying frontend localStorage, frontend defaults, request headers, route guards, menu/button gating, audit rows or ingest history rows as production identity, role or permission authority.
- Declaring `ai-orchestration-service`, JSON config files, header-based demo auth, request headers, frontend role cache or legacy `/api/tasks/*` paths final architecture.
- Closing D001, D002, D003, D007 or D008.
- Selecting a future target in a way that implies current implementation approval, breaking-change approval, permission behavior change, route migration, service extraction, config-store migration, role-store migration, frontend reshaping, Python behavior change, Kafka/database/Redis change or permanent modular-monolith outcome.
- Needing code behavior changes to make the role authority boundary artifact true.
- Finding that role authority selection cannot be scoped without a broader human decision than Phase 017 approved.
- The existing `node scripts/authority-boundary-check.mjs` guard fails and cannot be treated as an environment-only issue.

When blocked, Window 2 must:

1. Stop edits at the smallest reversible point.
2. Record the exact file, route, permission key, role mapping, consumer, dependency or decision point that forced the blocker.
3. State which acceptance condition cannot be met.
4. State the smallest future phase or human decision required to unblock.
5. Ask for human decision instead of expanding scope.

## Window 2 Shape

Use one docs-only architecture/governance Window 2 implementation window.

Do not partition into backend, frontend or Python implementers. Do not start gateway/auth/JWT work, external IdP integration, service extraction, route migration, config migration, role-store migration, test implementation, frontend guard edits, Python edits, Java edits, config edits, deployment edits or product feature work. Do not proceed until the user approves this Phase 017 architect handoff.

## Human Approval Request

Please approve this Phase 017 architect handoff before Window 2 starts.

Default approval meaning:

- No breaking changes.
- Keep all URL paths and frontend routes stable.
- No permission behavior change.
- No business behavior change.
- No new feature work.
- No gateway/auth/JWT implementation.
- No external IdP integration.
- No auth-service, user-service, role-service, login/session, OAuth or SSO implementation.
- No config mutation.
- No role-store or config-store migration.
- Window 2 may perform docs-only production role authority boundary work inside the allowed file boundaries above.
