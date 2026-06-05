# Phase 018 Architect Handoff

## Status

Window: Window 1 - Phase Architect.

Phase: Phase 018 - Consolidated Remaining Governance Closure.

This handoff is architecture planning only. It does not authorize implementation. Window 2 may start only after the user explicitly approves this file.

Phase 018 is not Phase 001, so the Phase 001 special Java/frontend reading list is not the controlling implementation scope for this handoff.

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
- `docs/harness/handoffs/steering-decision-phase-018.md`

Additional durable boundary inputs:

- `docs/harness/16-config-store-decision-boundary.md`
- `docs/harness/17-auth-gateway-permission-boundary.md`
- `docs/harness/18-production-auth-gateway-target-scope.md`
- `docs/harness/19-production-identity-issuer-validator-boundary.md`
- `docs/harness/20-production-identity-issuer-boundary.md`
- `docs/harness/21-production-role-authority-boundary.md`
- `docs/harness/handoffs/phase-016-architect.md`
- `docs/harness/handoffs/phase-017-architect.md`

## 1. Phase Goal

Produce one docs-only consolidated remaining-governance closure artifact:

- Required durable artifact: `docs/harness/22-remaining-governance-closure.md`.
- Required Window 2 handoff: `docs/harness/handoffs/phase-018-implementation.md`.
- Consolidate the remaining pre-implementation governance decisions and readiness gates after Phase 012 through Phase 017.
- Close, at documentation/governance level only, the remaining decision gaps for:
  - concrete production role authority host family and mapping boundary
  - user profile source boundary
  - service-to-service propagation and audit identity semantics
  - gateway/JWT implementation-design prerequisites and demo-header compatibility policy shape
  - config-store and role-store migration readiness gates
  - route migration readiness and breaking-change prerequisites
  - sequencing map for later implementation-oriented phases
- State which later implementation candidates may be scored by a future Window 0, without approving any implementation.

Window 2 must evaluate every statement in this order:

```text
belongs -> authority -> contract -> behavior
```

This phase is docs-only. It must not implement gateway/auth/JWT, external IdP integration, directory integration, auth-service, user-service, role-service, login/session, OAuth, SSO, route migration, endpoint aliases, permission behavior changes, config mutation, role-store migration, config-store migration, tests, static guards or business code.

## 2. Allowed File Scope

Window 2 may modify only:

- `docs/harness/22-remaining-governance-closure.md`
- `docs/harness/handoffs/phase-018-implementation.md`

Window 2 may read, but must not write:

- required harness inputs listed above
- durable Phase 012 through Phase 017 artifacts listed above
- prior handoffs needed for context
- Java, frontend, Python, config and test files needed for read-only inventory commands

Window 4 may later update state, debt, backlog or transition lifetime after Window 3 approval. Window 2 must not do that in Phase 018.

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
- durable Phase 012 through Phase 017 artifacts
- prior phase handoffs, including `steering-decision-phase-018.md`

If satisfying Phase 018 appears to require any forbidden file, Window 2 must stop as blocked.

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

- `role-access-configs.json` remains the current transition role/menu/permission config input under Phase 012.
- Header-based demo auth remains a transition mechanism, not production security.
- `X-User-Id` and `X-User-Role` remain local/demo compatibility inputs only.
- `UserContext` remains runtime context, not production identity, profile or role authority.
- Backend-owned ingress/gateway JWT validation remains only the preferred future validator placement from Phase 015.
- External IdP or enterprise directory remains only the preferred future production identity issuer direction from Phase 016.
- Backend-owned application role authority remains only the preferred future production role direction from Phase 017.
- External groups/claims remain future inputs only until later approved mapping, validation, compatibility and audit rules exist.
- `ai-orchestration-service` remains a transition host, not final architecture.
- Legacy `/api/tasks/*` remains a frozen transition namespace, not final route architecture.
- Phase 018 must not close D001, D002, D003, D007 or D008.

## 5. Allowed New Class / Method Types

No business-code classes or methods are allowed.

Allowed documentation-only structures:

- Markdown sections and tables.
- Current inherited fact inventory from Phase 012 through Phase 017.
- Consolidated option matrix for remaining governance closure decisions.
- Governance-only selection or bounded deferral for concrete role authority host family and role mapping boundary.
- Governance-only user profile source boundary and dependency rules.
- Governance-only service-to-service propagation and audit identity semantic rules.
- Gateway/JWT implementation-design prerequisites and demo-header compatibility policy options.
- Config-store and role-store migration readiness gates.
- Route migration readiness gates and breaking-change prerequisites.
- Later implementation sequencing map for future Window 0 scoring.
- Compatibility maps for role codes, permission keys, menu keys, headers, current frontend route/menu/action gating and current backend checks.
- Deferred-decision list.
- Explicit out-of-scope list.
- Stop-rule list.
- Belongs/authority/contract/behavior acceptance checklist.

