package com.spring.springboot.smartlink.link.dtos;

import lombok.*;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LinkQueryResponseDto {
    private List<LinkAsResponseDto> links;
    private String message;
}
