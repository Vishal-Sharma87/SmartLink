package com.spring.springboot.smartlink.email.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smart-link.email.provider-configs")
public record EmailProviderConfigs(
                String baseUrl,
                String apiKey) {
}
