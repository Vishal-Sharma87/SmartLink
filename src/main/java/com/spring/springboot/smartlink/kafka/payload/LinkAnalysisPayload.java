package com.spring.springboot.smartlink.kafka.payload;

import com.spring.springboot.smartlink.redirection.dtos.TrackPayloadDto;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class LinkAnalysisPayload {

    private String entityIp;
    private TrackPayloadDto trackPayloadDto;
}
