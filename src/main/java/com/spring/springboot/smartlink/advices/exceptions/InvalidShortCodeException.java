package com.spring.springboot.smartlink.advices.exceptions;

import com.spring.springboot.smartlink.advices.enums.ErrorCode;

public class InvalidShortCodeException extends SmartLinkApplicationException {
    public InvalidShortCodeException(String message) {
        super(ErrorCode.INVALID_LINK_HASH, message);
    }
}
