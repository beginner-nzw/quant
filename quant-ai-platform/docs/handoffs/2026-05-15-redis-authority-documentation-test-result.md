# Redis Authority Documentation/Test Execution Result

Date: 2026-05-15
Status: implemented with focused Redis key contract verification

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

H1 owner:

- Common Redis owns Redis key constants and helper builders.
- Java business services own Redis business cache/state writes.
- Python engine may only use Redis as explicitly bounded runtime support, not
  authoritative business persistence.

Allowed work completed:

- Reviewed Common Redis key constants and builders for the current business
  Redis key surface.
- Reviewed Java task control usage of `task:control:{taskId}`.
- Reviewed Python runtime cancellation guard usage of `task:control:{taskId}`.
- Added a focused Common Redis contract test for existing Redis key string
  formats without changing those formats.

## Redis Authority Evidence

Common Redis:

- `RedisKeyConstants` defines task state/cache/runtime keys:
  `TASK_STATE`, `TASK_RESULT`, `TASK_FULL`, `TASK_CONTROL`, and `TASK_META`.
- `RedisKeyConstants` defines task list/stat cache keys:
  `TASK_STATS_GLOBAL`, `TASK_LIST_VERSION`, and `TASK_LIST_CACHE_PREFIX`.
- `RedisKeyConstants` defines signal projection cache keys:
  `SIGNAL_LATEST` and `SIGNAL_RANKING`.
- `RedisKeyBuilder` provides builders for task state, result, full, control,
  meta, signal latest, and signal ranking keys.

Java business services:

- `TaskControlServiceImpl` writes `task:control:{taskId}` through
  `RedisKeyBuilder.taskControl(taskId)` as a 24-hour runtime cancellation
  signal.
- `TaskControlServiceImpl` also updates Java-owned task state cache and
  invalidates full/stat/list-version cache state through Common Redis
  constants/builders.
- Other observed Java business usage of task state/result/full/meta/list
  version/stat keys and signal latest/ranking keys goes through
  `RedisKeyBuilder` or `RedisKeyConstants`.

Python engine:

- `app/services/task_control_service.py` formats only
  `task:control:{taskId}` and calls `RedisClient.get(...)`.
- `RedisClient` exposes only `get(...)` in the current runtime support path.
- No Python write to Java-owned task state/result/full/meta/list/stat/signal
  Redis keys was introduced or observed in the reviewed file.

Boundary classification:

- `task:control:{taskId}` is a runtime cancellation signal, not authoritative
  persistence.
- Java remains authoritative for task cancellation state, task business
  cache/state writes, and signal projection cache writes.
- Python remains read-only for this Redis runtime guard and does not own Java
  business Redis cache/state.

## Changed Files

- `quant-services/quant-common/quant-common-redis/src/test/java/com/quant/common/redis/RedisKeyBuilderContractTests.java`
- `docs/handoffs/2026-05-15-redis-authority-documentation-test-result.md`

Files inspected but not changed:

- `quant-services/quant-common/quant-common-redis/src/main/java/com/quant/common/redis/RedisKeyConstants.java`
- `quant-services/quant-common/quant-common-redis/src/main/java/com/quant/common/redis/RedisKeyBuilder.java`
- `quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/TaskControlServiceImpl.java`
- `quant-ai-engine/app/services/task_control_service.py`

Pre-existing unrelated worktree changes were observed and not staged by this
window, including retry/common/message-contract files.

## Verification

Command:

```text
mvn -pl quant-common/quant-common-redis -Dtest=RedisKeyBuilderContractTests test
```

Result:

```text
BUILD SUCCESS
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
```

Note:

```text
The first sandboxed run failed before test execution because Maven could not
write compiler status files under target/maven-status. The same command was
rerun outside the sandbox and passed.
```

Command:

```text
python -m py_compile app/services/task_control_service.py
```

Result:

```text
passed
```

## Residual Risk

- The focused contract test locks current Common Redis key string formats but
  does not scan every future Redis use site.
- Python keeps a local string mirror for `task:control:{taskId}` because there
  is no cross-language Common Redis artifact; this is bounded as read-only
  runtime support.

## Stop Point

Stop at Redis Authority Documentation/Test. Candidate follow-up work requires a
new Orchestrator prompt and is not authorized by this result.
