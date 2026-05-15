package com.quant.common.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RedisLockHelper {

    private final StringRedisTemplate stringRedisTemplate;

    public String tryLock(String key, Duration ttl) {
        String value = UUID.randomUUID().toString();
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(key, value, ttl);
        return Boolean.TRUE.equals(success) ? value : null;
    }

    public void unlock(String key, String value) {
        String current = stringRedisTemplate.opsForValue().get(key);
        if (value != null && value.equals(current)) {
            stringRedisTemplate.delete(key);
        }
    }
}