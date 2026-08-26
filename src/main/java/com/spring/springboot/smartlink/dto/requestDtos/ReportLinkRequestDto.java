package com.spring.springboot.smartlink.dto.requestDtos;


import com.spring.springboot.smartlink.enums.ReportCause;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportLinkRequestDto {

    @NotBlank
    @NotNull
    private String reporterName;

    @Email
    private String reporterEmail;

    @NotBlank
    @NotNull
    private String linkToReport;

    @NotNull
    private List<ReportCause> cause;

    private String description;

    @NotBlank
    @NotNull
    private String otp;
}
