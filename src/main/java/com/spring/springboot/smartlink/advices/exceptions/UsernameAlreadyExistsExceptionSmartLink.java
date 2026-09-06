package com.spring.springboot.smartlink.advices.exceptions;

import com.spring.springboot.smartlink.advices.ErrorCode;

public class UsernameAlreadyExistsExceptionSmartLink extends SmartLinkApplicationException {
    public UsernameAlreadyExistsExceptionSmartLink(String message) {
        super(ErrorCode.USERNAME_ALREADY_EXISTS, message);
    }
}
