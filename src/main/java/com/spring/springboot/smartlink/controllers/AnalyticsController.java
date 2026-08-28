package com.spring.springboot.smartlink.controllers;

import com.spring.springboot.smartlink.dto.responseDtos.LinkAnalyticsResponseDto;
import com.spring.springboot.smartlink.dto.responseDtos.LinkAsResponseDto;
import com.spring.springboot.smartlink.services.LinkAnalyticsService;
import com.spring.springboot.smartlink.services.LinkService;
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

    private final LinkService linkService;
    private final LinkAnalyticsService linkAnalyticsService;

    @GetMapping("/link")
    public ResponseEntity<LinkAnalyticsResponseDto> getLinkAnalytics(@RequestParam String shortHash) {
        String userName = SecurityContextHolder.getContext().getAuthentication().getName();

        LinkAsResponseDto link = linkService.findLinkByHash(shortHash, userName);

        return ResponseEntity.ok(linkAnalyticsService.summarize(shortHash, link.getActualUrl(), link.getClickCnt() == null ? 0 : link.getClickCnt()));
    }
}
