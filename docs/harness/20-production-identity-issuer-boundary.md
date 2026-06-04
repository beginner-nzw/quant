# Production Identity Issuer Boundary

## Status And Scope

Phase: Phase 016 - Production Identity Issuer Selection Boundary.

Status: docs-only governance boundary.

Primary decision: prefer an external IdP or enterprise directory as the future production identity issuer, with backend-owned ingress/gateway JWT validation remaining the preferred future validator placement from Phase 015.

This selection is future-only. It does not implement or approve gateway/auth/JWT, auth-service, user-service, role-service, login/session, OAuth, SSO, external IdP integration, route migration, endpoint aliases, permission behavior changes, DTO/VO/entity/schema changes, Redis changes, Kafka changes, frontend changes, Python changes, config mutation, config-store migration, role-store migration, business behavior changes or new feature work.

All later implementation, integration, compatibility and migration work requires a future Window 0 decision and human approval.

## Inputs And Read-Only Inspection Sources

Phase 016 uses these governance inputs:

- `docs/harness/00-project-charter.md`
- `docs/harness/02-authority-matrix.md`
- `docs/harness/03-host-ownership.md`
- `docs/harness/04-contract-map.md`
- `docs/harness/05-transition-lifetime.md`
- `docs/harness/08-eval-checklist.md`
- `docs/harness/09-window-protocol.md`
- `docs/harness/state/current-state.md`
- `docs/harness/handoffs/steering-decision-phase-016.md`
- `docs/harness/handoffs/phase-016-architect.md`
- `docs/harness/handoffs/phase-015-final.md`
- `docs/harness/12-transition-host-exit-criteria.md`
- `docs/harness/13-report-boundary-readiness.md`
- `docs/harness/14-market-data-ingest-boundary-readiness.md`
- `docs/harness/15-risk-strategy-projection-boundary-readiness.md`
- `docs/harness/16-config-store-decision-boundary.md`
- `docs/harness/17-auth-gateway-permission-boundary.md`
- `docs/harness/18-production-auth-gateway-target-scope.md`
- `docs/harness/19-production-identity-issuer-validator-boundary.md`

Read-only inventory confirmed current auth, permission, header and frontend surfaces in:

- `quant-common-security`
- `ai-orchestration-service`
- `research-task-service`
- `quant-ui`
- `quant-ai-platform/ai-config/role-access-configs.json`

No runtime file was changed.

## Current Inherited Facts

These facts remain unchanged from Phase 013, Phase 014 and Phase 015:

- `X-User-Id` and `X-User-Role` are current demo/runtime inputs only.
- Missing backend header values still default to `guest` and `USER`.
- `X-Trace-Id` is runtime tracing input, not identity or permission authority.
- `UserContext` is request runtime context, not production identity authority.
- `SecurityUtils.currentUserId()` and `SecurityUtils.currentUserRole()` read runtime metadata and current permission inputs.
- `role-access-configs.json` is the current transition role/menu/permission config input under the Phase 012 JSON transition-store policy.
- Backend explicit `requirePermission` calls remain the current enforcement points for checked endpoints.
- Intentional no-explicit-permission read surfaces remain unchanged.
- Frontend route, menu and action gating is UI affordance only.
- Frontend local role state and `quant_current_user` are UI/runtime inputs only.
- Backend-owned ingress/gateway JWT validation is the preferred future validator placement from Phase 015.
- No production gateway/auth/JWT implementation exists.
- No auth-service, user-service, role-service, login/session, OAuth, SSO or external IdP integration exists.

## Production Identity Issuer Option Matrix

| Option | Future belongs | Authority fit | Contract fit | Phase 016 decision |
| --- | --- | --- | --- | --- |
| External IdP or enterprise directory issuer | External identity system, validated by backend-owned ingress/gateway | Strong fit for production identity authority; keeps application services from becoming credential authorities | Fits Phase 015 validator placement if token issuer, audience, claims, key rotation and rollback contracts are later defined | Preferred future issuer direction |
| Internal auth-service issuer | Future backend auth-service | Possible fit if the platform must own credentials and sessions directly | Requires new auth-service, token/session semantics and operational ownership before implementation | Deferred fallback candidate |
| User-service profile owner with separate issuer | User-service owns profile facts; separate issuer owns authentication | Good separation for profile and authentication, but not a standalone issuer decision | Requires profile schema and issuer handoff contracts later | Future dependency, not issuer selection |
| Another backend-owned issuer | Approved backend identity boundary | Possible only if a later phase defines concrete issuer ownership and operations | Too broad for current facts | Deferred |
| Continued issuer deferral | None | Avoids premature commitment but leaves Phase 015 issuer gap open | Does not reduce the current issuer ambiguity | Rejected for Phase 016 because external issuer preference can be stated safely as future-only |

## Selected Future Issuer Direction

The preferred future production identity issuer direction is an external IdP or enterprise directory that issues user identity tokens or equivalent identity assertions consumed through a backend-owned ingress/auth boundary.

