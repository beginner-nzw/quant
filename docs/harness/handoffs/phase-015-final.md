# Phase 015 Final Handoff

## Phase Status

completed with residual risk.

Window: Window 4 - Handoff.

Phase: Phase 015 - Production Identity Issuer/Validator Selection Boundary.

Latest Window 3 review: `docs/harness/handoffs/phase-015-review.md`.

Review decision: approve.

## Handoff Files Consumed

- `docs/harness/handoffs/steering-decision-phase-015.md`
- `docs/harness/handoffs/phase-015-architect.md`
- `docs/harness/handoffs/phase-015-implementation.md`
- `docs/harness/handoffs/phase-015-review.md`
- `docs/harness/19-production-identity-issuer-validator-boundary.md`

No Phase 015 Fix Pass files exist.

## Completed Scope

Phase 015 completed docs-only production identity issuer/validator governance work.

Completed output:

- Produced `docs/harness/19-production-identity-issuer-validator-boundary.md` as the durable production identity issuer/validator boundary artifact.
- Selected backend-owned ingress/gateway JWT validation as the preferred future production identity validator placement.
- Deferred the concrete production identity issuer to later Window 0 decision and human approval.
- Deferred user profile source and production role authority.
- Preserved demo-header compatibility for `X-User-Id` and `X-User-Role` as local/demo inputs only.
- Preserved `UserContext` as runtime context, not production identity authority.
- Preserved `role-access-configs.json` as the current transition role/menu/permission input under Phase 012.
- Recorded future readiness gates for token/session semantics, service-principal validation, service-to-service identity handoff, audit identity, gateway compatibility, route migration compatibility and rollback.

No Java, Python, frontend, database, Redis, Kafka, `ai-config`, prompt-template, dependency, build-config, deployment or business runtime file changed.

## Contract / Authority / Transition State

Authority state:

- Current request headers remain demo/runtime inputs only.
- Backend-owned ingress/gateway JWT validation is selected only as future validator placement.
- Concrete issuer, user profile authority and role authority remain deferred.
- Frontend localStorage, route/menu/action gating, workbench data, fallback provenance, audit rows and ingest metadata remain non-authoritative.

Contract state:

- All URLs, HTTP methods, endpoint owners, request bindings, response envelopes and response types remain stable.
- Permission keys, menu keys, role codes, header names/defaults, explicit `requirePermission` calls and intentional no-explicit-permission read surfaces remain stable.
- Frontend routes, API functions, endpoint strings, call signatures, TypeScript shapes, localStorage behavior, request-header behavior, menu gating and action gating remain stable.
- No route alias, gateway route, auth URL, login URL, endpoint migration or compatibility endpoint was added.

Transition state:

- `ai-orchestration-service` remains a multi-domain transition host, not final architecture.
- Legacy `/api/tasks/*` non-task domain paths remain frozen transitional contracts under Phase 006.
- Header-based demo auth remains transition behavior only.
- JSON role access config remains a transition role/menu/permission input, not final role-store architecture.
- Phase 015 adds validator-placement guidance for later auth/gateway planning but does not implement gateway/JWT.

## Unchanged Contracts

- No breaking changes.
- URL paths and frontend routes remain stable.
- Permission behavior remains stable.
- Business behavior remains stable.
- No new product feature was added.
- No gateway/auth/JWT, auth-service, user-service, role-service, login/session, OAuth, SSO or external IdP implementation was added.
- No config mutation, config-store migration, role-store migration, service extraction, route migration, endpoint alias, frontend reshaping, Python behavior change, Kafka/database/Redis change or permanent modular-monolith decision was made.

## Remaining Debt

- D001 remains open: `ai-orchestration-service` still hosts multiple domains and remains a transition host.
- D002 remains open: non-task domain surfaces still use frozen legacy `/api/tasks/*` routes.
- D003 remains open for future workbench/fallback/preview/display/generated-event/config-display/permission-display/service-propagation/identity metadata surfaces.
- D007 remains open: JSON config and `role-access-configs.json` remain transition stores, not final config or role-store architecture.
- D008 remains open: header-based demo auth remains local/demo transition behavior only, not production security.

Residual Phase 015 risks:

- Concrete production identity issuer remains deferred.
- Production role authority remains deferred.
- Gateway/JWT implementation design, service-to-service propagation, audit identity field changes, route migration, service extraction, config-store migration and role-store migration require later Window 0 selection and human approval.

## Latest State For Window 0

Window 0 should automatically discover this state from `docs/harness/state/current-state.md` and this final handoff:

- Current phase: none approved.
- Last completed phase: Phase 015 - Production Identity Issuer/Validator Selection Boundary.
- Latest final handoff: `docs/harness/handoffs/phase-015-final.md`.
- Durable Phase 015 artifact: `docs/harness/19-production-identity-issuer-validator-boundary.md`.
- Open blockers: none registered.
- Human approval required before any Window 1 starts.

Window 0 must read the matching Phase 015 steering, architect, implementation, review and final handoffs, then score candidates using `docs/harness/10-steering-state-machine.md`. Window 0 must propose exactly one primary candidate and one fallback candidate and wait for human approval.

## Recommended Candidate Inputs For Window 0

Window 4 does not select the next phase. Recommended candidate inputs for Window 0 evaluation are:

- Production identity issuer selection, such as auth-service, user-service, external IdP/directory or another backend-owned issuer, only if Window 0 and the user explicitly choose to act on Phase 015 issuer deferral gates.
- Production role authority selection, such as DB role store, config-store-backed role source, auth/user-service ownership or external role claims, only if Window 0 and the user explicitly choose to act on Phase 014 role-authority gates, Phase 015 validator placement and Phase 012 config-store constraints.
- Gateway/JWT implementation design with demo-header compatibility policy only if Window 0 and the user explicitly choose to act on Phase 014 target-scope gates and Phase 015 selected validator placement.
- Service-to-service propagation and audit identity semantics for AI callbacks, event auto task dispatch and future extracted services only if Window 0 and the user explicitly choose to act on Phase 014 propagation gates and Phase 015 service-principal/audit identity requirements.
- Config-store migration target/scoping, DB/Nacos/hybrid readiness, config schema/versioning or config audit/rollback planning only if Window 0 and the user explicitly choose to act on Phase 012 and Phase 014/015 role/config-store dependency gates.
- Legacy route migration decision phase only after Window 0 accounts for Phase 006 contract freeze and Phase 014/015 auth/gateway compatibility gates.
- Risk-service extraction, strategy-service extraction, projection-split planning, risk/strategy route migration or risk/strategy Kafka downstream planning only if Window 0 and the user explicitly choose to act on Phase 011 readiness gates.
- Report extraction or report route-migration planning only if Window 0 and the user explicitly choose to act on Phase 009 readiness gates.
- Market-service extraction, data-ingest-service extraction, market route migration or market config-store planning only if Window 0 and the user explicitly choose to act on Phase 010 readiness gates.

## Files Changed In This Handoff

- `docs/harness/state/current-state.md`
- `docs/harness/05-transition-lifetime.md`
- `docs/harness/06-debt-register.md`
- `docs/harness/07-phase-backlog.md`
- `docs/harness/handoffs/phase-015-final.md`

## Verification

Window 4 reviewed Phase 015 handoffs and confirmed the latest Window 3 review decision is approve.

Window 4 must run `git status --short --untracked-files=all` before staging and commit only this handoff's harness files.
