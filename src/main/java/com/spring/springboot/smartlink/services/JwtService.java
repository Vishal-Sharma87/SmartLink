package com.spring.springboot.smartlink.services;


import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtService {

    @Value("${jwt.secretKey}")
    private String secretKey;


    public String generateJwt(String subject) {

        Instant currentInstant = Instant.now();
        Date now = new Date(currentInstant.toEpochMilli());
        
        Date expiry = new Date(currentInstant.plus(Duration.ofMinutes(10)).toEpochMilli());

        return Jwts.builder()
                .subject(subject)              // sub
                .claim("name", subject)           // custom claim
                .issuedAt(now)                 // iat
                .expiration(expiry)            // exp
                .signWith(
                        Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8))
                )
                .compact();
    }

    public String getSubjectFromJwtToken(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
}
