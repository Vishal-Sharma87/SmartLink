package com.spring.springboot.smartlink.advices.exceptions;

import com.spring.springboot.smartlink.advices.enums.ErrorCode;

public class UserWithEmailExistsException extends SmartLinkApplicationException {
    public UserWithEmailExistsException(String message) {
        super(ErrorCode.USERNAME_ALREADY_EXISTS, message);
    }
}
