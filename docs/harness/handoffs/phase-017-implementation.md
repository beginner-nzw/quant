# Phase 017 Implementation Handoff

## Status

Window: Window 2 - Implementer.

Phase: Phase 017 - Production Role Authority Selection Boundary.

Mode: initial implementation.

Implementation type: docs-only architecture/governance work.

This handoff records a docs-only implementation of the approved Phase 017 architect handoff. It does not select the next phase and does not modify governance rules outside the architect-approved file scope.

## Git Baseline

`git status --short --untracked-files=all` was run before edits.

Pre-existing dirty or untracked files at Window 2 start:

- `M docs/harness/state/current-state.md`
- `?? docs/harness/handoffs/phase-017-architect.md`
- `?? docs/harness/handoffs/steering-decision-phase-017.md`

These files existed before this Window 2 implementation and are excluded from this Window 2 change claim.

## Files Changed By This Window

Window 2 changed only files allowed by `docs/harness/handoffs/phase-017-architect.md`:

- `docs/harness/21-production-role-authority-boundary.md`
- `docs/harness/handoffs/phase-017-implementation.md`

No Java, Python, frontend, database, Redis, Kafka, `ai-config`, prompt-template, dependency, build, deployment, state, debt, backlog, transition lifetime, durable Phase 012/013/014/015/016 artifact or prior handoff file was modified by this Window 2 pass.

## Completed Architect Acceptance

Completed:

- Created `docs/harness/21-production-role-authority-boundary.md` as the primary durable production role authority boundary artifact.
- Recorded exact files changed and verification outcomes in this implementation handoff.
- Kept the artifact docs-only and explicitly future-only.
- Restated inherited Phase 012, Phase 013, Phase 014, Phase 015 and Phase 016 facts for role config, headers, defaults, runtime context, backend permission checks, no-explicit-permission read surfaces, frontend gating, current absence of production auth/role infrastructure, future-only validator placement and future-only issuer direction.
- Selected a future production role authority direction: backend-owned application role authority for role assignment and role-permission/menu mapping, with external IdP or directory claims limited to future inputs pending later approved mapping.
- Labeled the selected role authority direction as future-only and not current runtime authority.
- Preserved demo-header compatibility without changing `X-User-Id` or `X-User-Role` behavior.
- Defined claim/group, user profile, token/session, service-principal, service-to-service handoff, config-store, role-store and audit identity readiness gates without changing code, headers, Kafka payloads, DTOs, frontend types or runtime behavior.
- Preserved Phase 005 through Phase 016 constraints.
- Labeled later gateway/JWT, user profile, role-store, config-store, route migration, service extraction and implementation work as deferred future decisions requiring a later Window 0 decision plus human approval.

## Contracts Kept Unchanged

The implementation preserves:

- All URL paths, HTTP methods, endpoint owners, request bindings, response envelopes and response types.
- All frontend routes, API function names, endpoint strings, call signatures, TypeScript shapes and local role behavior.
- All role codes, permission keys, menu keys, coarse access-role mappings, backend `requirePermission` calls and intentional no-explicit-permission read surfaces.
- `X-User-Id`, `X-User-Role`, `X-Trace-Id`, missing backend defaults `guest` and `USER`, and frontend selected-user key `quant_current_user`.
- `role-access-configs.json` shape and its current status as a transition role/menu/permission config input.
- DTO, VO, entity, mapper, schema, Redis key, Kafka topic, Kafka payload, JSON config shape, prompt-template shape, Python payload and runtime setting contracts.

## Behavior Changes

No runtime behavior changed.

No business behavior, permission behavior, frontend behavior, Python behavior, Kafka behavior, Redis behavior, database behavior, config behavior, prompt-template behavior, build behavior, deployment behavior or gateway/auth behavior changed.

## Verification Results

Read-only inventory checks from `D:\projects\bussiness`:

