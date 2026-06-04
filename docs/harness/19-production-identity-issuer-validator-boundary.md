# Production Identity Issuer/Validator Boundary

## Status And Scope

Phase: Phase 015 - Production Identity Issuer/Validator Selection Boundary.

Artifact status: durable docs-only governance artifact for production identity issuer/validator selection.

This artifact selects a future-only production identity issuer/validator direction. It does not implement or approve gateway/auth/JWT, auth-service, user-service, role-service, login/session, OAuth, SSO, external IdP integration, route migration, endpoint aliases, permission behavior changes, DTO/VO/entity/schema changes, Redis changes, Kafka changes, frontend changes, Python changes, config mutation, config-store migration, role-store migration, business behavior changes or new feature work.

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
- `docs/harness/handoffs/steering-decision-phase-015.md`
- `docs/harness/handoffs/phase-015-architect.md`

Durable boundary inputs:

- `docs/harness/12-transition-host-exit-criteria.md`
- `docs/harness/13-report-boundary-readiness.md`
- `docs/harness/14-market-data-ingest-boundary-readiness.md`
- `docs/harness/15-risk-strategy-projection-boundary-readiness.md`
- `docs/harness/16-config-store-decision-boundary.md`
- `docs/harness/17-auth-gateway-permission-boundary.md`
- `docs/harness/18-production-auth-gateway-target-scope.md`

Read-only inspection sources used by Phase 015:

