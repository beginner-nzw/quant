# Phase 006 Implementation Handoff

## Status

Window: Window 2 - Implementer.

Mode: Initial implementation.

Phase: Phase 006 - Legacy `/api/tasks/*` Contract Freeze for Non-Task Domains.

Git baseline recorded before edits: `b73c7d1`.

## Startup Recovery

Latest unfinished phase selected automatically:

- `docs/harness/handoffs/phase-006-architect.md` exists.
- `docs/harness/handoffs/phase-006-final.md` does not exist.
- `docs/harness/handoffs/phase-006-implementation.md` did not exist before this pass.

Related handoffs read:

- `docs/harness/handoffs/steering-decision-phase-006.md`
- `docs/harness/handoffs/phase-006-architect.md`

No Phase 006 review handoff existed, so this was not a Fix Pass.

## Files Changed By This Window

Added:

- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java`
- `docs/harness/handoffs/phase-006-implementation.md`

Not changed:

- Java production code.
- DTO, VO, entity, mapper, service, Kafka, database, `ai-config`, frontend, Python, dependency or build files.
- Harness rule/state files.
- `docs/harness/04-contract-map.md`; tests were sufficient, so no optional contract-map edit was needed.

Existing unrelated dirty/untracked harness files were observed before edits and were not reverted or bundled into this implementation claim.

## Implementation Summary

Added a focused backend test class that freezes the approved legacy non-task `/api/tasks/*` contract inventory from the Phase 006 architect handoff.

The new tests assert:

- Inventoried non-task endpoint path, HTTP method and controller owner.
- `Result<T>` response envelope and declared generic response type.
- Request binding shape for query objects, `@PathVariable`, `@RequestParam` and `@RequestBody`.
- Existing direct `roleAccessConfigService.requirePermission(...)` calls, including endpoints that intentionally have no explicit permission check.
- The approved set of `/api/tasks` controller owner classes.
- Absence of new domain namespace aliases such as `/api/reports`, `/api/market`, `/api/audit`, `/api/config` or `/api/workbench` in controller mappings.

The existing `TaskControllerMappingTest` continues to freeze the full task and non-task mapping list, including `TaskQueryController` staying task-only. The new test adds the Phase 006 response, binding, permission and owner-class guardrails without changing production behavior.

## Architect Acceptance Completed

- Non-task legacy `/api/tasks/*` endpoint inventory is documented in source tests.
- Focused backend tests now fail if inventoried endpoint path, HTTP method or controller owner drifts.
- Focused backend tests now fail if inventoried endpoints stop returning the declared `Result<T>` response type.
- Focused backend tests now fail if inventoried `@PathVariable`, `@RequestParam`, `@RequestBody` or query-object bindings drift.
- Focused backend tests now fail if current explicit permission calls are removed, changed or added contrary to the inventory.
- Focused backend tests now fail if a new `/api/tasks` controller owner appears without updating the approved inventory.
- Existing exact mapping tests continue to guard against non-task endpoints returning to `TaskQueryController`.
- No new domain URL aliases were introduced.
- No endpoint move, rename, deletion or consolidation was introduced.
- No frontend, Python, DTO/VO/entity, mapper, database schema, Kafka, `ai-config`, dependency or build-config change was introduced.
- No executable Java production behavior changed.

## Contracts Kept Stable

The following stayed unchanged:

- Legacy `/api/tasks/*` URL paths.
- HTTP methods.
- Non-task controller `@RequestMapping("/api/tasks")` base path.
- Request binding names and body/query shape.
- `com.quant.common.core.model.Result<T>` response envelope.
- Response generic types.
- Explicit permission calls and the absence of explicit permission calls where already absent.
- Workbench display-only aggregation boundary.
- Market-intelligence display/read-model boundary.
- Existing command permission behavior.

## Behavior Change

No runtime or user-visible behavior changed.

This pass added test-only guardrails. No production source, runtime annotation, route, permission, serialization, persistence, Kafka, cache, frontend or Python behavior was modified.

## Verification Results

Run from `D:\projects\bussiness`:

```powershell
rg -n "@RequestMapping|@GetMapping|@PostMapping|@PutMapping|@DeleteMapping" quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller
```

Result: passed inspection. Existing controller mappings remain under approved controller classes with `/api/tasks` base mappings.

```powershell
rg -n "requirePermission" quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller
```

Result: passed inspection. Permission calls match the Phase 006 inventory.

```powershell
rg -n "/api/(reports|risk|risks|strategy|strategies|market|markets|audit|config|workbench)" quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller
```

Result: no matches. No new domain URL namespace appeared in controller mappings or comments.

Run from `D:\projects\bussiness\quant-ai-platform\quant-services`:

```powershell
mvn -q test
```

Result: passed. Maven exit code `0`.

Note: test output included an existing simulated `kafka down` warning stack trace from `TaskOutboxPublisherServiceTests.publishPendingOnceShouldMarkFailedWhenKafkaSendFails`; the Maven test run still completed successfully.

Run from `D:\projects\bussiness`:

```powershell
git diff --name-only
```

Result before this handoff: only pre-existing tracked dirty file `docs/harness/state/current-state.md` was listed. The new test file and this implementation handoff are untracked additions until staged; unrelated pre-existing dirty/untracked files were left untouched.

## Blockers And Residual Risk

Blockers: none.

Residual risk:

- The new permission guard intentionally checks direct controller source calls to `RoleAccessConfigService.PERMISSION_*`. If a later approved phase changes permission style while preserving behavior, that later phase must update the Phase 006 contract inventory and tests deliberately.
- Existing unrelated harness dirty/untracked files remain outside this Window 2 change claim.

