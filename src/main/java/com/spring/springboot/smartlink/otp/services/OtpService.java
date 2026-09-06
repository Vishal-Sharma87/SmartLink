package com.spring.springboot.smartlink.otp.services;

import com.spring.springboot.smartlink.email.services.EmailService;
import com.spring.springboot.smartlink.configurations.ExceptionMessages;
import com.spring.springboot.smartlink.redis.services.RedisService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
@Service
public class OtpService {

    private final RedisService redisService;
    private final EmailService emailService;
    private final ExceptionMessages exceptionMessages;

    public OtpService(
            RedisService redisService,
            EmailService emailService,
            ExceptionMessages exceptionMessages) {

        this.redisService = redisService;
        this.emailService = emailService;
        this.exceptionMessages = exceptionMessages;
    }

    public void sendOtp(String email) {
        String generatedOtp = generateFourLengthNumericOtp();
        String hashedOtp = hashOtp(generatedOtp);

        redisService.saveOTP(email, hashedOtp);

        emailService.sendOtpEmail(email, generatedOtp);
    }

    public boolean isInvalidOtp(String email, String otp) {
        String hashedOtpToPass = hashOtp(otp);

        return redisService.isInvalidOtp(email, hashedOtpToPass);
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
            log.error("OTP hashing failed because SHA-256 algorithm was unavailable", e);
            throw new IllegalStateException(exceptionMessages.sha256Unavailable());
        }
    }
}
