package com.spring.springboot.smartlink.user.authentication.dtos;

import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class SignupInitiateDto {

    @NotBlank @Size(max = 100)
    private final String firstName;
    @NotBlank @Size(max = 100)
    private final String lastName;
    @NotBlank @Email @Size(max = 254)
    private final String email;
    @NotBlank @Size(min = 6, max = 128)
    private String password;


}
