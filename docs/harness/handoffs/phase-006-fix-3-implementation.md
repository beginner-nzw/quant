# Phase 006 Fix 3 Implementation Handoff

## Status

Window: Window 2 - Implementer.

Mode: Fix Pass 3.

Phase: Phase 006 - Legacy `/api/tasks/*` Contract Freeze for Non-Task Domains.

Latest review handoff: `docs/harness/handoffs/phase-006-review-fix-2.md`.

Review decision addressed: `require fixes`.

## Startup Recovery

Latest unfinished phase selected automatically:

- `docs/harness/handoffs/phase-006-architect.md` exists.
- `docs/harness/handoffs/phase-006-implementation.md` exists.
- `docs/harness/handoffs/phase-006-review.md` exists and required fixes.
- `docs/harness/handoffs/phase-006-fix-1-implementation.md` exists.
- `docs/harness/handoffs/phase-006-review-fix-1.md` exists and required fixes.
- `docs/harness/handoffs/phase-006-fix-2-implementation.md` exists.
- `docs/harness/handoffs/phase-006-review-fix-2.md` exists and required fixes.
- `docs/harness/handoffs/phase-006-final.md` does not exist.

Related handoffs read:

- `docs/harness/handoffs/steering-decision-phase-006.md`
- `docs/harness/handoffs/phase-006-architect.md`
- `docs/harness/handoffs/phase-006-implementation.md`
- `docs/harness/handoffs/phase-006-review.md`
- `docs/harness/handoffs/phase-006-fix-1-implementation.md`
- `docs/harness/handoffs/phase-006-review-fix-1.md`
- `docs/harness/handoffs/phase-006-fix-2-implementation.md`
- `docs/harness/handoffs/phase-006-review-fix-2.md`

Git baseline before edits already contained unrelated dirty or untracked files, including `docs/harness/state/current-state.md`, prior untracked phase handoffs and a dirty `TaskControllerMappingTest.java`. They were treated as baseline state and were not reverted. `TaskControllerMappingTest.java` was touched only because the latest review cited its path-joining helper as part of the remaining finding.

## Files Changed By This Fix Pass

Modified:

- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/TaskControllerMappingTest.java`

Added:

- `docs/harness/handoffs/phase-006-fix-3-implementation.md`

Not changed:

- Java production code.
- Controller runtime annotations or logic.
- DTO, VO, entity, mapper, service, Kafka, database, `ai-config`, frontend, Python, dependency or build files.
- Harness rule/state files and prior phase handoffs.

## Review Findings Addressed

### Finding 1: Medium - `/api/tasks/*` inventory scan still misses valid relative path combinations

Corresponding review finding:

- The all-controller scan joined base and method mapping strings directly, so class-level `@RequestMapping("/api")` plus method-level `@GetMapping("tasks/report-export")` could be misread as `/apitasks/report-export` and skipped by the `/api/tasks` inventory guard.

Fix:

- Normalized test-local path joining so base and method paths are combined with exactly one slash.
- Covered no-base full method paths, base `/api` plus relative `tasks/...`, base `/api/` plus `/tasks/...`, and existing `/api/tasks` plus child-path shapes.
- Applied the same normalization to the existing `TaskControllerMappingTest` helper cited by the review.

Why this did not expand scope:

- The change is backend test-only and stays inside the allowed Phase 006 test directory.
- No endpoint path, controller annotation, production route, alias, bridge or runtime behavior changed.
- The fix only models Spring mapping combination semantics for the already approved test guard.

### Finding 2: Medium - Permission guard can miss newly added explicit permission checks

Corresponding review finding:

- The permission assertion only collected `requirePermission(RoleAccessConfigService.PERMISSION_...)`, so endpoints with no explicit permission could gain a string-literal, local-variable or otherwise styled `requirePermission(...)` call without failing the test.

Fix:

- `LegacyTaskApiContractFreezeTest` now first detects any `requirePermission(...)` call in each inventoried controller method body.
- Endpoints with no explicit permission fail on any detected call, regardless of argument style.
- Endpoints with an expected explicit permission must keep exactly one call whose argument references the expected `RoleAccessConfigService.PERMISSION_*` constant.

Why this did not expand scope:

- The change is source-level test assertion only.
- No permission constant, role config, controller behavior or access rule changed.
- The fix closes only the latest review's permission-call detection gap.

## Architect Acceptance Completed By This Fix

- Focused backend tests now normalize `/api/tasks` mapping paths across common Spring base/method path combinations.
- Focused backend tests now fail if a new `/api/tasks/*` endpoint is hidden by relative path composition without updating the approved inventory.
- Focused backend tests now fail if any explicit permission call is added to an endpoint whose Phase 006 contract has no explicit permission.
- Focused backend tests now fail if an endpoint with an expected explicit permission removes the call, adds extra calls or stops referencing the expected permission constant.
- Existing URL, HTTP method, controller owner, response envelope, declared response type, request binding and inventory assertions remain in place.

## Contracts Kept Stable

The following stayed unchanged:

- Legacy `/api/tasks/*` URL paths.
- HTTP methods.
- Controller ownership.
- Request binding annotations and names.
- `Result<T>` response envelopes and declared generic response types.
- Explicit permission calls and intentional absence of explicit permission calls.
- Workbench display-only aggregation boundary.
- Market-intelligence display/read-model boundary.
- Existing command permission behavior.

## Behavior Change

No runtime or user-visible behavior changed.

This fix pass changed tests only. No production source, runtime annotation, route, permission, serialization, persistence, Kafka, cache, frontend or Python behavior was modified.

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

Result: passed, exit code `0`.

The Maven output still included the existing simulated `kafka down` warning stack trace from `TaskOutboxPublisherServiceTests.publishPendingOnceShouldMarkFailedWhenKafkaSendFails`; Maven completed successfully.

Run from `D:\projects\bussiness`:

```powershell
git diff --name-only
```

Result after the code fix and before this handoff listed:

- `docs/harness/state/current-state.md`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/TaskControllerMappingTest.java`

`docs/harness/state/current-state.md` was already dirty before this Fix Pass and was not modified by this pass.

## Blockers And Residual Risk

Blockers: none.

Residual risk:

- The permission assertion remains a source-level test over current controller method bodies. A later approved phase that changes permission style while preserving behavior must deliberately update the Phase 006 inventory and tests.
- The controller mapping scan continues to rely on controller classes and source files following the current `com.quant.aiorchestrator.controller` convention.
- Pre-existing unrelated dirty/untracked harness files remain outside this Window 2 fix claim.

## Re-Review Required

Yes. Window 3 should re-review this Fix Pass and write `docs/harness/handoffs/phase-006-review-fix-3.md`.
