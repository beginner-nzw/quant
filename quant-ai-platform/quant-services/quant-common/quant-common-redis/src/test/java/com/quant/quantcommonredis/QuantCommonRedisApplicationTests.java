package com.quant.quantcommonredis;

import com.quant.common.redis.RedisKeyBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuantCommonRedisApplicationTests {

    @Test
    void buildsTaskRedisKeys() {
        assertEquals("task:state:task-1", RedisKeyBuilder.taskState("task-1"));
        assertEquals("task:result:task-1", RedisKeyBuilder.taskResult("task-1"));
        assertEquals("task:full:task-1", RedisKeyBuilder.taskFull("task-1"));
        assertEquals("signal:latest:600519", RedisKeyBuilder.signalLatest("600519"));
        assertEquals("signal:ranking:2026-05-15", RedisKeyBuilder.signalRanking("2026-05-15"));
    }

}
