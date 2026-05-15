package com.quant.common.redis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RedisKeyBuilderContractTests {

    @Test
    void buildsTaskBusinessCacheKeys() {
        assertEquals("task:state:task-1", RedisKeyBuilder.taskState("task-1"));
        assertEquals("task:result:task-1", RedisKeyBuilder.taskResult("task-1"));
        assertEquals("task:full:task-1", RedisKeyBuilder.taskFull("task-1"));
        assertEquals("task:meta:task-1", RedisKeyBuilder.taskMeta("task-1"));
    }

    @Test
    void exposesTaskRuntimeAndListKeyContracts() {
        assertEquals("task:control:task-1", RedisKeyBuilder.taskControl("task-1"));
        assertEquals("task:stats:global", RedisKeyConstants.TASK_STATS_GLOBAL);
        assertEquals("task:list:version", RedisKeyConstants.TASK_LIST_VERSION);
        assertEquals("task:list:", RedisKeyConstants.TASK_LIST_CACHE_PREFIX);
    }

    @Test
    void buildsSignalProjectionCacheKeys() {
        assertEquals("signal:latest:600519", RedisKeyBuilder.signalLatest("600519"));
        assertEquals("signal:ranking:2026-05-15", RedisKeyBuilder.signalRanking("2026-05-15"));
    }
}
