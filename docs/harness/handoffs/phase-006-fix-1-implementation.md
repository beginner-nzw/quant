# Phase 006 Fix 1 Implementation Handoff

## Status

Window: Window 2 - Implementer.

Mode: Fix Pass 1.

Phase: Phase 006 - Legacy `/api/tasks/*` Contract Freeze for Non-Task Domains.

Latest review handoff: `docs/harness/handoffs/phase-006-review.md`.

Review decision addressed: `require fixes`.

Git baseline recorded before fix edits: `b73c7d1`.

## Startup Recovery

Latest unfinished phase selected automatically:

- `docs/harness/handoffs/phase-006-architect.md` exists.
- `docs/harness/handoffs/phase-006-implementation.md` exists.
- `docs/harness/handoffs/phase-006-final.md` does not exist.
- Latest Phase 006 review handoff is `docs/harness/handoffs/phase-006-review.md` with decision `require fixes`.
- No prior `phase-006-fix-*-implementation.md` handoff existed, so this is Fix Pass 1.

Related handoffs read:

- `docs/harness/handoffs/steering-decision-phase-006.md`
- `docs/harness/handoffs/phase-006-architect.md`
- `docs/harness/handoffs/phase-006-implementation.md`
- `docs/harness/handoffs/phase-006-review.md`

## Files Changed By This Fix Pass

Modified:

- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/TaskControllerMappingTest.java`

Added:

- `docs/harness/handoffs/phase-006-fix-1-implementation.md`

Not changed:

- Java production code.
- Controller runtime annotations or logic.
- DTO, VO, entity, mapper, service, Kafka, database, `ai-config`, frontend, Python, dependency or build files.
- Harness rule/state files.
- Prior phase handoffs.

Existing unrelated dirty/untracked harness files were observed and left untouched.

## Review Findings Addressed

### Finding 1: High - Contract freeze does not reject all unregistered legacy endpoints

Corresponding review finding:

- `LegacyTaskApiContractFreezeTest` and `TaskControllerMappingTest` only recognized `@GetMapping` and `@PostMapping`, so `@PutMapping`, `@DeleteMapping`, `@PatchMapping` or method-level `@RequestMapping(method = ...)` endpoints could bypass the inventory guard.

Fix:

- Updated `LegacyTaskApiContractFreezeTest` mapping extraction to collect all paths from:
  - `@GetMapping`
  - `@PostMapping`
  - `@PutMapping`
  - `@DeleteMapping`
  - `@PatchMapping`
  - method-level `@RequestMapping`
- Method-level `@RequestMapping` now records explicit `RequestMethod` values, or `ANY` when no method is specified.
- Multi-path annotation values are expanded instead of only checking the first path.
- Updated `TaskControllerMappingTest` with the same mapping coverage so the existing full `/api/tasks` mapping guard does not keep the same blind spot.
- Updated the domain namespace alias source check in `LegacyTaskApiContractFreezeTest` to include `@PatchMapping`.

Why this did not expand scope:

- Both edited files are backend test files under the allowed Phase 006 test directory.
- `TaskControllerMappingTest` was explicitly cited by the review evidence as having the same GET/POST-only extraction gap.
- No production helper, adapter, route, alias, bridge or runtime contract marker was added.
- The fix only closes the endpoint-detection gap listed by the latest review.

### Finding 2: Medium - Request parameter binding drift is not fully frozen

Corresponding review finding:

- `bindingsFor(...)` captured `@RequestParam` name and Java type, but did not compare `RequestParam.required()` or `RequestParam.defaultValue()`.

Fix:

- Extended the test-local `Binding` record to include a `defaultValue` field.
- Captured `RequestParam.required()` and normalized `RequestParam.defaultValue()` in `bindingsFor(...)`.
- Kept the Phase 006 inventory expectation for current request params as required with no default value.
- Existing request-body required behavior remains asserted through the same binding record.

Why this did not expand scope:

- The change is test-only and targets exactly the request-binding behavior named in the review finding.
- No controller parameter, DTO, request binding annotation or runtime behavior changed.

## Architect Acceptance Completed By This Fix

- Focused backend tests now see non-task legacy endpoint definitions created through GET, POST, PUT, DELETE, PATCH and method-level `@RequestMapping`.
- Focused backend tests now fail if a new non-task legacy endpoint appears under an approved `/api/tasks` controller through those mapping forms without updating the Phase 006 inventory.
- The existing full task/non-task mapping guard now uses the same mapping coverage, so `TaskQueryController` cannot regain a non-task endpoint through a non-GET/POST mapping without test failure.
- Focused backend tests now fail if inventoried `@RequestParam` required/default-value behavior drifts.
- Existing URL, HTTP method, controller owner, response envelope, declared response type, path variable, request body, query object and permission assertions remain in place.

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

Run from `D:\projects\bussiness\quant-ai-platform\quant-services`:

```powershell
mvn -q test
```

Result: passed, exit code `0`, after rerunning outside the sandbox because the first sandbox run could not write Maven `target/maven-status` files.

The passing Maven output still included the existing simulated `kafka down` warning stack trace from `TaskOutboxPublisherServiceTests.publishPendingOnceShouldMarkFailedWhenKafkaSendFails`; Maven completed successfully.

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

```powershell
git diff --name-only
```

Result at the end of this fix pass listed:

- `docs/harness/state/current-state.md`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/TaskControllerMappingTest.java`

`docs/harness/state/current-state.md` was a pre-existing unrelated dirty file and was not modified by this fix pass. `LegacyTaskApiContractFreezeTest.java` and this fix handoff are untracked additions, so they appear in `git status --short` rather than `git diff --name-only`.

## Blockers And Residual Risk

Blockers: none.

Residual risk:

- The tests intentionally remain reflection/source-level contract guards. A later approved phase that changes endpoint declaration style or permission style while preserving behavior must deliberately update the Phase 006 inventory and tests.
- Existing unrelated dirty/untracked harness files remain outside this Window 2 fix claim.

## Re-Review Required

Yes. Window 3 should re-review this Fix Pass and write `docs/harness/handoffs/phase-006-review-fix-1.md`.
