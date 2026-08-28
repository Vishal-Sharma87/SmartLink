package com.spring.springboot.smartlink.controllers;

import com.spring.springboot.smartlink.dto.requestDtos.ReportLinkRequestDto;
import com.spring.springboot.smartlink.services.ReportLinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/report-abuse")
@RequiredArgsConstructor
@Slf4j
public class ReportLinkController {

    private final ReportLinkService reportLinkService;



    @PostMapping()
    public ResponseEntity<String> reportLink(@Valid @RequestBody ReportLinkRequestDto dto) {

        reportLinkService.tryAcceptingReport(dto);
        log.info("Abuse report accepted for processing");

        return new ResponseEntity<>("We have accepted your request, we'll notify you once we have confirmation about the link", HttpStatus.OK);
    }

}
