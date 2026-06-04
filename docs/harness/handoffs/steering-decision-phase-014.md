# Steering Decision - Phase 014

## Status

Window: Window 0 - Steering.

Decision: Phase 014 approved by user for Window 1 architecture planning.

Human approval status: approved.

This file does not approve implementation. Window 1 is now allowed to start architecture planning and must produce a Phase 014 architect handoff before any implementation window can start.

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

- Phase 013 - Auth/Gateway Permission Authority Boundary.

Highest final handoff discovered:

- `docs/harness/handoffs/phase-013-final.md`

Matching Phase 013 handoffs consumed:

- `docs/harness/handoffs/steering-decision-phase-013.md`
- `docs/harness/handoffs/phase-013-architect.md`
- `docs/harness/handoffs/phase-013-implementation.md`
- `docs/harness/handoffs/phase-013-review.md`
- `docs/harness/handoffs/phase-013-final.md`

Durable readiness artifacts consumed:

- `docs/harness/12-transition-host-exit-criteria.md`
- `docs/harness/13-report-boundary-readiness.md`
- `docs/harness/14-market-data-ingest-boundary-readiness.md`
- `docs/harness/15-risk-strategy-projection-boundary-readiness.md`
- `docs/harness/16-config-store-decision-boundary.md`
- `docs/harness/17-auth-gateway-permission-boundary.md`

Missing matching handoff files:

- None for Phase 013.

## Startup Recovery Result

`docs/harness/state/current-state.md` and the handoff directory agree:

- Current active phase: none approved.
- Last completed phase: Phase 013 - Auth/Gateway Permission Authority Boundary.
- Latest final handoff: `phase-013-final.md`.
- Open blockers: none registered.

Bootstrap Phase 001 is not current fact because Phase 001 through Phase 013 are completed and frozen by Window 4.

## Current State Summary

- Phase 013 completed docs-only auth/gateway permission boundary work.
- `docs/harness/17-auth-gateway-permission-boundary.md` is now the durable auth/gateway permission boundary artifact.
- Header-based demo auth remains current transition behavior for the next governance horizon.
- `role-access-configs.json` remains the current role/menu/permission config input under the Phase 012 JSON transition-store policy.
- `X-User-Id` and `X-User-Role` remain demo/runtime inputs, not production identity or production role authority.
- Backend explicit `requirePermission` calls remain current enforcement points for checked endpoints.
- Frontend route/menu/action gating remains UI affordance only.
- Production gateway/auth/JWT, auth-service, user-service, role-store, login/session, service-to-service propagation and demo-header retirement remain deferred future decisions.
- `ai-orchestration-service` remains a multi-domain transition host, not final architecture.
- Legacy non-task `/api/tasks/*` paths remain frozen transitional contracts under Phase 006.
- Open debt remains for D001, D002, D003, D007 and D008.
- No main path breakage is registered.

## Latest Completed Or Blocked Phase

Latest completed phase:

- Phase 013 - Auth/Gateway Permission Authority Boundary.

Latest blocked phase:

- None registered.

Handoff files read for the latest completed phase:

- `steering-decision-phase-013.md`
- `phase-013-architect.md`
- `phase-013-implementation.md`
- `phase-013-review.md`
- `phase-013-final.md`

## Decision Order Result

Decision order from `docs/harness/10-steering-state-machine.md`:

```text
main path breakage -> authority ambiguity -> contract ambiguity -> transition host reduction -> eval/test coverage -> new feature work
```

Evaluation:

- Main path breakage: none registered.
- Authority ambiguity: still present. Phase 013 documented current permission boundaries, but production identity authority, production role authority, service-to-service propagation and demo-header compatibility or retirement are not selected.
- Contract ambiguity: still present. Phase 006 freezes legacy non-task `/api/tasks/*` routes; Phase 013 documents permission compatibility blockers, but route migration is not ready without auth/role authority decisions.
- Transition host reduction: still needed for D001, but extraction and route migration remain blocked by auth, role, config, route and domain-specific readiness gates.
- Eval/test coverage: useful, but lower order than the remaining authority and contract decisions.
- New feature work: not eligible while authority, contract and transition-host lifecycle debt remains open.

## Candidate Score Table

Scores are 0-2. Behavior risk is inverted: 2 means low risk.

