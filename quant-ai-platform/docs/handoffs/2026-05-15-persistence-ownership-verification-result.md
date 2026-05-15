# Persistence Ownership Verification Result

Date: 2026-05-15
Status: verified with documentation-only evidence

## Session Contract

Input authority:

- `docs/code-domain-boundary-map.md`
- `docs/harness-authority-contract.md`
- `docs/transition-lifetime.md`
- `docs/harness-session-protocol.md`
- `docs/fallback-retirement-plan.md`
- `docs/eval-harness.md`
- `docs/governance-orchestrator-protocol.md`
- `docs/handoffs/2026-05-15-business-boundary-review.md`
- `docs/handoffs/2026-05-15-cancel-runtime-signal-boundary-execution-result.md`
- `docs/handoffs/2026-05-15-projection-idempotency-stale-message-execution-result.md`
- `docs/handoffs/2026-05-15-redis-authority-documentation-test-result.md`

H1 owner:

- Java business services own MySQL persistence tables, entity/mapper access,
  and table-backed business state.
- Python engine must not directly own or access MySQL tables; it exchanges
  state through Kafka and Java HTTP APIs.

Allowed work completed:

- Reviewed MySQL init scripts under `docker/mysql/init/*.sql` as read-only
  schema inventory.
- Reviewed Java business `domain/entity` and `mapper` classes for table-backed
  ownership evidence.
- Reviewed `quant-ai-engine/app` for direct MySQL, DB-client, ORM, JDBC, DB URL,
  and raw SQL ownership patterns.
- Added this result handoff only. No SQL, Java, Python, HTTP, Kafka, Redis,
  frontend, fallback, retry, cancel, projection, T006/T007, or report snapshot
  behavior was changed.

## Table Owner Matrix

| Table | SQL source | H1 owner | Java entity evidence | Java mapper evidence | Python DB ownership |
| --- | --- | --- | --- | --- | --- |
| `research_task` | `001_init.sql` | `research-task-service` command owner; `ai-orchestration-service` read/control/projection access as frozen by H1 | `research-task-service` `ResearchTaskDO`; `ai-orchestration-service` `ResearchTaskDO` | both services `ResearchTaskMapper` | none observed |
| `research_task_step` | `001_init.sql` | `research-task-service` creation owner; `ai-orchestration-service` query/projection access as frozen by H1 | both services `ResearchTaskStepDO` | both services `ResearchTaskStepMapper` | none observed |
| `task_outbox_message` | `013_t007_task_dispatch_outbox.sql` | `research-task-service` | `TaskOutboxMessageDO` in `research-task-service` | `TaskOutboxMessageMapper` in `research-task-service` | none observed |
| `task_message_log` | `009_domain_foundation.sql` | Java business services local produced/consumed message records | both services `TaskMessageLogDO` | both services `TaskMessageLogMapper` | none observed |
| `research_task_retry_log` | `003_init.sql` | `ai-orchestration-service` | `ResearchTaskRetryLogDO` | `ResearchTaskRetryLogMapper` | none observed |
| `ai_workflow_instance` | `002_init.sql` | `ai-orchestration-service` | `AiWorkflowInstanceDO` | `AiWorkflowInstanceMapper` | none observed |
| `ai_agent_execution` | `002_init.sql` | `ai-orchestration-service` | `AiAgentExecutionDO` | `AiAgentExecutionMapper` | none observed |
| `audit_record` | `002_init.sql` | `ai-orchestration-service` | `AuditRecordDO` | `AuditRecordMapper` | none observed |
| `ai_prompt_audit` | `009_domain_foundation.sql` | `ai-orchestration-service` | `AiPromptAuditDO` | `AiPromptAuditMapper` | none observed |
| `human_review_record` | `009_domain_foundation.sql` | `ai-orchestration-service` | `HumanReviewRecordDO` | `HumanReviewRecordMapper` | none observed |
| `research_report` | `004_init.sql` | `ai-orchestration-service` | `ResearchReportDO` | `ResearchReportMapper` | none observed |
| `research_report_section` | `010_report_section.sql` | `ai-orchestration-service` | `ResearchReportSectionDO` | `ResearchReportSectionMapper` | none observed |
| `research_report_review_log` | `005_init.sql` | `ai-orchestration-service` | `ResearchReportReviewLogDO` | `ResearchReportReviewLogMapper` | none observed |
| `report_evidence_ref` | `009_domain_foundation.sql` | `ai-orchestration-service` | `ReportEvidenceRefDO` | `ReportEvidenceRefMapper` | none observed |
| `market_event` | `007_init.sql` | `ai-orchestration-service` | `MarketEventDO` | `MarketEventMapper` | none observed |
| `market_event_relation` | `009_domain_foundation.sql` | `ai-orchestration-service` | `MarketEventRelationDO` | `MarketEventRelationMapper` | none observed |
| `market_event_analysis` | `009_domain_foundation.sql` | `ai-orchestration-service` | `MarketEventAnalysisDO` | `MarketEventAnalysisMapper` | none observed |
| `risk_warning` | `009_domain_foundation.sql` | `ai-orchestration-service` | `RiskWarningDO` | `RiskWarningMapper` | none observed |
| `risk_warning_detail` | `009_domain_foundation.sql` | `ai-orchestration-service` | `RiskWarningDetailDO` | `RiskWarningDetailMapper` | none observed |
| `strategy_signal` | `009_domain_foundation.sql` | `ai-orchestration-service` | `StrategySignalDO` | `StrategySignalMapper` | none observed |
| `strategy_signal_factor` | `009_domain_foundation.sql` | `ai-orchestration-service` | `StrategySignalFactorDO` | `StrategySignalFactorMapper` | none observed |