- `rg -n "HEADER_USER_ID|HEADER_USER_ROLE|DEFAULT_USER_ID|DEFAULT_USER_ROLE|X-User-Id|X-User-Role|X-Trace-Id|UserContext|currentUserRole|currentUserId|UserRoleEnum" quant-ai-platform/quant-services/quant-common/quant-common-security/src/main/java/com/quant/common/security` passed and confirmed security constants, defaults, `UserContext`, `UserContextFilter`, `SecurityUtils` and `UserRoleEnum` surfaces.
- `rg -n "RoleAccessConfigService|TaskRoleAccessService|requirePermission|PERMISSION_|currentUserRole|currentUserId|role-access-configs|EventAutoTaskDispatchService|system|ADMIN" quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java quant-ai-platform/quant-services/quant-business/research-task-service/src/main/java` passed and confirmed current backend role access services, permission call sites, config file references, current context readers and event auto task dispatch context behavior.
- `rg -n "X-User-Id|X-User-Role|quant_current_user|quant_role_access_configs|ROLE_ACCESS_UPDATED_EVENT|USER_ROLE|PERMISSION_KEY|MENU_KEY|fetchRoleAccessConfigs|updateRoleAccessConfig|requiredMenuKey|requiredPermissionKey" quant-ui/src/api/task.ts quant-ui/src/router/index.ts quant-ui/src/layout/BasicLayout.vue quant-ui/src/utils/auth.ts quant-ui/src/utils/requestHeaders.ts quant-ui/src/utils/request.ts quant-ui/src/utils/roleAccess.ts quant-ui/src/utils/taskActionAccess.ts quant-ui/src/views quant-ui/scripts/authority-boundary-check.mjs` passed and confirmed frontend local role, header, role-access cache, route/menu/action gating and role API surfaces.
- `rg -n "roleCode|permissionKeys|menuKeys|TASK_VIEW|TASK_CREATE|TASK_RETRY|TASK_CANCEL|AUDIT_COMPLIANCE_VIEW|REPORT_REVIEW|MODEL_AGENT_CONFIG_VIEW|MODEL_AGENT_CONFIG_EDIT|TASK_LIST|MARKET_EVENTS|MARKET_INTELLIGENCE|RESEARCH_WORKBENCH|STRATEGY_SIGNALS|RISK_WARNINGS|RESEARCH_REPORTS|AUDIT_COMPLIANCE|MODEL_AGENT_CONFIG|REPORTS_PENDING|REPORTS_APPROVED|REPORTS_REJECTED|RESEARCHER|PM|RISK_MANAGER|COMPLIANCE_AUDITOR|ADMIN" quant-ai-platform/ai-config/role-access-configs.json` passed and confirmed current role codes, permission keys and menu keys.

Frontend guard from `D:\projects\bussiness\quant-ui`:

- `node scripts/authority-boundary-check.mjs` passed with `authority-boundary-check passed`.

Post-edit documentation checks from `D:\projects\bussiness`:

- `Test-Path docs/harness/21-production-role-authority-boundary.md` was run after edits.
- `Test-Path docs/harness/handoffs/phase-017-implementation.md` was run after edits.
- Required `rg` checks over the Phase 017 durable artifact and implementation handoff were run after edits.
- Final `git status --short --untracked-files=all` was run before staging.

Maven, npm build and Python runtime verification were not required because Phase 017 forbids Java, frontend, Python and test-code changes. No such files were touched.

## Blockers Or Residual Risks

Blockers: none.

Residual risks:

- D001, D002, D003, D007 and D008 remain open. Phase 017 does not close transition-host, legacy-route, metadata authority, JSON config or demo-header debt.
- The concrete production role authority host remains deferred. Later phases must decide whether a DB role store, auth-service, user-service, role-service, config-store-backed mapping or another backend-owned host carries the selected future authority.
- External IdP or directory group/claim mapping remains deferred. No claim mapping, gateway/JWT implementation, role-store migration or permission behavior change is approved by Phase 017.

## Re-Review Need

Phase 017 requires Window 3 review/eval after this implementation handoff. Window 3 should evaluate the durable artifact and this handoff against the architect acceptance conditions in:

- `docs/harness/handoffs/phase-017-architect.md`
