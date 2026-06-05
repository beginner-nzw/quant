# Steering Decision - Phase 017

## Status

Window: Window 0 - Steering.

Decision: Phase 017 approved by user for Window 1 architecture planning.

Human approval status: approved.

This file does not approve implementation. Window 1 is now allowed to start architecture planning and must produce a Phase 017 architect handoff before any implementation window can start.

## Inputs Read

Fixed harness artifacts:

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
- `docs/harness/handoffs/phase-000-harness-baseline.md`

Handoff directory was listed.

Latest completed phase from `current-state.md`:

- Phase 016 - Production Identity Issuer Selection Boundary.

Highest final handoff discovered:

- `docs/harness/handoffs/phase-016-final.md`

Matching Phase 016 handoffs consumed:

- `docs/harness/handoffs/steering-decision-phase-016.md`
- `docs/harness/handoffs/phase-016-architect.md`
- `docs/harness/handoffs/phase-016-implementation.md`
- `docs/harness/handoffs/phase-016-review.md`
- `docs/harness/handoffs/phase-016-final.md`

Durable boundary artifacts consumed:

- `docs/harness/16-config-store-decision-boundary.md`
- `docs/harness/17-auth-gateway-permission-boundary.md`
- `docs/harness/18-production-auth-gateway-target-scope.md`
- `docs/harness/19-production-identity-issuer-validator-boundary.md`
- `docs/harness/20-production-identity-issuer-boundary.md`

Missing matching handoff files:

- None for Phase 016.

## Startup Recovery Result

`docs/harness/state/current-state.md` and the handoff directory agree:

- Current active phase: none approved.
- Last completed phase: Phase 016 - Production Identity Issuer Selection Boundary.
- Latest final handoff: `phase-016-final.md`.
- Open blockers: none registered.

Bootstrap Phase 001 is not current fact because Phase 001 through Phase 016 are completed and frozen by Window 4.

## Current State Summary

- Phase 016 completed docs-only production identity issuer governance work.
- `docs/harness/20-production-identity-issuer-boundary.md` is now the durable production identity issuer boundary artifact.
- External IdP or enterprise directory is selected as the preferred future production identity issuer direction, future-only.
- Backend-owned ingress/gateway JWT validation remains the preferred future validator placement from Phase 015.
- `X-User-Id` and `X-User-Role` remain local/demo compatibility inputs only.
- `UserContext` remains runtime context, not production identity authority.
- `role-access-configs.json` remains the current transition role/menu/permission input under Phase 012.
- User profile source remains deferred.
- Production role authority remains deferred.
- Gateway/JWT implementation design remains deferred.
- Service-principal and service-to-service identity handoff remain deferred.
- `ai-orchestration-service` remains a multi-domain transition host, not final architecture.
- Legacy non-task `/api/tasks/*` paths remain frozen transitional contracts under Phase 006.
- Open debt remains for D001, D002, D003, D007 and D008.
- No main path breakage is registered.

## Latest Completed Or Blocked Phase

Latest completed phase:

- Phase 016 - Production Identity Issuer Selection Boundary.

Latest blocked phase:

- None registered.

Handoff files read for the latest completed phase:

- `steering-decision-phase-016.md`
- `phase-016-architect.md`
- `phase-016-implementation.md`
- `phase-016-review.md`
- `phase-016-final.md`

## Decision Order Result

Decision order from `docs/harness/10-steering-state-machine.md`:

```text
main path breakage -> authority ambiguity -> contract ambiguity -> transition host reduction -> eval/test coverage -> new feature work
```

Evaluation:

- Main path breakage: none registered.
- Authority ambiguity: still present. Phase 016 selected future issuer direction but deliberately deferred production role authority and user profile source.
- Contract ambiguity: still present. Gateway/JWT, route migration, service extraction, role-store/config-store migration and permission behavior decisions depend on trusted role authority and compatibility contracts.
- Transition host reduction: still needed for D001, but extraction or route migration remains blocked by identity, role, profile, propagation, config and route gates.
- Eval/test coverage: useful, but lower order than the remaining authority decisions.
- New feature work: not eligible while authority, contract and transition-host lifecycle debt remains open.

