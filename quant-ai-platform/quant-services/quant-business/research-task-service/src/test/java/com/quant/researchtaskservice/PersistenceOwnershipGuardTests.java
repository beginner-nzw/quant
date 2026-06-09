package com.quant.researchtaskservice;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quant.aiorchestrator.controller.TaskQueryController;
import com.quant.aiorchestrator.domain.entity.ResearchTaskRetryLogDO;
import com.quant.aiorchestrator.manager.AiTaskResultStateManager;
import com.quant.aiorchestrator.manager.AiTaskStatusStateManager;
import com.quant.aiorchestrator.manager.FollowUpTaskSummaryManager;
import com.quant.aiorchestrator.manager.KafkaMessagePublisherManager;
import com.quant.aiorchestrator.manager.ResearchWorkbenchDispositionManager;
import com.quant.aiorchestrator.manager.ResearchWorkbenchItemAssembler;
import com.quant.aiorchestrator.manager.ResearchWorkbenchProjectionManager;
import com.quant.aiorchestrator.manager.ResearchWorkbenchReadManager;
import com.quant.aiorchestrator.manager.ResearchWorkbenchRuleManager;
import com.quant.aiorchestrator.manager.TaskControlCommandManager;
import com.quant.aiorchestrator.manager.TaskControlDispatchManager;
import com.quant.aiorchestrator.manager.TaskControlRuntimeManager;
import com.quant.aiorchestrator.manager.TaskControlTaskLoaderManager;
import com.quant.aiorchestrator.manager.TaskPageItemAssembler;
import com.quant.aiorchestrator.manager.TaskPageProjectionManager;
import com.quant.aiorchestrator.manager.TaskMessageConsumeLogManager;
import com.quant.aiorchestrator.manager.TaskMessageLogManager;
import com.quant.aiorchestrator.manager.TaskRetryCommandManager;
import com.quant.aiorchestrator.manager.TaskRetryDispatchManager;
import com.quant.aiorchestrator.manager.TaskRetryStateManager;
import com.quant.aiorchestrator.manager.TaskStateManager;
import com.quant.aiorchestrator.manager.TaskStateProjectionManager;
import com.quant.aiorchestrator.manager.TaskStatsManager;
import com.quant.aiorchestrator.service.TaskMessageLogService;
import com.quant.aiorchestrator.service.impl.TaskControlServiceImpl;
import com.quant.aiorchestrator.service.impl.TaskMessageLogServiceImpl;
import com.quant.aiorchestrator.service.impl.TaskRetryServiceImpl;
import com.quant.aiorchestrator.util.CacheKeyUtil;
import com.quant.task.domain.entity.ResearchTaskDO;
import com.quant.task.domain.entity.ResearchTaskStepDO;
import com.quant.task.domain.entity.TaskMessageLogDO;
import com.quant.task.domain.entity.TaskOutboxMessageDO;
import com.quant.task.mapper.ResearchTaskMapper;
import com.quant.task.mapper.ResearchTaskStepMapper;
import com.quant.task.mapper.TaskMessageLogMapper;
import com.quant.task.mapper.TaskOutboxMessageMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PersistenceOwnershipGuardTests {

    @Test
    void commandSideTablesRemainMappedToResearchTaskEntitiesAndMappers() {
        assertTableMapping("research_task", ResearchTaskDO.class, ResearchTaskMapper.class);
        assertTableMapping("research_task_step", ResearchTaskStepDO.class, ResearchTaskStepMapper.class);
        assertTableMapping("task_outbox_message", TaskOutboxMessageDO.class, TaskOutboxMessageMapper.class);
        assertTableMapping("task_message_log", TaskMessageLogDO.class, TaskMessageLogMapper.class);
    }

    @Test
    void legacyOrchestrationTaskTablesAreOwnedByResearchTaskModule() {
        assertTableMapping("research_task",
                com.quant.aiorchestrator.domain.entity.ResearchTaskDO.class,
                com.quant.aiorchestrator.mapper.ResearchTaskMapper.class);
        assertTableMapping("research_task_step",
                com.quant.aiorchestrator.domain.entity.ResearchTaskStepDO.class,
                com.quant.aiorchestrator.mapper.ResearchTaskStepMapper.class);
        assertTableMapping("research_task_retry_log",
                ResearchTaskRetryLogDO.class,
                com.quant.aiorchestrator.mapper.ResearchTaskRetryLogMapper.class);
        assertTableMapping("task_message_log",
                com.quant.aiorchestrator.domain.entity.TaskMessageLogDO.class,
                com.quant.aiorchestrator.mapper.TaskMessageLogMapper.class);
    }

    @Test
    void followUpTaskSummaryBelongsToResearchTaskModule() {
        assertEquals("com.quant.aiorchestrator.manager", FollowUpTaskSummaryManager.class.getPackageName());
    }

    @Test
    void researchWorkbenchRuntimeBelongsToResearchTaskModule() {
        assertEquals("com.quant.aiorchestrator.manager", ResearchWorkbenchProjectionManager.class.getPackageName());
        assertEquals(ResearchWorkbenchProjectionManager.class.getPackageName(), ResearchWorkbenchReadManager.class.getPackageName());
        assertEquals(ResearchWorkbenchProjectionManager.class.getPackageName(), ResearchWorkbenchItemAssembler.class.getPackageName());
        assertEquals(ResearchWorkbenchProjectionManager.class.getPackageName(), ResearchWorkbenchDispositionManager.class.getPackageName());
        assertEquals(ResearchWorkbenchProjectionManager.class.getPackageName(), ResearchWorkbenchRuleManager.class.getPackageName());
    }

    @Test
    void taskRetryRuntimeBelongsToResearchTaskModule() {
        assertEquals("com.quant.aiorchestrator.service.impl", TaskRetryServiceImpl.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.manager", TaskRetryCommandManager.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.manager", TaskRetryDispatchManager.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.manager", TaskRetryStateManager.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.manager", TaskStateManager.class.getPackageName());
    }

    @Test
    void taskControlRuntimeBelongsToResearchTaskModule() {
        assertEquals("com.quant.aiorchestrator.service.impl", TaskControlServiceImpl.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.manager", TaskControlCommandManager.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.manager", TaskControlDispatchManager.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.manager", TaskControlRuntimeManager.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.manager", TaskControlTaskLoaderManager.class.getPackageName());
    }

    @Test
    void taskStatsRuntimeBelongsToResearchTaskModule() {
        assertEquals("com.quant.aiorchestrator.manager", TaskStatsManager.class.getPackageName());
    }

    @Test
    void taskStateProjectionBelongsToResearchTaskModule() {
        assertEquals("com.quant.aiorchestrator.manager", TaskStateProjectionManager.class.getPackageName());
    }

    @Test
    void aiTaskMessageStateUpdatesBelongToResearchTaskModule() {
        assertEquals("com.quant.aiorchestrator.manager", AiTaskResultStateManager.class.getPackageName());
        assertEquals(AiTaskResultStateManager.class.getPackageName(), AiTaskStatusStateManager.class.getPackageName());
    }

    @Test
    void taskPageProjectionBelongsToResearchTaskModule() {
        assertEquals("com.quant.aiorchestrator.manager", TaskPageProjectionManager.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.manager", TaskPageItemAssembler.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.util", CacheKeyUtil.class.getPackageName());
    }

    @Test
    void legacyTaskMessageLogRuntimeBelongsToResearchTaskModule() {
        assertEquals("com.quant.aiorchestrator.service", TaskMessageLogService.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.service.impl", TaskMessageLogServiceImpl.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.manager", TaskMessageLogManager.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.manager", TaskMessageConsumeLogManager.class.getPackageName());
        assertEquals(TaskMessageLogManager.class.getPackageName(), KafkaMessagePublisherManager.class.getPackageName());
    }

    @Test
    void legacyTaskQueryControllerBelongsToResearchTaskModule() {
        assertEquals("com.quant.aiorchestrator.controller", TaskQueryController.class.getPackageName());
    }

    private void assertTableMapping(
            String tableName,
            Class<?> entityClass,
            Class<? extends BaseMapper<?>> mapperClass
    ) {
        TableName annotation = entityClass.getAnnotation(TableName.class);

        assertEquals(tableName, annotation.value());
        assertEquals(entityClass, baseMapperEntityClass(mapperClass));
    }

    private Class<?> baseMapperEntityClass(Class<? extends BaseMapper<?>> mapperClass) {
        return Map.of(mapperClass, mapperClass.getGenericInterfaces()).entrySet().stream()
                .flatMap(entry -> java.util.Arrays.stream(entry.getValue()))
                .filter(ParameterizedType.class::isInstance)
                .map(ParameterizedType.class::cast)
                .filter(type -> BaseMapper.class.equals(type.getRawType()))
                .map(type -> type.getActualTypeArguments()[0])
                .map(this::asClass)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No BaseMapper<T> generic found for " + mapperClass.getName()));
    }

    private Class<?> asClass(Type type) {
        if (type instanceof Class<?> clazz) {
            return clazz;
        }
        throw new AssertionError("BaseMapper<T> generic is not a class: " + type.getTypeName());
    }
}
