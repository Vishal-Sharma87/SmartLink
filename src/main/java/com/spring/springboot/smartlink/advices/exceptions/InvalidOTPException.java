package com.spring.springboot.smartlink.advices.exceptions;

public class InvalidOTPException extends RuntimeException {
    public InvalidOTPException(String msg) {
        super(msg);
    }
}
