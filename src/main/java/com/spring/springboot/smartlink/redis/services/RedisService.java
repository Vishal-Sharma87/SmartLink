package com.spring.springboot.smartlink.redis.services;

import com.spring.springboot.smartlink.advices.enums.ErrorCode;
import com.spring.springboot.smartlink.advices.exceptions.SmartlinkAuthException;
import com.spring.springboot.smartlink.configurations.ExceptionMessages;
import com.spring.springboot.smartlink.enums.Verdict;
import com.spring.springboot.smartlink.redis.keys.RedisKeys;
import com.spring.springboot.smartlink.redis.repositories.RedisHashRepository;
import com.spring.springboot.smartlink.redis.repositories.RedisValueRepository;
import com.spring.springboot.smartlink.redis.scripts.LuaScripts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class RedisService {

    private final RedisKeys redisKeys;
    private final RedisHashRepository redisHashRepository;
    private final RedisValueRepository redisValueRepository;
    private final ExceptionMessages exceptionMessages;

    public RedisService(
            RedisKeys redisKeys,
            RedisHashRepository redisHashRepository,
            RedisValueRepository redisValueRepository, ExceptionMessages exceptionMessages) {

        this.redisKeys = redisKeys;
        this.redisHashRepository = redisHashRepository;
        this.redisValueRepository = redisValueRepository;
        this.exceptionMessages = exceptionMessages;
    }

    public Long getUrlCounter() {
        return redisValueRepository.getUrlCounter();
    }

    public void removeRedirectionCache(String shortCode) {
        String cacheKey = buildRedirectionCacheKey(shortCode);
        redisHashRepository.delete(cacheKey);
    }

    public boolean isNotInRange(Long counterToCheck) {
        return redisValueRepository.isNotInRange(counterToCheck);

    }

    public void saveOTP(String email, String hashedOtp) {
        redisValueRepository.saveOtpWithDefaultTtl(email, hashedOtp);
    }

    public boolean isInvalidOtp(String email, String otpToCheck) {
        return redisValueRepository.isInvalidOtp(email, otpToCheck);
    }

    public List<String> getRedirectionCache(String shortCode) {
        String cacheKey = buildRedirectionCacheKey(shortCode);

        List<String> cacheHashKeys = List.of(
                redisKeys.originalUrlHashKey(),
                redisKeys.statusHashKey());

        return redisHashRepository.multiGet(cacheKey, cacheHashKeys);
    }

    public void putInRedirectionCache(String shortCode, String originalUrl, Verdict status) {
        String cacheKey = buildRedirectionCacheKey(shortCode);

        Map<String, String> cache = new HashMap<>();

        cache.put(redisKeys.originalUrlHashKey(), originalUrl);
        cache.put(redisKeys.statusHashKey(), status.name());

        redisHashRepository.putAll(cacheKey, cache);
    }

    private String buildRedirectionCacheKey(String shortCode) {
        return redisKeys.redirectionCachePrefix() + shortCode;
    }
    public void revokeOtherThenSetCurrentRefreshToken(String refreshToken, String email) {
        RedisScript<Void> script =
                LuaScripts.REVOKE_OTHER_THEN_SET_CURRENT_REFRESH_TOKEN_SCRIPT;

        List<String> keys = List.of(
                redisKeys.emailToRefreshTokenPrefix(),
                redisKeys.refreshTokenToEmailKeyPrefix(),
                redisKeys.tokenHashKey(),
                redisKeys.emailHashKey()
        );

        redisHashRepository.executeRevocationTokenThenSetCurrentScript(
                script,
                keys,
                email,
                refreshToken
        );
    }

    public String getEmailFromRefreshToken(String refreshToken) {
        String refreshTokenToEmailKey =redisKeys.refreshTokenToEmailKeyPrefix() + refreshToken;
        String emailHashKey = redisKeys.emailHashKey();

        String stored = redisHashRepository.get(refreshTokenToEmailKey, emailHashKey);

        if (stored == null) {
            log.warn("Refresh token lookup failed because no active token mapping was found");
            throw new SmartlinkAuthException(
                    ErrorCode.INVALID_REFRESH_TOKEN,
                    exceptionMessages.authException()
            );
        }

        return stored;
    }
}
