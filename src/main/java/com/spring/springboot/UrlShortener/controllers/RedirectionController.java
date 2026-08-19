package com.spring.springboot.UrlShortener.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.spring.springboot.UrlShortener.dto.RedirectServiceResponseDto;
import com.spring.springboot.UrlShortener.enums.Verdict;
import com.spring.springboot.UrlShortener.services.RedirectService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller // using @Controller because it supports html page rendering and user clicks
@RequestMapping("/url.shortener")
@RequiredArgsConstructor
public class RedirectionController {

    private static final String LONG_URL = "longUrl";
    private static final String SHORT_CODE = "shortCode";
    private final RedirectService redirectService;

    @GetMapping("/{hash}")
    public String redirectionFromHashToLongUrl(@PathVariable String hash, Model model, HttpServletRequest request) throws JsonProcessingException {

        RedirectServiceResponseDto dto = redirectService.getUrlStatusIfExists(hash);

        /*
         * the link is found now there are three main options associated with status of link
         *
         * case 1 "SAFE" -> analysis + normal flow of redirection
         *
         * case 2 "SUSPICIOUS" -> a page to confirm "the link is reported suspicious continue redirection?"
         *                           if yes -> redirection
         *                           else user will take care
         *
         * case 3 "MALICIOUS" -> no redirection in any scenario
         */

        switch (dto.getStatus()) {
            case Verdict.SAFE:
                // Direct redirect
                model.addAttribute(SHORT_CODE, hash);
                model.addAttribute(LONG_URL, dto.getLongUrl());
                return "track";

            case Verdict.SUSPICIOUS, Verdict.PENDING_REVERIFICATION,
                 Verdict.UNVERIFIED:
                // Show warning + confirm
                model.addAttribute(SHORT_CODE, hash);
                model.addAttribute(LONG_URL, dto.getLongUrl());
                return "suspicious-warning";

            case Verdict.MALICIOUS:
                // Show blocked page
                model.addAttribute("message", "The link you are trying to access is confirmed Malicious.");
                return "malicious-warning";

            default:
                log.info("Unknown Url status for hash: {}", hash);
                return "error";
        }

    }


}
