package com.spring.springboot.smartlink.advices.exceptions;

public class NoSuchLinkExists extends RuntimeException {

    public NoSuchLinkExists(String msg) {
        super(msg);
    }
}
