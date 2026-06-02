# Production Auth/Gateway Target Scope

## Status And Scope

Phase: Phase 014 - Production Auth/Gateway Target Scoping and Identity/Role Authority Selection.

Artifact status: durable docs-only governance artifact for production auth/gateway target scoping.

This artifact scopes future production auth, gateway, identity authority and role authority direction. It does not implement or approve gateway/auth/JWT, auth-service, user-service, role-service, login/session, OAuth, SSO, role DB, external IdP integration, route migration, endpoint aliases, permission behavior changes, DTO/VO/entity/schema changes, Redis changes, Kafka changes, frontend changes, Python changes, config mutation, config-store migration, role-store migration, business behavior changes or new feature work.

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
- `docs/harness/handoffs/steering-decision-phase-014.md`
- `docs/harness/handoffs/phase-014-architect.md`

Durable boundary inputs:

- `docs/harness/12-transition-host-exit-criteria.md`
- `docs/harness/13-report-boundary-readiness.md`
- `docs/harness/14-market-data-ingest-boundary-readiness.md`
- `docs/harness/15-risk-strategy-projection-boundary-readiness.md`
- `docs/harness/16-config-store-decision-boundary.md`
- `docs/harness/17-auth-gateway-permission-boundary.md`

Read-only inspection sources used by Phase 014:

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

## Current Inherited Facts From Phase 013

| Surface | Current fact | Boundary |
| --- | --- | --- |
| `X-User-Id` | Current request user id header. Missing backend value defaults to `guest`. | Demo/runtime input only, not production identity authority. |
| `X-User-Role` | Current request role header. Missing backend value defaults to `USER`. | Demo/runtime input only, not production role authority. |
| `X-Trace-Id` | Current trace header from frontend request utilities. | Runtime tracing input, not permission authority. |
| `UserContext` | Request-scoped user id and role carrier installed by `UserContextFilter`. | Runtime context carrier, not source of truth. |
| `role-access-configs.json` | Current role/menu/permission config input. | JSON transition store under Phase 012, not final role-store architecture. |
| Backend `requirePermission` calls | Current runtime enforcement points for endpoints that explicitly call them. | Stable current permission behavior. |
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

Phase 014 does not add, remove or rename any header, default, role code, permission key, menu key, frontend route metadata, API function, TypeScript shape or backend permission check.

## Production Identity Authority Options

| Option | Future target meaning | Fit | Current status |
| --- | --- | --- | --- |
| Gateway/JWT ingress boundary | A future gateway or ingress auth boundary validates tokens and forwards trusted identity context to backend services. | Strong fit as a production auth boundary and route-migration prerequisite. | Future-only; not implemented or approved for implementation by Phase 014. |
| Auth-service identity issuer/validator | A backend auth-service owns login/session/token validation or delegates to an external IdP. | Strong fit if later phases select an internal identity boundary. | Future-only; no auth-service exists. |
| User-service identity profile owner | A backend user-service owns user profile facts after identity is validated elsewhere. | Useful as a later identity/profile authority split. | Future-only; no user-service exists. |
| External IdP | External identity provider supplies identity, groups or claims. | Useful if production deployment chooses IdP integration. | Future-only; no integration exists. |
| Continued demo headers | Keep `X-User-Id` and `X-User-Role` for local/demo compatibility only. | Required for current lightweight demo compatibility. | Current transition behavior, not production identity authority. |

## Governance-Only Identity Target Direction

Phase 014 selects this future-only direction:

- Production identity should be accepted through a backend-owned ingress/auth boundary before it is trusted by business services.
- A future gateway/JWT boundary is the preferred target shape for ingress identity validation, with the concrete issuer or validator left to a later phase.
- The future issuer/validator may be an auth-service, user-service, external IdP or another backend-owned identity boundary only after a later Window 0 decision and human approval.
- Domain services should consume trusted identity context, not raw frontend localStorage or unauthenticated demo headers, once a later approved production auth implementation exists.
- `X-User-Id` remains a local/demo compatibility input until a later approved compatibility or retirement phase changes it.

This direction is not current runtime authority. It does not create a gateway, auth-service, user-service, token validator, login flow, session behavior, route proxy or endpoint alias.

## Production Role Authority Options

