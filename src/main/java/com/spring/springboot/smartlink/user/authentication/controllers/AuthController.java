package com.spring.springboot.smartlink.user.authentication.controllers;

import com.spring.springboot.smartlink.apiresponse.ApiResponse;
import com.spring.springboot.smartlink.user.authentication.dtos.*;
import com.spring.springboot.smartlink.user.authentication.services.AuthService;
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

    private final AuthService authService;

    // Signup endpoint
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthResponse>> signup(@Valid @RequestBody SignupRequestDto request) {
        log.debug("Signup request received for username {}", request.getUserName());

        AuthResponse response = authService.registerUser(request);

        return ResponseEntity.ok(ApiResponse.of(response));
    }

    // Login endpoint
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequestDto request) {
        // check credentials and if correct generate JWT using username
        log.debug("Login request received for username {}", request.getUserName());

        AuthResponse response = authService.loginUser(request);

        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PostMapping("/signup/initiate")
    public ResponseEntity<ApiResponse<SignupInitiateResponse>> initiateSignup(
            @Valid @RequestBody SignupInitiationRequestDto request,
            HttpSession session) {

        SignupInitiateResponse response = authService.initiateSignup(request, session);

        return ResponseEntity.ok(ApiResponse.of(response));

    }

    @PostMapping("/signup/verify")
    public ResponseEntity<ApiResponse<AuthResponse>> verifySignup(
            @Valid @RequestBody SignupVerificationRequestDto request,
            HttpSession session) {

        AuthResponse response = authService.getJwtIfSignupCompletes(request, session);

        return ResponseEntity.ok(ApiResponse.of(response));
    }
}
