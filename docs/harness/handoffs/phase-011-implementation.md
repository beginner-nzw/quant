# Phase 011 Implementation Handoff

## Status

Window: Window 2 - Implementer.

Phase: Phase 011 - Risk/Strategy Projection Ownership Boundary.

Mode: initial implementation. `docs/harness/handoffs/phase-011-implementation.md` did not exist at startup, and no Phase 011 review handoff existed.

Implementation shape: docs-only architecture/governance work, as required by `docs/harness/handoffs/phase-011-architect.md`.

## Git Baseline

Before modifying any file, Window 2 ran:

```powershell
git status --short --untracked-files=all
```

Pre-existing dirty or untracked files at Window 2 start:

- `M docs/harness/state/current-state.md`
- `?? docs/harness/handoffs/phase-003-review.md`
- `?? docs/harness/handoffs/phase-004-architect.md`
- `?? docs/harness/handoffs/phase-004-review.md`
- `?? docs/harness/handoffs/phase-005-architect.md`
- `?? docs/harness/handoffs/phase-005-review.md`
- `?? docs/harness/handoffs/phase-006-architect.md`
- `?? docs/harness/handoffs/phase-006-fix-1-implementation.md`
- `?? docs/harness/handoffs/phase-006-implementation.md`
- `?? docs/harness/handoffs/phase-006-review-fix-1.md`
- `?? docs/harness/handoffs/phase-006-review-fix-2.md`
- `?? docs/harness/handoffs/phase-006-review-fix-3.md`
- `?? docs/harness/handoffs/phase-006-review.md`
- `?? docs/harness/handoffs/phase-007-architect.md`
- `?? docs/harness/handoffs/phase-007-review.md`
- `?? docs/harness/handoffs/phase-008-architect.md`
- `?? docs/harness/handoffs/phase-008-review.md`
- `?? docs/harness/handoffs/phase-009-architect.md`
- `?? docs/harness/handoffs/phase-009-review.md`
- `?? docs/harness/handoffs/phase-010-architect.md`
- `?? docs/harness/handoffs/phase-010-review.md`
- `?? docs/harness/handoffs/phase-011-architect.md`
- `?? docs/harness/handoffs/steering-decision-phase-004.md`
- `?? docs/harness/handoffs/steering-decision-phase-005.md`
- `?? docs/harness/handoffs/steering-decision-phase-006.md`
- `?? docs/harness/handoffs/steering-decision-phase-007.md`
- `?? docs/harness/handoffs/steering-decision-phase-008.md`
- `?? docs/harness/handoffs/steering-decision-phase-009.md`
- `?? docs/harness/handoffs/steering-decision-phase-010.md`
- `?? docs/harness/handoffs/steering-decision-phase-011.md`

These pre-existing files were treated as outside this Window 2 change claim. The only Window 2 files created are listed below.

## Files Changed

Window 2 created:

- `docs/harness/15-risk-strategy-projection-boundary-readiness.md`
- `docs/harness/handoffs/phase-011-implementation.md`

No Java, Python, frontend, database, Redis, Kafka, `ai-config`, dependency, build, deployment, test, state, debt, backlog, transition lifetime or prior handoff file was modified.

## Implementation Summary

Created `docs/harness/15-risk-strategy-projection-boundary-readiness.md` as the durable Phase 011 risk/strategy projection ownership readiness artifact.

The artifact documents:

- risk/strategy belongs analysis inside `ai-orchestration-service`
- current risk authority objects: `risk_warning` and `risk_warning_detail`
- current strategy authority objects: `strategy_signal` and `strategy_signal_factor`
- risk read-model surfaces under the frozen legacy /api/tasks namespace
- strategy read-model, factor query, manual create/update command and status command surfaces under the frozen legacy /api/tasks namespace
- `AiResultDomainProjectionService` as the current shared projection dependency, documented only and not split, moved or redesigned
- `TaskDomainEventPublisherService`, `risk.warning.generated` and `strategy.signal.generated` as current generated-event publication dependencies, not replacement SoT
- report, workbench, market, dashboard, frontend and Python context boundaries
- inherited guardrails from Phase 004, Phase 005, Phase 006, Phase 007, Phase 008, Phase 009 and Phase 010
- blockers and readiness gate checklists before any future service extraction, projection split, route migration, Kafka downstream redesign, gateway/auth change, config-store migration, frontend reshaping, Python behavior change, database schema change, permanent modular decision or new feature work

