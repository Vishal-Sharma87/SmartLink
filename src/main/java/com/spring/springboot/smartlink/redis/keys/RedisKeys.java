package com.spring.springboot.smartlink.redis.keys;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smart-link.redis.keys")
public record RedisKeys(
                String redirectionCachePrefix,
                String urlCounterKey,
                String statusHashKey,
                String longUrlHashKey) {
}
