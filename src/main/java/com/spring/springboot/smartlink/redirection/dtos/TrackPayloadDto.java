package com.spring.springboot.smartlink.redirection.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TrackPayloadDto(
        @NotBlank @Size(max = 32) String shortCode,
        @Min(0) int screenWidth,
        @Min(0) int viewportWidth,
        @NotBlank @Size(max = 1024) String userAgent,
        @NotBlank @Size(max = 128) String timezone
) {
}
