package com.spring.springboot.smartlink.kafka.configs;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smart-link.kafka.producer")
public record KafkaProducerConfigs(
        String keySerializer,
        String valueSerializer) {
}