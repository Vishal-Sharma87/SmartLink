package com.spring.springboot.UrlShortener.dto.responseDtos;

import com.spring.springboot.UrlShortener.enums.Verdict;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;


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
    private Verdict status;
    private Integer clickCnt;
    private Integer reportCnt;
    private Instant creationTime;
}
