package com.spring.springboot.smartlink.advices.exceptions;

public class UserWithUserNameAlreadyExitsException extends RuntimeException{
    public UserWithUserNameAlreadyExitsException(String msg){
        super(msg);
    }
}
