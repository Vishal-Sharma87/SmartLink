package com.spring.springboot.smartlink.redis.services;

import com.spring.springboot.smartlink.enums.Verdict;
import com.spring.springboot.smartlink.redis.keys.RedisKeys;
import com.spring.springboot.smartlink.redis.repositories.RedisHashRepository;
import com.spring.springboot.smartlink.redis.repositories.RedisValueRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class RedisService {

    private final RedisKeys redisKeys;
    private final RedisHashRepository redisHashRepository;
    private final RedisValueRepository redisValueRepository;

    public RedisService(
            RedisKeys redisKeys,
            RedisHashRepository redisHashRepository,
            RedisValueRepository redisValueRepository) {

        this.redisKeys = redisKeys;
        this.redisHashRepository = redisHashRepository;
        this.redisValueRepository = redisValueRepository;
    }

    public Long getUrlCounter() {
        return redisValueRepository.getUrlCounter();
    }

    public void removeRedirectionCache(String shortCode) {
        String cacheKey = buildRedirectionCacheKey(shortCode);
        redisHashRepository.delete(cacheKey);
    }

    public boolean isAfter(Long counterToCheck) {
        return redisValueRepository.isAfterUrlCounter(counterToCheck);

    }

    public void saveOTP(String email, String hashedOtp) {
        redisValueRepository.saveOtpWithDefaultTtl(email, hashedOtp);
    }

    public boolean isInvalidOtp(String email, String otpToCheck) {
        return redisValueRepository.isInvalidOtp(email, otpToCheck);
    }

    public List<Object> getRedirectionCache(String shortCode) {
        String cacheKey = buildRedirectionCacheKey(shortCode);

        List<Object> cacheHashKeys = List.of(
                redisKeys.longUrlHashKey(),
                redisKeys.statusHashKey());

        return redisHashRepository.multiGet(cacheKey, cacheHashKeys);
    }

    public void putInRedirectionCache(String shortCode, String longUrl, Verdict status) {
        String cacheKey = buildRedirectionCacheKey(shortCode);

        Map<String, String> cache = new HashMap<>();

        cache.put(redisKeys.longUrlHashKey(), longUrl);
        cache.put(redisKeys.statusHashKey(), status.name());

        redisHashRepository.putAll(cacheKey, cache);

    }

    private String buildRedirectionCacheKey(String shortCode) {
        return redisKeys.redirectionCachePrefix() + shortCode;
    }
}
