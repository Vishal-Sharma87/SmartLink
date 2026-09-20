package com.spring.springboot.smartlink.configurations;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smart-link.exception.messages")
public record ExceptionMessages(
                String linkNotFound,
                String invalidLinkHash,
                String userNotFound,
                String linkScanNotFound,
                String userWithEmailExists,
                String linkAlreadyReported,
                String invalidOtp,
                String missingUpdateFields,
                String sha256Unavailable,
                String authException,
                String validationException,
                String authenticationFailed,
                String accessDenied,
                String internalServerError) {
}