Allowed scripts/classes/methods:

- None.

Window 2 must not add Java classes, Java tests, frontend scripts, Python scripts, build steps, runtime code, config files, migration files, static guard scripts, helper classes or adapters in this phase.

## 6. Forbidden Helpers / Adapters / Fallbacks / Bridges

Do not add, approve or imply current implementation of:

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

Current belongs baseline inherited from Phase 012 through Phase 017:

| Area | Current host | Phase 018 boundary |
| --- | --- | --- |
| Role/menu/permission config input | `quant-ai-platform/ai-config/role-access-configs.json` | Current JSON transition input. Not final role-store architecture. |
| Request context plumbing | `quant-common-security` | Carries request user, role and trace context. Not production identity, profile or role authority. |
| Demo identity and role inputs | request headers / frontend request utilities | `X-User-Id` and `X-User-Role` remain local/demo compatibility inputs only. |
| Runtime user and role context | `UserContextFilter`, `UserContext`, `SecurityUtils` | Runtime carrier only. Not production identity, profile or role authority. |
| Most checked command permissions | `ai-orchestration-service` / `RoleAccessConfigService` | Transition host enforcement for current checked endpoints. Not final auth or role service. |
| Task-create permission | `research-task-service` / `TaskRoleAccessService` | Formal task-create host and permission reader only. Not auth or role config owner. |
| UI role selection and gating | `quant-ui` | UI affordance and demo header source only. Not identity, role or permission SoT. |
| Future production identity issuer | not implemented | Preferred future direction is external IdP or enterprise directory from Phase 016. |
| Future production identity validator | not implemented | Preferred placement is backend-owned ingress/gateway JWT validation from Phase 015. |
| Future production role authority | not implemented | Preferred future direction is backend-owned application role authority from Phase 017. |
| Future user profile source | not implemented | Phase 018 may select or defer a boundary at governance level only. |
| Future service principals and service-to-service identity/role handoff | not implemented | Phase 018 may define semantics and readiness gates only. |

Phase 018 may document future belongs options only:

- Backend DB role store, auth-service, user-service, role-service, config-store-backed mapping or bounded continuation of `role-access-configs.json` as role-host family options.
- External IdP or directory groups/claims as future inputs, not direct current application permission/menu authority.
- User profile source options such as directory claims, user-service, auth-service profile store, synchronized profile read model or another backend-owned source.
- Service-principal and delegation context as future backend-owned auth/gateway/service boundary responsibilities.

Phase 018 must not create any of these future hosts.

## 8. Authority

Current authority facts that must remain stable:

| Surface | Current meaning | Current classification |
| --- | --- | --- |
| `role-access-configs.json` | role/menu/permission config input | JSON transition store; current permission config input |
| `X-User-Id` | request user id header | demo/runtime input, not production identity |
| `X-User-Role` | request role header | demo/runtime input, not production role authority |
| `X-Trace-Id` | request trace header | runtime tracing input, not identity, role or permission authority |
| `UserContext` | request-scoped user id and role carrier | runtime context, not SoT |
| `SecurityUtils.currentUserId()` | current request user id reader | runtime metadata reader |
| `SecurityUtils.currentUserRole()` | current request role reader | current permission input reader |
| `RoleAccessConfigService` | permission checker and role-access config mutator in `ai-orchestration-service` | transition host service, not final auth or role service |
| `TaskRoleAccessService` | task-create permission checker in `research-task-service` | reader/checker only, not role config owner |
| frontend local role utilities | selected local demo user and role normalization | UI/runtime input only |
| frontend route/menu/action gating | UI visibility and navigation affordance | not backend enforcement or role authority |

Phase 018 governance-only authority rules:

- The artifact must distinguish identity issuer, identity validator, runtime user context, user profile source, role assignment authority, role-permission mapping authority, menu mapping authority, backend enforcement and frontend UI affordance.
- Any selected future host family or mapping boundary must be labeled future-only and not current runtime authority.
- `role-access-configs.json` remains current transition input and must not be reclassified as final architecture.
- External claims/groups may be future inputs only and must require later approved gateway/JWT validation, claim/group mapping, compatibility and audit rules before affecting application roles.
- User profile facts must not be inferred from frontend local role state, demo headers, role-access config, workbench data, fallback metadata, audit rows or report fields.
- Service principal, delegated actor and system actor semantics may be defined as future requirements only and must not change current audit fields, Kafka payloads or callback behavior.
- Backend `requirePermission` calls remain current enforcement points for checked endpoints and must not change.
- Intentional no-explicit-permission read surfaces remain stable and must not receive new authority claims.
- Frontend gating remains UI affordance and must not become backend permission or role truth.
- No documentation in Phase 018 may close D001, D002, D003, D007 or D008.

