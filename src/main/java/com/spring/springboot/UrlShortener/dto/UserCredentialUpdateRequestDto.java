package com.spring.springboot.UrlShortener.dto;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Data
public class UserCredentialUpdateRequestDto {

    private String userName;

    private String password;

    private String email;
}
