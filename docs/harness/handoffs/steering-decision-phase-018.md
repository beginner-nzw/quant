# Steering Decision - Phase 018

## Status

Window: Window 0 - Steering.

Decision: Phase 018 approved by user for Window 1 architecture planning.

Human approval status: approved.

This file does not approve implementation. Window 1 is now allowed to start architecture planning and must produce a Phase 018 architect handoff before any implementation window can start.

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

- Phase 017 - Production Role Authority Selection Boundary.

Highest final handoff discovered:

- `docs/harness/handoffs/phase-017-final.md`

Matching Phase 017 handoffs consumed:

- `docs/harness/handoffs/steering-decision-phase-017.md`
- `docs/harness/handoffs/phase-017-architect.md`
- `docs/harness/handoffs/phase-017-implementation.md`
- `docs/harness/handoffs/phase-017-review.md`
- `docs/harness/handoffs/phase-017-final.md`

Durable boundary artifacts consumed:

- `docs/harness/17-auth-gateway-permission-boundary.md`
- `docs/harness/18-production-auth-gateway-target-scope.md`
- `docs/harness/19-production-identity-issuer-validator-boundary.md`
- `docs/harness/20-production-identity-issuer-boundary.md`
- `docs/harness/21-production-role-authority-boundary.md`

Missing matching handoff files:

- None for Phase 017.

## Startup Recovery Result

`docs/harness/state/current-state.md` and the handoff directory agree:

- Current active phase: none approved.
- Last completed phase: Phase 017 - Production Role Authority Selection Boundary.
- Latest final handoff: `phase-017-final.md`.
- Open blockers: none registered.

Bootstrap Phase 001 is not current fact because Phase 001 through Phase 017 are completed and frozen by Window 4.

## Current State Summary

- Phase 017 completed docs-only production role authority governance work.
- `docs/harness/21-production-role-authority-boundary.md` is now the durable production role authority boundary artifact.
- Backend-owned application role authority is selected as the preferred future direction for production role assignment, role-permission mapping and menu mapping.
- External IdP or enterprise directory groups/claims remain future inputs only until a later approved gateway/JWT validation, claim/group mapping, compatibility and audit phase defines the contract.
- External IdP or enterprise directory remains the preferred future identity issuer direction from Phase 016.
- Backend-owned ingress/gateway JWT validation remains the preferred future validator placement from Phase 015.
- `role-access-configs.json` remains the current transition role/menu/permission input under Phase 012, not final role-store architecture.
- `X-User-Id`, `X-User-Role`, `X-Trace-Id`, `guest`, `USER`, `UserContext`, frontend local role state, frontend route/menu/action gating, backend `requirePermission` calls and intentional no-explicit-permission read surfaces remain unchanged.
- No gateway/auth/JWT, external IdP integration, directory integration, auth-service, user-service, role-service, login/session, OAuth, SSO, role-store migration, config-store migration, route migration, permission behavior change or service extraction is approved.
- D001, D002, D003, D007 and D008 remain open.
- No main path breakage is registered.

## Latest Completed Or Blocked Phase

Latest completed phase:

- Phase 017 - Production Role Authority Selection Boundary.

Latest blocked phase:

- None registered.

Handoff files read for the latest completed phase:

- `steering-decision-phase-017.md`
- `phase-017-architect.md`
- `phase-017-implementation.md`
- `phase-017-review.md`
- `phase-017-final.md`

## Decision Order Result

Decision order from `docs/harness/10-steering-state-machine.md`:

```text
main path breakage -> authority ambiguity -> contract ambiguity -> transition host reduction -> eval/test coverage -> new feature work
```

Evaluation:

- Main path breakage: none registered.
- Authority ambiguity: still present. Phase 017 selected future backend-owned application role authority, but deliberately deferred the concrete role authority host, role-store/config-store target, external group/claim mapping contract and user profile source.
- Contract ambiguity: still present. Gateway/JWT, route migration, service extraction, role-store/config-store migration and permission behavior decisions depend on a concrete role authority host and mapping boundary.
- Transition host reduction: still needed for D001, but extraction or route migration remains blocked by identity, role, profile, propagation, config and route gates.
- Eval/test coverage: useful, but lower order than the remaining authority decisions.
- New feature work: not eligible while authority, contract and transition-host lifecycle debt remains open.

## Candidate Score Table

Scores are 0-2. Behavior risk is inverted: 2 means low risk.

