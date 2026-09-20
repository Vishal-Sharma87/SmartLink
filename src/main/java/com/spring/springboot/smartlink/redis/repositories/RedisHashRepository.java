package com.spring.springboot.smartlink.redis.repositories;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@Slf4j
public class RedisHashRepository {

    private final StringRedisTemplate redisTemplate;
    private final HashOperations<String, String, String> hashOperations;

    public RedisHashRepository(
            StringRedisTemplate redisTemplate) {

        this.redisTemplate = redisTemplate;
        this.hashOperations = redisTemplate.opsForHash();
    }

    public String get(
            String key,
            String hashKey) {

        return hashOperations.get(key, hashKey);
    }

    public List<String> multiGet(
            String key,
            List<String> hashKeys) {

        return hashOperations.multiGet(key, hashKeys);
    }

    public void putAll(
            String key,
            Map<String, String> hashKeyValueMap) {

        hashOperations.putAll(key, hashKeyValueMap);
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public void executeRevocationTokenThenSetCurrentScript(
            RedisScript<Void> script,
            List<String> keys,
            String email,
            String refreshToken) {

        redisTemplate.execute(script, keys, email, refreshToken);
    }
}
