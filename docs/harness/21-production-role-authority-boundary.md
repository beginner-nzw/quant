# Production Role Authority Boundary

## Status And Scope

Phase: Phase 017 - Production Role Authority Selection Boundary.

Status: docs-only governance boundary.

Primary decision: prefer a backend-owned application role authority for future production role assignment, role-permission mapping and menu mapping. External IdP or enterprise directory groups/claims may be future identity-adjacent inputs, but they must not become application role authority until a later approved mapping and validation phase defines the contract.

This decision is future-only. It does not implement or approve gateway/auth/JWT, external IdP integration, directory integration, auth-service, user-service, role-service, login/session, OAuth, SSO, route migration, endpoint aliases, permission behavior changes, DTO/VO/entity/schema changes, Redis changes, Kafka changes, frontend changes, Python changes, config mutation, config-store migration, role-store migration, business behavior changes or new feature work.

All later implementation, integration, compatibility and migration work requires a future Window 0 decision and human approval.

Review order remains:

```text
belongs -> authority -> contract -> behavior
```

## Inputs And Read-Only Inspection Sources

Phase 017 uses these governance inputs:

- `docs/harness/00-project-charter.md`
- `docs/harness/02-authority-matrix.md`
- `docs/harness/03-host-ownership.md`
- `docs/harness/04-contract-map.md`
- `docs/harness/05-transition-lifetime.md`
- `docs/harness/08-eval-checklist.md`
- `docs/harness/09-window-protocol.md`
- `docs/harness/state/current-state.md`
- `docs/harness/handoffs/steering-decision-phase-017.md`
- `docs/harness/handoffs/phase-017-architect.md`
- `docs/harness/handoffs/phase-016-final.md`
- `docs/harness/16-config-store-decision-boundary.md`
- `docs/harness/17-auth-gateway-permission-boundary.md`
- `docs/harness/18-production-auth-gateway-target-scope.md`
- `docs/harness/19-production-identity-issuer-validator-boundary.md`
- `docs/harness/20-production-identity-issuer-boundary.md`

Read-only inventory confirmed current role authority, header, permission and frontend gating surfaces in:

- `quant-ai-platform/ai-config/role-access-configs.json`
- `quant-common-security`
- `ai-orchestration-service`
- `research-task-service`
- `quant-ui`
- `quant-ui/scripts/authority-boundary-check.mjs`

No runtime file was changed.

## Current Inherited Facts

These facts remain unchanged from Phase 012, Phase 013, Phase 014, Phase 015 and Phase 016:

- `role-access-configs.json` is the current transition role/menu/permission config input under the Phase 012 JSON transition-store policy.
- `X-User-Id` and `X-User-Role` are current demo/runtime inputs only.
- Missing backend values still default to `guest` and `USER`.
- `X-Trace-Id` is runtime tracing input, not identity, role or permission authority.
- `UserContext` is request runtime context, not production identity, profile or role authority.
- `SecurityUtils.currentUserId()` and `SecurityUtils.currentUserRole()` read runtime metadata and current permission inputs.
- Backend explicit `requirePermission` calls remain the current enforcement points for checked endpoints.
- Intentional no-explicit-permission read surfaces remain unchanged.
- Frontend route, menu and action gating is UI affordance only.
- Frontend local role state and `quant_current_user` are UI/runtime inputs only.
- Backend-owned ingress/gateway JWT validation is the preferred future validator placement from Phase 015.
- External IdP or enterprise directory is the preferred future production identity issuer direction from Phase 016.
- No production gateway/auth/JWT implementation exists.
- No external IdP integration, directory integration, auth-service, user-service, role-service, login/session, OAuth, SSO or production role store exists.

## Current Role, Permission And Menu Inventory

Current role codes:

- `RESEARCHER`
- `PM`
- `RISK_MANAGER`
- `COMPLIANCE_AUDITOR`
- `ADMIN`

Current permission keys:

- `TASK_VIEW`
- `TASK_CREATE`
- `TASK_RETRY`
- `TASK_CANCEL`
- `AUDIT_COMPLIANCE_VIEW`
- `REPORT_REVIEW`
- `MODEL_AGENT_CONFIG_VIEW`
- `MODEL_AGENT_CONFIG_EDIT`

