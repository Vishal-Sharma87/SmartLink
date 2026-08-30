package com.spring.springboot.smartlink.services;
import com.spring.springboot.smartlink.scripts.LuaScripts;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisService {

    private static final String REDIS_REDIRECTION_CACHE_PREFIX = "smart-link:redirection-hash:";
    private static final String URL_COUNTER_KEY = "url_counter";

    private final StringRedisTemplate redisTemplate;

    @PostConstruct
    public void seedRedisInitials(){
        redisTemplate.opsForValue().setIfAbsent(URL_COUNTER_KEY, String.valueOf(56_800_235_584L));
    }

    public Long getUrlCounter() {
        Long counter = redisTemplate.opsForValue().increment(URL_COUNTER_KEY);
        log.debug("Allocated new URL counter value {}", counter);
        return counter;
    }

    public void putInRedirectionCache(String cacheKey, Map<String, String> cache) {
        redisTemplate.opsForHash().putAll(cacheKey, cache);
    }

    public List<Object> getCachedDataByKey(String key, List<Object> hashKeys) {
         return redisTemplate.opsForHash().multiGet(key, hashKeys);
    }

    public void removeIfExists(String hash) {
        redisTemplate.delete(REDIS_REDIRECTION_CACHE_PREFIX + hash);
    }

    public boolean isAfter(Long counterToCheck) {
        String last = redisTemplate.opsForValue().get(URL_COUNTER_KEY);

        return last != null && Long.parseLong(last) < counterToCheck;
    }

    public void saveOTP(String email, String hashedOtp) {
        redisTemplate.opsForValue().set(email, hashedOtp, 10, TimeUnit.MINUTES);
    }

    public boolean isValidOtp(String email, String otpToCheck) {
        RedisScript<Long> validateOtpScript = LuaScripts.VALIDATE_OTP_SCRIPT;

        Long isValid = redisTemplate.execute(validateOtpScript, List.of(email), otpToCheck);
        log.debug("OTP validation {} for email {}", isValid.equals(1L) ? "succeeded" : "failed", email);

        return isValid.equals(1L);
    }
}
