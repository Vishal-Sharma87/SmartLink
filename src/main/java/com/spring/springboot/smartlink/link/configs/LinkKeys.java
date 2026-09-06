package com.spring.springboot.smartlink.link.configs;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smart-link.db-keys.links")
public record LinkKeys(
                String id,
                String actualUrl,
                String hashedKey,
                String ownerUserName,
                String status,
                String reportCount,
                String clickCount) {
}