Current menu keys:

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

Current role-to-permission mapping:

| Role code | Current permission keys |
| --- | --- |
| `RESEARCHER` | `TASK_VIEW`, `TASK_CREATE` |
| `PM` | `TASK_VIEW`, `TASK_CREATE` |
| `RISK_MANAGER` | `TASK_VIEW`, `TASK_CREATE`, `AUDIT_COMPLIANCE_VIEW` |
| `COMPLIANCE_AUDITOR` | `TASK_VIEW`, `TASK_CREATE`, `AUDIT_COMPLIANCE_VIEW`, `REPORT_REVIEW`, `MODEL_AGENT_CONFIG_VIEW` |
| `ADMIN` | `TASK_VIEW`, `TASK_CREATE`, `TASK_RETRY`, `TASK_CANCEL`, `AUDIT_COMPLIANCE_VIEW`, `REPORT_REVIEW`, `MODEL_AGENT_CONFIG_VIEW`, `MODEL_AGENT_CONFIG_EDIT` |

Current coarse access-role compatibility:

| Coarse role | Current business role candidates |
| --- | --- |
| `USER` | `USER`, `RESEARCHER`, `PM`, `RISK_MANAGER` |
| `REVIEWER` | `REVIEWER`, `COMPLIANCE_AUDITOR` |
| `ADMIN` | `ADMIN` |

Frontend coarse role mapping remains:

| Business role | Frontend coarse access role |
| --- | --- |
| `RESEARCHER` | `USER` |
| `PM` | `USER` |
| `RISK_MANAGER` | `USER` |
| `COMPLIANCE_AUDITOR` | `REVIEWER` |
| `ADMIN` | `ADMIN` |

Phase 017 does not add, remove or rename any role code, permission key, menu key, coarse access role, route metadata, API function, backend permission check, frontend localStorage key, header name or default value.

## Production Role Authority Option Matrix

| Option | Future belongs | Authority fit | Contract fit | Phase 017 decision |
| --- | --- | --- | --- | --- |
| External IdP or directory groups/claims | External issuer or directory, validated by backend-owned ingress/gateway | Useful as identity-adjacent input, but risky as direct application role authority because application permissions and menus need platform ownership and audit | Requires claim/group mapping, issuer/audience rules, token validation and rollback before use | Input only, not selected as direct application role authority |
| Backend DB role store | Backend-owned role authority, possibly inside an approved auth/user/role boundary | Strong fit for role assignment audit and application role lifecycle | Requires role-store migration, schema/versioning, compatibility and rollback | Preferred future authority shape for role assignments if a later role-store phase is approved |
| Auth-service, user-service or role-service role ownership | Future backend service owns application roles or role mappings | Strong fit if later service creation or extraction is approved | Requires service boundary, route, propagation, config and migration decisions | Allowed future host family, not created by Phase 017 |
| Config-store-backed role source | Backend-owned config store owns role-permission/menu mapping | Possible fit for mapping configuration after Phase 012 gates | Requires config-store migration, schema, single-writer and audit readiness | Deferred mapping-store candidate |
| Bounded continuation of `role-access-configs.json` | Current JSON transition input remains in place | Conservative and compatible with current behavior | Not final architecture and still leaves D007/D008 open | Preserved as current transition input only |
| Continued role-authority deferral | No selected future direction | Avoids commitment | Leaves role authority ambiguity too broad for later gateway/JWT planning | Rejected as primary outcome because a future backend-owned application role authority can be selected safely |

## Selected Future Role Authority Direction

Phase 017 selects this future-only direction:

- Production application role authority should be backend-owned.
- The preferred future authority shape is a backend-owned application role authority that owns role assignment and application role lifecycle, with a backend-owned role-permission and menu mapping contract.
- A later approved DB role store, auth-service, user-service, role-service or equivalent backend-owned role boundary may host this authority. Phase 017 does not choose or create the concrete host.
- External IdP or enterprise directory groups/claims may be used as future inputs to role assignment only after backend-owned ingress/gateway JWT validation, claim mapping, compatibility and audit rules are approved.
- External claims/groups must not directly define application permission keys, menu keys or backend enforcement unless a later phase explicitly approves that mapping and preserves current contracts.
- `role-access-configs.json` remains the current transition role/menu/permission config input until a later approved role-store or config-store migration changes it.

