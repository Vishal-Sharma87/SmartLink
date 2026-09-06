package com.spring.springboot.smartlink.report.configs;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smart-link.db-keys.abuse-report")
public record AbuseReportKeys(
                String reporterEmail) {
}
