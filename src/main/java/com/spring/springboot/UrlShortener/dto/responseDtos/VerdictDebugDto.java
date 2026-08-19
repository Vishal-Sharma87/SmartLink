package com.spring.springboot.UrlShortener.dto.responseDtos;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.spring.springboot.UrlShortener.enums.FinalVerdict;
import com.spring.springboot.UrlShortener.enums.VerdictReason;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerdictDebugDto {
    private String shortCode;
    private String originalUrl;
    private FinalVerdict.Verdict verdict;
    private VerdictReason verdictReason;
    private double harmlessRatio;
    private double maliciousRatio;
    private double undetectedRatio;
    private double timeoutRatio;
    private int totalEngines;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime analysedAt;
}