This direction fits Phase 015 because:

- Phase 015 already selected backend-owned ingress/gateway JWT validation as the preferred future validator placement.
- An external issuer keeps credential lifecycle, MFA, account recovery, password policy and directory membership outside business services.
- Backend-owned gateway validation can enforce issuer, audience, signature, expiry, key rotation and token-shape checks before application services receive trusted runtime context.
- Application services can continue treating `UserContext` as a runtime carrier rather than becoming issuer or validator authorities.
- Role authority can remain a separate future decision instead of being accidentally collapsed into demo headers or frontend gating.

This direction is not current runtime authority. It does not make any external IdP, directory, JWT, gateway, auth-service, user-service or role-service implemented or trusted in the current system.

## Belongs Rules

| Area | Current belongs | Future Phase 016 rule |
| --- | --- | --- |
| Demo identity inputs | Request headers and frontend request utilities | Remain local/demo compatibility inputs until a later approved compatibility or retirement phase. |
| Runtime user context | `UserContextFilter`, `UserContext`, `SecurityUtils` | Remains a runtime carrier. Later gateway/auth work may populate it only after approved validation design. |
| Production identity issuer | Not implemented | Preferred future belongs is external IdP or enterprise directory. |
| Production identity validator | Not implemented | Preferred future belongs remains backend-owned ingress/gateway JWT validation from Phase 015. |
| User profile source | Not implemented | Deferred. A later phase must decide whether profile facts come from directory claims, user-service, auth-service or another backend source. |
| Production role authority | Not implemented | Deferred. Phase 016 does not select or migrate role authority. |
| Service principals | Not implemented | Must belong to an approved auth/gateway/service boundary, not frontend utilities or ad hoc headers. |
| Audit identity | Current audit fields use runtime context | Future semantics must distinguish human actor, delegated actor, service principal and system-triggered action before implementation. |

## Authority Rules

- The external IdP or enterprise directory is selected only as the preferred future issuer direction.
- Backend-owned ingress/gateway JWT validation remains the preferred future validator placement.
- Business services must not trust production identity until a future approved ingress/auth boundary validates it.
- `X-User-Id`, `X-User-Role`, frontend localStorage, route guards, menu gating and action gating remain non-production identity and permission inputs.
- `UserContext` remains request runtime context and must not be described as the production identity source of truth.
- `role-access-configs.json` remains the current transition permission config input and is not final role authority.
- Role authority is intentionally not selected in Phase 016.
- User profile source is intentionally not selected in Phase 016.
- Audit rows, ingest history rows, workbench output, report metadata, Kafka callbacks and Python fallback provenance must not become identity authority.
- No current endpoint, DTO, VO, entity, mapper, schema, Redis key, Kafka topic, Kafka payload, JSON config shape, frontend type or Python payload becomes an identity authority contract from this phase.

## Token Claim And Session Dependency Rules

Any later gateway/JWT implementation design must define, before code changes:

- accepted issuer identifiers
- accepted audiences
- signing key and key-rotation behavior
- token expiry and clock-skew policy
- subject claim mapping to runtime user id
- display-name and profile claim policy, if any
- role/group claim policy, if any
- tenant or organization claim policy, if needed
- service-principal claim shape
- delegated-user claim shape for service-to-service calls, if needed
- token refresh, session lifetime and logout semantics
- local/demo header compatibility mode and rollback behavior

Until those decisions are approved, current header defaults `guest` and `USER` remain unchanged and no JWT/session/login/OAuth/SSO behavior is implemented.

## User Profile Source Dependency Rules

Phase 016 does not select a user profile source.

Later phases must decide whether profile facts come from:

- external IdP or directory claims
- a backend user-service
- an auth-service profile store
- a synchronized profile read model
- another approved backend-owned source

The profile source decision must define stable user id mapping, display name, department or organization facts if needed, deactivation behavior, audit display behavior and rollback. It must not be inferred from frontend local role state, demo headers, workbench data, fallback metadata or report fields.

## Production Role Authority Dependencies

Phase 016 does not select or migrate production role authority.

The external IdP or enterprise directory issuer direction affects later role-authority choices:

- role claims or directory groups may become an input only if a later phase approves them
- a backend role store may remain preferable if business permissions need application-owned lifecycle and audit
- `role-access-configs.json` may remain a transition input until a later approved role-store migration
- frontend route/menu/action gating must remain UI affordance
- backend explicit `requirePermission` checks must remain stable unless a later phase approves permission behavior changes

Any role-authority phase must preserve existing role codes, permission keys and menu keys unless a breaking change is explicitly approved.

## Service Principal And Service-To-Service Dependencies

Later service-to-service identity work must define:

- which service principals may call backend APIs
- how service tokens are issued, validated and rotated
- how user delegation is represented when a service acts for a human user
- how system-triggered work is represented when no human user exists
- how event auto task dispatch, callbacks and future extracted services carry identity
- how rollback returns to current demo/runtime compatibility without permission widening

