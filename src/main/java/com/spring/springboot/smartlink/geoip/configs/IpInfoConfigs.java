package com.spring.springboot.smartlink.geoip.configs;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smart-link.ip-info.configs")
public record IpInfoConfigs(
                String token) {
}
