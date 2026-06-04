# Phase 016 Architect Handoff

## Status

Window: Window 1 - Phase Architect.

Phase: Phase 016 - Production Identity Issuer Selection Boundary.

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
- `docs/harness/handoffs/steering-decision-phase-016.md`

Additional current-state and durable boundary inputs:

- `docs/harness/handoffs/phase-015-final.md`
- `docs/harness/12-transition-host-exit-criteria.md`
- `docs/harness/13-report-boundary-readiness.md`
- `docs/harness/14-market-data-ingest-boundary-readiness.md`
- `docs/harness/15-risk-strategy-projection-boundary-readiness.md`
- `docs/harness/16-config-store-decision-boundary.md`
- `docs/harness/17-auth-gateway-permission-boundary.md`
- `docs/harness/18-production-auth-gateway-target-scope.md`
- `docs/harness/19-production-identity-issuer-validator-boundary.md`

Phase 016 is not Phase 001, so the Phase 001 special Java reading list is not the controlling implementation scope for this handoff.

## 1. Phase Goal

Produce a docs-only production identity issuer selection boundary artifact:

- Required durable artifact: `docs/harness/20-production-identity-issuer-boundary.md`.
- Decide, at governance level only, the preferred future production identity issuer direction or deliberately keep issuer selection deferred with narrower readiness criteria.
- Use Phase 015 as input: backend-owned ingress/gateway JWT validation remains the preferred future production identity validator placement.
- Compare candidate issuer directions:
  - internal auth-service issuer
  - external IdP or directory issuer
  - user-service profile owner with a separate issuer
  - another backend-owned issuer
  - deliberate continued issuer deferral with explicit readiness criteria
- Define user profile source dependencies, token claim dependencies, role authority dependencies, service-principal dependencies, demo-header compatibility dependencies and rollback constraints.
- Preserve `X-User-Id` and `X-User-Role` as current local/demo compatibility inputs only.
- Preserve Phase 006 route freeze, Phase 012 config-store boundary, Phase 013 permission inventory, Phase 014 auth/gateway target scope and Phase 015 validator placement.

Window 2 must evaluate every statement in this order:

```text
belongs -> authority -> contract -> behavior
```

This phase is docs-only. It must not implement gateway/auth/JWT, auth-service, user-service, role-service, login/session, OAuth, SSO, external IdP integration, permission behavior changes, config mutation, tests, static guards or business code.

## 2. Allowed File Scope

Window 2 may modify only:

- `docs/harness/20-production-identity-issuer-boundary.md`
- `docs/harness/handoffs/phase-016-implementation.md`

Window 2 may read, but must not write:

- required harness inputs listed above
- durable Phase 008 through Phase 015 artifacts listed above
- Java, frontend, Python, config and test files needed for read-only inventory commands

Window 4 may later update state, debt, backlog or transition lifetime after Window 3 approval. Window 2 must not do that in Phase 016.

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
- durable Phase 008 through Phase 015 artifacts
- prior phase handoffs

If satisfying Phase 016 appears to require any forbidden file, Window 2 must stop as blocked.

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
- Backend-owned ingress/gateway JWT validation remains only the preferred future validator placement from Phase 015.
- Concrete production issuer, user profile source and production role authority are not current runtime authority.
- `ai-orchestration-service` remains a transition host, not final auth, gateway, identity or role architecture.
- `research-task-service` remains the formal host for task creation and only reads role config for task-create permission.
- `quant-ui` remains a UI/runtime header source and route/menu/action gating consumer, not backend permission or identity authority.
- Legacy `/api/tasks/*` remains a frozen transition namespace, not final route architecture.
- Phase 016 does not close D001, D002, D003, D007 or D008.

## 5. Allowed New Class / Method Types

No business-code classes or methods are allowed.

Allowed documentation-only structures:

