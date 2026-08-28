package com.spring.springboot.smartlink.controllers;

import com.spring.springboot.smartlink.dto.requestDtos.LoginRequestDto;
import com.spring.springboot.smartlink.dto.requestDtos.SignupRequestDto;
import com.spring.springboot.smartlink.dto.requestDtos.SignupInitiationRequestDto;
import com.spring.springboot.smartlink.dto.requestDtos.SignupVerificationRequestDto;
import com.spring.springboot.smartlink.dto.responseDtos.AuthResponseDto;
import com.spring.springboot.smartlink.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpSession;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    public static final String PENDING_SIGNUP_SESSION_KEY = "pendingSignup";

    private final AuthService authService;

    // Signup endpoint
    @PostMapping("/signup")
    public ResponseEntity<AuthResponseDto<String>> signup(@Valid @RequestBody SignupRequestDto request) {
        log.debug("Signup request received for username {}", request.getUserName());

        authService.registerUser(request);
//        user create ask him to login

        AuthResponseDto<String> successfulRegistration = new AuthResponseDto<>("User registered successfully", "SUCCESS", LocalDateTime.now());

        return ResponseEntity.ok(successfulRegistration);
    }

    // Login endpoint
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto<String>> login(@Valid @RequestBody LoginRequestDto request) {
//        check credentials and if correct generate JWT using username
        log.debug("Login request received for username {}", request.getUserName());

        String generatedJwtForLogin = authService.loginUser(request);

        AuthResponseDto<String> successfulLoginResponse = new AuthResponseDto<>(generatedJwtForLogin, "Login successful", LocalDateTime.now());

        return ResponseEntity.ok(successfulLoginResponse);
    }

    @PostMapping("/signup/initiate")
    public ResponseEntity<AuthResponseDto<String>> initiateSignup(@Valid @RequestBody SignupInitiationRequestDto request,
                                                                    HttpSession session) {
        authService.initiateSignup(request);
        session.setAttribute(PENDING_SIGNUP_SESSION_KEY, request);
        return ResponseEntity.ok(new AuthResponseDto<>(request.getEmail(), "Verification code sent", LocalDateTime.now()));
    }

    @PostMapping("/signup/verify")
    public ResponseEntity<AuthResponseDto<String>> verifySignup(@Valid @RequestBody SignupVerificationRequestDto request,
                                                                  HttpSession session) {
        SignupInitiationRequestDto pending = (SignupInitiationRequestDto) session.getAttribute(PENDING_SIGNUP_SESSION_KEY);
        if (pending == null) {
            return ResponseEntity.badRequest().body(new AuthResponseDto<>(null, "Your signup session has expired. Please start again.", LocalDateTime.now()));
        }
        authService.completeSignup(pending, request);
        session.removeAttribute(PENDING_SIGNUP_SESSION_KEY);
        return ResponseEntity.ok(new AuthResponseDto<>(null, "Account created successfully", LocalDateTime.now()));
    }
}
