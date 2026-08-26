package com.spring.springboot.smartlink.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class TrackPayloadDto {
    // used to get clicker's device's, browser's, etc. info for real time analysis

    private String shortHash;

    private Integer screenWidth;

    private Integer viewportWidth;

    private String userAgent;

    private String timezone;
}