- Markdown sections and tables.
- Current inherited fact inventory from Phase 013, Phase 014 and Phase 015.
- Production identity issuer option matrix.
- Governance-only selected issuer direction, or explicit bounded deferral if selection is not safe.
- User profile source dependency rules.
- Token claim and token/session dependency rules.
- Role authority dependency rules without selecting or migrating role authority.
- Service-principal and service-to-service identity handoff dependencies.
- Audit identity dependency rules.
- Demo-header compatibility constraints.
- Gateway validator compatibility dependencies.
- Route, service-extraction, config-store and role-store dependency rules.
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
- external IdP connector, directory connector or token introspection adapter
- any production `*Helper`, `*Adapter`, `*Bridge`, `*Facade`, `*Fallback`, `*Resolver`, `*Router`, `*Mapper`, `*Provider` or compatibility layer
- any frontend auth adapter, API adapter, permission resolver, identity resolver or truth resolver
- any route alias, compatibility endpoint or gateway route bridge
- any role-store bridge, config-store bridge, DB adapter, Nacos adapter or hybrid adapter
- any migration runner, sync job, dual-write path or rollback tool
- any service-to-service auth wrapper, event-auto auth bridge or callback identity wrapper
- any test helper or static guard script
- any Kafka, Redis or database compatibility bridge
- any temporary auth/permission wrapper around current services

Window 2 may document future target options and requirements, but those must be labeled as deferred future work requiring later Window 0 selection and human approval.

## 7. Belongs

Current belongs baseline inherited from Phase 013, Phase 014 and Phase 015:

| Area | Current host | Phase 016 boundary |
| --- | --- | --- |
| Request context plumbing | `quant-common-security` | Carries request user, role and trace context. Not production identity authority. |
| Demo identity inputs | request headers / frontend request utilities | `X-User-Id` and `X-User-Role` remain local/demo compatibility inputs only. |
| Runtime user context | `UserContextFilter`, `UserContext`, `SecurityUtils` | Runtime carrier only. Not a selected production issuer, validator or profile source. |
| Role/menu/permission config input | `quant-ai-platform/ai-config/role-access-configs.json` | Current JSON transition input. Not final role-store architecture. |
| Most checked command permissions | `ai-orchestration-service` / `RoleAccessConfigService` | Transition host enforcement for current checked endpoints. Not final auth service. |
| Task-create permission | `research-task-service` / `TaskRoleAccessService` | Formal task-create host and permission reader only. Not auth or role config owner. |
| UI role selection and gating | `quant-ui` | UI affordance and demo header source only. Not identity or permission SoT. |
| Future identity validator | not implemented | Preferred placement from Phase 015 is backend-owned ingress/gateway JWT validation, future-only. |
| Future identity issuer | not implemented | Phase 016 may select or defer the future issuer direction at governance level only. |

Future belongs options that may be documented only:

- A future internal auth-service issuer may belong to a new auth-service only after later approval.
- A future external IdP or directory may issue identity or supply claims only after later approved integration.
- A future user-service may own user profile facts, but user profile ownership must remain separate from issuer and validator unless later explicitly combined.
- Another backend-owned issuer may be documented only as a future target with clear ownership and readiness gates.
- A future service-principal source must belong to the approved auth/gateway/service boundary, not frontend utilities, domain read models, fallback metadata or ad hoc headers.

Phase 016 must not create any of these future hosts.

## 8. Authority

Current authority facts that must remain stable:

| Surface | Current meaning | Current classification |
| --- | --- | --- |
| `X-User-Id` | request user id header | demo/runtime input, not production identity |
| `X-User-Role` | request role header | demo/runtime input, not production role authority |
| `X-Trace-Id` | request trace header | runtime tracing input, not identity or permission authority |
| `UserContext` | request-scoped user id and role carrier | runtime context, not SoT |
| `SecurityUtils.currentUserId()` | current request user id reader | runtime metadata reader |
| `SecurityUtils.currentUserRole()` | current request role reader | current permission input reader |
| `role-access-configs.json` | role/menu/permission config input | JSON transition store; current permission config input |
| `RoleAccessConfigService` | permission checker and role-access config mutator in `ai-orchestration-service` | transition host service, not final auth service |
| `TaskRoleAccessService` | task-create permission checker in `research-task-service` | reader/checker only, not role config owner |
| frontend local role utilities | selected local demo user and role normalization | UI/runtime input only |

