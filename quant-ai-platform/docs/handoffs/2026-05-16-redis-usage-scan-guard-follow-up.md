# Redis Usage Scan / Guard Follow-up Result

Date: 2026-05-16
Status: completed as documentation-only scan; no production key changes

## Session Contract

Input authority:

- `docs/code-domain-boundary-map.md`
- `docs/harness-authority-contract.md`
- `docs/harness-session-protocol.md`
- `docs/eval-harness.md`
- `docs/governance-orchestrator-protocol.md`
- `docs/handoffs/2026-05-15-redis-authority-documentation-test-result.md`
- `docs/handoffs/2026-05-16-post-h4-business-backlog-slicing.md`

H1 owner:

- Common Redis owns Redis key constants and builders.
- Java business services own business cache/state writes.
- Python engine may only use Redis as bounded runtime support, not
  authoritative business persistence.

Allowed work completed:

- Scanned Java business Redis use in `research-task-service` and
  `ai-orchestration-service`.
- Scanned Python engine Redis use under `quant-ai-engine/app` as read-only
  inspection.
- Recorded Redis usage inventory and boundary classification.
- Did not change Redis key format, Java behavior, Python code, SQL/config,
  frontend, Kafka payloads, common model, DB ownership, fallback behavior,
  retry/cancel/projection implementation, T006/T007, or report version
  semantics.

## Scan Commands

Java Redis/helper/key scan:

```text
rg -n 'StringRedisTemplate|RedisTemplate|RedisKeyBuilder|RedisKeyConstants|"task:|"signal:' quant-services/quant-business/research-task-service/src/main/java quant-services/quant-business/ai-orchestration-service/src/main/java
```

Python Redis/write-command scan:

```text
rg -n "redis|task:control|\.set\(|\.hset\(|set\(|hset\(|delete\(|lpush\(|rpush\(|sadd\(|zadd\(" quant-ai-engine/app
rg -n "client\.|redis\.Redis|TASK_CONTROL_KEY|task:control" quant-ai-engine/app/clients quant-ai-engine/app/services
```

Additional Java hard-coded prefix scan:

```text
rg -n -F 'hot:' quant-services/quant-business/research-task-service/src/main/java quant-services/quant-business/ai-orchestration-service/src/main/java
rg -n -F 'lock:' quant-services/quant-business/research-task-service/src/main/java quant-services/quant-business/ai-orchestration-service/src/main/java
rg -n -F 'task:' quant-services/quant-business/research-task-service/src/main/java quant-services/quant-business/ai-orchestration-service/src/main/java
rg -n -F 'signal:' quant-services/quant-business/research-task-service/src/main/java quant-services/quant-business/ai-orchestration-service/src/main/java
```

## Redis Usage Inventory

### Java Business Cache/State Writes

Compliant:

- `research-task-service`
  - `ResearchTaskManager` writes `task:meta:{taskId}` and
    `task:state:{taskId}` through `RedisKeyBuilder.taskMeta(...)` and
    `RedisKeyBuilder.taskState(...)`.
  - `ResearchTaskServiceImpl` invalidates `task:stats:global` through
    `RedisKeyConstants.TASK_STATS_GLOBAL` and bumps list version through
    `TaskCacheVersionManager`.
  - `TaskCacheVersionManager` reads/increments `task:list:version` through
    `RedisKeyConstants.TASK_LIST_VERSION`.
- `ai-orchestration-service`
  - `AiTaskResultConsumer`, `AiTaskStatusConsumer`, `TaskControlServiceImpl`,
    and `TaskQueryServiceImpl` write task state/result/full/stat cache keys
    through `RedisKeyBuilder` or `RedisKeyConstants`.
  - `TaskQueryServiceImpl` uses `RedisKeyConstants.TASK_LIST_CACHE_PREFIX`
    plus the Java-owned list version/hash to read/write bounded list cache.
  - `TaskCacheVersionManager` reads/increments `task:list:version` through
    `RedisKeyConstants.TASK_LIST_VERSION`.
  - `AiResultDomainProjectionServiceImpl` and `StrategySignalServiceImpl`
    write/delete signal latest/ranking projection cache through
    `RedisKeyBuilder.signalLatest(...)` and
    `RedisKeyBuilder.signalRanking(...)`.

### Java Runtime Signal Writes

Compliant:

