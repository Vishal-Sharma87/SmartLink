package com.spring.springboot.UrlShortener.dto.requestDtos;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;


@Data
public class SignupRequestDto {

    @NotBlank
    private String userName;

    @Email
    private String email;

    @NotBlank
    private String password;

    @NotBlank
    private String otp;
}
