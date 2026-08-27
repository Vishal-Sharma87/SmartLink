package com.spring.springboot.smartlink.services;

import com.spring.springboot.smartlink.dto.requestDtos.ReportLinkRequestDto;
import com.spring.springboot.smartlink.dto.emailComponents.EmailContentBuilder;
import com.spring.springboot.smartlink.dto.emailComponents.EmailDto;
import com.spring.springboot.smartlink.entity.AbuseReport;
import com.spring.springboot.smartlink.enums.Verdict;
import com.spring.springboot.smartlink.repositories.AbuseReportRepository;

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
    private final RedisService redisService;


    @Async
    public void acceptReport(String hashedKey, @Valid ReportLinkRequestDto dto) {
        log.info("Asynchronous abuse-report processing started for short code {}", hashedKey);

        int newReportCount = mongoLinkService.incrementAndGetReportCount(hashedKey);

        if (newReportCount >= 3){
            mongoLinkService.updateStatus(hashedKey, Verdict.PENDING_REVERIFICATION);
            redisService.removeIfExists(hashedKey);
            log.info("Short code {} moved to pending reverification after {} abuse reports", hashedKey, newReportCount);
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
        log.info("Abuse report saved for short code {} with report count {}", hashedKey, newReportCount);
//        send a confirmation mail that we have accepted his report and will notify after confirmation
        EmailDto emailDtoWithSuccessfulReportContent = emailContentBuilder.getEmailDtoWithSuccessfulReportContent(dto.getLinkToReport(), dto.getReporterEmail());
        try {
            emailService.sendEmail(emailDtoWithSuccessfulReportContent);
            log.debug("Abuse report confirmation email sent for short code {}", hashedKey);
        } catch (IOException e) {
            log.error("Something went wrong and email for successful report failed. Exception: {}", e.getMessage());
            log.error("Failed to send abuse report confirmation for short code {}", hashedKey, e);
        }

    }
}
