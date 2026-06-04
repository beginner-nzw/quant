# Steering Decision - Phase 016

## Status

Window: Window 0 - Steering.

Decision: Phase 016 approved by user for Window 1 architecture planning.

Human approval status: approved.

This file does not approve implementation. Window 1 is now allowed to start architecture planning and must produce a Phase 016 architect handoff before any implementation window can start.

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

- Phase 015 - Production Identity Issuer/Validator Selection Boundary.

Highest final handoff discovered:

- `docs/harness/handoffs/phase-015-final.md`

Matching Phase 015 handoffs consumed:

- `docs/harness/handoffs/steering-decision-phase-015.md`
- `docs/harness/handoffs/phase-015-architect.md`
- `docs/harness/handoffs/phase-015-implementation.md`
- `docs/harness/handoffs/phase-015-review.md`
- `docs/harness/handoffs/phase-015-final.md`

Durable boundary artifacts consumed:

- `docs/harness/12-transition-host-exit-criteria.md`
- `docs/harness/13-report-boundary-readiness.md`
- `docs/harness/14-market-data-ingest-boundary-readiness.md`
- `docs/harness/15-risk-strategy-projection-boundary-readiness.md`
- `docs/harness/16-config-store-decision-boundary.md`
- `docs/harness/17-auth-gateway-permission-boundary.md`
- `docs/harness/18-production-auth-gateway-target-scope.md`
- `docs/harness/19-production-identity-issuer-validator-boundary.md`

Missing matching handoff files:

- None for Phase 015.

## Startup Recovery Result

`docs/harness/state/current-state.md` and the handoff directory agree:

- Current active phase: none approved.
- Last completed phase: Phase 015 - Production Identity Issuer/Validator Selection Boundary.
- Latest final handoff: `phase-015-final.md`.
- Open blockers: none registered.

Bootstrap Phase 001 is not current fact because Phase 001 through Phase 015 are completed and frozen by Window 4.

## Current State Summary

- Phase 015 completed docs-only production identity issuer/validator governance work.
- `docs/harness/19-production-identity-issuer-validator-boundary.md` is now the durable production identity issuer/validator boundary artifact.
- Backend-owned ingress/gateway JWT validation is selected as the preferred future production identity validator placement.
- The concrete production identity issuer remains deferred.
- User profile source remains deferred.
- Production role authority remains deferred.
- `X-User-Id` and `X-User-Role` remain local/demo compatibility inputs only.
- `UserContext` remains runtime context, not production identity authority.
- `role-access-configs.json` remains the current transition role/menu/permission input under Phase 012.
- `ai-orchestration-service` remains a multi-domain transition host, not final architecture.
- Legacy non-task `/api/tasks/*` paths remain frozen transitional contracts under Phase 006.
- Open debt remains for D001, D002, D003, D007 and D008.
- No main path breakage is registered.

## Latest Completed Or Blocked Phase

Latest completed phase:

- Phase 015 - Production Identity Issuer/Validator Selection Boundary.

Latest blocked phase:

- None registered.

Handoff files read for the latest completed phase:

- `steering-decision-phase-015.md`
- `phase-015-architect.md`
- `phase-015-implementation.md`
- `phase-015-review.md`
- `phase-015-final.md`

## Decision Order Result

Decision order from `docs/harness/10-steering-state-machine.md`:

```text
main path breakage -> authority ambiguity -> contract ambiguity -> transition host reduction -> eval/test coverage -> new feature work
```

Evaluation:

- Main path breakage: none registered.
- Authority ambiguity: still present. Phase 015 selected future validator placement but deliberately deferred the concrete production identity issuer, user profile source and production role authority.
- Contract ambiguity: still present. Gateway/JWT, issuer integration, demo-header compatibility, service-to-service propagation and role authority decisions are prerequisites for route migration and later extraction.
- Transition host reduction: still needed for D001, but extraction or route migration remains blocked by identity, role, propagation, config and domain-specific gates.
- Eval/test coverage: useful, but lower order than the remaining authority decisions.
- New feature work: not eligible while authority, contract and transition-host lifecycle debt remains open.

## Candidate Score Table

Scores are 0-2. Behavior risk is inverted: 2 means low risk.

