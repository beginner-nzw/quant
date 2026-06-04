# Phase 006 Review Fix 1 Handoff

## Status

Window: Window 3 - Review / Eval.

Phase: Phase 006 - Legacy `/api/tasks/*` Contract Freeze for Non-Task Domains.

Review mode: Re-review fix 1.

Decision: require fixes.

Window 4 allowed: no.

Window 2 must perform Fix Pass 2 and write `docs/harness/handoffs/phase-006-fix-2-implementation.md`.

## Startup Recovery

Latest unfinished phase selected automatically:

- `docs/harness/handoffs/phase-006-implementation.md` exists.
- `docs/harness/handoffs/phase-006-review.md` exists and required fixes.
- `docs/harness/handoffs/phase-006-fix-1-implementation.md` exists.
- `docs/harness/handoffs/phase-006-review-fix-1.md` did not exist before this review.
- `docs/harness/handoffs/phase-006-final.md` does not exist.

Handoffs read for this review:

- `docs/harness/handoffs/steering-decision-phase-006.md`
- `docs/harness/handoffs/phase-006-architect.md`
- `docs/harness/handoffs/phase-006-implementation.md`
- `docs/harness/handoffs/phase-006-review.md`
- `docs/harness/handoffs/phase-006-fix-1-implementation.md`

Required harness files read:

- `docs/harness/00-project-charter.md`
- `docs/harness/02-authority-matrix.md`
- `docs/harness/03-host-ownership.md`
- `docs/harness/04-contract-map.md`
- `docs/harness/05-transition-lifetime.md`
- `docs/harness/06-debt-register.md`
- `docs/harness/08-eval-checklist.md`
- `docs/harness/09-window-protocol.md`
- `docs/harness/state/current-state.md`

Related code reviewed:

- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/TaskControllerMappingTest.java`
- controller mapping and permission surfaces under `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller`

## Previous Findings Closure

### Closed - High: Contract freeze did not reject all unregistered legacy endpoints in approved controllers

Fix Pass 1 extended the mapping extraction in both contract guard tests to cover:

- `@GetMapping`
- `@PostMapping`
- `@PutMapping`
- `@DeleteMapping`
- `@PatchMapping`
- method-level `@RequestMapping(method = ...)`
- method-level `@RequestMapping` without an explicit method, recorded as `ANY`
- multiple mapping paths from `value` or `path`

Evidence:

- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java:359`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java:368`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java:393`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/TaskControllerMappingTest.java:121`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/TaskControllerMappingTest.java:133`

This closes the specific annotation blind spot identified in the initial review for the already approved controller inventory.

### Closed - Medium: Request parameter binding drift was not fully frozen

Fix Pass 1 now captures `RequestParam.required()` and normalized `RequestParam.defaultValue()` in the binding comparison, while the expected helper keeps the current contract as required with no default value.

Evidence:

- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java:419`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java:421`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java:588`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java:600`

## Findings

### Medium - New `/api/tasks/*` endpoints can still bypass the inventory if declared as full method-level paths in a new controller

`legacyNonTaskEndpointInventoryRejectsUnregisteredControllerMethods()` now sees non-GET/POST methods, but it only iterates the hard-coded `NON_TASK_LEGACY_CONTROLLERS` list. The owner guard then scans source for class-level `@RequestMapping("/api/tasks")`, but it does not discover a new controller that declares a full method-level mapping such as `@GetMapping("/api/tasks/report-export")` without a class-level `/api/tasks` base mapping.

Evidence:

- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java:284`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java:286`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java:297`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java:299`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/TaskControllerMappingTest.java:92`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/TaskControllerMappingTest.java:101`

The domain namespace alias guard does not close this gap because it searches for new `/api/reports`, `/api/market`, `/api/audit`, `/api/config`, etc. aliases, not unapproved `/api/tasks/*` owners or full-path method mappings.

Evidence:

- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java:321`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java:322`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java:324`

Why this requires fixes:

- Window 1 acceptance requires focused backend tests or source assertions to fail if a new non-task legacy endpoint appears under `/api/tasks/*` without updating the Phase 006 contract inventory.
- A new controller using a method-level full `/api/tasks/...` mapping would create exactly that endpoint but would not be included in the hard-coded controller reflection list and would not be detected by the class-level base mapping owner assertion.
- This is still test-only contract coverage risk; no production behavior is currently changed.

Required fix:

- Add a test-only source or reflection guard that discovers all controller endpoint mappings under `/api/tasks` across controller source files, including method-level full paths, and compares them to the approved Phase 006 inventory and owner set.
- Keep the fix limited to backend tests under the allowed Phase 006 test directory.
- Do not add production helpers, aliases, routes, bridges, controller behavior, DTO/VO/entity changes, frontend changes or Python changes.

## Belongs Review

Result: pass for code placement; fix still required for contract coverage.

Fix Pass 1 changed only backend test files plus its handoff. No executable Java production code, frontend, Python, DTO, VO, entity, mapper, Kafka, database, `ai-config`, dependency or build file change was found in the Fix Pass 1 claim.

`docs/harness/state/current-state.md` remains a tracked dirty file in `git diff --name-only`, but the implementation and fix handoffs identify it as pre-existing unrelated state. I did not revert or stage it.

## Authority Review

Result: pass.

No new source of truth, read-model command source, frontend truth inference, Python fallback path, workbench authority promotion or market-intelligence authority promotion was introduced. The implementation remains test-only.

## Contract Review

Result: fail until the finding above is fixed.

The two initial review findings are closed, but Window 1 acceptance is still not fully satisfied because the tests can miss an unapproved new `/api/tasks/*` endpoint declared as a full method-level mapping in a new controller.

## Behavior Review

Result: pass for runtime behavior; verification passed.

No runtime or user-visible behavior change was observed.

Verification run from `D:\projects\bussiness\quant-ai-platform\quant-services`:

```powershell
mvn -q test
```

Result: passed, exit code 0. Output still includes the existing simulated `kafka down` warning stack trace from `TaskOutboxPublisherServiceTests.publishPendingOnceShouldMarkFailedWhenKafkaSendFails`, but Maven completed successfully.

Verification run from `D:\projects\bussiness`:

```powershell
rg -n "@RequestMapping|@GetMapping|@PostMapping|@PutMapping|@DeleteMapping" quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller
rg -n "requirePermission" quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller
rg -n "/api/(reports|risk|risks|strategy|strategies|market|markets|audit|config|workbench)" quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller
git diff --name-only
```

Results:

- Mapping inspection matched the existing `/api/tasks` controller surfaces.
- Permission inspection matched the existing direct controller permission calls.
- Domain namespace alias search returned no matches.
- `git diff --name-only` reports `docs/harness/state/current-state.md` and `TaskControllerMappingTest.java`; untracked Phase 006 files remain visible through `git status --short`.

## Window 1 Acceptance

Not satisfied.

Satisfied:

- Initial review finding for non-GET/POST mapping annotation coverage in approved controllers is closed.
- Initial review finding for `@RequestParam.required` and `@RequestParam.defaultValue` coverage is closed.
- Existing inventoried endpoint URL, method, owner, response envelope, declared generic response type, path variable, request body, query object and direct permission assertions remain in place.
- No production behavior change was introduced.
- Maven tests passed.

Not satisfied:

- Tests/source assertions still do not fail for every possible new non-task legacy endpoint under `/api/tasks/*` because a new controller can declare a full method-level `/api/tasks/...` mapping without being discovered by the current inventory or approved-owner guard.

## Final Decision

Decision: require fixes.

Blocker: none.

Window 4 allowed: no.

Next required step: return to Window 2 Fix Pass 2, limited to test-only changes that close the remaining `/api/tasks/*` full-path owner/inventory detection gap, then write `docs/harness/handoffs/phase-006-fix-2-implementation.md`.
