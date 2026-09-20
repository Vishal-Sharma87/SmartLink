package com.spring.springboot.smartlink.user.authentication.dtos;

public record AuthTokens(String jwtToken, String refreshToken) {
}
