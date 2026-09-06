package com.spring.springboot.smartlink.configurations;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smart-link.application-configs")
public record ApplicationConfigs(
                String shortUrlPrefix) {
}
