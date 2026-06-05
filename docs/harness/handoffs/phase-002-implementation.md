# Phase 002 Implementation Handoff

## Status

Phase 002 implementation completed: backend-owned role authority, minimal user profile source and compatible role store path are now implemented.

This phase preserves existing role codes, permission keys, menu keys, endpoint paths, request headers and frontend localStorage behavior. Frontend localStorage remains UI/demo transport state only and is not role truth.

## Inputs Read First

- `docs/harness/handoffs/phase-001-implementation.md`
- `docs/harness/handoffs/phase-001-review.md`
- `docs/harness/16-config-store-decision-boundary.md`
- `docs/harness/17-auth-gateway-permission-boundary.md`
- `docs/harness/21-production-role-authority-boundary.md`
- `docs/harness/22-remaining-governance-closure.md`

## Files Changed

Shared security/profile/role authority:

- `quant-ai-platform/quant-services/quant-common/quant-common-security/src/main/java/com/quant/common/security/UserProfile.java`
- `quant-ai-platform/quant-services/quant-common/quant-common-security/src/main/java/com/quant/common/security/UserProfileStatus.java`
- `quant-ai-platform/quant-services/quant-common/quant-common-security/src/main/java/com/quant/common/security/UserProfileSource.java`
- `quant-ai-platform/quant-services/quant-common/quant-common-security/src/main/java/com/quant/common/security/InMemoryUserProfileSource.java`
- `quant-ai-platform/quant-services/quant-common/quant-common-security/src/main/java/com/quant/common/security/RoleAccessDefinition.java`
- `quant-ai-platform/quant-services/quant-common/quant-common-security/src/main/java/com/quant/common/security/RoleAccessAuthority.java`
- `quant-ai-platform/quant-services/quant-common/quant-common-security/src/main/java/com/quant/common/security/UserContext.java`
- `quant-ai-platform/quant-services/quant-common/quant-common-security/src/main/java/com/quant/common/security/UserContextFilter.java`
- `quant-ai-platform/quant-services/quant-common/quant-common-security/src/main/java/com/quant/common/security/SecurityUtils.java`
- `quant-ai-platform/quant-services/quant-common/quant-common-security/src/main/java/com/quant/common/security/RoleChecker.java`

Service wiring and permission readers:

- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/config/CommonInfraConfig.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/RoleAccessConfigServiceImpl.java`
- `quant-ai-platform/quant-services/quant-business/research-task-service/src/main/java/com/quant/task/config/CommonInfraConfig.java`
- `quant-ai-platform/quant-services/quant-business/research-task-service/src/main/java/com/quant/task/service/impl/TaskRoleAccessServiceImpl.java`

Tests:

- `quant-ai-platform/quant-services/quant-common/quant-common-security/src/test/java/com/quant/common/security/UserContextFilterTests.java`
- `quant-ai-platform/quant-services/quant-common/quant-common-security/src/test/java/com/quant/common/security/RoleAccessAuthorityTests.java`

Handoff:

- `docs/harness/handoffs/phase-002-implementation.md`

No frontend, Python, database schema, Redis, Kafka, endpoint, permission key, menu key, role code or `role-access-configs.json` file was deleted or renamed.

## Implementation Summary

- Added `UserProfile` with `userId`, `displayName`, `status` and `roles`.
- Added `InMemoryUserProfileSource` as the minimal backend profile source and role store seed.
- Added `RoleAccessAuthority` as the backend production authority for role-permission-menu mapping.
- Preserved `role-access-configs.json` as a migration/local compatibility input. It can narrow effective mappings, but cannot widen beyond the backend baseline.
- Extended `UserContext` to carry profile-derived display name, status and roles while keeping current user id and header/JWT role for compatibility/audit display.
- Updated `UserContextFilter` so JWT/demo identity resolves profile by `userId`.
- Updated `RoleAccessConfigServiceImpl`, `TaskRoleAccessServiceImpl` and `RoleChecker` so authorization uses backend profile roles, not frontend localStorage or request/JWT role claims.
- Kept existing `hasPermission(String currentRole, ...)` compatibility behavior but backed it by the new authority baseline.
- Added save-time compatibility validation for role access config updates to block accidental permission/menu widening.

## Authority Notes

- Production role/menu/permission authority is backend-owned in `RoleAccessAuthority`.
- Profile authority is backend-owned in `UserProfileSource`.
- JWT and demo headers provide identity input only. Their role value remains compatibility metadata and does not grant permissions by itself.
- Unknown users resolve to `UNKNOWN` profile status and no roles.
- Disabled users retain profile facts but receive no permissions.
- `role-access-configs.json` remains present and readable as a compatible seed/migration input, not final production authority.

## Verification Results

Run from `D:\projects\bussiness\quant-ai-platform\quant-services`:

```powershell
mvn -q -pl quant-common/quant-common-security test
```

Result: passed.

Run from `D:\projects\bussiness\quant-ai-platform\quant-services`:

```powershell
mvn -q -pl quant-business/ai-orchestration-service,quant-business/research-task-service -am test
```

Result: passed. The existing `TaskOutboxPublisherServiceTests.publishPendingOnceShouldMarkFailedWhenKafkaSendFails` still logs the expected `kafka down` stack trace while the test run succeeds.

Coverage added:

- Permission positive check: active backend profile role grants mapped permission.
- Permission negative check: active backend profile role does not grant unmapped permission.
- No widening check: JSON compatibility input cannot grant `MODEL_AGENT_CONFIG_EDIT` or menu access to `RESEARCHER`.
- Disabled user check: disabled profile does not grant permission.
- Unknown user check: unknown profile does not grant permission.
- JWT userId path: JWT `sub=admin` resolves backend profile roles and grants admin role authority even if JWT role claim is `USER`.
- JWT/header role non-authority path: unknown `jwt-user` with JWT role `ADMIN` does not receive admin authority.

## Residual Risks

- The minimal profile source is in-memory seed data. It is intentionally small for this phase and should be replaced by an approved durable user/profile store in a later phase.
- Role-access update APIs still mutate the existing JSON compatibility file for local/config-center behavior, but save-time validation prevents widening beyond backend production baseline.
