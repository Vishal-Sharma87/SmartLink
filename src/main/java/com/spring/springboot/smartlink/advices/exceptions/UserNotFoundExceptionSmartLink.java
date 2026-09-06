package com.spring.springboot.smartlink.advices.exceptions;

import com.spring.springboot.smartlink.advices.ErrorCode;

public class UserNotFoundExceptionSmartLink extends SmartLinkApplicationException {
    public UserNotFoundExceptionSmartLink(String message) {
        super(ErrorCode.USER_NOT_FOUND, message);
    }
}
