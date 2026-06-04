# Phase 017 Final Handoff

## Status

Window: Window 4 - Handoff.

Phase: Phase 017 - Production Role Authority Selection Boundary.

Phase status: completed with residual risk.

Latest review: `docs/harness/handoffs/phase-017-review.md`.

Review decision: approve.

## Completed Scope

Phase 017 produced `docs/harness/21-production-role-authority-boundary.md` as the durable production role authority boundary artifact.

Completed scope:

- Selected backend-owned application role authority as the preferred future direction for production role assignment, role-permission mapping and menu mapping.
- Kept external IdP or enterprise directory groups/claims as future identity-adjacent inputs only until a later approved gateway/JWT validation, claim/group mapping, compatibility and audit phase defines the contract.
- Preserved `role-access-configs.json` as the current transition role/menu/permission input under Phase 012.
- Preserved `X-User-Id`, `X-User-Role`, `X-Trace-Id`, missing defaults `guest` and `USER`, `UserContext`, backend `requirePermission` checks, intentional no-explicit-permission read surfaces, frontend local role state, frontend route/menu/action gating and `role-access-configs.json` shape.
- Deferred concrete role authority host, DB role store, auth-service/user-service/role-service ownership, config-store-backed mapping target, external group/claim mapping, gateway/JWT implementation, user profile source, service-to-service role handoff, audit identity changes, route migration, config-store migration and role-store migration to future Window 0 decisions and human approval.

## Unchanged Contracts

Unchanged:

- All URL paths, HTTP methods, endpoint owners, request bindings, response envelopes and response types.
- All frontend routes, frontend API function names, endpoint strings, call signatures, TypeScript shapes and localStorage behavior.
- All role codes, permission keys, menu keys, coarse access-role mappings, backend `requirePermission` calls and intentional no-explicit-permission read surfaces.
- `X-User-Id`, `X-User-Role`, `X-Trace-Id`, missing backend defaults `guest` and `USER`, and frontend selected-user key `quant_current_user`.
- DTO, VO, entity, mapper, schema, Redis key, Kafka topic, Kafka payload, JSON config shape, prompt-template shape, Python payload and runtime setting contracts.

Phase 017 was docs-only and did not implement or approve gateway/auth/JWT, external IdP integration, directory integration, auth-service, user-service, role-service, login/session, OAuth, SSO, route migration, endpoint aliases, permission behavior changes, frontend changes, Python changes, config mutation, config-store migration, role-store migration, database/Redis/Kafka changes, business behavior changes or new feature work.

## Remaining Debt

Remaining debt:

- D001 remains open because `ai-orchestration-service` still hosts multiple domains and remains a transition host.
- D002 remains open because non-task domain surfaces still use frozen legacy `/api/tasks/*` routes.
- D003 remains open for future workbench/fallback/preview/display/generated-event/config-display/permission-display/service-propagation/identity/issuer/profile/role metadata surfaces.
- D007 remains open because JSON config files, including `role-access-configs.json`, remain transition stores and no config-store or role-store migration is approved.
- D008 remains open because header-based demo auth remains local/demo transition behavior only, not production security.

## Latest State For Window 0

Window 0 should automatically discover this state from `docs/harness/state/current-state.md` and this final handoff:

- Latest completed phase: Phase 017 - Production Role Authority Selection Boundary.
- Latest final handoff: `docs/harness/handoffs/phase-017-final.md`.
- Durable Phase 017 artifact: `docs/harness/21-production-role-authority-boundary.md`.
- Current active phase: none approved.
- Open blockers: none registered.
- Phase 017 selected a future-only backend-owned application role authority direction.
- External IdP or enterprise directory remains the preferred future identity issuer direction from Phase 016.
- Backend-owned ingress/gateway JWT validation remains the preferred future validator placement from Phase 015.
- `role-access-configs.json` remains the current transition role/menu/permission input, not final role-store architecture.
- Demo headers and frontend role state remain local/demo inputs only.
- No implementation, integration, migration, route change, permission behavior change or service extraction is approved.

Window 0 must not ask the user to summarize Phase 017 manually. It must read this final handoff, the matching Phase 017 steering/architect/implementation/review handoffs and `docs/harness/21-production-role-authority-boundary.md`, then score next candidates using `docs/harness/10-steering-state-machine.md`.

## Recommended Candidate Inputs For Window 0

Recommended candidate inputs for Window 0 evaluation:

- Concrete production role authority host and mapping boundary, such as DB role store, auth-service/user-service/role-service ownership, config-store-backed mapping or external group/claim synchronization, only if Window 0 and the user explicitly choose to act on Phase 017 backend-owned application role authority direction and Phase 012/014/015/016 constraints.
- Gateway/JWT implementation design with demo-header compatibility policy only if Window 0 and the user explicitly choose to act on Phase 014 target-scope gates, Phase 015 selected validator placement, Phase 016 external issuer direction and Phase 017 backend-owned role authority direction.
- User profile source selection, such as external directory claims, user-service, auth-service profile store, synchronized profile read model or another backend-owned source, only if Window 0 and the user explicitly choose to act on Phase 016 profile-source deferral gates and Phase 017 role/profile separation rules.
- Service-to-service propagation and audit identity semantics for AI callbacks, event auto task dispatch and future extracted services only if Window 0 and the user explicitly choose to act on Phase 014 propagation gates and Phase 015/016 service-principal/audit identity requirements plus Phase 017 role handoff requirements.
- Config-store or role-store migration target/scoping, DB/Nacos/hybrid readiness, config schema/versioning or config audit/rollback planning only if Window 0 and the user explicitly choose to act on Phase 012 and Phase 017 role/config-store dependency gates.
- Legacy route migration decision phase only after Window 0 accounts for Phase 006 contract freeze and Phase 014/015/016/017 auth/gateway/issuer/role compatibility gates.
- Risk-service extraction, strategy-service extraction, projection-split planning, risk/strategy route migration or risk/strategy Kafka downstream planning only if Window 0 and the user explicitly choose to act on Phase 011 readiness gates.
- Report extraction or report route-migration planning only if Window 0 and the user explicitly choose to act on Phase 009 readiness gates.
- Market-service extraction, data-ingest-service extraction, market route migration or market config-store planning only if Window 0 and the user explicitly choose to act on Phase 010 readiness gates.

Window 4 does not select the next phase. Window 0 must propose exactly one primary candidate and one fallback candidate and wait for human approval.

## Files Changed In This Handoff

Window 4 changed:

- `docs/harness/state/current-state.md`
- `docs/harness/05-transition-lifetime.md`
- `docs/harness/06-debt-register.md`
- `docs/harness/07-phase-backlog.md`
- `docs/harness/handoffs/phase-017-final.md`

No business code was changed.
