package com.quant.task.manager;

import com.quant.common.redis.RedisKeyConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TaskCacheVersionManager {

    private final StringRedisTemplate stringRedisTemplate;

    public String currentVersion() {
        String version = stringRedisTemplate.opsForValue().get(RedisKeyConstants.TASK_LIST_VERSION);
        return version == null ? "1" : version;
    }

    public void bumpVersion() {
        stringRedisTemplate.opsForValue().increment(RedisKeyConstants.TASK_LIST_VERSION);
    }
}