| Candidate | Main path protection | Authority clarity | Contract clarity | Transition host reduction | Behavior risk | Verification feasibility | Total | Steering result |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| Phase 014 - Production Auth/Gateway Target Scoping and Identity/Role Authority Selection | 1 | 2 | 2 | 1 | 2 | 2 | 10 | Primary |
| Config-store schema/versioning and audit/rollback readiness | 1 | 1 | 2 | 1 | 2 | 2 | 9 | Fallback |
| Legacy route migration decision phase | 0 | 1 | 2 | 2 | 0 | 1 | 6 | Defer |
| Risk-service, strategy-service or projection-split planning | 1 | 1 | 1 | 2 | 0 | 1 | 6 | Defer |
| Market-service or data-ingest-service extraction planning | 1 | 1 | 1 | 2 | 0 | 1 | 6 | Defer |
| Report extraction or report route-migration planning | 1 | 1 | 1 | 2 | 0 | 1 | 6 | Defer |
| Generic eval/static guard expansion | 1 | 1 | 1 | 0 | 2 | 2 | 7 | Defer by decision order |
| Direct permanent modular-monolith declaration | 1 | 1 | 1 | 1 | 0 | 0 | 4 | Defer |
| New product feature or new agent work | 0 | 0 | 0 | 0 | 0 | 0 | 0 | Not eligible |

Tie-break and ordering notes:

- Auth/gateway target scoping is selected because it addresses the highest-order remaining authority ambiguity after Phase 013: production identity authority, production role authority, service-to-service propagation and demo-header compatibility.
- Config-store readiness is a strong fallback, but Phase 012 already selected JSON config and prompt files as current transition stores for the next governance horizon. Config migration readiness should account for the permission and role-authority decisions from Phase 013 and any Phase 014 auth target scoping.
- Route migration and service extraction are higher behavior risk and require auth/role compatibility decisions before they can be safely planned.

## Primary Candidate

Phase 014 - Production Auth/Gateway Target Scoping and Identity/Role Authority Selection.

Bounded goal:

- Produce a docs-only target-scoping and decision-boundary artifact for production auth/gateway readiness.
- Clarify the future target options for identity authority and role authority without implementing them.
- Decide, at governance level only, whether a later implementation phase should aim for gateway/auth/JWT, auth-service, user-service, role-service, external IdP, continued demo-header compatibility, demo-header retirement or another approved path.
- Define service-to-service propagation requirements for current async and internal flows, including event auto task dispatch and any future extracted services.
- Define how current `X-User-Id`, `X-User-Role`, `role-access-configs.json`, frontend UI gating and backend `requirePermission` checks would be preserved, retired or migrated in a later approved phase.
- Preserve Phase 006 route freeze, Phase 012 config-store boundary and Phase 013 permission behavior inventory.

Expected durable artifact:

- `docs/harness/18-production-auth-gateway-target-scope.md`

Why this is the next bounded step:

- Phase 013 made the current permission boundary explicit but deliberately did not choose production identity or role authority.
- Report, market, risk, strategy, config, route migration and service extraction readiness artifacts all list auth/gateway or permission authority as a future blocker.
- This phase is closer than route migration or service extraction because it resolves a shared authority prerequisite before changing contracts or hosts.
- It can remain docs-only, no behavior change and no breaking change, while giving Window 1 a precise boundary to plan.

Expected phase shape:

- Docs-only architecture/governance work by default.
- No production auth implementation.
- No gateway, JWT, session, login, OAuth, SSO, auth-service, user-service, role-service, role DB, route proxy or service extraction implementation.
- No permission behavior change.
- No URL, frontend route, API, DTO/VO/entity, database, Redis, Kafka, config file, Python payload or business behavior change.
- No reclassification of header-based demo auth or JSON role config as production security.

## Fallback Candidate

Config-store schema/versioning and audit/rollback readiness.

Fallback condition:

- Use this if the user rejects auth/gateway target scoping and wants to continue acting on Phase 012 and Phase 013 config/role-access readiness gates first.

Bounded fallback goal:

- Define future migration prerequisites for config schema/versioning, single-writer rules, audit retention, rollback, Java/Python reader compatibility, prompt-template rollout, role-access interaction and event-source/ingest-history handling.
- Keep JSON config and prompt templates as the current runtime stores.
- Do not select or implement DB, Nacos or hybrid migration unless a later Window 0 decision and human approval explicitly choose that target.

Why it is not primary:

- Phase 012 already selected JSON config and prompt files as the next-governance-horizon runtime transition stores.
- Phase 013 shows role-access config is tied to permission authority. A production auth/role target scoping phase should come before deeper role-store or config-store migration readiness.
- Config readiness remains valuable, but it is less cross-cutting than resolving identity, role and service-to-service permission authority prerequisites.

## Why Other Phases Are Not Selected

