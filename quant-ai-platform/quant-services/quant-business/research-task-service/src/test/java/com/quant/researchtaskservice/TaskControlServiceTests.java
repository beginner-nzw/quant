package com.quant.researchtaskservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.dto.TaskCancelDTO;
import com.quant.aiorchestrator.domain.dto.TaskWorkflowControlDTO;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.aiorchestrator.manager.TaskControlCommandManager;
import com.quant.aiorchestrator.manager.TaskControlDispatchManager;
import com.quant.aiorchestrator.manager.TaskControlRuntimeManager;
import com.quant.aiorchestrator.manager.TaskControlTaskLoaderManager;
import com.quant.aiorchestrator.manager.TaskStateManager;
import com.quant.aiorchestrator.mapper.ResearchTaskMapper;
import com.quant.aiorchestrator.service.impl.TaskControlServiceImpl;
import com.quant.common.core.exception.BizException;
import com.quant.common.messaging.KafkaTopicConstants;
import com.quant.common.model.message.AiTaskDispatchMessage;
import com.quant.common.model.message.MessageEnvelope;
import com.quant.common.model.TaskDomainConstants;
import com.quant.common.model.enums.TaskStageEnum;
import com.quant.common.model.enums.TaskStatusEnum;
import com.quant.common.redis.RedisKeyBuilder;
import com.quant.common.redis.RedisKeyConstants;
import com.quant.task.domain.entity.TaskOutboxMessageDO;
import com.quant.task.manager.TaskCacheVersionManager;
import com.quant.task.port.TaskControlAuditAppender;
import com.quant.task.port.TaskWorkflowTraceFinisher;
import com.quant.task.service.TaskMessageLogService;
import org.apache.kafka.clients.producer.Producer;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.SendResult;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskControlServiceTests {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void cancelWritesRuntimeSignalJsonThroughSharedRedisKeyBuilder() throws Exception {
        Dependencies deps = new Dependencies();
        deps.researchTaskMapperState.task = buildRunningTask();

        TaskCancelDTO dto = new TaskCancelDTO();
        dto.setCancelReason("manual \"risk\" cancel {quoted}\nline");
        dto.setOperatorId("operator-1");

        assertEquals("task-1", deps.service.cancelTask("task-1", dto));

        assertEquals("task-1", deps.researchTaskMapperState.cancelledTaskId);
        assertEquals("manual \"risk\" cancel {quoted}\nline", deps.researchTaskMapperState.cancelReason);

        FakeStringRedisTemplate.SetCall runtimeSignal = deps.stringRedisTemplate.setCalls.stream()
                .filter(call -> RedisKeyBuilder.taskControl("task-1").equals(call.key()))
                .findFirst()
                .orElseThrow();
        assertEquals(Duration.ofHours(24), runtimeSignal.timeout());

        JsonNode signal = OBJECT_MAPPER.readTree(runtimeSignal.value());
        assertTrue(signal.get("cancelled").asBoolean());
        assertEquals("manual \"risk\" cancel {quoted}\nline", signal.get("reason").asText());

        FakeStringRedisTemplate.SetCall taskState = deps.stringRedisTemplate.setCalls.stream()
                .filter(call -> RedisKeyBuilder.taskState("task-1").equals(call.key()))
                .findFirst()
                .orElseThrow();
        JsonNode state = OBJECT_MAPPER.readTree(taskState.value());
        assertEquals(TaskStatusEnum.CANCELLED.name(), state.get("status").asText());
        assertEquals(TaskStageEnum.CANCELLED.name(), state.get("currentStage").asText());
        assertEquals(100, state.get("progress").asInt());

        assertTrue(deps.stringRedisTemplate.deletedKeys.contains(RedisKeyBuilder.taskFull("task-1")));
        assertTrue(deps.stringRedisTemplate.deletedKeys.contains(RedisKeyConstants.TASK_STATS_GLOBAL));
        assertTrue(deps.taskCacheVersionManager.bumped);

        FakeTaskControlAuditAppender.InsertedAudit audit = deps.auditAppender.inserted;
        assertNotNull(audit);
        assertEquals("task-1", audit.taskId());
        assertEquals(TaskDomainConstants.AuditType.TASK_CONTROL.name(), audit.auditType());
        assertEquals(TaskDomainConstants.AuditActionCode.TASK_CANCEL.name(), audit.actionCode());
        assertEquals("operator-1", audit.operatorId());
        assertEquals("manual \"risk\" cancel {quoted}\nline", audit.actionDesc());

        assertEquals("wf-task-1", deps.traceFinisher.workflowInstanceId);
        assertEquals(TaskStageEnum.CANCELLED.name(), deps.traceFinisher.finalNode);
        assertEquals(TaskStatusEnum.CANCELLED.name(), deps.traceFinisher.finalStatus);
    }

    @Test
    void cancelStopsBeforeRuntimeSignalWhenTaskIsAlreadyFinal() {
        Dependencies deps = new Dependencies();
        ResearchTaskDO task = buildRunningTask();
        task.setStatus(TaskStatusEnum.SUCCESS.name());
        deps.researchTaskMapperState.task = task;

        assertThrows(BizException.class, () -> deps.service.cancelTask("task-1", null));

        assertFalse(deps.researchTaskMapperState.updateCancelledCalled);
        assertTrue(deps.stringRedisTemplate.setCalls.isEmpty());
        assertTrue(deps.stringRedisTemplate.deletedKeys.isEmpty());
        assertFalse(deps.taskCacheVersionManager.bumped);
        assertFalse(deps.auditAppender.insertCalled);
        assertFalse(deps.traceFinisher.finishCalled);
    }

    @Test
    void resumeWritesRuntimeSignalAndDispatchesFromCheckpointWithoutIncrementingRetry() throws Exception {
        Dependencies deps = new Dependencies();
        ResearchTaskDO task = buildRunningTask();
        task.setTraceId("trace-1");
        task.setTenantId("tenant-1");
        task.setTaskType("EQUITY_RESEARCH");
        task.setTaskTitle("resume task");
        task.setTargetType("STOCK");
        task.setTargetCode("000001");
        task.setTargetName("Ping An Bank");
        task.setPriority("HIGH");
        task.setRetryCount(3);
        deps.researchTaskMapperState.task = task;
        deps.stringRedisTemplate.getValues.put(
                RedisKeyBuilder.taskWorkflowCheckpoint("task-1"),
                "{\"currentNode\":\"risk_review_agent\",\"status\":\"FAILED\"}"
        );

        TaskWorkflowControlDTO dto = new TaskWorkflowControlDTO();
        dto.setOperatorId("operator-1");
        dto.setReason("timeout resume");

        assertEquals("task-1", deps.service.resumeTask("task-1", dto));

        FakeStringRedisTemplate.SetCall controlSignal = deps.stringRedisTemplate.setCalls.stream()
                .filter(call -> RedisKeyBuilder.taskControl("task-1").equals(call.key()))
                .findFirst()
                .orElseThrow();
        JsonNode signal = OBJECT_MAPPER.readTree(controlSignal.value());
        assertEquals("RESUME", signal.get("action").asText());
        assertEquals("timeout resume", signal.get("reason").asText());

        assertEquals(1, deps.kafkaTemplate.sent.size());
        assertEquals(KafkaTopicConstants.AI_TASK_DISPATCH, deps.kafkaTemplate.sent.get(0).topic);
        AiTaskDispatchMessage message = (AiTaskDispatchMessage) deps.taskMessageLogService.produced.get(0);
        assertEquals(3, message.getRetryCount());
        assertEquals("operator-1", message.getPayload().getActorProvenance().getOriginalActor().getActorId());
        assertEquals("research-task-service", message.getSourceService());
        assertTrue(deps.stringRedisTemplate.deletedKeys.contains(RedisKeyBuilder.taskFull("task-1")));
        assertTrue(deps.taskCacheVersionManager.bumped);
    }

    @Test
    void rerunRequiresPersistedNodeState() {
        Dependencies deps = new Dependencies();
        deps.researchTaskMapperState.task = buildRunningTask();

        TaskWorkflowControlDTO dto = new TaskWorkflowControlDTO();
        dto.setNodeName("risk_review_agent");

        assertThrows(BizException.class, () -> deps.service.rerunNode("task-1", dto));
        assertTrue(deps.kafkaTemplate.sent.isEmpty());
    }

    @Test
    void rerunWritesNodeRuntimeSignal() throws Exception {
        Dependencies deps = new Dependencies();
        deps.researchTaskMapperState.task = buildRunningTask();
        deps.stringRedisTemplate.getValues.put(
                RedisKeyBuilder.taskWorkflowNode("task-1", "risk_review_agent"),
                "{\"nodeName\":\"risk_review_agent\",\"status\":\"SUCCESS\"}"
        );

        TaskWorkflowControlDTO dto = new TaskWorkflowControlDTO();
        dto.setNodeName("risk_review_agent");
        dto.setOperatorId("operator-1");

        assertEquals("task-1", deps.service.rerunNode("task-1", dto));

        FakeStringRedisTemplate.SetCall controlSignal = deps.stringRedisTemplate.setCalls.stream()
                .filter(call -> RedisKeyBuilder.taskControl("task-1").equals(call.key()))
                .findFirst()
                .orElseThrow();
        JsonNode signal = OBJECT_MAPPER.readTree(controlSignal.value());
        assertEquals("RERUN_NODE", signal.get("action").asText());
        assertEquals("risk_review_agent", signal.get("nodeName").asText());
        assertEquals(1, deps.kafkaTemplate.sent.size());
    }

    private static ResearchTaskDO buildRunningTask() {
        ResearchTaskDO task = new ResearchTaskDO();
        task.setTaskId("task-1");
        task.setStatus(TaskStatusEnum.RUNNING.name());
        task.setDeleted(0);
        task.setTraceId("trace-1");
        task.setTenantId("tenant-1");
        task.setTaskType("EQUITY_RESEARCH");
        task.setTargetType("STOCK");
        task.setTargetCode("000001");
        task.setTargetName("Ping An Bank");
        task.setPriority("HIGH");
        task.setRetryCount(0);
        return task;
    }

    private static class Dependencies {
        private final FakeResearchTaskMapper researchTaskMapperState = new FakeResearchTaskMapper();
        private final FakeTaskControlAuditAppender auditAppender = new FakeTaskControlAuditAppender();
        private final FakeStringRedisTemplate stringRedisTemplate = new FakeStringRedisTemplate();
        private final FakeTaskCacheVersionManager taskCacheVersionManager = new FakeTaskCacheVersionManager();
        private final FakeTaskWorkflowTraceFinisher traceFinisher = new FakeTaskWorkflowTraceFinisher();
        private final FakeKafkaTemplate kafkaTemplate = new FakeKafkaTemplate();
        private final FakeTaskMessageLogService taskMessageLogService = new FakeTaskMessageLogService();
        private final ResearchTaskMapper researchTaskMapper = researchTaskMapperState.proxy();
        private final TaskStateManager taskStateManager = new TaskStateManager();
        private final TaskControlRuntimeManager runtimeManager = new TaskControlRuntimeManager(
                stringRedisTemplate,
                OBJECT_MAPPER,
                taskCacheVersionManager
        );
        private final TaskControlDispatchManager dispatchManager = new TaskControlDispatchManager(
                OBJECT_MAPPER,
                kafkaTemplate,
                taskMessageLogService
        );
        private final TaskControlCommandManager commandManager = new TaskControlCommandManager(
                researchTaskMapper,
                taskStateManager,
                traceFinisher,
                runtimeManager,
                dispatchManager,
                auditAppender
        );
        private final TaskControlServiceImpl service = new TaskControlServiceImpl(
                new TaskControlTaskLoaderManager(researchTaskMapper),
                commandManager
        );
    }

    private static class FakeResearchTaskMapper {
        private ResearchTaskDO task;
        private boolean updateCancelledCalled;
        private String cancelledTaskId;
        private String cancelReason;

        private ResearchTaskMapper proxy() {
            return (ResearchTaskMapper) Proxy.newProxyInstance(
                    ResearchTaskMapper.class.getClassLoader(),
                    new Class<?>[]{ResearchTaskMapper.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("selectOne")) {
                            return task;
                        }
                        if (method.getName().equals("updateTaskCancelled")) {
                            updateCancelledCalled = true;
                            cancelledTaskId = (String) args[0];
                            cancelReason = (String) args[1];
                            return 1;
                        }
                        return defaultReturn(method.getReturnType());
                    }
            );
        }
    }

    private static class FakeTaskControlAuditAppender implements TaskControlAuditAppender {
        private boolean insertCalled;
        private InsertedAudit inserted;

        @Override
        public void recordCancelAudit(String taskId, TaskCancelDTO dto, String cancelReason) {
            insertCalled = true;
            inserted = new InsertedAudit(
                    taskId,
                    TaskDomainConstants.AuditType.TASK_CONTROL.name(),
                    TaskDomainConstants.AuditActionCode.TASK_CANCEL.name(),
                    dto == null ? null : dto.getOperatorId(),
                    cancelReason
            );
        }

        private record InsertedAudit(
                String taskId,
                String auditType,
                String actionCode,
                String operatorId,
                String actionDesc
        ) {
        }
    }

    private static class FakeStringRedisTemplate extends StringRedisTemplate {
        private final List<SetCall> setCalls = new ArrayList<>();
        private final List<String> deletedKeys = new ArrayList<>();
        private final java.util.Map<String, String> getValues = new java.util.HashMap<>();

        @Override
        public ValueOperations<String, String> opsForValue() {
            return (ValueOperations<String, String>) Proxy.newProxyInstance(
                    ValueOperations.class.getClassLoader(),
                    new Class<?>[]{ValueOperations.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("get")) {
                            return getValues.get((String) args[0]);
                        }
                        if (method.getName().equals("set") && args.length == 3) {
                            setCalls.add(new SetCall((String) args[0], (String) args[1], (Duration) args[2]));
                            return null;
                        }
                        return defaultReturn(method.getReturnType());
                    }
            );
        }

        @Override
        public Boolean delete(String key) {
            deletedKeys.add(key);
            return true;
        }

        private record SetCall(String key, String value, Duration timeout) {
        }
    }

    private static class FakeTaskCacheVersionManager extends TaskCacheVersionManager {
        private boolean bumped;

        FakeTaskCacheVersionManager() {
            super(new FakeStringRedisTemplate());
        }

        @Override
        public void bumpVersion() {
            bumped = true;
        }
    }

    private static class FakeTaskWorkflowTraceFinisher implements TaskWorkflowTraceFinisher {
        private boolean finishCalled;
        private String workflowInstanceId;
        private String finalNode;
        private String finalStatus;

        @Override
        public void finishWorkflow(String workflowInstanceId, String finalNode, String finalStatus) {
            finishCalled = true;
            this.workflowInstanceId = workflowInstanceId;
            this.finalNode = finalNode;
            this.finalStatus = finalStatus;
        }
    }

    private static class FakeKafkaTemplate extends KafkaTemplate<String, String> {
        private final List<SentRecord> sent = new ArrayList<>();

        FakeKafkaTemplate() {
            super(new NoopProducerFactory());
        }

        @Override
        public CompletableFuture<SendResult<String, String>> send(String topic, String key, String data) {
            sent.add(new SentRecord(topic, key, data));
            return CompletableFuture.completedFuture(null);
        }

        private record SentRecord(String topic, String key, String data) {
        }
    }

    private static class NoopProducerFactory implements ProducerFactory<String, String> {
        @Override
        public Producer<String, String> createProducer() {
            throw new UnsupportedOperationException("not used by fake kafka template");
        }
    }

    private static class FakeTaskMessageLogService implements TaskMessageLogService {
        private final List<MessageEnvelope> produced = new ArrayList<>();

        @Override
        public void recordProduced(String topicName, MessageEnvelope message) {
            produced.add(message);
        }

        @Override
        public void recordFailed(String topicName, MessageEnvelope message, String errorMessage) {
        }

        @Override
        public void recordProduced(TaskOutboxMessageDO outbox) {
        }

        @Override
        public void recordFailed(TaskOutboxMessageDO outbox, String errorMessage) {
        }
    }

    private static Object defaultReturn(Class<?> returnType) {
        if (returnType.equals(String.class)) {
            return "";
        }
        if (returnType.equals(int.class) || returnType.equals(Integer.class)) {
            return 0;
        }
        if (returnType.equals(long.class) || returnType.equals(Long.class)) {
            return 0L;
        }
        if (returnType.equals(boolean.class) || returnType.equals(Boolean.class)) {
            return false;
        }
        return null;
    }
}
