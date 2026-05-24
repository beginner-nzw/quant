# Phase 006 Fix 2 Implementation Handoff

## Status

Window: Window 2 - Implementer.

Mode: Fix Pass 2.

Phase: Phase 006 - Legacy `/api/tasks/*` Contract Freeze for Non-Task Domains.

Latest review handoff: `docs/harness/handoffs/phase-006-review-fix-1.md`.

Review decision addressed: `require fixes`.

Git baseline recorded before fix edits: `b73c7d1`.

## Startup Recovery

Latest unfinished phase selected automatically:

- `docs/harness/handoffs/phase-006-architect.md` exists.
- `docs/harness/handoffs/phase-006-implementation.md` exists.
- `docs/harness/handoffs/phase-006-review.md` exists and required fixes.
- `docs/harness/handoffs/phase-006-fix-1-implementation.md` exists.
- `docs/harness/handoffs/phase-006-review-fix-1.md` exists and required fixes.
- `docs/harness/handoffs/phase-006-final.md` does not exist.

Related handoffs read:

- `docs/harness/handoffs/steering-decision-phase-006.md`
- `docs/harness/handoffs/phase-006-architect.md`
- `docs/harness/handoffs/phase-006-implementation.md`
- `docs/harness/handoffs/phase-006-review.md`
- `docs/harness/handoffs/phase-006-fix-1-implementation.md`
- `docs/harness/handoffs/phase-006-review-fix-1.md`

Git baseline at startup already contained unrelated dirty or untracked files, including `docs/harness/state/current-state.md`, prior Phase 006 handoffs, `TaskControllerMappingTest.java`, and the Phase 006 test file. They were treated as pre-existing baseline state and were not reverted.

## Files Changed By This Fix Pass

Modified:

- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java`

Added:

- `docs/harness/handoffs/phase-006-fix-2-implementation.md`

Not changed by this pass:

- Java production code.
- `TaskControllerMappingTest.java`; it remained dirty from the baseline before this Fix Pass.
- Controller runtime annotations or logic.
- DTO, VO, entity, mapper, service, Kafka, database, `ai-config`, frontend, Python, dependency or build files.
- Harness rule/state files and prior phase handoffs.

## Review Finding Addressed

### Finding: Medium - New `/api/tasks/*` endpoints can still bypass the inventory if declared as full method-level paths in a new controller

Corresponding review finding:

- `legacyNonTaskEndpointInventoryRejectsUnregisteredControllerMethods()` only scanned the hard-coded approved non-task controller list.
- The source owner guard only found class-level `@RequestMapping("/api/tasks")` owners.
- A new controller with a method-level full mapping such as `@GetMapping("/api/tasks/report-export")` and no class-level `/api/tasks` base mapping could create an unapproved legacy endpoint without being detected.

Fix:

- Added `allApiTasksEndpointMappingsRemainInApprovedInventory()`.
- The new guard discovers every `*Controller.java` source file in the controller package, loads its controller class, and reflects over all methods.
- The mapping extractor now supports controllers without class-level `@RequestMapping`; such methods use an empty base path, so full method-level paths are included.
- Every reflected mapping whose effective path is `/api/tasks` or starts with `/api/tasks/` is compared against the approved Phase 006 inventory.
- The approved inventory combines current task endpoints plus the existing Phase 006 non-task contract inventory.

Why this did not expand scope:

- The change is test-only and stays under the allowed backend test directory.
- It addresses only the endpoint-discovery gap listed in `phase-006-review-fix-1.md`.
- It does not add production helpers, aliases, routes, bridges, fallback paths, controller behavior, DTO/VO/entity changes, frontend changes or Python changes.
- It preserves the existing URL, method, owner, response, binding and permission assertions.

## Architect Acceptance Completed By This Fix

- Focused backend tests now detect `/api/tasks` mappings across all controller source files, not only the approved controller lists.
- Focused backend tests now fail if a new controller declares a full method-level `/api/tasks/...` mapping without updating the Phase 006 inventory.
- Existing non-task inventory, response envelope, declared generic response type, request binding and permission assertions remain in place.
- Existing checks for approved `/api/tasks` owner classes and domain namespace aliases remain in place.

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
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/TaskControllerMappingTest.java`

Both were already dirty before this Fix Pass. `LegacyTaskApiContractFreezeTest.java` and this handoff are untracked additions in `git status --short --untracked-files=all`.

## Blockers And Residual Risk

Blockers: none.

Residual risk:

- The new all-controller scan depends on controller source files matching loadable classes in `com.quant.aiorchestrator.controller`, which is the current codebase convention.
- Existing pre-baseline dirty/untracked Phase 006 files remain present. Only files touched by this Fix Pass should be staged by this window.

## Re-Review Required

Yes. Window 3 should re-review this Fix Pass and write `docs/harness/handoffs/phase-006-review-fix-2.md`.
