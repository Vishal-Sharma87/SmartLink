package com.spring.springboot.UrlShortener.controllers;


import com.spring.springboot.UrlShortener.dto.requestDtos.OtpRequestDto;
import com.spring.springboot.UrlShortener.services.OtpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/verify")
@RequiredArgsConstructor
public class OtpController {

    private final OtpService otpService;


    //    generate otp for a new user, reporter
    @PostMapping("/generate-otp")
    public ResponseEntity<String> generateOtpForEmail(@Valid @RequestBody OtpRequestDto otpRequestDto){

        otpService.sendOtp(otpRequestDto.getEntityEmail());

        String otpGenerationMessage = "OTP is sent to the email address: " + otpRequestDto.getEntityEmail();

        return ResponseEntity.ok(otpGenerationMessage);
    }


}