Phase 016 governance-only authority rules:

- The artifact must distinguish current runtime context from future trusted identity authority.
- The artifact may select a preferred future production identity issuer direction, or deliberately defer issuer selection with narrower readiness criteria.
- Any selected issuer direction must be labeled future-only and not current runtime authority.
- Phase 015's backend-owned ingress/gateway JWT validator placement remains future-only and not current runtime authority.
- Production identity must remain constrained to a future backend-owned ingress/auth boundary before business services may trust it.
- Demo headers must remain local/demo compatibility inputs until a later approved compatibility or retirement phase changes them.
- User profile ownership must be documented as a separate future decision unless the selected issuer direction explicitly states that a later phase may combine issuer and profile ownership.
- Role authority is not selected in Phase 016. The artifact may document how issuer options affect later role-authority candidates.
- Service principal validation and user delegation semantics may be specified as future dependencies only.
- Audit identity semantics may be specified as future dependencies only and must not change current audit fields.

Forbidden authority changes:

- No new current identity SoT may be introduced.
- No current request header may be reclassified as production identity authority.
- No JWT, gateway, auth-service, user-service, external IdP or directory may be documented as implemented current authority.
- No frontend cache, localStorage, route guard, menu gating or action gating may become backend identity or permission truth.
- No read model, workbench output, audit row, ingest history row, fallback provenance, report metadata or Kafka callback may become identity authority.
- No `role-access-configs.json` mutation, config-store migration or role-store migration may occur.
- No documentation may claim header-based demo auth, JSON role config, legacy `/api/tasks/*`, `ai-orchestration-service` or the modular monolith is final architecture.
- No documentation may close D001, D002, D003, D007 or D008.

## 9. Contract

Stable contract rules:

- Every endpoint keeps the same path, HTTP method, controller owner, request binding, response envelope, response type and permission behavior.
- `POST /api/research/tasks` remains hosted by `research-task-service` and keeps current `TASK_CREATE` behavior through `TaskRoleAccessService`.
- Existing checked commands keep current explicit permission checks.
- Intentional no-explicit-permission read surfaces from Phase 013 remain unchanged.
- No auth URL, gateway URL, login URL, callback URL, route alias, compatibility endpoint, proxy route or new namespace is added.
- No endpoint is deleted, renamed, consolidated, split or moved.
- No permission key, menu key, role code, header name or default value is added, removed or renamed.
- No existing backend `requirePermission` call is added, removed, widened, narrowed, moved or renamed.
- No explicit permission check is added to a read-model endpoint that currently has none.
- `X-User-Id`, `X-User-Role`, `X-Trace-Id`, `guest`, `USER` and `quant_current_user` behavior remain stable.
- Frontend route metadata, API function names, endpoint strings, call signatures, TypeScript shapes, role localStorage key, request-header utility, menu gating and action gating behavior remain stable.
- DTO, VO, entity, mapper, schema, Kafka topic, Kafka payload, Redis key, JSON config shape, prompt-template shape, Python payload and runtime settings remain stable.

Contract language allowed in the Phase 016 artifact:

- future issuer options
- future selected or deferred issuer direction
- future token claim and token/session prerequisites
- future user profile source constraints
- future role authority dependencies
- future service principal and service-to-service identity handoff dependencies
- future audit identity dependencies
- future gateway validator compatibility prerequisites
- future route-migration, service-extraction, config-store and role-store prerequisites
- future rollback and compatibility constraints