Forbidden authority changes:

- No new current identity, profile, role, permission or config SoT may be introduced.
- No current request header may be reclassified as production identity or role authority.
- No JWT, gateway, auth-service, user-service, role-service, external IdP, directory, DB role store, config store or role store may be documented as implemented current authority.
- No frontend cache, localStorage, route guard, menu gating or action gating may become backend identity, role or permission truth.
- No read model, workbench output, audit row, ingest history row, fallback provenance, report metadata or Kafka callback may become identity, profile or role authority.
- No `role-access-configs.json` mutation, config-store migration or role-store migration may occur.
- No documentation may claim header-based demo auth, JSON role config, legacy `/api/tasks/*`, `ai-orchestration-service` or the modular monolith is final architecture.

## 9. Contract

Stable contract rules:

- Every endpoint keeps the same path, HTTP method, controller owner, request binding, response envelope, response type and permission behavior.
- `POST /api/research/tasks` remains hosted by `research-task-service` and keeps current `TASK_CREATE` behavior through `TaskRoleAccessService`.
- Existing checked commands keep current explicit permission checks.
- Intentional no-explicit-permission read surfaces from Phase 013 remain unchanged.
- No auth URL, gateway URL, role URL, user/profile URL, login URL, callback URL, route alias, compatibility endpoint, proxy route or new namespace is added.
- No endpoint is deleted, renamed, consolidated, split or moved.
- No permission key, menu key, role code, coarse access-role mapping, header name or default value is added, removed or renamed.
- No existing backend `requirePermission` call is added, removed, widened, narrowed, moved or renamed.
- No explicit permission check is added to a read-model endpoint that currently has none.
- `X-User-Id`, `X-User-Role`, `X-Trace-Id`, `guest`, `USER` and `quant_current_user` behavior remain stable.
- Frontend route metadata, API function names, endpoint strings, call signatures, TypeScript shapes, role localStorage key, request-header utility, menu gating and action gating behavior remain stable.
- `role-access-configs.json` file shape remains stable.
- DTO, VO, entity, mapper, schema, Kafka topic, Kafka payload, Redis key, JSON config shape, prompt-template shape, Python payload and runtime settings remain stable.

Contract language allowed in the Phase 018 artifact:

- future role host family options and selected/deferred mapping boundary
- future user profile source options and selected/deferred profile boundary
- future service-principal, delegated-actor, original-actor and audit identity semantics
- future gateway/JWT design prerequisites and demo-header compatibility modes
- future config-store and role-store migration gates
- future route migration readiness gates and breaking-change prerequisites
- future implementation sequencing candidates for later Window 0 scoring
- future rollback and compatibility constraints

Contract language forbidden in the Phase 018 artifact:

- claiming a new current auth, role, user, profile or gateway endpoint exists
- claiming gateway/JWT/session/login/OAuth/SSO behavior is implemented
- claiming auth-service, user-service, role-service, external IdP, directory integration, DB role store, config store or role store exists
- claiming demo headers are retired now
- claiming current read endpoints now require permissions
- claiming route migration, endpoint aliasing or breaking changes are approved
- claiming frontend UI gating is backend enforcement
- claiming `role-access-configs.json` is final role authority
- claiming later implementation candidates are approved for implementation

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

Phase 018 is acceptable only as static governance documentation plus a Window 2 implementation handoff.

## 11. Required Remaining Governance Closure Artifact Shape

`docs/harness/22-remaining-governance-closure.md` must include:

- Status and scope.
- Inputs and read-only inspection sources.
- Current inherited facts from Phase 012 through Phase 017:
  - `role-access-configs.json` is the current transition role/menu/permission config input.
  - `X-User-Id` and `X-User-Role` are demo/runtime inputs only.
  - missing backend values default to `guest` and `USER`.
  - `UserContext` is current runtime context, not production identity, profile or role authority.
  - backend explicit `requirePermission` calls are current enforcement points.
  - intentional no-explicit-permission read surfaces remain stable.
  - frontend route/menu/action gating is UI affordance only.
  - backend-owned ingress/gateway JWT validation is the preferred future validator placement.
  - external IdP or enterprise directory is the preferred future issuer direction.
  - backend-owned application role authority is the preferred future role direction.
  - no production gateway/auth/JWT, external IdP integration, directory integration, auth-service, user-service, role-service, role store, config-store migration or route migration exists.
