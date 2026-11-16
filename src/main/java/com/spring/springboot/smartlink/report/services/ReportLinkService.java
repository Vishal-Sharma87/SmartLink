package com.spring.springboot.smartlink.report.services;

import com.spring.springboot.smartlink.advices.exceptions.LinkNotFoundExceptionSmartLink;
import com.spring.springboot.smartlink.apiresponse.ResponseMessage;
import com.spring.springboot.smartlink.enums.Verdict;
import com.spring.springboot.smartlink.link.services.LinkService;
import com.spring.springboot.smartlink.link.services.MongoLinkService;
import com.spring.springboot.smartlink.redis.services.RedisService;
import com.spring.springboot.smartlink.report.dtos.ReportLinkRequestDto;
import com.spring.springboot.smartlink.advices.exceptions.InvalidOTPExceptionSmartLink;
import com.spring.springboot.smartlink.advices.exceptions.LinkAlreadyReportedExceptionSmartLink;
import com.spring.springboot.smartlink.configurations.ExceptionMessages;
import com.spring.springboot.smartlink.otp.services.OtpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportLinkService {

    private final OtpService otpService;
    private final ExceptionMessages exceptionMessages;
    private final MongoReportService mongoReportService;
    private final ResponseMessage responseMessage;
    private final LinkService linkService;
    private final MongoLinkService mongoLinkService;
    private final RedisService redisService;

    public String tryAcceptingReport(@Valid ReportLinkRequestDto dto) {

        if (otpService.isInvalidOtp(dto.reporterEmail(), dto.otp())) {
            log.warn("Abuse report rejected because OTP validation failed");
            throw new InvalidOTPExceptionSmartLink(exceptionMessages.invalidOtp());
        }

        try {
            String[] parts = dto.linkToReport().split("/");
            String shortCode = parts[parts.length - 1];

            if (!linkService.existsById(shortCode)) {
                throw new LinkNotFoundExceptionSmartLink(exceptionMessages.linkNotFound());
            }

            int newReportCount = mongoReportService.acceptReportAndGetReportCount(dto);

            if (newReportCount >= 3) {
                mongoLinkService.updateStatus(shortCode, Verdict.PENDING_REVERIFICATION);
                redisService.removeRedirectionCache(shortCode);
            }

        } catch (DuplicateKeyException e) {
            log.warn("Abuse report rejected because the link is already reported");
            throw new LinkAlreadyReportedExceptionSmartLink(exceptionMessages.linkAlreadyReported());
        }

        return responseMessage.reportAccepted();
    }

}
