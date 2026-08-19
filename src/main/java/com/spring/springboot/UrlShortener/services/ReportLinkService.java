package com.spring.springboot.UrlShortener.services;


import com.spring.springboot.UrlShortener.dto.requestDtos.ReportLinkRequestDto;
import com.spring.springboot.UrlShortener.advices.exceptions.InvalidOTPException;
import com.spring.springboot.UrlShortener.advices.exceptions.LinkAlreadyReportedByCurrentEmailOfReporterException;
import com.spring.springboot.UrlShortener.repositories.MongoReportLinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportLinkService {

    private final OtpService otpService;
    private final MongoReportLinkService mongoReportLinkService;
    private final AsyncReportService asyncReportService;

    public void tryAcceptingReport(@Valid ReportLinkRequestDto dto) {
        /*
         * Steps to perform
         * step 1: -> validate otp
         * step 2: -> extract hashedKey from the given url
         * step 3: -> check if user have already reported this report or not
         * */

//        step 1: -> validate otp
        try {
            otpService.isValidOtp(dto.getReporterEmail(), dto.getOtp());
        } catch (Exception e) {
            throw new InvalidOTPException("Invalid otp, please enter a valid otp");
        }

//        step 2:-> extract hashedKey from the given url
//        extract hashedKey from the url
//        actualUrl = url.shortener/hashedKey
//        key = hashedKey
        String hashedKey = dto.getLinkToReport().substring(14);

//        step 3: -> check if user have already reported this report or not
        boolean alreadyReported = mongoReportLinkService.isAlreadyReported(hashedKey, dto);

        if (alreadyReported) {
            String msg = dto.getReporterName() + " you have already reported this url. The url is in verification phase, we will notify you once we have confirmation";
            throw new LinkAlreadyReportedByCurrentEmailOfReporterException(msg);
        }

//        if we are reaching this lines then the link is valid and needs to report
//        step 3: -> make a call to report this url
//        calling the @Async annotated method using proxy

        asyncReportService.acceptReport(hashedKey, dto);
    }
}
