package com.spring.springboot.smartlink.link.dtos;


import java.util.List;

public record LinkQueryResponseDto(
    List<LinkAsResponseDto> links,
    String message
) {
}