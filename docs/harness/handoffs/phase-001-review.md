# Phase 001 Review Handoff

## Status

Decision: approve.

Window 3 review completed for Phase 001. The implementation may proceed to Window 4 handoff.

This approval is for the Phase 001 controller-surface split only. It does not select the next phase and does not approve any new implementation work.

## Review Scope

Review order used:

```text
belongs -> authority -> contract -> behavior
```

Inputs read:

- `docs/harness/00-project-charter.md`
- `docs/harness/02-authority-matrix.md`
- `docs/harness/03-host-ownership.md`
- `docs/harness/04-contract-map.md`
- `docs/harness/05-transition-lifetime.md`
- `docs/harness/06-debt-register.md`
- `docs/harness/08-eval-checklist.md`
- `docs/harness/09-window-protocol.md`
- `docs/harness/state/current-state.md`
- `docs/harness/handoffs/steering-decision-phase-001.md`
- `docs/harness/handoffs/phase-001-architect.md`
- `docs/harness/handoffs/phase-001-implementation.md`
- Current git diff and the touched controller/test files.

## Findings

No blocking or required-fix findings.

Non-blocking workspace note:

- P3: `.gitignore` contains a harness tracking exception at `.gitignore:7`, while Phase 001 implementation file scope is limited in `docs/harness/handoffs/phase-001-architect.md:89`. I did not classify this as a Phase 001 implementation blocker because it does not change application belongs, authority, contract or behavior, and it appears related to making `docs/harness/**/*.md` visible despite the root `*.md` ignore at `.gitignore:2`. Window 4 should decide how to carry this workspace metadata forward.

## Belongs Review

Passed.

Evidence:

- `TaskQueryController` now contains only task read-model/runtime/retry/cancel mappings under `/api/tasks`: `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/TaskQueryController.java:34`, `:43`, `:73`, `:83`, `:95`, `:116`.
- Moved surfaces live in new controller classes inside the approved `ai-orchestration-service` controller package:
  - market: `MarketEventController.java:36`
  - risk: `RiskWarningController.java:14`
  - strategy: `StrategySignalController.java:24`
  - report: `ReportController.java:28`
  - market intelligence: `MarketIntelligenceController.java:14`
  - audit: `AuditComplianceController.java:15`
  - model/agent config: `ModelAgentConfigController.java:32`
  - workbench: `ResearchWorkbenchController.java:13`
- No service, mapper, entity, DTO, VO, frontend, Python, config JSON, Maven, Docker or deployment file appeared in the implementation diff reviewed by Window 3.

## Authority Review

Passed.

Evidence:

- `research-workbench` remains a display aggregation in the authority matrix: `docs/harness/02-authority-matrix.md:29`, `docs/harness/02-authority-matrix.md:35`.
- The workbench controller only delegates to the existing query service and does not introduce a command or new source of truth: `ResearchWorkbenchController.java:21`.
- Report/risk/strategy/market/config/audit endpoints remain transition-hosted by `ai-orchestration-service`, consistent with `docs/harness/03-host-ownership.md:32` and `docs/harness/05-transition-lifetime.md:5`.
- No frontend files were changed, so no frontend truth inference was introduced.

## Contract Review

Passed.

Evidence:

- The added mapping inventory test asserts every approved endpoint and target controller from the Window 1 handoff: `TaskControllerMappingTest.java:27` through `TaskControllerMappingTest.java:97`.
- Request mappings remain under the existing `/api/tasks` namespace in each split controller, for example `TaskQueryController.java:34`, `MarketEventController.java:36`, `ReportController.java:28`, and `ModelAgentConfigController.java:32`.
- Permission checks were preserved on command/config/review surfaces, for example `MarketEventController.java:73`, `StrategySignalController.java:44`, `ReportController.java:77`, `AuditComplianceController.java:24`, and `ModelAgentConfigController.java:59`.
- Sentinel annotations remain on `pageTasks` and `getTaskFullDetail`: `TaskQueryController.java:73` and `TaskQueryController.java:95`.

## Behavior Review

Passed.

Verification run by Window 3:

- From `D:\projects\bussiness\quant-ai-platform\quant-services`: `mvn -q test`
  - Result: passed with exit code 0.
  - Note: Maven output includes the existing intentional `kafka down` stack trace from `TaskOutboxPublisherServiceTests.publishPendingOnceShouldMarkFailedWhenKafkaSendFails`; the test run still passed.
- From `D:\projects\bussiness`: `rg -n "@(RequestMapping|GetMapping|PostMapping)" quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller`
  - Result: mapping scan matched the Phase 001 inventory and the added mapping test coverage.
- From `D:\projects\bussiness`: `git diff --check`
  - Result: no whitespace errors; only existing CRLF normalization warnings.

## Window 1 Acceptance

Satisfied:

- `TaskQueryController` no longer owns market, risk, strategy, report, market-intelligence, audit, config or workbench methods.
- Each moved endpoint exists in exactly one approved target controller.
- No endpoint path, HTTP method, request DTO, path variable, request parameter, response envelope or permission check change was found.
- No new endpoint, fallback, adapter, bridge, DTO alias or controller facade was found.
- No frontend or non-controller business implementation file was changed.
- Required Maven test and mapping scan verification passed.
- Window 2 wrote `docs/harness/handoffs/phase-001-implementation.md`.

## Window 4 Gate

Allowed to enter Window 4: yes.

Window 4 should freeze Phase 001, record the non-blocking workspace metadata note if relevant, and update harness state files according to the Window 4 protocol.
