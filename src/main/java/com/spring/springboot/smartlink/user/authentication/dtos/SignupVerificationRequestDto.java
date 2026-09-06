package com.spring.springboot.smartlink.user.authentication.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SignupVerificationRequestDto {
    @NotBlank
    private String otp;
}