| Candidate | Main path protection | Authority clarity | Contract clarity | Transition host reduction | Behavior risk | Verification feasibility | Total | Steering result |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| Phase 016 - Production Identity Issuer Selection Boundary | 1 | 2 | 2 | 1 | 2 | 2 | 10 | Primary |
| Production Role Authority Selection Boundary | 1 | 2 | 2 | 1 | 2 | 2 | 10 | Fallback |
| Gateway/JWT implementation design with demo-header compatibility policy | 1 | 1 | 2 | 1 | 1 | 2 | 8 | Defer |
| Service-to-service propagation and audit identity semantics boundary | 1 | 2 | 1 | 1 | 2 | 2 | 9 | Defer |
| Config-store schema/versioning and audit/rollback readiness | 1 | 1 | 2 | 1 | 2 | 2 | 9 | Defer |
| Legacy route migration decision phase | 0 | 1 | 2 | 2 | 0 | 1 | 6 | Defer |
| Report extraction or report route-migration planning | 1 | 1 | 1 | 2 | 0 | 1 | 6 | Defer |
| Market-service or data-ingest-service extraction planning | 1 | 1 | 1 | 2 | 0 | 1 | 6 | Defer |
| Risk-service, strategy-service or projection-split planning | 1 | 1 | 1 | 2 | 0 | 1 | 6 | Defer |
| Generic eval/static guard expansion | 1 | 1 | 1 | 0 | 2 | 2 | 7 | Defer by decision order |
| New product feature or new agent work | 0 | 0 | 0 | 0 | 0 | 0 | 0 | Not eligible |

Tie-break note:

- Production identity issuer selection and production role authority selection tie on numeric score.
- Identity issuer selection is selected first because Phase 015 already chose validator placement but explicitly deferred issuer selection.
- Role authority may depend on the issuer source. Internal auth-service, external IdP, user-service profile ownership and another backend-owned issuer imply different role-authority shapes.

## Primary Candidate

Phase 016 - Production Identity Issuer Selection Boundary.

Bounded goal:

- Produce a docs-only production identity issuer selection boundary artifact.
- Decide, at governance level only, the preferred future production identity issuer direction or explicitly keep issuer selection deferred with narrower criteria.
- Use Phase 015's selected validator placement as input: backend-owned ingress/gateway JWT validation remains the preferred future validator placement.
- Compare issuer candidates such as internal auth-service issuer, external IdP/directory issuer, user-service-adjacent profile source with separate issuer, or another backend-owned issuer.
- Define user profile source dependencies, token claim dependencies, role authority dependencies, service-principal dependencies, demo-header compatibility dependencies and rollback constraints.
- Preserve `X-User-Id` and `X-User-Role` as local/demo compatibility inputs only.

Expected durable artifact:

- `docs/harness/20-production-identity-issuer-boundary.md`

Why this is the next bounded step:

- Phase 015 selected where production identity should be validated, but not who issues production identity.
- Issuer authority is a higher-order authority decision than gateway/JWT implementation design, route migration, service extraction, config-store migration or new feature work.
- It is the closest next step because it can remain docs-only while resolving the explicit Phase 015 deferral.
- It should happen before production role authority selection because role authority may come from issuer claims, external directory groups, a backend role store, auth-service/user-service ownership or a retained transition role config.
- It should happen before service-to-service propagation and audit identity implementation because propagation depends on what issuer and claim model are trusted.

Expected phase shape:

- Docs-only architecture/governance work by default.
- No Java, Python, frontend, database, Redis, Kafka, config, dependency, build, deployment or runtime behavior change.
- No gateway/auth/JWT implementation.
- No auth-service, user-service, role-service, login/session, OAuth, SSO or external IdP implementation.
- No role authority migration.
- No route migration, route alias, endpoint rename, endpoint deletion, endpoint consolidation or gateway proxy.
- No permission behavior change.
- No config mutation, config-store migration or role-store migration.
- No service extraction or new product feature.

## Fallback Candidate

Production Role Authority Selection Boundary.

Fallback condition:

- Use this if the user rejects concrete issuer selection and wants to resolve role authority first while accepting the risk that issuer choice may later reshape role ownership.

Bounded fallback goal:

