package com.spring.springboot.smartlink.services;

import com.spring.springboot.smartlink.email.contentbuilder.EmailContentBuilder;
import com.spring.springboot.smartlink.email.dto.EmailBody;
import com.spring.springboot.smartlink.email.services.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {


    private final RedisService redisService;
    private final EmailService emailService;
    private final EmailContentBuilder emailContentBuilder;

    public void sendOtp(String email) {
        String generatedOtp = generateFourLengthNumericOtp();
        String hashedOtp = hashOtp(generatedOtp);

        redisService.saveOTP(email, hashedOtp);

        EmailBody otpToEmail = emailContentBuilder.otpContent(
                email,
                generatedOtp);

        emailService.sendEmail(otpToEmail);
    }


    public boolean isValidOtp(String email, String otp){
        String hashedOtpToPass = hashOtp(otp);

        return redisService.isValidOtp(email, hashedOtpToPass);
    }

    private String generateFourLengthNumericOtp() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(4);

        for (int i = 0; i < 4; i++) {
            sb.append(random.nextInt(10)); // 0–9
        }
        return sb.toString();
    }

    private String hashOtp(String otp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(otp.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            log.error("Something went wrong while hashing the OTP. Exception: {}", e.getMessage());
            log.error("OTP hashing failed because SHA-256 algorithm was unavailable", e);
            throw new IllegalStateException("SHA-256 not available");
        }
    }
}
