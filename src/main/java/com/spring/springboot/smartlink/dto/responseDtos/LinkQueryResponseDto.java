package com.spring.springboot.smartlink.dto.responseDtos;

import lombok.*;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LinkQueryResponseDto<T> {
    private T data;
    private String message;
    private Date timestamp;

}