- Consolidated current role-code, permission-key, menu-key and coarse access-role inventory, or a pointer to Phase 017 with an explicit stable-compatibility statement.
- Remaining decision closure matrix for:
  - concrete role authority host family and role mapping boundary
  - user profile source boundary
  - service-to-service propagation and audit identity semantics
  - gateway/JWT implementation-design prerequisites and demo-header compatibility policy
  - config-store and role-store migration readiness gates
  - route migration readiness and breaking-change prerequisites
- Governance-only selected direction, explicit bounded deferral or readiness gate for each closure area.
- Rationale explaining why each closure decision remains docs-only and fits Phase 006, Phase 012, Phase 013, Phase 014, Phase 015, Phase 016 and Phase 017 constraints.
- Belongs rules separating identity issuer, identity validator, user profile source, role assignment authority, role-permission mapping authority, menu mapping authority, backend enforcement, frontend gating, service principals and audit identity.
- Authority rules that preserve current transition inputs and label all new target directions future-only.
- Stable URL/API/permission/header/frontend/config contract rules.
- Demo-header compatibility policy shape, with retirement or translation deferred to later approved implementation.
- Service-principal, delegated actor, original actor, system actor and audit identity semantic requirements.
- Config-store and role-store migration prerequisites and rollback/readiness gates.
- Route migration prerequisites, compatibility inventory requirements and breaking-change gate rules.
- Sequencing map for later implementation-oriented phases that future Window 0 may score, explicitly without approving implementation.
- Deferred implementation decisions.
- Stop rules for later phases.
- Belongs/authority/contract/behavior acceptance checklist.

The artifact must explicitly state that it does not implement or approve gateway/auth/JWT, external IdP integration, directory integration, auth-service, user-service, role-service, login/session, OAuth, SSO, route migration, endpoint aliases, permission behavior changes, DTO/VO/entity/schema changes, Redis changes, Kafka changes, frontend changes, Python changes, config mutation, config-store migration, role-store migration, business behavior changes, service extraction or new feature work.

## 12. Acceptance Conditions

Phase 018 is acceptable only if all conditions hold:

