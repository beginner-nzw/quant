# Phase 017 Review

## Status

Window: Window 3 - Review / Eval.

Phase: Phase 017 - Production Role Authority Selection Boundary.

Review mode: initial Review.

Decision: approve.

Window 4 allowed: yes.

## Handoff Files Read

- `docs/harness/00-project-charter.md`
- `docs/harness/02-authority-matrix.md`
- `docs/harness/03-host-ownership.md`
- `docs/harness/04-contract-map.md`
- `docs/harness/05-transition-lifetime.md`
- `docs/harness/06-debt-register.md`
- `docs/harness/08-eval-checklist.md`
- `docs/harness/09-window-protocol.md`
- `docs/harness/state/current-state.md`
- `docs/harness/handoffs/steering-decision-phase-017.md`
- `docs/harness/handoffs/phase-017-architect.md`
- `docs/harness/handoffs/phase-017-implementation.md`

No prior Phase 017 review or fix-pass handoff existed, so this was an initial review.

## Git / Diff Evidence

Reviewed current worktree and implementation commit.

`git show --name-only --format="%H %s" HEAD` showed:

- `e9403d02affc6a967325326959372c44917ac09d phase-017: implement role authority boundary`
- `docs/harness/21-production-role-authority-boundary.md`
- `docs/harness/handoffs/phase-017-implementation.md`

This matches the architect-approved Window 2 write scope.

`git status --short --untracked-files=all` during review showed:

- `M docs/harness/state/current-state.md`
- `?? docs/harness/handoffs/phase-017-architect.md`
- `?? docs/harness/handoffs/steering-decision-phase-017.md`

These were recorded by the implementation handoff as pre-existing inputs and are not part of the Phase 017 implementation commit reviewed here. Review did not modify or revert them.

## Verification Run

Review reran the Phase 007 frontend authority guard from `D:\projects\bussiness\quant-ui`:

```powershell
node scripts/authority-boundary-check.mjs
```

Result:

```text
authority-boundary-check passed
```

Review also checked:

- `Test-Path docs/harness/21-production-role-authority-boundary.md` -> `True`
- `Test-Path docs/harness/handoffs/phase-017-implementation.md` -> `True`
- Required positive/negative text searches over the durable artifact and implementation handoff.

Maven, npm build and Python runtime verification were not required by the architect handoff because Phase 017 is docs-only and did not change Java, frontend, Python or test code.

## Findings

No findings requiring fixes.

## Belongs Review

Approved.

Evidence:

- `docs/harness/21-production-role-authority-boundary.md:11` states Phase 017 does not implement or approve gateway/auth/JWT, external IdP integration, service creation, route migration, config mutation, role-store migration, frontend/Python changes or business behavior changes.
- `docs/harness/21-production-role-authority-boundary.md:150` begins the selected future role-authority direction section.
- `docs/harness/21-production-role-authority-boundary.md:161` states the selected direction is not current runtime authority and creates no role store, role API, role service, gateway, JWT validator, claim mapper, connector, endpoint alias, migration path or production role implementation.
- `docs/harness/handoffs/phase-017-implementation.md:27` records the Window 2 changed-file scope.
- `docs/harness/handoffs/phase-017-implementation.md:34` states no Java, Python, frontend, database, Redis, Kafka, config, prompt-template, dependency, build, deployment, state, debt, backlog, transition-lifetime or prior durable artifacts were modified.

The implementation stays inside the docs-only host/scope approved by Window 1.

## Authority Review

Approved.

Evidence:

