package com.spring.springboot.smartlink.services;

import com.spring.springboot.smartlink.email.dto.EmailBody;
import com.spring.springboot.smartlink.dto.requestDtos.ReportLinkRequestDto;
import com.spring.springboot.smartlink.email.contentbuilder.EmailContentBuilder;
import com.spring.springboot.smartlink.email.services.EmailService;
import com.spring.springboot.smartlink.entity.AbuseReport;
import com.spring.springboot.smartlink.enums.Verdict;
import com.spring.springboot.smartlink.repositories.AbuseReportRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncReportService {

    private final MongoLinkService mongoLinkService;
    private final AbuseReportRepository abuseReportRepository;
    private final EmailContentBuilder emailContentBuilder;
    private final RedisService redisService;

    private final EmailService emailService;


    @Async
    public void acceptReport(String hashedKey, @Valid ReportLinkRequestDto dto) {
        log.info("Asynchronous abuse-report processing started for short code {}", hashedKey);

        String linkToReport = dto.getLinkToReport();
        String reporterName = dto.getReporterName();
        String reporterEmail = dto.getReporterEmail();

        int newReportCount = mongoLinkService.incrementAndGetReportCount(hashedKey);

        if (newReportCount >= 3){
            mongoLinkService.updateStatus(hashedKey, Verdict.PENDING_REVERIFICATION);
            redisService.removeIfExists(hashedKey);
            log.info("Short code {} moved to pending reverification after {} abuse reports", hashedKey, newReportCount);
        }
        AbuseReport reportObject = AbuseReport.builder()
                .reporterName(reporterName)
                .reporterEmail(reporterEmail)
                .reportStatus("SUCCESSFUL")
                .cause(dto.getCause())
                .description(dto.getDescription())
                .hashedKeyOfLink(hashedKey)
                .createdAt(LocalDateTime.now())
                .build();

        abuseReportRepository.save(reportObject);
        log.info("Abuse report saved for short code {} with report count {}", hashedKey, newReportCount);

        EmailBody reportAccepted =
                emailContentBuilder.reportAcceptedContent(
                        reporterName,
                        reporterEmail,
                        linkToReport);

        emailService.sendEmail(reportAccepted);


    }
}
