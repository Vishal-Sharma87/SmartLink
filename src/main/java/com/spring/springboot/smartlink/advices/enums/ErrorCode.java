package com.spring.springboot.smartlink.advices.enums;

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
    MISSING_UPDATE_FIELDS(400),
    INVALID_REFRESH_TOKEN(401),
    PENDING_SIGNUP_NOT_FOUND(404),
    INVALID_REQUEST(400),
    AUTHENTICATION_FAILED(401),
    ACCESS_DENIED(403),
    INTERNAL_SERVER_ERROR(500);

    private final HttpStatus httpStatus;

    ErrorCode(int statusCode) {
        this.httpStatus = HttpStatus.valueOf(statusCode);
    }

}