| Option | Future target meaning | Fit | Current status |
| --- | --- | --- | --- |
| Keep JSON transition store for a bounded horizon | Continue using `role-access-configs.json` while production identity is scoped and config-store readiness is unresolved. | Conservative and compatible with Phase 012. | Current transition input remains unchanged. |
| DB role store | Backend database owns role assignments, role-permission mapping or both. | Possible later production role authority. | Deferred; no DB role store or migration is approved. |
| Nacos/config-store-backed role config | Central config system owns role/menu/permission config. | Possible after config-store schema/versioning and rollback readiness. | Deferred by Phase 012. |
| Auth-service or user-service role ownership | Identity/auth/user service owns roles or maps external identity to internal roles. | Possible if later phases create those services. | Future-only; no service exists. |
| External directory or IdP role claims | External IdP or directory supplies role/group claims. | Possible if production identity delegates to external authority. | Future-only; no IdP integration exists. |

## Governance-Only Role Target Direction

Phase 014 selects this future-only direction:

- Production role authority must be backend-owned.
- Frontend localStorage, frontend defaults, route guards, menu gating, action gating and request headers must not become production role authority.
- `role-access-configs.json` remains the current transition role/menu/permission config input under Phase 012 until a later approved role-store or config-store migration phase selects a replacement.
- A later production role phase must decide whether roles come from a backend DB role store, a config-store-backed role source, auth-service/user-service ownership, external IdP claims or a deliberately bounded continuation of the JSON transition store.
- Any role-authority migration must preserve permission keys, menu keys, role-code compatibility and auditability unless a later phase obtains explicit breaking-change approval.

This direction does not mutate `role-access-configs.json`, change role mappings, add role-store code or approve DB/Nacos/hybrid adoption.

## Demo Header Compatibility And Retirement Rules

Current demo header compatibility remains stable:

- `X-User-Id` and `X-User-Role` continue as current local/demo request inputs.
- Missing backend values continue to default to `guest` and `USER`.
- Frontend request utilities continue to send `X-User-Id`, `X-User-Role` and `X-Trace-Id` from the selected local demo user.
- `quant_current_user` remains the current frontend local selected-user storage key.

Later phases must follow these rules:

- Preserve demo headers for local/demo compatibility until a later Window 0 decision and human approval explicitly retires or changes them.
- If a future gateway/JWT implementation is selected, document whether demo headers are ignored, accepted only in local/dev profiles, translated by a gateway, or retired.
- Do not trust demo headers as production identity or role authority.
- Do not change default `guest` or `USER` behavior without an approved permission behavior change.
- Do not add compatibility endpoints, route aliases, auth adapters or frontend API adapters without a later approved implementation phase.

## Service-To-Service Propagation Requirements

Future service-to-service propagation must define the following before implementation:

| Flow | Required future propagation semantics | Current Phase 014 result |
| --- | --- | --- |
| Task creation | The actor identity, role or permission claim used for `POST /api/research/tasks` must be trusted by the approved auth boundary and checked by the task-create host. | Requirement only; current `TaskRoleAccessService` behavior stays unchanged. |
| AI callbacks | Python-to-Java callbacks must distinguish service principal identity from original user context and must not forge production user authority from fallback data. | Requirement only; no Kafka payload or callback behavior changes. |
| Event auto task dispatch | System-triggered dispatch must define original actor, system principal, role claim, trace id and audit identity semantics before production auth is implemented. | Requirement only; current `system`/`ADMIN` fallback behavior stays unchanged. |
| Future extracted services | Extracted services must receive trusted service principal and user delegation context through the approved gateway/auth/service boundary. | Requirement only; no service extraction is approved. |
| Audit identity | Audit records must distinguish human actor, service principal, delegated actor and system-triggered action where needed. | Requirement only; current audit fields stay unchanged. |

Service-to-service propagation must not be implemented through ad hoc headers, frontend utilities, workbench output, read models, fallback provenance or one-off helper/adapter/bridge code. Any implementation requires a later approved phase.

## Permission Key, Menu Key And Role-Code Compatibility Rules

Stable compatibility rules:

- Existing permission keys remain stable.
- Existing menu keys remain stable.
- Existing role codes remain stable.
- Existing coarse access-role mapping remains stable.
- Existing backend `requirePermission` checks remain stable.
- Existing intentional no-explicit-permission read surfaces remain stable.
- Existing frontend route metadata, menu checks and action gating remain UI-only.

Later production auth, gateway, role-store or route-migration phases must:

- Provide an explicit compatibility map before adding, removing or renaming permission keys, menu keys or role codes.
- Preserve current behavior by default.
- Treat permission widening, narrowing or adding checks to no-explicit-permission reads as permission behavior changes requiring later Window 0 selection and human approval.
- Keep frontend gating advisory unless backend enforcement is explicitly implemented and reviewed in a later phase.

