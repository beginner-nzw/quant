# Phase 004 Architect Handoff

## Status

Window: Window 1 - Phase Architect.

Phase: Phase 004 - Python AI Workflow Contract Cleanup.

This handoff is architecture planning only. It does not authorize implementation. Window 2 may start only after the user explicitly approves this file.

## Inputs Read

Required harness files:

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
- `docs/harness/handoffs/steering-decision-phase-004.md`

Startup recovery file also read because `current-state.md` requires it:

- `docs/harness/handoffs/phase-003-final.md`

Code inspected for Phase 004 boundaries:

- `quant-ai-platform/quant-ai-engine/app/agents/planner_agent.py`
- `quant-ai-platform/quant-ai-engine/app/agents/intent_agent.py`
- `quant-ai-platform/quant-ai-engine/app/agents/financial_analysis_agent.py`
- `quant-ai-platform/quant-ai-engine/app/agents/risk_review_agent.py`
- `quant-ai-platform/quant-ai-engine/app/agents/report_generation_agent.py`
- `quant-ai-platform/quant-ai-engine/app/services/market_data_service.py`
- `quant-ai-platform/quant-ai-engine/app/messaging/kafka_producer.py`
- `quant-ai-platform/quant-ai-engine/app/messaging/message_models.py`
- `quant-ai-platform/quant-ai-engine/tests/test_ai_task_message_contract.py`
- `quant-ai-platform/quant-services/quant-common/quant-common-model/src/main/java/com/quant/common/model/message/AiTaskResultMessage.java`
- `quant-ai-platform/quant-services/quant-common/quant-common-model/src/main/java/com/quant/common/model/message/AiTaskAuditMessage.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/consumer/AiTaskResultConsumer.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/consumer/AiTaskAuditConsumer.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/AiResultDomainProjectionServiceImpl.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/ReportQueryServiceImpl.java`

## 1. Phase Goal

Separate AI execution fallback from business truth without changing current task behavior.

Phase 004 must make existing Python AI fallback paths auditable by carrying fallback reason or equivalent fallback signal through existing result/audit contracts. It must preserve the existing workflow, report/risk/financial content behavior, Java projection semantics, Kafka topics, URLs and frontend contracts.

The approved bounded goal is:

- Existing model fallback outputs are visibly marked as fallback.
- Report, risk, financial and existing market-data fallback state is observable in existing metadata or audit surfaces.
- Java can inspect fallback metadata through existing `reportMeta` / `rawPayload` / audit remark surfaces.
- Fallback metadata must not become a new source of business truth.

## 2. Belongs

### Formal / transition hosts

- Python fallback generation belongs to `quant-ai-engine`.
- AI execution audit message emission belongs to `quant-ai-engine`.
- AI result consumption, raw result persistence and projection currently belong to `ai-orchestration-service` as transition host.
- Report/risk/strategy/domain projection truth remains with Java tables and projection code already listed in the authority matrix.

### In-scope fallback surfaces

Window 2 must cover these existing surfaces only:

- Planner fallback: `plan_result.generationMode` and `plan_result.fallbackReason`.
- Intent fallback: `intent_result.generationMode` and `intent_result.fallbackReason`.
- Financial fallback: `financial_result.generationMode`, and a non-empty fallback reason or equivalent metadata when financial model output is unavailable or invalid.
- Risk fallback: `risk_result.generationMode`, and a non-empty fallback reason or equivalent metadata when risk model output is unavailable or invalid.
- Report fallback: `report_result.contextSnapshot.generationMode`, `reportGenerationPath` and `reportFallbackReason`.
- Market-data fallback: `market_context.dataSource == "fallback"` remains the existing fallback signal; optional reason metadata may be added only inside existing metadata maps and only without behavior change.

### Out-of-scope fallback surfaces

- Failed, cancelled and timeout workflow result behavior is out of scope except that it must remain unchanged.
- Python rule content generation itself is out of scope.
- Java display hydration fallback from Phase 003 is out of scope except that it must not be weakened.

## 3. Authority

Fallback metadata is audit/provenance data only.

It must not:

- Change `research_task.status`, `current_stage`, final status or retry semantics.
- Change whether a `risk_warning`, `strategy_signal`, `research_report_section` or evidence reference is created.
- Change warning level, signal score, signal direction, confidence score or human review requirement.
- Let Python fallback output become model-generated truth or business SoT.
- Let `research-workbench` become an authority source for Python output.

Java projection may inspect fallback metadata, but it must continue projecting from the same approved result content fields:

- `payload.summary`
- `payload.confidenceScore`
- `payload.needHumanReview`
- `payload.riskWarnings`
- `payload.reportMeta.riskPoints`
- `payload.reportMeta.highlights`
- `payload.reportMeta.evidenceItems`
- `payload.reportMeta.evidenceRefs`

Fallback metadata under `reportMeta.contextSnapshot` must remain non-authoritative.

## 4. Contract

### Stable contracts

Must remain stable:

- Kafka topics:
  - `ai.task.dispatch`
  - `ai.task.status`
  - `ai.task.result`
  - `ai.task.audit`
