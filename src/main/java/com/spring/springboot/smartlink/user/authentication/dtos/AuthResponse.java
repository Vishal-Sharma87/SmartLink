package com.spring.springboot.smartlink.user.authentication.dtos;

public record AuthResponse(String token, String message) {

    public static AuthResponse of(String token, String message) {
        return new AuthResponse(token, message);
    }
}
