package com.spring.springboot.smartlink.advices.exceptions;

import com.spring.springboot.smartlink.advices.ErrorCode;
import lombok.Getter;

@Getter
public abstract class SmartLinkApplicationException extends RuntimeException {
    private final ErrorCode errorCode;

    protected SmartLinkApplicationException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

}
