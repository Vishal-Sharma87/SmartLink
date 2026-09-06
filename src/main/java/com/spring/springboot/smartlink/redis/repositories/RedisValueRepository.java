package com.spring.springboot.smartlink.redis.repositories;

import com.spring.springboot.smartlink.redis.keys.RedisKeys;
import com.spring.springboot.smartlink.redis.scripts.LuaScripts;
import jakarta.annotation.PostConstruct;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Repository
public class RedisValueRepository {

    private final RedisTemplate<String, String> redisTemplate;
    private final RedisKeys redisKeys;

    public RedisValueRepository(
            RedisTemplate<String, String> redisTemplate,
            RedisKeys redisKeys) {

        this.redisTemplate = redisTemplate;
        this.redisKeys = redisKeys;
    }

    private static final Long INITIAL_URL_COUNTER = 56_800_235_584L;

    @PostConstruct
    public void seedRedisInitials() {
        seedInitialUrlCounter();
    }

    private void seedInitialUrlCounter() {
        redisTemplate.opsForValue().setIfAbsent(redisKeys.urlCounterKey(), String.valueOf(INITIAL_URL_COUNTER));
    }

    public long getUrlCounter() {
        Long counter = redisTemplate.opsForValue().increment(redisKeys.urlCounterKey());
        if (counter == null || counter < INITIAL_URL_COUNTER) {
            counter = INITIAL_URL_COUNTER;
            seedInitialUrlCounter();
        }
        return counter;
    }

    public boolean isAfterUrlCounter(Long counterToCheck) {
        String currentUrlCounterString = redisTemplate.opsForValue().get(redisKeys.urlCounterKey());
        Long currentUrlCounter;
        if (currentUrlCounterString == null
                || (currentUrlCounter = Long.parseLong(currentUrlCounterString)) < INITIAL_URL_COUNTER) {
            seedInitialUrlCounter();

            // Returning true, as Url_Counter was altered and safest option is to reject the
            // User request by returning true
            return true;
        }

        return counterToCheck > currentUrlCounter;
    }

    public void saveOtpWithDefaultTtl(String email, String hashedOtp) {
        redisTemplate.opsForValue().set(email, hashedOtp, 10, TimeUnit.MINUTES);
    }

    public boolean isInvalidOtp(String email, String otpToCheck) {
        RedisScript<Long> validateOtpScript = LuaScripts.VALIDATE_OTP_SCRIPT;

        Long isValid = redisTemplate.execute(validateOtpScript, List.of(email), otpToCheck);

        return isValid.equals(0L);
    }
}