## Audit Identity Semantics

Current audit and metadata surfaces remain metadata, not authority:

| Surface | Current metadata meaning | Future requirement |
| --- | --- | --- |
| Config audit | Records operator id and role from current request context. | Later production auth must define human actor and service principal semantics before changing audit behavior. |
| Event ingest history | Records operator metadata for source operations. | Later data-ingest/auth phases must define service-principal and delegated actor treatment. |
| Report review | Records reviewer role from current request context. | Later production auth must define trusted reviewer identity and role source. |
| Market created-by metadata | Records current user id for created market events. | Later market/auth phases must define trusted created-by semantics. |
| System-triggered actions | Event auto task dispatch currently forwards or defaults current context. | Later phases must distinguish original actor, system actor and service principal. |

Phase 014 does not change audit fields, audit payloads, ingest history shape, report review behavior, market created-by behavior or event auto task dispatch behavior.

## Stable URL/API/Permission Contract Rules

Phase 014 preserves all Phase 006 and Phase 013 contract facts:

- Every endpoint keeps the same path, HTTP method, controller owner, request binding, response envelope, response type and permission behavior.
- `POST /api/research/tasks` remains hosted by `research-task-service` and keeps `TASK_CREATE` checking through `TaskRoleAccessService`.
- Task retry remains `POST /api/tasks/{taskId}/retry` with current `TASK_RETRY` behavior.
- Task cancel remains `POST /api/tasks/{taskId}/cancel` with current `TASK_CANCEL` behavior.
- Report review remains `POST /api/tasks/{taskId}/report/review` with current `REPORT_REVIEW` behavior.
- Strategy command, market command/import/source, audit compliance and config update surfaces keep current explicit permission checks.
- Intentional no-explicit-permission read surfaces remain unchanged, including task reads, report reads, risk reads, strategy reads, market reads, market intelligence reads, workbench and `GET /api/tasks/role-access-configs`.
- No auth URL, gateway URL, route alias, compatibility endpoint, proxy route or new namespace is added.
- No endpoint is deleted, renamed, consolidated, split or moved.
- No frontend route, API function, endpoint string, call signature, TypeScript shape, localStorage key, request-header behavior, menu gating or action gating behavior changes.
- No DTO, VO, entity, mapper, schema, Kafka topic, Kafka payload, Redis key, JSON config shape, prompt-template shape, Python payload or runtime setting changes.

## Dependencies On Prior Phases

Phase 014 depends on these prior guardrails:

- Phase 005: `ai-orchestration-service` remains a next-governance-horizon modular monolith transition host, not final architecture.
- Phase 006: legacy non-task `/api/tasks/*` paths remain frozen transitional contracts.
- Phase 007: frontend workbench and fallback provenance consumers remain display/audit/UI-only.
- Phase 008: report, market, risk, strategy, audit, config and workbench transition-host exit criteria remain the cross-domain readiness inventory.
- Phase 009: report route migration or report-service extraction remains blocked until auth, route, projection and contract readiness are approved.
- Phase 010: market/data-ingest extraction, source adapter redesign and route migration remain blocked until auth, route, config and data-ingest ownership readiness are approved.
- Phase 011: risk/strategy extraction, projection split and Kafka redesign remain blocked until auth, route, Redis, generated-event and contract readiness are approved.
- Phase 012: JSON config files and prompt templates remain current transition stores; DB/Nacos/hybrid migration is deferred.
- Phase 013: header-based demo auth, role-access config, current backend permission checks, intentional no-explicit-permission read surfaces and frontend gating boundaries remain current facts.

Phase 014 does not close D001, D002, D003, D007 or D008.

## Route Migration And Service Extraction Dependencies

Later route migration depends on:

- A production auth/gateway boundary selected by a future Window 0 decision and human approval.
- A compatibility or breaking-change decision for current URLs.
- Updated Phase 006 contract inventory if endpoints move, alias, split, merge, rename or gain new permission behavior.
- A demo-header compatibility or retirement decision.

Later service extraction depends on:

- A selected identity authority and role authority.
- Service-to-service propagation semantics for actor, service principal, role claims and trace/audit metadata.
- Route and contract migration readiness.
- Domain-specific readiness gates from Phase 008 through Phase 011.
- Config-store and role-store ownership decisions where relevant.

Phase 014 does not approve route migration, service extraction, endpoint aliases, gateway proxy work or permanent modular-monolith architecture.

## Config-Store And Role-Store Dependencies

Later config-store or role-store migration depends on:

