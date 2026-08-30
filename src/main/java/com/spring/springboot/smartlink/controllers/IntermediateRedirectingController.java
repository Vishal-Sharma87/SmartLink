package com.spring.springboot.smartlink.controllers;

import com.spring.springboot.smartlink.dto.TrackPayloadDto;
import com.spring.springboot.smartlink.services.AnalysisService;
import com.spring.springboot.smartlink.services.RedirectService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class IntermediateRedirectingController {

    private final RedirectService redirectService;
    private final AnalysisService analysisService;

    @PostMapping("/track")
    public void trackUserInformationForLinkAnalysis(@RequestBody TrackPayloadDto dto, HttpServletRequest request) {

        analysisService.publish(dto, request);
        log.debug("Link tracking data queued for short code {}", dto.getShortHash());

    }

    @GetMapping("/confirm/{shortCode}")
    public String confirmAndRedirect(@PathVariable String shortCode, Model model) {

        return redirectService.addAttributeAndGetPageToReturn(shortCode, model);
    }

}
