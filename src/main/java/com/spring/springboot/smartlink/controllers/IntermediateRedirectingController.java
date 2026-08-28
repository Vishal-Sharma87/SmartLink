package com.spring.springboot.smartlink.controllers;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.spring.springboot.smartlink.dto.RedirectServiceResponseDto;
import com.spring.springboot.smartlink.dto.TrackPayloadDto;
import com.spring.springboot.smartlink.services.AnalysisService;
import com.spring.springboot.smartlink.services.RedirectService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
    public void trackUserInformationForLinkAnalysis(@RequestBody TrackPayloadDto dto, HttpServletRequest request, HttpServletResponse response, Model model) {

        analysisService.publish(dto, request);
        log.debug("Link tracking data queued for short code {}", dto.getShortHash());

    }

    @GetMapping("/confirm/{shortCode}")
    public String confirmAndRedirect(@PathVariable String shortCode, Model model) throws JsonProcessingException {

        RedirectServiceResponseDto res = redirectService.getActualUrlIfExists(shortCode);

        if (res == null) {
            log.warn("Intermediate redirect requested for unknown short code {}", shortCode);
            return "error";
        }

        // Redirect to the original long URL
        model.addAttribute("shortCode", shortCode);
        model.addAttribute("longUrl", res.getLongUrl());

//        serve an intermediate html page to get user device info
        log.info("Intermediate redirect page prepared for short code {}", shortCode);
        return "track";
    }

}
