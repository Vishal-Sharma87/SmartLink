package com.spring.springboot.smartlink.advices;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApiError {
    private LocalDateTime timestamp;
    private String message;
    private String code;
    private int httpStatus;

    public ApiError(LocalDateTime timestamp, String message, String code, int httpStatus) {
        this.timestamp = timestamp;
        this.message = message;
        this.code = code;
        this.httpStatus = httpStatus;
    }
}
