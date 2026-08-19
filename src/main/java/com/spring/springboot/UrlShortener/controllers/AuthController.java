package com.spring.springboot.UrlShortener.controllers;

import com.spring.springboot.UrlShortener.dto.requestDtos.LoginRequestDto;
import com.spring.springboot.UrlShortener.dto.requestDtos.SignupRequestDto;
import com.spring.springboot.UrlShortener.dto.responseDtos.AuthResponseDto;
import com.spring.springboot.UrlShortener.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // Signup endpoint
    @PostMapping("/signup")
    public ResponseEntity<AuthResponseDto<String>> signup(@Valid @RequestBody SignupRequestDto request) {

        authService.registerUser(request);
//        user create ask him to login

        AuthResponseDto<String> successfulRegistration = new AuthResponseDto<>("User registered successfully", "SUCCESS", LocalDateTime.now());

        return ResponseEntity.ok(successfulRegistration);
    }

    // Login endpoint
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto<String>> login(@Valid @RequestBody LoginRequestDto request) {
//        check credentials and if correct generate JWT using username

        String generatedJwtForLogin = authService.loginUser(request);

        AuthResponseDto<String> successfulLoginResponse = new AuthResponseDto<>(generatedJwtForLogin, "Login successful", LocalDateTime.now());

        return ResponseEntity.ok(successfulLoginResponse);
    }
}
