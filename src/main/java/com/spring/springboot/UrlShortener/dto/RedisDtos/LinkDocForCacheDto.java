package com.spring.springboot.UrlShortener.dto.RedisDtos;

import com.spring.springboot.UrlShortener.enums.FinalVerdict;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkDocForCacheDto {// used to store document similar to link in Redis for caching with as minimum field as possible
    private Long id;
    private String actualUrl;
    private FinalVerdict.Verdict status;
    private String ownerUserName;
}