Contract language forbidden in the Phase 016 artifact:

- claiming a new current auth endpoint exists
- claiming gateway/JWT/session/login/OAuth/SSO behavior is implemented
- claiming an auth-service, user-service, role-service, external IdP or directory integration exists
- claiming demo headers are retired now
- claiming current read endpoints now require permissions
- claiming route migration, endpoint aliasing or breaking changes are approved
- claiming frontend UI gating is backend enforcement
- claiming `role-access-configs.json` is final role authority

## 10. Behavior

No runtime behavior may change.

Stable behavior includes:

- no business behavior change
- no permission behavior change
- no frontend behavior change
- no Python behavior change
- no Kafka, Redis, database, config, prompt-template, build, dependency or deployment behavior change
- no gateway, auth, JWT, session, login, OAuth, SSO or external IdP behavior
- no test or static guard behavior change

Phase 016 is acceptable only as static governance documentation plus a Window 2 implementation handoff.

## 11. Required Production Identity Issuer Boundary Artifact Shape

`docs/harness/20-production-identity-issuer-boundary.md` must include:

- Status and scope.
- Inputs and read-only inspection sources.
- Current inherited facts from Phase 013, Phase 014 and Phase 015:
  - `X-User-Id` and `X-User-Role` are demo/runtime inputs only.
  - missing backend values default to `guest` and `USER`.
  - `UserContext` is current runtime context, not production identity authority.
  - `role-access-configs.json` is the current transition role/menu/permission config input.
  - backend explicit `requirePermission` calls are current enforcement points.
  - frontend route/menu/action gating is UI affordance only.
  - backend-owned ingress/gateway JWT validation is the preferred future validator placement.
  - no production gateway/auth/JWT implementation exists.
- Production identity issuer option matrix.
- Governance-only selected issuer direction, or explicit bounded deferral if selection is not safe.
- Rationale for why the selected or deferred issuer direction fits Phase 015's validator placement.
- User profile source dependency rules.
- Token claim, issuer/audience and token/session dependency rules.
- Production role authority dependencies without selecting or migrating role authority.
- Service-principal validation and service-to-service identity handoff dependencies.
- Audit identity dependencies for human actor, delegated actor, service principal and system-triggered action.
- Demo-header compatibility rules.
- Stable URL/API/permission/header/frontend contract rules.
- Dependencies on Phase 006 route freeze, Phase 012 config-store boundary, Phase 013 permission inventory, Phase 014 target-scope artifact and Phase 015 validator placement.
- Dependencies for later production role authority selection.
- Dependencies for later gateway/JWT implementation design.
- Dependencies for later route migration, service extraction, config-store migration and role-store migration.
- Deferred implementation decisions.
- Stop rules for later phases.
- Belongs/authority/contract/behavior acceptance checklist.

The artifact must explicitly state that it does not implement or approve gateway/auth/JWT, auth-service, user-service, role-service, login/session, OAuth, SSO, external IdP integration, route migration, endpoint aliases, permission behavior changes, DTO/VO/entity/schema changes, Redis changes, Kafka changes, frontend changes, Python changes, config mutation, config-store migration, role-store migration, business behavior changes or new feature work.

## 12. Acceptance Conditions

Phase 016 is acceptable only if all conditions hold:

