package com.spring.springboot.smartlink.advices;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    LINK_NOT_FOUND(404),
    INVALID_LINK_HASH(400),
    USER_NOT_FOUND(404),
    LINK_SCAN_NOT_FOUND(404),
    USERNAME_ALREADY_EXISTS(409),
    LINK_ALREADY_REPORTED(409),
    INVALID_OTP(401),
    MISSING_UPDATE_FIELDS(400);

    private final HttpStatus httpStatus;

    ErrorCode(int statusCode) {
        this.httpStatus = HttpStatus.valueOf(statusCode);
    }

}
