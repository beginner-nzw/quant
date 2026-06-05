# Remaining Governance Closure

## Status And Scope

Phase: Phase 018 - Consolidated Remaining Governance Closure.

Status: docs-only governance closure artifact.

This artifact consolidates the remaining pre-implementation governance decisions after Phase 012 through Phase 017. It closes decision gaps only at documentation level and prepares later implementation-oriented phases for future Window 0 scoring. It does not implement or approve gateway/auth/JWT, external IdP integration, directory integration, auth-service, user-service, role-service, login/session, OAuth, SSO, route migration, endpoint aliases, permission behavior changes, DTO/VO/entity/schema changes, Redis changes, Kafka changes, frontend changes, Python changes, config mutation, config-store migration, role-store migration, business behavior changes, service extraction or new feature work.

All later implementation requires a future Window 0 decision and human approval.

Review order remains:

```text
belongs -> authority -> contract -> behavior
```

## Inputs And Read-Only Inspection Sources

Harness and phase inputs:

- `docs/harness/00-project-charter.md`
- `docs/harness/02-authority-matrix.md`
- `docs/harness/03-host-ownership.md`
- `docs/harness/04-contract-map.md`
- `docs/harness/05-transition-lifetime.md`
- `docs/harness/08-eval-checklist.md`
- `docs/harness/09-window-protocol.md`
- `docs/harness/state/current-state.md`
- `docs/harness/handoffs/steering-decision-phase-018.md`
- `docs/harness/handoffs/phase-018-architect.md`
- `docs/harness/16-config-store-decision-boundary.md`
- `docs/harness/17-auth-gateway-permission-boundary.md`
- `docs/harness/18-production-auth-gateway-target-scope.md`
- `docs/harness/19-production-identity-issuer-validator-boundary.md`
- `docs/harness/20-production-identity-issuer-boundary.md`
- `docs/harness/21-production-role-authority-boundary.md`

Read-only inventory confirmed current role config, headers, permission checks, frontend gating and guard behavior in:

- `quant-ai-platform/ai-config/role-access-configs.json`
- `quant-common-security`
- `ai-orchestration-service`
- `research-task-service`
- `quant-ui`
- `quant-ui/scripts/authority-boundary-check.mjs`

No runtime file was changed.

## Current Inherited Facts

These facts remain stable from Phase 012 through Phase 017:

- `role-access-configs.json` is the current transition role/menu/permission config input.
- `X-User-Id` and `X-User-Role` are demo/runtime inputs only.
- Missing backend values default to `guest` and `USER`.
- `X-Trace-Id` is runtime tracing input, not identity, profile, role or permission authority.
- `UserContext` is current runtime context, not production identity, profile or role authority.
- `SecurityUtils.currentUserId()` and `SecurityUtils.currentUserRole()` read runtime context values.
- Backend explicit `requirePermission` calls are current enforcement points for checked endpoints.
- Intentional no-explicit-permission read surfaces remain stable.
- Frontend route/menu/action gating and `quant_current_user` are UI affordance and demo runtime inputs only.
- Backend-owned ingress/gateway JWT validation is the preferred future validator placement from Phase 015.
- External IdP or enterprise directory is the preferred future identity issuer direction from Phase 016.
- Backend-owned application role authority is the preferred future role direction from Phase 017.
- No production gateway/auth/JWT, external IdP integration, directory integration, auth-service, user-service, role-service, role store, config-store migration or route migration exists.
- D001, D002, D003, D007 and D008 remain open.

## Stable Role, Permission And Menu Compatibility

Phase 018 does not add, remove or rename role codes, permission keys, menu keys, coarse access roles, frontend route metadata, API functions, backend permission checks, header names, default values or localStorage keys.

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

The current role-to-permission, role-to-menu and coarse access-role mapping remains the inventory recorded in Phase 017. Phase 018 points to that inventory as the compatibility baseline and does not duplicate it as a new authority.

## Remaining Decision Closure Matrix

| Closure area | Governance-only Phase 018 outcome | Why docs-only |
| --- | --- | --- |
| Concrete role authority host family and role mapping boundary | Select backend-owned application role authority as the future host family direction. Concrete host remains bounded to later choices such as DB role store, auth-service, user-service, role-service or backend-owned config mapping. | Phase 017 selected the direction, but no concrete host, schema, API, migration or service is approved. |
| User profile source boundary | Defer concrete source. Allowed future candidates are external IdP/directory claims, backend user-service, auth-service profile store, synchronized profile read model or another backend-owned source. | Profile facts require user id mapping, active/deactivated behavior, display fields and audit semantics before implementation. |
| Service-to-service propagation and audit identity semantics | Define required future semantics for service principal, delegated actor, original actor and system actor. | Current headers, Kafka payloads, audit rows and callbacks must not change in this phase. |
| Gateway/JWT implementation-design prerequisites and demo-header compatibility policy | Gateway/JWT remains future-only. Later design must define issuer, validator, token claims, failure behavior, trusted backend context, demo-header compatibility and rollback. | Phase 018 cannot implement gateway/auth/JWT or retire demo headers. |
| Config-store and role-store migration readiness gates | Require schema/versioning, single-writer rules, audit retention, rollback, Java/Python/frontend compatibility and migration validation before any store migration. | Phase 012 keeps JSON config and prompt files as current transition stores. |
| Route migration readiness and breaking-change prerequisites | Require Phase 006 inventory compatibility, auth/gateway readiness, endpoint owner plan, frontend route/API compatibility and explicit breaking-change approval before route movement. | Legacy `/api/tasks/*` paths remain frozen transitional contracts. |
| Later implementation sequencing | Provide future Window 0 scoring candidates only. | Future candidates are not implementation approval. |

