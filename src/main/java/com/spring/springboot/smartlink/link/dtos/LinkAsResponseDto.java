package com.spring.springboot.smartlink.link.dtos;

import com.spring.springboot.smartlink.enums.Verdict;
import java.time.Instant;


public record LinkAsResponseDto(
        String originalUrl,
        String shortCode,
        Verdict status,
        int clickCnt,
        int reportCnt,
        Instant createdAt) {
}
