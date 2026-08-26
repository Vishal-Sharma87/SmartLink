package com.spring.springboot.smartlink.dto.responseDtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LinkCreationResponseDto {
    private String message;
    private String status;
    private String shortUrl;
}
