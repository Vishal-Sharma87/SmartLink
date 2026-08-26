package com.spring.springboot.smartlink.advices.exceptions;

public class SendgridEmailFailedException extends RuntimeException {
    public SendgridEmailFailedException(String msg) {
        super(msg);
    }
}
