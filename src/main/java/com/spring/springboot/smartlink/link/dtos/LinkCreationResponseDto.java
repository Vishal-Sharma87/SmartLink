package com.spring.springboot.smartlink.link.dtos;

import com.spring.springboot.smartlink.link.enums.LinkStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LinkCreationResponseDto {
    private String message;
    private LinkStatus status;
    private String shortUrl;
}
