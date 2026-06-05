# Steering Decision - Phase 019

## Status

Window: Window 0 - Steering.

Decision: propose candidate and wait for human approval.

Human approval status: pending.

This file replaces the earlier Phase 019 draft that incorrectly selected another docs-only role-boundary phase. Phase 018 already completed the remaining documentation governance closure. Phase 019 is now selected from the implementation-oriented or implementation-design candidates listed after Phase 018.

This file does not approve Window 1, implementation, code changes, route changes, permission behavior changes, gateway/auth/JWT work, service creation, role-store migration or config-store migration. Window 0 stops after this decision and waits for user approval.

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

- Phase 018 - Consolidated Remaining Governance Closure.

Highest final handoff discovered:

- `docs/harness/handoffs/phase-018-final.md`

Matching Phase 018 handoffs consumed:

- `docs/harness/handoffs/steering-decision-phase-018.md`
- `docs/harness/handoffs/phase-018-architect.md`
- `docs/harness/handoffs/phase-018-implementation.md`
- `docs/harness/handoffs/phase-018-review.md`
- `docs/harness/handoffs/phase-018-final.md`

Durable boundary artifact consumed:

- `docs/harness/22-remaining-governance-closure.md`

Missing matching handoff files:

- None for Phase 018.

## Startup Recovery Result

`docs/harness/state/current-state.md` and the handoff directory agree:

- Current active phase: none approved.
- Last completed phase: Phase 018 - Consolidated Remaining Governance Closure.
- Latest final handoff: `phase-018-final.md`.
- Open blockers: none registered.

Bootstrap Phase 001 is not current fact because Phase 001 through Phase 018 are completed and frozen by Window 4.

## Current State Summary

Phase 018 completed the remaining docs governance closure and produced `docs/harness/22-remaining-governance-closure.md`.

Current stable facts:

- `role-access-configs.json` remains the current transition role/menu/permission input, not final role-store architecture.
- `X-User-Id`, `X-User-Role`, `X-Trace-Id`, `guest`, `USER`, `UserContext`, frontend local role state and frontend route/menu/action gating remain runtime/demo/UI compatibility surfaces only.
- Backend explicit `requirePermission` checks and intentional no-explicit-permission read surfaces remain unchanged.
- Backend-owned ingress/gateway JWT validation remains future-only but is the selected preferred validator placement.
- External IdP or enterprise directory remains the preferred future identity issuer direction.
- Backend-owned application role authority remains the preferred future role direction.
- Phase 018 established demo-header compatibility policy shape, service-principal/audit-identity requirements, route migration gates and config-store/role-store migration gates.
- No gateway/auth/JWT, external IdP integration, directory integration, auth-service, user-service, role-service, login/session, OAuth, SSO, role-store migration, config-store migration, route migration, permission behavior change, service extraction or new feature work is currently approved.
- D001, D002, D003, D007 and D008 remain open.
- No main path breakage is registered.

## Latest Completed Or Blocked Phase

Latest completed phase:

- Phase 018 - Consolidated Remaining Governance Closure.

Latest blocked phase:

- None registered.

Handoff files read for the latest completed phase:

- `steering-decision-phase-018.md`
- `phase-018-architect.md`
- `phase-018-implementation.md`
- `phase-018-review.md`
- `phase-018-final.md`

## Decision Order Result

Decision order from `docs/harness/10-steering-state-machine.md`:

```text
main path breakage -> authority ambiguity -> contract ambiguity -> transition host reduction -> eval/test coverage -> new feature work
```

Evaluation:

- Main path breakage: none registered.
- Authority ambiguity: Phase 018 closed the remaining docs-level governance questions enough to move to an implementation-design candidate. Gateway/JWT design is the next authority entry point because it defines the future production validator boundary and demo-header compatibility before any runtime auth work.
- Contract ambiguity: present. Gateway/JWT design must preserve current URLs, headers, permission checks and frontend behavior while defining future compatibility contracts.
- Transition host reduction: still needed for D001, but service extraction should not precede production auth/identity compatibility design.
- Eval/test coverage: useful, but lower order than the current auth/contract design step.
- New feature work: not eligible while authority, contract and transition-host lifecycle debt remains open.

## Candidate Score Table

Scores are 0-2. Behavior risk is inverted: 2 means low risk.

