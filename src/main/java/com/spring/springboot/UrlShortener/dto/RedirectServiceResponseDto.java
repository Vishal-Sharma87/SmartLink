package com.spring.springboot.UrlShortener.dto;



import com.spring.springboot.UrlShortener.enums.Verdict;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RedirectServiceResponseDto {
    private Verdict status;
    private String longUrl;
    private String shortHash;
}
