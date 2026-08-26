package com.spring.springboot.smartlink.dto.requestDtos;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequestDto {

    @NotBlank
    private String userName;

    @NotBlank
    private String password;
}
