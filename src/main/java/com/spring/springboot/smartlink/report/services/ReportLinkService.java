package com.spring.springboot.smartlink.report.services;

import com.spring.springboot.smartlink.apiresponse.ResponseMessage;
import com.spring.springboot.smartlink.report.dtos.ReportLinkRequestDto;
import com.spring.springboot.smartlink.advices.exceptions.InvalidOTPExceptionSmartLink;
import com.spring.springboot.smartlink.advices.exceptions.LinkAlreadyReportedExceptionSmartLink;
import com.spring.springboot.smartlink.configurations.ExceptionMessages;
import com.spring.springboot.smartlink.otp.services.OtpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportLinkService {

    private final OtpService otpService;
    private final AsyncReportService asyncReportService;
    private final ExceptionMessages exceptionMessages;
    private final MongoReportService mongoReportService;
    private final ResponseMessage responseMessage;

    public String tryAcceptingReport(@Valid ReportLinkRequestDto dto) {

        if (otpService.isInvalidOtp(dto.getReporterEmail(), dto.getOtp())) {
            log.warn("Abuse report rejected because OTP validation failed");
            throw new InvalidOTPExceptionSmartLink(exceptionMessages.invalidOtp());
        }

        // actualUrl = domain/shortCode
        // key = shortCode

        String[] parts = dto.getLinkToReport().split("/");
        String shortCode = parts[parts.length - 1];

        boolean alreadyReported = mongoReportService.isAlreadyReported(shortCode, dto);

        if (alreadyReported) {
            log.warn("Duplicate abuse report rejected for short code {}", shortCode);
            throw new LinkAlreadyReportedExceptionSmartLink(exceptionMessages.linkAlreadyReported());
        }

        asyncReportService.acceptReport(shortCode, dto);
        log.info("Abuse report queued for short code {}", shortCode);

        return responseMessage.reportAccepted();
    }
}
