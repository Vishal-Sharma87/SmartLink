package com.spring.springboot.smartlink.advices.exceptions;

public class LinkAlreadyReportedByCurrentEmailOfReporterException extends RuntimeException {
    public LinkAlreadyReportedByCurrentEmailOfReporterException(String msg) {
        super(msg);
    }
}
