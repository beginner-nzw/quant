# Steering Decision - Phase 015

## Status

Window: Window 0 - Steering.

Decision: Phase 015 approved by user for Window 1 architecture planning.

Human approval status: approved.

This file does not approve implementation. Window 1 is now allowed to start architecture planning and must produce a Phase 015 architect handoff before any implementation window can start.

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

- Phase 014 - Production Auth/Gateway Target Scoping and Identity/Role Authority Selection.

Highest final handoff discovered:

- `docs/harness/handoffs/phase-014-final.md`

Matching Phase 014 handoffs consumed:

- `docs/harness/handoffs/steering-decision-phase-014.md`
- `docs/harness/handoffs/phase-014-architect.md`
- `docs/harness/handoffs/phase-014-implementation.md`
- `docs/harness/handoffs/phase-014-review.md`
- `docs/harness/handoffs/phase-014-final.md`

Durable readiness artifacts consumed:

- `docs/harness/12-transition-host-exit-criteria.md`
- `docs/harness/16-config-store-decision-boundary.md`
- `docs/harness/17-auth-gateway-permission-boundary.md`
- `docs/harness/18-production-auth-gateway-target-scope.md`

Missing matching handoff files:

- None for Phase 014.

## Startup Recovery Result

`docs/harness/state/current-state.md` and the handoff directory agree:

- Current active phase: none approved.
- Last completed phase: Phase 014 - Production Auth/Gateway Target Scoping and Identity/Role Authority Selection.
- Latest final handoff: `phase-014-final.md`.
- Open blockers: none registered.

Bootstrap Phase 001 is not current fact because Phase 001 through Phase 014 are completed and frozen by Window 4.

## Current State Summary

- Phase 014 completed docs-only production auth/gateway target scoping.
- `docs/harness/18-production-auth-gateway-target-scope.md` is now the durable production auth/gateway target-scope artifact.
- Production identity should be accepted through a future backend-owned ingress/auth boundary.
- Gateway/JWT is the preferred future identity target shape, but the concrete issuer or validator remains deferred.
- Production role authority must be backend-owned in a later phase, while `role-access-configs.json` remains the current transition role/menu/permission config input.
- `X-User-Id` and `X-User-Role` remain local/demo compatibility inputs only.
- Service-to-service propagation, audit identity semantics, demo-header compatibility or retirement, route migration and role/config-store dependencies remain future requirements only.
- `ai-orchestration-service` remains a multi-domain transition host, not final architecture.
- Legacy non-task `/api/tasks/*` paths remain frozen transitional contracts under Phase 006.
- Open debt remains for D001, D002, D003, D007 and D008.
- No main path breakage is registered.

## Latest Completed Or Blocked Phase

Latest completed phase:

- Phase 014 - Production Auth/Gateway Target Scoping and Identity/Role Authority Selection.

Latest blocked phase:

- None registered.

Handoff files read for the latest completed phase:

- `steering-decision-phase-014.md`
- `phase-014-architect.md`
- `phase-014-implementation.md`
- `phase-014-review.md`
- `phase-014-final.md`

## Decision Order Result

Decision order from `docs/harness/10-steering-state-machine.md`:

```text
main path breakage -> authority ambiguity -> contract ambiguity -> transition host reduction -> eval/test coverage -> new feature work
```

Evaluation:

- Main path breakage: none registered.
- Authority ambiguity: still present. Phase 014 selected the future-only ingress/auth target direction, but the concrete production identity issuer or validator is not selected. Production role authority is also not selected, but role choices depend on whether identity comes from an internal auth/user service, an external IdP or another backend-owned validator.
- Contract ambiguity: still present. Phase 006 freezes legacy non-task `/api/tasks/*` routes, and Phase 014 records auth/gateway compatibility prerequisites. Route migration is not ready until identity and role authority are selected.
- Transition host reduction: still needed for D001, but extraction and route migration remain blocked by identity, role, propagation, config and domain-specific readiness gates.
- Eval/test coverage: useful, but lower order than the remaining authority and contract decisions.
- New feature work: not eligible while authority, contract and transition-host lifecycle debt remains open.

## Candidate Score Table

Scores are 0-2. Behavior risk is inverted: 2 means low risk.

