package com.spring.springboot.smartlink.kafka.configs;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smart-link.kafka.connection-configs")
public record ConnectionConfigs(
        String bootstrapServers,
        String securityProtocol,
        String saslMechanism,
        String saslJaasConfig,
        Integer sessionTimeoutMs) {
}
