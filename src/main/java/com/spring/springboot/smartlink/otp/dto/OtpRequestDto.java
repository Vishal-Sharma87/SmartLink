package com.spring.springboot.smartlink.otp.dto;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpRequestDto {

    @Email(message = "Enter a valid email id.")
    private String email;

}
