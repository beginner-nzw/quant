# Phase 013 Final Handoff

## Status

Window: Window 4 - Handoff.

Phase: Phase 013 - Auth/Gateway Permission Authority Boundary.

Phase status: completed with residual risk.

Latest review: `docs/harness/handoffs/phase-013-review.md` approved the initial implementation. No Fix Pass was required.

## Completed Scope

- Produced `docs/harness/17-auth-gateway-permission-boundary.md` as the durable auth/gateway permission boundary artifact.
- Froze current permission belongs, authority, contract and transition facts for request headers, backend request context, `role-access-configs.json`, backend permission services, explicit backend permission checks, intentional no-explicit-permission read surfaces, `research-task-service` task-create permission behavior and frontend role/header/menu/action consumers.
- Recorded that `role-access-configs.json` remains the current role/menu/permission config input under the Phase 012 JSON transition-store policy.
- Recorded that `X-User-Id` and `X-User-Role` remain demo/runtime request inputs, not production identity or production role authority.
- Recorded that header-based demo auth remains current transition behavior for the next governance horizon.
- Recorded production gateway/auth/JWT, auth-service, user-service, role-store, login/session and demo-header retirement as deferred future decisions requiring later Window 0 selection and explicit human approval.

## Contract / Authority / Transition State Changes

- Authority state now includes Phase 013: backend explicit `requirePermission` calls remain current enforcement points for checked endpoints; frontend route/menu/action gating remains UI affordance only; audit and ingest operator metadata remains metadata, not auth authority.
- Contract state now includes Phase 013: stable request-header names/defaults, explicit backend permission checks, no-explicit-permission read surfaces, task-create permission behavior, frontend route/API/header/localStorage/menu/action gating behavior and URL/API contracts are documented without change.
- Transition state now includes Phase 013: header-based demo auth and role-access config are current transition mechanisms only; production auth/gateway/JWT remains unimplemented and unapproved.

## Unchanged Contracts

- No URL path, HTTP method, controller owner, request binding, response envelope, response type, frontend route, frontend API function, TypeScript shape or permission behavior changed.
- No backend `requirePermission` call was added, removed, widened, narrowed, moved or renamed.
- No permission key, role code, menu key, header name, default request user/role, frontend localStorage behavior, role mapping or role-access config schema changed.
- No Java, Python, frontend, database, Redis, Kafka, `ai-config`, prompt-template, dependency, build-config, deployment or business runtime file changed.

## Remaining Debt

- D001 remains open: `ai-orchestration-service` is still a multi-domain transition host.
- D002 remains open: legacy non-task `/api/tasks/*` contracts remain transitional namespace debt.
- D003 remains open for future workbench/fallback/preview/display/generated-event/config-display/permission-display metadata surfaces.
- D007 remains open: JSON config, including `role-access-configs.json`, remains transition storage and not final config/role-store architecture.
- D008 remains open: header-based demo auth is still transition behavior, not production security.

## Latest State For Window 0

Window 0 should start from `docs/harness/state/current-state.md`.

It should automatically discover:

- Last completed phase: Phase 013 - Auth/Gateway Permission Authority Boundary.
- Latest final handoff: `docs/harness/handoffs/phase-013-final.md`.
- Matching Phase 013 handoffs:
  - `docs/harness/handoffs/steering-decision-phase-013.md`
  - `docs/harness/handoffs/phase-013-architect.md`
  - `docs/harness/handoffs/phase-013-implementation.md`
  - `docs/harness/handoffs/phase-013-review.md`
  - `docs/harness/handoffs/phase-013-final.md`
- Durable Phase 013 artifact: `docs/harness/17-auth-gateway-permission-boundary.md`.
- Durable earlier artifacts still in force:
  - `docs/harness/12-transition-host-exit-criteria.md`
  - `docs/harness/13-report-boundary-readiness.md`
  - `docs/harness/14-market-data-ingest-boundary-readiness.md`
  - `docs/harness/15-risk-strategy-projection-boundary-readiness.md`
  - `docs/harness/16-config-store-decision-boundary.md`

Window 0 must not require the user to summarize Phase 013 manually. It must score candidates using `docs/harness/10-steering-state-machine.md`, propose exactly one primary candidate and one fallback candidate, and wait for human approval before Window 1 starts.

## Recommended Candidate Inputs For Window 0

- Production auth/gateway/JWT target scoping, identity/role authority selection, service-to-service propagation planning or demo-header retirement planning only if Window 0 and the user explicitly choose to act on Phase 013 readiness gates.
- Config-store migration target/scoping, DB/Nacos/hybrid readiness, config schema/versioning or config audit/rollback planning only if Window 0 and the user explicitly choose to act on Phase 012 and Phase 013 readiness gates.
- Legacy route migration decision phase only after Window 0 accounts for the Phase 006 contract freeze and Phase 013 auth/gateway permission compatibility gates.
- Risk-service extraction, strategy-service extraction, projection-split planning, risk/strategy route migration or risk/strategy Kafka downstream planning only if Window 0 and the user explicitly choose to act on Phase 011 readiness gates.
- Report extraction or report route-migration planning only if Window 0 and the user explicitly choose to act on Phase 009 readiness gates.
- Market-service extraction, data-ingest-service extraction, market route migration or market config-store planning only if Window 0 and the user explicitly choose to act on Phase 010 readiness gates.

Window 4 does not select the next phase.

## Files Changed In This Handoff

- `docs/harness/state/current-state.md`
- `docs/harness/05-transition-lifetime.md`
- `docs/harness/06-debt-register.md`
- `docs/harness/07-phase-backlog.md`
- `docs/harness/handoffs/phase-013-final.md`

No business code was changed by Window 4.