Ad hoc headers, frontend utilities, Kafka payload side effects and fallback metadata must not become service-principal authority.

## Audit Identity Dependencies

Later audit identity work must distinguish:

- human actor identity
- delegated actor identity
- service principal identity
- system-triggered action identity
- current runtime context used for compatibility

Phase 016 does not change audit fields, audit rows, config audit behavior, ingest history behavior or report review audit behavior.

## Demo Header Compatibility Rules

- `X-User-Id` and `X-User-Role` remain current local/demo compatibility inputs.
- Missing header defaults remain `guest` and `USER`.
- The frontend selected-user key remains `quant_current_user`.
- Current request-header utility behavior remains unchanged.
- Demo headers may be retained behind a later approved compatibility mode, but retirement is not approved by Phase 016.
- No current permission check may be widened, narrowed, added, removed or moved as part of this phase.

## Stable Contract Rules

Phase 016 preserves:

- all URL paths and HTTP methods
- all endpoint owners, request bindings, response envelopes and response types
- all frontend routes
- all frontend API function names, endpoint strings, call signatures and TypeScript shapes
- all role codes, permission keys and menu keys
- all backend `requirePermission` calls
- all intentional no-explicit-permission read surfaces
- all header names and default values
- all DTO, VO, entity, mapper, schema, Redis key, Kafka topic, Kafka payload, JSON config shape, prompt-template shape and Python payload contracts

Phase 016 adds no auth URL, login URL, callback URL, gateway URL, route alias, compatibility endpoint, proxy route or new namespace.

## Dependency Map

| Prior phase | Preserved dependency |
| --- | --- |
| Phase 006 | Legacy non-task `/api/tasks/*` paths remain frozen transition contracts. No route migration or alias is approved. |
| Phase 012 | JSON config files remain current transition stores. No config mutation, DB/Nacos/hybrid migration or role-store migration is approved. |
| Phase 013 | Permission inventory remains stable. Demo headers and frontend gating remain non-production inputs. |
| Phase 014 | Production identity should enter through a backend-owned ingress/auth boundary, future-only. |
| Phase 015 | Backend-owned ingress/gateway JWT validation remains preferred future validator placement. |

## Later Phase Dependencies

Later production role authority selection must decide whether role facts come from directory/group claims, a backend role store, auth-service, user-service, config-store-backed role source or another approved source.

Later gateway/JWT implementation design must define token validation, claim mapping, issuer/audience policy, key rotation, service principals, demo-header compatibility and rollback.

Later route migration must account for Phase 006 route freeze and cannot proceed without explicit compatibility or breaking-change approval.

Later service extraction must account for identity validation, role authority, service principals, audit identity and stable contracts before moving domain responsibility.

Later config-store or role-store migration must account for Phase 012 and the deferred role-authority decision.

## Deferred Implementation Decisions

Deferred decisions requiring a future Window 0 decision and human approval:

- external IdP or directory product/vendor selection
- gateway/auth/JWT implementation design
- auth-service, user-service, role-service or session-service creation
- login/session/OAuth/SSO integration
- token claim schema and session lifecycle
- user profile source
- production role authority
- service-principal issuance and validation
- service-to-service identity handoff
- audit identity field model
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
- implementing gateway/auth/JWT/session/login/OAuth/SSO/external IdP integration
- creating auth-service, user-service or role-service
- changing DTOs, VOs, entities, mappers, schemas, Kafka topics, Kafka payloads, Redis keys, frontend types or Python payloads
- creating route aliases, adapters, bridges, resolvers, wrappers, compatibility endpoints, sync jobs, dual-write paths or rollback runners
- declaring demo headers, frontend localStorage, JSON role config, legacy `/api/tasks/*` paths or the modular monolith final architecture
- closing D001, D002, D003, D007 or D008

## Acceptance Checklist

Belongs:

- [x] Future issuer belongs is selected as external IdP or enterprise directory, future-only.
- [x] Future validator belongs remains backend-owned ingress/gateway JWT validation, future-only.
- [x] Current `UserContext` belongs remains runtime carrier only.
- [x] Current demo headers remain local/demo compatibility inputs only.

Authority:

- [x] No current identity source of truth is introduced.
- [x] No current role authority is selected or migrated.
- [x] Frontend gating and localStorage remain UI/runtime inputs only.
- [x] `role-access-configs.json` remains current transition permission config input only.

Contract:

- [x] URL, API, header, permission, role, menu, frontend and payload contracts remain stable.
- [x] No gateway/auth/JWT/session/login/OAuth/SSO endpoint or route is added.
- [x] Phase 006, Phase 012, Phase 013, Phase 014 and Phase 015 constraints remain in force.

Behavior:

- [x] No Java, Python, frontend, config, database, Redis, Kafka, dependency, build, deployment or runtime file changed.
- [x] No permission behavior change.
- [x] No business behavior change.
- [x] All implementation work is deferred to later phases requiring future Window 0 selection and human approval.