- Kafka envelope fields and required payload fields.
- `AiTaskResultPayload` top-level field list.
- `AiTaskAuditPayload` top-level field list.
- `AgentAuditItem` top-level field list.
- Existing URL paths and HTTP methods under `/api/tasks/*` and `/api/research/tasks`.
- Existing frontend API files and route behavior.
- Existing database schema and entity field sets.
- Existing DTO/VO top-level fields and response envelopes.

### Allowed metadata contract

Window 2 may add optional keys inside existing map-like metadata only:

- `report_result.contextSnapshot`
- `reportMeta.contextSnapshot` as serialized by `send_result`
- Python in-memory `financial_result` / `risk_result` dictionaries when those keys are later copied into `contextSnapshot`

Recommended optional key names if needed:

- `financialGenerationMode`
- `financialFallbackReason`
- `riskGenerationMode`
- `riskFallbackReason`
- `marketDataSource`
- `marketDataFallbackReason`

These keys are optional provenance metadata. They must not be required by existing Java DTOs, frontend code, database schema or Kafka top-level models.

### Not allowed as contract changes

Window 2 must not add or rename top-level fields in:

- `AiTaskResultPayload`
- `AiTaskAuditPayload`
- `AgentAuditItem`
- Java `AiTaskResultMessage.ResultPayload`
- Java `AiTaskAuditMessage.AuditPayload`
- Java `AiTaskAuditMessage.AgentAuditItem`

If this phase cannot expose fallback metadata without one of those top-level schema changes, Window 2 must stop as a blocker.

## 5. Allowed File Scope

Recommended Window 2 type: one mixed Python/backend implementer, with Python as the primary scope and Java limited to tests or read-only contract assertions.

### Python production files allowed

- `quant-ai-platform/quant-ai-engine/app/agents/planner_agent.py`
- `quant-ai-platform/quant-ai-engine/app/agents/intent_agent.py`
- `quant-ai-platform/quant-ai-engine/app/agents/financial_analysis_agent.py`
- `quant-ai-platform/quant-ai-engine/app/agents/risk_review_agent.py`
- `quant-ai-platform/quant-ai-engine/app/agents/report_generation_agent.py`
- `quant-ai-platform/quant-ai-engine/app/services/market_data_service.py`
- `quant-ai-platform/quant-ai-engine/app/messaging/kafka_producer.py`

Use only if necessary, and only to preserve existing field lists:

- `quant-ai-platform/quant-ai-engine/app/messaging/message_models.py`

### Python test files allowed

- Existing tests under `quant-ai-platform/quant-ai-engine/tests/`
- New Python test files under `quant-ai-platform/quant-ai-engine/tests/`

### Java/backend files allowed

Java production changes are not expected.

Allowed Java/backend changes are limited to tests unless Window 2 proves a tiny read-only production comment/helper is necessary:

- `quant-ai-platform/quant-services/quant-common/quant-common-model/src/test/java/com/quant/quantcommonmodel/AiTaskMessageContractTests.java`
- Existing or new tests under `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/`

If Java production code is changed, it may only inspect existing `reportMeta` map metadata or document non-authority. It must not alter projection outputs.

### Handoff file allowed

- `docs/harness/handoffs/phase-004-implementation.md`

## 6. Forbidden File Scope

Do not modify:

- `quant-ui/**`
- Java controllers under `ai-orchestration-service/src/main/java/**/controller/**`
- Java DTO/VO/entity/mapper classes
- Database migration or SQL files
- Kafka topic constants
- Top-level Java/Python Kafka message payload field definitions
- `quant-ai-platform/ai-config/**`
- Docker, gateway, Nacos, Sentinel or deployment files
- `docs/harness/00-project-charter.md` through `docs/harness/10-steering-state-machine.md`
- `docs/harness/state/current-state.md`
- Phase 001, Phase 002 or Phase 003 handoffs

Do not modify frontend API consumers, router paths or page behavior.

## 7. Allowed New Class / Method Types

Allowed:

- Small private Python helper methods inside in-scope agent classes for:
  - fallback reason normalization
  - generation mode normalization
  - local metadata assembly
  - testable extraction of provenance metadata
- Python test doubles and test-only helper functions.
- Java test helper methods/classes under test source roots.
- Optional production comments/Javadoc-style notes that clarify fallback metadata is provenance only.

Not preferred but allowed if kept local and private:

- A tiny private Python helper in an in-scope file to avoid duplicating the same fallback metadata shape.

## 8. Forbidden Helpers / Adapters / Fallbacks / Bridges

Do not add:

- New Agent classes.
- New workflow nodes, branches, checkpointing, retry redesign or parallel execution.
- New fallback source, fallback router, fallback adapter or fallback bridge.
- New backend client endpoint calls for fallback metadata.
- New frontend adapter or API mapper.
- New Java service, controller, mapper or repository for fallback metadata.
- New shared "truth resolver" for fallback/model selection.
- New URL aliases.
- New Kafka topics.
- New database tables or columns.
- New transition host responsibilities.

