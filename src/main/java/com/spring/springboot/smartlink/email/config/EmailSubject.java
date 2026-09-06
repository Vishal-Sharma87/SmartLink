package com.spring.springboot.smartlink.email.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smart-link.email.subject")
public record EmailSubject(
                String otp,
                String reportAccepted,
                String linkMalicious,
                String linkCreated,
                String welcome) {
}
