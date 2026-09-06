package com.spring.springboot.smartlink.user.authentication.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SignupInitiationRequestDto {
    @NotBlank
    private String userName;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;
}