| Candidate | Main path protection | Authority clarity | Contract clarity | Transition host reduction | Behavior risk | Verification feasibility | Total | Steering result |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| Phase 015 - Production Identity Issuer/Validator Selection Boundary | 1 | 2 | 2 | 1 | 2 | 2 | 10 | Primary |
| Production Role Authority Selection Boundary | 1 | 2 | 2 | 1 | 2 | 2 | 10 | Fallback |
| Service-to-service propagation and audit identity semantics boundary | 1 | 2 | 1 | 1 | 2 | 2 | 9 | Defer |
| Config-store schema/versioning and audit/rollback readiness | 1 | 1 | 2 | 1 | 2 | 2 | 9 | Defer |
| Gateway/JWT implementation design with demo-header compatibility policy | 1 | 1 | 2 | 1 | 1 | 2 | 8 | Defer |
| Legacy route migration decision phase | 0 | 1 | 2 | 2 | 0 | 1 | 6 | Defer |
| Report extraction or report route-migration planning | 1 | 1 | 1 | 2 | 0 | 1 | 6 | Defer |
| Market-service or data-ingest-service extraction planning | 1 | 1 | 1 | 2 | 0 | 1 | 6 | Defer |
| Risk-service, strategy-service or projection-split planning | 1 | 1 | 1 | 2 | 0 | 1 | 6 | Defer |
| Generic eval/static guard expansion | 1 | 1 | 1 | 0 | 2 | 2 | 7 | Defer by decision order |
| New product feature or new agent work | 0 | 0 | 0 | 0 | 0 | 0 | 0 | Not eligible |

Tie-break note:

- Production identity issuer/validator selection and production role authority selection tie on numeric score.
- Identity is selected first because Phase 014 already chose the future ingress/auth direction but left the concrete issuer or validator unresolved.
- Role authority may depend on that identity choice: auth-service/user-service ownership, external IdP claims, external directory groups or local backend role store are not equivalent role-authority shapes.

## Primary Candidate

Phase 015 - Production Identity Issuer/Validator Selection Boundary.

Bounded goal:

- Produce a docs-only identity issuer/validator selection boundary artifact.
- Decide, at governance level only, which production identity issuer or validator direction should be prepared for later implementation.
- Compare and constrain candidate identity sources such as future auth-service, future user-service, external IdP integration, gateway-local JWT validation, or another backend-owned validator.
- Preserve Phase 014's target direction: production identity must be trusted only after a backend-owned ingress/auth boundary validates it.
- Preserve demo headers as local/demo compatibility inputs unless a later approved phase changes them.
- Define what must stay stable for current request headers, default `guest` / `USER` behavior, frontend local selected user, current backend `UserContext`, permission keys, role codes, menu keys and no-explicit-permission read surfaces.
- Define what Window 1 must inventory before any later implementation: issuer ownership, validator ownership, token/session semantics, user profile source, service-to-service identity handoff, audit identity fields, route/gateway compatibility and rollback constraints.

Expected durable artifact:

- `docs/harness/19-production-identity-issuer-validator-boundary.md`

Why this is the next bounded step:

- Phase 014 resolved the broad target direction but deliberately deferred the concrete identity issuer or validator.
- Identity issuer/validator selection is a higher-order authority decision than route migration, service extraction, config-store migration or gateway implementation design.
- It is closer than Gateway/JWT implementation because it can stay docs-only and establish the authority target before any code, deployment or contract change.
- It is closer than production role authority selection because role authority options depend on whether identity arrives from internal auth/user services, external IdP claims or another backend-owned validator.
- It helps later Window 0 decisions sequence role authority, service-to-service propagation, audit identity, gateway/JWT implementation, route migration and domain extraction without making the transition host permanent.

Expected phase shape:

- Docs-only architecture/governance work by default.
- No Java, Python, frontend, database, Redis, Kafka, config, dependency, build, deployment or runtime behavior change.
- No gateway/auth/JWT implementation.
- No auth-service, user-service, role-service, login/session, OAuth, SSO or external IdP implementation.
- No route migration, route alias, endpoint rename, endpoint deletion, endpoint consolidation or gateway proxy.
- No permission behavior change.
- No config mutation, config-store migration or role-store migration.
- No service extraction or new product feature.

## Fallback Candidate

Production Role Authority Selection Boundary.

Fallback condition:

- Use this if the user rejects identity issuer/validator selection and wants to act on Phase 014 role-authority gates first.

Bounded fallback goal:

- Produce a docs-only production role authority boundary artifact.
- Compare DB role store, config-store-backed role source, auth-service/user-service role ownership, external IdP claims and bounded continuation of `role-access-configs.json`.
- Keep `role-access-configs.json` as the current transition role/menu/permission input unless a later approved phase changes it.
- Preserve current role codes, permission keys, menu keys, frontend route/menu/action gating behavior, backend `requirePermission` calls and intentional no-explicit-permission read surfaces.

Why it is not primary:

- Phase 014 requires production role authority to be backend-owned, but the concrete identity issuer/validator is still unresolved.
- If identity is later delegated to an external IdP, role authority could plausibly come from external claims or directory groups.
- If identity is later owned by auth-service or user-service, role ownership might belong there or in a separate backend role store.
- Selecting role authority before identity issuer/validator risks choosing a role model that does not fit the eventual identity boundary.

## Why Other Phases Are Not Selected

