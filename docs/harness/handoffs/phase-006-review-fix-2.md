# Phase 006 Review Fix 2 Handoff

## Status

Window: Window 3 - Review / Eval.

Phase: Phase 006 - Legacy `/api/tasks/*` Contract Freeze for Non-Task Domains.

Review mode: Re-review fix 2.

Decision: require fixes.

Window 4 allowed: no.

Window 2 must perform Fix Pass 3 and write `docs/harness/handoffs/phase-006-fix-3-implementation.md`.

## Startup Recovery

Latest unfinished phase selected automatically:

- `docs/harness/handoffs/phase-006-implementation.md` exists.
- `docs/harness/handoffs/phase-006-review.md` exists and required fixes.
- `docs/harness/handoffs/phase-006-fix-1-implementation.md` exists.
- `docs/harness/handoffs/phase-006-review-fix-1.md` exists and required fixes.
- `docs/harness/handoffs/phase-006-fix-2-implementation.md` exists.
- `docs/harness/handoffs/phase-006-review-fix-2.md` did not exist before this review.
- `docs/harness/handoffs/phase-006-final.md` does not exist.

Handoffs read for this review:

- `docs/harness/handoffs/steering-decision-phase-006.md`
- `docs/harness/handoffs/phase-006-architect.md`
- `docs/harness/handoffs/phase-006-implementation.md`
- `docs/harness/handoffs/phase-006-review.md`
- `docs/harness/handoffs/phase-006-fix-1-implementation.md`
- `docs/harness/handoffs/phase-006-review-fix-1.md`
- `docs/harness/handoffs/phase-006-fix-2-implementation.md`

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

### Closed - Initial review High: Contract freeze did not reject non-GET/POST endpoint annotations in approved controllers

Fix Pass 1 extended both mapping guards to cover `@PutMapping`, `@DeleteMapping`, `@PatchMapping` and method-level `@RequestMapping`, including `ANY` when no method is specified.

Evidence:

- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java:376`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java:395`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java:400`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java:405`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/TaskControllerMappingTest.java:121`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/TaskControllerMappingTest.java:125`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/TaskControllerMappingTest.java:129`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/TaskControllerMappingTest.java:133`

### Closed - Initial review Medium: Request parameter required/default drift was not frozen

Fix Pass 1 captures `RequestParam.required()` and normalized `RequestParam.defaultValue()` in the binding comparison.

Evidence:

- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java:434`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java:437`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java:662`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java:688`

### Closed for the cited shape - Review Fix 1 Medium: Full method-level `/api/tasks/*` mappings in a new controller could bypass inventory

Fix Pass 2 added `allApiTasksEndpointMappingsRemainInApprovedInventory()`, discovers every `*Controller.java` source file, loads the controller classes, reflects over methods and compares mappings under `/api/tasks` against the approved task plus non-task inventory. This closes the specific bypass from `@GetMapping("/api/tasks/report-export")` in a controller without a class-level `/api/tasks` base.

Evidence:

- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java:297`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java:299`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java:302`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java:309`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java:502`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java:525`

## Findings

### Medium - `/api/tasks/*` inventory scan still misses valid relative path combinations

The new all-controller scan relies on `joinPath(basePath, methodPath)` to decide whether a reflected mapping is under `/api/tasks`. That helper concatenates strings directly. A new controller can declare a valid Spring mapping as class-level `@RequestMapping("/api")` plus method-level `@GetMapping("tasks/report-export")`; Spring treats that as `/api/tasks/report-export`, but this test records `/apitasks/report-export`. `isApiTasksPath(...)` then excludes it, so the endpoint bypasses the Phase 006 inventory.

Evidence:

- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java:297`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java:302`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java:590`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java:595`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java:602`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/TaskControllerMappingTest.java:171`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/TaskControllerMappingTest.java:176`

Why this requires fixes:

- Window 1 acceptance requires focused backend tests or source assertions to fail if a new non-task legacy endpoint appears under `/api/tasks/*` without updating the Phase 006 contract inventory.
- The current scan sees the exact no-base full method path shape from the last review, but it still does not model ordinary Spring path combination semantics for relative method paths.
- This is still test-only contract coverage risk. No production behavior currently changed.

Required fix:

- Normalize test-local path joining in the mapping guards so a base path and method path are combined with exactly one slash, including base `/api` plus method `tasks/...`, base `/api/` plus method `/tasks/...`, no base plus full method path, and existing `/api/tasks` plus `/...` shapes.
- Keep the fix limited to backend tests. Do not add production helpers, routes, aliases, DTO/VO/entity changes, frontend changes or Python changes.

### Medium - Permission guard can miss newly added explicit permission checks

`assertPermissionCall(...)` only records calls matching `requirePermission(RoleAccessConfigService.PERMISSION_...)`. If a later edit adds an explicit permission check using a string literal, local variable, static import or other expression to an endpoint that currently has no explicit permission check, `actualPermissions` remains empty and the "absence of explicit permission checks" assertion still passes.

Evidence:

- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java:272`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java:453`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java:455`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java:457`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java:464`

Why this requires fixes:

- Window 1 acceptance requires focused backend tests or source assertions to fail if explicit permission calls are added, removed or changed contrary to the inventory.
- Existing absence of explicit permission checks is part of the behavior contract because adding a check changes endpoint access behavior.
- This is a contract guard gap only; the production controllers currently still match the Phase 006 inventory.

Required fix:

- Make the source assertion first detect any `requirePermission(...)` call in the method body.
- For endpoints with no explicit permission, fail on any such call regardless of argument style.
- For endpoints with an expected explicit permission, assert exactly one call and assert that the argument still references the expected permission constant.
- Keep the fix test-only.

## Belongs Review

Result: pass for code placement; fixes still required for contract coverage.

Fix Pass 2 changed the Phase 006 backend test file and added its handoff. No executable Java production code, frontend, Python, DTO, VO, entity, mapper, Kafka, database, `ai-config`, dependency or build file change was found in the Fix Pass 2 claim.

`docs/harness/state/current-state.md` and `TaskControllerMappingTest.java` remain dirty in the working tree relative to HEAD. The handoffs identify `current-state.md` as pre-existing unrelated state and `TaskControllerMappingTest.java` as prior Fix Pass state. I did not revert or stage them.

## Authority Review

Result: pass.

No source of truth, read-model command source, frontend truth inference, Python fallback path, workbench authority promotion or market-intelligence authority promotion was introduced. The implementation remains test-only.

## Contract Review

Result: fail until the findings above are fixed.

The previous full method-level `/api/tasks/...` bypass is closed for the exact cited shape, but the all-controller endpoint inventory guard still misses relative path combinations that Spring maps under `/api/tasks`. The permission assertion also does not fully freeze added explicit permission behavior.

Window 1 acceptance is therefore not fully satisfied.

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
- `git diff --name-only` reports `docs/harness/state/current-state.md` and `TaskControllerMappingTest.java`; committed Fix Pass 2 changes are visible through `git diff b73c7d1 --name-only`.

## Window 1 Acceptance

Not satisfied.

Satisfied:

- Initial review findings for non-GET/POST mapping annotation coverage and request parameter required/default coverage are closed.
- Review Fix 1 finding for no-base full method-level `/api/tasks/...` mappings in a new controller is closed for that exact shape.
- Existing inventoried endpoint URL, method, owner, response envelope, declared generic response type, path variable, request body, query object and direct constant-style permission assertions remain in place.
- No production behavior change was introduced.
- Maven tests passed.

Not satisfied:

- Tests/source assertions still do not fail for every possible new `/api/tasks/*` endpoint because relative base/method path combinations can be misjoined and excluded from the inventory scan.
- Permission source assertions still do not fail for every added explicit permission check contrary to the inventory.

## Final Decision

Decision: require fixes.

Blocker: none.

Window 4 allowed: no.

Next required step: return to Window 2 Fix Pass 3, limited to backend test-only changes that close the path-normalization and permission-call detection gaps above, then write `docs/harness/handoffs/phase-006-fix-3-implementation.md`.
