# Projection Idempotency And Stale-Message Execution Result

Date: 2026-05-15
Status: implemented with focused verification

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
- `docs/handoffs/2026-05-15-task-retry-dispatch-consistency-implementation-note.md`
- `docs/handoffs/2026-05-15-cancel-runtime-signal-boundary-execution-result.md`

H1 owner:

- AI Orchestration Service owns inbound AI task status/result/audit
  consumption, Java task state, projection persistence, message-log handling,
  and cache invalidation.

Allowed work completed:

- Reviewed `ai.task.status`, `ai.task.result`, and `ai.task.audit` Java
  consumer paths.
- Reviewed `task_message_log` consumed/failed/duplicate handling.
- Preserved existing messageId idempotency, retry-count checks, final-state
  gates, failed inbound observability, Kafka payload shape, DB schema, Redis
  key usage, HTTP API, DTOs, SQL, frontend, Python, and report version
  semantics.
- Added a bounded DB-level final-state guard in the `ai.task.result` consumer
  before report/domain projection side effects.

## Changed Files

- `quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/consumer/AiTaskResultConsumer.java`
- `docs/handoffs/2026-05-15-projection-idempotency-stale-message-execution-result.md`

Files inspected but not changed by this window:

- `quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/consumer/AiTaskStatusConsumer.java`
- `quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/consumer/AiTaskAuditConsumer.java`
- `quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/TaskMessageLogService.java`
- `quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/TaskMessageLogServiceImpl.java`
- `quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/mapper/TaskMessageLogMapper.java`
- `quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/mapper/ResearchTaskMapper.java`

Pre-existing unrelated worktree changes were observed and not staged by this
window, including retry/common/message-contract files and the pre-existing
`ResearchTaskMapper.java` retry-dispatch change.

## Contract Evidence

Kafka:

- No topic, envelope field, payload field, payload field name, or Java/Python
  mirror contract changed.
- Consumers remain on `ai.task.status`, `ai.task.result`, and `ai.task.audit`
  using the existing shared message classes.

DB:

- No schema or mapper XML change was made.
- Existing Java-owned task state and projection tables remain authoritative.
- `task_message_log` continues to record `PROCESSING`, `SUCCESS`, and `FAILED`
  inbound states through the existing service and mapper.

Redis:

- Existing task state/result/full cache writes, invalidations, stats
  invalidation, and cache-version bump behavior are unchanged.
- Redis remains cache/runtime support, not authoritative persistence.

Package:

- Code change stayed inside AI Orchestration Service consumer internals.
- No Python, frontend, SQL/config, Java common model, or Java common messaging
  file was changed.

HTTP/DTO:

- No HTTP route, request DTO, response DTO, or frontend-facing API was changed.

## Behavior Evidence

Existing behavior retained:

- `TaskMessageLogService.beginConsume(...)` skips already successful or
  processing deliveries by `topicName + messageId + consumerService`, and
  allows a previously failed delivery to be reset to `PROCESSING`.
- Invalid inbound messages remain observable through `FAILED` message-log
  records and dead-letter publication.
- Status/result/audit consumers reject stale retry generations by comparing
  message `retryCount` with persisted task `retry_count`.
- Status messages skip progress projection if the task is already final.
- Result messages skip final projection if the current task state cannot
  transfer to the inbound final status.

New guarded behavior:

- `ai.task.result` now performs the final task-state write with a conditional
  MyBatis update guarded by task id, the exact status read before projection,
  the inbound retry generation, and `deleted = 0`.
- If that conditional update affects no row, the message is recorded as skipped
  with `TASK_FINAL_STATE_UPDATE_SKIPPED` and the consumer returns before report
  save, domain projection, domain event publication, workflow finish, retry-log
  update, Redis result/state write, cache invalidation, or cache-version bump.
- This closes the race where a stale final result could pass the read-side
  check and then overwrite a newer authoritative task state before projection.

Duplicate handling evidence:

- Duplicate message IDs are rejected by `beginConsume(...)` before projection.
- Duplicate successful deliveries are recorded as skipped rather than
  re-projecting.

Stale handling evidence:

- Stale retry generations are skipped before projection.
- Stale final-state races are skipped by the new conditional update before any
  downstream projection side effects.

## Verification

Command:

```text
mvn -pl quant-business/ai-orchestration-service -am "-Dtest=TaskMessageLogServiceTests,AiTaskInboundMessageSupportServiceTests,AiResultDomainProjectionServiceTests" "-Dsurefire.failIfNoSpecifiedTests=false" "-DforkCount=0" test
```

Result:

```text
BUILD SUCCESS
Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
```

Notes:

- The run compiled `ai-orchestration-service` successfully.
- Maven reported existing `target/maven-status` write warnings in common
  modules; they did not fail the build.
- `forkCount=0` was used to avoid the local forked-JVM/Mockito environment
  issue recorded by earlier handoffs.

## Residual Risk

- No new AI task result consumer test class was added because this window only
  allowed updating existing matching tests. Behavior is covered by source
  inspection plus focused compilation and existing message/inbound/projection
  tests.
- Existing pre-existing retry/common/message-contract worktree changes remain
  outside this window and were not staged.

## Stop Point

Stop at Projection Idempotency and Stale-Message Boundary. Candidate follow-up
work requires a new Orchestrator prompt and is not authorized by this result.
