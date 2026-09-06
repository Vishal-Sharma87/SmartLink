package com.spring.springboot.smartlink.advices.exceptions;

import com.spring.springboot.smartlink.advices.ErrorCode;

public class LinkScanNotFoundExceptionSmartLink extends SmartLinkApplicationException {
    public LinkScanNotFoundExceptionSmartLink(String message) {
        super(ErrorCode.LINK_SCAN_NOT_FOUND, message);
    }
}
