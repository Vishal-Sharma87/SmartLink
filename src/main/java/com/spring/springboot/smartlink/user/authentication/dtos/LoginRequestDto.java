package com.spring.springboot.smartlink.user.authentication.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequestDto {

    @NotBlank
    private String userName;

    @NotBlank
    private String password;
}
