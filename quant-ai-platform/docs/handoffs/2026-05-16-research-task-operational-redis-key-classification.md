# Research Task Operational Redis Key Classification

Date: 2026-05-16
Status: completed as documentation-only classification

## Session Contract

Input authority:

- `docs/code-domain-boundary-map.md`
- `docs/harness-authority-contract.md`
- `docs/harness-session-protocol.md`
- `docs/eval-harness.md`
- `docs/governance-orchestrator-protocol.md`
- `docs/handoffs/2026-05-16-redis-usage-scan-guard-follow-up.md`
- `docs/handoffs/2026-05-15-redis-authority-documentation-test-result.md`

H1 owner:

- Research Task Service owns task creation operational throttling and locking
  behavior.
- Common Redis owns shared cross-service Redis key constants and builders.

Allowed work completed:

- Reviewed the residual Redis usage scan keys:
  `hot:create:target:{targetCode}` and
  `lock:task:create:{taskType}:{targetCode}`.
- Classified their behavior and sharing boundary.
- Reviewed the existing Common Redis key surface as read-only evidence.
- Did not change Java, Common Redis, Python, ai-orchestration, frontend,
  SQL/config, Kafka payloads/schema, Redis key formats, task creation behavior,
  fallback, retry, cancel, projection, T006/T007, or report version snapshot
  behavior.

## Usage Evidence

Search commands:

```text
rg -n -F 'hot:create:target' quant-ai-platform
rg -n -F 'lock:task:create' quant-ai-platform
```

Observed production usage:

- `quant-services/quant-business/research-task-service/src/main/java/com/quant/task/service/impl/ResearchTaskServiceImpl.java`
  - `hot:create:target:{targetCode}` is incremented with a one-second TTL
    before task creation and rejects creation when the short-window count is
    above the local threshold. This is hot-key throttling for task creation.
  - `lock:task:create:{taskType}:{targetCode}` is passed to
    `RedisLockHelper.tryLock(...)` with a 10-second lease and unlocked in the
    `finally` block. This is the task creation lock.

Read-only Common Redis inspection:

- `RedisKeyConstants` currently defines shared task state/cache/runtime keys
  such as `task:state:%s`, `task:result:%s`, `task:full:%s`,
  `task:control:%s`, `task:meta:%s`, task list/stat keys, and signal projection
  keys.
- `RedisKeyBuilder` currently builds task state/result/full/control/meta keys
  and signal latest/ranking keys.
- No existing Common Redis constant or builder exactly covers
  `hot:create:target:{targetCode}` or
  `lock:task:create:{taskType}:{targetCode}`.

## Classification

| Redis key | Behavior | Sharing decision | Reason |
| --- | --- | --- | --- |
| `hot:create:target:{targetCode}` | Hot-key throttling for Research Task task creation | Keep as Research Task Service local operational key | The key is only observed in `ResearchTaskServiceImpl`, governs a local create-rate guard, has no observed cross-service reader/writer, and is not authoritative business persistence. |
| `lock:task:create:{taskType}:{targetCode}` | Task creation lock for Research Task task creation | Keep as Research Task Service local operational key | The key is only observed in `ResearchTaskServiceImpl`, is consumed through `RedisLockHelper` for local task creation serialization, has no observed cross-service reader/writer, and is not authoritative business persistence. |

## Common Redis Promotion Decision

Do not promote these keys to Common Redis in this window.

Rationale:

- The authority stack says Common Redis owns shared cross-service constants and
  builders, while Research Task Service owns task creation behavior.
- The two keys are operational support for one Research Task Service create
  workflow, not a shared contract between services.
- No cross-service dependency was observed.
- Adding public Common Redis builders would widen the shared Redis surface
  without evidence that another service needs the contract.
- The current key formats and behavior must remain unchanged.

## Changed Files

- `docs/handoffs/2026-05-16-research-task-operational-redis-key-classification.md`

Files inspected but not changed:

- `quant-services/quant-business/research-task-service/src/main/java/com/quant/task/service/impl/ResearchTaskServiceImpl.java`
- `quant-services/quant-business/research-task-service/src/test/java/**`
- `quant-services/quant-common/quant-common-redis/src/main/java/com/quant/common/redis/RedisKeyConstants.java`
- `quant-services/quant-common/quant-common-redis/src/main/java/com/quant/common/redis/RedisKeyBuilder.java`

## Verification

Doc-only classification; no Java production or test files changed.

Commands run:

```text
rg -n -F 'hot:create:target' quant-ai-platform
rg -n -F 'lock:task:create' quant-ai-platform
git status --short
```

Result:

- Each key has one observed production use, both in
  `ResearchTaskServiceImpl`.
- Existing unrelated dirty worktree changes were observed outside this window's
  allowed file scope and were not staged.

Maven:

```text
mvn -pl quant-business/research-task-service -am "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Result:

- Not run; optional because this window changed documentation only and did not
  modify Research Task Service Java production or test code.

## Residual Risk

- This classification is based on source search and read-only helper inspection;
  it is not a permanent static-analysis guard.
- The two keys remain local hard-coded operational Redis keys in
  `ResearchTaskServiceImpl`. That is intentional under the current evidence
  because promoting them would expand Common Redis without a cross-service
  contract need.

## Stop Point

Stop at Research Task operational Redis key classification. Candidate follow-up
work requires a separate Governance Orchestrator prompt and is not authorized by
this result.