## Candidate Score Table

Scores are 0-2. Behavior risk is inverted: 2 means low risk.

| Candidate | Main path protection | Authority clarity | Contract clarity | Transition host reduction | Behavior risk | Verification feasibility | Total | Steering result |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| Phase 017 - Production Role Authority Selection Boundary | 1 | 2 | 2 | 1 | 2 | 2 | 10 | Primary |
| User Profile Source Selection Boundary | 1 | 2 | 1 | 1 | 2 | 2 | 9 | Fallback |
| Service-to-Service Propagation and Audit Identity Semantics Boundary | 1 | 2 | 1 | 1 | 2 | 2 | 9 | Defer |
| Gateway/JWT implementation design with demo-header compatibility policy | 1 | 1 | 2 | 1 | 1 | 2 | 8 | Defer |
| Config-store or role-store migration target/scoping | 1 | 1 | 2 | 1 | 1 | 2 | 8 | Defer |
| Legacy route migration decision phase | 0 | 1 | 2 | 2 | 0 | 1 | 6 | Defer |
| Report extraction or report route-migration planning | 1 | 1 | 1 | 2 | 0 | 1 | 6 | Defer |
| Market-service or data-ingest-service extraction planning | 1 | 1 | 1 | 2 | 0 | 1 | 6 | Defer |
| Risk-service, strategy-service or projection-split planning | 1 | 1 | 1 | 2 | 0 | 1 | 6 | Defer |
| Generic eval/static guard expansion | 1 | 1 | 1 | 0 | 2 | 2 | 7 | Defer by decision order |
| New product feature or new agent work | 0 | 0 | 0 | 0 | 0 | 0 | 0 | Not eligible |

Tie-break note:

- User profile source selection and service-to-service/audit identity semantics tie numerically.
- User profile source is the fallback because Phase 016 explicitly deferred it alongside role authority, and profile/source mapping is a nearer authority prerequisite for token claims and audit display than propagation implementation semantics.

## Primary Candidate

Phase 017 - Production Role Authority Selection Boundary.

Bounded goal:

- Produce a docs-only production role authority boundary artifact.
- Select, at governance level only, the preferred future production role authority direction or explicitly keep role authority deferred with narrower readiness criteria.
- Use Phase 014 role-authority target rules, Phase 015 validator placement, Phase 016 external IdP or enterprise directory issuer direction and Phase 012 config-store constraints.
- Compare role authority candidates such as:
  - external IdP or directory role/group claims
  - backend DB role store
  - auth-service or user-service role ownership
  - config-store-backed role source
  - bounded continuation of `role-access-configs.json` as a transition input
- Define role assignment authority, role-permission mapping authority, permission-key/menu-key compatibility, frontend UI-gating boundaries, backend enforcement boundaries, auditability, config-store dependency and migration/readiness gates.
- Preserve current role codes, permission keys, menu keys, backend `requirePermission` calls, no-explicit-permission read surfaces, frontend route/menu/action gating behavior and `role-access-configs.json` shape.

Expected durable artifact:

- `docs/harness/21-production-role-authority-boundary.md`

Why this is the next bounded step:

- Phase 016 resolved issuer direction but left production role authority unresolved.
- Role authority is a higher-order authority decision than gateway/JWT implementation design, route migration, service extraction, role-store migration or feature work.
- It is closer than service-to-service propagation because propagation needs trusted role/permission semantics before it can safely define delegated actor and service-principal authorization behavior.
- It is closer than role-store migration because migration target and compatibility depend on whether role authority belongs to external claims, DB, auth/user service, config store or a bounded transition file.
- It can remain docs-only and reduce D008/D007 ambiguity without changing runtime behavior.

Expected phase shape:

- Docs-only architecture/governance work by default.
- No Java, Python, frontend, database, Redis, Kafka, config, dependency, build, deployment or runtime behavior change.
- No gateway/auth/JWT implementation.
- No external IdP integration.
- No auth-service, user-service, role-service, login/session, OAuth or SSO implementation.
- No role-store migration.
- No `role-access-configs.json` mutation.
- No route migration, route alias, endpoint rename, endpoint deletion, endpoint consolidation or gateway proxy.
- No permission behavior change.
- No service extraction or new product feature.

