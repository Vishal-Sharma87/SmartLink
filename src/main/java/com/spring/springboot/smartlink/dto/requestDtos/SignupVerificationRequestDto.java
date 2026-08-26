package com.spring.springboot.smartlink.dto.requestDtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SignupVerificationRequestDto {
    @NotBlank
    private String otp;
}
