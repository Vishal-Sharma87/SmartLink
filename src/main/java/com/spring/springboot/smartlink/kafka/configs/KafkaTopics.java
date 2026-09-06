package com.spring.springboot.smartlink.kafka.configs;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smart-link.kafka.topic")
public record KafkaTopics(
        String linkCreation,
        String linkAnalysis) {
}
