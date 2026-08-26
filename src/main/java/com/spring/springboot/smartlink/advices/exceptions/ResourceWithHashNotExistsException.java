package com.spring.springboot.smartlink.advices.exceptions;

public class ResourceWithHashNotExistsException extends RuntimeException {

    public ResourceWithHashNotExistsException(String msg) {
        super(msg);
    }

}
