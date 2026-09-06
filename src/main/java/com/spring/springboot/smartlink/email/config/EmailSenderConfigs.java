package com.spring.springboot.smartlink.email.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smart-link.email.sender-configs")
public record EmailSenderConfigs(
                String name,
                String email) {
}
