package com.spring.springboot.UrlShortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UrlToShortRequestDto {

    @NotNull(message = "URL cannot be null")
    @NotBlank(message = "URL cannot be blank")
    private String actualUrl;
}
