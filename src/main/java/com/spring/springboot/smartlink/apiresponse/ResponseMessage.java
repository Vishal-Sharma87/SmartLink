package com.spring.springboot.smartlink.apiresponse;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smart-link.api-response")
public record ResponseMessage(
                String linkCreated,
                String linkCreationPending,
                String allLinksOfUserMessage,
                String linkDeleted,
                String allLinksOfUserDeleted,
                String userDeleted,
                String reportAccepted,
                String signupInitiated,
                String malformedSignupSession,
                String authCompleted) {

}
