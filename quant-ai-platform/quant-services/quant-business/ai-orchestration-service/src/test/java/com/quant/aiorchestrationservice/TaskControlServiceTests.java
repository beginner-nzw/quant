package com.quant.aiorchestrationservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.dto.TaskCancelDTO;
import com.quant.aiorchestrator.domain.entity.AuditRecordDO;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.aiorchestrator.manager.TaskCacheVersionManager;
import com.quant.aiorchestrator.manager.TaskStateManager;
import com.quant.aiorchestrator.manager.TaskTraceManager;
import com.quant.aiorchestrator.mapper.AuditRecordMapper;
import com.quant.aiorchestrator.mapper.ResearchTaskMapper;
import com.quant.aiorchestrator.service.impl.TaskControlServiceImpl;
import com.quant.common.core.exception.BizException;
import com.quant.common.model.TaskDomainConstants;
import com.quant.common.model.enums.TaskStageEnum;
import com.quant.common.model.enums.TaskStatusEnum;
import com.quant.common.redis.RedisKeyBuilder;
import com.quant.common.redis.RedisKeyConstants;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

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

        AuditRecordDO audit = deps.auditRecordMapperState.inserted;
        assertNotNull(audit);
        assertEquals("task-1", audit.getTaskId());
        assertEquals(TaskDomainConstants.AuditType.TASK_CONTROL.name(), audit.getAuditType());
        assertEquals(TaskDomainConstants.AuditActionCode.TASK_CANCEL.name(), audit.getActionCode());
        assertEquals("operator-1", audit.getOperatorId());
        assertEquals("manual \"risk\" cancel {quoted}\nline", audit.getActionDesc());

        assertEquals("wf-task-1", deps.taskTraceManager.workflowInstanceId);
        assertEquals(TaskStageEnum.CANCELLED.name(), deps.taskTraceManager.finalNode);
        assertEquals(TaskStatusEnum.CANCELLED.name(), deps.taskTraceManager.finalStatus);
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
        assertFalse(deps.auditRecordMapperState.insertCalled);
        assertFalse(deps.taskTraceManager.finishCalled);
    }

    private static ResearchTaskDO buildRunningTask() {
        ResearchTaskDO task = new ResearchTaskDO();
        task.setTaskId("task-1");
        task.setStatus(TaskStatusEnum.RUNNING.name());
        task.setDeleted(0);
        return task;
    }

    private static class Dependencies {
        private final FakeResearchTaskMapper researchTaskMapperState = new FakeResearchTaskMapper();
        private final FakeAuditRecordMapper auditRecordMapperState = new FakeAuditRecordMapper();
        private final FakeStringRedisTemplate stringRedisTemplate = new FakeStringRedisTemplate();
        private final FakeTaskCacheVersionManager taskCacheVersionManager = new FakeTaskCacheVersionManager();
        private final FakeTaskTraceManager taskTraceManager = new FakeTaskTraceManager();
        private final TaskControlServiceImpl service = new TaskControlServiceImpl(
                researchTaskMapperState.proxy(),
                auditRecordMapperState.proxy(),
                new TaskStateManager(),
                stringRedisTemplate,
                OBJECT_MAPPER,
                taskCacheVersionManager,
                taskTraceManager
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

    private static class FakeAuditRecordMapper {
        private boolean insertCalled;
        private AuditRecordDO inserted;

        private AuditRecordMapper proxy() {
            return (AuditRecordMapper) Proxy.newProxyInstance(
                    AuditRecordMapper.class.getClassLoader(),
                    new Class<?>[]{AuditRecordMapper.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("insert")) {
                            insertCalled = true;
                            inserted = (AuditRecordDO) args[0];
                            return 1;
                        }
                        return defaultReturn(method.getReturnType());
                    }
            );
        }
    }

    private static class FakeStringRedisTemplate extends StringRedisTemplate {
        private final List<SetCall> setCalls = new ArrayList<>();
        private final List<String> deletedKeys = new ArrayList<>();

        @Override
        public ValueOperations<String, String> opsForValue() {
            return (ValueOperations<String, String>) Proxy.newProxyInstance(
                    ValueOperations.class.getClassLoader(),
                    new Class<?>[]{ValueOperations.class},
                    (proxy, method, args) -> {
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

    private static class FakeTaskTraceManager extends TaskTraceManager {
        private boolean finishCalled;
        private String workflowInstanceId;
        private String finalNode;
        private String finalStatus;

        FakeTaskTraceManager() {
            super(null, null, null, null, null);
        }

        @Override
        public void finishWorkflow(String workflowInstanceId, String finalNode, String finalStatus) {
            finishCalled = true;
            this.workflowInstanceId = workflowInstanceId;
            this.finalNode = finalNode;
            this.finalStatus = finalStatus;
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
