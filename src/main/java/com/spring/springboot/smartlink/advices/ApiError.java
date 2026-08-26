package com.spring.springboot.smartlink.advices;

import lombok.Data;

import java.time.LocalDateTime;
@Data
public class ApiError {
    private LocalDateTime timestamp;
    private String message;
    private int errorCode;

    public ApiError(String message, int errorCode) {
        this.message = message;
        this.errorCode = errorCode;
        this.timestamp = LocalDateTime.now();
    }
}