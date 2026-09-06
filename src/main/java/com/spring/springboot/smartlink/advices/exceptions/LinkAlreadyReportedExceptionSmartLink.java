package com.spring.springboot.smartlink.advices.exceptions;

import com.spring.springboot.smartlink.advices.ErrorCode;

public class LinkAlreadyReportedExceptionSmartLink extends SmartLinkApplicationException {
    public LinkAlreadyReportedExceptionSmartLink(String message) {
        super(ErrorCode.LINK_ALREADY_REPORTED, message);
    }
}