- `docs/harness/21-production-role-authority-boundary.md:9` selects a future backend-owned application role authority while keeping external IdP/directory groups and claims as future inputs only.
- `docs/harness/21-production-role-authority-boundary.md:57` keeps `role-access-configs.json` as the current transition role/menu/permission config input.
- `docs/harness/21-production-role-authority-boundary.md:58` keeps `X-User-Id` and `X-User-Role` as demo/runtime inputs only.
- `docs/harness/21-production-role-authority-boundary.md:61` keeps `UserContext` as runtime context, not production identity, profile or role authority.
- `docs/harness/21-production-role-authority-boundary.md:190` states future production role authority is backend-owned application role authority, not frontend localStorage, headers, workbench output, audit metadata or fallback provenance.
- `docs/harness/21-production-role-authority-boundary.md:199` explicitly does not close D001, D002, D003, D007 or D008.

No second current source of truth is introduced. The selected authority direction is future-only and bounded.

## Contract Review

Approved.

Evidence:

- `docs/harness/21-production-role-authority-boundary.md:137` states Phase 017 adds, removes or renames no role code, permission key, menu key, coarse access role, route metadata, API function, backend permission check, frontend localStorage key, header name or default value.
- `docs/harness/21-production-role-authority-boundary.md:207` states no explicit `requirePermission` call is added, removed, widened, narrowed, moved or renamed.
- `docs/harness/21-production-role-authority-boundary.md:211` requires any later permission widening/narrowing/check change to go through a later Window 0 decision and human approval.
- `docs/harness/21-production-role-authority-boundary.md:229` through `docs/harness/21-production-role-authority-boundary.md:239` preserve current role codes, permission keys, menu keys, coarse access roles, backend checks, no-explicit-permission surfaces, headers/defaults, frontend key, config shape, URL/API contracts and frontend contracts.
- `docs/harness/21-production-role-authority-boundary.md:339` states Phase 017 adds no auth URL, login URL, callback URL, gateway URL, route alias, compatibility endpoint, proxy route, role endpoint or new namespace.
- `docs/harness/21-production-role-authority-boundary.md:345` through `docs/harness/21-production-role-authority-boundary.md:350` preserve Phase 006, Phase 012, Phase 013, Phase 014, Phase 015 and Phase 016 dependencies.

No endpoint, role, permission, frontend route, header, DTO/payload, or config contract is changed or duplicated.

## Behavior Review

Approved.

Evidence:

- `docs/harness/21-production-role-authority-boundary.md:476` states no Java, Python, frontend, config, database, Redis, Kafka, dependency, build, deployment or runtime file changed.
- `docs/harness/handoffs/phase-017-implementation.md:64` states no runtime behavior changed.
- `docs/harness/handoffs/phase-017-implementation.md:79` records `authority-boundary-check passed`.
- `git show --name-only --format="%H %s" HEAD` showed only the durable artifact and implementation handoff in commit `e9403d02affc6a967325326959372c44917ac09d`.

Behavior acceptance is satisfied for a docs-only phase. Runtime build/test commands were not required because no runtime code changed.

## Acceptance Against Window 1

Window 1 acceptance is satisfied:

- `docs/harness/21-production-role-authority-boundary.md` exists as the durable artifact.
- `docs/harness/handoffs/phase-017-implementation.md` exists and records changed files plus verification outcomes.
- The artifact selects a future-only backend-owned application role authority direction.
- Current role config, headers, defaults, runtime context, backend permission checks, no-explicit-permission surfaces, frontend gating, future validator placement and future issuer direction are preserved.
- Later gateway/JWT, user profile, token/session, claim mapping, service-principal, service-to-service, role-store, config-store, route migration, service extraction and audit identity work is deferred to future Window 0 decision and human approval.
- No forbidden Java, Python, frontend, config, database, Redis, Kafka, dependency, build, deployment, state/debt/backlog/transition-lifetime, durable Phase 012-016 or prior handoff file changes are part of the implementation commit.

## Residual Risk

- D001, D002, D003, D007 and D008 remain open by design.
- The concrete production role authority host remains deferred.
- External IdP/directory claim mapping, gateway/JWT implementation, user profile source, role-store migration, config-store migration and permission behavior changes remain future-only and unapproved.

These are expected residual risks, not Phase 017 fix findings.

## Final Decision

approve.

Phase 017 may proceed to Window 4.