## Fallback Candidate

User Profile Source Selection Boundary.

Fallback condition:

- Use this if the user rejects role authority selection and wants to resolve profile ownership first while accepting that role authority will remain the larger permission/security blocker.

Bounded fallback goal:

- Produce a docs-only user profile source boundary artifact.
- Compare future profile sources such as external directory claims, user-service profile owner, auth-service profile store, synchronized profile read model or another backend-owned source.
- Define stable user-id mapping, display name/profile facts, department or organization fields if needed, active/deactivated user treatment, audit display semantics and rollback constraints.
- Preserve `UserContext` as runtime context and preserve demo headers as local/demo inputs.
- Do not select or migrate production role authority.

Why it is not primary:

- Role authority directly controls permission truth and current D008 security debt.
- `role-access-configs.json` remains a transition permission input, so role authority is the sharper blocker for gateway/JWT implementation, permission behavior, role-store migration and later service extraction.
- User profile source matters, but it can remain separate after role-authority direction is selected or bounded.

## Why Other Phases Are Not Selected

- Phase 001 through Phase 016 are completed and frozen.
- Gateway/JWT implementation design is deferred because production role authority and user profile source remain unresolved. Designing JWT claims and demo-header compatibility before role/profile authority would risk premature token, claim and permission assumptions.
- Service-to-service propagation and audit identity semantics are deferred because they require role authority, profile source, service-principal and delegated-actor inputs before detailed propagation can be selected.
- Config-store or role-store migration target/scoping is deferred because Phase 012 keeps JSON/prompt files as current transition stores and role-store migration should follow role-authority selection.
- Legacy route migration is deferred because Phase 006 intentionally froze current URLs. Migration needs explicit breaking-change or compatibility approval plus auth/gateway, issuer, role, profile and demo-header compatibility decisions.
- Report extraction or route migration is deferred because Phase 009 produced readiness documentation only. Report movement still depends on route, auth, identity, role, projection and contract decisions.
- Market-service or data-ingest-service extraction is deferred because Phase 010 readiness blockers remain, including auth, route, config, source sync and ingest ownership.
- Risk-service, strategy-service or projection-split planning is deferred because Phase 011 readiness blockers remain, including shared projection, generated events, auth, route, Redis and contract decisions.
- Generic eval/static guard expansion is deferred by decision order. It should attach to a bounded authority or contract phase rather than replace role-authority work.
- New product features and new agent work remain ineligible while authority, contract and transition-host lifecycle debt remains open.

## Window 1 Must Define

Window 1 must turn the primary candidate into an implementation-ready handoff and define:

- Exact Phase 017 scope and whether `docs/harness/21-production-role-authority-boundary.md` is the required durable artifact.
- Current facts inherited from Phase 012, Phase 013, Phase 014, Phase 015 and Phase 016:
  - `role-access-configs.json` is the current transition role/menu/permission config input.
  - `X-User-Id` and `X-User-Role` are demo/runtime inputs only.
  - `UserContext` is runtime context, not production identity or role authority.
  - backend explicit `requirePermission` calls are current checked-endpoint enforcement points.
  - intentional no-explicit-permission read surfaces remain stable.
  - frontend route/menu/action gating is UI affordance only.
  - production identity should be trusted only after a backend-owned ingress/auth boundary validates it.
  - backend-owned ingress/gateway JWT validation is the preferred future validator placement.
  - external IdP or enterprise directory is the preferred future issuer direction, future-only.
- Candidate production role authority options:
  - external IdP or directory claims/groups
  - backend DB role store
  - auth-service or user-service role ownership
  - config-store-backed role source
  - bounded continuation of `role-access-configs.json`
  - deliberate continued role-authority deferral with explicit readiness criteria
