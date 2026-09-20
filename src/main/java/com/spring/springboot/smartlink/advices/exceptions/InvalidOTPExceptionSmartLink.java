package com.spring.springboot.smartlink.advices.exceptions;

import com.spring.springboot.smartlink.advices.enums.ErrorCode;

public class InvalidOTPExceptionSmartLink extends SmartLinkApplicationException {
    public InvalidOTPExceptionSmartLink(String msg) {
        super(ErrorCode.INVALID_OTP, msg);
    }
}