This direction is not current runtime authority. It does not create a role store, role API, role service, gateway, JWT validator, claim mapper, directory connector, endpoint alias, migration path or production role implementation.

## Rationale

Backend-owned application role authority fits Phase 012 because the current JSON role config remains a transition store and no config-store migration is approved. It fits Phase 015 because trusted production role decisions should sit behind backend-owned ingress/gateway JWT validation, not raw frontend headers. It fits Phase 016 because an external IdP or enterprise directory can issue identity while application-specific authorization remains under backend governance.

This split avoids collapsing identity issuer, identity validator, user profile source, role assignment authority and permission mapping into one premature surface. It keeps application permission keys and menu keys compatible while allowing future external directory claims to inform, but not silently own, application roles.

## Belongs Rules

| Area | Current belongs | Future Phase 017 rule |
| --- | --- | --- |
| Demo role input | Request headers and frontend request utilities | Remain local/demo compatibility inputs only. |
| Runtime user and role context | `UserContextFilter`, `UserContext`, `SecurityUtils` | Remain runtime carriers. They are not production role authority. |
| Current role/menu/permission config input | `role-access-configs.json` | Remains JSON transition input under Phase 012. It is not final role-store architecture. |
| Most checked command permissions | `ai-orchestration-service` / `RoleAccessConfigService` | Remain transition host enforcement points for current checked endpoints. |
| Task-create permission | `research-task-service` / `TaskRoleAccessService` | Remains formal task-create host and permission reader/checker only. |
| UI role selection and gating | `quant-ui` | Remains UI affordance and demo header source only. |
| Future production identity issuer | Not implemented | Preferred future direction remains external IdP or enterprise directory from Phase 016. |
| Future production identity validator | Not implemented | Preferred placement remains backend-owned ingress/gateway JWT validation from Phase 015. |
| Future production role assignment authority | Not implemented | Should belong to a backend-owned application role authority if later approved. |
| Future role-permission and menu mapping authority | Not implemented | Should belong to the same backend-owned role authority or a backend-owned config mapping contract selected by a later phase. |
| Future service principals | Not implemented | Must belong to an approved auth/gateway/service boundary, not frontend utilities or ad hoc headers. |

## Authority Rules

- Identity issuer, identity validator, runtime user context, user profile source, role assignment authority, role-permission mapping authority and frontend UI affordance are distinct concerns.
- External IdP or directory remains the preferred future identity issuer, not current runtime identity authority.
- Backend-owned ingress/gateway JWT validation remains the preferred future validator placement, not current runtime validation.
- The future production role authority direction is backend-owned application role authority, not frontend localStorage, request headers, workbench output, audit metadata or fallback provenance.
- External groups/claims may become role inputs only after a later approved claim/group mapping phase. They must not directly become permission key or menu key authority.
- Role assignment authority and role-permission mapping authority may share a backend host or be split between a backend role store and backend config mapping only after a later approved phase.
- `role-access-configs.json` remains current transition input and is not final role authority.
- Backend `requirePermission` calls remain current enforcement points for checked endpoints.
- Intentional no-explicit-permission read surfaces remain stable and do not gain new authority claims.
- Frontend gating remains UI affordance and must not become backend permission or role truth.
- Audit identity and role auditability are future dependencies only and do not change current audit fields.
- Service-principal and service-to-service role semantics are future dependencies only.
- Phase 017 does not close D001, D002, D003, D007 or D008.

## Backend Enforcement Boundary

Current backend enforcement remains unchanged:

- `POST /api/research/tasks` remains hosted by `research-task-service` and checks `TASK_CREATE` through `TaskRoleAccessService`.
- Existing checked commands continue using current `RoleAccessConfigService.requirePermission` or `TaskRoleAccessService.requirePermission` call sites.
- No explicit `requirePermission` call is added, removed, widened, narrowed, moved or renamed.
- No read-model endpoint receives a new explicit permission check in Phase 017.
- Current no-explicit-permission read surfaces from Phase 013 remain stable.

Future backend role authority must preserve current permission behavior by default. Any permission widening, narrowing, new check, removed check or changed no-explicit-permission surface is a permission behavior change requiring a later Window 0 decision and human approval.

