package com.spring.springboot.smartlink.report.dtos;

import com.spring.springboot.smartlink.enums.ReportCause;

import java.util.List;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record ReportLinkRequestDto(
        @NotBlank @Size(max = 100) String reporterName,
        @NotBlank @Email @Size(max = 254) String reporterEmail,
        @NotBlank @Size(max = 2048) @URL String linkToReport,
        @NotNull @NotEmpty List<ReportCause> cause,
        @Size(max = 2000) String description,
        @NotBlank @Size(min = 6, max = 6) @Pattern(regexp = "\\d{6}") String otp) {
}
