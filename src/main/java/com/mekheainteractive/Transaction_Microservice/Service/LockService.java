package com.mekheainteractive.Transaction_Microservice.Service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class LockService {

    private final StringRedisTemplate redis;

    public LockService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public boolean lock(String key) {
        return Boolean.TRUE.equals(
                redis.opsForValue().setIfAbsent(key, "locked", Duration.ofSeconds(5))
        );
    }

    public void unlock(String key) {
        redis.delete(key);
    }
}