| Candidate | Main path protection | Authority clarity | Contract clarity | Transition host reduction | Behavior risk | Verification feasibility | Total | Steering result |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| Phase 018 - Consolidated Remaining Governance Closure | 1 | 2 | 2 | 1 | 1 | 2 | 9 | Primary |
| Phase 018 Narrow Fallback - Concrete Production Role Authority Host And Mapping Boundary | 1 | 2 | 2 | 1 | 2 | 2 | 10 | Fallback |
| User Profile Source Selection Boundary | 1 | 2 | 1 | 1 | 2 | 2 | 9 | Included in primary / defer if narrow fallback chosen |
| Service-to-Service Propagation And Audit Identity Semantics Boundary | 1 | 2 | 1 | 1 | 2 | 2 | 9 | Included in primary / defer if narrow fallback chosen |
| Gateway/JWT Implementation Design With Demo-Header Compatibility Policy | 1 | 1 | 2 | 1 | 1 | 2 | 8 | Defer |
| Config-store Or Role-store Migration Target/Scoping | 1 | 1 | 2 | 1 | 1 | 2 | 8 | Defer |
| Legacy Route Migration Decision Phase | 0 | 1 | 2 | 2 | 0 | 1 | 6 | Defer |
| Report Extraction Or Report Route-Migration Planning | 1 | 1 | 1 | 2 | 0 | 1 | 6 | Defer |
| Market-service Or Data-ingest-service Extraction Planning | 1 | 1 | 1 | 2 | 0 | 1 | 6 | Defer |
| Risk-service, Strategy-service Or Projection-split Planning | 1 | 1 | 1 | 2 | 0 | 1 | 6 | Defer |
| Generic eval/static guard expansion | 1 | 1 | 1 | 0 | 2 | 2 | 7 | Defer by decision order |
| New product feature or new agent work | 0 | 0 | 0 | 0 | 0 | 0 | 0 | Not eligible |

Consolidation note:

- The narrow role-host candidate has the highest raw score because it is more focused and lower risk.
- Window 0 nevertheless proposes the consolidated candidate as primary because the user explicitly asked whether remaining docs governance can be completed in one phase.
- The consolidated candidate is acceptable only if Window 1 keeps it docs-only, bounded to governance closure, and forbids implementation, migration, code changes, permission changes, route changes and service creation.

## Primary Candidate

Phase 018 - Consolidated Remaining Governance Closure.

Bounded goal:

- Produce one docs-only consolidated governance closure artifact that collects the remaining pre-implementation decisions needed before implementation-oriented phases can be proposed.
- Close the remaining governance decision gaps at the documentation level only:
  - concrete production role authority host family and mapping boundary
  - user profile source boundary
  - service-to-service propagation and audit identity semantics
  - gateway/JWT implementation-design prerequisites and demo-header compatibility policy shape
  - config-store and role-store migration readiness gates
  - route migration readiness and breaking-change gate prerequisites
  - sequencing map for later implementation phases
- Preserve all current contracts and runtime behavior.
- Explicitly state which later implementation phases are now eligible to be scored by a future Window 0, without approving any implementation.

Expected durable artifact:

- `docs/harness/22-remaining-governance-closure.md`

Why this is the next bounded step:

- Phase 012 through Phase 017 have narrowed the governance field enough that the remaining docs-only decisions are tightly related rather than independent product work.
- The user explicitly requested that remaining document governance not be stretched across many more phases.
- A single closure phase reduces process overhead while still respecting the Window protocol and human approval gates.
- It is still not a code phase: it only prepares the project so the next Window 0 can score concrete implementation candidates without reopening the same authority questions one by one.

Expected phase shape:

- Docs-only architecture/governance work by default.
- No Java, Python, frontend, database, Redis, Kafka, config, dependency, build, deployment or runtime behavior change.
- No gateway/auth/JWT implementation.
- No external IdP or directory integration.
- No auth-service, user-service, role-service, login/session, OAuth or SSO implementation.
- No role-store or config-store migration.
- No `role-access-configs.json` mutation.
- No route migration, route alias, endpoint rename, endpoint deletion, endpoint consolidation or gateway proxy.
- No permission behavior change.
- No service extraction or new product feature.
- Window 1 must aggressively split "decision closure" from "implementation approval" so this phase cannot smuggle in gateway/auth/JWT, migration, route or permission behavior changes.

## Fallback Candidate

Phase 018 Narrow Fallback - Concrete Production Role Authority Host And Mapping Boundary.

Fallback condition:

- Use this if the user rejects a consolidated governance closure as too broad or too risky.

Bounded fallback goal:

- Produce a docs-only concrete role authority host and mapping boundary artifact.
- Act on the Phase 017 future-only direction that production application role authority should be backend-owned.
- Select or narrowly defer the concrete host family and mapping boundary.
- Preserve all current role codes, permission keys, menu keys, backend `requirePermission` calls, no-explicit-permission read surfaces, frontend gating, demo headers and URL/API contracts.

Why it is not primary:

- The user explicitly asked whether remaining docs governance can be done in one phase.
- The narrow fallback is safer and more focused, but it would continue the many-small-doc-phases pattern the user is pushing back on.

## Why Other Phases Are Not Selected

