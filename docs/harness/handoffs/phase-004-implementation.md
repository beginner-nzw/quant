# Phase 004 Implementation Handoff

## Status

Window: Window 2 - Implementer.

Phase: Phase 004 - Python AI Workflow Contract Cleanup.

Mode: Initial implementation.

Git baseline before edits: `93af53f`.

Implementation followed `docs/harness/handoffs/phase-004-architect.md` only. No next phase was selected.

## Files Changed

Python production files:

- `quant-ai-platform/quant-ai-engine/app/agents/financial_analysis_agent.py`
- `quant-ai-platform/quant-ai-engine/app/agents/risk_review_agent.py`
- `quant-ai-platform/quant-ai-engine/app/agents/report_generation_agent.py`

Python test files:

- `quant-ai-platform/quant-ai-engine/tests/test_fallback_provenance.py`

Handoff file:

- `docs/harness/handoffs/phase-004-implementation.md`

Pre-existing dirty or untracked harness files were observed and left untouched:

- `docs/harness/state/current-state.md`
- `docs/harness/handoffs/phase-003-review.md`
- `docs/harness/handoffs/phase-004-architect.md`
- `docs/harness/handoffs/steering-decision-phase-004.md`

## Implementation Summary

Financial fallback provenance:

- `FinancialAnalysisAgent` now returns a fallback reason from `_generate_model_result` when both LangChain/custom model paths fail, are disabled, or return invalid output.
- `financial_result.fallbackReason` is populated only for `RULE_FALLBACK` results.
- No financial fallback content, trend resolution, model invocation order, task status, audit insertion or stage/progress behavior changed.

Risk fallback provenance:

- `RiskReviewAgent` now returns a fallback reason from `_generate_model_result` when both LangChain/custom model paths fail, are disabled, or return invalid output.
- `risk_result.fallbackReason` is populated only for `RULE_FALLBACK` results.
- No risk level, risk point, warning, human review, priority external risk enrichment or audit behavior changed.

Report metadata propagation:

- `ReportGenerationAgent` now copies `financialGenerationMode`, `financialFallbackReason`, `riskGenerationMode` and `riskFallbackReason` into the existing `report_result.contextSnapshot` map.
- `marketDataSource == "fallback"` remains the primary market-data fallback signal.
- `marketDataFallbackReason` is added inside `contextSnapshot` only as optional provenance metadata, defaulting to `MARKET_DATA_FALLBACK_SNAPSHOT` when the existing market data source is fallback.

Tests:

- Added focused Python tests for planner/intent existing fallback reason behavior.
- Added focused Python tests for financial/risk fallback reason metadata.
- Added focused Python tests that report `contextSnapshot` carries report, financial, risk and market fallback provenance.

## Architect Acceptance Completed

- Planner fallback remains marked with `generationMode == "RULE_FALLBACK"` and a non-empty `fallbackReason`; guarded by test.
- Intent fallback remains marked with `generationMode == "RULE_FALLBACK"` and a non-empty `fallbackReason`; guarded by test.
- Financial rule fallback is marked with `generationMode == "RULE_FALLBACK"` and exposes non-empty `fallbackReason`.
- Risk rule fallback is marked with `generationMode == "RULE_FALLBACK"` and exposes non-empty `fallbackReason`.
- Report fallback keeps `generationMode`, `reportGenerationPath` and `reportFallbackReason` in `contextSnapshot`.
- Report `contextSnapshot` now distinguishes report, financial, risk and market fallback provenance without new Kafka top-level fields.
- Market-data fallback remains visible through `marketDataSource == "fallback"` and optional `marketDataFallbackReason`.
- Java can inspect the provenance through existing `reportMeta.contextSnapshot` / raw payload storage.
- Java projection was not changed and does not use fallback metadata to create, delete or score domain facts.

## Contracts Kept Unchanged

- Kafka topics unchanged.
- Python Kafka payload top-level field lists unchanged.
- Java Kafka payload classes unchanged.
- URL paths and HTTP methods unchanged.
- Frontend files and routes unchanged.
- Database schema, entity, DTO, VO and mapper shapes unchanged.
- Java controller and projection production code unchanged.
- Existing workflow order, enabled-agent selection, status transitions, final status and final stage unchanged.

## Behavior Change

Only optional provenance metadata was added inside existing Python dictionaries and `reportMeta.contextSnapshot`.

No business behavior was intentionally changed:

- Fallback selection remains the same.
- Report/risk/financial generated content shape remains the same except optional metadata keys.
- Task status, retry/cancel behavior and Java projection semantics remain unchanged.
- No fallback source was added, removed or downgraded.

## Verification Results

From `D:\projects\bussiness\quant-ai-platform\quant-ai-engine`:

- `python -m compileall app` passed.
- `python -m unittest discover -s tests` passed: 43 tests.
- `python -m pytest` unavailable: `No module named pytest`.

From `D:\projects\bussiness\quant-ai-platform\quant-services`:

- `mvn -q test` passed.

Note: Maven output included an expected `kafka down` stack trace from an existing failure-path test, but the command exited successfully.

## Blockers And Residual Risk

Blockers: none.

Residual risk:

- `pytest` is not installed in this environment, so only `compileall`, `unittest` and Maven verification were run.
- Java production code can inspect fallback provenance through existing `reportMeta.contextSnapshot`, but no Java production change was needed for this phase.