- `docs/harness/22-remaining-governance-closure.md` exists and is the primary durable remaining-governance closure artifact.
- `docs/harness/handoffs/phase-018-implementation.md` records exact files changed and verification outcomes.
- The artifact is docs-only and does not claim runtime implementation.
- The artifact restates or precisely references current Phase 012 through Phase 017 facts for role config, headers, defaults, runtime context, backend permission checks, no-explicit-permission read surfaces, frontend gating, current absence of production auth/role/profile infrastructure, future-only validator placement, future-only issuer direction and future-only role authority direction.
- The artifact closes remaining governance decision gaps at documentation level only, or explicitly defers any unsafe closure with narrower readiness criteria.
- Any selected direction for role host, profile source, service propagation, audit identity, gateway/JWT prerequisites, config-store/role-store migration gates or route migration gates is labeled future-only and not current runtime authority.
- The artifact preserves demo-header compatibility without changing current `X-User-Id` or `X-User-Role` behavior.
- The artifact defines service principal, service-to-service handoff, audit identity, role-store/config-store and route-readiness gates without changing code, headers, Kafka payloads, DTOs, frontend types or runtime behavior.
- The artifact preserves every URL, HTTP method, request binding, response envelope, response type, permission key, explicit permission check, intentional no-explicit-permission read surface, frontend route, frontend API function, frontend role utility, localStorage behavior, request-header behavior, TypeScript shape and `role-access-configs.json` shape.
- The artifact preserves Phase 005 through Phase 017 constraints.
- The artifact does not choose or implement gateway/auth/JWT, external IdP integration, directory integration, auth-service, user-service, role-service, service extraction, route migration, route aliases, endpoint deletion/rename/consolidation, permission behavior change, role-access config mutation, config-store migration, role-store migration, frontend reshaping, Python behavior change, Kafka/database/Redis changes, permanent modular-monolith architecture or new feature work.
- No Java, Python, frontend, database, Redis, Kafka, `ai-config`, prompt-template, dependency, build-config, deployment, state, debt, backlog, transition lifetime, durable Phase 012/013/014/015/016/017 artifact or prior handoff file changes occur.
- Any future-phase recommendation is explicitly labeled as deferred and requiring a later Window 0 decision plus human approval.
- `git status --short --untracked-files=all` after Window 2 shows only allowed Phase 018 documentation files as new Window 2 changes, aside from pre-existing unrelated dirty files clearly excluded from the Window 2 claim.

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
Test-Path docs/harness/22-remaining-governance-closure.md
```

Expected result: `True`.

```powershell
Test-Path docs/harness/handoffs/phase-018-implementation.md
```

Expected result: `True`.

```powershell
rg -n "remaining governance|role authority|user profile|service-to-service|audit identity|gateway|JWT|demo header|config-store|role-store|route migration|breaking change|role-access-configs|permission key|menu key|role code|requirePermission|no-explicit-permission|frontend gating|X-User-Id|X-User-Role|UserContext|external IdP|directory|Phase 006|Phase 012|Phase 013|Phase 014|Phase 015|Phase 016|Phase 017|deferred|future Window 0|human approval|no behavior change" docs/harness/22-remaining-governance-closure.md docs/harness/handoffs/phase-018-implementation.md
```

Expected result: the durable artifact and implementation handoff contain the required closure facts, future-only labels and inherited guardrail references.

```powershell
rg -n "implemented|created gateway|created auth-service|created user-service|created role-service|JWT implemented|session implemented|login implemented|OAuth implemented|SSO implemented|IdP integrated|directory integrated|role store created|role store migrated|route migrated|route alias added|permission behavior changed|permission widened|permission narrowed|config mutated|config-store migrated|service extracted|database schema changed|Redis changed|Kafka changed|frontend changed|Python changed|business code changed|new feature implemented|permanent modular monolith" docs/harness/22-remaining-governance-closure.md docs/harness/handoffs/phase-018-implementation.md
```

Expected result: there should be no matches that claim implemented work. If matches appear, they must be in explicit no-change, out-of-scope, blocker, deferred-decision, future-target or stop-rule sections.

```powershell
git status --short --untracked-files=all
```

Expected result: Window 2 records the final dirty/untracked state and claims only allowed Phase 018 documentation files, excluding pre-existing unrelated files.

Maven, npm build and Python runtime verification are not required because Phase 018 forbids Java, frontend, Python and test-code changes. If Window 2 touches any such area, it must stop as blocked instead of validating with builds.

## 14. Blocker Stop Rules

Window 2 must stop and write a blocker in `docs/harness/handoffs/phase-018-implementation.md` if any of these become necessary:

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
- Reclassifying frontend localStorage, frontend defaults, request headers, route guards, menu/button gating, audit rows or ingest history rows as production identity, profile, role or permission authority.
- Declaring `ai-orchestration-service`, JSON config files, header-based demo auth, request headers, frontend role cache or legacy `/api/tasks/*` paths final architecture.
- Closing D001, D002, D003, D007 or D008.
- Selecting a future target in a way that implies current implementation approval, breaking-change approval, permission behavior change, route migration, service extraction, config-store migration, role-store migration, frontend reshaping, Python behavior change, Kafka/database/Redis change or permanent modular-monolith outcome.
- Needing code behavior changes to make the remaining-governance closure artifact true.
- Finding that consolidated governance closure cannot be scoped without a broader human decision than Phase 018 approved.
- Finding that a closure area needs a separate steering decision instead of docs-only consolidation.
- The existing `node scripts/authority-boundary-check.mjs` guard fails and cannot be treated as an environment-only issue.

When blocked, Window 2 must:

1. Stop edits at the smallest reversible point.
2. Record the exact file, route, permission key, role mapping, consumer, dependency or decision point that forced the blocker.
3. State which acceptance condition cannot be met.
4. State the smallest future phase or human decision required to unblock.
5. Ask for human decision instead of expanding scope.

## Window 2 Shape

Use one docs-only architecture/governance Window 2 implementation window.

Do not partition into backend, frontend or Python implementers. Do not start gateway/auth/JWT work, external IdP integration, directory integration, service extraction, route migration, config migration, role-store migration, test implementation, frontend guard edits, Python edits, Java edits, config edits, deployment edits or product feature work. Do not proceed until the user approves this Phase 018 architect handoff.

## Human Approval Request

Please approve this Phase 018 architect handoff before Window 2 starts.

Default approval meaning:

- No breaking changes.
- Keep all URL paths and frontend routes stable.
- No permission behavior change.
- No business behavior change.
- No new feature work.
- No gateway/auth/JWT implementation.
- No external IdP or directory integration.
- No auth-service, user-service, role-service, login/session, OAuth or SSO implementation.
- No config mutation.
- No role-store or config-store migration.
- Window 2 may perform docs-only remaining-governance closure work inside the allowed file boundaries above.