## Java Persistence Evidence

Read-only inspection found Java MyBatis-Plus ownership evidence through
`@TableName(...)` domain entities and `BaseMapper<...>` mapper interfaces in
the Java business services.

Research Task Service owns command-side task persistence:

- `ResearchTaskDO` / `ResearchTaskMapper`
- `ResearchTaskStepDO` / `ResearchTaskStepMapper`
- `TaskOutboxMessageDO` / `TaskOutboxMessageMapper`
- `TaskMessageLogDO` / `TaskMessageLogMapper`

AI Orchestration Service owns query/control/projection persistence:

- task access: `ResearchTaskDO`, `ResearchTaskStepDO`, `ResearchTaskRetryLogDO`,
  `TaskMessageLogDO`
- workflow/audit access: `AiWorkflowInstanceDO`, `AiAgentExecutionDO`,
  `AuditRecordDO`, `AiPromptAuditDO`, `HumanReviewRecordDO`
- report/evidence access: `ResearchReportDO`, `ResearchReportSectionDO`,
  `ResearchReportReviewLogDO`, `ReportEvidenceRefDO`
- market/risk/signal access: `MarketEventDO`, `MarketEventRelationDO`,
  `MarketEventAnalysisDO`, `RiskWarningDO`, `RiskWarningDetailDO`,
  `StrategySignalDO`, `StrategySignalFactorDO`

This matches H1: MySQL tables remain a Java business persistence contract.

## Python No-DB Evidence

Reviewed `quant-ai-engine/app` with focused searches for:

- DB clients and ORMs: `sqlalchemy`, `pymysql`, `aiomysql`, `asyncmy`,
  `mysqlclient`, `MySQLdb`, `create_engine`, `sessionmaker`,
  `declarative_base`
- direct database URLs or JDBC ownership: `jdbc:mysql`, `DATABASE_URL`,
  `DB_URL`, `MYSQL_`, `mysql+`
- raw SQL execution patterns: SQL verbs with `from`/`into`/`set`,
  `.execute(...)`, `.executemany(...)`

Result:

- No direct MySQL client, SQLAlchemy-style ORM, JDBC URL, DB URL, or raw SQL
  ownership pattern was observed under `quant-ai-engine/app`.
- The engine still uses Kafka messaging and Java backend HTTP client paths for
  business state exchange.
- Redis runtime support remains outside MySQL persistence ownership and does
  not change this DB boundary.

## Test Pattern Decision

No existing `*Persistence*Tests.java` pattern was found under the allowed test
scopes:

- `quant-business/ai-orchestration-service/src/test/java/**`
- `quant-business/research-task-service/src/test/java/**`

Because this window found sufficient documentation evidence and no clear
pre-existing persistence ownership test pattern, no Java test was added.

## Verification

Command:

```text
mvn -pl quant-business/ai-orchestration-service,quant-business/research-task-service -am "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Working directory:

```text
quant-ai-platform/quant-services
```

Result:

```text
BUILD SUCCESS
```

Observed test summary:

- `quant-common-core`: 1 test passed.
- `quant-common-model`: 5 tests passed.
- `quant-common-messaging`: 1 test passed.
- `ai-orchestration-service`: 41 tests passed.
- `research-task-service` and several common modules reached reactor success,
  but some surefire report files could not be written because of local
  `target/surefire-reports` access warnings; those modules reported zero tests
  in the affected fork output while the overall Maven reactor still completed
  with `BUILD SUCCESS`.

## Changed Files

- `docs/handoffs/2026-05-15-persistence-ownership-verification-result.md`

No Java, Python, SQL, frontend, Kafka, Redis, DTO/VO/controller, common model,
or common messaging files were changed by this window.

## Residual Risk

- This verification is documentation-only and source-inspection based; no new
  persistence guard test was added because no matching existing test pattern was
  present in the allowed scopes.
- Pre-existing unrelated worktree changes remain outside this window and were
  not staged, including retry/common/message-contract files and a pre-existing
  `ResearchTaskMapper.java` modification.
- Maven logged existing local report-write warnings under `target` for several
  modules, although the required reactor command returned `BUILD SUCCESS`.

## Stop Point

Stop at Persistence Ownership Verification. Candidate next steps require a new
Orchestrator prompt and are not authorized by this result.
