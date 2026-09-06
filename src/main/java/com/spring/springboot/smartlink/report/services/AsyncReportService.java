package com.spring.springboot.smartlink.report.services;

import com.spring.springboot.smartlink.report.dtos.ReportLinkRequestDto;
import com.spring.springboot.smartlink.email.services.EmailService;
import com.spring.springboot.smartlink.report.entities.AbuseReport;
import com.spring.springboot.smartlink.advices.exceptions.SmartLinkApplicationException;
import com.spring.springboot.smartlink.enums.Verdict;
import com.spring.springboot.smartlink.redis.services.RedisService;
import com.spring.springboot.smartlink.report.repositories.AbuseReportRepository;

import com.spring.springboot.smartlink.link.services.MongoLinkService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class AsyncReportService {

    private final MongoLinkService mongoLinkService;
    private final AbuseReportRepository abuseReportRepository;
    private final RedisService redisService;
    private final EmailService emailService;

    public AsyncReportService(
            MongoLinkService mongoLinkService,
            AbuseReportRepository abuseReportRepository,
            RedisService redisService,
            EmailService emailService) {
        this.mongoLinkService = mongoLinkService;
        this.abuseReportRepository = abuseReportRepository;
        this.redisService = redisService;
        this.emailService = emailService;
    }

    @Async
    public void acceptReport(String shortCode, @Valid ReportLinkRequestDto dto) {
        try {
            processReport(shortCode, dto);
        } catch (SmartLinkApplicationException ex) {
            log.error("Asynchronous abuse-report processing failed for short code {}", shortCode, ex);
        }
    }

    private void processReport(String shortCode, ReportLinkRequestDto dto) {
        log.info("Asynchronous abuse-report processing started for short code {}", shortCode);

        String linkToReport = dto.getLinkToReport();
        String reporterName = dto.getReporterName();
        String reporterEmail = dto.getReporterEmail();

        int newReportCount = mongoLinkService.incrementAndGetReportCount(shortCode);

        if (newReportCount >= 3) {
            mongoLinkService.updateStatus(shortCode, Verdict.PENDING_REVERIFICATION);
            redisService.removeRedirectionCache(shortCode);
            log.info("Short code {} moved to pending reverification after {} abuse reports", shortCode, newReportCount);
        }
        AbuseReport reportObject = AbuseReport.builder()
                .reporterName(reporterName)
                .reporterEmail(reporterEmail)
                .reportStatus("SUCCESSFUL")
                .cause(dto.getCause())
                .description(dto.getDescription())
                .hashedKeyOfLink(shortCode)
                .createdAt(LocalDateTime.now())
                .build();

        abuseReportRepository.save(reportObject);
        log.info("Abuse report saved for short code {} with report count {}", shortCode, newReportCount);

        emailService.sendReportAcceptedEmail(reporterName, reporterEmail, linkToReport);

    }
}
