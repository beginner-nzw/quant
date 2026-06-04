# Phase 014 Final Handoff

## Status

Window: Window 4 - Handoff.

Phase: Phase 014 - Production Auth/Gateway Target Scoping and Identity/Role Authority Selection.

Phase status: completed with residual risk.

Latest Window 3 decision: approve in `docs/harness/handoffs/phase-014-review.md`.

No Phase 014 fix pass was required.

## Inputs Read

Required harness files:

- `docs/harness/05-transition-lifetime.md`
- `docs/harness/06-debt-register.md`
- `docs/harness/07-phase-backlog.md`
- `docs/harness/09-window-protocol.md`
- `docs/harness/10-steering-state-machine.md`
- `docs/harness/state/current-state.md`

Phase 014 handoffs:

- `docs/harness/handoffs/steering-decision-phase-014.md`
- `docs/harness/handoffs/phase-014-architect.md`
- `docs/harness/handoffs/phase-014-implementation.md`
- `docs/harness/handoffs/phase-014-review.md`

Durable artifact:

- `docs/harness/18-production-auth-gateway-target-scope.md`

## Completed Scope

Phase 014 completed docs-only production auth/gateway target scoping.

Completed durable artifact:

- `docs/harness/18-production-auth-gateway-target-scope.md`

The phase froze these governance conclusions:

- Production identity should be accepted through a future backend-owned ingress/auth boundary.
- Gateway/JWT is the preferred future identity target shape, with the concrete issuer or validator deferred to a later Window 0 decision and human approval.
- Production role authority must be backend-owned in a later phase.
- `role-access-configs.json` remains the current transition role/menu/permission config input under the Phase 012 JSON transition-store policy.
- `X-User-Id` and `X-User-Role` remain local/demo compatibility inputs only, not production identity or production role authority.
- Service-to-service propagation, audit identity semantics, demo-header compatibility or retirement, route migration and role/config-store dependencies are future requirements only.

## Unchanged Contracts

Phase 014 changed no runtime behavior.

Unchanged:

- URL paths, HTTP methods, controller owners, request bindings, response envelopes and response types.
- Permission keys, menu keys, role codes, header names and default values.
- Existing backend `requirePermission` checks.
- Intentional no-explicit-permission read surfaces.
- Frontend routes, API function names, endpoint strings, call signatures, TypeScript shapes, localStorage behavior, request-header behavior, menu gating and action gating.
- DTOs, VOs, entities, mappers, database schema, Redis keys, Kafka topics and payloads, JSON config shapes, prompt-template shapes, Python payloads and runtime settings.

Not approved:

- Gateway/auth/JWT implementation.
- Auth-service, user-service, role-service, login/session, OAuth, SSO, role DB or external IdP integration.
- Route migration, endpoint aliases, endpoint rename/deletion/consolidation or gateway proxy.
- Permission behavior changes, config mutation, config-store migration, role-store migration, service extraction, frontend/Python reshaping, Kafka/database/Redis changes, permanent modular-monolith status, business behavior changes or new feature work.

## Remaining Debt

Still open:

- D001: `ai-orchestration-service` remains a multi-domain transition host.
- D002: non-task domains still use frozen legacy `/api/tasks/*` routes.
- D003: future workbench/fallback/preview/display/generated-event/config-display/permission-display/service-propagation metadata must remain non-authoritative.
- D007: JSON config and prompt files remain transition stores; `role-access-configs.json` is not final role-store architecture.
- D008: header-based demo auth remains local/demo transition behavior only, not production security.

Phase 014 reduces ambiguity for later auth/gateway and role-authority work, but it does not close these debts.

## Latest State For Window 0

Window 0 should automatically discover this state from `docs/harness/state/current-state.md`:

- Current phase: none approved.
- Latest frozen phase: Phase 014 - Production Auth/Gateway Target Scoping and Identity/Role Authority Selection.
- Last completed phase: Phase 014.
- Latest final handoff: `docs/harness/handoffs/phase-014-final.md`.
- Open blockers: none registered.
- No active candidate is approved.

Startup recovery for the next Window 0:

- Read `docs/harness/state/current-state.md`.
- Discover `docs/harness/handoffs/phase-014-final.md`.
- Read the Phase 014 steering, architect, implementation, review and final handoffs.
- Consume `docs/harness/18-production-auth-gateway-target-scope.md` together with the durable Phase 008/009/010/011/012/013 artifacts.
- Score candidate next phases using `docs/harness/10-steering-state-machine.md`.
- Propose exactly one primary candidate and one fallback candidate.
- Wait for human approval before Window 1 starts.

## Recommended Candidate Inputs For Window 0

Window 4 does not select the next phase. Candidate inputs for Window 0 scoring:

- Gateway/JWT implementation design with demo-header compatibility policy, if acting on Phase 014 target-scope gates.
- Production identity issuer/validator selection, such as auth-service, user-service or external IdP integration, if acting on Phase 014 identity-authority gates.
- Production role authority selection, such as DB role store, config-store-backed role source, auth/user-service ownership or external role claims, if acting on Phase 014 role-authority gates and Phase 012 config-store constraints.
- Service-to-service propagation and audit identity semantics for AI callbacks, event auto task dispatch and future extracted services, if acting on Phase 014 propagation gates.
- Config-store migration target/scoping, audit/rollback planning, DB/Nacos/hybrid readiness or schema/versioning, if acting on Phase 012 and Phase 014 role/config-store dependency gates.
- Legacy route migration only after Window 0 accounts for Phase 006 contract freeze and Phase 014 auth/gateway compatibility gates.
- Report, market/data-ingest or risk/strategy extraction/route/projection/Kafka planning only if Window 0 accounts for the relevant Phase 009/010/011 readiness gates and the Phase 014 auth/role prerequisites.

## Files Changed In This Handoff

- `docs/harness/state/current-state.md`
- `docs/harness/06-debt-register.md`
- `docs/harness/07-phase-backlog.md`
- `docs/harness/05-transition-lifetime.md`
- `docs/harness/handoffs/phase-014-final.md`

No business code was changed by Window 4.
