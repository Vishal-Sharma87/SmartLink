package com.spring.springboot.smartlink.otp.controllers;

import com.spring.springboot.smartlink.otp.dto.OtpRequestDto;
import com.spring.springboot.smartlink.otp.services.OtpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/verify")
@RequiredArgsConstructor
public class OtpController {

    private final OtpService otpService;

    // generate otp for a new user, reporter
    @PostMapping("/generate-otp")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> generateOtpForEmail(@Valid @RequestBody OtpRequestDto otpRequestDto) {

        otpService.sendOtp(otpRequestDto.getEmail());

        return ResponseEntity.noContent().build();
    }

}