## Frontend Gating Boundary

Current frontend facts remain unchanged:

- `auth.ts` defines local demo roles and stores the selected user under `quant_current_user`.
- `requestHeaders.ts` sends `X-User-Id`, `X-User-Role` and `X-Trace-Id`.
- `roleAccess.ts` defines menu keys, permission keys, local cache key `quant_role_access_configs`, `ROLE_ACCESS_UPDATED_EVENT`, local defaults, role access fetch and UI helper functions.
- Router metadata uses `requiredMenuKey` and `requiredPermissionKey` only as navigation affordance.
- `BasicLayout.vue` and `taskActionAccess.ts` use menu/permission helpers for UI visibility.

Frontend route/menu/action gating remains UI affordance only. It does not become identity issuer, identity validator, role authority, permission-key authority, menu-key authority or backend enforcement. Future frontend reshaping requires later approval and must preserve current routes, API functions, endpoint strings, call signatures, TypeScript shapes, localStorage behavior and request-header behavior unless a breaking change is explicitly approved.

## Compatibility Rules

Stable compatibility rules:

- Current role codes remain stable.
- Current permission keys remain stable.
- Current menu keys remain stable.
- Current coarse access-role mapping remains stable.
- Current backend `requirePermission` calls remain stable.
- Current intentional no-explicit-permission read surfaces remain stable.
- Current header names `X-User-Id`, `X-User-Role` and `X-Trace-Id` remain stable.
- Current missing header defaults `guest` and `USER` remain stable.
- Current frontend selected-user key `quant_current_user` remains stable.
- Current `role-access-configs.json` shape remains stable.
- Current URL paths, HTTP methods, endpoint owners, request bindings, response envelopes and response types remain stable.
- Current frontend routes, API function names, endpoint strings, call signatures and TypeScript shapes remain stable.

Later role authority, gateway/JWT, route migration, service extraction, config-store migration or role-store migration phases must provide an explicit compatibility map before changing any role, permission, menu, header, endpoint, route, DTO, VO, entity, schema, Redis key, Kafka topic, Kafka payload, JSON shape, prompt-template shape, Python payload or frontend type.

## User Profile Source Dependency Rules

Phase 017 does not select a user profile source.

Later phases must decide whether profile facts come from:

- external IdP or directory claims
- a backend user-service
- an auth-service profile store
- a synchronized profile read model
- another approved backend-owned source

The profile source decision must define stable user id mapping, display name, department or organization facts if needed, active/deactivated user treatment, audit display behavior and rollback. It must not be inferred from frontend local role state, demo headers, role-access config, workbench data, fallback metadata, audit rows or report fields.

## Token Claim, Group Claim And Gateway Dependency Rules

Before external groups/claims can inform application role assignment, a later phase must define:

- accepted issuer identifiers and audiences
- gateway/JWT validation and failure behavior
- subject claim mapping to runtime user id
- group/role claim names and allowed values
- mapping from external groups/claims to current application role codes
- whether group/claim data is authoritative, advisory or a synchronization input
- conflict behavior when external claims and backend role records disagree
- local/demo header compatibility and rollback behavior
- audit visibility for claim source, mapping source and operator

Until those decisions are approved, `X-User-Role` and frontend local role state remain local/demo inputs only, and no JWT/session/login/OAuth/SSO behavior is implemented.

## Service Principal And Service-To-Service Role Handoff Dependencies

Future service-to-service role and permission propagation must define:

| Flow | Required future semantics | Current Phase 017 result |
| --- | --- | --- |
| Task creation | Actor identity and application role/permission used for `POST /api/research/tasks` must be trusted by the approved auth boundary and checked by the task-create host. | Requirement only; current `TaskRoleAccessService` behavior stays unchanged. |
| AI callbacks | Python-to-Java callbacks must distinguish service principal identity from original user context and must not derive production user authority from fallback data. | Requirement only; no Kafka payload or callback behavior changes. |
| Event auto task dispatch | System-triggered dispatch must define original actor, system principal, role claim, trace id and audit identity semantics. | Requirement only; current `system`/`ADMIN` fallback behavior stays unchanged. |
| Future extracted services | Extracted services must receive trusted service principal and user delegation context through the approved auth/gateway/service boundary. | Requirement only; no service extraction is approved. |
| Config and source operations | Operator identity and role authority must distinguish human operator from service automation where relevant. | Requirement only; current file-backed audit behavior stays unchanged. |

