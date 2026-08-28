package com.spring.springboot.smartlink.services;


import com.spring.springboot.smartlink.dto.requestDtos.ReportLinkRequestDto;
import com.spring.springboot.smartlink.advices.exceptions.InvalidOTPException;
import com.spring.springboot.smartlink.advices.exceptions.LinkAlreadyReportedByCurrentEmailOfReporterException;
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
    private final MongoLinkService mongoLinkService;

    public void tryAcceptingReport(@Valid ReportLinkRequestDto dto) {
        /*
         * Steps to perform
         * step 1: -> validate otp
         * step 2: -> extract hashedKey from the given url
         * step 3: -> check if user have already reported this report or not
         * */

        if (!otpService.isValidOtp(dto.getReporterEmail(), dto.getOtp())){
            log.warn("Abuse report rejected because OTP validation failed");
            throw new InvalidOTPException("Invalid otp, please enter a valid otp");
        }

//        extract hashedKey from the url
//        actualUrl = domain/hashedKey
//        key = hashedKey
        String[] parts = dto.getLinkToReport().split("/");
        String hashedKey =  parts[parts.length - 1];

        boolean alreadyReported = mongoLinkService.isAlreadyReported(hashedKey, dto);

        if (alreadyReported) {
            log.warn("Duplicate abuse report rejected for short code {}", hashedKey);
            String msg = dto.getReporterName() + " you have already reported this url. The url is in verification phase, we will notify you once we have confirmation";
                throw new LinkAlreadyReportedByCurrentEmailOfReporterException(msg);
        }

        asyncReportService.acceptReport(hashedKey, dto);
        log.info("Abuse report queued for short code {}", hashedKey);
    }
}
