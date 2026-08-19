package com.spring.springboot.UrlShortener.services;

import com.spring.springboot.UrlShortener.scripts.LuaScripts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {


    private final SecureRandom random = new SecureRandom();
    private final StringRedisTemplate stringRedisTemplate;

    public void sendOtp(String email) {
        String generatedOtp = generateFourLengthNumericOtp();
        String hashedOtp = hashOtp(generatedOtp);

        stringRedisTemplate.opsForValue().set(email, hashedOtp, 10, TimeUnit.MINUTES);

        log.info("OTP for email: {} is {}", email, generatedOtp);
    }


    public boolean isValidOtp(String email, String otp){
        String hashedOtpToPass = hashOtp(otp);

        RedisScript<Long> validateOtpScript = LuaScripts.VALIDATE_OTP_SCRIPT;

        Long isValid = stringRedisTemplate.execute(validateOtpScript, List.of(email), hashedOtpToPass);

        return isValid.equals(1L);
    }

    private String generateFourLengthNumericOtp() {
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
            throw new IllegalStateException("SHA-256 not available");
        }
    }
}
