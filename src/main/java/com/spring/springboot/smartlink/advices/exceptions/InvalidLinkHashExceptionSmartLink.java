package com.spring.springboot.smartlink.advices.exceptions;

import com.spring.springboot.smartlink.advices.ErrorCode;

public class InvalidLinkHashExceptionSmartLink extends SmartLinkApplicationException {
    public InvalidLinkHashExceptionSmartLink(String message) {
        super(ErrorCode.INVALID_LINK_HASH, message);
    }
}
