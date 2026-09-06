package com.spring.springboot.smartlink.configurations;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smart-link.redirection.pages")
public record RedirectionPageConfigs(
                String errorPage,
                String trackPage,
                String suspiciousWarningPage,
                String maliciousPage) {
}