Service-to-service propagation must not be implemented through ad hoc headers, frontend utilities, workbench output, read models, fallback provenance or one-off helper/adapter/bridge code.

## Audit Identity And Role Auditability Dependencies

Later production role authority work must define audit semantics for:

- human actor identity
- application role assigned to the human actor
- external claim/group input, if used
- backend role assignment source
- delegated actor identity
- service principal identity
- system-triggered action identity
- original actor on async follow-up work
- role assignment changes and role-permission mapping changes
- local/demo actor compatibility

Current audit fields and metadata remain unchanged:

- config audit operator id and role from current request context
- event ingest history operator metadata
- report review reviewer role
- market created-by metadata
- event auto task dispatch current-context or fallback metadata

Phase 017 does not change audit rows, audit payloads, ingest history shape, report review behavior, market created-by behavior or event auto task dispatch behavior.

## Demo Header Compatibility Rules

- `X-User-Id` and `X-User-Role` remain current local/demo compatibility inputs.
- Missing header defaults remain `guest` and `USER`.
- `X-Trace-Id` remains runtime tracing input.
- The frontend selected-user key remains `quant_current_user`.
- Current request-header utility behavior remains unchanged.
- Demo headers may be retained behind a later approved compatibility mode, but retirement is not approved by Phase 017.
- Demo headers must not be trusted as production identity, role or permission authority.
- No current permission check may be widened, narrowed, added, removed or moved as part of this phase.

## Stable URL/API/Permission/Header/Frontend/Config Contract Rules

Phase 017 preserves:

- all URL paths and HTTP methods
- all endpoint owners, request bindings, response envelopes and response types
- all frontend routes
- all frontend API function names, endpoint strings, call signatures and TypeScript shapes
- all role codes, permission keys and menu keys
- all backend `requirePermission` calls
- all intentional no-explicit-permission read surfaces
- all header names and default values
- all frontend localStorage keys and request-header behavior
- all DTO, VO, entity, mapper, schema, Redis key, Kafka topic, Kafka payload, JSON config shape, prompt-template shape and Python payload contracts

Phase 017 adds no auth URL, login URL, callback URL, gateway URL, route alias, compatibility endpoint, proxy route, role endpoint or new namespace.

## Dependency Map

| Prior phase | Preserved dependency |
| --- | --- |
| Phase 006 | Legacy non-task `/api/tasks/*` paths remain frozen transition contracts. No route migration or alias is approved. |
| Phase 012 | JSON config files remain current transition stores. No config mutation, DB/Nacos/hybrid migration or role-store migration is approved. |
| Phase 013 | Permission inventory remains stable. Demo headers and frontend gating remain non-production inputs. |
| Phase 014 | Production identity should enter through a backend-owned ingress/auth boundary, future-only; production role authority must be backend-owned. |
| Phase 015 | Backend-owned ingress/gateway JWT validation remains preferred future validator placement. |
| Phase 016 | External IdP or enterprise directory remains preferred future identity issuer direction. |

## Later Gateway/JWT Implementation Dependencies

Later gateway/JWT implementation design must define:

- concrete gateway or ingress component ownership
- issuer product/vendor or issuer integration
- JWT validation rules and failure behavior
- trusted identity context forwarded to backend services
- external group/claim treatment and mapping source
- backend application role authority lookup or synchronization behavior
- demo-header compatibility or retirement policy
- local/dev profile behavior
- service-to-service principal validation
- audit identity mapping
- rollback strategy
- route and contract compatibility with Phase 006 inventory
- test and static guard scope

Phase 017 only selects a future role authority direction. It does not start gateway/JWT design or implementation.

## Later User Profile Source Dependencies

Later user profile source selection must define:

- profile authority host
- stable user id mapping from external issuer subject or backend user id
- display name and organization fields, if needed
- active/deactivated user treatment
- relationship to role assignment records and role audit
- audit display behavior
- rollback and local/demo compatibility

Phase 017 does not select or implement profile ownership.

## Later Route, Service, Config-Store And Role-Store Dependencies

Later route migration depends on:

