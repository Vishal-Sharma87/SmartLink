package com.spring.springboot.smartlink.jwt.configs;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smart-link.jwt")
public record JwtConfigs(
                String secretKey,
                Integer expiryDurationMinutes) {
}
