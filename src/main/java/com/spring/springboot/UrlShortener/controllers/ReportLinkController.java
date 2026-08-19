package com.spring.springboot.UrlShortener.controllers;

import com.spring.springboot.UrlShortener.dto.requestDtos.ReportLinkRequestDto;
import com.spring.springboot.UrlShortener.services.ReportLinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/report-abuse")
@RequiredArgsConstructor
public class ReportLinkController {

    private final ReportLinkService reportLinkService;



    @PostMapping()
    public ResponseEntity<String> reportLink(@Valid @RequestBody ReportLinkRequestDto dto) {

        reportLinkService.tryAcceptingReport(dto);

        return new ResponseEntity<>("We have accepted your request, we'll notify you once we have confirmation about the link", HttpStatus.OK);
    }

}
