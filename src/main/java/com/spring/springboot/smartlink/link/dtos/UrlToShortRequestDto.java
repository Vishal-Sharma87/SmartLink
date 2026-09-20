package com.spring.springboot.smartlink.link.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record UrlToShortRequestDto(
    @NotBlank @Size(max = 2048) @URL String originalUrl
) {
}
