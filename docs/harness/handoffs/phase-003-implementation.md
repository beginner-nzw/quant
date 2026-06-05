# Phase 003 Implementation Handoff

## Status

Phase: Phase 003 - Service-To-Service Identity And Audit Identity.

Window: Implementation.

Result: Complete.

## Scope Implemented

- Added a shared optional AI task actor provenance contract:
  - `servicePrincipal`
  - `identitySource`
  - `roleSource`
  - `systemActor`
  - `originalActor`
  - `delegatedActor`
- Extended AI task dispatch/status/result/audit payloads with optional `actorProvenance`.
- Kept Kafka topic names unchanged.
- Preserved payload compatibility by making the new field optional and by keeping existing field names/order stable except for appending the optional field.
- Updated Java and Python message mirrors.
- Preserved actor provenance through:
  - research-task dispatch outbox
  - AI engine dispatch consume
  - AI status/result/audit callbacks
  - event auto dispatch
  - manual retry
  - cancel audit records
- Stopped using frontend-style user headers or request-body fields as the source of service-principal truth for event auto dispatch. The internal service call still sends existing user headers for current endpoint compatibility, but research-task-service does not treat them as service-principal authority.
- Fix: event auto dispatch now signs internal service actor headers with `quant.security.service-actor.secret`; research-task-service only promotes service/system provenance when `ServiceActorContextFilter` validates that signature.
- Fix: event auto dispatch now fails closed with `EVENT_AUTO_TRIGGER_SERVICE_IDENTITY_NOT_CONFIGURED` when `quant.security.service-actor.secret` is missing, instead of silently falling back to human-like `system/ADMIN` provenance.
- Added `quant.security.service-actor.secret` to local and docker resource configuration for both ai-orchestration-service and research-task-service.
- Fix: external task creation DTOs no longer expose or trust `actorProvenance`; research-task dispatch provenance is always constructed server-side from `UserContext` for this boundary.
- Fix: request body `actorProvenance` is not read from `requestPayload` by the outbox path, so human callers cannot forge `SERVICE_PRINCIPAL`, system actor, or delegated actor identity through task creation payloads.
- Updated audit/message log persistence to record:
  - `identity_source`
  - `role_source`
  - `service_principal`
  - audit `original_actor_id`
  - audit `delegated_actor_id`

## Files Changed By This Window

Core message contract:

- `quant-ai-platform/quant-services/quant-common/quant-common-model/src/main/java/com/quant/common/model/message/AiTaskActorProvenance.java`
- `quant-ai-platform/quant-services/quant-common/quant-common-model/src/main/java/com/quant/common/model/message/AiTaskActorProvenanceSupport.java`
- `quant-ai-platform/quant-services/quant-common/quant-common-model/src/main/java/com/quant/common/model/message/AiTaskDispatchMessage.java`
- `quant-ai-platform/quant-services/quant-common/quant-common-model/src/main/java/com/quant/common/model/message/AiTaskStatusMessage.java`
- `quant-ai-platform/quant-services/quant-common/quant-common-model/src/main/java/com/quant/common/model/message/AiTaskResultMessage.java`
- `quant-ai-platform/quant-services/quant-common/quant-common-model/src/main/java/com/quant/common/model/message/AiTaskAuditMessage.java`

Java service paths:

- `quant-ai-platform/quant-services/quant-business/research-task-service/src/main/java/com/quant/task/domain/dto/CreateResearchTaskDTO.java`
- `quant-ai-platform/quant-services/quant-business/research-task-service/src/main/java/com/quant/task/service/impl/TaskOutboxMessageServiceImpl.java`
- `quant-ai-platform/quant-services/quant-business/research-task-service/src/main/java/com/quant/task/config/CommonInfraConfig.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/config/CommonInfraConfig.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/EventAutoTaskDispatchServiceImpl.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/resources/application-local.yml`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/resources/application-docker.yml`
- `quant-ai-platform/quant-services/quant-business/research-task-service/src/main/resources/application-local.yml`
- `quant-ai-platform/quant-services/quant-business/research-task-service/src/main/resources/application-docker.yml`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/TaskRetryServiceImpl.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/TaskControlServiceImpl.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/consumer/AiTaskAuditConsumer.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/TaskMessageLogServiceImpl.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/domain/entity/AuditRecordDO.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/domain/entity/TaskMessageLogDO.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/domain/vo/AuditRecordVO.java`

