package com.spring.springboot.smartlink.jwt.services;

import com.spring.springboot.smartlink.advices.enums.ErrorCode;
import com.spring.springboot.smartlink.advices.exceptions.SmartlinkAuthException;
import com.spring.springboot.smartlink.configurations.ExceptionMessages;
import com.spring.springboot.smartlink.jwt.configs.JwtConfigs;
import com.spring.springboot.smartlink.redis.services.RedisService;
import com.spring.springboot.smartlink.user.authentication.dtos.AuthTokens;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
@Slf4j
public class JwtService {

    private final JwtConfigs jwtConfigs;
    private final RedisService redisService;
    private final ExceptionMessages exceptionMessages;

    public JwtService(
            JwtConfigs jwtConfigs,
            RedisService redisService,
            ExceptionMessages exceptionMessages) {

        this.jwtConfigs = jwtConfigs;
        this.redisService = redisService;
        this.exceptionMessages = exceptionMessages;
    }

    public AuthTokens generateJwt(String email) {

        Instant currentInstant = Instant.now();
        Date now = new Date(currentInstant.toEpochMilli());
        Date jwtExpiry = new Date(
                currentInstant.plus(Duration.ofMinutes(jwtConfigs.expiryDurationMinutes())).toEpochMilli());
        Date refreshExpiry = new Date(
                currentInstant.plus(Duration.ofDays(jwtConfigs.refreshExpiryDays())).toEpochMilli());

        String jwt = Jwts.builder()
                .subject(email) // sub
                .issuedAt(now) // iat
                .expiration(jwtExpiry) // exp
                .signWith(
                        Keys.hmacShaKeyFor(jwtConfigs.secretKey().getBytes(StandardCharsets.UTF_8)))
                .compact();

        String refreshToken = Jwts.builder()
                .issuedAt(now)
                .expiration(refreshExpiry)
                .signWith(
                        Keys.hmacShaKeyFor(jwtConfigs.secretKey().getBytes(StandardCharsets.UTF_8)))
                .compact();

        redisService.revokeOtherThenSetCurrentRefreshToken(refreshToken, email);

        return new AuthTokens(jwt, refreshToken);
    }

    public String getSubjectFromJwtToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(jwtConfigs.secretKey().getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (JwtException | IllegalArgumentException ex) {
            throw authenticationTokenFailure();
        }
    }

    public AuthTokens rotateTokensIfValid(String refreshToken) {
        try {
            Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(jwtConfigs.secretKey().getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseSignedClaims(refreshToken);
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Refresh token validation failed");
            throw invalidRefreshToken();
        }

        String email = redisService.getEmailFromRefreshToken(refreshToken);
        return generateJwt(email);

    }

    private SmartlinkAuthException authenticationTokenFailure() {
        return new SmartlinkAuthException(
                ErrorCode.AUTHENTICATION_FAILED,
                exceptionMessages.authenticationFailed());
    }

    private SmartlinkAuthException invalidRefreshToken() {
        return new SmartlinkAuthException(
                ErrorCode.INVALID_REFRESH_TOKEN,
                exceptionMessages.authException());
    }
}
