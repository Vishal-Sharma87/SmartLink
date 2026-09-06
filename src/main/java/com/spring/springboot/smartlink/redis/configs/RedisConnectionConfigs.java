package com.spring.springboot.smartlink.redis.configs;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smart-link.redis.connection-configs")
public record RedisConnectionConfigs(
                String host,
                Integer port) {
}
