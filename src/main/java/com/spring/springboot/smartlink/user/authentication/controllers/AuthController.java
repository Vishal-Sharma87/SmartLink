package com.spring.springboot.smartlink.user.authentication.controllers;

import com.spring.springboot.smartlink.advices.enums.ErrorCode;
import com.spring.springboot.smartlink.advices.exceptions.SmartlinkAuthException;
import com.spring.springboot.smartlink.apiresponse.ApiResponse;
import com.spring.springboot.smartlink.apiresponse.ResponseMessage;
import com.spring.springboot.smartlink.configurations.ExceptionMessages;
import com.spring.springboot.smartlink.user.authentication.dtos.*;
import com.spring.springboot.smartlink.user.authentication.services.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";

    private final AuthService authService;
    private final ResponseMessage responseMessage;
    private final ExceptionMessages exceptionMessages;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginDto request,
            HttpServletResponse httpServletResponse) {

        AuthTokens tokens = authService.loginUser(request);

        addRefreshTokenCookie(httpServletResponse, tokens.refreshToken());
        AuthResponse response = AuthResponse.of(tokens.jwtToken(), responseMessage.authCompleted());

        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PostMapping("/signup/initiate")
    public ResponseEntity<ApiResponse<SignupInitiateResponse>> initiateSignup(
            @Valid @RequestBody SignupInitiateDto request,
            HttpSession session) {

        SignupInitiateResponse response = authService.initiateSignup(request, session);

        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PostMapping("/signup/verify")
    public ResponseEntity<ApiResponse<AuthResponse>> verifySignup(
            @Valid @RequestBody OtpDto request,
            HttpSession session,
            HttpServletResponse httpServletResponse) {

        AuthTokens tokens = authService.getJwtIfSignupCompletes(request.otp(), session);
        addRefreshTokenCookie(httpServletResponse, tokens.refreshToken());

        AuthResponse response = AuthResponse.of(tokens.jwtToken(), responseMessage.authCompleted());

        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) {

        Cookie[] cookies = httpServletRequest.getCookies();
        String refreshToken = getRefreshTokenCookie(cookies);

        if (refreshToken == null) {
            log.warn("Refresh token request rejected because the refresh cookie was missing");
            throw new SmartlinkAuthException(ErrorCode.INVALID_REFRESH_TOKEN, exceptionMessages.authException());
        }

        AuthTokens tokens = authService.refreshToken(refreshToken);
        addRefreshTokenCookie(httpServletResponse, tokens.refreshToken());

        AuthResponse response = AuthResponse.of(tokens.jwtToken(), responseMessage.refreshTokenRotated());

        return ResponseEntity.ok(ApiResponse.of(response));
    }

    private String getRefreshTokenCookie(Cookie[] cookies) {
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (AuthController.REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }

    private void addRefreshTokenCookie(
            HttpServletResponse httpServletResponse,
            String refreshToken) {

        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/auth/refresh-token");

        httpServletResponse.addCookie(cookie);
    }
}
