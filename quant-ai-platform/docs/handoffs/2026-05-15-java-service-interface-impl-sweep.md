# Java Service Interface / Impl Sweep

Date: 2026-05-15
Status: implemented

## Session Contract

Input authority:

- `docs/code-domain-boundary-map.md`
- `docs/harness-authority-contract.md`
- `docs/harness-session-protocol.md`
- `docs/eval-harness.md`
- `docs/governance-orchestrator-protocol.md`
- `docs/handoffs/2026-05-15-h4-query-service-interface-impl-correction.md`
- `docs/handoffs/2026-05-15-h4-pre-gate-completion-review.md`

H1 owner:

- AI Orchestration Service owns its service/controller/package internals.
- Research Task Service owns its service/controller/package internals.

Allowed work:

- Normalize Java business `service/*Service.java` files to public interfaces and
  `service/impl/*ServiceImpl.java` files to Spring implementation classes.
- Adjust existing direct-construction tests to instantiate implementation
  classes while preserving assertions.
- Preserve HTTP, Kafka, DB, Redis, DTO, VO, mapper, entity, SQL, config, Python,
  frontend, permission, transaction, and business semantics.

Scope amendment:

- The initial prompt excluded test edits. During execution, existing tests
  directly instantiated former concrete services. The user explicitly
  authorized changing those direct instantiations to implementation classes so
  the interface/impl shape could be kept.

Hard exclusions observed:

- No cancel runtime signal work.
- No retry dispatch behavior work.
- No projection/idempotency/stale-message work.
- No fallback backfill, deletion, or retirement.
- No T006/T007.
- No report version snapshot or `version_no` semantic change.
- No frontend, Python, SQL/config, common model/messaging contract, DTO, VO,
  mapper, entity, route, Kafka payload, DB, or Redis behavior change.

## Audit List

Already compliant before this sweep:

- AI Orchestration: `AuditConfigDashboardQueryService`,
  `MarketEventService`, `MarketQueryService`, `ReportQueryService`,
  `RiskQueryService`, `StrategyQueryService`, `TaskControlService`,
  `TaskQueryService`, `TaskReportService`, `TaskRetryService`.
- Research Task: `ResearchTaskService`.

Converted from `service/*Service.java` concrete class to interface plus
`service/impl/*ServiceImpl.java` implementation:

- AI Orchestration: `AgentConfigService`,
  `AiResultDomainProjectionService`, `AiTaskDeadLetterPublisherService`,
  `AiTaskInboundMessageSupportService`, `CninfoProxyAnnouncementService`,
  `ConfigChangeAuditService`, `EventAutoTaskDispatchService`,
  `EventAutoTriggerConfigService`, `EventSourceConfigService`,
  `EventSourcePreviewService`, `MarketEventAutoTriggerService`,
  `MarketEventIngestHistoryService`, `MarketEventStandardizedPublisherService`,
  `ModelStrategyConfigService`, `PromptTemplateConfigService`,
  `RoleAccessConfigService`, `StrategySignalService`,
  `TaskDomainEventPublisherService`, `TaskMessageLogService`,
  `WorkflowConfigService`.
- Research Task: `TaskMessageLogService`, `TaskOutboxMessageService`,
  `TaskOutboxPublisherService`, `TaskRoleAccessService`.

Implementation annotations retained on implementation classes:

- `@Service`, `@RequiredArgsConstructor`, `@Slf4j`, constructor injection, and
  method runtime annotations such as `@Scheduled` remained on the implementation
  side.
- Public constants and nested public types referenced by callers remained
  available from the service interface names.

Direct impl dependency audit:

- Production controller/consumer/service consumers do not import
  `*.service.impl.*ServiceImpl`.
- Production injection points continue to use service interface types.
- Existing tests that directly constructed service classes now construct
  `*ServiceImpl` classes.

Special files left unchanged:

- `EventSourceSyncAdapter` and concrete `*SyncAdapter` classes are adapter
  plugin components, not `XXXService` services.
- `MarketEventMockIngestGenerator` is a component/generator, not `XXXService`.
- `TaskOutboxStatusConstants` is a constants holder, not a service.

## Changed Files

Production service interfaces and implementations under:

- `quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/`
- `quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/`
- `quant-services/quant-business/research-task-service/src/main/java/com/quant/task/service/`
- `quant-services/quant-business/research-task-service/src/main/java/com/quant/task/service/impl/`

Existing tests adjusted only for implementation construction:

- `quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/*ServiceTests.java`
- `quant-services/quant-business/research-task-service/src/test/java/com/quant/task/service/*ServiceTests.java`

Result document:

- `docs/handoffs/2026-05-15-java-service-interface-impl-sweep.md`

Unrelated pre-existing worktree changes observed and not staged by this sweep:

- `quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/mapper/ResearchTaskMapper.java`
- `quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/TaskRetryServiceImpl.java`
- `quant-services/quant-common/quant-common-messaging/pom.xml`
- `quant-services/quant-common/quant-common-model/pom.xml`
- `quant-ai-engine/tests/test_ai_task_message_contract.py`
- common-module test additions under `quant-common`

## Behavior Invariance Evidence

- No controller route, HTTP method, request binding, request body, response DTO,
  serialized field, or permission check was changed.
- No mapper, entity, SQL, config, Kafka payload/schema/topic, Redis key/behavior,
  Python, frontend, DTO, or VO file was edited by this sweep.
- Service method names, parameters, return types, and checked runtime behavior
  were preserved by moving method bodies unchanged into implementation classes.
- Tests that previously constructed concrete services now construct the
  corresponding implementation classes with the same constructor arguments and
  assertions.

## Verification

Command:

```text
mvn -pl quant-business/ai-orchestration-service,quant-business/research-task-service -am "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Result:

```text
BUILD SUCCESS
research-task-service: Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
ai-orchestration-service: Tests run: 41, Failures: 0, Errors: 0, Skipped: 0
Reactor summary: quant-services, required common modules, research-task-service,
and ai-orchestration-service SUCCESS
Finished at: 2026-05-15T17:47:30+08:00
```

## Commit

Commit message:

```text
chore(java): normalize business services to interface implementations
```

Commit hash:

```text
pending; final execution output records the exact hash
```

## Residual Risk

- `TaskRetryServiceTests.java` was an untracked pre-existing test file in the
  working tree. It was adjusted locally to implement the new
  `TaskMessageLogService` interface so Maven verification could compile the
  current workspace. It should only be staged if the window is allowed to include
  that pre-existing untracked test file.
- Existing unrelated retry/common/Python/test worktree changes remain outside
  this sweep.
