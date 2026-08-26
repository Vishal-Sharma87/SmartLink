package com.spring.springboot.smartlink.model;


import com.spring.springboot.smartlink.dto.TrackPayloadDto;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class LinkAnalysisDto {

    private String entityIp;
    private TrackPayloadDto trackPayloadDto;
}