- Produce a docs-only production role authority boundary artifact.
- Compare DB role store, config-store-backed role source, auth-service/user-service role ownership, external IdP/directory claims and bounded continuation of `role-access-configs.json`.
- Keep `role-access-configs.json` as the current transition role/menu/permission input unless a later approved phase changes it.
- Preserve current role codes, permission keys, menu keys, frontend route/menu/action gating behavior, backend `requirePermission` calls and intentional no-explicit-permission read surfaces.

Why it is not primary:

- Phase 015 left issuer selection unresolved.
- If a future external IdP is selected, role authority could plausibly depend on claims or directory groups.
- If a future internal auth-service or user-service is selected, role ownership may belong there or in a backend role store.
- Selecting role authority before issuer risks choosing a role model that does not fit the eventual production identity source.

## Why Other Phases Are Not Selected

- Phase 001 through Phase 015 are completed and frozen.
- Gateway/JWT implementation design is deferred because the concrete production identity issuer is not yet selected. Implementation design before issuer authority would risk premature token, claim, compatibility and deployment assumptions.
- Service-to-service propagation and audit identity semantics are deferred because they need issuer, role authority and service-principal inputs before detailed propagation can be selected.
- Config-store migration readiness is deferred because Phase 012 keeps JSON and prompt files as current transition stores, and role/config migration depends on identity issuer and role authority decisions.
- Legacy route migration is deferred because Phase 006 intentionally froze current URLs. Migration needs explicit breaking-change or compatibility approval plus auth/gateway, issuer, role and demo-header compatibility decisions.
- Report extraction or route migration is deferred because Phase 009 produced readiness documentation only. Report movement still depends on route, auth, identity, role, projection and contract decisions.
- Market-service or data-ingest-service extraction is deferred because Phase 010 readiness blockers remain, including auth, route, config, source sync and ingest ownership.
- Risk-service, strategy-service or projection-split planning is deferred because Phase 011 readiness blockers remain, including shared projection, generated events, auth, route, Redis and contract decisions.
- Generic eval/static guard expansion is deferred by decision order. It should attach to a bounded authority or contract phase rather than replace issuer-authority work.
- New product features and new agent work remain ineligible while authority, contract and transition-host lifecycle debt remains open.

## Window 1 Must Define

Window 1 must turn the primary candidate into an implementation-ready handoff and define:

- Exact Phase 016 scope and whether `docs/harness/20-production-identity-issuer-boundary.md` is the required durable artifact.
- Current facts inherited from Phase 013, Phase 014 and Phase 015:
  - `X-User-Id` and `X-User-Role` are demo/runtime inputs only.
  - `role-access-configs.json` is the current transition role/menu/permission config input.
  - production identity should be trusted only after a backend-owned ingress/auth boundary validates it.
  - backend-owned ingress/gateway JWT validation is the preferred future validator placement.
  - concrete issuer, user profile source and role authority remain deferred after Phase 015.
  - backend explicit `requirePermission` calls are current enforcement points.
  - frontend route/menu/action gating is UI affordance only.
- Candidate production identity issuer options:
  - internal auth-service issuer
  - external IdP or directory issuer
  - user-service profile owner with a separate issuer
  - another backend-owned issuer
  - deliberate continued issuer deferral with explicit readiness criteria
- Belongs rules for issuer, validator, user profile source, token/session metadata, service principals and audit identity.
- Authority rules distinguishing issuer, validator, user profile, role authority, runtime context and local/demo headers.
- Contract boundaries for token claims, current headers, current defaults, permission keys, role codes, menu keys, frontend local role behavior, current no-explicit-permission reads and checked commands.
- Dependencies on Phase 006 route freeze, Phase 012 config-store boundary, Phase 013 permission inventory, Phase 014 auth/gateway target scope and Phase 015 validator placement.
- Whether Phase 016 only selects a future issuer direction or also recommends the smallest later role-authority or gateway-design phase.
- Verification commands appropriate for docs-only work, including read-only inventories of current auth/permission/header/frontend surfaces and existing Phase 006/007 guards if relevant.
- Stop rules if Window 2 discovers that Phase 016 requires gateway/auth/JWT implementation, auth-service/user-service code, external IdP integration, permission behavior changes, route changes, config mutation, frontend behavior changes, Java/Python changes, database changes or business behavior changes.

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

- Phase 016 - Production Identity Issuer Selection Boundary.

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

Window 0 stops here. Window 1 is allowed to start Phase 016 architecture planning, but no implementation is approved.