- Belongs rules for role assignment authority, role-permission mapping authority, permission-key/menu-key ownership, frontend gating, backend enforcement, role config storage, role audit and compatibility.
- Authority rules distinguishing identity issuer, identity validator, runtime user context, user profile source, role authority, role-permission mapping, frontend UI gating and local/demo headers.
- Contract boundaries for current role codes, permission keys, menu keys, backend `requirePermission` calls, no-explicit-permission read surfaces, current headers/defaults, frontend local role behavior, route/menu/action metadata and `role-access-configs.json` shape.
- Dependencies on Phase 006 route freeze, Phase 012 config-store boundary, Phase 013 permission inventory, Phase 014 auth/gateway target scope, Phase 015 validator placement and Phase 016 issuer direction.
- Whether Phase 017 only selects a future role authority direction or also recommends the smallest later gateway-design, profile-source, role-store or config-store phase.
- Verification commands appropriate for docs-only work, including read-only inventories of current role config, permission checks, frontend role utilities and existing Phase 006/007 guards if relevant.
- Stop rules if Window 2 discovers that Phase 017 requires gateway/auth/JWT implementation, external IdP integration, auth-service/user-service/role-service code, role-store migration, config mutation, permission behavior changes, route changes, frontend behavior changes, Java/Python changes, database changes or business behavior changes.

Default constraints Window 1 must carry forward:

- No breaking changes.
- Keep URL paths stable.
- Keep frontend routes stable.
- No permission behavior change.
- No business behavior change.
- No new feature work.
- No gateway/auth/JWT implementation.
- No auth-service, user-service, role-service, login/session, OAuth, SSO or external IdP implementation.
- No config mutation.
- No role-store or config-store migration.
- Expected Window 2 type: docs-only by default.

## Explicitly Out Of Scope

- No business code change from Window 0.
- No Window 2 implementation before Window 1 and human approval.
- No Java, Python, frontend, database, Redis, Kafka, `ai-config`, prompt-template, dependency, build, deployment or runtime behavior change by default.
- No gateway, auth-service, user-service, role-service, JWT, session, login flow, OAuth, SSO, external IdP integration or production identity implementation.
- No role-store migration, role DB adoption, config-store migration, DB/Nacos/hybrid adoption or config mutation.
- No route migration, route alias, endpoint rename, endpoint deletion, endpoint consolidation or gateway proxy.
- No permission behavior change, permission widening or permission narrowing.
- No new backend permission check added to a currently no-explicit-permission read surface.
- No service extraction.
- No DTO, VO, entity, mapper, schema, Kafka topic, Kafka payload, Redis key, frontend type or Python payload change.
- No new helper, adapter, bridge, fallback, wrapper, resolver, proxy, compatibility endpoint or temporary auth/role bridge.
- No reclassification of `ai-orchestration-service`, JSON config files, role-access headers, header-based demo auth, legacy `/api/tasks/*` paths, workbench aggregation or fallback provenance as final architecture.
- No closing D001, D002, D003, D007 or D008.
- No new product feature or new agent work.

## Human Approval Request

Please approve or reject the proposed primary candidate:

- Primary: Phase 017 - Production Role Authority Selection Boundary.
- Fallback: User Profile Source Selection Boundary.

Default approval meaning:

- No breaking changes.
- URL paths remain stable.
- Frontend routes remain stable.
- No permission behavior change.
- No business behavior change.
- No new feature work.
- No gateway/auth/JWT implementation.
- No auth-service, user-service, role-service, login/session, OAuth, SSO or external IdP implementation.
- No config mutation.
- No role-store or config-store migration.
- Expected Window 2 type is docs-only by default after Window 1 planning is separately approved.

## Human Approval Result

User approved the proposed primary candidate.

Primary candidate:

- Phase 017 - Production Role Authority Selection Boundary.

Fallback candidate:

- User Profile Source Selection Boundary.

Default approval meaning:

- No breaking changes.
- URL paths remain stable.
- Frontend routes remain stable.
- No permission behavior change.
- No business behavior change.
- No new feature work.
- No gateway/auth/JWT implementation.
- No auth-service, user-service, role-service, login/session, OAuth, SSO or external IdP implementation.
- No config mutation.
- No role-store or config-store migration.
- Expected Window 2 type is docs-only by default after Window 1 planning is separately approved.

Window 0 stops here. Window 1 is allowed to start Phase 017 architecture planning, but no implementation is approved.
