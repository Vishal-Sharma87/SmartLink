package com.spring.springboot.smartlink.link.dtos;

import com.spring.springboot.smartlink.link.enums.LinkStatus;

public record LinkCreationResponseDto(
    String message,
    LinkStatus status,
    String shortUrl
) {
}
