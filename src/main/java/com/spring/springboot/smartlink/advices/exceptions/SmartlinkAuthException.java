package com.spring.springboot.smartlink.advices.exceptions;

import com.spring.springboot.smartlink.advices.enums.ErrorCode;

public class SmartlinkAuthException extends SmartLinkApplicationException{
    public SmartlinkAuthException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