Do not move fallback handling into `research-task-service`, frontend, workbench query services or Java domain projection code.

## 9. Behavior Constraints

Must remain unchanged:

- Workflow order and enabled-agent selection.
- Task status transitions, final status and final stage.
- Existing progress values and node names unless already generated by untouched code.
- Report summary/highlights/risk point/risk warning content selection, except for optional metadata keys.
- Risk warning projection creation/deletion rules.
- Strategy signal score/direction/factor rules.
- Report evidence and section projection rules.
- Existing Redis cache keys and TTL intent.
- Existing audit record insertion behavior.
- Existing `Result.success(...)` frontend response envelopes.
- Existing Python model invocation order: LangChain first where currently used, custom HTTP fallback where currently used, rule fallback where currently used.

Allowed behavior change:

- Add optional metadata keys that make existing fallback paths auditable.
- Add tests that fail when fallback reason/audit metadata disappears.

## 10. Acceptance Criteria

Phase 004 implementation is acceptable only if all are true:

- Planner fallback remains marked with `generationMode == "RULE_FALLBACK"` and non-empty `fallbackReason` when LangChain is disabled or returns no result.
- Intent fallback remains marked with `generationMode == "RULE_FALLBACK"` and non-empty `fallbackReason` when LangChain is disabled or returns no result.
- Financial fallback is marked with `generationMode == "RULE_FALLBACK"` and exposes a non-empty fallback reason or equivalent audit metadata through existing result metadata.
- Risk fallback is marked with `generationMode == "RULE_FALLBACK"` and exposes a non-empty fallback reason or equivalent audit metadata through existing result metadata.
- Report fallback keeps `generationMode`, `reportGenerationPath` and `reportFallbackReason` in `contextSnapshot`.
- Report `contextSnapshot` includes enough provenance to distinguish model-assisted, custom-HTTP fallback and rule fallback paths without adding Kafka top-level fields.
- Market-data fallback remains clearly visible as `marketDataSource == "fallback"` in `contextSnapshot`; if a reason is added, it is optional metadata only.
- Java can inspect fallback provenance from existing `reportMeta` / `rawPayload` / report context hydration paths.
- Java projection does not use fallback metadata to create, delete or score report/risk/strategy domain facts.
- No URL, HTTP method, frontend route, DTO/VO/entity, DB schema, Kafka topic or top-level Kafka payload field changes occur.
- No existing fallback is removed or downgraded.
- No new agent or product feature is added.
- Required verification commands pass, or any unavailable command is recorded with exact reason in the implementation handoff.

## 11. Required Verification Commands

From `D:\projects\bussiness\quant-ai-platform\quant-ai-engine`:

```powershell
python -m compileall app
python -m unittest discover -s tests
```

If `pytest` is installed in the environment, also run:

```powershell
python -m pytest
```

From `D:\projects\bussiness\quant-ai-platform\quant-services`:

```powershell
mvn -q test
```

Frontend verification is not required because frontend changes are forbidden.

## 12. Blocker Stop Rules

Window 2 must stop and write a blocker in its implementation handoff if any of these become necessary:

- Adding top-level fields to Java or Python Kafka payload classes.
- Changing Kafka topics, envelope fields or required fields.
- Changing URL paths, controller mappings or frontend API consumers.
- Changing database schema, entity fields, DTO fields or VO fields.
- Changing business behavior to make fallback metadata visible.
- Using fallback metadata to change risk warning, strategy signal, report section, evidence or domain event projection.
- Adding a new agent, workflow branch, checkpoint, retry mechanism or model execution path.
- Adding a new fallback source, helper service, adapter or bridge.
- Removing or downgrading existing fallback behavior.
- Treating `research-workbench` or market-data fallback as business truth.
- Needing frontend display work to satisfy this phase.
- Tests reveal current Java/Python message contracts cannot preserve the metadata through existing `reportMeta` / `rawPayload` / audit remark surfaces.

When blocked, Window 2 must not improvise a workaround. It must record:

- The exact file and code path that forced the blocker.
- The smallest contract change that would be required.
- Whether the required change is breaking or additive.
- Which approval is needed before continuing.

## 13. Window 2 Instruction Summary

Window 2 must implement only the approved Phase 004 handoff.

Recommended implementation order:

1. Confirm current fallback metadata with focused tests.
2. Add missing Python fallback reason metadata for financial and risk fallback paths.
3. Ensure report `contextSnapshot` carries financial/risk fallback provenance.
4. Add or update Python tests for all in-scope fallback metadata.
5. Add Java tests only if needed to guard existing message contract or projection non-authority.
6. Run required verification.
7. Write `docs/harness/handoffs/phase-004-implementation.md`.

Window 2 must not choose the next phase and must not proceed beyond this handoff.

## Human Approval Request

Please approve this Phase 004 architect handoff before Window 2 starts.

Default approval meaning:

- No breaking changes.
- Keep URLs stable.
- Keep Kafka top-level contracts stable.
- No frontend changes.
- No business behavior changes.
- Mixed Python/backend implementation is allowed only inside the file boundaries above.