- `docs/harness/20-production-identity-issuer-boundary.md` exists and is the primary durable production identity issuer boundary artifact.
- `docs/harness/handoffs/phase-016-implementation.md` records exact files changed and verification outcomes.
- The artifact is docs-only and does not claim runtime implementation.
- The artifact restates current Phase 013, Phase 014 and Phase 015 facts for headers, defaults, runtime context, role config, backend permission checks, no-explicit-permission read surfaces, frontend gating, current absence of production auth and future-only validator placement.
- The artifact selects or explicitly defers a future production identity issuer direction while labeling it future-only and not current runtime authority.
- The artifact preserves demo-header compatibility without changing current `X-User-Id` or `X-User-Role` behavior.
- The artifact defines user profile source, token claim, token/session, service-principal, service-to-service handoff, role-authority dependency and audit identity readiness gates without changing code, headers, Kafka payloads, DTOs, frontend types or runtime behavior.
- The artifact records dependencies for later production role authority selection without choosing or migrating role authority.
- The artifact preserves every URL, HTTP method, request binding, response envelope, response type, permission key, explicit permission check, intentional no-explicit-permission read surface, frontend route, frontend API function, frontend role utility, localStorage behavior, request-header behavior and TypeScript shape.
- The artifact preserves Phase 005 through Phase 015 constraints.
- The artifact does not choose or implement gateway/auth/JWT, auth-service, user-service, role-service, service extraction, route migration, route aliases, endpoint deletion/rename/consolidation, permission behavior change, role-access config mutation, config-store migration, role-store migration, frontend reshaping, Python behavior change, Kafka/database/Redis changes, permanent modular-monolith architecture or new feature work.
- No Java, Python, frontend, database, Redis, Kafka, `ai-config`, prompt-template, dependency, build-config, deployment, state, debt, backlog, transition lifetime, durable Phase 008/009/010/011/012/013/014/015 artifact or prior handoff file changes occur.
- Any future-phase recommendation is explicitly labeled as deferred and requiring a later Window 0 decision plus human approval.
- `git status --short --untracked-files=all` after Window 2 shows only allowed Phase 016 documentation files as new Window 2 changes, aside from pre-existing unrelated dirty files clearly excluded from the Window 2 claim.

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
rg -n "roleCode|permissionKeys|menuKeys|TASK_CREATE|TASK_RETRY|TASK_CANCEL|AUDIT_COMPLIANCE_VIEW|REPORT_REVIEW|MODEL_AGENT_CONFIG_VIEW|MODEL_AGENT_CONFIG_EDIT|RESEARCHER|PM|RISK_MANAGER|COMPLIANCE_AUDITOR|ADMIN" quant-ai-platform/ai-config/role-access-configs.json
```

Window 2 must run from `D:\projects\bussiness\quant-ui`:

```powershell
node scripts/authority-boundary-check.mjs
```

Expected result: the existing Phase 007 static guard still passes. If it fails, Window 2 must record the failure as a blocker and must not patch frontend code in this phase.

Window 2 must run from `D:\projects\bussiness` after edits:

```powershell
Test-Path docs/harness/20-production-identity-issuer-boundary.md
```

Expected result: `True`.

```powershell
Test-Path docs/harness/handoffs/phase-016-implementation.md
```

Expected result: `True`.

```powershell
rg -n "identity issuer|production identity|gateway|JWT|auth-service|user-service|external IdP|directory|demo header|X-User-Id|X-User-Role|UserContext|service principal|service-to-service|audit identity|Phase 006|Phase 012|Phase 013|Phase 014|Phase 015|deferred|future Window 0|human approval|no behavior change" docs/harness/20-production-identity-issuer-boundary.md docs/harness/handoffs/phase-016-implementation.md
```

Expected result: the durable artifact and implementation handoff contain the required issuer boundary facts, future-only labels and inherited guardrail references.

```powershell
rg -n "implemented|created gateway|created auth-service|created user-service|JWT implemented|session implemented|login implemented|OAuth implemented|SSO implemented|IdP integrated|route migrated|route alias added|permission behavior changed|config mutated|role store migrated|service extracted|database schema changed|Redis changed|Kafka changed|frontend changed|Python changed|business code changed|new feature implemented|permanent modular monolith" docs/harness/20-production-identity-issuer-boundary.md docs/harness/handoffs/phase-016-implementation.md
```

Expected result: there should be no matches that claim implemented work. If matches appear, they must be in explicit no-change, out-of-scope, blocker, deferred-decision, future-target or stop-rule sections.

```powershell
git status --short --untracked-files=all
```

Expected result: Window 2 records the final dirty/untracked state and claims only allowed Phase 016 documentation files, excluding pre-existing unrelated files.

Maven, npm build and Python runtime verification are not required because Phase 016 forbids Java, frontend, Python and test-code changes. If Window 2 touches any such area, it must stop as blocked instead of validating with builds.

## 14. Blocker Stop Rules

Window 2 must stop and write a blocker in `docs/harness/handoffs/phase-016-implementation.md` if any of these become necessary:

- Changing any URL path, HTTP method, endpoint owner, request binding, response envelope, response type, frontend route, frontend API function, TypeScript shape or permission behavior.
- Adding, removing, widening or narrowing any backend `requirePermission` call.
- Adding a permission check to a read-model surface that currently has no explicit check.
- Changing role codes, permission keys, menu keys, role mappings, header names, default header behavior or local role behavior.
- Mutating `role-access-configs.json` or any other config file.
- Adding gateway/auth/JWT/session/login/OAuth/SSO/auth-service/user-service/role-service/external-IdP code.
- Adding route aliases, compatibility bridges, gateway proxies, frontend API adapters, identity resolvers, permission resolvers, auth adapters, role-store bridges, config-store bridges, DB adapters, Nacos adapters, service wrappers, migration runners, dual-write paths, rollback runners or sync jobs.
- Moving identity, permission, role-access, task-create auth, config or route responsibility into another service.
- Creating or modifying Java, Python, frontend, database, Redis, Kafka, config, prompt-template, dependency, test, build or deployment files.
- Changing DTO, VO, entity, mapper, schema, topic, payload, Redis key, JSON shape, prompt-template shape, API type or frontend type shapes.
- Reclassifying frontend localStorage, frontend defaults, request headers, route guards, menu/button gating, audit rows or ingest history rows as production identity or permission authority.
- Declaring `ai-orchestration-service`, JSON config files, header-based demo auth, request headers, frontend role cache or legacy `/api/tasks/*` paths final architecture.
- Closing D001, D002, D003, D007 or D008.
- Selecting a future target in a way that implies current implementation approval, breaking-change approval, permission behavior change, route migration, service extraction, config-store migration, role-store migration, frontend reshaping, Python behavior change, Kafka/database/Redis change or permanent modular-monolith outcome.
- Needing code behavior changes to make the identity issuer boundary artifact true.
- Finding that identity issuer selection cannot be scoped without a broader human decision than Phase 016 approved.
- The existing `node scripts/authority-boundary-check.mjs` guard fails and cannot be treated as an environment-only issue.

When blocked, Window 2 must:

1. Stop edits at the smallest reversible point.
2. Record the exact file, route, permission key, role mapping, consumer, dependency or decision point that forced the blocker.
3. State which acceptance condition cannot be met.
4. State the smallest future phase or human decision required to unblock.
5. Ask for human decision instead of expanding scope.

## Window 2 Shape

Use one docs-only architecture/governance Window 2 implementation window.

Do not partition into backend, frontend or Python implementers. Do not start gateway/auth/JWT work, service extraction, route migration, config migration, role-store migration, test implementation, frontend guard edits, Python edits, Java edits, config edits, deployment edits or product feature work. Do not proceed until the user approves this Phase 016 architect handoff.

## Human Approval Request

Please approve this Phase 016 architect handoff before Window 2 starts.

Default approval meaning:

- No breaking changes.
- Keep all URL paths and frontend routes stable.
- No permission behavior change.
- No business behavior change.
- No new feature work.
- No gateway/auth/JWT implementation.
- No auth-service, user-service, role-service, login/session, OAuth, SSO or external IdP implementation.
- No config mutation.
- Window 2 may perform docs-only production identity issuer boundary work inside the allowed file boundaries above.