- Phase 001 through Phase 017 are completed and frozen.
- Gateway/JWT implementation is still not selected because consolidated governance closure may document prerequisites and design gates only; it must not implement gateway/JWT.
- Service-to-service propagation and audit identity implementation is still not selected because the primary phase may document semantics only; it must not change Kafka, callbacks, audit rows or runtime identity behavior.
- Config-store or role-store migration implementation is still not selected because Phase 012 keeps JSON/prompt files as current transition stores and migration still requires later approval.
- Legacy route migration implementation is still not selected because Phase 006 intentionally froze current URLs. Migration needs explicit breaking-change or compatibility approval.
- Report extraction or route migration is deferred because Phase 009 produced readiness documentation only. Report movement still depends on route, auth, identity, role, projection and contract decisions.
- Market-service or data-ingest-service extraction is deferred because Phase 010 readiness blockers remain, including auth, route, config, source sync and ingest ownership.
- Risk-service, strategy-service or projection-split planning is deferred because Phase 011 readiness blockers remain, including shared projection, generated events, auth, route, Redis and contract decisions.
- Generic eval/static guard expansion is deferred by decision order. It should attach to a bounded authority or contract phase rather than replace concrete role-host work.
- New product features and new agent work remain ineligible while authority, contract and transition-host lifecycle debt remains open.

## Window 1 Must Define

Window 1 must turn the primary candidate into an implementation-ready handoff and define:

- Exact Phase 018 scope and whether `docs/harness/22-remaining-governance-closure.md` is the required durable artifact.
- Current facts inherited from Phase 012 through Phase 017:
  - `role-access-configs.json` is the current transition role/menu/permission config input.
  - backend-owned application role authority is the preferred future direction from Phase 017.
  - concrete role authority host, role-store migration, config-store migration and external group/claim mapping remain deferred.
  - `X-User-Id` and `X-User-Role` are demo/runtime inputs only.
  - `UserContext` is runtime context, not production identity, profile or role authority.
  - backend explicit `requirePermission` calls are current checked-endpoint enforcement points.
  - intentional no-explicit-permission read surfaces remain stable.
  - frontend route/menu/action gating is UI affordance only.
  - backend-owned ingress/gateway JWT validation is the preferred future validator placement.
  - external IdP or enterprise directory is the preferred future issuer direction, future-only.
- In-scope governance-closure sections:
  - concrete role authority host and mapping boundary
  - user profile source boundary
  - service-to-service propagation and audit identity semantics
  - gateway/JWT implementation-design prerequisites and demo-header compatibility policy
  - config-store and role-store migration readiness gates
  - route migration readiness and breaking-change prerequisites
  - sequencing map for later implementation phases
- Candidate concrete role host/mapping options:
  - DB role store
  - auth-service/user-service/role-service ownership
  - config-store-backed role-permission/menu mapping
  - external group/claim synchronization or advisory mapping
  - bounded continuation of `role-access-configs.json`
  - deliberate continued concrete-host deferral with explicit readiness criteria
- Belongs rules for role assignment authority, role-permission mapping authority, menu mapping authority, external group/claim mapping, backend enforcement, frontend gating, role audit, role-store migration and config-store interaction.
- Authority rules distinguishing identity issuer, identity validator, runtime user context, user profile source, concrete role authority host, role assignment authority, role-permission mapping, menu mapping and frontend UI affordance.
- Contract boundaries for current role codes, permission keys, menu keys, backend `requirePermission` calls, no-explicit-permission read surfaces, current headers/defaults, frontend local role behavior, route/menu/action metadata and `role-access-configs.json` shape.
- Dependencies on Phase 006 route freeze, Phase 012 config-store boundary, Phase 013 permission inventory, Phase 014 auth/gateway target scope, Phase 015 validator placement, Phase 016 issuer direction and Phase 017 role authority direction.
- Whether Phase 018 only closes docs governance, or also recommends a small ordered list of later implementation candidates for future Window 0 scoring.
- Verification commands appropriate for docs-only work, including read-only inventories of current role config, permission checks, frontend role utilities and existing Phase 006/007 guards if relevant.
- Stop rules if Window 2 discovers that the consolidated phase cannot be kept docs-only, or requires gateway/auth/JWT implementation, external IdP integration, auth-service/user-service/role-service code, role-store migration, config mutation, permission behavior changes, route changes, frontend behavior changes, Java/Python changes, database changes or business behavior changes.

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
- No concrete service creation.
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

- Primary: Phase 018 - Consolidated Remaining Governance Closure.
- Fallback: Phase 018 Narrow Fallback - Concrete Production Role Authority Host And Mapping Boundary.

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

- Phase 018 - Consolidated Remaining Governance Closure.

Fallback candidate:

- Phase 018 Narrow Fallback - Concrete Production Role Authority Host And Mapping Boundary.

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

Window 0 stops here. Window 1 is allowed to start Phase 018 architecture planning, but no implementation is approved.
