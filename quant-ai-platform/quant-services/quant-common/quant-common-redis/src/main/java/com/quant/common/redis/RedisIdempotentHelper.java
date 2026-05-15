package com.quant.common.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RedisIdempotentHelper {

    private final StringRedisTemplate stringRedisTemplate;

    public boolean markIfFirstConsume(String messageId, Duration ttl) {
        String key = "idempotent:message:" + messageId;
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", ttl);
        return Boolean.TRUE.equals(success);
    }
}