| Candidate | Main path protection | Authority clarity | Contract clarity | Transition host reduction | Behavior risk | Verification feasibility | Total | Steering result |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| Phase 019 - Gateway/JWT Implementation Design With Demo-Header Compatibility Policy | 1 | 2 | 2 | 1 | 1 | 2 | 9 | Primary |
| Phase 019 Fallback - Concrete Production Role Authority Host And Mapping Implementation Plan | 1 | 2 | 2 | 1 | 1 | 2 | 9 | Fallback |
| User Profile Source Selection | 1 | 2 | 1 | 1 | 2 | 2 | 9 | Defer |
| Service-to-Service Propagation And Audit Identity Implementation Design | 1 | 1 | 2 | 1 | 1 | 2 | 8 | Defer |
| Config-store Or Role-store Migration Planning | 1 | 1 | 2 | 1 | 1 | 2 | 8 | Defer |
| Legacy Route Migration Planning | 0 | 1 | 2 | 2 | 0 | 1 | 6 | Defer |
| Report Extraction Or Report Route-Migration Planning | 1 | 1 | 1 | 2 | 0 | 1 | 6 | Defer |
| Market-service Or Data-ingest-service Extraction Planning | 1 | 1 | 1 | 2 | 0 | 1 | 6 | Defer |
| Risk-service, Strategy-service Or Projection-split Planning | 1 | 1 | 1 | 2 | 0 | 1 | 6 | Defer |
| Generic eval/static guard expansion | 1 | 1 | 1 | 0 | 2 | 2 | 7 | Defer by decision order |
| New product feature or new agent work | 0 | 0 | 0 | 0 | 0 | 0 | 0 | Not eligible |

Tie note:

- Gateway/JWT design and concrete role-authority implementation planning tie on raw score.
- Window 0 selects Gateway/JWT design as primary because Phase 018 explicitly moved the project past docs-only closure and identified gateway/JWT compatibility as the production identity validator entry point. It is the narrowest next step that can prepare implementation without changing current behavior.

## Primary Candidate

Phase 019 - Gateway/JWT Implementation Design With Demo-Header Compatibility Policy.

Bounded goal:

- Produce an implementation-design handoff for future backend-owned ingress/gateway JWT validation.
- Define how validated production identity will eventually enter backend request context without trusting frontend headers as production authority.
- Define demo-header compatibility policy for `X-User-Id`, `X-User-Role`, `X-Trace-Id`, `guest`, `USER` and `quant_current_user`.
- Define token claim, trusted forwarded context, failure behavior, rollback, local/demo profile behavior, service-to-service identity handoff and audit identity design requirements.
- Preserve all current runtime contracts and behavior unless a later Window 1 explicitly scopes a narrow implementation and the user separately approves it.

Expected durable artifact:

- `docs/harness/23-gateway-jwt-implementation-design.md`

Why this is the next bounded step:

- Phase 018 completed the remaining governance closure, so another pure boundary-document phase would repeat solved work.
- Gateway/JWT design is the next implementation-oriented entry point for D008 because header-based demo auth remains local/demo only and production security is still absent.
- It is still bounded: Window 1 must separate implementation design from runtime implementation and must keep current contracts stable by default.
- It comes before route migration and domain extraction because those depend on a stable auth/identity compatibility boundary.

Expected phase shape:

- Implementation-design first.
- Docs-only by default unless Window 1 explicitly asks for a narrow non-runtime validation or guard scope and the user separately approves it.
- No gateway/auth/JWT runtime implementation by default.
- No external IdP or directory integration.
- No auth-service, user-service, role-service, login/session, OAuth or SSO implementation.
- No role-store or config-store migration.
- No `role-access-configs.json` mutation.
- No route migration, route alias, endpoint rename, endpoint deletion, endpoint consolidation or gateway proxy.
- No permission behavior change.
- No service extraction or new product feature.

## Fallback Candidate

Phase 019 Fallback - Concrete Production Role Authority Host And Mapping Implementation Plan.

Fallback condition:

- Use this if the user rejects Gateway/JWT implementation design as too early before role-authority implementation planning.

Bounded fallback goal:

- Produce an implementation-plan artifact for concrete production role authority host and mapping.
- Define role assignment authority, role-permission mapping authority, menu mapping authority and external group/claim input mapping.
- Preserve `role-access-configs.json` as current transition input until a later approved migration.
- Preserve all current runtime contracts and behavior.

Why it is not primary:

- The user explicitly requested that Phase 019 primary be Gateway/JWT implementation design with demo-header compatibility policy.
- Phase 018 already captured role-host and mapping gates enough for Window 1 to account for them as dependencies in Gateway/JWT design.

## Why Other Phases Are Not Selected

- Phase 001 through Phase 018 are completed and frozen.
- User profile source selection is deferred because Gateway/JWT design can record profile-source dependencies without selecting or implementing the profile source in this phase.
- Service-to-service propagation and audit identity implementation design is deferred because it should use the gateway/JWT identity boundary selected by the primary phase.
- Config-store or role-store migration planning is deferred because store migration should follow auth/role compatibility design and explicit target selection.
- Legacy route migration is deferred because Phase 006 froze current URLs and Phase 018 requires explicit compatibility or breaking-change gates; route movement should not precede auth compatibility.
- Report, market, data-ingest, risk, strategy or projection extraction planning is deferred because those moves depend on auth, route, role, profile and service-propagation readiness.
- Generic eval/static guard expansion is deferred by decision order. It should attach to a bounded implementation-design or contract phase rather than replace it.
- New product features and new agent work remain ineligible while authority, contract and transition-host lifecycle debt remains open.