- Phase 012 schema/versioning, single-writer, audit retention, rollback and Java/Python/frontend reader compatibility readiness.
- A later Window 0 decision and human approval selecting DB, Nacos, hybrid, auth-service/user-service, external directory, IdP claims or another role authority target.
- A compatibility plan for `role-access-configs.json`, permission keys, menu keys, role codes and current API contracts.
- A migration and rollback plan that does not silently widen or narrow permissions.

Phase 014 keeps `role-access-configs.json` as the current transition input and does not mutate config files or approve config-store migration.

## Deferred Implementation Decisions

The following remain deferred and require a later Window 0 decision plus human approval:

- whether to implement gateway/auth/JWT
- whether to create auth-service, user-service, role-service or session service
- whether to introduce login, OAuth, SSO, refresh tokens or production session flows
- whether to integrate an external IdP or directory
- whether to keep, constrain, translate or retire demo headers
- whether to move role authority to DB, Nacos, hybrid config store, auth-service, user-service or external claims
- whether to add, remove, widen or narrow backend `requirePermission` calls
- whether to add permission checks to current no-explicit-permission read surfaces
- whether to migrate, alias, split, merge, rename or delete endpoints
- whether to extract report, market, risk, strategy, config, audit or workbench responsibilities
- whether to reshape frontend auth utilities, Python callbacks, Kafka payloads, database schema, Redis keys or deployment architecture

## Later Candidate Phases Clarified By Phase 014

Phase 014 clarifies these possible future phases without selecting or approving them:

- Gateway/JWT implementation design with demo-header compatibility policy.
- Production identity issuer/validator selection, such as auth-service, user-service or external IdP integration.
- Production role authority selection, such as DB role store, config-store-backed roles, auth/user-service ownership or external role claims.
- Service-to-service propagation design for AI callbacks, event auto task dispatch and future extracted services.
- Audit identity semantics for human actor, delegated actor, service principal and system-triggered actions.
- Route migration readiness after auth/gateway compatibility is selected.
- Role-access config-store migration readiness after Phase 012 gates are satisfied.

Each candidate still requires future Window 0 scoring and human approval before implementation.

## Stop Rules For Later Phases

Stop and request a new approved phase before:

- changing any URL path, HTTP method, endpoint owner, request binding, response envelope, response type, frontend route, frontend API function, TypeScript shape or permission behavior
- adding, removing, widening or narrowing any backend `requirePermission` call
- adding a permission check to a read-model surface that currently has no explicit check
- changing role codes, permission keys, menu keys, role mappings, header names, default header behavior or local role behavior
- mutating `role-access-configs.json` or any other config file
- adding gateway/auth/JWT/session/login/OAuth/SSO/auth-service/user-service/role-service code
- adding route aliases, compatibility endpoints, gateway proxies, frontend API adapters, permission resolvers, auth adapters, role-store bridges, config-store bridges, DB adapters, Nacos adapters, service wrappers, migration runners, dual-write paths, rollback runners or sync jobs
- moving permission, role-access, task-create auth, config or route responsibility into another service
- creating or modifying Java, Python, frontend, database, Redis, Kafka, config, prompt-template, dependency, test, build or deployment files
- changing DTO, VO, entity, mapper, schema, topic, payload, Redis key, JSON shape, prompt-template shape, API type or frontend type shapes
- reclassifying frontend localStorage, frontend defaults, request headers, route guards, menu/button gating, audit rows or ingest history rows as production permission authority
- declaring `ai-orchestration-service`, JSON config files, header-based demo auth, request headers, frontend role cache or legacy `/api/tasks/*` paths final architecture
- closing D001, D002, D003, D007 or D008 without a later approved governance phase

## Acceptance Checklist

- Belongs: current auth, role config, frontend gating and task-create permission responsibilities remain in their current hosts.
- Authority: production identity authority and production role authority are scoped as future-only target directions; no new current SoT is introduced.
- Contract: all URLs, methods, bindings, response envelopes, response types, permission behavior, frontend routes, frontend API functions, TypeScript shapes, headers and config shapes remain stable.
- Transition lifetime: header-based demo auth, JSON role-access config, legacy `/api/tasks/*`, `ai-orchestration-service` and modular-monolith policy remain transition facts, not final architecture.
- Behavior: no runtime behavior, business behavior, permission behavior, frontend behavior, Python behavior, Kafka behavior, Redis behavior, database behavior, config behavior or deployment behavior changes.
- Future governance: all implementation choices remain deferred to future Window 0 scoring and explicit human approval.
