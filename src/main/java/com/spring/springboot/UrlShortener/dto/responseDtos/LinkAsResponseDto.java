package com.spring.springboot.UrlShortener.dto.responseDtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

import com.spring.springboot.UrlShortener.enums.FinalVerdict;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LinkAsResponseDto {

    /*
     * actual
     * hash
     * creation time
     * status
     * report cnt
     * click cnt
     * */
    private String id;
    private String actualUrl;
    private String hashedKey;
    private FinalVerdict.Verdict status;
    private Integer clickCnt;
    private Integer reportCnt;
    private Instant creationTime;
}
