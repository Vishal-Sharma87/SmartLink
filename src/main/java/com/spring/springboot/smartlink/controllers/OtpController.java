package com.spring.springboot.smartlink.controllers;


import com.spring.springboot.smartlink.dto.requestDtos.OtpRequestDto;
import com.spring.springboot.smartlink.services.OtpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/verify")
@RequiredArgsConstructor
@Slf4j
public class OtpController {

    private final OtpService otpService;


    //    generate otp for a new user, reporter
    @PostMapping("/generate-otp")
    public ResponseEntity<String> generateOtpForEmail(@Valid @RequestBody OtpRequestDto otpRequestDto){

        otpService.sendOtp(otpRequestDto.getEmail());
        log.info("OTP generation completed for the requested email address");

        String otpGenerationMessage = "OTP is sent to the email address: " + otpRequestDto.getEmail();

        return ResponseEntity.ok(otpGenerationMessage);
    }


}
