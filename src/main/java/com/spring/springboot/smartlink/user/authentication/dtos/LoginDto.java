package com.spring.springboot.smartlink.user.authentication.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginDto(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6, max = 128) String password) {
}
