package com.spring.springboot.UrlShortener.controllers;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.spring.springboot.UrlShortener.dto.RedirectServiceResponseDto;
import com.spring.springboot.UrlShortener.dto.TrackPayloadDto;
import com.spring.springboot.UrlShortener.services.AnalysisService;
import com.spring.springboot.UrlShortener.services.RedirectService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/api")
@RequiredArgsConstructor
public class IntermediateRedirectingController {

    private final RedirectService redirectService;
    private final AnalysisService analysisService;

    @PostMapping("/track")
    public void trackUserInformationForLinkAnalysis(@RequestBody TrackPayloadDto dto, HttpServletRequest request, HttpServletResponse response, Model model) {

        analysisService.sendDataToKafka(dto, request);

    }

    @GetMapping("/confirm/{shortCode}")
    public String confirmAndRedirect(@PathVariable String shortCode, Model model) throws JsonProcessingException {

        RedirectServiceResponseDto res = redirectService.getActualUrlIfExists(shortCode);

        if (res == null) {
            return "error";
        }

        // Redirect to the original long URL
        model.addAttribute("shortCode", shortCode);
        model.addAttribute("longUrl", res.getLongUrl());

//        serve an intermediate html page to get user device info
        return "track";
    }

}
