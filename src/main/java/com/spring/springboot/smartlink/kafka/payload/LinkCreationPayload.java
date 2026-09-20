package com.spring.springboot.smartlink.kafka.payload;

public record LinkCreationPayload(
    String originalUrl,
    String ownerEmail,
    String shortUrl
) {
}