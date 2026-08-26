package com.spring.springboot.smartlink.advices.exceptions;

public class ResourceNotExistsException extends RuntimeException {

    public ResourceNotExistsException(String msg) {
        super(msg);
    }
}
