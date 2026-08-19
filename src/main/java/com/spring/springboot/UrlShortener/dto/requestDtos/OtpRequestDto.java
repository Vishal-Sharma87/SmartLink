package com.spring.springboot.UrlShortener.dto.requestDtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    private String entityEmail;

    @NotNull
    @NotBlank
    private String entityName;

}