## Architect Acceptance Completed

Acceptance from `phase-011-architect.md`:

- `docs/harness/15-risk-strategy-projection-boundary-readiness.md` exists as the primary durable risk/strategy projection ownership readiness artifact.
- The artifact covers risk warning facts, risk warning details, risk read models, strategy signal facts, strategy signal factors, strategy read models, strategy manual create/update command, strategy status command, AI result projection, generated domain-event publication, frontend consumers and Python context consumers.
- The artifact names `risk_warning`, `risk_warning_detail`, `strategy_signal` and `strategy_signal_factor` as current authority objects.
- The artifact states that report risk points, report highlights, workbench summaries, market intelligence rows, Python risk/strategy context, fallback provenance, generated Kafka messages, frontend local state and dashboard cards are not risk/strategy SoT unless selected data is persisted through existing approved projection or command paths.
- The artifact treats `AiResultDomainProjectionService` as a current shared projection dependency only.
- The artifact treats `TaskDomainEventPublisherService`, `risk.warning.generated` and `strategy.signal.generated` as current generated-event dependencies only.
- The artifact preserves all risk/strategy URLs, HTTP methods, bindings, response envelopes, response types, permission behavior, frontend routes, frontend API functions and TypeScript shapes.
- The artifact preserves Phase 005, Phase 006, Phase 007, Phase 008, Phase 009 and Phase 010 constraints.
- The artifact does not choose risk-service extraction, strategy-service extraction, projection splitting, route migration, route aliases, endpoint deletion/rename/consolidation, gateway/auth, config-store migration, Python behavior change, frontend reshaping, Kafka/database changes, permanent modular-monolith architecture or new feature work.
- The artifact defines risk/strategy-specific readiness gates for future extraction, route migration, projection split, Kafka downstream change and permanence decisions.

## Contracts Kept Stable

Stable backend contracts preserved by documentation only:

- `GET /api/tasks/risk-warnings`
- `GET /api/tasks/risk-warning-stats`
- `GET /api/tasks/strategy-signals`
- `GET /api/tasks/strategy-signal-stats`
- `GET /api/tasks/strategy-signals/{signalId}/factors`
- `POST /api/tasks/strategy-signals`
- `POST /api/tasks/strategy-signals/{signalId}/status`

Stable frontend contracts preserved:

- `/risk-warnings`
- `/signals`
- `/dashboard`
- `/research-workbench`
- `fetchRiskWarnings`
- `fetchRiskWarningStats`
- `fetchStrategySignals`
- `fetchStrategySignalStats`
- `createStrategySignal`
- `fetchStrategySignalFactors`
- `updateStrategySignalStatus`

Stable Kafka/Python contracts preserved:

- `ai.task.result`
- `risk.warning.generated`
- `strategy.signal.generated`
- Python backend client paths for `list_risk_warnings` and `list_strategy_signals`
- Python fallback/provenance metadata remains non-authoritative context

## Behavior Changes

None.

Phase 011 changed documentation only. It did not change business code, runtime code, tests, URL paths, permissions, request/response shapes, frontend routes, TypeScript types, Python behavior, Redis behavior, Kafka topic/payload behavior, database schema, config files or generated-event behavior.

## Verification Results

Read-only inventory checks run from `D:\projects\bussiness`:

- Controller mapping/permission inventory for `RiskWarningController.java` and `StrategySignalController.java`: passed.
- Phase 006 contract-freeze references for risk/strategy controllers, paths, response types, DTOs and `PERMISSION_REPORT_REVIEW`: passed.
- Service/projection/publisher inventory for `pageRiskWarnings`, `getRiskWarningStats`, `pageStrategySignals`, `getStrategySignalStats`, `listStrategySignalFactors`, `createOrUpdate`, `updateStatus`, `saveRiskWarning`, `saveStrategySignal`, `publishRiskWarningGenerated`, `publishStrategySignalGenerated`, `risk_warning` and `strategy_signal`: passed.
- Kafka topic constant inventory for `RISK_WARNING_GENERATED`, `STRATEGY_SIGNAL_GENERATED`, `risk.warning.generated` and `strategy.signal.generated`: passed.
- Frontend route/API/type/view inventory for risk/strategy consumers: passed.
- Python backend client, market context and fallback/provenance inventory for risk/strategy context: passed.

Static frontend guard run from `D:\projects\bussiness\quant-ui`:

```powershell
node scripts/authority-boundary-check.mjs
```

Result: passed with `authority-boundary-check passed`.

Final docs verification commands run after this handoff was written:

```powershell
git diff --name-only
Test-Path docs/harness/15-risk-strategy-projection-boundary-readiness.md
rg -n "risk_warning|risk_warning_detail|strategy_signal|strategy_signal_factor|RiskWarningController|StrategySignalController|RiskQueryService|StrategyQueryService|StrategySignalService|AiResultDomainProjectionService|TaskDomainEventPublisherService|risk.warning.generated|strategy.signal.generated|frontend|Python|fallback|readiness gate|legacy /api/tasks|Phase 005|Phase 006|Phase 007|Phase 008|Phase 009|Phase 010" docs/harness/15-risk-strategy-projection-boundary-readiness.md docs/harness/handoffs/phase-011-implementation.md
rg -n "service extraction|projection split|route migration|route alias|breaking change|gateway/auth|config-store|database schema|Redis|Kafka|frontend reshaping|Python behavior|business code|new feature|permanent modular" docs/harness/15-risk-strategy-projection-boundary-readiness.md docs/harness/handoffs/phase-011-implementation.md
```

Results:

- `git diff --name-only`: showed only `docs/harness/state/current-state.md`, which was a pre-existing dirty file recorded in the startup baseline. The two Phase 011 files were still untracked at this point, so they did not appear in `git diff`; they are the only Window 2 change claim and will be staged explicitly.
- `Test-Path docs/harness/15-risk-strategy-projection-boundary-readiness.md`: `True`.
- Required coverage `rg`: passed with matches across the readiness artifact and implementation handoff for authority objects, controllers, services, projection dependency, generated topics, frontend, Python, fallback, readiness gate and inherited Phase 005 through Phase 010 guardrails.
- Out-of-scope `rg`: passed. Matches appear in no-change, prohibited, blocker, deferred, prerequisite, readiness or residual-risk sections, not as completed implementation claims.

Maven, npm build and Python runtime verification were not required because Phase 011 forbids Java, frontend, Python and test-code changes. No such files were touched.

## Blockers And Residual Risk

Blockers: none.

Residual risk:

- Phase 011 is documentation-only and therefore does not add new executable guards. It relies on existing Phase 006 backend contract tests and the existing Phase 007 frontend authority guard.
- `ai-orchestration-service`, legacy /api/tasks paths, JSON config files, header-based demo auth, `AiResultDomainProjectionService`, Redis cache behavior and generated topics remain transition mechanisms, not final architecture.
- D001, D002, D003, D007 and D008 remain open for later approved phases.

## Scope Control

This implementation did not expand scope beyond the architect handoff.

It did not:

- modify Java, Python, frontend, database, Redis, Kafka, config, dependency, build, deployment, state, debt, backlog, transition lifetime or prior handoff files
- add helpers, adapters, bridges, fallbacks, wrappers, route aliases, compatibility endpoints, gateway proxies, frontend API adapters, projection wrappers, Kafka compatibility bridges or service wrappers
- approve or implement service extraction, projection split, route migration, route alias, endpoint changes, gateway/auth, config-store migration, database schema change, Redis change, Kafka change, frontend reshaping, Python behavior change, business behavior change, permanent modular-monolith architecture or new feature work

Re-review is required by Window 3 before Window 4 may freeze Phase 011.