## Window 1 Must Define

Window 1 must turn the primary candidate into an implementation-ready handoff and define:

- Exact Phase 019 scope and whether `docs/harness/23-gateway-jwt-implementation-design.md` is the required durable artifact.
- Whether Phase 019 is docs-only implementation design, or whether any narrow non-runtime validation/guard work is requested for separate human approval.
- Current inherited facts from Phase 012 through Phase 018:
  - `role-access-configs.json` is the current transition role/menu/permission input.
  - backend-owned ingress/gateway JWT validation is the preferred future validator placement.
  - external IdP or enterprise directory is the preferred future issuer direction.
  - backend-owned application role authority is the preferred future role direction.
  - current headers, defaults, `UserContext`, backend permission checks, no-explicit-permission read surfaces and frontend gating remain stable.
- Belongs rules for identity issuer, identity validator, trusted backend request context, runtime `UserContext`, service principals, delegated actor, original actor, audit identity and frontend demo header source.
- Authority rules preventing request headers, frontend localStorage, route/menu/action gating, workbench output, fallback metadata or audit rows from becoming production identity or role authority.
- Contract boundaries for headers/defaults, current endpoints, frontend request utilities, local/demo behavior, permission checks, no-explicit-permission reads, route stability and rollback compatibility.
- Demo-header compatibility modes to compare:
  - retain demo headers only in local/demo profiles
  - translate validated JWT context into current backend context
  - retire demo headers with explicit breaking/compatibility approval
  - hybrid migration with guards and rollback
- JWT design dependencies:
  - issuer selection and metadata
  - token claim mapping
  - role/source mapping dependency
  - user profile dependency
  - service principal semantics
  - failure behavior and unauthenticated behavior
  - audit identity fields or future audit requirements
  - route/gateway compatibility
  - rollback and local/demo compatibility
- Verification commands appropriate for docs-only design, including read-only inventories of current headers, security utilities, permission checks, frontend request headers, role config and existing Phase 006/007 guards if relevant.
- Stop rules if Window 2 discovers that the phase requires runtime gateway/auth/JWT implementation, external IdP integration, service creation, config mutation, role-store migration, route changes, permission behavior changes, frontend changes, Java/Python changes, database/Kafka/Redis changes or business behavior changes.

Default constraints Window 1 must carry forward:

- No breaking changes.
- Keep URL paths stable.
- Keep frontend routes stable.
- No permission behavior change.
- No business behavior change.
- No new feature work.
- No gateway/auth/JWT runtime implementation by default.
- No auth-service, user-service, role-service, login/session, OAuth, SSO or external IdP implementation.
- No config mutation.
- No role-store or config-store migration.
- Expected Window 2 type: docs-only implementation design by default unless Window 1 asks for and receives separate human approval.

## Explicitly Out Of Scope

- No business code change from Window 0.
- No Window 2 work before Window 1 and human approval.
- No Java, Python, frontend, database, Redis, Kafka, `ai-config`, prompt-template, dependency, build, deployment or runtime behavior change by default.
- No gateway, auth-service, user-service, role-service, JWT, session, login flow, OAuth, SSO, external IdP integration or production identity implementation by default.
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

- Primary: Phase 019 - Gateway/JWT Implementation Design With Demo-Header Compatibility Policy.
- Fallback: Phase 019 Fallback - Concrete Production Role Authority Host And Mapping Implementation Plan.

Default approval meaning:

- No breaking changes.
- URL paths remain stable.
- Frontend routes remain stable.
- No permission behavior change.
- No business behavior change.
- No new feature work.
- No gateway/auth/JWT runtime implementation by default.
- No auth-service, user-service, role-service, login/session, OAuth, SSO or external IdP implementation.
- No config mutation.
- No role-store or config-store migration.
- Expected Window 2 type is docs-only implementation design by default after Window 1 planning is separately approved.

Window 0 stops here. Window 1 must not start until the user approves.

## Human Approval Result

User authorization was delegated from source thread `019e9558-7bb9-7443-bbed-e4d97e5315e0`: within the default safety constraints for the Window 0-4 flow, the coordinating window may approve this Window 0 decision on the user's behalf.

Approval status: approved.

Approved primary candidate:

- Phase 019 - Gateway/JWT Implementation Design With Demo-Header Compatibility Policy.

Fallback candidate retained:

- Phase 019 Fallback - Concrete Production Role Authority Host And Mapping Implementation Plan.

Default approval constraints:

- No breaking change.
- URL paths remain stable.
- Frontend routes remain stable.
- No permission behavior change.
- No business behavior change.
- No new feature work.
- No runtime gateway/auth/JWT implementation.
- No auth-service, user-service or role-service creation.
- No login/session/OAuth/SSO implementation.
- No external IdP integration.
- No config mutation.
- No role-store or config-store migration.
- Window 1 may start Phase 019 architecture planning.

Window 0 stops here. This approval does not start Window 1 in this thread and does not approve implementation.
