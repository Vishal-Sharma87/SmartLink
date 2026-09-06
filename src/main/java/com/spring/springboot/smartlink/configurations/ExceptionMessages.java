package com.spring.springboot.smartlink.configurations;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smart-link.exception.messages")
public record ExceptionMessages(
                String linkNotFound,
                String invalidLinkHash,
                String userNotFound,
                String linkScanNotFound,
                String usernameAlreadyExists,
                String linkAlreadyReported,
                String invalidOtp,
                String missingUpdateFields,
                String sha256Unavailable) {
}