- production auth/gateway implementation selected by a future Window 0 decision and human approval
- compatibility or breaking-change decision for current URLs
- updated Phase 006 contract inventory if endpoints move, alias, split, merge, rename or gain new permission behavior
- demo-header compatibility or retirement decision

Later service extraction depends on:

- selected identity authority and role authority
- service-to-service propagation semantics for actor, service principal, role claims and trace/audit metadata
- route and contract migration readiness
- domain-specific readiness gates from Phase 008 through Phase 011
- config-store and role-store ownership decisions where relevant

Later config-store or role-store migration depends on:

- Phase 012 schema/versioning, single-writer, audit retention, rollback and Java/Python/frontend reader compatibility readiness
- a later Window 0 decision and human approval selecting DB, Nacos, hybrid, auth-service/user-service, role-service, external directory, IdP claims or another role authority target
- compatibility plan for `role-access-configs.json`, permission keys, menu keys, role codes and current API contracts
- migration and rollback plan that does not silently widen or narrow permissions

Phase 017 does not approve route migration, service extraction, endpoint aliases, gateway proxy work, config-store migration, role-store migration or permanent modular-monolith architecture.

## Deferred Implementation Decisions

Deferred decisions requiring a future Window 0 decision and human approval:

- concrete backend production role authority host
- DB role store, role-service, auth-service, user-service or config-store-backed mapping target
- external group/claim mapping contract
- role assignment schema and role-permission mapping schema
- gateway/auth/JWT implementation design
- external IdP or directory product/vendor integration
- auth-service, user-service, role-service or session-service creation
- login/session/OAuth/SSO integration
- token claim schema and session lifecycle
- user profile source
- service-principal issuance and validation
- service-to-service identity and role handoff
- audit identity and role-audit field model
- demo-header compatibility or retirement
- route migration or endpoint aliasing
- config-store or role-store migration
- service extraction

## Stop Rules For Later Phases

Stop and return to Window 0 if a later phase needs any of these without explicit approval:

- changing URLs, methods, endpoint owners, request bindings, envelopes or response types
- changing frontend routes, API functions, endpoint strings, call signatures or TypeScript shapes
- changing header names, defaults, role codes, permission keys or menu keys
- adding, removing, moving, widening or narrowing a `requirePermission` check
- adding permissions to current no-explicit-permission reads
- mutating `role-access-configs.json` or any config file
- implementing gateway/auth/JWT/session/login/OAuth/SSO/external IdP integration or directory integration
- creating auth-service, user-service or role-service
- changing DTOs, VOs, entities, mappers, schemas, Kafka topics, Kafka payloads, Redis keys, frontend types or Python payloads
- creating route aliases, adapters, bridges, resolvers, wrappers, compatibility endpoints, sync jobs, dual-write paths or rollback runners
- declaring demo headers, frontend localStorage, JSON role config, legacy `/api/tasks/*` paths or the modular monolith final architecture
- closing D001, D002, D003, D007 or D008

## Acceptance Checklist

Belongs:

- [x] Future production role authority direction is backend-owned application role authority, future-only.
- [x] External IdP or directory remains future identity issuer direction, not direct application role authority.
- [x] Future validator belongs remains backend-owned ingress/gateway JWT validation, future-only.
- [x] Current `UserContext` belongs remains runtime carrier only.
- [x] Current demo headers remain local/demo compatibility inputs only.

Authority:

- [x] No current role source of truth is introduced.
- [x] `role-access-configs.json` remains current transition permission config input only.
- [x] Frontend gating and localStorage remain UI/runtime inputs only.
- [x] External groups/claims are limited to future inputs pending later mapping approval.

Contract:

- [x] URL, API, header, permission, role code, menu key, frontend and payload contracts remain stable.
- [x] No gateway/auth/JWT/session/login/OAuth/SSO endpoint or route is added.
- [x] Phase 006, Phase 012, Phase 013, Phase 014, Phase 015 and Phase 016 constraints remain in force.

Behavior:

- [x] No Java, Python, frontend, config, database, Redis, Kafka, dependency, build, deployment or runtime file changed.
- [x] No permission behavior change.
- [x] No business behavior change.
- [x] All implementation work is deferred to later phases requiring future Window 0 selection and human approval.
