package com.spring.springboot.UrlShortener.services;

import com.spring.springboot.UrlShortener.dto.requestDtos.ReportLinkRequestDto;
import com.spring.springboot.UrlShortener.dto.emailComponents.EmailContentBuilder;
import com.spring.springboot.UrlShortener.dto.emailComponents.EmailDto;
import com.spring.springboot.UrlShortener.entity.AbuseReport;
import com.spring.springboot.UrlShortener.enums.FinalVerdict;
import com.spring.springboot.UrlShortener.repositories.AbuseReportRepository;
import com.spring.springboot.UrlShortener.repositories.MongoLinkService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncReportService {

    private final MongoLinkService mongoLinkService;
    private final AbuseReportRepository abuseReportRepository;
    private final EmailContentBuilder emailContentBuilder;
    private final EmailService emailService;


    @Async
    public void acceptReport(String hashedKey, @Valid ReportLinkRequestDto dto) {

        int newReportCount = mongoLinkService.incrementAndGetReportCount(hashedKey);

        if (newReportCount >= 3){
            mongoLinkService.updateStatus(hashedKey, FinalVerdict.Verdict.PENDING_REVERIFICATION);
        }
        AbuseReport reportObject = AbuseReport.builder()
                .reporterName(dto.getReporterName())
                .reporterEmail(dto.getReporterEmail())
                .reportStatus("Pending")
                .cause(dto.getCause())
                .description(dto.getDescription())
                .hashedKeyOfLink(hashedKey)
                .createdAt(LocalDateTime.now())
                .build();

        abuseReportRepository.save(reportObject);
//        send a confirmation mail that we have accepted his report and will notify after confirmation
        EmailDto emailDtoWithSuccessfulReportContent = emailContentBuilder.getEmailDtoWithSuccessfulReportContent(dto.getLinkToReport(), dto.getReporterEmail());
        try {
            emailService.sendEmail(emailDtoWithSuccessfulReportContent);
        } catch (IOException e) {
            log.error("Something went wrong and email for successful report failed. Exception: {}", e.getMessage());
        }

    }
}
