package com.spring.springboot.smartlink.dto.responseDtos;

import lombok.*;

import java.time.LocalDateTime;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponseDto <T>{
    private T data;
    private String message;
    private LocalDateTime timestamp;
}