## Belongs Rules

| Area | Current belongs | Phase 018 closure rule |
| --- | --- | --- |
| Identity issuer | Not implemented | Future preferred direction remains external IdP or enterprise directory. It is not current authority. |
| Identity validator | Not implemented | Future preferred placement remains backend-owned ingress/gateway JWT validation. It is not current behavior. |
| Runtime user context | `quant-common-security` | `UserContext`, `UserContextFilter` and `SecurityUtils` remain runtime carriers only. |
| User profile source | Not implemented | Must be selected later as a backend-owned or validated external profile boundary. |
| Role assignment authority | Not implemented | Future direction is backend-owned application role authority. Concrete host is deferred. |
| Role-permission mapping authority | `role-access-configs.json` as current transition input | Future mapping must be backend-owned and compatibility-preserving. Current JSON shape remains stable. |
| Menu mapping authority | `role-access-configs.json` and frontend consumers as current transition inputs | Future mapping must preserve current menu keys and UI affordance boundaries. |
| Backend enforcement | Existing backend `requirePermission` call sites | Current checked endpoints remain unchanged. |
| Frontend gating | `quant-ui` | UI affordance only, not identity, role, permission or menu source of truth. |
| Service principals and audit identity | Not implemented | Future backend-owned auth/gateway/service boundary responsibility only. |

## Authority Rules

- Identity issuer, identity validator, runtime user context, user profile source, role assignment authority, role-permission mapping authority, menu mapping authority, backend enforcement and frontend UI affordance remain separate concerns.
- `role-access-configs.json` remains the current transition role/menu/permission config input and is not final role authority.
- `X-User-Id`, `X-User-Role`, `X-Trace-Id`, `guest`, `USER`, frontend localStorage and route/menu/action gating remain local/demo or runtime inputs only.
- External IdP or directory groups/claims may be future inputs only after gateway/JWT validation, claim/group mapping, compatibility and audit rules are approved.
- User profile facts must not be inferred from frontend local role state, demo headers, role-access config, workbench data, fallback metadata, audit rows or report fields.
- Service principal, delegated actor, original actor and system actor semantics are future requirements only.
- Backend `requirePermission` calls remain current enforcement points for checked endpoints.
- Intentional no-explicit-permission read surfaces remain stable and must not receive new authority claims.
- Frontend gating remains UI affordance and must not become backend permission or role truth.
- Phase 018 does not introduce any new current identity, profile, role, permission or config source of truth.
- Phase 018 does not close D001, D002, D003, D007 or D008.

## Stable Contract Rules

Phase 018 preserves:

- all URL paths and HTTP methods
- all endpoint owners, request bindings, response envelopes and response types
- all frontend routes
- all frontend API function names, endpoint strings, call signatures and TypeScript shapes
- all role codes, permission keys, menu keys and coarse access-role mappings
- all backend `requirePermission` calls
- all intentional no-explicit-permission read surfaces
- `X-User-Id`, `X-User-Role`, `X-Trace-Id`, `guest`, `USER` and `quant_current_user`
- all frontend request-header, route/menu/action gating and localStorage behavior
- `role-access-configs.json` shape
- DTO, VO, entity, mapper, database schema, Redis key, Kafka topic, Kafka payload, JSON config shape, prompt-template shape, Python payload and runtime settings

No auth URL, gateway URL, role URL, user/profile URL, login URL, callback URL, route alias, compatibility endpoint, proxy route or new namespace is added.

## Demo Header Compatibility Policy Shape

Current demo headers remain:

- `X-User-Id`
- `X-User-Role`
- `X-Trace-Id`

Current missing-header defaults remain:

- `guest`
- `USER`

Current frontend selected-user key remains:

- `quant_current_user`

Later gateway/JWT work must choose one approved compatibility mode before implementation:

| Future compatibility mode | Requirement |
| --- | --- |
| Retain demo headers only in local/demo profiles | Must prove production paths cannot trust headers as identity or role authority. |
| Translate validated JWT context into current backend context | Must define trusted forwarded fields, failure behavior and rollback. |
| Retire demo headers | Requires explicit human approval because it changes current compatibility behavior. |
| Hybrid compatibility during migration | Requires route, audit, config and frontend compatibility guards before implementation. |

Phase 018 approves none of these modes for implementation. It records the policy shape only.

## Service-To-Service And Audit Identity Semantics

