package com.quant.aiorchestrationservice;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quant.aiorchestrator.domain.entity.AiAgentExecutionDO;
import com.quant.aiorchestrator.domain.entity.AiWorkflowInstanceDO;
import com.quant.aiorchestrator.mapper.AiAgentExecutionMapper;
import com.quant.aiorchestrator.mapper.AiWorkflowInstanceMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PersistenceOwnershipGuardTests {

    @Test
    void orchestrationSideTablesRemainMappedToAiOrchestrationEntitiesAndMappers() {
        assertTableMapping("ai_workflow_instance", AiWorkflowInstanceDO.class, AiWorkflowInstanceMapper.class);
        assertTableMapping("ai_agent_execution", AiAgentExecutionDO.class, AiAgentExecutionMapper.class);
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
        return Arrays.stream(mapperClass.getGenericInterfaces())
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
