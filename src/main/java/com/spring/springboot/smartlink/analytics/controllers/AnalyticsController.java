package com.spring.springboot.smartlink.analytics.controllers;

import com.spring.springboot.smartlink.analytics.dtos.LinkAnalyticsResponseDto;
import com.spring.springboot.smartlink.apiresponse.ApiResponse;
import com.spring.springboot.smartlink.analytics.services.LinkAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final LinkAnalyticsService linkAnalyticsService;

    @GetMapping("/link")
    public ResponseEntity<ApiResponse<LinkAnalyticsResponseDto>> getLinkAnalytics(@RequestParam String shortHash) {
        String userName = SecurityContextHolder.getContext().getAuthentication().getName();

        LinkAnalyticsResponseDto response = linkAnalyticsService.summarize(userName, shortHash);

        return ResponseEntity.ok(ApiResponse.of(response));
    }
}