Later phases must define these semantics before implementation:

| Semantic | Future requirement | Current Phase 018 result |
| --- | --- | --- |
| Service principal | Backend-owned identity for service callers such as Python callbacks, gateway, schedulers or future extracted services. | Requirement only; no JWT, header, Kafka or callback change. |
| Delegated actor | Human actor on whose behalf an async or service action is performed. | Requirement only; no audit payload change. |
| Original actor | Actor that started a chain such as task creation or event auto dispatch. | Requirement only; current metadata remains unchanged. |
| System actor | Explicit system identity for automation when no human actor applies. | Requirement only; current `system`/`ADMIN` fallback behavior remains unchanged. |
| Audit identity | Records identity source, role source, mapping source and service principal where applicable. | Requirement only; current audit fields remain unchanged. |

Service-to-service propagation must not be implemented through ad hoc headers, frontend utilities, workbench output, read models, fallback provenance or one-off helper/adapter/bridge code.

## Config-Store And Role-Store Migration Gates

Before any config-store or role-store migration, a later approved phase must define:

- target store or host family
- schema/versioning and compatibility rules
- single-writer and write-conflict rules
- audit retention and rollback requirements
- Java reader compatibility and cutover behavior
- Python reader compatibility and cutover behavior
- frontend reader/type compatibility
- role code, permission key and menu key compatibility
- migration, validation and rollback plan
- local/demo profile behavior
- observability and failure behavior
- explicit no-permission-widening and no-permission-narrowing checks

Until those gates are approved, JSON config files and prompt template files remain current transition stores under Phase 012.

## Route Migration And Breaking-Change Gates

Before any route migration, route alias, endpoint rename, endpoint deletion, endpoint consolidation, gateway proxy or compatibility endpoint, a later approved phase must define:

- Phase 006 frozen endpoint inventory impact
- current URL, method, owner, binding, envelope and response-type compatibility
- frontend route and API function compatibility
- permission behavior compatibility
- gateway/auth readiness and demo-header compatibility
- backend service ownership after route movement
- breaking-change or compatibility approval
- migration, rollback and test/static guard plan

No route migration or breaking change is approved by Phase 018.

## Future Implementation Sequencing Map

Future Window 0 may score these implementation-oriented or design phases after Phase 018, but Phase 018 does not approve any of them:

| Future candidate | Depends on |
| --- | --- |
| Gateway/JWT implementation design with demo-header compatibility | Phase 014, Phase 015, Phase 016, Phase 017 and this Phase 018 closure. |
| Concrete production role authority host and mapping implementation plan | Phase 012 role/config migration gates, Phase 017 role direction and this closure. |
| User profile source selection | Phase 016 issuer direction, Phase 017 role/profile separation and this closure. |
| Service-to-service propagation and audit identity implementation design | Gateway/auth readiness, service principal semantics and this closure. |
| Config-store or role-store migration planning | Phase 012 gates, role authority host decision and this closure. |
| Legacy route migration planning | Phase 006 route freeze, auth/gateway readiness and explicit breaking-change or compatibility approval. |
| Domain extraction planning for report, market, risk or strategy | Phase 008 through Phase 011 readiness plus auth, route, role and propagation decisions. |

Each candidate still requires a future Window 0 decision and human approval.

## Deferred Implementation Decisions

Deferred decisions include:

- concrete production role authority host
- DB role store, auth-service, user-service, role-service or config-store-backed mapping target
- external group/claim mapping contract
- user profile source
- gateway/auth/JWT implementation design
- external IdP or directory product/vendor integration
- auth-service, user-service, role-service or session-service creation
- login/session/OAuth/SSO integration
- token/session semantics and claim mapping
- service-principal issuance and validation
- service-to-service identity and role handoff
- audit identity and role-audit field model
- demo-header compatibility or retirement
- route migration or endpoint aliasing
- config-store or role-store migration
- service extraction

## Stop Rules For Later Phases

Stop and return to Window 0 if later work needs any of these without explicit approval:

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

- [x] Current role config, headers, runtime context, backend enforcement and frontend gating stay in their current hosts.
- [x] Future identity issuer, validator, profile source, role authority, service principal and audit identity boundaries are future-only.
- [x] No future host is created by this phase.

Authority:

- [x] No current source of truth is introduced for identity, profile, role, permission or config.
- [x] `role-access-configs.json` remains current transition input only.
- [x] `UserContext`, request headers and frontend gating remain runtime/UI inputs only.
- [x] All new target directions are labeled future-only or deferred.

Contract:

- [x] URL, API, header, permission key, menu key, role code, frontend, config and payload contracts remain stable.
- [x] No gateway/auth/JWT/session/login/OAuth/SSO endpoint or route is added.
- [x] Any future Window 0 candidate is explicitly not approved for implementation.

Behavior:

- [x] No Java, Python, frontend, config, database, Redis, Kafka, dependency, build, deployment or runtime file changed.
- [x] No permission behavior change.
- [x] No business behavior change.
- [x] No new feature implemented.
