package com.spring.springboot.smartlink.virustotal.configs;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smart-link.virus-total")
public record VirusTotalConfigs(
        String apiKey,
        String apiUrl,
        int pollingTimeoutMinutes,
        int pollingIntervalSeconds) {
}
