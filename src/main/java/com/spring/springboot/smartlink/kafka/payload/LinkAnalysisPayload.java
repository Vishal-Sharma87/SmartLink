package com.spring.springboot.smartlink.kafka.payload;

import com.spring.springboot.smartlink.redirection.dtos.TrackPayloadDto;

public record LinkAnalysisPayload(
        String entityIp,
        TrackPayloadDto trackPayloadDto
) {
}
