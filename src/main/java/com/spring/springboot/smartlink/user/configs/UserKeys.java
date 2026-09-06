package com.spring.springboot.smartlink.user.configs;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smart-link.db-keys.users")
public record UserKeys(
                String maliciousUrlsCreatedCount) {
}
