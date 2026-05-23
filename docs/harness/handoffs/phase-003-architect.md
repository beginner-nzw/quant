# Phase 003 Architect Handoff

## Status

Phase: Phase 003 - Contract Hardening for Workbench and Fallback.

Window: Window 1 - Phase Architect.

Status: ready for human approval before Window 2. This handoff does not authorize implementation by itself.

## Inputs Read

- `docs/harness/00-project-charter.md`
- `docs/harness/01-current-architecture.md`
- `docs/harness/02-authority-matrix.md`
- `docs/harness/03-host-ownership.md`
- `docs/harness/04-contract-map.md`
- `docs/harness/05-transition-lifetime.md`
- `docs/harness/06-debt-register.md`
- `docs/harness/07-phase-backlog.md`
- `docs/harness/08-eval-checklist.md`
- `docs/harness/09-window-protocol.md`
- `docs/harness/10-steering-state-machine.md`
- `docs/harness/state/current-state.md`
- `docs/harness/handoffs/steering-decision-phase-003.md`
- `docs/harness/handoffs/phase-002-architect.md`
- `docs/harness/handoffs/phase-002-review-fix-1.md`
- `docs/harness/handoffs/phase-002-final.md`
- Current workbench, query service, projection and boundary test code under `ai-orchestration-service`.

Phase 003 is not Phase 001, so the Phase 001 special code reading list is not the primary scope for this handoff.

## 1. Phase Goal

Harden the contract around `research-workbench` and existing fallback/preferred-field hydration so they cannot drift into sources of truth.

The phase must make the current architecture harder to misunderstand after Phase 001 and Phase 002:

- `GET /api/tasks/research-workbench` remains display-only aggregation.
- Workbench output must not define task status, report truth, risk warning truth, strategy signal truth, market event truth, audit truth or config truth.
- Backend command and projection paths must not consume workbench output as authority.
- Existing fallback/preferred-field behavior remains display hydration only.
- Feasible fallback observability checks may be added for existing Java-side metadata, but Python fallback cleanup is deferred to Phase 004 unless separately approved.

This is a backend-only contract hardening phase. It is not a feature phase, not a refactor phase and not a Python cleanup phase.

## Decision Order

Window 2 must evaluate every edit in this order:

```text
belongs -> authority -> contract -> behavior
```

Passing tests is not enough if a change lets workbench or fallback hydration become authority.

## 2. Allowed File Scope

Window 2 may modify these production files only for comments or Javadoc-style contract notes. No executable production logic may change in these files:

- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/ResearchWorkbenchQueryService.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/ResearchWorkbenchQueryServiceImpl.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/RiskQueryServiceImpl.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/StrategyQueryServiceImpl.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/ReportQueryServiceImpl.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/TaskQueryServiceImpl.java`, only if a fallback/display hydration contract comment is needed near the existing task full-detail display hydration.
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/AiResultDomainProjectionServiceImpl.java`, only if a contract comment is needed to state that projection must consume AI result payload and authoritative domain tables, not workbench output.

Window 2 may modify or add backend tests under:

- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/`

Preferred test targets:

- Extend `QueryServiceBoundaryTests.java`.
- Add a focused contract/boundary test class if it is clearer than growing the existing file.
- Extend existing workbench/fallback behavior tests only when the new assertions stay focused on contract hardening:
  - `TaskQueryServiceRiskWarningTests.java`
  - `TaskQueryServiceRiskProjectionTests.java`
  - `AiResultDomainProjectionServiceTests.java`
  - `AiTaskResultConsumerTests.java`

Window 2 must write:

- `docs/harness/handoffs/phase-003-implementation.md`

## 3. Forbidden File Scope

Window 2 must not modify:

- `quant-ui/**`
- `quant-ai-platform/quant-ai-engine/**`
- `quant-ai-platform/quant-services/quant-business/research-task-service/**`
- `quant-ai-platform/quant-services/quant-common/**`
- database migration or schema files
- `quant-ai-platform/ai-config/**`
- Maven POM files
- Docker, deployment, gateway, auth-service, Nacos, Sentinel dashboard or service discovery files
- controller code, including mappings, permissions, request binding and response envelopes
- DTO, VO, entity or mapper classes
- Kafka producers, consumers, topic constants or message DTOs
- command service implementation logic
- read-model query implementation logic, except comments in the explicitly allowed files

If Window 2 finds that executable production code must change to satisfy Phase 003, it must stop and record a blocker.

## 4. Stable URL / API / Behavior Contracts

All external contracts from Phase 001 and Phase 002 remain stable.

Stable workbench endpoint:

- `GET /api/tasks/research-workbench`

Workbench stability requirements:

- Same URL path.
- Same HTTP method.
- Same request binding through `ResearchWorkbenchQueryDTO`.
- Same `Result.success(...)` envelope.
- Same `ResearchWorkbenchVO` response shape.
- Same null, empty, pagination-limit and sorting behavior.
- Same display fallback/preferred-field precedence.

Stable authoritative/read-model endpoints that workbench must not replace:

- `GET /api/tasks/{taskId}`
- `GET /api/tasks/{taskId}/state`
- `GET /api/tasks/{taskId}/workflow`
- `GET /api/tasks/{taskId}/agents`
- `GET /api/tasks/{taskId}/audits`
- `GET /api/tasks/{taskId}/report`
- `GET /api/tasks/{taskId}/report/versions`
- `GET /api/tasks/risk-warnings`
- `GET /api/tasks/risk-warning-stats`
- `GET /api/tasks/strategy-signals`
- `GET /api/tasks/strategy-signal-stats`
- `GET /api/tasks/market-events`
- `GET /api/tasks/market-intelligence`
- `GET /api/tasks/audit-compliance`
- `GET /api/tasks/model-agent-config`

Stable command endpoints that must not consume workbench output as authority:

- `POST /api/tasks/{taskId}/retry`
- `POST /api/tasks/{taskId}/cancel`
- `POST /api/tasks/{taskId}/report/review`
- `POST /api/tasks/strategy-signals`
- `POST /api/tasks/strategy-signals/{signalId}/status`
- `POST /api/tasks/market-events`
- `POST /api/tasks/market-events/batch-import`
- `POST /api/tasks/market-events/mock-ingest`
- `POST /api/tasks/market-events/source-sync/{sourceCode}`
- `POST /api/tasks/model-agent-config/*`

Stable backend behavior:

- No new database writes in query services.
- No change to Redis cache keys or TTLs.
- No change to Kafka topics, message fields or message interpretation.
- No change to report/risk/strategy/market/audit projection behavior.
- No change to report review, retry, cancel, strategy signal, market event or config commands.
- No change to frontend-visible data fields.

## 5. Contract Boundaries To Harden

### Workbench Boundary

`ResearchWorkbenchQueryService` is a display aggregation surface only.

It may read task, report, risk, strategy and market data to compose:

- latest insight display
- recent task display
- disposition summary display

It must not:

- create, update or delete domain facts
- decide final task status
- decide final report truth
- decide risk warning truth
- decide strategy signal truth
- decide market event truth
- decide audit truth
- expose copied domain list/read-model entrypoints
- become an input to backend command services or AI result projection

### Fallback / Preferred-Field Boundary

Existing fallback/preferred-field logic is allowed only as display hydration.

Examples of allowed existing behavior:

- revised report text may be preferred over original report text for display.
- domain risk/strategy records may be preferred for workbench display, with legacy report-derived values only as display fallback.
- empty display sections may be hydrated from report meta for visual completeness.

It must not:

- define domain truth.
- become hidden routing logic for commands.
- create new fallback values, precedence rules or data sources.
- hide whether fallback data originated from report/display hydration.
- overwrite authoritative domain records.

Fallback reason propagation in Python remains Phase 004 unless Phase 003 can assert existing Java-side metadata without modifying Python, Kafka contracts, DTOs or VO shapes.

## 6. Allowed New Class / Method Types

Allowed in tests:

- A new package-private test class under `src/test/java/com/quant/aiorchestrationservice/`.
- New JUnit test methods in existing boundary or behavior tests.
- Private test helper methods that read Java source files and assert forbidden imports, injections, method names or write operations.
- Private test constants for source roots and allowlists.
- Focused test fixtures that exercise existing workbench/fallback behavior without changing production code.

Allowed in production:

- Comments or Javadoc-style notes in the explicitly allowed files.

Not allowed in production:

- New production classes.
- New production methods.
- New fields.
- New annotations that affect runtime behavior.
- New dependencies.
- New runtime contract marker objects.

## 7. Forbidden Helpers / Adapters / Fallbacks / Bridges

Do not add:

- Any new `*Helper`, `*Adapter`, `*Bridge`, `*Facade`, `*Fallback`, router, dispatcher or service locator.
- A shared display-hydration utility.
- A generic query boundary checker in production code.
- A service that wraps or delegates to `ResearchWorkbenchQueryService`.
- A command path that calls `ResearchWorkbenchQueryService`.
- A projection path that calls `ResearchWorkbenchQueryService`.
- A new endpoint, endpoint alias or compatibility route.
- A new DTO/VO/entity/mapper field.
- A new fallback source, fallback reason field, fallback precedence rule or hidden merge rule.
- A new source of truth for task, report, risk, strategy, market, audit or config semantics.

Existing fallback/preferred-field behavior may be documented and tested as-is. It must not be expanded.

## 8. Acceptance Conditions

Phase 003 is acceptable only if all conditions hold:

- `research-workbench` remains a display-only aggregation.
- No backend command service imports, injects or calls `ResearchWorkbenchQueryService`.
- No backend command service depends on `ResearchWorkbenchVO` or workbench DTO/VO classes.
- `AiResultDomainProjectionServiceImpl` does not import, inject or call workbench services or workbench VO/DTO classes.
- `ResearchWorkbenchQueryServiceImpl` contains no database writes, no Redis writes and no Kafka publishes.
- `ResearchWorkbenchQueryServiceImpl` does not regain copied domain read-model entrypoints removed in Phase 002.
- Existing fallback/preferred-field behavior is documented or tested as display hydration only.
- No new fallback source, precedence rule, response field or message field is introduced.
- No URL path, HTTP method, request binding, permission or response envelope changes.
- No DTO, VO, entity, mapper, database schema, Kafka contract, frontend or Python changes.
- Any added fallback metadata test only asserts existing Java-side visibility and defers Python fallback cleanup to Phase 004 if new metadata would be required.
- At least one focused boundary test fails if workbench becomes a command/projection source.
- At least one focused boundary test or source assertion fails if workbench starts writing domain facts.
- `mvn -q test` passes from `quant-ai-platform/quant-services`.

## 9. Required Verification Commands

Window 2 must run this command from `D:\projects\bussiness\quant-ai-platform\quant-services`:

```powershell
mvn -q test
```

Window 2 must also run or manually inspect these boundary checks from `D:\projects\bussiness`.

Check that workbench references stay confined to the controller, query service, VO/DTO/interface and tests:

```powershell
rg -n "ResearchWorkbench|research-workbench|getResearchWorkbench" quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator
```

Expected result: no command service or projection service references workbench. Allowed production matches are the workbench controller, workbench query service/interface and workbench DTO/VO classes.

Check that workbench does not perform writes:

```powershell
rg -n "\.(insert|update|updateById|delete|deleteById)\(" quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/ResearchWorkbenchQueryServiceImpl.java
```

Expected result: no matches.

Check that the Phase 002 removed domain read-model entrypoints stay out of workbench:

```powershell
rg -n "listRiskWarningRecords|listStrategySignalRecords|listReportCenterRecords|listMarketIntelligenceRecords|RiskWarningListItemVO|StrategySignalListItemVO|ReportCenterListItemVO|MarketIntelligenceListItemVO" quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/ResearchWorkbenchQueryServiceImpl.java
```

Expected result: no matches.

Check that changed files stay within the approved scope:

```powershell
git diff --name-only
```

Expected result: only allowed Phase 003 documentation, tests and comment-only production files should appear as Window 2 changes. Existing unrelated dirty worktree files must not be reverted or bundled into the Phase 003 implementation claim.

Window 2 must include exact command outcomes in `phase-003-implementation.md`.

Frontend and Python verification commands are not required because frontend and Python are forbidden in this phase.

## 10. Blocker Stop Rules

Window 2 must stop and write a blocker note instead of expanding scope if it discovers any of these:

- Preserving the approved contract requires changing executable production code.
- A URL path, HTTP method, request binding, permission, response envelope, DTO, VO, entity, mapper, database schema or Kafka contract must change.
- A Python or frontend change is needed to prove fallback visibility.
- A new fallback field, fallback source or fallback precedence rule seems necessary.
- Workbench must be consumed by a command, projection or AI workflow path to satisfy a test.
- A generic helper, bridge, adapter, facade, fallback service or shared hydration utility appears necessary.
- Current behavior is internally inconsistent and cannot be documented or tested without changing behavior.
- Added tests would require brittle assertions against unrelated formatting instead of real boundary rules.

When blocked, Window 2 must:

1. Stop business-code edits at the smallest reversible point.
2. Record the blocker in `docs/harness/handoffs/phase-003-implementation.md`.
3. State which acceptance condition cannot be met.
4. State the exact file or contract that forced the blocker.
5. Ask for human decision instead of choosing Phase 004 or any new fallback work.

## Window 2 Shape

Use one backend Window 2 implementation window.

Do not partition into frontend or Python implementers. Do not start Phase 004. Do not proceed until the user approves this Phase 003 architect handoff.