- Phase 001 through Phase 013 are completed and frozen.
- Legacy route migration is deferred because Phase 006 intentionally froze current URLs. Migration needs explicit breaking-change or compatibility approval, an updated contract inventory and an auth/role compatibility plan from Phase 013 or Phase 014.
- Report extraction or route migration is deferred because Phase 009 produced readiness documentation only. Report ownership movement still depends on route, auth, projection and contract decisions.
- Market-service or data-ingest-service extraction is deferred because Phase 010 produced readiness documentation only. Event source config, ingest history, source preview/diagnose, mock ingest, CNINFO proxy, auth and route blockers remain.
- Risk-service, strategy-service or projection-split planning is deferred because Phase 011 produced readiness documentation only. Shared projection, generated-event, auth, route, Redis and contract blockers remain.
- Generic eval/static guard expansion is deferred by decision order. It should attach to a bounded authority or contract phase instead of replacing auth/role authority work.
- Direct permanent modular-monolith declaration is too far because the transition host is explicitly not final architecture.
- New product features and new agent work remain ineligible while authority, contract and transition-host lifecycle debt remains open.

## Window 1 Must Define

Window 1 must turn the primary candidate into an implementation-ready handoff and define:

- Exact Phase 014 scope and whether `docs/harness/18-production-auth-gateway-target-scope.md` is the required durable artifact.
- Current facts inherited from Phase 013:
  - `X-User-Id` and `X-User-Role` are demo/runtime inputs.
  - `role-access-configs.json` is current role/menu/permission config input.
  - backend explicit `requirePermission` checks are current enforcement points.
  - frontend route/menu/action gating is UI affordance only.
- Candidate production identity-authority options:
  - gateway/JWT
  - auth-service
  - user-service
  - external identity provider
  - continued demo-header compatibility for local/demo only
- Candidate production role-authority options:
  - keep JSON transition store for a bounded horizon
  - DB role store
  - Nacos/config-store-backed role config
  - auth-service or user-service role ownership
  - external directory or IdP role claims
- Service-to-service propagation requirements for task creation, AI callbacks, event auto task dispatch, future extracted services and audit identity.
- Contract boundaries for request headers, tokens/sessions, permission keys, menu keys, role codes, frontend route gating, existing no-explicit-permission read surfaces and checked command surfaces.
- Compatibility or retirement rules for demo headers, including whether later phases must preserve them for local/demo use.
- Dependencies on Phase 006 route freeze, Phase 012 config-store boundary and Phase 013 permission inventory.
- Whether the Phase 014 artifact only scopes targets or also recommends a single future target for a later implementation phase.
- Belongs, authority, contract, transition-lifetime and behavior acceptance gates.
- Verification commands appropriate for docs-only work, including read-only inventories of current auth/permission/config/frontend surfaces and existing Phase 006/007 guards.
- Stop rules if Window 2 discovers that Phase 014 requires gateway/auth implementation, permission behavior changes, route changes, config mutation, frontend behavior changes, Java/Python changes, database changes, JWT/session work or business behavior changes.

Default constraints Window 1 must carry forward:

- No breaking changes.
- Keep URL paths stable.
- No permission behavior change.
- No business behavior change.
- No new feature work.
- No gateway/auth/JWT implementation.
- No config mutation.
- Expected Window 2 type: docs-only by default.

## Explicitly Out Of Scope

- No business code change from Window 0.
- No Window 2 implementation before Window 1 and human approval.
- No Java, Python, frontend, database, Redis, Kafka, `ai-config`, prompt-template, dependency, build, deployment or runtime behavior change by default.
- No gateway, auth-service, user-service, role-service, JWT, session, login flow, OAuth, SSO or production identity implementation.
- No route migration, route alias, endpoint rename, endpoint deletion, endpoint consolidation or gateway proxy.
- No permission behavior change, permission widening or permission narrowing.
- No config-store migration, DB/Nacos/hybrid adoption, config mutation or role-store migration.
- No service extraction.
- No DTO, VO, entity, mapper, schema, Kafka topic, Kafka payload, Redis key, frontend type or Python payload change.
- No new helper, adapter, bridge, fallback, wrapper, resolver, proxy, compatibility endpoint or temporary auth bridge.
- No reclassification of `ai-orchestration-service`, JSON config files, role-access headers, header-based demo auth, legacy `/api/tasks/*` paths, workbench aggregation or fallback provenance as final architecture.
- No new product feature or new agent work.

## Human Approval Result

User approved the proposed primary candidate.

Primary candidate:

- Phase 014 - Production Auth/Gateway Target Scoping and Identity/Role Authority Selection.

Fallback candidate:

- Config-store schema/versioning and audit/rollback readiness.

Approval constraints:

- No breaking changes.
- URL paths remain stable.
- No permission behavior change.
- No business behavior change.
- No new feature work.
- No gateway/auth/JWT implementation.
- No config mutation.
- Expected Window 2 type is docs-only by default after Window 1 planning is separately approved.

Window 0 stops here. Window 1 is allowed to start Phase 014 architecture planning, but no implementation is approved.
