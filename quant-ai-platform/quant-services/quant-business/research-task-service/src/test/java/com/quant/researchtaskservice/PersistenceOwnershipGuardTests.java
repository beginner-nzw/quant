package com.quant.researchtaskservice;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
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