- `TaskControlServiceImpl` writes `task:control:{taskId}` through
  `RedisKeyBuilder.taskControl(...)` with a bounded TTL as a runtime
  cancellation signal, while Java remains authoritative for persistent cancel
  state.

### Java Read-only / Cache Reads

Compliant:

- `TaskQueryServiceImpl` reads task state, full detail, result, stats, and list
  cache keys through `RedisKeyBuilder` or `RedisKeyConstants`.
- Both business `TaskCacheVersionManager` implementations read
  `RedisKeyConstants.TASK_LIST_VERSION`.

### Python Runtime Reads

Compliant:

- `app/clients/redis_client.py` constructs `redis.Redis(...)` and exposes only
  `get(key)`.
- `app/services/task_control_service.py` formats
  `task:control:{task_id}` and calls `RedisClient.get(...)`.
- No Python Redis `set`, `hset`, `delete`, list/set/zset write, or Java
  business cache/state ownership was observed under `quant-ai-engine/app`.

### Suspicious Hard-coded Keys

Residual risk:

- `ResearchTaskServiceImpl` uses `hot:create:target:{targetCode}` for short
  create-rate limiting.
- `ResearchTaskServiceImpl` uses
  `lock:task:create:{taskType}:{targetCode}` through `RedisLockHelper` for a
  create-task lock.

No replacement was performed because these keys do not have existing
`RedisKeyBuilder`/`RedisKeyConstants` helpers. Adding new helpers would expand
the Common Redis key surface and was not necessary to preserve current business
semantics. They are bounded operational support keys, not Python-owned
business persistence and not observed as Java/Python cross-domain state.

## Classification

| Item | Classification | Notes |
| --- | --- | --- |
| Task state/result/full/meta keys | compliant | Java business use goes through Common Redis builders. |
| Task stats/list-version/list-cache keys | compliant | Constants own stable prefixes; list cache is Java query cache. |
| Signal latest/ranking keys | compliant | Java projection/cache use goes through Common Redis builders. |
| `task:control:{taskId}` | compliant | Java writes via builder; Python reads bounded runtime guard only. |
| `hot:create:target:{targetCode}` | residual risk | Local Research Task create throttling key; no safe existing builder replacement. |
| `lock:task:create:{taskType}:{targetCode}` | residual risk | Local Research Task create lock key; no safe existing builder replacement. |
| Python Redis writes | compliant | None observed. |
| Python business-state Redis ownership | compliant | None observed. |
| Safe Java builder replacement performed | not applicable | No existing builder/constant covered the only hard-coded non-task/signal keys. |
| Blocker | none | No Python business-state write or required key-format change found. |

## Changed Files

- `docs/handoffs/2026-05-16-redis-usage-scan-guard-follow-up.md`

Files inspected but not changed:

- `quant-services/quant-common/quant-common-redis/src/main/java/com/quant/common/redis/RedisKeyConstants.java`
- `quant-services/quant-common/quant-common-redis/src/main/java/com/quant/common/redis/RedisKeyBuilder.java`
- `quant-services/quant-common/quant-common-redis/src/test/java/com/quant/common/redis/RedisKeyBuilderContractTests.java`
- `quant-services/quant-business/research-task-service/src/main/java/**`
- `quant-services/quant-business/ai-orchestration-service/src/main/java/**`
- `quant-ai-engine/app/**`

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
The first sandboxed run returned BUILD SUCCESS but Surefire could not write its
report file under target/surefire-reports and reported Tests run: 0. The same
command was rerun outside the sandbox and passed with 3 tests executed.
```

Business Java test command:

```text
mvn -pl quant-business/ai-orchestration-service,quant-business/research-task-service -am "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Result:

```text
not run; optional because this window did not change Java production or test code
```

## Residual Risk

- This was a source scan, not a permanent static guard.
- Python keeps a local string mirror for `task:control:{task_id}` because there
  is no cross-language Common Redis artifact; it remains bounded to read-only
  runtime support.
- Research Task local `hot:` and `lock:` keys remain hard-coded operational
  support keys. They are not currently covered by Common Redis builders and
  were not changed to avoid widening Common Redis authority in this window.

## Stop Point

Stop at Redis Usage Scan / Guard Follow-up. Candidate next steps require a
separate Governance Orchestrator prompt and are not authorized by this result.
