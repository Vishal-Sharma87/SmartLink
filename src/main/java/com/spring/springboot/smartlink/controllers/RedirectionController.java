package com.spring.springboot.smartlink.controllers;
import com.spring.springboot.smartlink.services.RedirectService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller // using @Controller because it supports html page rendering and user clicks
@RequestMapping()
@RequiredArgsConstructor
public class RedirectionController {

    private final RedirectService redirectService;

    @GetMapping("/{hash}")
    public String redirectionFromHashToLongUrl(@PathVariable String hash, Model model) {

        return redirectService.addModelAttributesAndGetPageToServeString(hash, model);

    }


}
