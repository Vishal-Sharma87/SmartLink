package com.spring.springboot.smartlink.advices.exceptions;

import com.spring.springboot.smartlink.advices.ErrorCode;

public class LinkNotFoundExceptionSmartLink extends SmartLinkApplicationException {
    public LinkNotFoundExceptionSmartLink(String message) {
        super(ErrorCode.LINK_NOT_FOUND, message);
    }
}
