package com.spring.springboot.smartlink.jwt.services;

import com.spring.springboot.smartlink.jwt.configs.JwtConfigs;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final JwtConfigs jwtConfigs;

    public JwtService(JwtConfigs jwtConfigs) {
        this.jwtConfigs = jwtConfigs;
    }

    public String generateJwt(String subject) {

        Instant currentInstant = Instant.now();
        Date now = new Date(currentInstant.toEpochMilli());
        Date expiry = new Date(
                currentInstant.plus(Duration.ofMinutes(jwtConfigs.expiryDurationMinutes())).toEpochMilli());

        return Jwts.builder()
                .subject(subject) // sub
                .claim("name", subject) // custom claim
                .issuedAt(now) // iat
                .expiration(expiry) // exp
                .signWith(
                        Keys.hmacShaKeyFor(jwtConfigs.secretKey().getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    public String getSubjectFromJwtToken(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(jwtConfigs.secretKey().getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
}