- Phase 001 through Phase 014 are completed and frozen.
- Gateway/JWT implementation design is deferred because the concrete production identity issuer or validator is not yet selected. Designing implementation before identity authority is fixed would risk premature contract and deployment assumptions.
- Service-to-service propagation and audit identity semantics are deferred because they need identity and role authority inputs before implementation or detailed propagation shape can be selected.
- Config-store migration readiness is deferred because Phase 012 already keeps JSON and prompt files as current transition stores, and role-access config migration depends on identity and role authority decisions.
- Legacy route migration is deferred because Phase 006 intentionally froze current URLs. Migration needs explicit breaking-change or compatibility approval plus auth/gateway compatibility from Phase 014 and later identity/role decisions.
- Report extraction or route migration is deferred because Phase 009 produced readiness documentation only. Report movement still depends on route, auth, identity, role, projection and contract decisions.
- Market-service or data-ingest-service extraction is deferred because Phase 010 readiness blockers remain, including auth, route, config, source sync and ingest ownership.
- Risk-service, strategy-service or projection-split planning is deferred because Phase 011 readiness blockers remain, including shared projection, generated events, auth, route, Redis and contract decisions.
- Generic eval/static guard expansion is deferred by decision order. It should attach to a bounded authority or contract phase rather than replace identity-authority work.
- New product features and new agent work remain ineligible while authority, contract and transition-host lifecycle debt remains open.

## Window 1 Must Define

Window 1 must turn the primary candidate into an implementation-ready handoff and define:

- Exact Phase 015 scope and whether `docs/harness/19-production-identity-issuer-validator-boundary.md` is the required durable artifact.
- Current facts inherited from Phase 013 and Phase 014:
  - `X-User-Id` and `X-User-Role` are demo/runtime inputs only.
  - `role-access-configs.json` is the current transition role/menu/permission config input.
  - production identity should be accepted through a future backend-owned ingress/auth boundary.
  - gateway/JWT is the preferred target shape, but no issuer or validator is implemented.
  - backend explicit `requirePermission` calls are current enforcement points.
  - frontend route/menu/action gating is UI affordance only.
- Candidate production identity issuer/validator options:
  - gateway-local JWT validator
  - auth-service issuer/validator
  - user-service profile owner with separate validator
  - external IdP or directory integration
  - continued demo-header compatibility for local/demo only
  - deliberately deferred issuer/validator if no safe selection can be made
- Belongs rules for future ingress, token validation, user profile ownership, service principal validation and audit identity.
- Authority rules distinguishing current runtime context from future trusted identity authority.
- Contract boundaries for headers, tokens/sessions, user id shape, trace id propagation, frontend local role selection, current no-explicit-permission read surfaces and checked command surfaces.
- Dependencies on Phase 006 route freeze, Phase 012 config-store boundary, Phase 013 permission inventory and Phase 014 target-scope artifact.
- Whether Phase 015 only selects a future issuer/validator direction or also recommends the smallest later implementation-design phase.
- Verification commands appropriate for docs-only work, including read-only inventories of current auth/permission/header/frontend surfaces and existing Phase 006/007 guards if relevant.
- Stop rules if Window 2 discovers that Phase 015 requires gateway/auth/JWT implementation, auth-service/user-service code, external IdP integration, permission behavior changes, route changes, config mutation, frontend behavior changes, Java/Python changes, database changes or business behavior changes.

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
- Expected Window 2 type: docs-only by default.

## Explicitly Out Of Scope

- No business code change from Window 0.
- No Window 2 implementation before Window 1 and human approval.
- No Java, Python, frontend, database, Redis, Kafka, `ai-config`, prompt-template, dependency, build, deployment or runtime behavior change by default.
- No gateway, auth-service, user-service, role-service, JWT, session, login flow, OAuth, SSO, external IdP integration or production identity implementation.
- No route migration, route alias, endpoint rename, endpoint deletion, endpoint consolidation or gateway proxy.
- No permission behavior change, permission widening or permission narrowing.
- No config-store migration, DB/Nacos/hybrid adoption, config mutation or role-store migration.
- No service extraction.
- No DTO, VO, entity, mapper, schema, Kafka topic, Kafka payload, Redis key, frontend type or Python payload change.
- No new helper, adapter, bridge, fallback, wrapper, resolver, proxy, compatibility endpoint or temporary auth bridge.
- No reclassification of `ai-orchestration-service`, JSON config files, role-access headers, header-based demo auth, legacy `/api/tasks/*` paths, workbench aggregation or fallback provenance as final architecture.
- No closing D001, D002, D003, D007 or D008.
- No new product feature or new agent work.

## Human Approval Result

User approved the proposed primary candidate.

Primary candidate:

- Phase 015 - Production Identity Issuer/Validator Selection Boundary.

Fallback candidate:

- Production Role Authority Selection Boundary.

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
- Expected Window 2 type is docs-only by default after Window 1 planning is separately approved.

Window 0 stops here. Window 1 is allowed to start Phase 015 architecture planning, but no implementation is approved.
