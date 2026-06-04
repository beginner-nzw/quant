# Phase 006 Review Handoff

## Status

Window: Window 3 - Review / Eval.

Phase: Phase 006 - Legacy `/api/tasks/*` Contract Freeze for Non-Task Domains.

Review mode: Initial Review.

Decision: require fixes.

Window 4 allowed: no.

Window 2 must perform a Fix Pass and write `docs/harness/handoffs/phase-006-fix-1-implementation.md`.

## Startup Recovery

Latest unfinished phase selected automatically:

- `docs/harness/handoffs/phase-006-implementation.md` exists.
- `docs/harness/handoffs/phase-006-final.md` does not exist.
- `docs/harness/handoffs/phase-006-review.md` did not exist before this review.

Handoffs read for this review:

- `docs/harness/handoffs/steering-decision-phase-006.md`
- `docs/harness/handoffs/phase-006-architect.md`
- `docs/harness/handoffs/phase-006-implementation.md`

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

## Findings

### High - Contract freeze does not reject all unregistered legacy endpoints

`LegacyTaskApiContractFreezeTest` only imports and inspects `GetMapping` and `PostMapping` for the non-task legacy endpoint inventory. In `mappingFor(...)`, methods annotated with `@PutMapping`, `@DeleteMapping`, `@PatchMapping` or method-level `@RequestMapping(method = ...)` return `null` and are excluded from the actual inventory. Evidence:

- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java:66`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java:68`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java:270`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java:353`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java:357`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java:362`

The existing `TaskControllerMappingTest` has the same GET/POST-only extraction pattern, so it does not close this gap. Evidence:

- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/TaskControllerMappingTest.java:13`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/TaskControllerMappingTest.java:14`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/TaskControllerMappingTest.java:100`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/TaskControllerMappingTest.java:107`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/TaskControllerMappingTest.java:112`

Why this requires fixes:

- Window 1 acceptance requires focused backend tests or source assertions to fail if a new non-task legacy endpoint appears under `/api/tasks/*` without updating the Phase 006 contract inventory.
- A future `@PutMapping("/market-events/{eventId}")` or `@DeleteMapping("/risk-warnings/{id}")` inside an existing approved non-task `/api/tasks` controller would be a new legacy endpoint, but the current inventory tests would ignore it.
- This is a contract guardrail gap in the central purpose of Phase 006. It is not a production behavior change today, but the phase is not acceptable as a contract freeze until the guard sees all Spring mapping shapes that can define endpoints.

Required fix:

- Update the Phase 006 test guard so inventoried and unregistered endpoint detection covers every Spring mapping annotation that can create a controller endpoint in this codebase, including `@PutMapping`, `@DeleteMapping`, `@PatchMapping` if available, and method-level `@RequestMapping`.
- Keep the fix test-only. Do not add production helpers, aliases, routes or behavior.

### Medium - Request parameter binding drift is not fully frozen

`bindingsFor(...)` records the `@RequestParam` name and Java type, but it drops `RequestParam.required()` and `RequestParam.defaultValue()`. The expected binding helper always writes `required=true`, so changing an inventoried request parameter from required to optional, or adding a default value, would still compare equal. Evidence:

- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java:380`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java:382`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java:523`
- Current inventoried request params exist at `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/ReportController.java:58` and `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/ReportController.java:59`.

Why this requires fixes:

- Window 1 acceptance requires tests to fail if inventoried `@RequestParam` bindings drift where they are part of the current contract.
- Required/default value changes are externally visible request-binding behavior, so they should be part of the freeze.

Required fix:

- Include `@RequestParam.required()` and `@RequestParam.defaultValue()` in the binding assertion, or add an equivalent focused assertion for the inventoried request parameters.

## Belongs Review

Result: pass with residual test-scope fix required.

Observed Phase 006 implementation changes are test-only plus implementation handoff. No executable Java production code, frontend, Python, DTO, VO, entity, mapper, Kafka, database, `ai-config`, dependency or build file change was found in the Phase 006 implementation claim.

`git diff --name-only` currently reports only `docs/harness/state/current-state.md`, which the implementation handoff identifies as pre-existing unrelated dirty state. `git status --short` also shows the Phase 006 handoff/test files as untracked additions. I did not revert or stage unrelated files.

## Authority Review

Result: pass.

No new source of truth, read-model command source, frontend truth inference, Python fallback path or workbench authority promotion was introduced. The implementation adds tests around controller contracts and does not alter domain ownership.

## Contract Review

Result: fail until the findings above are fixed.

The new test class freezes the declared GET/POST inventory, response envelopes, declared generics, many bindings and direct permission calls. However, the unregistered endpoint guard does not see all endpoint-defining Spring annotations, and request parameter required/default behavior is not fully frozen.

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
- `git diff --name-only` reports only the pre-existing tracked dirty `docs/harness/state/current-state.md`; untracked Phase 006 files are visible through `git status --short`.

## Window 1 Acceptance

Not satisfied.

Satisfied:

- Legacy non-task `/api/tasks/*` URL paths and owners are documented in the new test inventory for existing GET/POST endpoints.
- Response envelope and declared generic response type are checked for the inventoried endpoints.
- Many path variable, request body, query-object and request parameter names/types are checked.
- Direct permission calls are checked for the inventoried endpoints.
- No production behavior change was introduced.
- Maven tests passed.

Not satisfied:

- Tests do not fail for all possible new non-task legacy endpoints under `/api/tasks/*` because non-GET/POST mapping annotations are ignored.
- Request parameter binding behavior is not fully frozen because `required` and `defaultValue` are not asserted.

## Final Decision

Decision: require fixes.

Blocker: none.

Window 4 allowed: no.

Next required step: return to Window 2 Fix Pass 1, limited to test-only changes that close the two contract guard gaps above and then write `docs/harness/handoffs/phase-006-fix-1-implementation.md`.
