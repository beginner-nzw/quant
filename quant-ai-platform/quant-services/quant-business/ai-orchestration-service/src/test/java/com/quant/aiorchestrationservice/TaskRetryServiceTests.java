package com.quant.aiorchestrationservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.dto.TaskRetryDTO;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.aiorchestrator.domain.entity.ResearchTaskRetryLogDO;
import com.quant.aiorchestrator.domain.entity.TaskMessageLogDO;
import com.quant.aiorchestrator.manager.TaskCacheVersionManager;
import com.quant.aiorchestrator.manager.TaskStateManager;
import com.quant.aiorchestrator.mapper.ResearchTaskMapper;
import com.quant.aiorchestrator.mapper.ResearchTaskRetryLogMapper;
import com.quant.aiorchestrator.mapper.TaskMessageLogMapper;
import com.quant.aiorchestrator.service.TaskMessageLogService;
import com.quant.aiorchestrator.service.impl.TaskRetryServiceImpl;
import com.quant.common.core.exception.BizException;
import com.quant.common.messaging.KafkaTopicConstants;
import com.quant.common.messaging.MessageTypeConstants;
import com.quant.common.model.TaskDomainConstants;
import com.quant.common.model.enums.TaskStageEnum;
import com.quant.common.model.enums.TaskStatusEnum;
import com.quant.common.model.message.AiTaskDispatchMessage;
import com.quant.common.model.message.MessageEnvelope;
import com.quant.common.redis.RedisKeyBuilder;
import com.quant.common.redis.RedisKeyConstants;
import org.apache.kafka.clients.producer.Producer;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.SendResult;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskRetryServiceTests {

    @Test
    void retryDispatchUsesSharedAiTaskDispatchContractAndRecordsProducedLog() {
        Dependencies deps = new Dependencies();
        ResearchTaskDO task = buildFailedTask();
        task.setRetryCount(1);
        deps.researchTaskMapperState.task = task;
        deps.researchTaskMapperState.updateResult = 1;

        TaskRetryDTO dto = new TaskRetryDTO();
        dto.setRetryReason("manual retry");
        dto.setOperatorId("operator-1");

        assertEquals("task-1", deps.service.retryTask("task-1", dto));

        ResearchTaskRetryLogDO insertedLog = deps.retryLogMapperState.inserted;
        assertNotNull(insertedLog);
        assertEquals("task-1", insertedLog.getTaskId());
        assertEquals(2, insertedLog.getRetryNo());
        assertEquals(TaskDomainConstants.RetryStatus.DISPATCHED.name(), insertedLog.getRetryStatus());

        assertEquals("task-1", deps.researchTaskMapperState.updatedTaskId);
        assertEquals(TaskStatusEnum.FAILED.name(), deps.researchTaskMapperState.expectedStatus);
        assertEquals(1, deps.researchTaskMapperState.expectedRetryCount);
        assertEquals(2, deps.researchTaskMapperState.nextRetryCount);
        assertEquals(TaskStatusEnum.DISPATCHED.name(), deps.researchTaskMapperState.nextStatus);
        assertEquals(TaskStageEnum.RETRY_DISPATCHED.name(), deps.researchTaskMapperState.nextStage);

        assertEquals(1, deps.kafkaTemplate.sent.size());
        FakeKafkaTemplate.SentRecord sent = deps.kafkaTemplate.sent.get(0);
        assertEquals(KafkaTopicConstants.AI_TASK_DISPATCH, sent.topic);
        assertEquals("task-1", sent.key);

        assertEquals(1, deps.taskMessageLogService.produced.size());
        AiTaskDispatchMessage message = (AiTaskDispatchMessage) deps.taskMessageLogService.produced.get(0).message;
        assertNotNull(message.getMessageId());
        assertEquals("trace-1", message.getTraceId());
        assertEquals("task-1", message.getTaskId());
        assertEquals("event-1", message.getEventId());
        assertEquals(MessageTypeConstants.AI_TASK_DISPATCH, message.getMessageType());
        assertEquals("ai-orchestration-service", message.getSourceService());
        assertEquals("python-ai-engine", message.getTargetService());
        assertEquals("tenant-1", message.getTenantId());
        assertEquals("STOCK:000001", message.getBizKey());
        assertEquals("1.0", message.getVersion());
        assertEquals(2, message.getRetryCount());

        AiTaskDispatchMessage.AiTaskDispatchPayload payload = message.getPayload();
        assertEquals("EQUITY_RESEARCH", payload.getTaskType());
        assertEquals("Ping An Bank analysis", payload.getTaskTitle());
        assertEquals("STOCK", payload.getTargetType());
        assertEquals("000001", payload.getTargetCode());
        assertEquals("Ping An Bank", payload.getTargetName());
        assertEquals("HIGH", payload.getPriority());
        assertEquals("source-task-1", payload.getSourceTaskId());
        assertEquals("source-report-1", payload.getSourceReportId());
        assertEquals("event-1", payload.getSourceEventId());
        assertEquals("MARKET_EVENT", payload.getSourceDomain());
        assertEquals("APPROVED", payload.getSourceReviewStatus());
        assertEquals("FULL", payload.getAnalysisScope());

        assertEquals(2, deps.stringRedisTemplate.deletedKeys.size());
        assertTrue(deps.stringRedisTemplate.deletedKeys.contains(RedisKeyBuilder.taskFull("task-1")));
        assertTrue(deps.stringRedisTemplate.deletedKeys.contains(RedisKeyConstants.TASK_STATS_GLOBAL));
        assertTrue(deps.taskCacheVersionManager.bumped);
    }

    @Test
    void retryDispatchStopsWhenTaskStateChangedBeforeConditionalUpdate() {
        Dependencies deps = new Dependencies();
        ResearchTaskDO task = buildFailedTask();
        task.setRetryCount(1);
        deps.researchTaskMapperState.task = task;
        deps.researchTaskMapperState.updateResult = 0;

        assertThrows(BizException.class, () -> deps.service.retryTask("task-1", null));

        assertTrue(deps.kafkaTemplate.sent.isEmpty());
        assertTrue(deps.taskMessageLogService.produced.isEmpty());
        assertTrue(deps.taskMessageLogService.failed.isEmpty());
        assertFalse(deps.retryLogMapperState.updateCalled);
        assertFalse(deps.taskCacheVersionManager.bumped);
    }

    private ResearchTaskDO buildFailedTask() {
        ResearchTaskDO task = new ResearchTaskDO();
        task.setTaskId("task-1");
        task.setTraceId("trace-1");
        task.setTenantId("tenant-1");
        task.setStatus(TaskStatusEnum.FAILED.name());
        task.setTaskType("EQUITY_RESEARCH");
        task.setTaskTitle("Ping An Bank analysis");
        task.setTargetType("STOCK");
        task.setTargetCode("000001");
        task.setTargetName("Ping An Bank");
        task.setPriority("HIGH");
        task.setSourceTaskId("source-task-1");
        task.setSourceReportId("source-report-1");
        task.setSourceEventId("event-1");
        task.setSourceDomain("MARKET_EVENT");
        task.setSourceReviewStatus("APPROVED");
        task.setAnalysisScope("FULL");
        return task;
    }

    private static class Dependencies {
        private final FakeResearchTaskMapper researchTaskMapperState = new FakeResearchTaskMapper();
        private final ResearchTaskMapper researchTaskMapper = researchTaskMapperState.proxy();
        private final FakeResearchTaskRetryLogMapper retryLogMapperState = new FakeResearchTaskRetryLogMapper();
        private final ResearchTaskRetryLogMapper retryLogMapper = retryLogMapperState.proxy();
        private final FakeKafkaTemplate kafkaTemplate = new FakeKafkaTemplate();
        private final FakeStringRedisTemplate stringRedisTemplate = new FakeStringRedisTemplate();
        private final FakeTaskCacheVersionManager taskCacheVersionManager = new FakeTaskCacheVersionManager();
        private final FakeTaskMessageLogService taskMessageLogService = new FakeTaskMessageLogService();
        private final TaskRetryServiceImpl service = new TaskRetryServiceImpl(
                researchTaskMapper,
                retryLogMapper,
                kafkaTemplate,
                new ObjectMapper(),
                stringRedisTemplate,
                taskCacheVersionManager,
                new TaskStateManager(),
                taskMessageLogService
        );
    }

    private static class FakeResearchTaskMapper {
        private ResearchTaskDO task;
        private int updateResult;
        private String updatedTaskId;
        private String expectedStatus;
        private Integer expectedRetryCount;
        private Integer nextRetryCount;
        private String nextStatus;
        private String nextStage;

        private ResearchTaskMapper proxy() {
            return (ResearchTaskMapper) Proxy.newProxyInstance(
                    ResearchTaskMapper.class.getClassLoader(),
                    new Class<?>[]{ResearchTaskMapper.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("selectOne")) {
                            return task;
                        }
                        if (method.getName().equals("updateTaskRetryDispatched")) {
                            this.updatedTaskId = (String) args[0];
                            this.expectedStatus = (String) args[1];
                            this.expectedRetryCount = (Integer) args[2];
                            this.nextRetryCount = (Integer) args[3];
                            this.nextStatus = (String) args[4];
                            this.nextStage = (String) args[5];
                            return updateResult;
                        }
                        return defaultReturn(method.getReturnType());
                    }
            );
        }
    }

    private static class FakeResearchTaskRetryLogMapper {
        private ResearchTaskRetryLogDO inserted;
        private boolean updateCalled;

        private ResearchTaskRetryLogMapper proxy() {
            return (ResearchTaskRetryLogMapper) Proxy.newProxyInstance(
                    ResearchTaskRetryLogMapper.class.getClassLoader(),
                    new Class<?>[]{ResearchTaskRetryLogMapper.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("insert")) {
                            this.inserted = (ResearchTaskRetryLogDO) args[0];
                            return 1;
                        }
                        if (method.getName().equals("updateById")) {
                            this.updateCalled = true;
                            return 1;
                        }
                        return defaultReturn(method.getReturnType());
                    }
            );
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

    private static class FakeStringRedisTemplate extends StringRedisTemplate {
        private final List<String> deletedKeys = new ArrayList<>();

        @Override
        public Boolean delete(String key) {
            deletedKeys.add(key);
            return true;
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

    private static class FakeTaskMessageLogService implements TaskMessageLogService {
        private final List<RecordedMessage> produced = new ArrayList<>();
        private final List<RecordedMessage> failed = new ArrayList<>();
        @Override
        public void recordProduced(String topicName, MessageEnvelope message) {
            produced.add(new RecordedMessage(topicName, message, null));
        }

        @Override
        public void recordFailed(String topicName, MessageEnvelope message, String errorMessage) {
            failed.add(new RecordedMessage(topicName, message, errorMessage));
        }
        @Override
        public boolean beginConsume(String topicName, MessageEnvelope message, String consumerService) {
            return true;
        }

        @Override
        public void recordConsumed(String topicName, MessageEnvelope message, String consumerService) {
        }

        @Override
        public void recordSkipped(String topicName, MessageEnvelope message, String consumerService, String reason) {
        }

        @Override
        public void recordFailed(String topicName, MessageEnvelope message, String consumerService, String errorMessage) {
            failed.add(new RecordedMessage(topicName, message, errorMessage));
        }

        private record RecordedMessage(String topicName, MessageEnvelope message, String errorMessage) {
        }
    }

    private static TaskMessageLogMapper noopTaskMessageLogMapper() {
        return (TaskMessageLogMapper) Proxy.newProxyInstance(
                TaskMessageLogMapper.class.getClassLoader(),
                new Class<?>[]{TaskMessageLogMapper.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("toString")) {
                        return "noopTaskMessageLogMapper";
                    }
                    if (method.getName().equals("insert")) {
                        return 1;
                    }
                    if (method.getReturnType().equals(int.class)) {
                        return 0;
                    }
                    if (method.getReturnType().equals(long.class)) {
                        return 0L;
                    }
                    if (method.getReturnType().equals(boolean.class)) {
                        return false;
                    }
                    return null;
                }
        );
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
