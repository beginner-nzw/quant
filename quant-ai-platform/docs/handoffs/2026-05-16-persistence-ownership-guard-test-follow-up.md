# Persistence Ownership Guard Test Follow-up

Date: 2026-05-16
Status: completed

## Session Contract

Input authority:

- `docs/code-domain-boundary-map.md`
- `docs/harness-authority-contract.md`
- `docs/harness-session-protocol.md`
- `docs/eval-harness.md`
- `docs/governance-orchestrator-protocol.md`
- `docs/handoffs/2026-05-15-persistence-ownership-verification-result.md`
- `docs/handoffs/2026-05-16-post-h4-business-backlog-slicing.md`

H1 owner:

- Java business services own MySQL persistence tables, entity/mapper access,
  and table-backed business state.
- Python engine must not directly own or access MySQL tables.

Allowed work completed:

1. Added minimal focused persistence ownership guard tests.
2. Verified Java business entity/mapper table ownership mappings by reflection.
3. Verified Python engine app source has no direct DB ownership signals.
4. Added this result handoff.

Hard exclusions preserved:

- No production Java, production Python, SQL, config, frontend, Kafka payload,
  Java common model, or Java common messaging files were changed.
- No fallback backfill, deletion, retirement, retry, cancel, projection,
  T006/T007, report snapshot, or `TaskQueryServiceImpl` work was performed.
- No real database was required.

## Coverage Evidence

Java command-side guard:

- `research_task` -> `ResearchTaskDO` / `ResearchTaskMapper`
- `research_task_step` -> `ResearchTaskStepDO` / `ResearchTaskStepMapper`
- `task_outbox_message` -> `TaskOutboxMessageDO` / `TaskOutboxMessageMapper`
- `task_message_log` -> `TaskMessageLogDO` / `TaskMessageLogMapper`

Java orchestration-side guard:

- task/message tables: `task_message_log`, `research_task`,
  `research_task_step`, `research_task_retry_log`
- workflow/audit tables: `ai_workflow_instance`, `ai_agent_execution`,
  `audit_record`, `ai_prompt_audit`, `human_review_record`
- report/evidence tables: `research_report`, `research_report_section`,
  `research_report_review_log`, `report_evidence_ref`
- market tables: `market_event`, `market_event_relation`,
  `market_event_analysis`
- risk/signal tables: `risk_warning`, `risk_warning_detail`,
  `strategy_signal`, `strategy_signal_factor`

Python direct-DB guard scans `quant-ai-engine/app` source files for:

- SQLAlchemy/ORM factory signals
- MySQL clients
- JDBC/MySQL URL and DB URL signals
- raw SQL execution and SQL verb ownership patterns

## Verification

Initial command:

```text
mvn -pl quant-business/research-task-service -am "-Dtest=PersistenceOwnershipGuardTests" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Working directory:

```text
quant-ai-platform/quant-services
```

Initial result:

```text
BUILD FAILURE
```

Reason:

- Maven failed before test execution while storing compiler plugin status under
  `target/maven-status/.../createdFiles.lst`, matching the local target write
  warnings recorded in the earlier persistence verification handoff.

Successful rerun command:

```text
mvn -pl quant-business/research-task-service -am "-Dtest=PersistenceOwnershipGuardTests" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Successful rerun result:

```text
BUILD SUCCESS
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
```

Successful orchestration command:

```text
mvn -pl quant-business/ai-orchestration-service -am "-Dtest=PersistenceOwnershipGuardTests" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Working directory:

```text
quant-ai-platform/quant-services
```

Result:

```text
BUILD SUCCESS
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
```

Successful Python command:

```text
python -m unittest tests.test_no_direct_db_ownership
```

Working directory:

```text
quant-ai-platform/quant-ai-engine
```

Result:

```text
Ran 1 test in 0.072s
OK
```

## Changed Files

- `quant-services/quant-business/research-task-service/src/test/java/com/quant/researchtaskservice/PersistenceOwnershipGuardTests.java`
- `quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/PersistenceOwnershipGuardTests.java`
- `quant-ai-engine/tests/test_no_direct_db_ownership.py`
- `docs/handoffs/2026-05-16-persistence-ownership-guard-test-follow-up.md`

## Residual Risk

- The guards are static/reflection checks. They lock the Java table mapping
  signals and Python absence-of-direct-DB signals, but they do not prove runtime
  persistence behavior against a real database.
- The Python guard is pattern-based and may need updates if legitimate
  non-ownership text introduces a false positive.
- Pre-existing unrelated dirty files remain outside this window and were not
  staged.

## Stop Point

Stop at Persistence Ownership Guard Test Follow-up. Candidate next steps require
a separate Governance Orchestrator prompt and are not authorized by this result.
