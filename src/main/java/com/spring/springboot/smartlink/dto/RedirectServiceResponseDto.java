package com.spring.springboot.smartlink.dto;



import com.spring.springboot.smartlink.enums.Verdict;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RedirectServiceResponseDto {
    private Verdict status;
    private String longUrl;
    private String shortHash;
}