Python AI engine:

- `quant-ai-platform/quant-ai-engine/app/messaging/message_models.py`
- `quant-ai-platform/quant-ai-engine/app/messaging/kafka_consumer.py`
- `quant-ai-platform/quant-ai-engine/app/messaging/kafka_producer.py`
- `quant-ai-platform/quant-ai-engine/app/graph/node_executor.py`
- `quant-ai-platform/quant-ai-engine/app/graph/state.py`

Database:

- `quant-ai-platform/docker/mysql/init/002_init.sql`
- `quant-ai-platform/docker/mysql/init/009_domain_foundation.sql`
- `quant-ai-platform/docker/mysql/init/015_service_identity_audit.sql`

Tests:

- `quant-ai-platform/quant-services/quant-common/quant-common-security/src/test/java/com/quant/common/security/ServiceActorContextFilterTests.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/EventAutoTaskDispatchServiceTests.java`
- `quant-ai-platform/quant-services/quant-common/quant-common-model/src/test/java/com/quant/quantcommonmodel/AiTaskMessageContractTests.java`
- `quant-ai-platform/quant-services/quant-business/research-task-service/src/test/java/com/quant/task/service/AiTaskDispatchCompatibilityTests.java`
- `quant-ai-platform/quant-services/quant-business/research-task-service/src/test/java/com/quant/task/service/TaskOutboxMessageServiceTests.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/TaskRetryServiceTests.java`
- `quant-ai-platform/quant-ai-engine/tests/test_ai_task_message_contract.py`

## Compatibility Notes

- Existing Kafka topic names are unchanged.
- New AI task payload field is optional: `actorProvenance`.
- Old messages without `actorProvenance` still deserialize in Java and Python.
- Existing Python Pydantic models keep old required fields unchanged.
- Existing Java payload fields are unchanged, with the optional provenance field appended.
- Existing message logs and audit records can continue to contain null identity fields for old data.
- External create-task requests cannot set service principal by payload.
- Internal auto dispatch requires both services to share `quant.security.service-actor.secret`; unsigned or invalid service actor headers are ignored by the receiver, and missing sender secret fails closed before dispatch.

## Verification

Passed:

- `python -c "import ast, pathlib; files=[...]; [compile(ast.parse(pathlib.Path(f).read_text(encoding='utf-8')), f, 'exec') for f in files]; print('syntax ok')"`
- `python -m unittest tests.test_ai_task_message_contract`
- `python -m unittest discover tests`
- `mvn -q -pl quant-common/quant-common-model test`
- `mvn -q -pl quant-common/quant-common-security -Dtest=ServiceActorContextFilterTests test`
- `mvn -q -pl quant-business/ai-orchestration-service -am -Dtest=EventAutoTaskDispatchServiceTests '-Dsurefire.failIfNoSpecifiedTests=false' test`
- `mvn -q -pl quant-business/research-task-service -am -Dtest=AiTaskDispatchCompatibilityTests '-Dsurefire.failIfNoSpecifiedTests=false' test`
- `mvn -q -pl quant-business/research-task-service -am '-Dtest=TaskOutboxMessageServiceTests,AiTaskDispatchCompatibilityTests' '-Dsurefire.failIfNoSpecifiedTests=false' test`
- `mvn -q -pl quant-business/ai-orchestration-service -am -Dtest=TaskRetryServiceTests '-Dsurefire.failIfNoSpecifiedTests=false' test`
- `mvn -q test`

Notes:

- `python -m py_compile ...` was attempted first, but Windows denied writing `__pycache__`; the AST compile command above was used instead and passed.
- `mvn -q test` prints an expected stack trace from a test that simulates `kafka down`; Maven still exited successfully.

## Worktree Note

The worktree had pre-existing dirty and untracked Phase 2/security files before this implementation. This handoff only claims the Service-To-Service Identity changes listed above.