- `quant-ai-platform/ai-config/role-access-configs.json`
- `quant-ai-platform/quant-services/quant-common/quant-common-security/src/main/java/com/quant/common/security/**`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/**`
- `quant-ai-platform/quant-services/quant-business/research-task-service/src/main/java/**`
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

## Current Inherited Facts From Phase 013 And Phase 014

| Surface | Current fact | Boundary |
| --- | --- | --- |
| `X-User-Id` | Current request user id header. Missing backend value defaults to `guest`. | Demo/runtime input only, not production identity authority. |
| `X-User-Role` | Current request role header. Missing backend value defaults to `USER`. | Demo/runtime input only, not production role authority. |
| `X-Trace-Id` | Current trace header from frontend request utilities. | Runtime tracing input, not identity or permission authority. |
| `UserContext` | Request-scoped user id and role carrier installed by `UserContextFilter`. | Runtime context carrier, not source of truth. |
| `SecurityUtils.currentUserId()` | Current request user id reader. | Runtime metadata reader, not production identity validator. |
| `SecurityUtils.currentUserRole()` | Current request role reader. | Current permission input reader, not production role authority. |
| `role-access-configs.json` | Current role/menu/permission config input. | JSON transition store under Phase 012, not final role-store architecture. |
| Backend `requirePermission` calls | Current runtime enforcement points for checked endpoints. | Stable current permission behavior. |
| Intentional no-explicit-permission read surfaces | Current read surfaces documented by Phase 013. | Stable current contract behavior unless a later approved phase changes it. |
| Frontend route/menu/action gating | Current UI navigation and button affordance. | UI affordance only, not backend permission authority. |
| Production gateway/auth/JWT | Not implemented. | Future-only target area requiring later Window 0 selection and human approval. |

Current role codes remain:

- `RESEARCHER`
- `PM`
- `RISK_MANAGER`
- `COMPLIANCE_AUDITOR`
- `ADMIN`

Current permission keys remain:

- `TASK_VIEW`
- `TASK_CREATE`
- `TASK_RETRY`
- `TASK_CANCEL`
- `AUDIT_COMPLIANCE_VIEW`
- `REPORT_REVIEW`
- `MODEL_AGENT_CONFIG_VIEW`
- `MODEL_AGENT_CONFIG_EDIT`

Current menu keys remain:

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

## Production Identity Issuer/Validator Option Matrix

| Option | Future target meaning | Strengths | Risks and blockers | Phase 015 result |
| --- | --- | --- | --- | --- |
| Gateway-local JWT validator | A future backend-owned ingress/gateway validates JWTs and forwards trusted identity context to backend services. | Fits Phase 014 gateway/JWT target shape; keeps business services behind a common validation boundary; helps later route and service extraction. | Still needs issuer selection, token claim contract, key rotation, local/demo compatibility, service principal semantics and rollback plan. | Selected as preferred validator placement, future-only. |
| Auth-service issuer/validator | A future backend auth-service issues or validates tokens, possibly with login/session behavior. | Strong internal ownership if the platform wants first-party identity and session semantics. | Requires auth-service scope, credential/session model, user profile dependency, storage and operations decisions. | Deferred issuer candidate, not implemented. |
| User-service profile owner with separate validator | A future user-service owns user profile facts while another boundary validates tokens. | Separates profile authority from token validation. | Does not by itself validate identity; requires a validator and role authority decision. | Future dependency, not selected as validator. |
| External IdP or directory integration | A future external provider supplies identity, groups or claims. | Avoids building core identity and can align with enterprise deployment. | Requires provider choice, claim mapping, availability, audit, role-source and rollback decisions. | Deferred issuer candidate, not implemented. |
| Continued demo-header compatibility | Keep `X-User-Id` and `X-User-Role` for local/demo compatibility. | Preserves current lightweight workflow and avoids behavior changes. | Not production security and must not become trusted identity authority. | Preserved as current transition input only. |
| Deliberate deferral | Avoid selecting a direction until broader deployment decisions are made. | Lowest immediate commitment. | Leaves Phase 014 identity-authority ambiguity unresolved. | Not chosen because validator placement can be bounded now. |

## Governance-Only Selected Direction

Phase 015 selects this future-only direction:

- The preferred future production identity validator placement is a backend-owned ingress/gateway JWT validation boundary.
- The concrete identity issuer remains deferred. Later phases may select an internal auth-service, an external IdP/directory, or another backend-owned issuer only through a future Window 0 decision and human approval.
- Business services should eventually consume trusted identity context after ingress validation, not raw frontend localStorage or unauthenticated demo headers.
- User profile ownership is not selected by Phase 015. A future user profile phase must decide whether profile facts belong to a user-service, auth-service, external directory projection, or another backend-owned store.
- `X-User-Id` and `X-User-Role` remain local/demo compatibility inputs until a later approved compatibility or retirement phase changes them.

This direction is not current runtime authority. It does not create a gateway, JWT validator, auth-service, user-service, login flow, session behavior, token format, route proxy, endpoint alias or production identity implementation.

## Rationale

Gateway-local JWT validation best fits the Phase 014 backend-owned ingress/auth target because it places the first production trust decision before business services consume identity. It also keeps later service extraction, route migration and service-to-service propagation from depending on frontend-local identity state.

Selecting validator placement now is safer than selecting the issuer now. The issuer depends on deployment and organizational choices that Phase 015 is not allowed to implement: internal auth-service, external IdP, directory integration, session model, credential ownership and user profile authority. Keeping the issuer deferred preserves the Phase 015 scope while still clarifying the validator boundary for later planning.

## Belongs Rules

| Area | Current host | Future boundary rule |
| --- | --- | --- |
| Request context plumbing | `quant-common-security` | Remains a carrier for current request user, role and trace context. It is not production identity authority. |
| Demo identity inputs | request headers and frontend request utilities | Remain local/demo compatibility inputs only. |
| Runtime user context | `UserContextFilter`, `UserContext`, `SecurityUtils` | Remains runtime context. Later production identity may flow through it only after a future approved auth implementation. |
| Role/menu/permission config input | `quant-ai-platform/ai-config/role-access-configs.json` | Remains current transition input under Phase 012. |
| Most checked command permissions | `ai-orchestration-service` / `RoleAccessConfigService` | Remain transition host enforcement points for current checked endpoints. |
| Task-create permission | `research-task-service` / `TaskRoleAccessService` | Remains formal task-create host and permission reader/checker only. |
| UI role selection and gating | `quant-ui` | Remains UI affordance and demo header source only. |
| Future identity validation | not implemented | Should belong to a backend-owned ingress/gateway boundary if later approved. |
| Future identity issuer | not implemented | Deferred to later Window 0 decision and human approval. |
| Future user profile source | not implemented | Deferred and must be separated from validator placement unless a later phase deliberately combines them. |
| Future service-principal validation | not implemented | Must belong to the approved auth/gateway/service boundary, not frontend utilities or domain read models. |

## Authority Rules

- Current runtime context and future trusted identity authority must remain distinct.
- The selected gateway/JWT validator placement is future-only and does not become current authority.
- Production identity must be trusted only after a future backend-owned ingress/auth boundary validates it.
- Demo headers must remain local/demo compatibility inputs until a later approved compatibility or retirement phase changes them.
- Role authority is not selected in Phase 015. Role authority remains a later phase that must account for the deferred issuer and selected validator placement.
- User profile ownership is not selected in Phase 015 and must not be inferred from `UserContext`, audit metadata, frontend cache or role config.
- Service principal validation and user delegation semantics are future requirements only.
- Audit identity semantics are future requirements only and do not change current audit fields.
- No frontend cache, localStorage, route guard, menu gating or action gating may become backend identity or permission truth.
- No read model, workbench output, audit row, ingest history row, fallback provenance, report metadata or Kafka callback may become identity authority.
- No documentation in Phase 015 closes D001, D002, D003, D007 or D008.

## User Profile Source And Validator Ownership Separation

Later implementation-design phases must decide these separately:

| Concern | Future decision required | Phase 015 constraint |
| --- | --- | --- |
| Token validation | Where JWT or equivalent token validation runs. | Preferred placement is backend-owned ingress/gateway, future-only. |
| Token issuer | Which system issues, signs or delegates production identity. | Deferred; no auth-service or external IdP is selected as current issuer. |
| User profile source | Where user display name, department, status and profile facts live. | Deferred; not `UserContext`, frontend storage or role-access config by default. |
| Role authority | Where role assignment and role-permission mapping become production truth. | Deferred to a later role authority phase. |
| Service principal source | Where service identities are registered and validated. | Deferred; must be backend-owned and auditable. |

Combining issuer, validator, profile and role ownership is allowed only if a later Window 0 decision explicitly selects that combined shape and the user approves the scope.

## Token And Session Semantics Readiness Gates

Before gateway/JWT, auth-service, user-service, login/session, OAuth, SSO or external IdP work can start, a later phase must define:

- token format and required claims
- issuer and audience rules
- signing key or introspection trust model
- expiration and refresh behavior
- session presence or deliberate stateless-token policy
- user id shape and compatibility with current `X-User-Id`
- role claim treatment, if any, and whether it is authoritative
- trace id propagation and relation to `X-Trace-Id`
- local/demo compatibility behavior for `X-User-Id` and `X-User-Role`
- failure behavior for missing, expired, malformed or unauthorized identity
- rollback behavior if gateway validation is disabled or unavailable
- audit treatment for human actor, delegated actor and service principal

Phase 015 defines these as readiness gates only and does not choose token fields, session storage, login URLs or callback routes.

## Service Principal And Service-To-Service Identity Handoff Requirements

Future service-to-service identity must define:

| Flow | Required future semantics | Current Phase 015 result |
| --- | --- | --- |
| Task creation | Actor identity and role/permission claim used by `POST /api/research/tasks` must be trusted by the approved auth boundary and checked by the task-create host. | Requirement only; current `TaskRoleAccessService` behavior stays unchanged. |
| AI callbacks | Python-to-Java callbacks must distinguish service principal identity from original user context and must not derive production user authority from fallback data. | Requirement only; no Kafka payload or callback behavior changes. |
| Event auto task dispatch | System-triggered dispatch must define original actor, system principal, role claim, trace id and audit identity semantics. | Requirement only; current `system`/`ADMIN` fallback behavior stays unchanged. |
| Future extracted services | Extracted services must receive trusted service principal and user delegation context through the approved auth/gateway/service boundary. | Requirement only; no service extraction is approved. |
| Config and source operations | Operator identity must distinguish human operator from service automation where relevant. | Requirement only; current file-backed audit behavior stays unchanged. |

Service-to-service propagation must not be implemented through ad hoc headers, frontend utilities, workbench output, read models, fallback provenance or one-off helper/adapter/bridge code. Any implementation requires a later approved phase.

## Audit Identity Requirements

Later production auth phases must define audit identity fields or mappings for:

- human actor
- delegated actor
- service principal
- system-triggered action
- original actor on async follow-up work
- validator/issuer source when relevant to audit
- trace id and request correlation
- local/demo actor compatibility

Current audit fields and metadata remain unchanged:

- config audit operator id and role from current request context
- event ingest history operator metadata
- report review reviewer role
- market created-by metadata
- event auto task dispatch current-context or fallback metadata

Phase 015 does not change audit rows, audit payloads, ingest history shape, report review behavior, market created-by behavior or event auto task dispatch behavior.

## Demo Header Compatibility Rules

Current demo header compatibility remains stable:

- `X-User-Id` and `X-User-Role` continue as current local/demo request inputs.
- Missing backend values continue to default to `guest` and `USER`.
- Frontend request utilities continue to send `X-User-Id`, `X-User-Role` and `X-Trace-Id` from the selected local demo user.
- `quant_current_user` remains the current frontend local selected-user storage key.

Later phases must follow these rules:

- Preserve demo headers for local/demo compatibility until a later Window 0 decision and human approval explicitly retires or changes them.
- If gateway/JWT implementation is later selected, document whether demo headers are ignored, accepted only in local/dev profiles, translated by a gateway, or retired.
- Do not trust demo headers as production identity or role authority.
- Do not change default `guest` or `USER` behavior without an approved permission behavior change.
- Do not add compatibility endpoints, route aliases, auth adapters or frontend API adapters without a later approved implementation phase.

## Stable URL/API/Permission/Header/Frontend Contract Rules

Phase 015 preserves all Phase 006, Phase 013 and Phase 014 contract facts:

- Every endpoint keeps the same path, HTTP method, controller owner, request binding, response envelope, response type and permission behavior.
- `POST /api/research/tasks` remains hosted by `research-task-service` and keeps `TASK_CREATE` checking through `TaskRoleAccessService`.
- Task retry remains `POST /api/tasks/{taskId}/retry` with current `TASK_RETRY` behavior.
- Task cancel remains `POST /api/tasks/{taskId}/cancel` with current `TASK_CANCEL` behavior.
- Report review remains `POST /api/tasks/{taskId}/report/review` with current `REPORT_REVIEW` behavior.
- Strategy command, market command/import/source, audit compliance and config update surfaces keep current explicit permission checks.
- Intentional no-explicit-permission read surfaces remain unchanged, including task reads, report reads, risk reads, strategy reads, market reads, market intelligence reads, workbench and `GET /api/tasks/role-access-configs`.
- No auth URL, gateway URL, login URL, callback URL, route alias, compatibility endpoint, proxy route or new namespace is added.
- No endpoint is deleted, renamed, consolidated, split or moved.
- No role code, permission key, menu key, header name or default value is added, removed or renamed.
- No backend `requirePermission` call is added, removed, widened, narrowed, moved or renamed.
- No explicit permission check is added to a read-model endpoint that currently has none.
- No frontend route, API function, endpoint string, call signature, TypeScript shape, localStorage key, request-header behavior, menu gating or action gating behavior changes.
- No DTO, VO, entity, mapper, schema, Kafka topic, Kafka payload, Redis key, JSON config shape, prompt-template shape, Python payload or runtime setting changes.

## Dependencies On Prior Phases

Phase 015 depends on these prior guardrails:

- Phase 005: `ai-orchestration-service` remains a next-governance-horizon modular monolith transition host, not final architecture.
- Phase 006: legacy non-task `/api/tasks/*` paths remain frozen transitional contracts.
- Phase 007: frontend workbench and fallback provenance consumers remain display/audit/UI-only.
- Phase 008: report, market, risk, strategy, audit, config and workbench transition-host exit criteria remain the cross-domain readiness inventory.
- Phase 009: report route migration or report-service extraction remains blocked until auth, route, projection and contract readiness are approved.
- Phase 010: market/data-ingest extraction, source adapter redesign and route migration remain blocked until auth, route, config and data-ingest ownership readiness are approved.
- Phase 011: risk/strategy extraction, projection split and Kafka redesign remain blocked until auth, route, Redis, generated-event and contract readiness are approved.
- Phase 012: JSON config files and prompt templates remain current transition stores; DB/Nacos/hybrid migration is deferred.
- Phase 013: header-based demo auth, role-access config, current backend permission checks, intentional no-explicit-permission read surfaces and frontend gating boundaries remain current facts.
- Phase 014: production identity must pass through a backend-owned ingress/auth boundary before business services trust it; gateway/JWT is the preferred future target shape.

Phase 015 does not close D001, D002, D003, D007 or D008.

## Dependencies For Later Production Role Authority Selection

A later production role authority phase must use the Phase 015 selected validator placement as input and still decide:

- whether role authority belongs to DB, config store, auth-service, user-service, external IdP claims, external directory groups or a bounded continuation of JSON transition config
- whether role assignments and role-permission mappings share one authority or separate authorities
- how current role codes map to future trusted claims or records
- how permission keys and menu keys remain compatible
- how frontend route/menu/action gating remains UI-only unless backend enforcement changes are explicitly approved
- how `role-access-configs.json` is preserved, migrated or retired under Phase 012 constraints

Phase 015 does not choose or migrate production role authority.

## Dependencies For Later Gateway/JWT Implementation Design

A later gateway/JWT implementation-design phase must define:

- concrete gateway or ingress component ownership
- issuer selection or issuer integration
- JWT validation rules and failure behavior
- trusted identity context forwarded to backend services
- demo-header compatibility or retirement policy
- local/dev profile behavior
- service-to-service principal validation
- audit identity mapping
- rollback strategy
- route and contract compatibility with Phase 006 inventory
- test and static guard scope

Phase 015 only selects future validator placement. It does not start gateway/JWT design or implementation.

## Dependencies For Later Route Migration, Service Extraction, Config-Store Migration And Role-Store Migration

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
- a later Window 0 decision and human approval selecting DB, Nacos, hybrid, auth-service/user-service, external directory, IdP claims or another role authority target
- compatibility plan for `role-access-configs.json`, permission keys, menu keys, role codes and current API contracts
- migration and rollback plan that does not silently widen or narrow permissions

Phase 015 does not approve route migration, service extraction, endpoint aliases, gateway proxy work, config-store migration, role-store migration or permanent modular-monolith architecture.

## Deferred Implementation Decisions

The following remain deferred and require a later Window 0 decision plus human approval:

- which issuer supplies production identity
- whether to build an auth-service, user-service, role-service or session service
- whether to integrate an external IdP or directory
- whether to implement gateway/auth/JWT
- whether to introduce login, OAuth, SSO, refresh tokens or production session flows
- whether to keep, constrain, translate or retire demo headers
- whether to move role authority to DB, Nacos, hybrid config store, auth-service, user-service or external claims
- whether to add, remove, widen or narrow backend `requirePermission` calls
- whether to add permission checks to current no-explicit-permission read surfaces
- whether to migrate, alias, split, merge, rename or delete endpoints
- whether to extract report, market, risk, strategy, config, audit or workbench responsibilities
- whether to reshape frontend auth utilities, Python callbacks, Kafka payloads, database schema, Redis keys or deployment architecture

## Stop Rules For Later Phases

Stop and request a new approved phase before:

- changing any URL path, HTTP method, endpoint owner, request binding, response envelope, response type, frontend route, frontend API function, TypeScript shape or permission behavior
- adding, removing, widening or narrowing any backend `requirePermission` call
- adding a permission check to a read-model surface that currently has no explicit check
- changing role codes, permission keys, menu keys, role mappings, header names, default header behavior or local role behavior
- mutating `role-access-configs.json` or any other config file
- adding gateway/auth/JWT/session/login/OAuth/SSO/auth-service/user-service/role-service code
- adding route aliases, compatibility endpoints, gateway proxies, frontend API adapters, identity resolvers, permission resolvers, auth adapters, role-store bridges, config-store bridges, DB adapters, Nacos adapters, service wrappers, migration runners, dual-write paths, rollback runners or sync jobs
- moving identity, permission, role-access, task-create auth, config or route responsibility into another service
- creating or modifying Java, Python, frontend, database, Redis, Kafka, config, prompt-template, dependency, test, build or deployment files
- changing DTO, VO, entity, mapper, schema, topic, payload, Redis key, JSON shape, prompt-template shape, API type or frontend type shapes
- reclassifying frontend localStorage, frontend defaults, request headers, route guards, menu/button gating, audit rows or ingest history rows as production identity or permission authority
- declaring `ai-orchestration-service`, JSON config files, header-based demo auth, request headers, frontend role cache or legacy `/api/tasks/*` paths final architecture
- closing D001, D002, D003, D007 or D008 without a later approved governance phase

## Acceptance Checklist

- Belongs: current auth, role config, frontend gating and task-create permission responsibilities remain in their current hosts.
- Authority: future production identity validation is selected as backend-owned ingress/gateway JWT validation, while issuer, user profile and role authority remain deferred; no new current SoT is introduced.
- Contract: all URLs, methods, bindings, response envelopes, response types, permission behavior, frontend routes, frontend API functions, TypeScript shapes, headers and config shapes remain stable.
- Transition lifetime: header-based demo auth, JSON role-access config, legacy `/api/tasks/*`, `ai-orchestration-service` and modular-monolith policy remain transition facts, not final architecture.
- Behavior: no runtime behavior, business behavior, permission behavior, frontend behavior, Python behavior, Kafka behavior, Redis behavior, database behavior, config behavior or deployment behavior changes.
- Future governance: all implementation choices remain deferred to future Window 0 scoring and explicit human approval.
