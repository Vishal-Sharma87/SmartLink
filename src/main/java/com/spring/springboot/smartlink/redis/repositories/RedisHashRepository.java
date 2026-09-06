package com.spring.springboot.smartlink.redis.repositories;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class RedisHashRepository {

    private final RedisTemplate<String, String> redisTemplate;

    public RedisHashRepository(
            RedisTemplate<String, String> redisTemplate) {

        this.redisTemplate = redisTemplate;
    }

    public List<Object> multiGet(String key, List<Object> hashKeys) {
        return redisTemplate.opsForHash().multiGet(key, hashKeys);
    }

    public void putAll(String key, Map<String, String> hashKeyValueMap) {
        redisTemplate.opsForHash().putAll(key, hashKeyValueMap);
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }
}
