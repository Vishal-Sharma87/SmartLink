package com.spring.springboot.smartlink.dto.RedisDtos;


import com.spring.springboot.smartlink.enums.Verdict;
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
    private Verdict status;
    private String ownerUserName;
}
