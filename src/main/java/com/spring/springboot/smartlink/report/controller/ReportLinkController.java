package com.spring.springboot.smartlink.report.controller;

import com.spring.springboot.smartlink.apiresponse.ApiResponse;
import com.spring.springboot.smartlink.report.dtos.ReportLinkRequestDto;
import com.spring.springboot.smartlink.report.services.ReportLinkService;
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

    @PostMapping("/")
    public ResponseEntity<ApiResponse<String>> reportLink(@Valid @RequestBody ReportLinkRequestDto dto) {

        String message = reportLinkService.tryAcceptingReport(dto);
        log.info("Abuse report accepted for processing");

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.of(message));
    }

}
