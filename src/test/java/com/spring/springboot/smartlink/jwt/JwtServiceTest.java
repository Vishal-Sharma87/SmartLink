package com.spring.springboot.smartlink.jwt;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.spring.springboot.smartlink.configurations.ExceptionMessages;
import com.spring.springboot.smartlink.jwt.configs.JwtConfigs;
import com.spring.springboot.smartlink.jwt.services.JwtService;
import com.spring.springboot.smartlink.redis.services.RedisService;
import com.spring.springboot.smartlink.user.authentication.dtos.AuthTokens;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private static final String SECRET_KEY =
            "v3ry-53cur3-4nd-long-jwt-53cr3t-k3y-2026!";
    private static final String DIFFERENT_SECRET_KEY =
            "v3ry-53cur3-4nd-long-jwt-53cr3t-k3y-2016!";

    private static final String EMAIL = "user@example.com";
    private static final String REFRESH_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyXzk4NzY1NDMyMSIsIm5hbWUiOiJBbGV4IE1lcmNlciIsInJvbGUiOiJhZG1pbiIsImlhdCI6MTc4OTc4NTYwMCwiZXhwIjoyMTA0OTYzMjAwfQ.UNK9kWqjBsdkPKeS2dJmV-aOeZdr4pBfgkfTREMuwAw";
    private static final String INVALID_REFRESH_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyXzk4NzY1NDMyMSIsIm5hbWUiOiJBbGV4IE1lcmNlciIsInJvbGUiOiJhZG1pbiIsImlhdCI6MTc4OTc4NTYwMCwiZXhwIjoyMTA0OTYzMjAwfQ.UNK9kWqjBsdkPKeS2dJmV-aOeZdr4pBfgkfcremuwAw";


    private final JwtConfigs jwtConfigs = new JwtConfigs(SECRET_KEY, 15, 30);

    String linkNotFound = "Link not found for hash: %s";
    String invalidLinkHashFormat = "Invalid link hash format: %s";
    String userNotFound = "User not found: %s";
    String noScanResultFound = "No scan result found for short code: %s";
    String linkAlreadyReported = "This link has already been reported by this email";
    String invalidOrExpiredOTP = "Invalid or expired OTP";
    String atLeastOneFieldRequired = "At least one field (username, email, or password) must be provided for update";
    String sha256Unavailable = "SHA-256 algorithm is unavailable";
    String userWithEmailExists = "User with email already exists: %s";
    String pleaseLoginAgain = "Please login again";
    String requestContainsInvalidFields = "Request contains invalid or missing fields";
    String authenticationFailed = "Authentication failed";
    String accessDenied = "You do not have permission to access this resource";
    String unexpectedServerError = "An unexpected server error occurred";

    private final ExceptionMessages exceptionMessages = new ExceptionMessages(
            linkNotFound,
            invalidLinkHashFormat,
            userNotFound,
            noScanResultFound,
            linkAlreadyReported,
            invalidOrExpiredOTP,
            atLeastOneFieldRequired,
            sha256Unavailable,
            userWithEmailExists,
            pleaseLoginAgain,
            requestContainsInvalidFields,
            authenticationFailed,
            accessDenied,
            unexpectedServerError
    );

    @Mock
    private RedisService redisService;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(jwtConfigs, redisService, exceptionMessages);
    }

    @Test
    void shouldGenerateJwtAndRefreshToken() {

        AuthTokens tokens = jwtService.generateJwt(EMAIL);

        assertNotNull(tokens);
        assertNotNull(tokens.jwtToken());
        assertNotNull(tokens.refreshToken());

        verify(redisService).revokeOtherThenSetCurrentRefreshToken(
                tokens.refreshToken(),
                EMAIL
        );
    }

    @Test
    void generatedJwtShouldContainEmailAsSubject() {
        AuthTokens tokens = jwtService.generateJwt(EMAIL);

        String subject = jwtService.getSubjectFromJwtToken(
                tokens.jwtToken()
        );

        assertEquals(EMAIL, subject);
    }

    @Test
    void shouldRejectInvalidJwtSignature() {

        String invalidToken = Jwts.builder()
                .subject(EMAIL)
                .issuedAt(new Date())
                .expiration(
                        new Date(
                                System.currentTimeMillis()
                                        + Duration.ofMinutes(15).toMillis()
                        )
                )
                .signWith(
                        Keys.hmacShaKeyFor(
                                DIFFERENT_SECRET_KEY
                                        .getBytes(StandardCharsets.UTF_8)
                        )
                )
                .compact();

        assertThrows(
                Exception.class,
                () -> jwtService.getSubjectFromJwtToken(invalidToken)
        );
    }

    @Test
    void shouldRotateTokensForValidRefreshToken() {
        when(redisService.getEmailFromRefreshToken(REFRESH_TOKEN))
                .thenReturn(EMAIL);

        AuthTokens tokens = jwtService.rotateTokensIfValid(REFRESH_TOKEN);

        assertNotNull(tokens);
        assertNotNull(tokens.jwtToken());
        assertNotNull(tokens.refreshToken());

        verify(redisService).getEmailFromRefreshToken(REFRESH_TOKEN);

        verify(redisService).revokeOtherThenSetCurrentRefreshToken(
                tokens.refreshToken(),
                EMAIL
        );
    }

    @Test
    void shouldNotLookupRedisWhenRefreshTokenJwtIsInvalid() {

        assertThrows(
                Exception.class,
                () -> jwtService.rotateTokensIfValid(INVALID_REFRESH_TOKEN)
        );

        verify(
                redisService,
                never()
        ).getEmailFromRefreshToken(INVALID_REFRESH_TOKEN);
    }
}